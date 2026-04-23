# Implementation Backlog (Execution Order)

## Phase 1: Data Layer
1. Add Room entities/DAO for `Flat`, `FlatUsage`, `TenantBalance`.
2. Update existing entities:
- `Tenant` add `flatId`.
- `TenantMonthlyCharge` add `adjustmentAmount`.
- `PaymentRecord.paidOn` required.
3. Create migrations with seed/backfill logic.
4. Add DAO query methods for month-level joins and aggregates.

## Phase 2: Domain/Service Layer
1. Implement `BillingMonthService.computeMonthCharges` with deterministic rounding reconciliation.
2. Implement `PaymentService.recordPayment` + `paidOn` validation.
3. Implement `PaymentService.recomputeTenantBalance`.
4. Implement `CarryForwardService.applyCarryForward`.
5. Implement month recompute-after-edit with confirmation gate at use-case boundary.

## Phase 3: UI
1. Flats screen: CRUD + fixed rent.
2. Tenant screen: flat linkage validation.
3. Monthly Billing screen: electricity bill + per-flat readings + recompute/finalize actions.
4. Payment form: date picker required, block save when missing.
5. Dashboard screen: expected/paid/pending and unpaid tenant list.

## Phase 4: Tests
1. Unit tests
- split + rounding,
- total due formula,
- carry-forward math.
2. Integration tests
- full month lifecycle,
- edit-after-payment recompute,
- tenant flat reassignment.
3. UI tests
- payment date required,
- monthly reading entry,
- dashboard refresh.

## Definition of Done
- All acceptance criteria in `requirements.md` pass.
- Migration from older schema validated on seeded sample DB.
- No payment can be saved without `paidOn`.
- Recompute behavior preserves payment history and updates balances deterministically.
