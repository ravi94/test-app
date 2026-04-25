package com.testapp.renttracker

import com.testapp.renttracker.logic.BillingCalculator
import com.testapp.renttracker.model.FlatUsage
import kotlin.test.Test
import kotlin.test.assertEquals
import java.math.BigDecimal

class BillingCalculatorTest {
    @Test
    fun `electricity charge is computed from per-unit rate`() {
        val flatLabels = listOf("A-101", "A-102", "A-103")
        val usages = listOf(
            FlatUsage("A-101", "2026-02", BigDecimal("31")),
            FlatUsage("A-102", "2026-02", BigDecimal("17")),
            FlatUsage("A-103", "2026-02", BigDecimal("29")),
        )

        val shares = BillingCalculator.computeElectricityChargeByFlatLabel(flatLabels, usages, BigDecimal("12.50"))

        assertEquals(BigDecimal("387.50"), shares.getValue("A-101"))
        assertEquals(BigDecimal("212.50"), shares.getValue("A-102"))
        assertEquals(BigDecimal("362.50"), shares.getValue("A-103"))
    }
}
