package com.testapp.renttracker.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testapp.renttracker.error.ErrorCodes
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.BillingMonth
import com.testapp.renttracker.model.BillingMonthStatus
import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.model.TenantMonthlyCharge
import com.testapp.renttracker.repo.BillingMonthRepository
import com.testapp.renttracker.repo.FlatUsageRepository
import com.testapp.renttracker.repo.TenantRepository
import com.testapp.renttracker.service.BillingMonthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class MonthlyBillingViewModel(
    private val billingService: BillingMonthService,
    private val tenantRepo: TenantRepository,
    private val monthRepo: BillingMonthRepository,
    private val usageRepo: FlatUsageRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(
        MonthlyBillingUiState(
            availableTenants = tenantRepo.getActiveTenants(),
        )
    )
    val state: StateFlow<MonthlyBillingUiState> = _state.asStateFlow()

    init {
        val initialTenantId = _state.value.availableTenants.firstOrNull()?.id
        if (initialTenantId != null) {
            _state.update { it.copy(selectedTenantId = initialTenantId) }
        }
        refreshFormState()
    }

    fun setMonth(monthId: String) {
        _state.update { it.copy(monthId = monthId) }
        refreshFormState()
    }

    fun setSelectedTenant(tenantId: String) {
        _state.update { it.copy(selectedTenantId = tenantId) }
        refreshFormState()
    }

    fun refreshTenants() {
        val tenants = tenantRepo.getActiveTenants()
        val selectedTenantId = _state.value.selectedTenantId.takeIf { currentId ->
            tenants.any { it.id == currentId }
        } ?: tenants.firstOrNull()?.id.orEmpty()

        _state.update {
            it.copy(
                availableTenants = tenants,
                selectedTenantId = selectedTenantId,
            )
        }
        refreshFormState()
    }

    fun setElectricityRateInput(ratePerUnit: String) {
        _state.update { it.copy(electricityRateInput = ratePerUnit) }
    }

    fun setCurrentMeterReadingInput(reading: String) {
        _state.update { it.copy(currentMeterReadingInput = reading) }
    }

    fun saveBillingEntry() {
        val current = _state.value
        val monthId = current.monthId ?: return
        val tenant = current.availableTenants.firstOrNull { it.id == current.selectedTenantId } ?: return

        execute("Billing updated") {
            validateBillingMonthForTenant(monthId, tenant)
            ensureMonthExists(monthId)
            billingService.setElectricityRate(monthId, current.electricityRateInput.toBigDecimal())
            billingService.upsertFlatUsage(monthId, tenant, current.currentMeterReadingInput.toBigDecimal())
            billingService.computeMonthCharges(monthId)
            refreshFormState()
        }
    }

    fun finalizeMonth() {
        val monthId = _state.value.monthId ?: return
        execute("Month finalized") {
            billingService.finalizeMonth(monthId)
            _state.update { it.copy(isFinalized = true) }
        }
    }

    fun setComputedCharges(charges: List<TenantMonthlyCharge>) {
        _state.update { it.copy(computedCharges = charges) }
    }

    private fun refreshFormState() {
        val current = _state.value
        val monthId = current.monthId ?: return
        val tenant = current.availableTenants.firstOrNull { it.id == current.selectedTenantId }
        val month = monthRepo.getMonth(monthId)
        val currentReadingForTenant = tenant?.let { usageRepo.getUsage(it.flatLabel, monthId)?.meterReading?.formatAmount() }.orEmpty()
        val previousReadingForTenant = tenant?.let { resolvePreviousMeterReading(it, monthId) }
        val computedUnits = runCatching {
            val currentReading = currentReadingForTenant.toBigDecimalOrNull()
            val previousReading = previousReadingForTenant?.toBigDecimalOrNull()
            if (currentReading != null && previousReading != null) {
                currentReading.subtract(previousReading).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            } else {
                ""
            }
        }.getOrDefault("")

        _state.update {
            it.copy(
                currentMeterReadingInput = currentReadingForTenant,
                previousMeterReading = previousReadingForTenant.orEmpty(),
                computedUnits = computedUnits,
                electricityRateInput = month?.electricityRatePerUnit?.formatAmount() ?: it.electricityRateInput,
                isFinalized = month?.status == BillingMonthStatus.Finalized,
            )
        }
    }

    private fun ensureMonthExists(monthId: String) {
        if (monthRepo.getMonth(monthId) == null) {
            monthRepo.createMonth(
                BillingMonth(
                    id = monthId,
                    electricityRatePerUnit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    status = BillingMonthStatus.Draft,
                )
            )
        }
    }

    private fun validateBillingMonthForTenant(monthId: String, tenant: Tenant) {
        val selectedMonth = parseMonth(monthId)
        val startMonth = parseMonth(tenant.billingStartMonth)
        if (selectedMonth != null && startMonth != null && selectedMonth < startMonth) {
            throw ValidationError(
                ErrorCodes.BILLING_BEFORE_START_MONTH,
                "Cannot add billing for ${tenant.name} before ${tenant.billingStartMonth}",
                "monthId",
            )
        }
    }

    private fun parseMonth(monthId: String): Int? {
        val parts = monthId.split("-")
        if (parts.size != 2) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        return year * 100 + month
    }

    private fun resolvePreviousMeterReading(tenant: Tenant, monthId: String): String? {
        val previousMonthId = previousMonth(monthId) ?: return tenant.initialMeterReading.formatAmount()
        val previousUsage = usageRepo.getUsage(tenant.flatLabel, previousMonthId)
        if (previousUsage != null) {
            return previousUsage.meterReading.formatAmount()
        }
        if (monthId == tenant.billingStartMonth) {
            return tenant.initialMeterReading.formatAmount()
        }
        return null
    }

    private fun previousMonth(monthId: String): String? {
        val parts = monthId.split("-")
        if (parts.size != 2) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        return if (month == 1) {
            "${year - 1}-12"
        } else {
            "%04d-%02d".format(year, month - 1)
        }
    }

    private fun execute(successMessage: String, block: () -> Unit) {
        _state.update { it.copy(isLoading = true, error = null, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block.invoke()
                _state.update { it.copy(isLoading = false, message = successMessage) }
            } catch (e: ValidationError) {
                _state.update { it.copy(isLoading = false, error = "${e.code}: ${e.message}", message = null) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error", message = null) }
            }
        }
    }

    private fun BigDecimal.formatAmount(): String = setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

data class MonthlyBillingUiState(
    val monthId: String? = currentMonth(),
    val availableTenants: List<Tenant> = emptyList(),
    val selectedTenantId: String = "",
    val previousMeterReading: String = "",
    val currentMeterReadingInput: String = "",
    val computedUnits: String = "",
    val electricityRateInput: String = "",
    val computedCharges: List<TenantMonthlyCharge> = emptyList(),
    val isFinalized: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

private fun currentMonth(): String {
    val today = LocalDate.now()
    return "%04d-%02d".format(today.year, today.monthValue)
}
