const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PARENT_ALICE = "parent_alice_1";
const PARENT_BOB = "parent_bob_2";
const DRIVER_ERICK = "driver_erick_1";
const DRIVER_JOHN = "driver_john_2";
const ADMIN_CHIEF = "admin_chief_1";
const ADMIN_EMAIL = "info@veritasglobaladvisory.org";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await db.collection("admins").doc(ADMIN_CHIEF).set({
        uid: ADMIN_CHIEF,
        email: ADMIN_EMAIL,
        createdAt: serverTimestamp(),
      });
      await db.collection("drivers").doc(DRIVER_ERICK).set({
        uid: DRIVER_ERICK,
        name: "Erick Mwangi",
        busPlate: "KDE 732X",
        yearsExperience: 6,
        onTimePct: 96,
        active: true,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      await db.collection("drivers").doc(DRIVER_JOHN).set({
        uid: DRIVER_JOHN,
        name: "John Kiprop",
        busPlate: "KDE 119A",
        yearsExperience: 4,
        onTimePct: 92,
        active: true,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      await db.collection("subscriptions").doc("main").set({
        status: "ACTIVE",
        planName: "Safiri Fleet Enterprise Tier",
        monthlyAmountKsh: 15000,
        currentPeriodEnd: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
        updatedAt: serverTimestamp(),
      });
    });
  }
});

// --- 1. UNAUTHENTICATED SECURITY BOUNDS ---

test("Unauthenticated caller cannot read or list any protected collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(PARENT_ALICE).get());
  await assertFails(unauthDb.collection("students").where("parentUid", "==", PARENT_ALICE).get());
  await assertFails(unauthDb.collection("interportal_messages").where("senderUid", "==", PARENT_ALICE).get());
  await assertFails(unauthDb.collection("alerts").where("status", "==", "Ongoing").get());
  await assertFails(unauthDb.collection("community_incidents").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("fleet_buses").where("status", "==", "On Time").get());
});

// --- 2. USER PROFILE, PII ISOLATION & PRIVILEGE ESCALATION ---

