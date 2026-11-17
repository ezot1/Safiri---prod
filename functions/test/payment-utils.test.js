const assert = require("node:assert/strict");
const { test } = require("node:test");
const {
  isSuccessfulStkQuery,
  isTerminalStkFailure,
  normalizeKenyanPhone,
} = require("../src/payment-utils");

test("Kenyan M-Pesa numbers normalize to the 254 international format", () => {
  assert.equal(normalizeKenyanPhone("0712345678"), "254712345678");
  assert.equal(normalizeKenyanPhone("+254112345678"), "254112345678");
  assert.equal(normalizeKenyanPhone("254712345678"), "254712345678");
  assert.equal(normalizeKenyanPhone("12345"), null);
});

test("an STK query is successful only for the expected checkout request", () => {
  assert.equal(
    isSuccessfulStkQuery(
      { ResponseCode: "0", ResultCode: 0, CheckoutRequestID: "ws_CO_1" },
      "ws_CO_1"
    ),
    true
  );
  assert.equal(
    isSuccessfulStkQuery(
      { ResponseCode: "0", ResultCode: 0, CheckoutRequestID: "ws_CO_2" },
      "ws_CO_1"
    ),
    false
  );
  assert.equal(
    isSuccessfulStkQuery(
      { ResponseCode: "0", ResultCode: 1032, CheckoutRequestID: "ws_CO_1" },
      "ws_CO_1"
    ),
    false
  );
});

test("only a definitive nonzero STK result is treated as payment failure", () => {
  assert.equal(
    isTerminalStkFailure(
      { ResponseCode: "0", ResultCode: "1032", CheckoutRequestID: "ws_CO_1" },
      "ws_CO_1"
    ),
    true
  );
  assert.equal(
    isTerminalStkFailure(
      { ResponseCode: "1", ResultCode: "1032", CheckoutRequestID: "ws_CO_1" },
      "ws_CO_1"
    ),
    false
  );
  assert.equal(
    isTerminalStkFailure(
      { ResultCode: "1032", CheckoutRequestID: "ws_CO_1" },
      "ws_CO_1"
    ),
    false
  );
  assert.equal(
    isTerminalStkFailure(
      { ResponseCode: "0", ResultCode: "1032", CheckoutRequestID: "ws_CO_2" },
      "ws_CO_1"
    ),
    false
  );
});
