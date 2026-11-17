const { getApp, getApps, initializeApp } = require("firebase-admin/app");
const {
  FieldValue,
  Timestamp,
  getFirestore,
} = require("firebase-admin/firestore");
const { defineSecret, defineString } = require("firebase-functions/params");
const { HttpsError, onCall, onRequest } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const logger = require("firebase-functions/logger");
const {
  isSuccessfulStkQuery,
  isTerminalStkFailure,
  normalizeKenyanPhone,
} = require("./payment-utils");

const REGION = "us-central1";
const SUBSCRIPTION_ID = "main";
const DEFAULT_MONTHLY_AMOUNT_KSH = 15000;
const BILLING_PERIOD_DAYS = 30;
const INITIATION_TIMEOUT_MS = 5 * 60_000;
const PAYMENT_ID_PATTERN = /^[a-f0-9-]{36}$/i;

const mpesaConsumerKey = defineSecret("MPESA_CONSUMER_KEY");
const mpesaConsumerSecret = defineSecret("MPESA_CONSUMER_SECRET");
const mpesaShortcode = defineSecret("MPESA_SHORTCODE");
const mpesaPasskey = defineSecret("MPESA_PASSKEY");
const mpesaEnvironment = defineString("MPESA_ENVIRONMENT", {
  default: "sandbox",
});
const mpesaCallbackUrl = defineString("MPESA_CALLBACK_URL");

if (getApps().length === 0) initializeApp();
const database = getFirestore(
  getApp(),
  "ai-studio-android-safiri-50dfd15d-a4b4-46df-a41c-15b322f89f7e",
);
const subscriptionRef = database.collection("subscriptions").doc(SUBSCRIPTION_ID);
const invoiceCollection = subscriptionRef.collection("invoices");

const darajaSecrets = [
  mpesaConsumerKey,
  mpesaConsumerSecret,
  mpesaShortcode,
  mpesaPasskey,
];

function assertAdmin(request) {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in as a school administrator.");
  }
  const { uid, token } = request.auth;
  return database
    .collection("admins")
    .doc(uid)
    .get()
    .then((admin) => {
      if (
        !admin.exists &&
        !(
          token.email === "info@veritasglobaladvisory.org" &&
          token.email_verified === true
        )
      ) {
        throw new HttpsError(
          "permission-denied",
          "Only an authorized school administrator can manage billing.",
        );
      }
    });
}

function darajaBaseUrl() {
  const environment = mpesaEnvironment.value().trim().toLowerCase();
  if (environment === "sandbox") return "https://sandbox.safaricom.co.ke";
  if (environment === "production") return "https://api.safaricom.co.ke";
  throw new HttpsError(
    "failed-precondition",
    "M-Pesa environment must be sandbox or production.",
  );
}

function currentKenyaTimestamp() {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Africa/Nairobi",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date());
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]));
  return `${values.year}${values.month}${values.day}${values.hour}${values.minute}${values.second}`;
}

function stkPassword(shortcode, passkey, timestamp) {
  return Buffer.from(`${shortcode}${passkey}${timestamp}`).toString("base64");
}

function unknownPaymentOutcomeError(cause) {
  const error = new Error("M-Pesa request outcome is unknown.", { cause });
  error.paymentOutcomeUnknown = true;
  return error;
}