test("Parent can create own PARENT profile, but cannot self-assign ADMIN or DRIVER role", async () => {
  const aliceDb = testEnv.authenticatedContext(PARENT_ALICE, {
    email: "alice@safiri.co.ke",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(PARENT_ALICE).set({
      userId: PARENT_ALICE,
      email: "alice@safiri.co.ke",
      displayName: "Alice Kamau",
      role: "PARENT",
      phone: "0712345678",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  const bobDb = testEnv.authenticatedContext(PARENT_BOB, {
    email: "bob@safiri.co.ke",
    email_verified: true,
  }).firestore();

  // Attempt privilege escalation to ADMIN or DRIVER
  await assertFails(
    bobDb.collection("users").doc(PARENT_BOB).set({
      userId: PARENT_BOB,
      email: "bob@safiri.co.ke",
      displayName: "Bob Escalator",
      role: "ADMIN",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(PARENT_BOB).set({
      userId: PARENT_BOB,
      email: "bob@safiri.co.ke",
      displayName: "Bob Driver",
      role: "DRIVER",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Device messaging tokens are owner-write-only and cannot be read by clients", async () => {
  const aliceDb = testEnv.authenticatedContext(PARENT_ALICE).firestore();
  const aliceToken = aliceDb
    .collection("users")
    .doc(PARENT_ALICE)
    .collection("device_tokens")
    .doc("token_hash_1");
  const tokenData = {
    token: "fcm-device-token",
    platform: "android",
    updatedAt: serverTimestamp(),
  };

  await assertSucceeds(aliceToken.set(tokenData));
  await assertFails(aliceToken.get());
  await assertFails(
    aliceToken.update({
      platform: "ios",
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    aliceToken.update({
      updatedAt: serverTimestamp(),
    })
  );

  const bobToken = testEnv
    .authenticatedContext(PARENT_BOB)
    .firestore()
    .collection("users")
    .doc(PARENT_ALICE)
    .collection("device_tokens")
    .doc("token_hash_1");
  await assertFails(bobToken.get());
  await assertFails(bobToken.delete());
  await assertSucceeds(aliceToken.delete());
});

test("Inactive subscriptions gate fleet data but leave admin billing readable", async () => {
  const alert = {
    authorUid: ADMIN_CHIEF,
    title: "Test alert",
    detail: "Subscription gate test",
    severity: "INFO",
    status: "Ongoing",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("subscriptions").doc("main").update({
      status: "INACTIVE",
    });
    await db.collection("subscriptions").doc("main")
      .collection("invoices").doc("invoice_1").set({
        status: "PENDING",
        amountKsh: 15000,
        currency: "KES",
      });
    await db.collection("alerts").doc("alert_subscription_gate").set(alert);
  });

  const parentDb = testEnv.authenticatedContext(PARENT_ALICE).firestore();
  await assertSucceeds(
    parentDb.collection("subscriptions").doc("main").get()
  );
  await assertFails(
    parentDb.collection("students").where("parentUid", "==", PARENT_ALICE).get()
  );
  await assertFails(
    parentDb.collection("alerts").where("status", "==", "Ongoing").get()
  );
  await assertFails(
    parentDb.collection("subscriptions").doc("main").update({
      status: "ACTIVE",
    })
  );

  const adminDb = testEnv.authenticatedContext(ADMIN_CHIEF).firestore();
  await assertSucceeds(
    adminDb.collection("subscriptions").doc("main")
      .collection("invoices").get()
  );
});

test("Parent can atomically register a profile and child before bus assignment", async () => {
  const aliceDb = testEnv.authenticatedContext(PARENT_ALICE, {
    email: "alice@safiri.co.ke",
    email_verified: true,
  }).firestore();
  const batch = aliceDb.batch();

  batch.set(aliceDb.collection("users").doc(PARENT_ALICE), {
    userId: PARENT_ALICE,
    email: "alice@safiri.co.ke",
    displayName: "Alice Kamau",
    role: "PARENT",
    phone: "0712345678",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  batch.set(aliceDb.collection("students").doc("st_alice_pending"), {
    parentUid: PARENT_ALICE,
    name: "Brian Kamau",
    initials: "BK",
    parentName: "Alice Kamau",
    parentEmail: "alice@safiri.co.ke",
    parentPhone: "0712345678",
    assignedBusPlate: "",
    assignedDriverName: "",
    pickupStop: "Kilimani Stop",
    dropoffStop: "St. Mary's Academy",
    status: "NOT_BOARDED",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });

  await assertSucceeds(batch.commit());

  await assertSucceeds(
    aliceDb.collection("students").doc("st_alice_pending").update({
      pickupStop: "Kilimani Gate",
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    aliceDb.collection("students").doc("st_alice_unapproved").set({
      parentUid: PARENT_ALICE,
      name: "Brian Kamau",
      initials: "BK",
      parentName: "Alice Kamau",
      parentEmail: "alice@safiri.co.ke",
      parentPhone: "0712345678",
      assignedBusPlate: "KDE 732X",
      assignedDriverName: "Erick Mwangi",
      pickupStop: "Kilimani Stop",
      dropoffStop: "St. Mary's Academy",
      status: "NOT_BOARDED",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  const adminDb = testEnv.authenticatedContext(ADMIN_CHIEF).firestore();
  await assertSucceeds(
    adminDb.collection("students").doc("st_alice_pending").update({
      assignedBusPlate: "KDE 732X",
      assignedDriverName: "Erick Mwangi",
      pickupStop: "Kilimani Gate",
      dropoffStop: "St. Mary's Academy",
      schoolName: "St. Mary's Academy",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("students").doc("st_alice_pending").update({
      assignedBusPlate: "KDE 119A",
      assignedDriverName: "John Kiprop",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("students").doc("st_alice_pending").update({
      dropoffStop: "Another school",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Shadow update & PII isolation: Bob cannot read Alice's profile, Alice cannot inject ghost fields or escalate role", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(PARENT_ALICE).set({
      userId: PARENT_ALICE,
      email: "alice@safiri.co.ke",
      displayName: "Alice Kamau",
      role: "PARENT",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  });

  const bobDb = testEnv.authenticatedContext(PARENT_BOB).firestore();
  await assertFails(bobDb.collection("users").doc(PARENT_ALICE).get());

  const aliceDb = testEnv.authenticatedContext(PARENT_ALICE).firestore();
  // Role escalation via update
  await assertFails(
    aliceDb.collection("users").doc(PARENT_ALICE).update({
      role: "ADMIN",
      updatedAt: serverTimestamp(),
    })
  );
  // Ghost field injection
  await assertFails(
    aliceDb.collection("users").doc(PARENT_ALICE).update({
      isVerified: true,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Email spoofing test: unverified admin email token is rejected", async () => {
  const spooferDb = testEnv.authenticatedContext("spoofer_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(spooferDb.collection("users").get());
  await assertFails(
    spooferDb.collection("alerts").doc("spoof_alert").set({
      authorUid: "spoofer_uid",
      title: "Fake Admin Alert",
      detail: "Spoofed broadcast",
      severity: "DANGER",
      status: "Ongoing",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

// --- 3. STUDENTS: CLIENT QUERY ALIGNMENT & ACTION-BASED UPDATES ---

test("Students: Parent can create/query own child, cannot run unfiltered list or mutate another parent's child", async () => {
  const aliceDb = testEnv.authenticatedContext(PARENT_ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(PARENT_BOB).firestore();
  const driverDb = testEnv.authenticatedContext(DRIVER_ERICK).firestore();
  const secondDriverDb = testEnv.authenticatedContext(DRIVER_JOHN).firestore();

  await assertSucceeds(
    aliceDb.collection("students").doc("st_alice_1").set({
      parentUid: PARENT_ALICE,
      name: "Brian Kamau",
      initials: "BK",
      parentName: "Alice Kamau",
      parentEmail: "alice@safiri.co.ke",
      parentPhone: "0712345678",
      assignedBusPlate: "",
      assignedDriverName: "",
      pickupStop: "Kilimani Stop",
      dropoffStop: "St. Mary's Academy",
      status: "NOT_BOARDED",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  const adminDb = testEnv.authenticatedContext(ADMIN_CHIEF).firestore();
  await assertSucceeds(
    adminDb.collection("students").doc("st_alice_1").update({
      assignedBusPlate: "KDE 732X",
      assignedDriverName: "Erick Mwangi",
      updatedAt: serverTimestamp(),
    })
  );

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("students").doc("st_bob_bus").set({
      parentUid: PARENT_BOB,
      name: "Mia Bob",
      initials: "MB",
      parentName: "Bob Kamau",
      parentEmail: "bob@safiri.co.ke",
      parentPhone: "0712345678",
      assignedBusPlate: "KDE 119A",
      assignedDriverName: "John Kiprop",
      pickupStop: "Westlands Stop",
      dropoffStop: "St. Mary's Academy",
      status: "NOT_BOARDED",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  });

  // Query aligned with rule
  await assertSucceeds(
    aliceDb.collection("students").where("parentUid", "==", PARENT_ALICE).get()
  );
  await assertSucceeds(
    driverDb.collection("students").where("assignedBusPlate", "==", "KDE 732X").get()
  );
  await assertSucceeds(
    secondDriverDb.collection("students").where("assignedBusPlate", "==", "KDE 119A").get()
  );
  await assertFails(driverDb.collection("students").get());
  await assertFails(driverDb.collection("students").doc("st_bob_bus").get());
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("drivers").doc(DRIVER_JOHN).update({ active: false });
  });
  await assertFails(
    secondDriverDb.collection("students").where("assignedBusPlate", "==", "KDE 119A").get()
  );

  // Unfiltered list query by parent MUST fail
  await assertFails(aliceDb.collection("students").get());

  // Cross-parent read/write fails
  await assertFails(bobDb.collection("students").doc("st_alice_1").get());
  await assertFails(
    bobDb.collection("students").doc("st_alice_1").update({
      parentNote: "Malicious edit",
      updatedAt: serverTimestamp(),
    })
  );

  // Parent cannot mark child BOARDED directly (Driver/Admin action only)
  await assertFails(
    aliceDb.collection("students").doc("st_alice_1").update({
      status: "BOARDED",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("students").doc("st_alice_1").update({
      pickupStop: "Unapproved stop",
      updatedAt: serverTimestamp(),
    })
  );

  // Parent CAN mark child ABSENT with note
  await assertSucceeds(
    aliceDb.collection("students").doc("st_alice_1").update({
      status: "ABSENT",
      absentReason: "Doctor appointment",
      parentNote: "Will resume tomorrow",
      updatedAt: serverTimestamp(),
    })
  );

  // Verified Driver CAN board child
  await assertSucceeds(
    driverDb.collection("students").doc("st_alice_1").update({
      status: "BOARDED",
      boardedTime: "7:15 AM",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    driverDb.collection("students").doc("st_bob_bus").update({
      status: "BOARDED",
      boardedTime: "7:15 AM",
      updatedAt: serverTimestamp(),
    })
  );
});

// --- 4. RESOURCE POISONING & OTHER COLLECTIONS ---

test("Resource poisoning: overly long or invalid document IDs are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(PARENT_ALICE).firestore();
  const oversizedId = "a".repeat(200);
  await assertFails(
    aliceDb.collection("community_incidents").doc(oversizedId).set({
      reporterUid: PARENT_ALICE,
      title: "Traffic delay",
      location: "Ngong Rd",
      category: "Traffic",
      votes: 1,
      isActive: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Messages, alerts, incidents & fleet buses enforce RBAC and bounded queries", async () => {
  const aliceDb = testEnv.authenticatedContext(PARENT_ALICE).firestore();
  const driverDb = testEnv.authenticatedContext(DRIVER_ERICK).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_CHIEF).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("students").doc("st_alice_message").set({
      parentUid: PARENT_ALICE,
      assignedBusPlate: "KDE 732X",
    });
    await db.collection("interportal_messages").doc("msg_other_bus").set({
      senderUid: PARENT_BOB,
      senderName: "Bob Kamau",
      senderRole: "PARENT",
      recipientRole: "DRIVER",
      targetBusPlate: "KDE 119A",
      content: "Message for the other bus",
      createdAt: serverTimestamp(),
    });
  });

  // Parent can post own PARENT interportal message
  await assertSucceeds(
    aliceDb.collection("interportal_messages").doc("msg_101").set({
      senderUid: PARENT_ALICE,
      senderName: "Alice Kamau",
      senderRole: "PARENT",
      recipientRole: "DRIVER",
      targetBusPlate: "KDE 732X",
      studentId: "st_alice_message",
      content: "Brian is ready at Kilimani gate.",
      displayTime: "7:10 AM",
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("interportal_messages").doc("msg_wrong_bus").set({
      senderUid: PARENT_ALICE,
      senderName: "Alice Kamau",
      senderRole: "PARENT",
      recipientRole: "DRIVER",
      targetBusPlate: "KDE 119A",
      studentId: "st_alice_message",
      content: "Attempt to message another bus",
      createdAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    driverDb.collection("interportal_messages")
      .where("targetBusPlate", "==", "KDE 732X")
      .get()
  );
  await assertFails(driverDb.collection("interportal_messages").get());
  await assertFails(driverDb.collection("interportal_messages").doc("msg_other_bus").get());

  await assertSucceeds(
    driverDb.collection("interportal_messages").doc("msg_driver_own_bus").set({
      senderUid: DRIVER_ERICK,
      senderName: "Erick Mwangi",
      senderRole: "DRIVER",
      recipientRole: "PARENT",
      targetBusPlate: "KDE 732X",
      content: "Bus is approaching the stop.",
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(
    driverDb.collection("interportal_messages").doc("msg_driver_other_bus").set({
      senderUid: DRIVER_ERICK,
      senderName: "Erick Mwangi",
      senderRole: "DRIVER",
      recipientRole: "PARENT",
      targetBusPlate: "KDE 119A",
      content: "Attempt to message another bus.",
      createdAt: serverTimestamp(),
    })
  );

  // Parent CANNOT impersonate DRIVER role in messages
  await assertFails(
    aliceDb.collection("interportal_messages").doc("msg_spoof").set({
      senderUid: PARENT_ALICE,
      senderName: "Fake Driver",
      senderRole: "DRIVER",
      recipientRole: "ALL",
      targetBusPlate: "KDE 732X",
      content: "Fake route cancel",
      createdAt: serverTimestamp(),
    })
  );

  // Parent CANNOT create official BusAlert (Driver/Admin only)
  await assertFails(
    aliceDb.collection("alerts").doc("alert_1").set({
      authorUid: PARENT_ALICE,
      title: "False SOS",
      detail: "Spoof",
      severity: "DANGER",
      status: "Ongoing",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Driver CAN create official SOS alert and Admin CAN resolve it
  await assertSucceeds(
    driverDb.collection("alerts").doc("alert_sos_1").set({
      authorUid: DRIVER_ERICK,
      title: "EMERGENCY SOS TRIGGERED",
      detail: "Tire puncture on Ngong Rd",
      severity: "DANGER",
      status: "Ongoing",
      busPlate: "KDE 732X",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    adminDb.collection("alerts").doc("alert_sos_1").update({
      status: "Resolved",
      updatedAt: serverTimestamp(),
    })
  );
  // Terminal status locking: resolved alert cannot be edited further even by driver
  await assertFails(
    driverDb.collection("alerts").doc("alert_sos_1").update({
      status: "Ongoing",
      updatedAt: serverTimestamp(),
    })
  );

  // Community incident + upvote (+1 only)
  await assertSucceeds(
    aliceDb.collection("community_incidents").doc("inc_101").set({
      reporterUid: PARENT_ALICE,
      title: "Rain slowing Ngong Rd",
      location: "Kilimani Junction",
      category: "Weather",
      votes: 1,
      isActive: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    driverDb.collection("community_incidents").doc("inc_101").update({
      votes: 2,
      updatedAt: serverTimestamp(),
    })
  );
  // Jumping votes +50 is rejected
  await assertFails(
    driverDb.collection("community_incidents").doc("inc_101").update({
      votes: 52,
      updatedAt: serverTimestamp(),
    })
  );

  // Fleet bus created by Admin, telemetry updated by assigned Driver only
  await assertSucceeds(
    adminDb.collection("fleet_buses").doc("bus_732x").set({
      plate: "KDE 732X",
      driverUid: DRIVER_ERICK,
      driverName: "Erick Mwangi",
      studentCount: 18,
      occupancyCount: 12,
      capacity: 22,
      status: "On Time",
      speed: 42,
      lat: -1.2921,
      lng: 36.8219,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    driverDb.collection("fleet_buses").doc("bus_732x").update({
      lat: -1.2935,
      lng: 36.8205,
      speed: 38,
      occupancyCount: 14,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("fleet_buses").doc("bus_732x").update({
      status: "Delayed",
      updatedAt: serverTimestamp(),
    })
  );
});
