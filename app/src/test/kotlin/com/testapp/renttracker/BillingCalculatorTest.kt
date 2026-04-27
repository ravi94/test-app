package com.testapp.renttracker

import com.testapp.renttracker.logic.BillingCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import java.math.BigDecimal

class BillingCalculatorTest {
    @Test
    fun `electricity charge is computed from per-unit rate`() {
        val unitsByFlat = mapOf(
            "A-101" to BigDecimal("31"),
            "A-102" to BigDecimal("17"),
            "A-103" to BigDecimal("29"),
        )

        val shares = BillingCalculator.computeElectricityChargeByFlatLabel(unitsByFlat, BigDecimal("12.50"))

        assertEquals(BigDecimal("387.50"), shares.getValue("A-101"))
        assertEquals(BigDecimal("212.50"), shares.getValue("A-102"))
        assertEquals(BigDecimal("362.50"), shares.getValue("A-103"))
    }

    @Test
    fun `units consumed are derived from current and previous readings`() {
        val units = BillingCalculator.computeUnitsConsumed(
            currentReading = BigDecimal("143"),
            previousReading = BigDecimal("131"),
        )

        assertEquals(BigDecimal("12.00"), units)
    }
}
