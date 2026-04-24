package com.testapp.renttracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.testapp.renttracker.model.OverallTenantDashboardRow
import com.testapp.renttracker.model.PaymentComponent
import com.testapp.renttracker.model.TenantHistoryScreenData
import com.testapp.renttracker.model.TenantMonthlyAmountRow
import com.testapp.renttracker.model.TenantPaymentHistoryRow
import com.testapp.renttracker.presentation.billing.MonthlyBillingViewModel
import com.testapp.renttracker.presentation.dashboard.DashboardViewModel
import com.testapp.renttracker.presentation.payment.PaymentDraft
import com.testapp.renttracker.presentation.payment.PaymentViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private enum class RootTab { Billing, Payment, Dashboard }

@Composable
fun RentTrackerApp(
    billingViewModel: MonthlyBillingViewModel,
    paymentViewModel: PaymentViewModel,
    dashboardViewModel: DashboardViewModel,
) {
    var selectedTab by remember { mutableStateOf(RootTab.Billing) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
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
    var electricityRate by remember { mutableStateOf("12.00") }
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
            value = electricityRate,
            onValueChange = { electricityRate = it },
            label = { Text("Electricity Rate Per Unit") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.createMonth() }) { Text("Create Month") }
            Button(onClick = { viewModel.setElectricityRate(electricityRate) }) { Text("Save Rate") }
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

        if (state.isLoading) {
            Text("Working...")
        }
        if (state.message != null) {
            Text(state.message ?: "", color = MaterialTheme.colorScheme.primary)
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

        OutlinedTextField(
            value = tenantId,
            onValueChange = { tenantId = it },
            label = { Text("Tenant ID") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = monthId,
            onValueChange = { monthId = it },
            label = { Text("Month") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = paidOn,
            onValueChange = { paidOn = it },
            label = { Text("Paid On (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
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

        if (state.isLoading) {
            Text("Working...")
        }
        if (state.message != null) {
            Text(state.message ?: "", color = MaterialTheme.colorScheme.primary)
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
    val tenantHistory = state.selectedTenantHistory

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        if (tenantHistory == null) {
            Button(onClick = { viewModel.refresh() }) { Text("Refresh Overall Summary") }
        } else {
            Button(onClick = { viewModel.closeTenantHistory() }) { Text("Back To Summary") }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (state.isLoading) {
            Text("Working...")
        }
        if (state.message != null) {
            Text(state.message ?: "", color = MaterialTheme.colorScheme.primary)
        }

        if (tenantHistory != null) {
            TenantHistoryScreen(tenantHistory)
        } else {
            val summary = state.summary
            if (summary != null) {
                Text("Total Billed: ${summary.totalBilled}")
                Text("Paid: ${summary.totalPaid}")
                Text("Balance: ${summary.totalBalance}")
                Text("Paid Tenants: ${summary.paidTenantCount}/${summary.totalTenantCount}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Overall Summary Per Tenant", fontWeight = FontWeight.Bold)
                OverallTenantDetailsTable(
                    rows = summary.tenantRows,
                    onTenantClick = viewModel::openTenantHistory,
                )
            }
        }

        if (state.error != null) {
            Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun OverallTenantDetailsTable(
    rows: List<OverallTenantDashboardRow>,
    onTenantClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OverallTenantDetailsRow(
            tenant = "Tenant",
            billed = "Billed",
            paid = "Paid",
            balance = "Balance",
            status = "Status",
            isHeader = true,
        )
        rows.forEach { row ->
            OverallTenantDetailsRow(
                tenant = row.tenantName,
                billed = row.totalBilled.money(),
                paid = row.totalPaid.money(),
                balance = row.balance.money(),
                status = row.status.name,
                onClick = { onTenantClick(row.tenantId) },
            )
        }
    }
}

@Composable
private fun OverallTenantDetailsRow(
    tenant: String,
    billed: String,
    paid: String,
    balance: String,
    status: String,
    isHeader: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
    val rowColor = if (isHeader) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = rowColor,
        shape = RoundedCornerShape(8.dp),
        border = if (isHeader) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(tenant, modifier = Modifier.width(130.dp), fontWeight = fontWeight)
            Text(billed, modifier = Modifier.width(100.dp), fontWeight = fontWeight)
            Text(paid, modifier = Modifier.width(90.dp), fontWeight = fontWeight)
            Text(balance, modifier = Modifier.width(100.dp), fontWeight = fontWeight)
            Text(status, modifier = Modifier.width(90.dp), fontWeight = fontWeight)
        }
    }
}

@Composable
private fun TenantHistoryScreen(history: TenantHistoryScreenData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(history.tenantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Text("Payments", fontWeight = FontWeight.Bold)
        PaymentHistoryTable(history.payments)

        Text("Electricity", fontWeight = FontWeight.Bold)
        MonthlyAmountTable(rows = history.electricityCharges, amountHeader = "Electricity")

        Text("Rent", fontWeight = FontWeight.Bold)
        MonthlyAmountTable(rows = history.rentCharges, amountHeader = "Rent")

        Text("Total Payment Made: ${history.totalPayments.money()}", fontWeight = FontWeight.Bold)
        Text("Total Due: ${history.totalDue.money()}", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PaymentHistoryTable(rows: List<TenantPaymentHistoryRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PaymentHistoryRow(
            month = "Month",
            paidOn = "Paid On",
            component = "Component",
            amount = "Amount",
            isHeader = true,
        )
        if (rows.isEmpty()) {
            Text("No payments recorded.")
        } else {
            rows.forEach { row ->
                PaymentHistoryRow(
                    month = row.billingMonthId,
                    paidOn = row.paidOn.toString(),
                    component = row.component.name,
                    amount = row.amountPaid.money(),
                )
            }
        }
    }
}

@Composable
private fun PaymentHistoryRow(
    month: String,
    paidOn: String,
    component: String,
    amount: String,
    isHeader: Boolean = false,
) {
    val fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
    val rowColor = if (isHeader) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = rowColor,
        shape = RoundedCornerShape(8.dp),
        border = if (isHeader) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(month, modifier = Modifier.width(90.dp), fontWeight = fontWeight)
            Text(paidOn, modifier = Modifier.width(110.dp), fontWeight = fontWeight)
            Text(component, modifier = Modifier.width(110.dp), fontWeight = fontWeight)
            Text(amount, modifier = Modifier.width(90.dp), fontWeight = fontWeight)
        }
    }
}

@Composable
private fun MonthlyAmountTable(
    rows: List<TenantMonthlyAmountRow>,
    amountHeader: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MonthlyAmountRow(
            month = "Month",
            amount = amountHeader,
            isHeader = true,
        )
        if (rows.isEmpty()) {
            Text("No charges found.")
        } else {
            rows.forEach { row ->
                MonthlyAmountRow(
                    month = row.billingMonthId,
                    amount = row.amount.money(),
                )
            }
        }
    }
}

@Composable
private fun MonthlyAmountRow(
    month: String,
    amount: String,
    isHeader: Boolean = false,
) {
    val fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
    val rowColor = if (isHeader) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = rowColor,
        shape = RoundedCornerShape(8.dp),
        border = if (isHeader) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(month, modifier = Modifier.width(110.dp), fontWeight = fontWeight)
            Text(amount, modifier = Modifier.width(110.dp), fontWeight = fontWeight)
        }
    }
}

private fun BigDecimal.money(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()
