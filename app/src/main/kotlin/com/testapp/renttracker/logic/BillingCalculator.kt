package com.testapp.renttracker.logic

import com.testapp.renttracker.error.ErrorCodes
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.Flat
import com.testapp.renttracker.model.FlatUsage
import java.math.BigDecimal
import java.math.RoundingMode

object BillingCalculator {
    private val ZERO = BigDecimal.ZERO

    fun computeElectricityChargeByFlat(
        flats: List<Flat>,
        usages: List<FlatUsage>,
        ratePerUnit: BigDecimal,
    ): Map<String, BigDecimal> {
        val usageByFlat = usages.associateBy { it.flatId }
        if (ratePerUnit < ZERO) {
            throw ValidationError(
                code = ErrorCodes.INVALID_AMOUNT,
                message = "ratePerUnit must be non-negative",
                field = "ratePerUnit",
            )
        }

        return flats.associate { flat ->
            val units = usageByFlat[flat.id]?.unitsConsumed ?: ZERO
            val charge = units.multiply(ratePerUnit).setScale(2, RoundingMode.HALF_UP)
            flat.id to charge
        }
    }
}
