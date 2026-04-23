package com.testapp.renttracker.data.room

import androidx.room.TypeConverter
import com.testapp.renttracker.model.BillingMonthStatus
import com.testapp.renttracker.model.PaymentComponent
import java.math.BigDecimal
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromBillingStatus(value: BillingMonthStatus?): String? = value?.name

    @TypeConverter
    fun toBillingStatus(value: String?): BillingMonthStatus? = value?.let(BillingMonthStatus::valueOf)

    @TypeConverter
    fun fromPaymentComponent(value: PaymentComponent?): String? = value?.name

    @TypeConverter
    fun toPaymentComponent(value: String?): PaymentComponent? = value?.let(PaymentComponent::valueOf)
}
