package com.testapp.renttracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.testapp.renttracker.model.PaymentComponent
import com.testapp.renttracker.presentation.billing.MonthlyBillingViewModel
import com.testapp.renttracker.presentation.dashboard.DashboardViewModel
import com.testapp.renttracker.presentation.payment.PaymentDraft
import com.testapp.renttracker.presentation.payment.PaymentViewModel
import java.time.LocalDate

private enum class RootTab { Billing, Payment, Dashboard }

@Composable
fun RentTrackerApp(
    billingViewModel: MonthlyBillingViewModel,
    paymentViewModel: PaymentViewModel,
    dashboardViewModel: DashboardViewModel,
) {
    var selectedTab by remember { mutableStateOf(RootTab.Billing) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            RootTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.name) },
                )
            }
        }

        when (selectedTab) {
            RootTab.Billing -> BillingScreen(billingViewModel)
            RootTab.Payment -> PaymentScreen(paymentViewModel)
            RootTab.Dashboard -> DashboardScreen(dashboardViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillingScreen(viewModel: MonthlyBillingViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var monthId by remember { mutableStateOf("2026-02") }
    var billAmount by remember { mutableStateOf("1200.00") }
    var f1Units by remember { mutableStateOf("40") }
    var f2Units by remember { mutableStateOf("60") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Monthly Billing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = monthId,
            onValueChange = {
                monthId = it
                viewModel.setMonth(it)
            },
            label = { Text("Month (YYYY-MM)") },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = billAmount,
            onValueChange = { billAmount = it },
            label = { Text("Electricity Bill") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.createMonth() }) { Text("Create Month") }
            Button(onClick = { viewModel.setElectricityBill(billAmount) }) { Text("Save Bill") }
        }

        OutlinedTextField(
            value = f1Units,
            onValueChange = { f1Units = it },
            label = { Text("Units F1") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = f2Units,
            onValueChange = { f2Units = it },
            label = { Text("Units F2") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.setFlatUnits("F1", f1Units) }) { Text("Save F1") }
            Button(onClick = { viewModel.setFlatUnits("F2", f2Units) }) { Text("Save F2") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.computeCharges() }) { Text("Compute") }
            Button(onClick = { viewModel.finalizeMonth() }) { Text("Finalize") }
        }

        if (state.isFinalized) {
            Text("Month finalized", color = MaterialTheme.colorScheme.primary)
        }
        if (state.error != null) {
            Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PaymentScreen(viewModel: PaymentViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var tenantId by remember { mutableStateOf("T1") }
    var monthId by remember { mutableStateOf("2026-02") }
    var amount by remember { mutableStateOf("500.00") }
    var paidOn by remember { mutableStateOf("2026-02-10") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Payments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(value = tenantId, onValueChange = { tenantId = it }, label = { Text("Tenant ID") })
        OutlinedTextField(value = monthId, onValueChange = { monthId = it }, label = { Text("Month") })
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") })
        OutlinedTextField(
            value = paidOn,
            onValueChange = { paidOn = it },
            label = { Text("Paid On (YYYY-MM-DD)") },
        )

        Button(
            onClick = {
                val parsedDate = runCatching { LocalDate.parse(paidOn) }.getOrNull()
                viewModel.setDraft(
                    PaymentDraft(
                        tenantId = tenantId,
                        billingMonthId = monthId,
                        component = PaymentComponent.Combined,
                        amountPaid = amount,
                        paidOn = parsedDate,
                    )
                )
                viewModel.recordPayment()
            }
        ) {
            Text("Record Payment")
        }

        Button(onClick = { viewModel.recomputeBalance(monthId) }) {
            Text("Recompute Balance")
        }

        state.latestBalances.forEach { balance ->
            Text("${balance.tenantId}: ${balance.balanceAmount}")
        }

        if (state.error != null) {
            Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var monthId by remember { mutableStateOf("2026-02") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(value = monthId, onValueChange = { monthId = it }, label = { Text("Month") })
        Button(onClick = { viewModel.refresh(monthId) }) { Text("Refresh") }

        Spacer(modifier = Modifier.height(6.dp))

        val summary = state.summary
        if (summary != null) {
            Text("Expected: ${summary.totalExpected}")
            Text("Paid: ${summary.totalPaid}")
            Text("Pending: ${summary.totalPending}")
            Text("Paid Tenants: ${summary.paidTenantCount}/${summary.totalTenantCount}")
            Text("Unpaid IDs: ${summary.unpaidTenantIds.joinToString()}")
        }

        if (state.error != null) {
            Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}
