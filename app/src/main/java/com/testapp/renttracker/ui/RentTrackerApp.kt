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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    val monthId = state.monthId ?: "2026-02"
    val selectedTenant = state.availableTenants.firstOrNull { it.id == state.selectedTenantId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Monthly Billing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        TenantDropdownField(
            selectedTenantId = state.selectedTenantId,
            tenants = state.availableTenants,
            onTenantSelected = viewModel::setSelectedTenant,
        )

        MonthYearPickerField(
            value = monthId,
            label = "Billing Month",
            onValueChange = viewModel::setMonth,
        )

        OutlinedTextField(
            value = state.electricityRateInput,
            onValueChange = viewModel::setElectricityRateInput,
            label = { Text("Electricity Rate Per Unit") },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.selectedTenantUnitsInput,
            onValueChange = viewModel::setSelectedTenantUnitsInput,
            label = { Text("Units") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { viewModel.saveBillingEntry() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save Billing")
        }

        if (state.isLoading) {
            Text("Working...")
        }
        if (state.message != null) {
            Text(state.message ?: "", color = MaterialTheme.colorScheme.primary)
        }
        selectedTenant?.let { tenant ->
            Text("Selected Tenant: ${tenant.name} (${tenant.id})")
            if (state.selectedTenantUnitsInput.isNotBlank()) {
                Text("Saved Units For $monthId: ${state.selectedTenantUnitsInput}")
            }
        }
        if (state.error != null) {
            Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PaymentScreen(viewModel: PaymentViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val tenants = state.availableTenants

    var tenantId by remember { mutableStateOf(tenants.firstOrNull()?.id ?: "T1") }
    var monthId by remember { mutableStateOf("%04d-%02d".format(today.year, today.monthValue)) }
    var amount by remember { mutableStateOf("500.00") }
    var paidOn by remember { mutableStateOf(today.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Payments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        TenantDropdownField(
            selectedTenantId = tenantId,
            tenants = tenants,
            onTenantSelected = { tenantId = it },
        )
        MonthYearPickerField(
            value = monthId,
            label = "Payment Month",
            onValueChange = { monthId = it },
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
        )
        DatePickerField(
            value = paidOn,
            label = "Paid On",
            onValueChange = { paidOn = it },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TenantDropdownField(
    selectedTenantId: String,
    tenants: List<com.testapp.renttracker.model.Tenant>,
    onTenantSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTenant = tenants.firstOrNull { it.id == selectedTenantId }
    val displayValue = selectedTenant?.let { "${it.name} (${it.id})" } ?: selectedTenantId

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tenant") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            tenants.forEach { tenant ->
                DropdownMenuItem(
                    text = { Text("${tenant.name} (${tenant.id})") },
                    onClick = {
                        onTenantSelected(tenant.id)
                        expanded = false
                    },
                )
            }
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

@Composable
private fun MonthYearPickerField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    value.toDisplayMonthYear(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                "Select",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    Spacer(modifier = Modifier.height(2.dp))

    if (showPicker) {
        MonthYearPickerDialog(
            initialValue = value,
            onDismiss = { showPicker = false },
            onConfirm = {
                onValueChange(it)
                showPicker = false
            },
        )
    }
}

private fun String.toDisplayMonthYear(): String {
    val parts = split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return this
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return this
    val monthName = month.monthName()
    if (monthName.isEmpty()) return this
    return "$monthName $year"
}

private fun String.toDisplayDate(): String {
    val parsed = runCatching { LocalDate.parse(this) }.getOrNull() ?: return this
    return "${parsed.dayOfMonth} ${parsed.monthValue.monthName()} ${parsed.year}"
}

@Composable
private fun MonthYearPickerDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val today = LocalDate.now()
    val initialParts = initialValue.split("-")
    var selectedYear by remember {
        mutableStateOf(initialParts.getOrNull(0)?.toIntOrNull() ?: today.year)
    }
    var selectedMonth by remember {
        mutableStateOf(initialParts.getOrNull(1)?.toIntOrNull() ?: today.monthValue)
    }
    val years = remember(today.year) { (today.year - 5..today.year + 5).toList() }
    val months = remember { (1..12).map { it to it.monthName() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectionColumn(
                    title = "Month",
                    options = months.map { it.second },
                    selectedOption = selectedMonth.monthName(),
                    onSelect = { monthName ->
                        selectedMonth = months.first { it.second == monthName }.first
                    },
                    modifier = Modifier.weight(1f),
                )
                SelectionColumn(
                    title = "Year",
                    options = years.map { it.toString() },
                    selectedOption = selectedYear.toString(),
                    onSelect = { selectedYear = it.toInt() },
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm("%04d-%02d".format(selectedYear, selectedMonth)) }
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    value.toDisplayDate(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                "Select",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    Spacer(modifier = Modifier.height(2.dp))

    if (showPicker) {
        DatePickerFieldDialog(
            initialValue = value,
            onDismiss = { showPicker = false },
            onConfirm = {
                onValueChange(it)
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerFieldDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val initialDate = runCatching { LocalDate.parse(initialValue) }.getOrNull() ?: LocalDate.now()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val selectedDate = Instant.ofEpochMilli(selectedMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onConfirm(selectedDate.toString())
                    }
                },
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun SelectionColumn(
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(title, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option) },
                ) {
                    Text(
                        option,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

private fun Int.monthName(): String = when (this) {
    1 -> "January"
    2 -> "February"
    3 -> "March"
    4 -> "April"
    5 -> "May"
    6 -> "June"
    7 -> "July"
    8 -> "August"
    9 -> "September"
    10 -> "October"
    11 -> "November"
    12 -> "December"
    else -> ""
}

private fun BigDecimal.money(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()
