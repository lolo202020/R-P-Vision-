package com.example.ui.screens.incharge

import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.LinearProgressIndicator
import com.example.ui.components.PaymentModeBadge
import com.example.ui.viewmodel.CategorySummaryStats
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudOff
import com.example.ui.components.CloudSyncDialog
import com.example.ui.components.EditInchargeProfileDialog
import com.example.ui.theme.ForestGreen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.data.model.TransactionEntry
import com.example.ui.components.ReceiptViewerDialog
import com.example.ui.components.RpvcBrandLogo
import com.example.ui.components.StatCard
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedContainer
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OnExpenseRedContainer
import com.example.ui.theme.OnIncomeGreenContainer
import com.example.ui.theme.SlateContainer
import com.example.ui.theme.SlateSecondary
import com.example.ui.viewmodel.ConstructionViewModel
import com.example.ui.components.NavDestination
import com.example.ui.components.ResponsiveAppShell

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteInchargeMainScreen(
    viewModel: ConstructionViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val stats by viewModel.inchargeStats.collectAsStateWithLifecycle()
    val transactions by viewModel.inchargeTransactions.collectAsStateWithLifecycle()
    val categoryStats by viewModel.inchargeCategoryExpenseStats.collectAsStateWithLifecycle()

    var selectedNavTab by remember { mutableIntStateOf(0) } // 0 = Dashboard, 1 = Add Entry, 2 = Day Book, 3 = Profile
    var addEntryInitialType by remember { mutableIntStateOf(0) } // 0 = Expense, 1 = Income
    var selectedTxForReceipt by remember { mutableStateOf<TransactionEntry?>(null) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }

    val isCloudConfigured by viewModel.isCloudConfigured.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val navDestinations = remember {
        listOf(
            NavDestination(0, "Dashboard", Icons.Default.Dashboard, "nav_incharge_dashboard"),
            NavDestination(1, "Add Entry", Icons.Default.Add, "nav_incharge_add"),
            NavDestination(2, "Day Book", Icons.AutoMirrored.Filled.ReceiptLong, "nav_incharge_daybook"),
            NavDestination(3, "Staff & Salary", Icons.Default.People, "nav_incharge_staff"),
            NavDestination(4, "Profile", Icons.Default.Person, "nav_incharge_profile")
        )
    }

    BackHandler(enabled = selectedNavTab != 0) {
        selectedNavTab = 0
    }

    if (showCloudSyncDialog) {
        CloudSyncDialog(
            viewModel = viewModel,
            onDismiss = { showCloudSyncDialog = false }
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
                    Text("Are you sure you want to sign out from the Site Incharge panel?", style = MaterialTheme.typography.bodyMedium)
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
            onDismiss = { selectedTxForReceipt = null }
        )
    }

    ResponsiveAppShell(
        items = navDestinations,
        selectedIndex = selectedNavTab,
        onItemSelected = { selectedNavTab = it },
        brandSubtitle = currentUser?.assignedSiteName ?: "Site Incharge",
        userName = currentUser?.name ?: "Incharge",
        userRole = "Site Incharge",
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
                                    text = currentUser?.assignedSiteName ?: "Assigned Site",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "R P V C • ${currentUser?.name ?: "Incharge"} • ${currentUser?.mobile ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCloudSyncDialog = true },
                        modifier = Modifier.testTag("btn_incharge_cloud_sync")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isCloudConfigured) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = "Cloud Sync",
                                tint = if (isCloudConfigured) ForestGreen else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    IconButton(
                        onClick = { showLogoutConfirmation = true },
                        modifier = Modifier.testTag("btn_incharge_logout")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (selectedNavTab == 0 || selectedNavTab == 2) {
                FloatingActionButton(
                    onClick = {
                        addEntryInitialType = 0
                        selectedNavTab = 1
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_entry")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense/Income")
                }
            }
        }
    ) { windowInfo ->
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            when (selectedNavTab) {
                0 -> InchargeDashboardView(
                    stats = stats,
                    recentTransactions = transactions.take(6),
                    categoryStats = categoryStats,
                    allTransactions = transactions,
                    onAddExpenseClick = {
                        addEntryInitialType = 0
                        selectedNavTab = 1
                    },
                    onAddIncomeClick = {
                        addEntryInitialType = 1
                        selectedNavTab = 1
                    },
                    onViewAllEntriesClick = { selectedNavTab = 2 },
                    onViewReceipt = { selectedTxForReceipt = it },
                    onOpenCategoryInDayBook = { cat ->
                        viewModel.setInchargeSearch(cat)
                        selectedNavTab = 2
                    },
                    isWideScreen = windowInfo.isWideScreen
                )
                1 -> AddEntryScreen(
                    viewModel = viewModel,
                    initialTab = addEntryInitialType,
                    onSubmitted = { selectedNavTab = 2 }
                )
                2 -> DayBookScreen(viewModel = viewModel)
                3 -> StaffAttendanceAndSalaryScreen(viewModel = viewModel)
                4 -> InchargeProfileView(
                    user = currentUser,
                    stats = stats,
                    viewModel = viewModel,
                    onLogout = { showLogoutConfirmation = true }
                )
            }
        }
    }
}

