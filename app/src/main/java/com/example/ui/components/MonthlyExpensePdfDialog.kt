package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Site
import com.example.data.model.TransactionEntry
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.OnAmberContainer
import com.example.util.MonthlyExpensePdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MonthlyExpensePdfDialog(
    transactions: List<TransactionEntry>,
    sites: List<Site>,
    initialSiteId: Long? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Determine available recent months from transactions or current date
    val availableMonths = remember(transactions) {
        val ymFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val cal = Calendar.getInstance()
        val set = linkedSetOf<String>()
        // Current month
        set.add(ymFormat.format(cal.time))
        // Previous 3 months
        for (i in 1..3) {
            cal.add(Calendar.MONTH, -1)
            set.add(ymFormat.format(cal.time))
        }
        // Months from transactions
        transactions.filter { it.type == "EXPENSE" }.forEach { tx ->
            if (tx.dateFormatted.length >= 7) {
                set.add(tx.dateFormatted.substring(0, 7))
            }
        }
        set.sortedDescending()
    }

    var selectedYearMonth by remember { mutableStateOf(availableMonths.firstOrNull() ?: SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Calendar.getInstance().time)) }
    var selectedSiteId by remember { mutableLongStateOf(initialSiteId ?: 0L) } // 0 = All Sites
    var siteDropdownExpanded by remember { mutableStateOf(false) }

    var isGenerating by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadConfirmed by remember { mutableStateOf<String?>(null) }

    val reportData = remember(transactions, sites, selectedYearMonth, selectedSiteId) {
        MonthlyExpensePdfHelper.buildMonthlyReportData(
            transactions = transactions,
            sites = sites,
            yearMonth = selectedYearMonth,
            siteIdFilter = if (selectedSiteId > 0) selectedSiteId else null
        )
    }

    val selectedSiteName = remember(selectedSiteId, sites) {
        if (selectedSiteId > 0) sites.firstOrNull { it.id == selectedSiteId }?.name ?: "Selected Site" else "All Construction Sites"
    }

    // Auto-generate or update PDF when month or site changes
    fun generatePdf() {
        isGenerating = true
        errorMessage = null
        downloadConfirmed = null
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    MonthlyExpensePdfHelper.generateMonthlyExpensePdf(
                        context = context,
                        reportData = reportData,
                        selectedSiteName = selectedSiteName
                    )
                }
                generatedFile = file
                isGenerating = false
            } catch (e: Exception) {
                isGenerating = false
                errorMessage = "Failed to generate PDF: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    LaunchedEffect(selectedYearMonth, selectedSiteId) {
        generatePdf()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Monthly Site Expense PDF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Categorized by site • Download & Share",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Month Selector Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Select Month & Year:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableMonths.take(5).forEach { ym ->
                            val label = try {
                                val d = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(ym)
                                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(d!!)
                            } catch (e: Exception) {
                                ym
                            }
                            val isSelected = ym == selectedYearMonth
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedYearMonth = ym },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Site Filter Dropdown
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Site Categorization Filter:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    ExposedDropdownMenuBox(
                        expanded = siteDropdownExpanded,
                        onExpandedChange = { siteDropdownExpanded = !siteDropdownExpanded }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apartment,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (selectedSiteId > 0) selectedSiteName else "All Sites (Full Breakdown)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteDropdownExpanded)
                            }
                        }

                        ExposedDropdownMenu(
                            expanded = siteDropdownExpanded,
                            onDismissRequest = { siteDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Sites (Full Multi-Project Breakdown)", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedSiteId = 0L
                                    siteDropdownExpanded = false
                                }
                            )
                            sites.forEach { site ->
                                DropdownMenuItem(
                                    text = { Text("${site.code} - ${site.name} (${site.location})") },
                                    onClick = {
                                        selectedSiteId = site.id
                                        siteDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Executive Report Preview Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Summary Preview",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = reportData.monthDisplay,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = MonthlyExpensePdfHelper.formatCurrency(reportData.totalExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active Sites in Month:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${reportData.totalSitesCount} Sites", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Expense Transactions:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${reportData.totalTransactions} entries", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        // Top Category Head
                        val topCat = reportData.overallCategoryTotals.firstOrNull()
                        if (topCat != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Top Expense Head:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${topCat.category} (${MonthlyExpensePdfHelper.formatCurrency(topCat.totalAmount)})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Site mini list
                        if (reportData.siteSummaries.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Text("Site Breakdown:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            reportData.siteSummaries.take(3).forEach { s ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(s.siteName, style = MaterialTheme.typography.bodySmall, maxLines = 1, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${MonthlyExpensePdfHelper.formatCurrency(s.totalExpense)} (${String.format(Locale.US, "%.1f%%", s.percentageOfTotal)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (s.percentageOfTotal / 100.0).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                            if (reportData.siteSummaries.size > 3) {
                                Text(
                                    "+ ${reportData.siteSummaries.size - 3} more sites in full PDF report",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        } else {
                            Text(
                                "No expense entries recorded for this selected month & site.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Error / Success Feedback
                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (downloadConfirmed != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                            Text(
                                text = downloadConfirmed!!,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Generating State Indicator
                if (isGenerating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Rendering Monthly PDF Report...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Direct Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Primary Action: View / Open PDF
                    Button(
                        onClick = {
                            if (generatedFile != null && generatedFile!!.exists()) {
                                MonthlyExpensePdfHelper.openPdfFile(context, generatedFile!!)
                            } else {
                                generatePdf()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_view_monthly_pdf"),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isGenerating && reportData.totalExpense > 0
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View / Open PDF Report", fontWeight = FontWeight.Bold)
                    }

                    // Row: Download PDF & Share via Chooser
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (generatedFile != null && generatedFile!!.exists()) {
                                    val downloaded = MonthlyExpensePdfHelper.downloadPdfToPublicFolder(context, generatedFile!!)
                                    downloadConfirmed = "Report downloaded: ${downloaded.name}"
                                } else {
                                    Toast.makeText(context, "Please generate the PDF report first.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_download_monthly_pdf"),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isGenerating && generatedFile != null
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download", style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = {
                                if (generatedFile != null && generatedFile!!.exists()) {
                                    MonthlyExpensePdfHelper.sharePdfFile(
                                        context = context,
                                        file = generatedFile!!,
                                        chooserTitle = "Share Monthly Expense PDF (${reportData.monthDisplay})"
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_share_monthly_pdf"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            enabled = !isGenerating && generatedFile != null
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Direct Share Buttons: Email & Messaging
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (generatedFile != null && generatedFile!!.exists()) {
                                    MonthlyExpensePdfHelper.shareViaEmail(
                                        context = context,
                                        file = generatedFile!!,
                                        monthDisplay = reportData.monthDisplay,
                                        totalExpenseFormatted = MonthlyExpensePdfHelper.formatCurrency(reportData.totalExpense)
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_email_monthly_pdf"),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isGenerating && generatedFile != null
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Email", style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedButton(
                            onClick = {
                                if (generatedFile != null && generatedFile!!.exists()) {
                                    MonthlyExpensePdfHelper.shareViaMessaging(
                                        context = context,
                                        file = generatedFile!!,
                                        monthDisplay = reportData.monthDisplay,
                                        totalExpenseFormatted = MonthlyExpensePdfHelper.formatCurrency(reportData.totalExpense)
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_messaging_monthly_pdf"),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isGenerating && generatedFile != null
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Messaging", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
