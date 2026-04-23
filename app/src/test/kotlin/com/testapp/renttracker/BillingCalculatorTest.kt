package com.testapp.renttracker

import com.testapp.renttracker.logic.BillingCalculator
import com.testapp.renttracker.model.Flat
import com.testapp.renttracker.model.FlatUsage
import kotlin.test.Test
import kotlin.test.assertEquals
import java.math.BigDecimal

class BillingCalculatorTest {
    @Test
    fun `electricity split reconciles to exact bill after rounding`() {
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

        val shares = BillingCalculator.computeElectricityShareByFlat(flats, usages, BigDecimal("1000.00"))
        val total = shares.values.reduce(BigDecimal::add)

        assertEquals(BigDecimal("1000.00"), total)
    }
}
