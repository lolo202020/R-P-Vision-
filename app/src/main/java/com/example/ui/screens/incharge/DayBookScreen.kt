package com.example.ui.screens.incharge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntry
import com.example.ui.components.DayBookPdfExportDialog
import com.example.ui.components.DateRangeFilterCard
import com.example.ui.components.ReceiptViewerDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.formatCurrency
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.ConstructionViewModel
import com.example.util.DayBookExcelHelper
import com.example.util.DayBookPdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayBookScreen(
    viewModel: ConstructionViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.inchargeTransactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.inchargeSearchQuery.collectAsStateWithLifecycle()
    val typeFilter by viewModel.inchargeTypeFilter.collectAsStateWithLifecycle()
    val fromDate by viewModel.inchargeFromDate.collectAsStateWithLifecycle()
    val toDate by viewModel.inchargeToDate.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTxForReceipt by remember { mutableStateOf<TransactionEntry?>(null) }
    var showExcelDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }

    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    if (selectedTxForReceipt != null) {
        ReceiptViewerDialog(
            tx = selectedTxForReceipt!!,
            onDismiss = { selectedTxForReceipt = null }
        )
    }

    if (showPdfDialog) {
        DayBookPdfExportDialog(
            transactions = transactions,
            fromDate = fromDate,
            toDate = toDate,
            siteName = currentUser?.name?.let { "Site Incharge: $it" } ?: "Main Site",
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("incharge_daybook_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Excel & PDF Report Header Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Day Book Register",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showPdfDialog = true },
                        modifier = Modifier.testTag("btn_daybook_pdf"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download PDF", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { showExcelDialog = true },
                        modifier = Modifier.testTag("btn_daybook_excel"),
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

        // Date Range Filter (From Date & To Date)
        item {
            DateRangeFilterCard(
                fromDate = fromDate,
                toDate = toDate,
                onDateRangeSelected = { start, end ->
                    viewModel.setInchargeDateRange(start, end)
                }
            )
        }

        // Search Bar & Type Filter Chips
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setInchargeSearch(it) },
                placeholder = { Text("Search by Party / Vendor Name, category, amount...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setInchargeSearch("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("incharge_search_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Type Filter Chips Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("ALL" to "All Entries", "EXPENSE" to "Expenses", "INCOME" to "Income").forEach { (type, label) ->
                    val isSelected = typeFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setInchargeType(type) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("chip_type_$type")
                    )
                }
            }
        }

        // Summary Bar for filtered view
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
                        Text(formatCurrency(totalIncome), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }

                    HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalExpense), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }

                    HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Closing Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            formatCurrency(netBalance),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (netBalance >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }
        }

        // Transactions List
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "No Day Book entries found",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Use the + button or Add Entry tab to record site income/expenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                TransactionItemCard(
                    tx = tx,
                    onViewReceipt = { selectedTxForReceipt = it }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ExcelImportExportDialog(
    viewModel: ConstructionViewModel,
    currentTransactions: List<TransactionEntry>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fromDate by viewModel.inchargeFromDate.collectAsStateWithLifecycle()
    val toDate by viewModel.inchargeToDate.collectAsStateWithLifecycle()
    val allSites by viewModel.allSites.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Export XLS, 1 = Import XLS, 2 = Sample Template
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    var parsedResult by remember { mutableStateOf<DayBookExcelHelper.ImportResult?>(null) }
    var pickedFileName by remember { mutableStateOf<String?>(null) }

    val totalIncome = currentTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = currentTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            statusMessage = null
            pickedFileName = uri.lastPathSegment ?: "Selected Excel File"
            coroutineScope.launch {
                try {
                    val defaultSite = allSites.firstOrNull()
                    val siteId = defaultSite?.id ?: 1L
                    val siteName = defaultSite?.name ?: "Default Site"
                    val userId = currentUser?.id ?: 1L
                    val userName = currentUser?.name ?: "Incharge"
                    val userMobile = currentUser?.mobile ?: "9999999999"

                    val result = withContext(Dispatchers.IO) {
                        DayBookExcelHelper.parseExcelFile(
                            context = context,
                            uri = uri,
                            defaultSiteId = siteId,
                            defaultSiteName = siteName,
                            userId = userId,
                            userName = userName,
                            userMobile = userMobile
                        )
                    }
                    parsedResult = result
                    isProcessing = false
                    if (result.validEntries.isNotEmpty()) {
                        isSuccess = true
                        statusMessage = "Found ${result.validEntries.size} valid records in Excel file."
                    } else {
                        isSuccess = false
                        statusMessage = if (result.errors.isNotEmpty()) result.errors.first() else "No valid transaction rows found in Excel file."
                    }
                } catch (e: Exception) {
                    isProcessing = false
                    isSuccess = false
                    parsedResult = null
                    statusMessage = "Failed to parse Excel file: ${e.localizedMessage ?: "Invalid file format"}"
                }
            }
        }
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
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Day Book Excel (.xls) Tools",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                }

                // Tab Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            selectedTab = 0
                            statusMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Export XLS",
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            selectedTab = 1
                            statusMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Import XLS",
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            selectedTab = 2
                            statusMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Sample XLS",
                            color = if (selectedTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Status Banner
                if (statusMessage != null) {
                    Surface(
                        color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = statusMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // TAB 0: EXPORT REAL XLS
                if (selectedTab == 0) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Filtered Day Book Export Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Records:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${currentTransactions.size} entries", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Income (Credit):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(totalIncome), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Expense (Debit):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(totalExpense), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Closing Balance:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    formatCurrency(netBalance),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netBalance >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                            if (!fromDate.isNullOrBlank() || !toDate.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Filter Period:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${fromDate ?: "Start"} to ${toDate ?: "End"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Text(
                        text = "• Generates genuine Microsoft Excel .xls binary spreadsheet (MIME: application/vnd.ms-excel)\n• Columns: Date, Entry Type, Particular, Category, Sub Category, Site, Description, Payment Mode, Debit, Credit, Balance\n• Formats numeric cells and calculates closing totals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Button(
                        onClick = {
                            if (currentTransactions.isEmpty()) {
                                isSuccess = false
                                statusMessage = "No Day Book records found to export. Please add entries or adjust filters."
                                return@Button
                            }
                            isProcessing = true
                            coroutineScope.launch {
                                try {
                                    val siteName = allSites.firstOrNull()?.name ?: "All Sites"
                                    val file = withContext(Dispatchers.IO) {
                                        DayBookExcelHelper.exportDayBookToXls(
                                            context = context,
                                            transactions = currentTransactions,
                                            fromDate = fromDate,
                                            toDate = toDate,
                                            siteName = siteName
                                        )
                                    }
                                    isProcessing = false
                                    isSuccess = true
                                    statusMessage = "Day Book Excel exported successfully."
                                    Toast.makeText(context, "Day Book Excel exported successfully.", Toast.LENGTH_SHORT).show()
                                    DayBookExcelHelper.shareExcelFile(context, file, "Share Day Book Excel (.xls)")
                                } catch (e: Exception) {
                                    isProcessing = false
                                    isSuccess = false
                                    statusMessage = "Export failed: ${e.localizedMessage}"
                                    Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Excel .xls...")
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export & Share Excel (.xls)")
                        }
                    }
                }

                // TAB 1: IMPORT REAL XLS / XLSX
                else if (selectedTab == 1) {
                    if (parsedResult == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Select .xls or .xlsx Excel Workbook",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Supports standard Day Book columns (Date, Type, Particular, Debit, Credit, etc.)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Button(
                                    onClick = {
                                        filePickerLauncher.launch(arrayOf(
                                            "application/vnd.ms-excel",
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            "*/*"
                                        ))
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Choose Excel File")
                                }
                            }
                        }
                    } else {
                        // Display Parsed Result Preview
                        val result = parsedResult!!
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Parsed Excel File Preview",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Valid Records:", style = MaterialTheme.typography.bodySmall)
                                    Text("${result.validEntries.size} / ${result.totalRowsRead} rows", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Debit (Expenses):", style = MaterialTheme.typography.bodySmall)
                                    Text(formatCurrency(result.totalDebit), fontWeight = FontWeight.Bold, color = ExpenseRed)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Credit (Income):", style = MaterialTheme.typography.bodySmall)
                                    Text(formatCurrency(result.totalCredit), fontWeight = FontWeight.Bold, color = IncomeGreen)
                                }

                                if (result.errors.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "${result.errors.size} row warnings/skipped:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            result.errors.take(2).forEach { err ->
                                                Text("• $err", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Scrollable list preview of entries
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(result.validEntries) { entry ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = entry.type,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (entry.type == "INCOME") IncomeGreen else ExpenseRed
                                                )
                                                Text(entry.dateFormatted, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(
                                                text = entry.partyName.ifBlank { entry.category },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = formatCurrency(entry.amount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (entry.type == "INCOME") IncomeGreen else ExpenseRed
                                        )
                                    }
                                }
                            }
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    parsedResult = null
                                    statusMessage = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Pick Another", style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = {
                                    isProcessing = true
                                    viewModel.insertImportedTransactions(result.validEntries) { success, msg ->
                                        isProcessing = false
                                        isSuccess = success
                                        statusMessage = msg
                                        if (success) {
                                            Toast.makeText(context, "Day Book Excel imported successfully.", Toast.LENGTH_SHORT).show()
                                            parsedResult = null
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(8.dp),
                                enabled = result.validEntries.isNotEmpty() && !isProcessing
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Confirm Import (${result.validEntries.size})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // TAB 2: SAMPLE XLS
                else {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Official Day Book .xls Template",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Download our pre-structured Microsoft Excel workbook (.xls) with standard columns and 4 sample construction entries:\n" +
                                        "1. Client Mobilisation Advance (₹5,00,000 Income)\n" +
                                        "2. Cement & Steel Materials (₹42,000 Expense)\n" +
                                        "3. Labor Union Daily Wages (₹18,500 Expense)\n" +
                                        "4. JCB Excavator Diesel Fuel (₹6,750 Expense)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isProcessing = true
                            coroutineScope.launch {
                                try {
                                    val file = withContext(Dispatchers.IO) {
                                        DayBookExcelHelper.generateSampleXlsFile(context)
                                    }
                                    isProcessing = false
                                    isSuccess = true
                                    statusMessage = "Day Book sample Excel generated successfully."
                                    Toast.makeText(context, "Sample Excel template ready.", Toast.LENGTH_SHORT).show()
                                    DayBookExcelHelper.shareExcelFile(context, file, "Share Day Book Sample Template (.xls)")
                                } catch (e: Exception) {
                                    isProcessing = false
                                    isSuccess = false
                                    statusMessage = "Failed to create sample XLS: ${e.localizedMessage}"
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Sample...")
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download / Share Sample .xls")
                        }
                    }
                }
            }
        }
    }
}




