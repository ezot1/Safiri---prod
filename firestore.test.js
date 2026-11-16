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

  await assertSucceeds(
    aliceDb.collection("students").doc("st_alice_1").set({
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

  // Query aligned with rule
  await assertSucceeds(
    aliceDb.collection("students").where("parentUid", "==", PARENT_ALICE).get()
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

  // Parent can post own PARENT interportal message
  await assertSucceeds(
    aliceDb.collection("interportal_messages").doc("msg_101").set({
      senderUid: PARENT_ALICE,
      senderName: "Alice Kamau",
      senderRole: "PARENT",
      recipientRole: "DRIVER",
      targetBusPlate: "KDE 732X",
      content: "Brian is ready at Kilimani gate.",
      displayTime: "7:10 AM",
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
