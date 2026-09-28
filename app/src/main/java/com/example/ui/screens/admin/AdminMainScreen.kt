package com.example.ui.screens.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.ui.screens.incharge.ExcelImportExportDialog
import com.example.data.model.User
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudOff
import com.example.ui.components.CloudSyncDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Badge
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryConstants
import com.example.data.model.Site
import com.example.data.model.TransactionEntry
import com.example.ui.components.DateFilterUtils
import com.example.ui.components.DateRangeFilterCard
import com.example.ui.components.DayBookPdfExportDialog
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.PaymentModeBadge
import com.example.ui.components.ReceiptViewerDialog
import com.example.ui.components.RpvcBrandLogo
import com.example.ui.components.StatCard
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.formatCurrency
import com.example.ui.components.NavDestination
import com.example.ui.components.ResponsiveAppShell
import com.example.ui.components.ResponsiveTransactionsTable
import com.example.ui.components.ResponsiveWindowInfo
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedContainer
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OnExpenseRedContainer
import com.example.ui.theme.OnIncomeGreenContainer
import com.example.ui.theme.SlateContainer
import com.example.ui.theme.SlateSecondary
import com.example.ui.viewmodel.CategorySummaryStats
import com.example.ui.viewmodel.ConstructionViewModel
import com.example.ui.viewmodel.InchargeSummaryStats
import com.example.ui.viewmodel.OverallFinancialSummary
import com.example.ui.viewmodel.PaymentModeStats
import com.example.ui.viewmodel.SiteSummaryStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    viewModel: ConstructionViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val sites by viewModel.allSites.collectAsStateWithLifecycle()
    val summary by viewModel.adminSummary.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.adminFilteredTransactions.collectAsStateWithLifecycle()
    val siteStats by viewModel.adminSiteStats.collectAsStateWithLifecycle()
    val categoryStats by viewModel.adminCategoryExpenseStats.collectAsStateWithLifecycle()
    val paymentModeStats by viewModel.adminPaymentModeStats.collectAsStateWithLifecycle()

    val selectedSiteId by viewModel.adminSelectedSiteId.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.adminSelectedCategory.collectAsStateWithLifecycle()
    val selectedPaymentMode by viewModel.adminSelectedPaymentMode.collectAsStateWithLifecycle()
    val adminFromDate by viewModel.adminFromDate.collectAsStateWithLifecycle()
    val adminToDate by viewModel.adminToDate.collectAsStateWithLifecycle()
    val adminSearchQuery by viewModel.adminSearchQuery.collectAsStateWithLifecycle()

    var selectedAdminTab by remember { mutableIntStateOf(0) } // 0 = Dashboard, 1 = Sites, 2 = Categories, 3 = Day Book, 4 = Salary
    var selectedTxForReceipt by remember { mutableStateOf<TransactionEntry?>(null) }
    var selectedTxForEdit by remember { mutableStateOf<TransactionEntry?>(null) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var showImportExportDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var siteDropdownExpanded by remember { mutableStateOf(false) }

    val isCloudConfigured by viewModel.isCloudConfigured.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    if (showCloudSyncDialog) {
        CloudSyncDialog(
            viewModel = viewModel,
            onDismiss = { showCloudSyncDialog = false }
        )
    }

    if (showImportExportDialog) {
        ImportExportDialog(
            viewModel = viewModel,
            onDismiss = { showImportExportDialog = false }
        )
    }

    if (selectedTxForEdit != null) {
        EditTransactionDialog(
            transaction = selectedTxForEdit!!,
            sites = sites,
            onSave = { updatedTx ->
                viewModel.updateTransaction(updatedTx)
                selectedTxForEdit = null
            },
            onDelete = { txId ->
                viewModel.deleteTransaction(txId)
                selectedTxForEdit = null
            },
            onDismiss = { selectedTxForEdit = null }
        )
    }

    if (showLogoutConfirmation) {
        Dialog(onDismissRequest = { showLogoutConfirmation = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Confirm Logout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Are you sure you want to sign out from Admin Console?", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showLogoutConfirmation = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                showLogoutConfirmation = false
                                viewModel.logout()
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                        ) {
                            Text("Logout")
                        }
                    }
                }
            }
        }
    }

    if (selectedTxForReceipt != null) {
        ReceiptViewerDialog(
            tx = selectedTxForReceipt!!,
            onDismiss = { selectedTxForReceipt = null },
            onEdit = { tx ->
                selectedTxForReceipt = null
                selectedTxForEdit = tx
            }
        )
    }

    val navDestinations = remember {
        listOf(
            NavDestination(0, "Overview", Icons.Default.Dashboard, "nav_admin_overview"),
            NavDestination(1, "Sites", Icons.Default.Apartment, "nav_admin_sites"),
            NavDestination(2, "Categories", Icons.Default.Category, "nav_admin_categories"),
            NavDestination(3, "Day Book", Icons.AutoMirrored.Filled.ReceiptLong, "nav_admin_daybook"),
            NavDestination(4, "Salary", Icons.Default.Payments, "nav_admin_salary")
        )
    }

    ResponsiveAppShell(
        items = navDestinations,
        selectedIndex = selectedAdminTab,
        onItemSelected = { selectedAdminTab = it },
        brandSubtitle = "Admin Console",
        userName = currentUser?.name ?: "Director",
        userRole = "Administrator",
        onLogoutClick = { showLogoutConfirmation = true },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RpvcBrandLogo(size = 36.dp)
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "R P V C • Admin Console",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SlateSecondary
                                ) {
                                    Text(
                                        text = "AUDIT",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${currentUser?.name ?: "Director"} • Multi-Site Overview",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCloudSyncDialog = true },
                        modifier = Modifier.testTag("btn_admin_cloud_sync")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isCloudConfigured) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = "Cloud Sync",
                                tint = if (isCloudConfigured) ForestGreen else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    IconButton(
                        onClick = { showImportExportDialog = true },
                        modifier = Modifier.testTag("btn_admin_import_export")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Import / Export",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    IconButton(
                        onClick = { showLogoutConfirmation = true },
                        modifier = Modifier.testTag("btn_admin_logout")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    ) { windowInfo ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Global Filter Header Row (Filter by Site & Date)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Site Filter Dropdown
                    ExposedDropdownMenuBox(
                        expanded = siteDropdownExpanded,
                        onExpandedChange = { siteDropdownExpanded = !siteDropdownExpanded }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .menuAnchor()
                                .testTag("admin_site_filter_dropdown")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    text = sites.firstOrNull { it.id == selectedSiteId }?.let { "${it.code} (${it.name})" } ?: "All Sites (Multi-Project)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteDropdownExpanded)
                            }
                        }

                        ExposedDropdownMenu(
                            expanded = siteDropdownExpanded,
                            onDismissRequest = { siteDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Sites (Consolidated View)", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    viewModel.setAdminSiteFilter(null)
                                    siteDropdownExpanded = false
                                }
                            )
                            HorizontalDivider()
                            sites.forEach { site ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${site.name} [${site.code}]", fontWeight = FontWeight.SemiBold)
                                            Text("Incharge: ${site.inchargeName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        viewModel.setAdminSiteFilter(site.id)
                                        siteDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Range Active Indicator / Filter
                    val isDateFilterActive = adminFromDate != null || adminToDate != null
                    val dateLabel = when {
                        adminFromDate != null && adminToDate != null && adminFromDate == adminToDate -> adminFromDate!!
                        adminFromDate != null && adminToDate != null -> "$adminFromDate → $adminToDate"
                        adminFromDate != null -> "From: $adminFromDate"
                        adminToDate != null -> "To: $adminToDate"
                        else -> "All Dates"
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDateFilterActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable {
                                if (selectedAdminTab != 3) {
                                    selectedAdminTab = 3
                                }
                            }
                            .testTag("admin_date_filter_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isDateFilterActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDateFilterActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isDateFilterActive) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.clearAdminDateRange() }
                                )
                            }
                        }
                    }
                }
            }

            // Sub-Screen Content
            when (selectedAdminTab) {
                0 -> AdminOverviewDashboard(
                    summary = summary,
                    paymentModeStats = paymentModeStats,
                    recentTransactions = filteredTransactions.take(6),
                    onViewReceipt = { selectedTxForReceipt = it },
                    onNavigateToDayBook = { selectedAdminTab = 3 },
                    onEditTransaction = { selectedTxForEdit = it },
                    isWideScreen = windowInfo.isWideScreen
                )
                1 -> AdminSitesComparisonView(siteStats = siteStats, viewModel = viewModel)
                2 -> AdminCategoryBreakdownView(
                    categoryStats = categoryStats,
                    summary = summary
                )
                3 -> AdminMasterDayBookView(
                    transactions = filteredTransactions,
                    searchQuery = adminSearchQuery,
                    onSearchChange = { viewModel.setAdminSearch(it) },
                    fromDate = adminFromDate,
                    toDate = adminToDate,
                    onDateRangeSelected = { f, t -> viewModel.setAdminDateRange(f, t) },
                    selectedCategory = selectedCategory,
                    onCategoryChange = { viewModel.setAdminCategoryFilter(it) },
                    selectedPaymentMode = selectedPaymentMode,
                    onPaymentModeChange = { viewModel.setAdminPaymentModeFilter(it) },
                    onViewReceipt = { selectedTxForReceipt = it },
                    viewModel = viewModel,
                    onEditTransaction = { selectedTxForEdit = it },
                    isWideScreen = windowInfo.isWideScreen
                )
                4 -> com.example.ui.screens.incharge.StaffAttendanceAndSalaryScreen(viewModel = viewModel, isAdminView = true)
            }
        }
    }
}

