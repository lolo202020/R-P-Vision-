package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.ConstructionViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TransactionEntry
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFilterUtils {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun today(): String = sdf.format(Date())

    fun yesterday(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return sdf.format(cal.time)
    }

    fun thisWeekStart(): String {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return sdf.format(cal.time)
    }

    fun thisMonthStart(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return sdf.format(cal.time)
    }

    fun formatReadable(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            val date = sdf.parse(dateStr) ?: return dateStr
            val out = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            out.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(amount).replace("₹", "₹ ")
}

@Composable
fun StatCard(
    title: String,
    amount: Double,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("stat_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = contentColor,
                fontWeight = FontWeight.Bold
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PaymentModeBadge(
    mode: String,
    modifier: Modifier = Modifier
) {
    val (icon, bg, fg) = when (mode.uppercase()) {
        "CASH" -> Triple(Icons.Default.Payments, AmberContainer, OnAmberContainer)
        "BANK" -> Triple(Icons.Default.AccountBalance, Color(0xFFE0E7FF), Color(0xFF3730A3))
        "UPI" -> Triple(Icons.Default.QrCode, Color(0xFFF3E8FF), Color(0xFF6B21A8))
        else -> Triple(Icons.Default.CreditCard, SlateContainer, SlateSecondary)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = mode,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = mode,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TransactionItemCard(
    tx: TransactionEntry,
    onViewReceipt: (TransactionEntry) -> Unit,
    modifier: Modifier = Modifier,
    onEdit: ((TransactionEntry) -> Unit)? = null
) {
    val isIncome = tx.type == "INCOME"
    val accentColor = if (isIncome) IncomeGreen else ExpenseRed
    val badgeBg = if (isIncome) IncomeGreenContainer else ExpenseRedContainer
    val badgeFg = if (isIncome) OnIncomeGreenContainer else OnExpenseRedContainer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewReceipt(tx) }
            .testTag("tx_item_${tx.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Type Badge + Date + Amount
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
                        color = badgeBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = tx.type,
                                tint = badgeFg,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isIncome) "INCOME" else "EXPENSE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = badgeFg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = tx.dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${if (isIncome) "+" else "-"} ${formatCurrency(tx.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Category & Subcategory
            Text(
                text = tx.category,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (tx.subCategory.isNotBlank() && tx.subCategory != "Income Entry") {
                Text(
                    text = tx.subCategory,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Party / Vendor name & Particulars
            Text(
                text = "Party: ${tx.partyName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (tx.description.isNotBlank()) {
                Text(
                    text = tx.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Site Name + Payment Mode + Receipt indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = tx.siteName,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentModeBadge(mode = tx.paymentMode)
                    if (onEdit != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { onEdit(tx) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Entry",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptViewerDialog(
    tx: TransactionEntry,
    onDismiss: () -> Unit,
    onEdit: ((TransactionEntry) -> Unit)? = null
) {
    val isIncome = tx.type == "INCOME"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isIncome) "Income Voucher" else "Expense Bill / Receipt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Bill Graphic Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isIncome) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                        )
                        .border(
                            1.dp,
                            if (isIncome) Color(0xFFA7F3D0) else Color(0xFFFECACA),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isIncome) Icons.Default.AccountBalance else Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = if (isIncome) IncomeGreen else ExpenseRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "R.P. VISION CONSTRUCTION PVT. LTD.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isIncome) OnIncomeGreenContainer else OnExpenseRedContainer
                        )
                        Text(
                            text = "OFFICIAL SITE VOUCHER #${tx.id}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) OnIncomeGreenContainer else OnExpenseRedContainer
                        )
                        Text(
                            text = "Project: ${tx.siteName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Details List
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailRow(label = "Amount", value = formatCurrency(tx.amount), isHighlight = true, highlightColor = if (isIncome) IncomeGreen else ExpenseRed)
                    DetailRow(label = "Date", value = tx.dateFormatted)
                    DetailRow(label = "Category", value = tx.category)
                    if (tx.subCategory.isNotBlank() && tx.subCategory != "Income Entry") {
                        DetailRow(label = "Sub-Category", value = tx.subCategory)
                    }
                    DetailRow(label = "Party / Vendor", value = tx.partyName)
                    DetailRow(label = "Payment Mode", value = tx.paymentMode)
                    DetailRow(label = "Recorded By", value = "${tx.userName} (${tx.userMobile})")
                    if (!tx.description.isNullOrBlank()) {
                        DetailRow(label = "Particulars", value = tx.description)
                    }
                    if (!tx.remarks.isNullOrBlank()) {
                        DetailRow(label = "Remarks", value = tx.remarks)
                    }
                }

                if (onEdit != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Close")
                        }
                        Button(
                            onClick = {
                                onDismiss()
                                onEdit(tx)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Entry", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    ButtonClose(onDismiss)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    highlightColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = if (isHighlight) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
            color = if (isHighlight) highlightColor else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ButtonClose(onDismiss: () -> Unit) {
    TextButton(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("close_receipt_dialog")
    ) {
        Text("Close Voucher", fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilterCard(
    fromDate: String?,
    toDate: String?,
    onDateRangeSelected: (String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickerTarget by remember { mutableStateOf<String?>(null) } // "FROM" or "TO" or null

    if (pickerTarget != null) {
        val target = pickerTarget!!
        val initialMillis = remember(target, fromDate, toDate) {
            val dateStr = if (target == "FROM") fromDate else toDate
            if (!dateStr.isNullOrBlank()) {
                try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)?.time
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        DatePickerDialog(
            onDismissRequest = { pickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatted = sdf.format(Date(millis))
                            if (target == "FROM") {
                                val newTo = if (toDate != null && formatted > toDate) formatted else toDate
                                onDateRangeSelected(formatted, newTo)
                            } else {
                                val newFrom = if (fromDate != null && formatted < fromDate) formatted else fromDate
                                onDateRangeSelected(newFrom, formatted)
                            }
                        }
                        pickerTarget = null
                    }
                ) {
                    Text("Set Date", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (target == "FROM") {
                            onDateRangeSelected(null, toDate)
                        } else {
                            onDateRangeSelected(fromDate, null)
                        }
                        pickerTarget = null
                    }
                ) {
                    Text("Clear")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Quick Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val todayStr = DateFilterUtils.today()
                val yestStr = DateFilterUtils.yesterday()
                val weekStr = DateFilterUtils.thisWeekStart()
                val monthStr = DateFilterUtils.thisMonthStart()

                val isAll = fromDate == null && toDate == null
                val isToday = fromDate == todayStr && toDate == todayStr
                val isYesterday = fromDate == yestStr && toDate == yestStr
                val isThisWeek = fromDate == weekStr && toDate == todayStr
                val isThisMonth = fromDate == monthStr && toDate == todayStr

                FilterChip(
                    selected = isAll,
                    onClick = { onDateRangeSelected(null, null) },
                    label = { Text("All Dates", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("chip_date_all")
                )
                FilterChip(
                    selected = isToday,
                    onClick = { onDateRangeSelected(todayStr, todayStr) },
                    label = { Text("Today", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("chip_date_today")
                )
                FilterChip(
                    selected = isYesterday,
                    onClick = { onDateRangeSelected(yestStr, yestStr) },
                    label = { Text("Yesterday", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("chip_date_yesterday")
                )
                FilterChip(
                    selected = isThisWeek,
                    onClick = { onDateRangeSelected(weekStr, todayStr) },
                    label = { Text("This Week", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("chip_date_week")
                )
                FilterChip(
                    selected = isThisMonth,
                    onClick = { onDateRangeSelected(monthStr, todayStr) },
                    label = { Text("This Month", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("chip_date_month")
                )
            }

            // Dual Date Selector (FROM DATE -> TO DATE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FROM DATE BOX
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (fromDate != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { pickerTarget = "FROM" }
                        .testTag("btn_date_from")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "From Date",
                            modifier = Modifier.size(18.dp),
                            tint = if (fromDate != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "From Date (से)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (fromDate != null) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = fromDate ?: "Start Date",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (fromDate != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (fromDate != null) {
                            IconButton(
                                onClick = { onDateRangeSelected(null, toDate) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear From Date",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // TO DATE BOX
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (toDate != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { pickerTarget = "TO" }
                        .testTag("btn_date_to")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "To Date",
                            modifier = Modifier.size(18.dp),
                            tint = if (toDate != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "To Date (तक)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (toDate != null) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = toDate ?: "End Date",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (toDate != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (toDate != null) {
                            IconButton(
                                onClick = { onDateRangeSelected(fromDate, null) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear To Date",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RpvcBrandLogo(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 72.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_rpvc_logo),
        contentDescription = "R.P. VISION CONSTRUCTION Pvt. Ltd. Logo",
        modifier = modifier.size(size)
    )
}

@Composable
fun RpvcBrandHeader(
    modifier: Modifier = Modifier,
    logoSize: androidx.compose.ui.unit.Dp = 64.dp,
    showTagline: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        RpvcBrandLogo(size = logoSize)
        
        Text(
            text = "R P V C PVT. LTD.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "R.P. VISION CONSTRUCTION PVT. LTD.",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        if (showTagline) {
            Text(
                text = "\"Empowering Vision\" • Construction Site & Expense Management",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

