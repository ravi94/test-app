# PRD: Android Flat Rent & Electricity Tracker (V1)

## Summary
Build an Android app for a single landlord to register flats and tenants, track monthly flat rent payments, enter monthly unit readings and common electricity bill, allocate electricity cost, and monitor paid/unpaid status from one dashboard.

V1 will be landlord-only and offline-first local (no tenant login in V1).

## Goals
- Maintain a flat registry with fixed monthly rent per flat.
- Maintain a tenant registry mapped to flats.
- Track monthly rent payment status.
- Enter one common monthly electricity bill.
- Enter monthly unit readings per flat.
- Automatically split electricity amount proportionally by usage.
- Maintain per-tenant running balance (credit/debit) and apply adjustment in next cycle.
- Record manual payment date for each payment record.
- Show monthly total due, paid amount, and balance from dashboard.

## Non-Goals (V1)
- Tenant login or tenant self-service portal.
- Automatic bank reconciliation.
- Smart meter integrations.
- PDF invoicing or CSV export.
- Multi-landlord teams and role-based access.

## Target Users
- Primary: single landlord managing multiple flats and tenants in one property.
- Secondary (future): tenants (view/pay/acknowledge).

## Core User Stories
1. As a landlord, I can create/edit/delete flat profiles.
2. As a landlord, I can set fixed monthly rent per flat.
3. As a landlord, I can create/edit/delete tenant profiles and map each tenant to a flat.
4. As a landlord, I can create a monthly billing cycle.
5. As a landlord, I can record one total electricity bill for the month.
6. As a landlord, I can input monthly unit reading per flat.
7. As a landlord, I can mark payments as unpaid/partial/paid with amount, mandatory payment date, and notes.
8. As a landlord, I can carry tenant overpayment/underpayment as adjustment into next cycle.
9. As a landlord, I can see unpaid tenants and monthly summary at a glance.

## Functional Requirements
1. Flat Management
- Fields: flat/unit label, fixed monthly rent, active/inactive, notes.
- Validation: fixed monthly rent > 0 for active flat.

2. Tenant Management
- Fields: tenant name, phone (optional), linked flat, active/inactive, notes.
- Validation: active tenant must be linked to an active flat.

3. Monthly Ledger
- One ledger per month (`YYYY-MM`).
- Stores bill inputs, per-flat readings, per-tenant computed dues, adjustments, and payment records.

4. Electricity Allocation
- Input: total electricity bill amount and per-flat monthly unit reading.
- Formula: flat share = `(flat_units / sum_all_units) * total_bill`.
- Flat share is assigned to the active tenant linked to that flat in the month.
- If total units = 0, block finalization and ask for correction.
- Round to 2 decimals; adjust final tenant by rounding delta so totals match bill.

5. Adjustment Logic (Next Cycle)
- Every tenant has a running balance.
- If tenant pays more than due, extra amount becomes credit for next cycle.
- If tenant pays less than due, shortfall becomes debit for next cycle.
- Next cycle total due must include `adjustmentAmount` from previous balance.

6. Payment Tracking
- Manual status update per component:
- Rent: unpaid/partial/paid.
- Electricity: unpaid/partial/paid.
- Track paid amount, mandatory payment date, and optional note.
- Payment record cannot be saved without payment date.
- Show outstanding balance per tenant.

7. Dashboard
- Current month summary:
- total expected, total paid, total pending.
- paid tenants count vs total active tenants.
- overdue/unpaid list.

8. Reminders
- Local notification reminders to landlord for due-date and overdue checks.

9. Security
- App entry protected with local PIN.
- Phone-auth deferred (because V1 is local-only single-user).

## UX Requirements
- Android-first UX with simple tabs/screens:
- Dashboard
- Flats
- Tenants
- Monthly Billing
- Payments/History
- Quick actions: "New Month", "Enter Bill", "Enter Reading", "Mark Paid".
- Empty states for first-time setup.

## Data Model (Public Interfaces/Types)
1. `Flat`
- `id: String`
- `unitLabel: String`
- `fixedMonthlyRent: Decimal`
- `isActive: Boolean`
- `notes: String?`

