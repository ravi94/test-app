package com.testapp.renttracker.presentation.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.PaymentComponent
import com.testapp.renttracker.model.RecordPaymentInput
import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.model.TenantBalance
import com.testapp.renttracker.repo.TenantRepository
import com.testapp.renttracker.service.PaymentService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class PaymentViewModel(
    private val paymentService: PaymentService,
    private val tenantRepo: TenantRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(
        PaymentUiState(
            availableTenants = tenantRepo.getActiveTenants(),
        )
    )
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    fun setDraft(draft: PaymentDraft) {
        _state.update { it.copy(draft = draft) }
    }

    fun recordPayment() {
        val draft = _state.value.draft
        execute("Payment recorded") {
            paymentService.recordPayment(
                RecordPaymentInput(
                    tenantId = draft.tenantId,
                    billingMonthId = draft.billingMonthId,
                    component = draft.component,
                    amountPaid = draft.amountPaid.toBigDecimal(),
                    paidOn = draft.paidOn,
                    note = draft.note,
                )
            )
        }
    }

    fun recomputeBalance(monthId: String) {
        execute("Balance recomputed") {
            val balances = paymentService.recomputeTenantBalance(monthId)
            _state.update { it.copy(latestBalances = balances) }
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
}

data class PaymentDraft(
    val tenantId: String = "",
    val billingMonthId: String = "",
    val component: PaymentComponent = PaymentComponent.Combined,
    val amountPaid: String = "",
    val paidOn: LocalDate? = null,
    val note: String? = null,
)

data class PaymentUiState(
    val draft: PaymentDraft = PaymentDraft(),
    val availableTenants: List<Tenant> = emptyList(),
    val latestBalances: List<TenantBalance> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)
