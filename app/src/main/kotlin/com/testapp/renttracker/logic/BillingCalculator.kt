package com.testapp.renttracker.logic

import com.testapp.renttracker.error.ErrorCodes
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.Flat
import com.testapp.renttracker.model.FlatUsage
import java.math.BigDecimal
import java.math.RoundingMode

object BillingCalculator {
    private val ZERO = BigDecimal.ZERO

    fun computeElectricityShareByFlat(
        flats: List<Flat>,
        usages: List<FlatUsage>,
        totalBill: BigDecimal,
    ): Map<String, BigDecimal> {
        val usageByFlat = usages.associateBy { it.flatId }
        val flatUnits = flats.map { flat ->
            flat.id to (usageByFlat[flat.id]?.unitsConsumed ?: ZERO)
        }

        val totalUnits = flatUnits.fold(ZERO) { acc, (_, u) -> acc.add(u) }
        if (totalUnits.compareTo(ZERO) == 0) {
            throw ValidationError(
                code = ErrorCodes.ZERO_TOTAL_UNITS,
                message = "Cannot compute electricity split when total units are zero",
                field = "unitsConsumed",
            )
        }

        val roundedShares = mutableListOf<Pair<String, BigDecimal>>()
        var runningTotal = ZERO

        flatUnits.forEachIndexed { idx, (flatId, units) ->
            val share = if (idx == flatUnits.lastIndex) {
                totalBill.subtract(runningTotal)
            } else {
                totalBill
                    .multiply(units)
                    .divide(totalUnits, 2, RoundingMode.HALF_UP)
            }
            val normalized = share.setScale(2, RoundingMode.HALF_UP)
            roundedShares += flatId to normalized
            runningTotal = runningTotal.add(normalized)
        }

        val delta = totalBill.setScale(2, RoundingMode.HALF_UP).subtract(runningTotal)
        if (delta.compareTo(ZERO) != 0 && roundedShares.isNotEmpty()) {
            val last = roundedShares.last()
            roundedShares[roundedShares.lastIndex] = last.first to last.second.add(delta)
        }

        return roundedShares.toMap()
    }
}
