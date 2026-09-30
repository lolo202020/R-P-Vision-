package com.example.ui.screens.incharge

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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryConstants
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.OnAmberContainer
import com.example.ui.viewmodel.ConstructionViewModel
import com.example.ui.components.ResponsiveFormContainer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEntryScreen(
    viewModel: ConstructionViewModel,
    initialTab: Int = 0, // 0 = Expense Entry, 1 = Income Entry
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(initialTab) } // 0 = Expense, 1 = Income

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()

    // Form Common States
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Expense Form States
    var selectedExpenseCategory by remember { mutableStateOf(CategoryConstants.EXPENSE_CATEGORIES[0].mainCategory) }
    var expenseCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var expensePartyName by remember { mutableStateOf("") }
    var expenseDescription by remember { mutableStateOf("") }
    var expenseAmountStr by remember { mutableStateOf("") }
    var expensePaymentMode by remember { mutableStateOf("Cash") }
    var expenseError by remember { mutableStateOf<String?>(null) }

    // Income Form States
    var selectedIncomeCategory by remember { mutableStateOf(CategoryConstants.INCOME_CATEGORIES[0].mainCategory) }
    var incomeCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var incomeSourceParty by remember { mutableStateOf("") }
    var incomeDescription by remember { mutableStateOf("") }
    var incomeAmountStr by remember { mutableStateOf("") }
    var incomePaymentMode by remember { mutableStateOf("Bank") }
    var incomeRemarks by remember { mutableStateOf("") }
    var incomeError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

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
                    Text("OK")
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

    ResponsiveFormContainer(
        maxWidth = 720.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Entry Type Tabs
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = if (selectedTab == 0) ExpenseRed else IncomeGreen,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("tab_add_expense"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                            Text("Expense Entry", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("tab_add_income"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                            Text("Income Entry", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }
        }

        // Assigned Site Info (Locked to Incharge's Assigned Project)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Assigned Site / Project",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentUser?.assignedSiteName ?: "Assigned Site",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AmberContainer
                ) {
                    Text(
                        text = "LOCKED TO SITE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = OnAmberContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Common Date Selector Field
        OutlinedTextField(
            value = dateFormat.format(Date(selectedDateMillis)),
            onValueChange = {},
            readOnly = true,
            label = { Text("Transaction Date") },
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.EditNote, contentDescription = "Pick Date")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
                .testTag("input_tx_date"),
            shape = RoundedCornerShape(12.dp)
        )

        // EXPENSE ENTRY FORM
        if (selectedTab == 0) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Expense Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )

                    // 1. Expense Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expenseCategoryDropdownExpanded,
                        onExpandedChange = { expenseCategoryDropdownExpanded = !expenseCategoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedExpenseCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expense Main Category *") },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = ExpenseRed) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expenseCategoryDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("dropdown_expense_category"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = expenseCategoryDropdownExpanded,
                            onDismissRequest = { expenseCategoryDropdownExpanded = false }
                        ) {
                            CategoryConstants.EXPENSE_CATEGORIES.forEachIndexed { index, cat ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    },
                                    text = {
                                        Column {
                                            Text(
                                                text = cat.mainCategory,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = cat.subCategories.joinToString(", ").take(45) + if (cat.subCategories.joinToString(", ").length > 45) "..." else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedExpenseCategory = cat.mainCategory
                                        expenseCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Party / Vendor Name
                    OutlinedTextField(
                        value = expensePartyName,
                        onValueChange = { expensePartyName = it },
                        label = { Text("Party / Vendor Name *") },
                        placeholder = { Text("e.g. UltraTech Cement / Raju Mason") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_expense_party"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 4. Amount (₹)
                    OutlinedTextField(
                        value = expenseAmountStr,
                        onValueChange = { expenseAmountStr = it },
                        label = { Text("Amount (₹) *") },
                        placeholder = { Text("0.00") },
                        leadingIcon = { Text(" ₹ ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ExpenseRed) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_expense_amount"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 5. Payment Mode Selection
                    Text(
                        text = "Payment Mode *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryConstants.PAYMENT_MODES.forEach { mode ->
                            val isSelected = expensePaymentMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { expensePaymentMode = mode },
                                label = { Text(mode, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.testTag("chip_pay_mode_$mode")
                            )
                        }
                    }

                    // 5. Particulars / Description
                    OutlinedTextField(
                        value = expenseDescription,
                        onValueChange = { expenseDescription = it },
                        label = { Text("Description / Particulars") },
                        placeholder = { Text("e.g. 500 bags for 3rd floor slab casting") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_expense_description"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    // Error text
                    if (!expenseError.isNullOrBlank()) {
                        Text(
                            text = expenseError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Submit Expense Button
                    Button(
                        onClick = {
                            val amount = expenseAmountStr.toDoubleOrNull()
                            if (amount == null || amount <= 0.0) {
                                expenseError = "Please enter a valid expense amount"
                                return@Button
                            }
                            if (expensePartyName.isBlank()) {
                                expenseError = "Please enter party / vendor name"
                                return@Button
                            }

                            expenseError = null
                            viewModel.submitExpense(
                                dateMillis = selectedDateMillis,
                                category = selectedExpenseCategory,
                                subCategory = "General",
                                partyName = expensePartyName.trim(),
                                description = expenseDescription.trim(),
                                amount = amount,
                                paymentMode = expensePaymentMode,
                                receiptPhotoUri = null,
                                remarks = null,
                                onSuccess = {
                                    expenseAmountStr = ""
                                    expensePartyName = ""
                                    expenseDescription = ""
                                    onSubmitted()
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_submit_expense"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit Expense Entry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // INCOME ENTRY FORM
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Income Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )

                    // 1. Income Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = incomeCategoryDropdownExpanded,
                        onExpandedChange = { incomeCategoryDropdownExpanded = !incomeCategoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedIncomeCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Income Category *") },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = incomeCategoryDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("dropdown_income_category"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = incomeCategoryDropdownExpanded,
                            onDismissRequest = { incomeCategoryDropdownExpanded = false }
                        ) {
                            CategoryConstants.INCOME_CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(text = cat.mainCategory, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = cat.subCategories.joinToString(", "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedIncomeCategory = cat.mainCategory
                                        incomeCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Source / Party Name
                    OutlinedTextField(
                        value = incomeSourceParty,
                        onValueChange = { incomeSourceParty = it },
                        label = { Text("Source / Party Name *") },
                        placeholder = { Text("e.g. Head Office / SBI Project Loan / Machinery Hire") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_income_source"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 3. Amount (₹)
                    OutlinedTextField(
                        value = incomeAmountStr,
                        onValueChange = { incomeAmountStr = it },
                        label = { Text("Amount (₹) *") },
                        placeholder = { Text("0.00") },
                        leadingIcon = { Text(" ₹ ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = IncomeGreen) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_income_amount"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 4. Payment Mode Selection
                    Text(
                        text = "Payment Mode *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryConstants.PAYMENT_MODES.forEach { mode ->
                            val isSelected = incomePaymentMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { incomePaymentMode = mode },
                                label = { Text(mode, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.testTag("chip_income_pay_mode_$mode")
                            )
                        }
                    }

                    // 5. Particulars / Description
                    OutlinedTextField(
                        value = incomeDescription,
                        onValueChange = { incomeDescription = it },
                        label = { Text("Description / Particulars") },
                        placeholder = { Text("e.g. Received for phase 2 excavation & reinforcement") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_income_description"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    // 6. Remarks
                    OutlinedTextField(
                        value = incomeRemarks,
                        onValueChange = { incomeRemarks = it },
                        label = { Text("Remarks (Optional)") },
                        placeholder = { Text("UTR / Reference number / Note") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_income_remarks"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Error text
                    if (!incomeError.isNullOrBlank()) {
                        Text(
                            text = incomeError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Submit Income Button
                    Button(
                        onClick = {
                            val amount = incomeAmountStr.toDoubleOrNull()
                            if (amount == null || amount <= 0.0) {
                                incomeError = "Please enter a valid income amount"
                                return@Button
                            }
                            if (incomeSourceParty.isBlank()) {
                                incomeError = "Please enter source / party name"
                                return@Button
                            }

                            incomeError = null
                            viewModel.submitIncome(
                                dateMillis = selectedDateMillis,
                                category = selectedIncomeCategory,
                                sourceParty = incomeSourceParty.trim(),
                                description = incomeDescription.trim(),
                                amount = amount,
                                paymentMode = incomePaymentMode,
                                receiptPhotoUri = null,
                                remarks = incomeRemarks.trim().ifBlank { null },
                                onSuccess = {
                                    incomeAmountStr = ""
                                    incomeSourceParty = ""
                                    incomeDescription = ""
                                    incomeRemarks = ""
                                    onSubmitted()
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_submit_income"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit Income Entry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
    }
}
