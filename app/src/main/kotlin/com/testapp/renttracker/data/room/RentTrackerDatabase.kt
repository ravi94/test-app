package com.testapp.renttracker.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.testapp.renttracker.data.room.dao.BillingMonthDao
import com.testapp.renttracker.data.room.dao.FlatDao
import com.testapp.renttracker.data.room.dao.FlatUsageDao
import com.testapp.renttracker.data.room.dao.PaymentRecordDao
import com.testapp.renttracker.data.room.dao.TenantBalanceDao
import com.testapp.renttracker.data.room.dao.TenantDao
import com.testapp.renttracker.data.room.dao.TenantMonthlyChargeDao
import com.testapp.renttracker.data.room.entity.BillingMonthEntity
import com.testapp.renttracker.data.room.entity.FlatEntity
import com.testapp.renttracker.data.room.entity.FlatUsageEntity
import com.testapp.renttracker.data.room.entity.PaymentRecordEntity
import com.testapp.renttracker.data.room.entity.TenantBalanceEntity
import com.testapp.renttracker.data.room.entity.TenantEntity
import com.testapp.renttracker.data.room.entity.TenantMonthlyChargeEntity

@Database(
    entities = [
        FlatEntity::class,
        TenantEntity::class,
        BillingMonthEntity::class,
        FlatUsageEntity::class,
        TenantMonthlyChargeEntity::class,
        TenantBalanceEntity::class,
        PaymentRecordEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class RentTrackerDatabase : RoomDatabase() {
    abstract fun flatDao(): FlatDao
    abstract fun tenantDao(): TenantDao
    abstract fun billingMonthDao(): BillingMonthDao
    abstract fun flatUsageDao(): FlatUsageDao
    abstract fun tenantMonthlyChargeDao(): TenantMonthlyChargeDao
    abstract fun tenantBalanceDao(): TenantBalanceDao
    abstract fun paymentRecordDao(): PaymentRecordDao
}
