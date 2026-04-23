package com.testapp.renttracker.error

class ValidationError(
    val code: String,
    override val message: String,
    val field: String? = null,
) : RuntimeException(message)

object ErrorCodes {
    const val PAYMENT_DATE_REQUIRED = "PAYMENT_DATE_REQUIRED"
    const val ZERO_TOTAL_UNITS = "ZERO_TOTAL_UNITS"
    const val INVALID_FLAT_TENANT_LINK = "INVALID_FLAT_TENANT_LINK"
    const val MONTH_NOT_DRAFT = "MONTH_NOT_DRAFT"
    const val MISSING_MONTH_CHARGES = "MISSING_MONTH_CHARGES"
    const val MONTH_ALREADY_EXISTS = "MONTH_ALREADY_EXISTS"
    const val MONTH_NOT_FOUND = "MONTH_NOT_FOUND"
    const val INVALID_AMOUNT = "INVALID_AMOUNT"
}
