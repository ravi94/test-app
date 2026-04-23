# Room Schema and Migration Plan

## 1. Tables

### `flats`
- `id TEXT PRIMARY KEY`
- `unit_label TEXT NOT NULL UNIQUE`
- `fixed_monthly_rent TEXT NOT NULL`  -- Decimal as string
- `is_active INTEGER NOT NULL DEFAULT 1`
- `notes TEXT`

### `tenants`
- `id TEXT PRIMARY KEY`
- `name TEXT NOT NULL`
- `flat_id TEXT NOT NULL REFERENCES flats(id)`
- `phone TEXT`
- `is_active INTEGER NOT NULL DEFAULT 1`
- `notes TEXT`

### `billing_months`
- `id TEXT PRIMARY KEY`  -- YYYY-MM
- `electricity_total_amount TEXT NOT NULL DEFAULT '0.00'`
- `status TEXT NOT NULL` -- Draft | Finalized

### `flat_usage`
- `flat_id TEXT NOT NULL REFERENCES flats(id)`
- `billing_month_id TEXT NOT NULL REFERENCES billing_months(id)`
- `units_consumed TEXT NOT NULL`
- `PRIMARY KEY(flat_id, billing_month_id)`

### `tenant_monthly_charges`
- `tenant_id TEXT NOT NULL REFERENCES tenants(id)`
- `billing_month_id TEXT NOT NULL REFERENCES billing_months(id)`
- `rent_amount TEXT NOT NULL`
- `electricity_share TEXT NOT NULL`
- `adjustment_amount TEXT NOT NULL DEFAULT '0.00'`
- `total_due TEXT NOT NULL`
- `PRIMARY KEY(tenant_id, billing_month_id)`

### `tenant_balances`
- `tenant_id TEXT NOT NULL REFERENCES tenants(id)`
- `as_of_month_id TEXT NOT NULL`
- `balance_amount TEXT NOT NULL`
- `PRIMARY KEY(tenant_id, as_of_month_id)`

### `payment_records`
- `id TEXT PRIMARY KEY`
- `tenant_id TEXT NOT NULL REFERENCES tenants(id)`
- `billing_month_id TEXT NOT NULL REFERENCES billing_months(id)`
- `component TEXT NOT NULL` -- Rent | Electricity | Combined
- `amount_paid TEXT NOT NULL`
- `paid_on TEXT NOT NULL` -- ISO-8601 date
- `note TEXT`

## 2. Indexes
- `idx_tenants_flat_id` on `tenants(flat_id)`
- `idx_charges_month` on `tenant_monthly_charges(billing_month_id)`
- `idx_payments_tenant_month` on `payment_records(tenant_id, billing_month_id)`
- `idx_balances_tenant` on `tenant_balances(tenant_id)`

## 3. Migration Strategy
If migrating from tenant-rent schema:
1. Create new `flats`, `flat_usage`, `tenant_balances` tables.
2. Add `flat_id` to tenants and backfill one flat per old unit label.
3. Move tenant rent into `flats.fixed_monthly_rent` (snapshot existing values per mapped flat).
4. Add `adjustment_amount` to `tenant_monthly_charges` default `0.00`.
5. Enforce `payment_records.paid_on NOT NULL`.
6. Seed `tenant_balances` rows at `0.00` where absent.

All migrations must be forward-only, idempotent, and Room versioned.
