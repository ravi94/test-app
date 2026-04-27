package com.testapp.renttracker.logic

import com.testapp.renttracker.error.ErrorCodes
import com.testapp.renttracker.error.ValidationError
import java.math.BigDecimal
import java.math.RoundingMode

object BillingCalculator {
    private val ZERO = BigDecimal.ZERO

    fun computeUnitsConsumed(currentReading: BigDecimal, previousReading: BigDecimal): BigDecimal {
        val units = currentReading.subtract(previousReading)
        if (units < ZERO) {
            throw ValidationError(
                code = ErrorCodes.INVALID_METER_READING,
                message = "Current meter reading must be greater than or equal to previous meter reading ($previousReading)",
                field = "currentMeterReading",
            )
        }
        return units.setScale(2, RoundingMode.HALF_UP)
    }

    fun computeElectricityChargeByFlatLabel(
        unitsByFlat: Map<String, BigDecimal>,
        ratePerUnit: BigDecimal,
    ): Map<String, BigDecimal> {
        if (ratePerUnit < ZERO) {
            throw ValidationError(
                code = ErrorCodes.INVALID_AMOUNT,
                message = "ratePerUnit must be non-negative",
                field = "ratePerUnit",
            )
        }

        return unitsByFlat.mapValues { (_, units) ->
            val charge = units.multiply(ratePerUnit).setScale(2, RoundingMode.HALF_UP)
            charge
        }
    }
}
