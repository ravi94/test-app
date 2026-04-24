package com.testapp.renttracker

import com.testapp.renttracker.logic.BillingCalculator
import com.testapp.renttracker.model.Flat
import com.testapp.renttracker.model.FlatUsage
import kotlin.test.Test
import kotlin.test.assertEquals
import java.math.BigDecimal

class BillingCalculatorTest {
    @Test
    fun `electricity charge is computed from per-unit rate`() {
        val flats = listOf(
            Flat("F1", "A-101", BigDecimal("5000.00")),
            Flat("F2", "A-102", BigDecimal("6000.00")),
            Flat("F3", "A-103", BigDecimal("5500.00")),
        )
        val usages = listOf(
            FlatUsage("F1", "2026-02", BigDecimal("31")),
            FlatUsage("F2", "2026-02", BigDecimal("17")),
            FlatUsage("F3", "2026-02", BigDecimal("29")),
        )

        val shares = BillingCalculator.computeElectricityChargeByFlat(flats, usages, BigDecimal("12.50"))

        assertEquals(BigDecimal("387.50"), shares.getValue("F1"))
        assertEquals(BigDecimal("212.50"), shares.getValue("F2"))
        assertEquals(BigDecimal("362.50"), shares.getValue("F3"))
    }
}