2. `Tenant`
- `id: String`
- `name: String`
- `flatId: String`
- `phone: String?`
- `isActive: Boolean`
- `notes: String?`

3. `BillingMonth`
- `id: String` (`YYYY-MM`)
- `electricityTotalAmount: Decimal`
- `status: Draft | Finalized`

4. `FlatUsage`
- `flatId: String`
- `billingMonthId: String`
- `unitsConsumed: Decimal`

5. `TenantMonthlyCharge`
- `tenantId: String`
- `billingMonthId: String`
- `rentAmount: Decimal`
- `electricityShare: Decimal`
- `adjustmentAmount: Decimal`
- `totalDue: Decimal`

6. `TenantBalance`
- `tenantId: String`
- `asOfMonthId: String`
- `balanceAmount: Decimal` (positive = credit, negative = debit)

7. `PaymentRecord`
- `id: String`
- `tenantId: String`
- `billingMonthId: String`
- `component: Rent | Electricity | Combined`
- `amountPaid: Decimal`
- `paidOn: Date` (required)
- `note: String?`

## Technical Direction
- Platform: Android (Kotlin).
- UI: Jetpack Compose.
- Local DB: Room.
- Architecture: MVVM + Repository pattern.
- Notifications: WorkManager/AlarmManager for due reminders.
- No backend in V1.

## Edge Cases and Failure Modes
- Tenant added mid-month: include from activation date (default full-month in V1; pro-rating deferred).
- Inactive flat/tenant: excluded from new months, retained in history.
- Missing usage for a flat: month cannot be finalized.
- Bill edited after payments recorded: require confirmation and re-compute balances.
- Tenant overpayment or underpayment must carry to next cycle adjustment.
- Tenant moved to another flat: tenant balance stays with tenant.
- Missing payment date: block payment save with validation message.
- Decimal precision/rounding discrepancies handled by adjustment rule.

## Acceptance Criteria
1. Landlord can manage flat list with fixed monthly rent per flat.
2. Landlord can manage tenant list and map tenants to flats.
3. Landlord can create monthly record and input common electricity bill.
4. App computes per-flat electricity split by usage correctly and assigns dues to tenant.
5. App applies tenant balance adjustment to next cycle correctly.
6. App shows paid/unpaid and pending balances per tenant and month.
7. Payment record save is blocked when payment date is missing.
8. Dashboard accurately reflects monthly collection progress.
9. Data persists offline and remains available after app restart.
10. Local reminders trigger on configured due dates.

## Test Cases and Scenarios
1. Unit Tests
- Electricity split formula correctness.
- Rounding reconciliation totals equal original bill.
- Payment status transitions (unpaid -> partial -> paid).
- Adjustment carry-forward math (overpay and underpay).

2. Integration Tests
- Create flat -> create tenant -> create month -> enter readings/bill -> compute dues end-to-end.
- Partial payment in month N correctly adjusts month N+1 due.
- Edit bill after partial payments and verify recalculated balances.

3. UI Tests
- Add/edit flat flow.
- Add/edit tenant flow.
- Mark payment flow with required payment date validation.
- Dashboard figures update after payment entry.

4. Edge Tests
- Zero total usage.
- One flat/one tenant only.
- Tenant inactive mid-lifecycle.
- Tenant changes flat while keeping running balance.
- Large bill values and decimal rents.

## Milestones
1. M1: Project setup, core models, Room schema, flat and tenant CRUD.
2. M2: Monthly billing + usage capture + electricity allocation engine.
3. M3: Payment tracking + adjustment carry-forward + dashboard.
4. M4: Notifications, polishing, QA, release candidate.

## Assumptions and Defaults
- Scope fixed to single landlord using one device.
- Tenant login is deferred to V2.
- Billing cycle is monthly calendar cycle.
- Payment capture is manual entry only.
- Payment date is mandatory for every payment record.
- Dashboard only in V1 (no CSV/PDF export).
- PIN-based local app lock is sufficient for V1.