@Composable
private fun AdminOverviewDashboard(
    summary: OverallFinancialSummary,
    paymentModeStats: List<PaymentModeStats>,
    recentTransactions: List<TransactionEntry>,
    onViewReceipt: (TransactionEntry) -> Unit,
    onNavigateToDayBook: () -> Unit,
    onEditTransaction: ((TransactionEntry) -> Unit)? = null,
    isWideScreen: Boolean = false
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Net Balance Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSecondary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Company Closing Balance (Net Cash)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (summary.netBalance >= 0) IncomeGreenContainer else ExpenseRedContainer
                        ) {
                            Text(
                                text = if (summary.netBalance >= 0) "SURPLUS" else "DEFICIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (summary.netBalance >= 0) OnIncomeGreenContainer else OnExpenseRedContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = formatCurrency(summary.netBalance),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Income", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(formatCurrency(summary.totalIncome), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IncomeGreenContainer)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Expenses", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(formatCurrency(summary.totalExpense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseRedContainer)
                        }
                    }
                }
            }
        }

        // Cash / Bank / UPI Summary Cards
        item {
            Text(
                text = "Liquidity & Payment Mode Balances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (isWideScreen) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Cash in Hand",
                        amount = summary.cashBalance,
                        icon = Icons.Default.Payments,
                        iconColor = AmberPrimary,
                        containerColor = AmberContainer,
                        contentColor = OnAmberContainer,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Bank Balance",
                        amount = summary.bankBalance,
                        icon = Icons.Default.AccountBalance,
                        iconColor = Color(0xFF4F46E5),
                        containerColor = Color(0xFFEEF2FF),
                        contentColor = Color(0xFF312E81),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "UPI Balance",
                        amount = summary.upiBalance,
                        icon = Icons.Default.QrCode,
                        iconColor = Color(0xFF9333EA),
                        containerColor = Color(0xFFFAF5FF),
                        contentColor = Color(0xFF581C87),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Other Payments",
                        amount = summary.otherBalance,
                        icon = Icons.Default.CreditCard,
                        iconColor = SlateSecondary,
                        containerColor = SlateContainer,
                        contentColor = SlateSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Cash in Hand",
                        amount = summary.cashBalance,
                        icon = Icons.Default.Payments,
                        iconColor = AmberPrimary,
                        containerColor = AmberContainer,
                        contentColor = OnAmberContainer,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Bank Balance",
                        amount = summary.bankBalance,
                        icon = Icons.Default.AccountBalance,
                        iconColor = Color(0xFF4F46E5),
                        containerColor = Color(0xFFEEF2FF),
                        contentColor = Color(0xFF312E81),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "UPI Balance",
                        amount = summary.upiBalance,
                        icon = Icons.Default.QrCode,
                        iconColor = Color(0xFF9333EA),
                        containerColor = Color(0xFFFAF5FF),
                        contentColor = Color(0xFF581C87),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Other Payments",
                        amount = summary.otherBalance,
                        icon = Icons.Default.CreditCard,
                        iconColor = SlateSecondary,
                        containerColor = SlateContainer,
                        contentColor = SlateSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Major Expense Highlights (Material, Labour, Machinery, Fuel)
        item {
            Text(
                text = "Key Expense Pillars",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Material Purchase",
                    amount = summary.materialExpense,
                    icon = Icons.Default.Apartment,
                    iconColor = ExpenseRed,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Site Labour",
                    amount = summary.labourExpense,
                    icon = Icons.Default.Engineering,
                    iconColor = AmberPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Machinery & Equip",
                    amount = summary.machineryExpense,
                    icon = Icons.Default.Construction,
                    iconColor = Color(0xFF0284C7),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Machinery Fuel",
                    amount = summary.fuelExpense,
                    icon = Icons.Default.LocalGasStation,
                    iconColor = Color(0xFFEA580C),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Loan Tracking Section (Loan Received vs Loan Return)
        item {
            Text(
                text = "Loan Ledger Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Loan Received (Inflow)", style = MaterialTheme.typography.bodySmall, color = IncomeGreen)
                            Text(formatCurrency(summary.loanReceived), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IncomeGreen)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Loan Return (Repaid)", style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                            Text(formatCurrency(summary.loanReturn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
                        }
                    }

                    val outstanding = summary.loanReceived - summary.loanReturn
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Outstanding Loan Balance", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(formatCurrency(outstanding), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (outstanding > 0) ExpenseRed else IncomeGreen)
                    }
                }
            }
        }

        // Recent Entries Across All Sites
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Latest Multi-Site Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToDayBook) {
                    Text("Full Day Book")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions found for the selected filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(recentTransactions, key = { it.id }) { tx ->
                TransactionItemCard(tx = tx, onViewReceipt = onViewReceipt, onEdit = onEditTransaction)
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AdminSitesComparisonView(
    siteStats: List<SiteSummaryStats>,
    viewModel: ConstructionViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedSiteForEdit by remember { mutableStateOf<Site?>(null) }
    var sitePendingDelete by remember { mutableStateOf<Site?>(null) }

    if (showDialog) {
        CreateEditSiteDialog(
            site = selectedSiteForEdit,
            viewModel = viewModel,
            onDismiss = {
                showDialog = false
                selectedSiteForEdit = null
            }
        )
    }

    if (sitePendingDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { sitePendingDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = ExpenseRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Project / Site",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${sitePendingDelete?.name}'? This will permanently remove this project."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        sitePendingDelete?.let { s ->
                            viewModel.deleteSite(s.id)
                        }
                        sitePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier.testTag("btn_confirm_delete_site")
                ) {
                    Text("Delete Project", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sitePendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Projects & Sites Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage construction sites, budgets & assignments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        selectedSiteForEdit = null
                        showDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_add_project_site")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Project", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(siteStats, key = { it.site.id }) { stat ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_site_card_${stat.site.id}")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stat.site.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = stat.site.code,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Client: ${stat.site.clientName} • Location: ${stat.site.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Assigned Incharge: ${stat.site.inchargeName} • Budget: ${formatCurrency(stat.site.budget)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    selectedSiteForEdit = stat.site
                                    showDialog = true
                                },
                                modifier = Modifier.testTag("btn_edit_site_${stat.site.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Site", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = {
                                    sitePendingDelete = stat.site
                                },
                                modifier = Modifier.testTag("btn_delete_site_${stat.site.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Site", tint = ExpenseRed)
                            }
                        }
                    }

                    // Incharge Credentials Info Card for Admin
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val context = LocalContext.current
                        val inchargeMob = stat.site.mobile.ifBlank { "Not Set" }
                        val inchargePass = stat.site.password.ifBlank { "123456" }

                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Incharge Login Credentials",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Mobile: $inchargeMob  |  Password: $inchargePass",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Incharge Login", "Site: ${stat.site.name}\nMobile: $inchargeMob\nPassword: $inchargePass")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Credentials copied for ${stat.site.name}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Credentials",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Income", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                            Text(formatCurrency(stat.totalIncome), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = IncomeGreen)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Expense", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                            Text(formatCurrency(stat.totalExpense), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Site Net Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                formatCurrency(stat.netBalance),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (stat.netBalance >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    // Progress Bar for Expense vs Income
                    if (stat.totalIncome > 0) {
                        val ratio = (stat.totalExpense / stat.totalIncome).toFloat().coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Budget Utilization vs Income", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(ratio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = if (ratio > 0.9f) ExpenseRed else AmberPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CreateEditSiteDialog(
    site: Site?,
    viewModel: ConstructionViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(site?.name ?: "") }
    var code by remember { mutableStateOf(site?.code ?: "") }
    var location by remember { mutableStateOf(site?.location ?: "") }
    var clientName by remember { mutableStateOf(site?.clientName ?: "") }
    var budgetStr by remember { mutableStateOf(site?.budget?.toString() ?: "500000") }
    var inchargeName by remember { mutableStateOf(site?.inchargeName ?: "") }
    var inchargeMobile by remember { mutableStateOf(site?.mobile ?: "") }
    var inchargePassword by remember { mutableStateOf(site?.password ?: "123456") }
    var passwordVisible by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && site != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = ExpenseRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Delete Project", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete '${site.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSite(site.id) {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (site == null) "Add New Construction Project / Site" else "Edit Project / Site",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project / Site Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_site_name")
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Site Code (e.g. PRJ-01)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_site_code")
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_site_location")
                )

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Client Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_client_name")
                )

                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { budgetStr = it },
                    label = { Text("Allocated Budget (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_site_budget")
                )

                OutlinedTextField(
                    value = inchargeName,
                    onValueChange = { inchargeName = it },
                    label = { Text("Assigned Incharge Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_site_incharge")
                )

                OutlinedTextField(
                    value = inchargeMobile,
                    onValueChange = { inchargeMobile = it },
                    label = { Text("Incharge Mobile No. (Login ID)") },
                    placeholder = { Text("e.g. 9876500005") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_incharge_mobile")
                )

                OutlinedTextField(
                    value = inchargePassword,
                    onValueChange = { inchargePassword = it },
                    label = { Text("Incharge Login Password / PIN") },
                    placeholder = { Text("e.g. 123456") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_incharge_password")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Used by Incharge to log into site console",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val randomPin = (100000..999999).random().toString()
                            inchargePassword = randomPin
                        }
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto PIN", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (site != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                            modifier = Modifier.testTag("btn_delete_site_dialog")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = ExpenseRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = ExpenseRed)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.saveSite(
                                    id = site?.id ?: 0,
                                    name = name,
                                    code = code,
                                    location = location,
                                    clientName = clientName,
                                    budgetStr = budgetStr,
                                    inchargeName = inchargeName,
                                    inchargeMobile = inchargeMobile,
                                    inchargePassword = inchargePassword,
                                    onSuccess = onDismiss
                                )
                            },
                            modifier = Modifier.testTag("btn_save_site")
                        ) {
                            Text(if (site == null) "Create Project" else "Update Project")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCategoryBreakdownView(
    categoryStats: List<CategorySummaryStats>,
    summary: OverallFinancialSummary
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_category_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Category-Wise Expense Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Complete audit of where company project funds are spent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSecondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Project Expenses", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                        Text(formatCurrency(summary.totalExpense), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${categoryStats.size} Categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (categoryStats.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No category expense data found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(categoryStats, key = { it.category }) { cat ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(AmberContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = OnAmberContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = cat.category,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${cat.count} transactions recorded",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatCurrency(cat.totalAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.1f", cat.percentage)}% of total",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (cat.percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = ExpenseRed,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminMasterDayBookView(
    transactions: List<TransactionEntry>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    fromDate: String?,
    toDate: String?,
    onDateRangeSelected: (String?, String?) -> Unit,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    selectedPaymentMode: String?,
    onPaymentModeChange: (String?) -> Unit,
    onViewReceipt: (TransactionEntry) -> Unit,
    viewModel: ConstructionViewModel,
    onEditTransaction: ((TransactionEntry) -> Unit)? = null,
    isWideScreen: Boolean = false
) {
    val context = LocalContext.current
    var showExcelDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }
    var viewMode by remember(isWideScreen) { mutableStateOf(if (isWideScreen) "TABLE" else "CARDS") }

    if (showPdfDialog) {
        DayBookPdfExportDialog(
            transactions = transactions,
            fromDate = fromDate,
            toDate = toDate,
            siteName = "All Sites (Admin Audit)",
            onDismiss = { showPdfDialog = false }
        )
    }

    if (showExcelDialog) {
        ExcelImportExportDialog(
            viewModel = viewModel,
            currentTransactions = transactions,
            onDismiss = { showExcelDialog = false }
        )
    }

    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val closingBalance = totalIncome - totalExpense

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_daybook_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Date Range Filter (From Date & To Date)
        item {
            DateRangeFilterCard(
                fromDate = fromDate,
                toDate = toDate,
                onDateRangeSelected = onDateRangeSelected
            )
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search by Party / Vendor Name, site, item, amount...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_search_daybook"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Payment Mode Filter Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mode:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                FilterChip(
                    selected = selectedPaymentMode == null,
                    onClick = { onPaymentModeChange(null) },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall) }
                )
                CategoryConstants.PAYMENT_MODES.forEach { mode ->
                    FilterChip(
                        selected = selectedPaymentMode == mode,
                        onClick = { onPaymentModeChange(if (selectedPaymentMode == mode) null else mode) },
                        label = { Text(mode, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Closing Balance Summary Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalIncome), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = IncomeGreenContainer)
                    }

                    HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalExpense), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ExpenseRedContainer)
                    }

                    HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Closing Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            formatCurrency(closingBalance),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (closingBalance >= 0) IncomeGreenContainer else ExpenseRedContainer
                        )
                    }
                }
            }
        }

        // Transactions list count summary with PDF and Excel Tools + View Mode Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Day Book (${transactions.size} entries)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle Table vs Cards
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (viewMode == "TABLE") MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewMode = "TABLE" }
                        ) {
                            Text(
                                "Table",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (viewMode == "TABLE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (viewMode == "CARDS") MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewMode = "CARDS" }
                        ) {
                            Text(
                                "Cards",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (viewMode == "CARDS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showPdfDialog = true },
                        modifier = Modifier.testTag("btn_admin_pdf"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { showExcelDialog = true },
                        modifier = Modifier.testTag("btn_admin_excel"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No day book records matching selected filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (viewMode == "TABLE") {
            item {
                ResponsiveTransactionsTable(
                    transactions = transactions,
                    onViewReceipt = onViewReceipt,
                    onEditTransaction = onEditTransaction
                )
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                TransactionItemCard(tx = tx, onViewReceipt = onViewReceipt, onEdit = onEditTransaction)
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ImportExportDialog(
    viewModel: ConstructionViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var exportJsonText by remember { mutableStateOf("") }
    var importJsonText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Export, 1 = Import
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        exportJsonText = viewModel.exportDatabaseJson()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Data Backup & Restore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Export", fontSize = 13.sp, color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Import", fontSize = 13.sp, color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 2) ExpenseRed else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Clean DB", fontSize = 13.sp, color = if (selectedTab == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (statusMessage != null) {
                    Surface(
                        color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (selectedTab == 0) {
                    Text(
                        text = "Export all sites and transaction records as a backup JSON string:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = exportJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("SiteKhata Backup", exportJsonText))
                                Toast.makeText(context, "Copied backup JSON to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Copy JSON")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "SiteKhata Backup JSON")
                                    putExtra(Intent.EXTRA_TEXT, exportJsonText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Backup JSON"))
                            }
                        ) {
                            Text("Share / Save")
                        }
                    }
                } else if (selectedTab == 1) {
                    Text(
                        text = "Paste your backup JSON string below to restore/import sites and transactions:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("Paste JSON backup here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (importJsonText.isBlank()) {
                                    statusMessage = "Please paste valid JSON backup text."
                                    isSuccess = false
                                    return@Button
                                }
                                viewModel.importDatabaseJson(importJsonText) { success, msg ->
                                    isSuccess = success
                                    statusMessage = msg
                                    if (success) {
                                        importJsonText = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Import & Restore")
                        }
                    }
                } else {
                    Text(
                        text = "Production Reset: Clear all mock transactions, dummy staff attendance, and worker entries to start fresh with clean daybook and payroll.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.clearAllOperationalData {
                                statusMessage = "All operational entries (Day Book, Staff, Attendance) cleared successfully!"
                                isSuccess = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Purge All Day Book & Staff Records", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}