async function postDarajaJson(path, payload) {
  const baseUrl = darajaBaseUrl();
  const credentials = Buffer.from(
    `${mpesaConsumerKey.value()}:${mpesaConsumerSecret.value()}`,
  ).toString("base64");
  const tokenResponse = await fetch(
    `${baseUrl}/oauth/v1/generate?grant_type=client_credentials`,
    {
      headers: { Authorization: `Basic ${credentials}` },
      signal: AbortSignal.timeout(15_000),
    },
  );
  const tokenBody = await tokenResponse.json();
  if (!tokenResponse.ok || typeof tokenBody.access_token !== "string") {
    logger.error("M-Pesa OAuth request failed", {
      httpStatus: tokenResponse.status,
    });
    throw new HttpsError(
      "unavailable",
      "M-Pesa is temporarily unavailable. Please try again.",
    );
  }

  let response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${tokenBody.access_token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
      signal: AbortSignal.timeout(20_000),
    });
  } catch (error) {
    if (path === "/mpesa/stkpush/v1/processrequest") {
      throw unknownPaymentOutcomeError(error);
    }
    throw error;
  }
  let body;
  try {
    body = await response.json();
  } catch (error) {
    if (path === "/mpesa/stkpush/v1/processrequest" && response.ok) {
      throw unknownPaymentOutcomeError(error);
    }
    throw error;
  }
  if (!response.ok) {
    logger.error("M-Pesa API request failed", {
      path,
      httpStatus: response.status,
      responseCode: body?.ResponseCode,
    });
    const apiError = new HttpsError(
      "unavailable",
      "M-Pesa could not process the request. Please try again.",
    );
    if (
      path === "/mpesa/stkpush/v1/processrequest" &&
      response.status >= 500
    ) {
      apiError.paymentOutcomeUnknown = true;
    }
    throw apiError;
  }
  return body;
}

function getCallbackUrl() {
  let url;
  try {
    url = new URL(mpesaCallbackUrl.value());
  } catch {
    throw new HttpsError(
      "failed-precondition",
      "The M-Pesa callback URL is not configured.",
    );
  }
  if (url.protocol !== "https:") {
    throw new HttpsError(
      "failed-precondition",
      "The M-Pesa callback URL must use HTTPS.",
    );
  }
  return url.toString();
}

function getInvoiceRef(invoiceId) {
  if (typeof invoiceId !== "string" || !PAYMENT_ID_PATTERN.test(invoiceId)) {
    throw new HttpsError("invalid-argument", "Invalid invoice reference.");
  }
  return invoiceCollection.doc(invoiceId);
}

async function queryStkStatus(invoice) {
  if (!invoice.checkoutRequestId) return null;
  const shortcode = mpesaShortcode.value();
  const timestamp = currentKenyaTimestamp();
  return postDarajaJson("/mpesa/stkpushquery/v1/query", {
    BusinessShortCode: shortcode,
    Password: stkPassword(shortcode, mpesaPasskey.value(), timestamp),
    Timestamp: timestamp,
    CheckoutRequestID: invoice.checkoutRequestId,
  });
}