@Composable
private fun InchargeDashboardView(
    stats: com.example.ui.viewmodel.OverallFinancialSummary,
    recentTransactions: List<TransactionEntry>,
    categoryStats: List<CategorySummaryStats>,
    allTransactions: List<TransactionEntry>,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onViewAllEntriesClick: () -> Unit,
    onViewReceipt: (TransactionEntry) -> Unit,
    onOpenCategoryInDayBook: (String) -> Unit,
    isWideScreen: Boolean = false
) {
    var expandedCategories by remember { mutableStateOf(setOf<String>()) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Net Balance Card
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
                            text = "Site Closing Balance (Net Cash)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (stats.netBalance >= 0) IncomeGreenContainer else ExpenseRedContainer
                        ) {
                            Text(
                                text = if (stats.netBalance >= 0) "SURPLUS" else "DEFICIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (stats.netBalance >= 0) OnIncomeGreenContainer else OnExpenseRedContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = formatCurrency(stats.netBalance),
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
                            Text("Total Site Income", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(formatCurrency(stats.totalIncome), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IncomeGreenContainer)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Site Expense", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(formatCurrency(stats.totalExpense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseRedContainer)
                        }
                    }
                }
            }
        }

        // Quick Entry Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddExpenseClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_dash_add_expense")
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Expense", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddIncomeClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_dash_add_income")
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Income", fontWeight = FontWeight.Bold)
                }
            }
        }


        // Payment Mode Breakdown
        item {
            Text(
                text = "Site Payment Modes Summary",
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
                        amount = stats.cashBalance,
                        icon = Icons.Default.Payments,
                        iconColor = AmberPrimary,
                        containerColor = AmberContainer,
                        contentColor = OnAmberContainer,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Bank Account",
                        amount = stats.bankBalance,
                        icon = Icons.Default.AccountBalance,
                        iconColor = Color(0xFF4F46E5),
                        containerColor = Color(0xFFEEF2FF),
                        contentColor = Color(0xFF312E81),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "UPI Balance",
                        amount = stats.upiBalance,
                        icon = Icons.Default.QrCode,
                        iconColor = Color(0xFF9333EA),
                        containerColor = Color(0xFFFAF5FF),
                        contentColor = Color(0xFF581C87),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Material Spend",
                        amount = stats.materialExpense,
                        icon = Icons.Default.Apartment,
                        iconColor = ExpenseRed,
                        containerColor = ExpenseRedContainer,
                        contentColor = OnExpenseRedContainer,
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
                        amount = stats.cashBalance,
                        icon = Icons.Default.Payments,
                        iconColor = AmberPrimary,
                        containerColor = AmberContainer,
                        contentColor = OnAmberContainer,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Bank Account",
                        amount = stats.bankBalance,
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
                        amount = stats.upiBalance,
                        icon = Icons.Default.QrCode,
                        iconColor = Color(0xFF9333EA),
                        containerColor = Color(0xFFFAF5FF),
                        contentColor = Color(0xFF581C87),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Material Spend",
                        amount = stats.materialExpense,
                        icon = Icons.Default.Apartment,
                        iconColor = ExpenseRed,
                        containerColor = ExpenseRedContainer,
                        contentColor = OnExpenseRedContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Category-Wise Expense Breakdown Section
        if (categoryStats.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Category-Wise Expense Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap category to open and check entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(categoryStats, key = { "incharge_cat_${it.category}" }) { cat ->
                val isExpanded = expandedCategories.contains(cat.category)
                val catTxs = remember(allTransactions, cat.category) {
                    allTransactions.filter {
                        it.type == "EXPENSE" && it.category.equals(cat.category, ignoreCase = true)
                    }.sortedByDescending { it.dateMillis }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .testTag("incharge_card_cat_${cat.category.replace(" ", "_")}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clickable Header to open / close
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    expandedCategories = if (isExpanded) {
                                        expandedCategories - cat.category
                                    } else {
                                        expandedCategories + cat.category
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AmberContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = OnAmberContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = cat.category,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${cat.count} entries",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (isExpanded) "Tap to close ▲" else "Tap to open ▼",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
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
                                IconButton(
                                    onClick = {
                                        expandedCategories = if (isExpanded) {
                                            expandedCategories - cat.category
                                        } else {
                                            expandedCategories + cat.category
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
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

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Entries in ${cat.category} (${catTxs.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    TextButton(
                                        onClick = { onOpenCategoryInDayBook(cat.category) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Filter Day Book", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                if (catTxs.isEmpty()) {
                                    Text(
                                        text = "No detailed entries found for this category.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    catTxs.forEach { tx ->
                                        InchargeCategoryEntryItem(
                                            tx = tx,
                                            onViewReceipt = onViewReceipt
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Entries Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Site Entries",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewAllEntriesClick) {
                    Text("View Day Book")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No site entries recorded yet. Tap + to create your first transaction.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentTransactions, key = { it.id }) { tx ->
                TransactionItemCard(
                    tx = tx,
                    onViewReceipt = onViewReceipt
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun InchargeCategoryEntryItem(
    tx: TransactionEntry,
    onViewReceipt: (TransactionEntry) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = tx.dateFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    PaymentModeBadge(mode = tx.paymentMode)
                }

                Text(
                    text = "- ${formatCurrency(tx.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (tx.partyName.isNotBlank()) tx.partyName else "Unknown Party",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (tx.description.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = tx.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!tx.receiptPhotoUri.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { onViewReceipt(tx) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("View Receipt", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun InchargeProfileView(
    user: com.example.data.model.User?,
    stats: com.example.ui.viewmodel.OverallFinancialSummary,
    viewModel: ConstructionViewModel,
    onLogout: () -> Unit
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showEditProfileDialog && user != null) {
        EditInchargeProfileDialog(
            userId = user.id,
            siteId = user.assignedSiteId,
            siteName = user.assignedSiteName,
            initialName = user.name,
            initialMobile = user.mobile,
            initialPassword = user.password,
            initialDesignation = user.designation,
            viewModel = viewModel,
            onDismiss = { showEditProfileDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Info Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Engineering,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = user?.name ?: "Site Incharge",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = user?.designation ?: "Site Engineer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Mobile: ${user?.mobile ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Edit Profile Action Button
                    Button(
                        onClick = { showEditProfileDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_edit_incharge_profile")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Profile & Credentials",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    HorizontalDivider()

                    // Login Credentials Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val inchargeMob = user?.mobile?.ifBlank { "Not Set" } ?: "Not Set"
                        val inchargePass = user?.password?.ifBlank { "123456" } ?: "123456"

                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "My Sign-In Credentials",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { passwordVisible = !passwordVisible },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Hide PIN" else "Show PIN",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Incharge Sign-In", "Mobile: $inchargeMob\nPassword: $inchargePass")
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Credentials copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Login ID (Mobile)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(inchargeMob, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Password / PIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        if (passwordVisible) inchargePass else "••••••••",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Assigned Site Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = user?.assignedSiteName ?: "Assigned Site",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Site Access Rules & Security",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnAmberContainer
                            )
                            Text(
                                text = "✅ Add Site Income\n✅ Add Site Expenses & Bills\n✅ View Own Site Day Book & Records\n❌ Access to Other Project Sites is Restricted",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnAmberContainer
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onLogout,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_profile_logout")
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of RPVC Portal", fontWeight = FontWeight.Bold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
