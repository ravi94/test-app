package com.testapp.renttracker.data.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `flats` (
                    `id` TEXT NOT NULL,
                    `unit_label` TEXT NOT NULL,
                    `fixed_monthly_rent` TEXT NOT NULL,
                    `is_active` INTEGER NOT NULL,
                    `notes` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_flats_unit_label` ON `flats` (`unit_label`)")

            db.execSQL("ALTER TABLE `tenants` ADD COLUMN `flat_id` TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_tenants_flat_id` ON `tenants` (`flat_id`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `flat_usage` (
                    `flat_id` TEXT NOT NULL,
                    `billing_month_id` TEXT NOT NULL,
                    `units_consumed` TEXT NOT NULL,
                    PRIMARY KEY(`flat_id`, `billing_month_id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_flat_usage_billing_month_id` ON `flat_usage` (`billing_month_id`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tenant_balances` (
                    `tenant_id` TEXT NOT NULL,
                    `as_of_month_id` TEXT NOT NULL,
                    `balance_amount` TEXT NOT NULL,
                    PRIMARY KEY(`tenant_id`, `as_of_month_id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_balances_tenant` ON `tenant_balances` (`tenant_id`)")

            db.execSQL("ALTER TABLE `tenant_monthly_charges` ADD COLUMN `adjustment_amount` TEXT NOT NULL DEFAULT '0.00'")
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `payment_records_new` (
                    `id` TEXT NOT NULL,
                    `tenant_id` TEXT NOT NULL,
                    `billing_month_id` TEXT NOT NULL,
                    `component` TEXT NOT NULL,
                    `amount_paid` TEXT NOT NULL,
                    `paid_on` TEXT NOT NULL,
                    `note` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO `payment_records_new` (`id`, `tenant_id`, `billing_month_id`, `component`, `amount_paid`, `paid_on`, `note`)
                SELECT `id`, `tenant_id`, `billing_month_id`, `component`, `amount_paid`,
                       COALESCE(`paid_on`, date('now')) as `paid_on`,
                       `note`
                FROM `payment_records`
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `payment_records`")
            db.execSQL("ALTER TABLE `payment_records_new` RENAME TO `payment_records`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_payments_tenant_month` ON `payment_records` (`tenant_id`, `billing_month_id`)")
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tenants_new` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `flat_label` TEXT NOT NULL,
                    `monthly_rent` TEXT NOT NULL,
                    `phone` TEXT,
                    `is_active` INTEGER NOT NULL,
                    `notes` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_tenants_flat_label` ON `tenants_new` (`flat_label`)")
            db.execSQL(
                """
                INSERT INTO `tenants_new` (`id`, `name`, `flat_label`, `monthly_rent`, `phone`, `is_active`, `notes`)
                SELECT t.`id`,
                       t.`name`,
                       COALESCE(f.`unit_label`, t.`flat_id`, ''),
                       COALESCE(f.`fixed_monthly_rent`, '0.00'),
                       t.`phone`,
                       t.`is_active`,
                       t.`notes`
                FROM `tenants` t
                LEFT JOIN `flats` f ON f.`id` = t.`flat_id`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `tenants`")
            db.execSQL("ALTER TABLE `tenants_new` RENAME TO `tenants`")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `flat_usage_new` (
                    `flat_label` TEXT NOT NULL,
                    `billing_month_id` TEXT NOT NULL,
                    `units_consumed` TEXT NOT NULL,
                    PRIMARY KEY(`flat_label`, `billing_month_id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `flat_usage_new` (`flat_label`, `billing_month_id`, `units_consumed`)
                SELECT COALESCE(f.`unit_label`, fu.`flat_id`, ''),
                       fu.`billing_month_id`,
                       fu.`units_consumed`
                FROM `flat_usage` fu
                LEFT JOIN `flats` f ON f.`id` = fu.`flat_id`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `flat_usage`")
            db.execSQL("ALTER TABLE `flat_usage_new` RENAME TO `flat_usage`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_flat_usage_billing_month_id` ON `flat_usage` (`billing_month_id`)")

            db.execSQL("DROP TABLE IF EXISTS `flats`")
        }
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tenants_new` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `flat_label` TEXT NOT NULL,
                    `monthly_rent` TEXT NOT NULL,
                    `billing_start_month` TEXT NOT NULL,
                    `phone` TEXT,
                    `is_active` INTEGER NOT NULL,
                    `notes` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `tenants_new` (`id`, `name`, `flat_label`, `monthly_rent`, `billing_start_month`, `phone`, `is_active`, `notes`)
                SELECT `id`,
                       `name`,
                       `flat_label`,
                       `monthly_rent`,
                       '0001-01',
                       `phone`,
                       `is_active`,
                       `notes`
                FROM `tenants`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `tenants`")
            db.execSQL("ALTER TABLE `tenants_new` RENAME TO `tenants`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_tenants_flat_label` ON `tenants` (`flat_label`)")
        }
    }

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tenants_new` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `flat_label` TEXT NOT NULL,
                    `monthly_rent` TEXT NOT NULL,
                    `billing_start_month` TEXT NOT NULL,
                    `initial_meter_reading` TEXT NOT NULL,
                    `phone` TEXT,
                    `is_active` INTEGER NOT NULL,
                    `notes` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `tenants_new` (`id`, `name`, `flat_label`, `monthly_rent`, `billing_start_month`, `initial_meter_reading`, `phone`, `is_active`, `notes`)
                SELECT `id`,
                       `name`,
                       `flat_label`,
                       `monthly_rent`,
                       `billing_start_month`,
                       '0.00',
                       `phone`,
                       `is_active`,
                       `notes`
                FROM `tenants`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `tenants`")
            db.execSQL("ALTER TABLE `tenants_new` RENAME TO `tenants`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_tenants_flat_label` ON `tenants` (`flat_label`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `flat_usage_new` (
                    `flat_label` TEXT NOT NULL,
                    `billing_month_id` TEXT NOT NULL,
                    `meter_reading` TEXT NOT NULL,
                    PRIMARY KEY(`flat_label`, `billing_month_id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `flat_usage_new` (`flat_label`, `billing_month_id`, `meter_reading`)
                SELECT `flat_label`,
                       `billing_month_id`,
                       `units_consumed`
                FROM `flat_usage`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `flat_usage`")
            db.execSQL("ALTER TABLE `flat_usage_new` RENAME TO `flat_usage`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_flat_usage_billing_month_id` ON `flat_usage` (`billing_month_id`)")
        }
    }
}
