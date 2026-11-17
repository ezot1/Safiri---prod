function normalizeKenyanPhone(value) {
  if (typeof value !== "string") return null;
  const phone = value.replace(/[\s-]/g, "");
  if (/^0[17]\d{8}$/.test(phone)) return `254${phone.slice(1)}`;
  if (/^\+254[17]\d{8}$/.test(phone)) return phone.slice(1);
  if (/^254[17]\d{8}$/.test(phone)) return phone;
  return null;
}

function isSuccessfulStkQuery(response, expectedCheckoutRequestId) {
  return (
    String(response?.ResponseCode) === "0" &&
    String(response?.ResultCode) === "0" &&
    (!response.CheckoutRequestID ||
      response.CheckoutRequestID === expectedCheckoutRequestId)
  );
}

function isTerminalStkFailure(response, expectedCheckoutRequestId) {
  return (
    String(response?.ResponseCode) === "0" &&
    response.ResultCode !== undefined &&
    String(response.ResultCode) !== "0" &&
    (!response.CheckoutRequestID ||
      response.CheckoutRequestID === expectedCheckoutRequestId)
  );
}

module.exports = {
  isSuccessfulStkQuery,
  isTerminalStkFailure,
  normalizeKenyanPhone,
};
