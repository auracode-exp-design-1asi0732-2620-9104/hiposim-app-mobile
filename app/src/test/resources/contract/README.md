# API contract examples

JSON copied from `docs/api-contract/openapi.v0.yaml` in the backend repository (`hiposim-backEnd`), v0.1.0:

- `simulation-response-with-bonus.json`: 200 example `withBonus` of `POST /api/simulations` (the schedule has 3 of the
  240 rows, as in the contract).
- `problem-down-payment-too-low.json`: 400 example `downPaymentTooLow`.

The mobile tests parse them, so a change in the contract that breaks the app shows up here. Update them whenever the
contract changes.