async function settleSuccessfulPayment(invoiceRef) {
  return database.runTransaction(async (transaction) => {
    const [invoiceSnapshot, subscriptionSnapshot] = await Promise.all([
      transaction.get(invoiceRef),
      transaction.get(subscriptionRef),
    ]);
    if (!invoiceSnapshot.exists) return false;
    const invoice = invoiceSnapshot.data();
    if (invoice.status === "PAID") return false;

    const now = new Date();
    const currentPeriodEnd = subscriptionSnapshot.data()?.currentPeriodEnd?.toDate();
    const periodStart =
      currentPeriodEnd && currentPeriodEnd > now ? currentPeriodEnd : now;
    const periodEnd = new Date(periodStart);
    periodEnd.setUTCDate(periodEnd.getUTCDate() + BILLING_PERIOD_DAYS);

    transaction.update(invoiceRef, {
      status: "PAID",
      receiptNumber: "Verified by Safaricom",
      paidAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    const subscriptionUpdate = {
      status: "ACTIVE",
      planName: "Safiri Fleet Enterprise Tier",
      monthlyAmountKsh: invoice.amountKsh,
      currentPeriodStart: Timestamp.fromDate(periodStart),
      currentPeriodEnd: Timestamp.fromDate(periodEnd),
      updatedAt: FieldValue.serverTimestamp(),
    };
    if (subscriptionSnapshot.data()?.pendingPaymentId === invoiceRef.id) {
      subscriptionUpdate.pendingPaymentId = FieldValue.delete();
      subscriptionUpdate.pendingPaymentStartedAt = FieldValue.delete();
    }
    transaction.set(subscriptionRef, subscriptionUpdate, { merge: true });
    return true;
  });
}

async function markPaymentFailed(invoiceRef, resultDescription) {
  return database.runTransaction(async (transaction) => {
    const [invoiceSnapshot, subscriptionSnapshot] = await Promise.all([
      transaction.get(invoiceRef),
      transaction.get(subscriptionRef),
    ]);
    if (!invoiceSnapshot.exists || invoiceSnapshot.data().status === "PAID") {
      return false;
    }
    transaction.update(invoiceRef, {
      status: "FAILED",
      failureReason:
        typeof resultDescription === "string"
          ? resultDescription.slice(0, 200)
          : "M-Pesa payment was not completed.",
      updatedAt: FieldValue.serverTimestamp(),
    });
    if (subscriptionSnapshot.data()?.pendingPaymentId === invoiceRef.id) {
      transaction.set(
        subscriptionRef,
        {
          pendingPaymentId: FieldValue.delete(),
          pendingPaymentStartedAt: FieldValue.delete(),
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
    }
    return true;
  });
}

async function markPaymentInitiationUnknown(invoiceRef) {
  await invoiceRef.update({
    status: "INITIATION_UNKNOWN",
    failureReason: "Check with Safiri support before submitting another payment.",
    updatedAt: FieldValue.serverTimestamp(),
  });
}

async function verifyPayment(invoiceRef, invoice) {
  const query = await queryStkStatus(invoice);
  if (isSuccessfulStkQuery(query, invoice.checkoutRequestId)) {
    await settleSuccessfulPayment(invoiceRef);
    return "PAID";
  }
  if (isTerminalStkFailure(query, invoice.checkoutRequestId)) {
    await markPaymentFailed(invoiceRef, query.ResultDesc);
    return "FAILED";
  }
  return invoice.status;
}

exports.startSubscriptionPayment = onCall(
  {
    region: REGION,
    enforceAppCheck: true,
    secrets: darajaSecrets,
    timeoutSeconds: 60,
  },
  async (request) => {
    await assertAdmin(request);
    const phoneNumber = normalizeKenyanPhone(request.data?.phoneNumber);
    if (!phoneNumber) {
      throw new HttpsError(
        "invalid-argument",
        "Enter a valid Kenyan M-Pesa number.",
      );
    }
    const invoiceId = request.data?.requestId;
    const invoiceRef = getInvoiceRef(invoiceId);
    const callbackUrl = getCallbackUrl();

    let invoice;
    let shouldInitiate = false;
    await database.runTransaction(async (transaction) => {
      shouldInitiate = false;
      const [subscriptionSnapshot, invoiceSnapshot] = await Promise.all([
        transaction.get(subscriptionRef),
        transaction.get(invoiceRef),
      ]);
      if (invoiceSnapshot.exists) {
        invoice = invoiceSnapshot.data();
        if (invoice.createdByUid !== request.auth.uid) {
          throw new HttpsError(
            "permission-denied",
            "This payment request belongs to another administrator.",
          );
        }
        return;
      }

      const subscription = subscriptionSnapshot.data() ?? {};
      const now = new Date();
      if (subscription.pendingPaymentId) {
        throw new HttpsError(
          "failed-precondition",
          "A payment request is already in progress. Check the invoice status before retrying.",
        );
      }

      const amountKsh = Number(
        subscription.monthlyAmountKsh ?? DEFAULT_MONTHLY_AMOUNT_KSH,
      );
      if (!Number.isInteger(amountKsh) || amountKsh < 1 || amountKsh > 250_000) {
        throw new HttpsError(
          "failed-precondition",
          "The subscription amount is not configured correctly.",
        );
      }
      invoice = {
        amountKsh,
        currency: "KES",
        createdByUid: request.auth.uid,
        phoneNumber,
        status: "INITIATING",
      };
      shouldInitiate = true;
      transaction.set(invoiceRef, {
        ...invoice,
        paymentMethod: "M-Pesa STK",
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });
      transaction.set(
        subscriptionRef,
        {
          status: subscription.status ?? "INACTIVE",
          planName: subscription.planName ?? "Safiri Fleet Enterprise Tier",
          monthlyAmountKsh: amountKsh,
          pendingPaymentId: invoiceId,
          pendingPaymentStartedAt: Timestamp.fromDate(now),
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
    });

    if (!shouldInitiate) {
      if (invoice.status === "PENDING" || invoice.status === "PAID") {
        return {
          invoiceId,
          status: invoice.status,
          message:
            invoice.status === "PAID"
              ? "This invoice has already been paid."
              : "Approve the M-Pesa prompt on your phone.",
        };
      }
      throw new HttpsError(
        "failed-precondition",
        "This payment request has already been submitted. Refresh its invoice instead of retrying it.",
      );
    }
    if (invoice.status !== "INITIATING") {
      throw new HttpsError(
        "failed-precondition",
        "This payment request cannot be reused. Submit a new payment request.",
      );
    }

    try {
      const shortcode = mpesaShortcode.value();
      const timestamp = currentKenyaTimestamp();
      const accountReference = `SAF${invoiceId.replaceAll("-", "").slice(0, 9)}`;
      const response = await postDarajaJson(
        "/mpesa/stkpush/v1/processrequest",
        {
          BusinessShortCode: shortcode,
          Password: stkPassword(shortcode, mpesaPasskey.value(), timestamp),
          Timestamp: timestamp,
          TransactionType: "CustomerPayBillOnline",
          Amount: invoice.amountKsh,
          PartyA: phoneNumber,
          PartyB: shortcode,
          PhoneNumber: phoneNumber,
          CallBackURL: callbackUrl,
          AccountReference: accountReference,
          TransactionDesc: "Safiri fee",
        },
      );
      const responseCode = response?.ResponseCode;
      if (responseCode != null && String(responseCode) !== "0") {
        logger.error("M-Pesa did not accept the STK request", {
          responseCode,
        });
        throw new HttpsError(
          "unavailable",
          "M-Pesa did not accept the payment request. Please try again.",
        );
      }
      if (
        String(responseCode) !== "0" ||
        typeof response?.CheckoutRequestID !== "string" ||
        typeof response?.MerchantRequestID !== "string"
      ) {
        throw unknownPaymentOutcomeError(
          new Error("M-Pesa returned incomplete checkout details."),
        );
      }
      try {
        await invoiceRef.update({
          status: "PENDING",
          checkoutRequestId: response.CheckoutRequestID,
          merchantRequestId: response.MerchantRequestID,
          updatedAt: FieldValue.serverTimestamp(),
        });
      } catch (error) {
        throw unknownPaymentOutcomeError(error);
      }
      return {
        invoiceId,
        status: "PENDING",
        message: "Approve the M-Pesa prompt on your phone.",
      };
    } catch (error) {
      if (error?.paymentOutcomeUnknown) {
        await markPaymentInitiationUnknown(invoiceRef);
        logger.error("M-Pesa STK request outcome is unknown", {
          invoiceId,
          errorCode: error?.name ?? "unknown",
        });
        throw new HttpsError(
          "unavailable",
          "M-Pesa may have received this request. Do not retry; contact Safiri support and provide the invoice ID.",
          { invoiceId },
        );
      }
      await markPaymentFailed(invoiceRef, "Could not start the M-Pesa prompt.");
      if (error instanceof HttpsError) throw error;
      logger.error("Could not initiate the M-Pesa STK payment", {
        invoiceId,
        errorCode: error?.name ?? "unknown",
      });
      throw new HttpsError(
        "unavailable",
        "Could not start the M-Pesa prompt. Please try again.",
      );
    }
  },
);

exports.refreshSubscriptionPayment = onCall(
  {
    region: REGION,
    enforceAppCheck: true,
    secrets: darajaSecrets,
    timeoutSeconds: 60,
  },
  async (request) => {
    await assertAdmin(request);
    const invoiceRef = getInvoiceRef(request.data?.invoiceId);
    const invoiceSnapshot = await invoiceRef.get();
    if (!invoiceSnapshot.exists) {
      throw new HttpsError("not-found", "Payment invoice was not found.");
    }
    const invoice = invoiceSnapshot.data();
    if (invoice.status === "PAID") return { status: "PAID" };
    if (!invoice.checkoutRequestId) {
      const createdAt = invoice.createdAt?.toDate();
      if (
        invoice.status === "INITIATING" &&
        createdAt &&
        Date.now() - createdAt.getTime() >= INITIATION_TIMEOUT_MS
      ) {
        await markPaymentInitiationUnknown(invoiceRef);
        return { status: "INITIATION_UNKNOWN" };
      }
      return { status: invoice.status };
    }

    try {
      const status = await verifyPayment(invoiceRef, invoice);
      return { status };
    } catch (error) {
      logger.error("Could not refresh M-Pesa payment status", {
        invoiceId: invoiceRef.id,
        errorCode: error?.name ?? "unknown",
      });
      throw new HttpsError(
        "unavailable",
        "Could not check the payment status. Please try again.",
      );
    }
  },
);

exports.mpesaPaymentCallback = onRequest(
  {
    region: REGION,
    invoker: "public",
    secrets: darajaSecrets,
    timeoutSeconds: 60,
  },
  async (request, response) => {
    if (request.method !== "POST") {
      response.status(405).send("Method not allowed.");
      return;
    }
    const callback = request.body?.Body?.stkCallback;
    const checkoutRequestId = callback?.CheckoutRequestID;
    if (
      typeof checkoutRequestId !== "string" ||
      checkoutRequestId.length > 128
    ) {
      response.status(400).send("Invalid M-Pesa callback.");
      return;
    }

    try {
      const invoiceSnapshot = await invoiceCollection
        .where("checkoutRequestId", "==", checkoutRequestId)
        .limit(1)
        .get();
      const invoiceDocument = invoiceSnapshot.docs[0];
      if (!invoiceDocument) {
        logger.warn("Received an M-Pesa callback for an unknown checkout", {
          checkoutRequestId,
        });
        response.status(200).json({ ResultCode: 0, ResultDesc: "Accepted" });
        return;
      }
      const invoice = invoiceDocument.data();
      if (
        callback.MerchantRequestID &&
        invoice.merchantRequestId !== callback.MerchantRequestID
      ) {
        logger.warn("M-Pesa callback merchant request ID did not match", {
          invoiceId: invoiceDocument.id,
        });
        response.status(200).json({ ResultCode: 0, ResultDesc: "Accepted" });
        return;
      }
      if (invoice.status === "PAID") {
        response.status(200).json({ ResultCode: 0, ResultDesc: "Accepted" });
        return;
      }

      const status = await verifyPayment(invoiceDocument.ref, {
        ...invoice,
      });
      logger.info("M-Pesa callback verified", {
        invoiceId: invoiceDocument.id,
        status,
        callbackResultCode: callback.ResultCode,
      });
      response.status(200).json({ ResultCode: 0, ResultDesc: "Accepted" });
    } catch (error) {
      logger.error("M-Pesa callback verification failed", {
        checkoutRequestId,
        errorCode: error?.name ?? "unknown",
      });
      response.status(500).json({ ResultCode: 1, ResultDesc: "Retry later" });
    }
  },
);

exports.updateSubscriptionExpiry = onSchedule(
  {
    schedule: "every 6 hours",
    region: REGION,
  },
  async () => {
    const snapshot = await subscriptionRef.get();
    const periodEnd = snapshot.data()?.currentPeriodEnd?.toDate();
    if (!snapshot.exists || !periodEnd) return;

    const now = Date.now();
    const status =
      periodEnd.getTime() <= now
        ? "INACTIVE"
        : periodEnd.getTime() <= now + 7 * 24 * 60 * 60 * 1000
          ? "PAYMENT_DUE"
          : "ACTIVE";
    if (snapshot.data().status !== status) {
      await subscriptionRef.update({
        status,
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
  },
);
