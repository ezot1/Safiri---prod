const assert = require("node:assert/strict");
const { test } = require("node:test");
const { createActiveAlertMessage } = require("../src/alert-message");

test("active alert pushes contain generic text and no alert details", () => {
  const message = createActiveAlertMessage("alert_123", {
    status: "Ongoing",
    severity: "DANGER",
    title: "Sensitive route title",
    detail: "Sensitive student or route details",
  });

  assert.deepEqual(message, {
    notification: {
      title: "Safiri bus alert",
      body: "An active bus alert is available. Open Safiri for details.",
    },
    data: {
      type: "bus_alert",
      alertId: "alert_123",
      severity: "DANGER",
    },
    android: {
      priority: "high",
      notification: {
        channelId: "safiri_emergency_sos",
        tag: "alert_123",
      },
    },
  });
  assert.equal(JSON.stringify(message).includes("Sensitive"), false);
});

test("resolved alerts do not produce a push message", () => {
  assert.equal(
    createActiveAlertMessage("alert_456", {
      status: "Resolved",
      severity: "INFO",
    }),
    null
  );
});
