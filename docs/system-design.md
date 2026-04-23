# System Design: Flat-Based Rent + Reading + Tenant Adjustment

## 1. Objective
Implement a deterministic monthly ledger where:
- flats are billing sources (`fixedMonthlyRent`, monthly units),
- tenants are payment actors (manual payments with required date),
- tenant over/under payments automatically carry into next cycle.

## 2. Architecture
- Platform: Android (Kotlin)
- UI: Jetpack Compose
- Local storage: Room (single-tenant local DB)
- Layers: UI -> ViewModel -> UseCase/Service -> Repository -> DAO/Room

## 3. Core Domain Flow
1. Landlord sets up `Flat` and `Tenant` (tenant linked to flat).
2. Landlord creates `BillingMonth` in `Draft`.
3. Landlord enters electricity bill and per-flat units.
4. System computes per-flat electricity share and snapshots `TenantMonthlyCharge`.
5. Landlord records one or more payments (`PaymentRecord`) with mandatory `paidOn`.
6. System recomputes due/paid/pending and stores `TenantBalance`.
7. Next month reads prior `TenantBalance` and applies `adjustmentAmount`.

## 4. Monthly Computation Rules
- `flatShareRaw = (flatUnits / totalUnits) * electricityBill`
- Round to 2 decimals for each line.
- Reconcile rounding by applying final delta to last eligible line.
- `totalDue = rentAmount + electricityShare - adjustmentAmount`
  - Positive `adjustmentAmount` means credit reducing due.
  - Negative effective carry is represented by negative balance and reflected in adjustment application logic.

## 5. Validation Rules
- Active tenant must reference active flat.
- Cannot finalize month if `sum(flatUnits) == 0`.
- Cannot save payment if `paidOn` missing.
- Amount fields must be non-negative decimals.

## 6. Recompute Behavior
When bill/readings are changed after payments exist:
- Payment rows remain immutable.
- Charges are recomputed.
- Month/tenant balances are refreshed from immutable payments vs new due.
- UI requires confirmation before recompute.

## 7. Tenant Flat Reassignment
- Reassignment affects future month charge generation.
- Existing finalized month snapshots remain unchanged.
- Running balance remains tenant-owned, not flat-owned.

## 8. Dashboard Aggregation
For selected month:
- `totalExpected = sum(totalDue)`
- `totalPaid = sum(payment.amountPaid)`
- `totalPending = totalExpected - totalPaid`
- `paidTenantCount = tenants where pending <= 0`
- `unpaidList = tenants where pending > 0`
