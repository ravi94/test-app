package com.testapp.renttracker.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.CreateTenantOnboardingInput
import com.testapp.renttracker.service.TenantOnboardingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

class TenantOnboardingViewModel(
    private val onboardingService: TenantOnboardingService,
) : ViewModel() {
    private val _state = MutableStateFlow(
        TenantOnboardingUiState(
            billingStartMonth = currentMonth(),
        )
    )
    val state: StateFlow<TenantOnboardingUiState> = _state.asStateFlow()

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setPhone(value: String) = _state.update { it.copy(phone = value) }
    fun setFlatLabel(value: String) = _state.update { it.copy(flatLabel = value) }
    fun setMonthlyRent(value: String) = _state.update { it.copy(monthlyRent = value) }
    fun setActive(value: Boolean) = _state.update { it.copy(isActive = value) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value) }
    fun setBillingStartMonth(value: String) = _state.update { it.copy(billingStartMonth = value) }
    fun setInitialDue(value: String) = _state.update { it.copy(initialDue = value) }

    fun submit(onSuccess: (() -> Unit)? = null) {
        val current = _state.value
        val initialDue = current.initialDue.toBigDecimalOrNull()
        val monthlyRent = current.monthlyRent.toBigDecimalOrNull()
        if (current.initialDue.isBlank()) {
            _state.update { it.copy(error = "REQUIRED_FIELD: Initial due is required", message = null) }
            return
        }
        if (initialDue == null) {
            _state.update { it.copy(error = "INVALID_AMOUNT: Initial due must be a valid number", message = null) }
            return
        }
        if (current.monthlyRent.isBlank()) {
            _state.update { it.copy(error = "REQUIRED_FIELD: Monthly rent is required", message = null) }
            return
        }
        if (monthlyRent == null) {
            _state.update { it.copy(error = "INVALID_AMOUNT: Monthly rent must be a valid number", message = null) }
            return
        }

        _state.update { it.copy(isLoading = true, error = null, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                onboardingService.createTenant(
                    CreateTenantOnboardingInput(
                        name = current.name,
                        flatLabel = current.flatLabel,
                        monthlyRent = monthlyRent,
                        phone = current.phone,
                        isActive = current.isActive,
                        notes = current.notes,
                        billingStartMonth = current.billingStartMonth,
                        initialDue = initialDue,
                    )
                )
                _state.update {
                    TenantOnboardingUiState(
                        billingStartMonth = currentMonth(),
                        message = "Tenant created",
                    )
                }
                onSuccess?.invoke()
            } catch (e: ValidationError) {
                _state.update { it.copy(isLoading = false, error = "${e.code}: ${e.message}", message = null) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error", message = null) }
            }
        }
    }

    private fun currentMonth(): String {
        val today = LocalDate.now()
        return "%04d-%02d".format(today.year, today.monthValue)
    }
}

data class TenantOnboardingUiState(
    val name: String = "",
    val phone: String = "",
    val flatLabel: String = "",
    val monthlyRent: String = "",
    val isActive: Boolean = true,
    val notes: String = "",
    val billingStartMonth: String = "",
    val initialDue: String = "0.00",
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)
