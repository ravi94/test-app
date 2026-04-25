package com.testapp.renttracker.logic

import com.testapp.renttracker.error.ErrorCodes
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.FlatUsage
import java.math.BigDecimal
import java.math.RoundingMode

object BillingCalculator {
    private val ZERO = BigDecimal.ZERO

    fun computeElectricityChargeByFlatLabel(
        flatLabels: List<String>,
        usages: List<FlatUsage>,
        ratePerUnit: BigDecimal,
    ): Map<String, BigDecimal> {
        val usageByFlat = usages.associateBy { it.flatLabel }
        if (ratePerUnit < ZERO) {
            throw ValidationError(
                code = ErrorCodes.INVALID_AMOUNT,
                message = "ratePerUnit must be non-negative",
                field = "ratePerUnit",
            )
        }

        return flatLabels.associateWith { flatLabel ->
            val units = usageByFlat[flatLabel]?.unitsConsumed ?: ZERO
            val charge = units.multiply(ratePerUnit).setScale(2, RoundingMode.HALF_UP)
            charge
        }
    }
}
