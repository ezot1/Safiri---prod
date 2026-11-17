const { getApp, initializeApp, getApps } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const { createActiveAlertMessage } = require("./alert-message");
const {
  mpesaPaymentCallback,
  refreshSubscriptionPayment,
  startSubscriptionPayment,
  updateSubscriptionExpiry,
} = require("./subscription-payments");

const DATABASE_ID =
  "ai-studio-android-safiri-50dfd15d-a4b4-46df-a41c-15b322f89f7e";
const FCM_BATCH_SIZE = 500;

if (getApps().length === 0) {
  initializeApp();
}
const database = getFirestore(getApp(), DATABASE_ID);

exports.notifyOnActiveAlert = onDocumentCreated(
  {
    document: "alerts/{alertId}",
    database: DATABASE_ID,
    region: "us-central1",
    maxInstances: 10,
    retry: true,
  },
  async (event) => {
    const alert = event.data?.data();
    const message = createActiveAlertMessage(event.params.alertId, alert);
    if (!message) return;

    const tokenSnapshot = await database.collectionGroup("device_tokens").get();
    const tokenReferences = new Map();
    for (const tokenDocument of tokenSnapshot.docs) {
      const token = tokenDocument.get("token");
      if (typeof token !== "string" || token.length === 0) continue;
      const references = tokenReferences.get(token) ?? [];
      references.push(tokenDocument.ref);
      tokenReferences.set(token, references);
    }

    const tokens = [...tokenReferences.keys()];
    if (tokens.length === 0) {
      logger.info("No registered devices for active bus alert", {
        alertId: event.params.alertId,
      });
      return;
    }

    const failedTokenReferences = [];
    let hasTransientFailure = false;
    for (let offset = 0; offset < tokens.length; offset += FCM_BATCH_SIZE) {
      const batchTokens = tokens.slice(offset, offset + FCM_BATCH_SIZE);
      const response = await getMessaging().sendEachForMulticast({
        tokens: batchTokens,
        ...message,
      });

      response.responses.forEach((result, index) => {
        if (result.success) return;
        const code = result.error?.code;
        if (
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token"
        ) {
          failedTokenReferences.push(...tokenReferences.get(batchTokens[index]));
        } else {
          hasTransientFailure = true;
          logger.error("FCM delivery failed for an active bus alert", {
            alertId: event.params.alertId,
            errorCode: code ?? "unknown",
          });
        }
      });

    }

    for (
      let offset = 0;
      offset < failedTokenReferences.length;
      offset += FCM_BATCH_SIZE
    ) {
      const batch = database.batch();
      failedTokenReferences
        .slice(offset, offset + FCM_BATCH_SIZE)
        .forEach((reference) => batch.delete(reference));
      await batch.commit();
    }

    if (hasTransientFailure) {
      throw new Error("One or more active bus alert notifications could not be delivered.");
    }

    logger.info("Delivered active bus alert notification", {
      alertId: event.params.alertId,
      deviceCount: tokens.length,
      invalidTokenCount: failedTokenReferences.length,
    });
  },
);

exports.mpesaPaymentCallback = mpesaPaymentCallback;
exports.refreshSubscriptionPayment = refreshSubscriptionPayment;
exports.startSubscriptionPayment = startSubscriptionPayment;
exports.updateSubscriptionExpiry = updateSubscriptionExpiry;
