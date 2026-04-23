# Service Contracts (Application Layer)

## 1. BillingMonthService

### `createBillingMonth(monthId: String)`
- Precondition: month does not exist.
- Output: month in `Draft`.

### `setElectricityBill(monthId: String, totalAmount: BigDecimal)`
- Precondition: amount >= 0.
- Effect: updates `billing_months.electricity_total_amount`.

### `upsertFlatUsage(monthId: String, flatId: String, units: BigDecimal)`
- Precondition: units >= 0.
- Effect: insert/update flat usage row.

### `computeMonthCharges(monthId: String)`
- Preconditions:
  - month exists,
  - active tenant <-> active flat mapping valid,
  - `sum(units)` > 0 for finalizable computation.
- Effect:
  - computes electricity shares,
  - snapshots rent per tenant,
  - applies carry-forward adjustment,
  - writes `tenant_monthly_charges`.

### `finalizeMonth(monthId: String)`
- Preconditions:
  - month in Draft,
  - charges computed,
  - no blocking validation errors.
- Effect: status set to `Finalized`.

## 2. PaymentService

### `recordPayment(input: RecordPaymentInput)`
- Input: `tenantId`, `billingMonthId`, `component`, `amountPaid`, `paidOn`, `note?`
- Preconditions:
  - `paidOn` required,
  - `amountPaid > 0`.
- Effect: append immutable payment record.

### `recomputeTenantBalance(monthId: String)`
- Effect:
  - sums due vs paid per tenant,
  - writes `tenant_balances` for `as_of_month_id = monthId`.

## 3. CarryForwardService

### `applyCarryForward(nextMonthId: String)`
- Reads previous month `tenant_balances`.
- Maps balance into `tenant_monthly_charges.adjustment_amount` for next month.

## 4. DashboardQueryService

### `getMonthSummary(monthId: String): MonthSummary`
- Returns total expected, paid, pending, paidTenantCount, totalTenantCount, unpaid list.

## 5. Error Contract
- `ValidationError(code, message, field?)`
- Codes:
  - `PAYMENT_DATE_REQUIRED`
  - `ZERO_TOTAL_UNITS`
  - `INVALID_FLAT_TENANT_LINK`
  - `MONTH_NOT_DRAFT`
  - `MISSING_MONTH_CHARGES`
