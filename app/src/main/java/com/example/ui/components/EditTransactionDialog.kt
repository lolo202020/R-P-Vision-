package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CategoryConstants
import com.example.data.model.Site
import com.example.data.model.TransactionEntry
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedContainer
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenContainer
import com.example.ui.theme.OnExpenseRedContainer
import com.example.ui.theme.OnIncomeGreenContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntry,
    sites: List<Site>,
    onSave: (TransactionEntry) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var entryType by remember { mutableStateOf(transaction.type) } // "INCOME" or "EXPENSE"
    var selectedDateMillis by remember { mutableLongStateOf(transaction.dateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedSiteId by remember {
        mutableLongStateOf(
            if (transaction.siteId > 0) transaction.siteId else (sites.firstOrNull()?.id ?: 0L)
        )
    }
    var selectedSiteName by remember {
        mutableStateOf(
            if (transaction.siteName.isNotBlank()) transaction.siteName else (sites.firstOrNull()?.name ?: "Main Site")
        )
    }
    var siteDropdownExpanded by remember { mutableStateOf(false) }

    var amountText by remember {
        mutableStateOf(
            if (transaction.amount % 1.0 == 0.0) transaction.amount.toLong().toString() else transaction.amount.toString()
        )
    }
    var partyName by remember { mutableStateOf(transaction.partyName) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var selectedSubCategory by remember { mutableStateOf(transaction.subCategory) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var subCategoryDropdownExpanded by remember { mutableStateOf(false) }

    var paymentMode by remember { mutableStateOf(transaction.paymentMode) }
    var paymentModeDropdownExpanded by remember { mutableStateOf(false) }

    var description by remember { mutableStateOf(transaction.description) }
    var remarks by remember { mutableStateOf(transaction.remarks ?: "") }

    var validationError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val paymentModes = listOf("Cash", "Bank", "UPI", "Cheque", "Other")

    val isIncome = entryType == "INCOME"
    val categoriesList = if (isIncome) CategoryConstants.INCOME_CATEGORIES else CategoryConstants.EXPENSE_CATEGORIES

    val currentSubCategories = categoriesList
        .firstOrNull { it.mainCategory.equals(selectedCategory, ignoreCase = true) }
        ?.subCategories ?: emptyList()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirmDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Transaction #${transaction.id}?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to permanently delete this ${if (transaction.type == "INCOME") "income" else "expense"} entry of ₹${transaction.amount}? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(transaction.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(modifier = Modifier.padding(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Edit ${if (isIncome) "Income" else "Expense"} #${transaction.id}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Admin Permission Override Mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Type Selector (Income / Expense)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = entryType == "EXPENSE",
                            onClick = {
                                if (entryType != "EXPENSE") {
                                    entryType = "EXPENSE"
                                    val firstExp = CategoryConstants.EXPENSE_CATEGORIES.firstOrNull()
                                    selectedCategory = firstExp?.mainCategory ?: "Material Purchase"
                                    selectedSubCategory = firstExp?.subCategories?.firstOrNull() ?: ""
                                }
                            },
                            label = { Text("Expense (भुगतान)", fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseRedContainer,
                                selectedLabelColor = OnExpenseRedContainer,
                                selectedLeadingIconColor = OnExpenseRedContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = entryType == "INCOME",
                            onClick = {
                                if (entryType != "INCOME") {
                                    entryType = "INCOME"
                                    val firstInc = CategoryConstants.INCOME_CATEGORIES.firstOrNull()
                                    selectedCategory = firstInc?.mainCategory ?: "Head Office"
                                    selectedSubCategory = firstInc?.subCategories?.firstOrNull() ?: "HO se received amount"
                                }
                            },
                            label = { Text("Income (आमदनी)", fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IncomeGreenContainer,
                                selectedLabelColor = OnIncomeGreenContainer,
                                selectedLeadingIconColor = OnIncomeGreenContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Project / Site Dropdown
                    if (sites.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = siteDropdownExpanded,
                            onExpandedChange = { siteDropdownExpanded = !siteDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedSiteName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Project / Construction Site") },
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = siteDropdownExpanded,
                                onDismissRequest = { siteDropdownExpanded = false }
                            ) {
                                sites.forEach { site ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(site.name, fontWeight = FontWeight.Bold)
                                                Text(site.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = {
                                            selectedSiteId = site.id
                                            selectedSiteName = site.name
                                            siteDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Date Picker Input
                    OutlinedTextField(
                        value = dateFormat.format(Date(selectedDateMillis)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date of Transaction") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Pick Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )

                    // Amount Input
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            validationError = null
                        },
                        label = { Text("Amount (₹) *") },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = validationError != null && amountText.toDoubleOrNull() == null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Party Name
                    OutlinedTextField(
                        value = partyName,
                        onValueChange = {
                            partyName = it
                            validationError = null
                        },
                        label = { Text(if (isIncome) "Received From / Client / Source *" else "Party / Vendor / Person Name *") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        singleLine = true,
                        isError = validationError != null && partyName.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = { selectedCategory = it },
                            label = { Text("Category") },
                            leadingIcon = {
                                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categoriesList.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.mainCategory) },
                                    onClick = {
                                        selectedCategory = cat.mainCategory
                                        selectedSubCategory = cat.subCategories.firstOrNull() ?: ""
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Sub-Category Dropdown / Field
                    if (currentSubCategories.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = subCategoryDropdownExpanded,
                            onExpandedChange = { subCategoryDropdownExpanded = !subCategoryDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedSubCategory,
                                onValueChange = { selectedSubCategory = it },
                                label = { Text("Sub-Category / Item Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCategoryDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = subCategoryDropdownExpanded,
                                onDismissRequest = { subCategoryDropdownExpanded = false }
                            ) {
                                currentSubCategories.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub) },
                                        onClick = {
                                            selectedSubCategory = sub
                                            subCategoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = selectedSubCategory,
                            onValueChange = { selectedSubCategory = it },
                            label = { Text("Sub-Category / Detail") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Payment Mode Dropdown
                    ExposedDropdownMenuBox(
                        expanded = paymentModeDropdownExpanded,
                        onExpandedChange = { paymentModeDropdownExpanded = !paymentModeDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = paymentMode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Mode") },
                            leadingIcon = {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentModeDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = paymentModeDropdownExpanded,
                            onDismissRequest = { paymentModeDropdownExpanded = false }
                        ) {
                            paymentModes.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode) },
                                    onClick = {
                                        paymentMode = mode
                                        paymentModeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Description / Particulars
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Particulars") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Remarks
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Admin Remarks / Note (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Validation Error Text
                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                contentColor = ExpenseRed
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Entry")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val amountVal = amountText.trim().replace(",", "").toDoubleOrNull()
                                if (amountVal == null || amountVal <= 0) {
                                    validationError = "Please enter a valid amount greater than 0"
                                    return@Button
                                }
                                if (partyName.trim().isBlank()) {
                                    validationError = "Please enter Party/Vendor name"
                                    return@Button
                                }

                                val updated = transaction.copy(
                                    type = entryType,
                                    dateMillis = selectedDateMillis,
                                    dateFormatted = dateFormat.format(Date(selectedDateMillis)),
                                    siteId = selectedSiteId,
                                    siteName = selectedSiteName,
                                    amount = amountVal,
                                    partyName = partyName.trim(),
                                    category = selectedCategory.trim(),
                                    subCategory = selectedSubCategory.trim(),
                                    paymentMode = paymentMode,
                                    description = description.trim(),
                                    remarks = remarks.trim().ifBlank { null }
                                )

                                onSave(updated)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
