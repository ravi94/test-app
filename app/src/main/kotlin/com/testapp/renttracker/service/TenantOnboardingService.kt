package com.testapp.renttracker.service

import com.testapp.renttracker.error.ErrorCodes
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.CreateTenantOnboardingInput
import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.model.TenantBalance
import com.testapp.renttracker.repo.IdGenerator
import com.testapp.renttracker.repo.TenantBalanceRepository
import com.testapp.renttracker.repo.TenantRepository
import java.math.BigDecimal
import java.math.RoundingMode

class TenantOnboardingService(
    private val tenantRepo: TenantRepository,
    private val balanceRepo: TenantBalanceRepository,
    private val idGenerator: IdGenerator,
) {
    fun createTenant(input: CreateTenantOnboardingInput): Tenant {
        val name = input.name.trim()
        if (name.isBlank()) {
            throw ValidationError(ErrorCodes.REQUIRED_FIELD, "Tenant name is required", "name")
        }
        val flatLabel = input.flatLabel.trim()
        if (flatLabel.isBlank()) {
            throw ValidationError(ErrorCodes.REQUIRED_FIELD, "Flat identifier is required", "flatLabel")
        }
        if (input.monthlyRent <= BigDecimal.ZERO) {
            throw ValidationError(ErrorCodes.INVALID_AMOUNT, "Monthly rent must be greater than zero", "monthlyRent")
        }
        if (!MONTH_PATTERN.matches(input.billingStartMonth)) {
            throw ValidationError(
                ErrorCodes.INVALID_MONTH_FORMAT,
                "Billing start month must be in YYYY-MM format",
                "billingStartMonth",
            )
        }
        if (input.initialDue < BigDecimal.ZERO) {
            throw ValidationError(ErrorCodes.INVALID_AMOUNT, "Initial due must be non-negative", "initialDue")
        }

        val activeTenantOnFlat = tenantRepo.getActiveTenantByFlatLabel(flatLabel)
        if (input.isActive && activeTenantOnFlat != null) {
            throw ValidationError(
                ErrorCodes.FLAT_ALREADY_OCCUPIED,
                "Selected flat identifier already has an active tenant",
                "flatLabel",
            )
        }

        val tenant = Tenant(
            id = idGenerator.newId(),
            name = name,
            flatLabel = flatLabel,
            monthlyRent = input.monthlyRent.setScale(2, RoundingMode.HALF_UP),
            billingStartMonth = input.billingStartMonth,
            phone = input.phone?.trim().orEmpty().ifBlank { null },
            isActive = input.isActive,
            notes = input.notes?.trim().orEmpty().ifBlank { null },
        )
        tenantRepo.upsertTenant(tenant)

        val initialDue = input.initialDue.setScale(2, RoundingMode.HALF_UP)
        if (initialDue > BigDecimal.ZERO) {
            balanceRepo.upsertBalance(
                TenantBalance(
                    tenantId = tenant.id,
                    asOfMonthId = previousMonth(input.billingStartMonth),
                    balanceAmount = initialDue.negate().setScale(2, RoundingMode.HALF_UP),
                )
            )
        }

        return tenant
    }

    private fun previousMonth(monthId: String): String {
        val (yearText, monthText) = monthId.split("-")
        val year = yearText.toInt()
        val month = monthText.toInt()
        return if (month == 1) {
            "${year - 1}-12"
        } else {
            "%04d-%02d".format(year, month - 1)
        }
    }

    private companion object {
        val MONTH_PATTERN = Regex("""\d{4}-(0[1-9]|1[0-2])""")
    }
}
