package com.testapp.renttracker.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.TenantMonthlyCharge
import com.testapp.renttracker.service.BillingMonthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MonthlyBillingViewModel(
    private val billingService: BillingMonthService,
) : ViewModel() {
    private val _state = MutableStateFlow(MonthlyBillingUiState())
    val state: StateFlow<MonthlyBillingUiState> = _state.asStateFlow()

    fun setMonth(monthId: String) {
        _state.update { it.copy(monthId = monthId) }
    }

    fun createMonth() {
        val monthId = _state.value.monthId ?: return
        execute("Month created") {
            billingService.createBillingMonth(monthId)
        }
    }

    fun setElectricityBill(totalAmount: String) {
        val monthId = _state.value.monthId ?: return
        execute("Electricity bill saved") {
            billingService.setElectricityBill(monthId, totalAmount.toBigDecimal())
            _state.update { it.copy(electricityBillInput = totalAmount) }
        }
    }

    fun setFlatUnits(flatId: String, units: String) {
        val monthId = _state.value.monthId ?: return
        execute("Units saved for $flatId") {
            billingService.upsertFlatUsage(monthId, flatId, units.toBigDecimal())
            _state.update { current ->
                current.copy(flatUnitsInput = current.flatUnitsInput + (flatId to units))
            }
        }
    }

    fun computeCharges() {
        val monthId = _state.value.monthId ?: return
        execute("Charges computed") {
            billingService.computeMonthCharges(monthId)
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
}

data class MonthlyBillingUiState(
    val monthId: String? = "2026-02",
    val electricityBillInput: String = "",
    val flatUnitsInput: Map<String, String> = emptyMap(),
    val computedCharges: List<TenantMonthlyCharge> = emptyList(),
    val isFinalized: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)
