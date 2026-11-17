const ACTIVE_ALERT_STATUSES = new Set(["Ongoing", "Upcoming", "Scheduled"]);

function createActiveAlertMessage(alertId, alert) {
  if (!alert || !ACTIVE_ALERT_STATUSES.has(alert.status)) return null;

  const severity = typeof alert.severity === "string" ? alert.severity : "INFO";
  return {
    notification: {
      title: "Safiri bus alert",
      body: "An active bus alert is available. Open Safiri for details.",
    },
    data: {
      type: "bus_alert",
      alertId,
      severity,
    },
    android: {
      priority: "high",
      notification: {
        channelId:
          severity === "DANGER"
            ? "safiri_emergency_sos"
            : "safiri_boarding_alerts",
        tag: alertId,
      },
    },
  };
}

module.exports = { createActiveAlertMessage };
