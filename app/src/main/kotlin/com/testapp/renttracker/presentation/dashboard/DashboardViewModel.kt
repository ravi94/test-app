package com.testapp.renttracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testapp.renttracker.model.MonthSummary
import com.testapp.renttracker.model.TenantDashboardRow
import com.testapp.renttracker.service.DashboardQueryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardQueryService: DashboardQueryService,
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    fun refresh(monthId: String) {
        _state.update { it.copy(isLoading = true, error = null, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val summary = dashboardQueryService.getMonthSummary(monthId)
                _state.update { it.copy(summary = summary, isLoading = false, message = "Dashboard refreshed") }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error", message = null) }
            }
        }
    }

    fun loadTenantDetails(monthId: String) {
        _state.update { it.copy(isLoading = true, error = null, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tenantDetails = dashboardQueryService.getTenantDetails(monthId)
                _state.update {
                    it.copy(
                        tenantDetails = tenantDetails,
                        isLoading = false,
                        message = "Tenant details loaded",
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error", message = null) }
            }
        }
    }
}

data class DashboardUiState(
    val summary: MonthSummary? = null,
    val tenantDetails: List<TenantDashboardRow> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)
