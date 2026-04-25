package com.testapp.renttracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testapp.renttracker.model.OverallDashboardSummary
import com.testapp.renttracker.model.TenantListItem
import com.testapp.renttracker.model.TenantHistoryScreenData
import com.testapp.renttracker.service.TenantManagementService
import com.testapp.renttracker.service.DashboardQueryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardQueryService: DashboardQueryService,
    private val tenantManagementService: TenantManagementService,
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    fun refresh() {
        _state.update { it.copy(isLoading = true, error = null, message = null, selectedTenantHistory = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val summary = dashboardQueryService.getOverallSummary()
                val tenants = tenantManagementService.getAllTenants().map { tenant ->
                    TenantListItem(
                        tenantId = tenant.id,
                        tenantName = tenant.name,
                        flatLabel = tenant.flatLabel,
                        monthlyRent = tenant.monthlyRent,
                        isActive = tenant.isActive,
                    )
                }
                _state.update {
                    it.copy(
                        summary = summary,
                        allTenants = tenants,
                        isLoading = false,
                        message = "Dashboard refreshed",
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error", message = null) }
            }
        }
    }

    fun openTenantHistory(tenantId: String) {
        _state.update { it.copy(isLoading = true, error = null, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tenantHistory = dashboardQueryService.getTenantHistory(tenantId)
                _state.update {
                    it.copy(
                        selectedTenantHistory = tenantHistory,
                        isLoading = false,
                        message = "Tenant history loaded",
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error", message = null) }
            }
        }
    }

    fun closeTenantHistory() {
        _state.update { it.copy(selectedTenantHistory = null, error = null, message = null) }
    }

    fun deleteTenant(tenantId: String, onSuccess: (() -> Unit)? = null) {
        _state.update { it.copy(isLoading = true, error = null, message = null, selectedTenantHistory = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tenantManagementService.deleteTenantAndData(tenantId)
                val summary = dashboardQueryService.getOverallSummary()
                val tenants = tenantManagementService.getAllTenants().map { tenant ->
                    TenantListItem(
                        tenantId = tenant.id,
                        tenantName = tenant.name,
                        flatLabel = tenant.flatLabel,
                        monthlyRent = tenant.monthlyRent,
                        isActive = tenant.isActive,
                    )
                }
                _state.update {
                    it.copy(
                        summary = summary,
                        allTenants = tenants,
                        isLoading = false,
                        message = "Tenant deleted",
                    )
                }
                onSuccess?.invoke()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error", message = null) }
            }
        }
    }
}

data class DashboardUiState(
    val summary: OverallDashboardSummary? = null,
    val allTenants: List<TenantListItem> = emptyList(),
    val selectedTenantHistory: TenantHistoryScreenData? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)
