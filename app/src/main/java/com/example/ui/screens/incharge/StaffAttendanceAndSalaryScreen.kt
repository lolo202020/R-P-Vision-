package com.example.ui.screens.incharge

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SalaryRecord
import com.example.data.model.Site
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.User
import com.example.ui.components.AttendancePdfExportDialog
import com.example.ui.viewmodel.ConstructionViewModel
import com.example.util.AttendancePdfHelper
import androidx.compose.ui.platform.testTag
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAttendanceAndSalaryScreen(
    viewModel: ConstructionViewModel,
    isAdminView: Boolean = false
) {
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Daily Attendance (1) as requested
    val staffList by viewModel.inchargeStaff.collectAsState()
    val attendanceList by viewModel.inchargeAttendance.collectAsState()
    val salaryRecords by viewModel.inchargeSalaryRecords.collectAsState()
    val auditLogs by viewModel.allAuditLogs.collectAsState()
    val sites by viewModel.allSites.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showAddStaffDialog by remember { mutableStateOf(false) }
    var selectedStaffForEdit by remember { mutableStateOf<StaffMember?>(null) }
    var showSalaryDialog by remember { mutableStateOf(false) }
    var selectedStaffForSalary by remember { mutableStateOf<StaffMember?>(null) }
    var salarySlipPreview by remember { mutableStateOf<SalaryRecord?>(null) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Sub-Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Staff Directory (${staffList.size})") },
                icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Daily Attendance") },
                icon = { Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Salary & Payouts") },
                icon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Audit Logs") },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1100.dp)
            ) {
                when (selectedTab) {
                    0 -> StaffDirectoryView(
                        staffList = staffList,
                        onAddStaff = {
                            selectedStaffForEdit = null
                            showAddStaffDialog = true
                        },
                        onEditStaff = { staff ->
                            selectedStaffForEdit = staff
                            showAddStaffDialog = true
                        },
                        onDeleteStaff = { viewModel.deleteStaff(it) }
                    )
                    1 -> ButtonBasedAttendanceView(
                        staffList = staffList,
                        attendanceList = attendanceList,
                        sites = sites,
                        viewModel = viewModel,
                        currentUser = currentUser
                    )
                    2 -> SalaryPayoutsView(
                        salaryRecords = salaryRecords,
                        staffList = staffList,
                        attendanceList = attendanceList,
                        onOpenSalaryCalculator = {
                            selectedStaffForSalary = it
                            showSalaryDialog = true
                        },
                        onViewSlip = { salarySlipPreview = it },
                        onDeleteRecord = { viewModel.deleteSalaryRecord(it) }
                    )
                    3 -> AuditLogsView(auditLogs = auditLogs)
                }
            }
        }
    }

    if (showAddStaffDialog) {
        AddStaffDialog(
            sites = sites,
            currentUser = currentUser,
            staffToEdit = selectedStaffForEdit,
            onDismiss = {
                showAddStaffDialog = false
                selectedStaffForEdit = null
            },
            onSave = { name, mobile, desig, wage, paymentType, sId, sName ->
                val editId = selectedStaffForEdit?.id ?: 0L
                viewModel.addStaffMember(editId, name, mobile, desig, wage, paymentType, sId, sName) {
                    showAddStaffDialog = false
                    selectedStaffForEdit = null
                }
            }
        )
    }

    if (showSalaryDialog && selectedStaffForSalary != null) {
        SalaryCalculatorDialog(
            staff = selectedStaffForSalary!!,
            attendanceList = attendanceList.filter { it.staffId == selectedStaffForSalary!!.id },
            onDismiss = {
                showSalaryDialog = false
                selectedStaffForSalary = null
            },
            onSaveRecord = { record ->
                viewModel.saveSalaryRecord(record) {
                    showSalaryDialog = false
                    selectedStaffForSalary = null
                }
            }
        )
    }

    if (salarySlipPreview != null) {
        SalarySlipDialog(
            record = salarySlipPreview!!,
            onDismiss = { salarySlipPreview = null },
            onShare = {
                val slipText = "--- PROFESSIONAL SALARY SLIP ---\n" +
                        "Staff: ${salarySlipPreview!!.staffName}\n" +
                        "Site: ${salarySlipPreview!!.siteName}\n" +
                        "Month: ${salarySlipPreview!!.monthYear}\n" +
                        "Basic Salary: ₹${salarySlipPreview!!.basicSalary}\n" +
                        "Present: ${salarySlipPreview!!.presentDays} | Absent: ${salarySlipPreview!!.absentDays} | Half-Day: ${salarySlipPreview!!.halfDays}\n" +
                        "Overtime: ${salarySlipPreview!!.overtimeHours} hrs (+₹${salarySlipPreview!!.overtimeAmount})\n" +
                        "Bonus: +₹${salarySlipPreview!!.bonus} | Advance: -₹${salarySlipPreview!!.advance} | Deduction: -₹${salarySlipPreview!!.deduction}\n" +
                        "NET SALARY: ₹${salarySlipPreview!!.netSalary}\n" +
                        "Status: ${salarySlipPreview!!.paymentStatus} (${salarySlipPreview!!.paymentMode})\n"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Salary Slip - ${salarySlipPreview!!.staffName}")
                    putExtra(Intent.EXTRA_TEXT, slipText)
                }
                context.startActivity(Intent.createChooser(intent, "Share Salary Slip"))
            }
        )
    }
}

@Composable
private fun ButtonBasedAttendanceView(
    staffList: List<StaffMember>,
    attendanceList: List<StaffAttendance>,
    sites: List<Site>,
    viewModel: ConstructionViewModel,
    currentUser: User? = null
) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = dateFormatter.format(Date())
    var selectedDate by remember { mutableStateOf(todayStr) }

    var selectedSiteId by remember { mutableStateOf<Long?>(null) }
    val filteredStaff = if (selectedSiteId == null) staffList else staffList.filter { it.siteId == selectedSiteId }
    val siteName = sites.find { it.id == selectedSiteId }?.name ?: "All Sites"

    var showAttendancePdfDialog by remember { mutableStateOf(false) }

    if (showAttendancePdfDialog) {
        val reportItems = remember(filteredStaff, attendanceList, selectedDate, selectedSiteId) {
            AttendancePdfHelper.buildReportItems(
                staffList = filteredStaff,
                attendanceList = attendanceList,
                dateFormatted = selectedDate,
                siteId = selectedSiteId
            )
        }
        val managerDisplayName = currentUser?.name?.let { "R P V C - $it" } ?: "R P V C - Ramesh Kumar"
        val projectSiteName = if (siteName != "All Sites") siteName else (sites.firstOrNull()?.name ?: "Metro City Tower")

        AttendancePdfExportDialog(
            reportItems = reportItems,
            selectedDate = selectedDate,
            siteName = projectSiteName,
            managerName = managerDisplayName,
            onDismiss = { showAttendancePdfDialog = false }
        )
    }

    // Map to keep track of attendance states for staff members in current view
    // staffId -> Attendance State (status, ot, checkIn, checkOut)
    val currentAttendanceMap = remember(attendanceList, selectedDate, selectedSiteId) {
        attendanceList.filter { it.dateFormatted == selectedDate && (selectedSiteId == null || it.siteId == selectedSiteId) }
            .associateBy { it.staffId }
    }

    val dayAttendance = attendanceList.filter { it.dateFormatted == selectedDate && (selectedSiteId == null || it.siteId == selectedSiteId) }
    val presentCount = dayAttendance.count { it.status == "PRESENT" }
    val absentCount = dayAttendance.count { it.status == "ABSENT" }
    val halfDayCount = dayAttendance.count { it.status == "HALF_DAY" }
    val leaveCount = dayAttendance.count { it.status == "LEAVE" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("attendance_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily Attendance System", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Select date to mark attendance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(
                    onClick = { showAttendancePdfDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_export_attendance_pdf")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export")
                }
            }
        }

        // Date Selection Bar with Prev, Next & Date Picker
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        try {
                            val parts = selectedDate.split("-")
                            if (parts.size == 3) {
                                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                            }
                        } catch (_: Exception) {}
                        cal.add(Calendar.DAY_OF_MONTH, -1)
                        selectedDate = dateFormatter.format(cal.time)
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                    }

                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            try {
                                val parts = selectedDate.split("-")
                                if (parts.size == 3) {
                                    calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                }
                            } catch (_: Exception) {}

                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Date: $selectedDate", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        try {
                            val parts = selectedDate.split("-")
                            if (parts.size == 3) {
                                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                            }
                        } catch (_: Exception) {}
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                        selectedDate = dateFormatter.format(cal.time)
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                    }
                }
            }
        }

        // Site Filter & Date Selection Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScrollableTabRow(
                    selectedTabIndex = if (selectedSiteId == null) 0 else (sites.indexOfFirst { it.id == selectedSiteId } + 1),
                    edgePadding = 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Tab(
                        selected = selectedSiteId == null,
                        onClick = { selectedSiteId = null },
                        text = { Text("All Sites") }
                    )
                    sites.forEach { site ->
                        Tab(
                            selected = selectedSiteId == site.id,
                            onClick = { selectedSiteId = site.id },
                            text = { Text(site.name) }
                        )
                    }
                }
            }
        }

        // Daily Summary Counters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryBadge("Present", presentCount.toString(), Color(0xFFE8F5E9), Color(0xFF2E7D32), Modifier.weight(1f))
                SummaryBadge("Absent", absentCount.toString(), Color(0xFFFFEBEE), Color(0xC62828), Modifier.weight(1f))
                SummaryBadge("Half Day", halfDayCount.toString(), Color(0xFFFFFDE7), Color(0xFFF57F17), Modifier.weight(1f))
                SummaryBadge("Leave", leaveCount.toString(), Color(0xFFE3F2FD), Color(0xFF1565C0), Modifier.weight(1f))
            }
        }

        // Master Action: Mark All Present
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Staff List (${filteredStaff.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = {
                        filteredStaff.forEach { st ->
                            val baseWage = st.dailyWage
                            val calculatedWage = if (st.paymentType == "MONTHLY") baseWage / 30.0 else baseWage
                            viewModel.markAttendance(
                                staffId = st.id,
                                staffName = st.name,
                                siteId = st.siteId,
                                siteName = st.siteName,
                                dateMillis = System.currentTimeMillis(),
                                dateFormatted = selectedDate,
                                status = "PRESENT",
                                wageAmount = calculatedWage,
                                overtimeHours = 0.0,
                                remarks = "Marked All Present"
                            ) {}
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark All Present")
                }
            }
        }

        if (filteredStaff.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No staff found for attendance.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filteredStaff, key = { it.id }) { staff ->
                val existing = currentAttendanceMap[staff.id]
                val currentStatus = existing?.status ?: "PRESENT"
                val currentOt = existing?.overtimeHours ?: 0.0

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Staff Info & Overtime indication
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(staff.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("${staff.designation} • Site: ${staff.siteName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Surface(
                                color = when (currentStatus) {
                                    "PRESENT" -> Color(0xFFE8F5E9)
                                    "ABSENT" -> Color(0xFFFFEBEE)
                                    "HALF_DAY" -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFE0E7FF)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = currentStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (currentStatus) {
                                        "PRESENT" -> Color(0xFF15803D)
                                        "ABSENT" -> Color(0xFFDC2626)
                                        "HALF_DAY" -> Color(0xFFB45309)
                                        else -> Color(0xFF1D4ED8)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Large Status Buttons Row: [ PRESENT ] [ ABSENT ] [ HALF DAY ] [ LEAVE ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("PRESENT", "ABSENT", "HALF_DAY", "LEAVE").forEach { st ->
                                val isSelected = currentStatus == st
                                Button(
                                    onClick = {
                                        val baseWage = staff.dailyWage
                                        val calculatedWage = when (st) {
                                            "PRESENT" -> if (staff.paymentType == "MONTHLY") baseWage / 30.0 else baseWage
                                            "HALF_DAY" -> if (staff.paymentType == "MONTHLY") baseWage / 60.0 else baseWage / 2.0
                                            else -> 0.0
                                        } + (currentOt * 150.0)

                                        viewModel.markAttendance(
                                            staffId = staff.id,
                                            staffName = staff.name,
                                            siteId = staff.siteId,
                                            siteName = staff.siteName,
                                            dateMillis = System.currentTimeMillis(),
                                            dateFormatted = selectedDate,
                                            status = st,
                                            wageAmount = calculatedWage,
                                            overtimeHours = currentOt,
                                            remarks = existing?.remarks
                                        ) {}
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) {
                                            when (st) {
                                                "PRESENT" -> Color(0xFF15803D)
                                                "ABSENT" -> Color(0xFFDC2626)
                                                "HALF_DAY" -> Color(0xFFD97706)
                                                else -> Color(0xFF2563EB)
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                ) {
                                    Text(
                                        text = when (st) {
                                            "PRESENT" -> "Present"
                                            "ABSENT" -> "Absent"
                                            "HALF_DAY" -> "Half Day"
                                            else -> "Leave"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Overtime & Save Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val newOt = currentOt + 1.0
                                    val baseWage = staff.dailyWage
                                    val calculatedWage = when (currentStatus) {
                                        "PRESENT" -> if (staff.paymentType == "MONTHLY") baseWage / 30.0 else baseWage
                                        "HALF_DAY" -> if (staff.paymentType == "MONTHLY") baseWage / 60.0 else baseWage / 2.0
                                        else -> 0.0
                                    } + (newOt * 150.0)

                                    viewModel.markAttendance(
                                        staffId = staff.id,
                                        staffName = staff.name,
                                        siteId = staff.siteId,
                                        siteName = staff.siteName,
                                        dateMillis = System.currentTimeMillis(),
                                        dateFormatted = selectedDate,
                                        status = currentStatus,
                                        wageAmount = calculatedWage,
                                        overtimeHours = newOt,
                                        remarks = "Added Overtime"
                                    ) {}
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("+ Overtime (${currentOt}h)", fontSize = 11.sp)
                            }

                            Text("Wage: ₹${existing?.wageAmount ?: 0.0}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SummaryBadge(title: String, count: String, bgColor: Color? = null, textColor: Color? = null, modifier: Modifier = Modifier) {
    Surface(
        color = bgColor ?: MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = textColor ?: MaterialTheme.colorScheme.onSurfaceVariant)
            Text(count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor ?: MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun StaffDirectoryView(
    staffList: List<StaffMember>,
    onAddStaff: () -> Unit,
    onEditStaff: (StaffMember) -> Unit,
    onDeleteStaff: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("staff_directory_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Staff & Labour Directory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = onAddStaff, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Staff")
                }
            }
        }

        if (staffList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No staff members added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(staffList, key = { it.id }) { staff ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(staff.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = if (staff.paymentType == "MONTHLY") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (staff.paymentType == "MONTHLY") "Monthly Salary" else "Daily Basis",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text("Role: ${staff.designation} • Mobile: ${staff.mobile}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Site: ${staff.siteName} | Wage/Salary: ₹${staff.dailyWage}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onEditStaff(staff) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDeleteStaff(staff.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SalaryPayoutsView(
    salaryRecords: List<SalaryRecord>,
    staffList: List<StaffMember>,
    attendanceList: List<StaffAttendance>,
    onOpenSalaryCalculator: (StaffMember) -> Unit,
    onViewSlip: (SalaryRecord) -> Unit,
    onDeleteRecord: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("salary_payouts_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Salary & Payout Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Text("Select a staff member below to calculate monthly salary, advance deductions, overtime & generate professional salary slips:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Text("Staff Eligible for Payout", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        if (staffList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No staff registered yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(staffList, key = { it.id }) { staff ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(staff.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Surface(
                                color = if (staff.paymentType == "MONTHLY") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (staff.paymentType == "MONTHLY") "Monthly Salary" else "Daily Basis",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "${staff.designation} • ${staff.siteName} • Base: ₹${staff.dailyWage}${if (staff.paymentType == "MONTHLY") "/month" else "/day"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { onOpenSalaryCalculator(staff) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Calculate & Payment", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Processed Salary Records & History", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        if (salaryRecords.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No salary records generated yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(salaryRecords, key = { it.id }) { rec ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(rec.staffName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("₹${rec.netSalary}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("Month: ${rec.monthYear} • Site: ${rec.siteName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Present: ${rec.presentDays} | Absent: ${rec.absentDays} | Overtime: ${rec.overtimeHours} hrs (+₹${rec.overtimeAmount})", style = MaterialTheme.typography.bodySmall)
                        Text("Bonus: +₹${rec.bonus} | Advance: -₹${rec.advance} | Deduction: -₹${rec.deduction}", style = MaterialTheme.typography.bodySmall)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = when (rec.paymentStatus) {
                                    "PAID", "APPROVED" -> Color(0xFFE8F5E9)
                                    "PARTIAL" -> Color(0xFFFFFDE7)
                                    else -> Color(0xFFFFEBEE)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${rec.paymentStatus} (${rec.paymentMode})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(onClick = { onViewSlip(rec) }, shape = RoundedCornerShape(8.dp)) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Salary Slip")
                                }
                                IconButton(onClick = { onDeleteRecord(rec.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AuditLogsView(auditLogs: List<com.example.data.model.AuditLog>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("audit_logs_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Audit Logs & Duplicate Prevention", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Text("System tracks all critical actions, approvals, and salary disbursements for transparency and audit compliance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (auditLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No audit logs recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(auditLogs, key = { it.id }) { log ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(log.action, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                            Text(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(log.details, style = MaterialTheme.typography.bodySmall)
                        Text("User: ${log.userName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStaffDialog(
    sites: List<Site>,
    currentUser: com.example.data.model.User?,
    staffToEdit: StaffMember? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, String, Long, String) -> Unit
) {
    var name by remember { mutableStateOf(staffToEdit?.name ?: "") }
    var mobile by remember { mutableStateOf(staffToEdit?.mobile ?: "") }
    var designation by remember { mutableStateOf(staffToEdit?.designation ?: "Mason") }
    var wageStr by remember { mutableStateOf(staffToEdit?.dailyWage?.toString() ?: "15000") }
    var paymentType by remember { mutableStateOf(staffToEdit?.paymentType ?: "MONTHLY") }

    val assignedSiteId = staffToEdit?.siteId ?: (currentUser?.assignedSiteId ?: (sites.firstOrNull()?.id ?: 1L))
    val assignedSiteName = staffToEdit?.siteName ?: (currentUser?.assignedSiteName ?: (sites.firstOrNull()?.name ?: "Main Site"))

    var selectedSiteId by remember { mutableStateOf(assignedSiteId) }
    var selectedSiteName by remember { mutableStateOf(assignedSiteName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (staffToEdit == null) "Add Staff / Labour Member" else "Edit Staff Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Staff Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation (e.g. Mason, Supervisor)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { paymentType = "MONTHLY" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (paymentType == "MONTHLY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Monthly Salary", color = if (paymentType == "MONTHLY") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { paymentType = "DAILY" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (paymentType == "DAILY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Daily Wage", color = if (paymentType == "DAILY") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                OutlinedTextField(
                    value = wageStr,
                    onValueChange = { wageStr = it },
                    label = { Text(if (paymentType == "MONTHLY") "Monthly Salary (₹) *" else "Daily Wage (₹) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) return@Button
                val wage = wageStr.toDoubleOrNull() ?: 0.0
                onSave(name.trim(), mobile.trim(), designation.trim(), wage, paymentType, selectedSiteId, selectedSiteName)
            }) {
                Text("Save Staff")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalaryCalculatorDialog(
    staff: StaffMember,
    attendanceList: List<StaffAttendance>,
    onDismiss: () -> Unit,
    onSaveRecord: (SalaryRecord) -> Unit
) {
    val presentCount = attendanceList.count { it.status == "PRESENT" }
    val absentCount = attendanceList.count { it.status == "ABSENT" }
    val halfDayCount = attendanceList.count { it.status == "HALF_DAY" }
    val leaveCount = attendanceList.count { it.status == "LEAVE" }
    val totalOt = attendanceList.sumOf { it.overtimeHours }
    val otAmount = totalOt * 150.0

    val calculatedBasic = if (staff.paymentType == "MONTHLY") staff.dailyWage else (presentCount * staff.dailyWage + halfDayCount * (staff.dailyWage / 2.0))

    var bonusStr by remember { mutableStateOf("0") }
    var advanceStr by remember { mutableStateOf("0") }
    var deductionStr by remember { mutableStateOf("0") }
    var paymentMode by remember { mutableStateOf("Bank") }
    var paymentStatus by remember { mutableStateOf("PAID") }

    val bonus = bonusStr.toDoubleOrNull() ?: 0.0
    val advance = advanceStr.toDoubleOrNull() ?: 0.0
    val deduction = deductionStr.toDoubleOrNull() ?: 0.0
    val netSalary = (calculatedBasic + otAmount + bonus) - (advance + deduction)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calculate & Pay Salary - ${staff.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Role: ${staff.designation} (${staff.paymentType})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Attendance Summary: Present: $presentCount | Absent: $absentCount | Half-Day: $halfDayCount | OT: ${totalOt} hrs", style = MaterialTheme.typography.bodySmall)

                HorizontalDivider()

                Text("Base / Calculated Salary: ₹${"%.2f".format(calculatedBasic)}")
                Text("Overtime Amount (${totalOt} hrs): +₹${"%.2f".format(otAmount)}")

                OutlinedTextField(
                    value = bonusStr,
                    onValueChange = { bonusStr = it },
                    label = { Text("Bonus / Allowance (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = advanceStr,
                    onValueChange = { advanceStr = it },
                    label = { Text("Advance Recovery (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = deductionStr,
                    onValueChange = { deductionStr = it },
                    label = { Text("Other Deductions (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("NET SALARY:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("₹${"%.2f".format(netSalary)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cash", "Bank", "UPI").forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val record = SalaryRecord(
                    staffId = staff.id,
                    staffName = staff.name,
                    siteId = staff.siteId,
                    siteName = staff.siteName,
                    monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
                    basicSalary = calculatedBasic,
                    presentDays = presentCount,
                    absentDays = absentCount,
                    halfDays = halfDayCount,
                    leaveDays = leaveCount,
                    overtimeHours = totalOt,
                    overtimeAmount = otAmount,
                    bonus = bonus,
                    advance = advance,
                    deduction = deduction,
                    netSalary = netSalary,
                    paymentStatus = paymentStatus,
                    paymentMode = paymentMode,
                    paidDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    remarks = "Monthly Salary Settlement"
                )
                onSaveRecord(record)
            }) {
                Text("Approve & Pay Salary")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SalarySlipDialog(
    record: SalaryRecord,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Professional Salary Slip")
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share Slip")
                }
            }
        },
        text = {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("CONSTRUCTION KHATA & ERP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    Text("Site: ${record.siteName}", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()

                    Text("Employee: ${record.staffName}", fontWeight = FontWeight.Bold)
                    Text("Salary Month: ${record.monthYear}")

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Earnings:", fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Basic Salary / Wages")
                        Text("₹${record.basicSalary}")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Overtime (${record.overtimeHours} hrs)")
                        Text("+₹${record.overtimeAmount}")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bonus & Allowance")
                        Text("+₹${record.bonus}")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Deductions:", fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Advance Recovery")
                        Text("-₹${record.advance}")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Other Deductions")
                        Text("-₹${record.deduction}")
                    }

                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("NET PAYABLE:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("₹${record.netSalary}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Payment Status: ${record.paymentStatus} via ${record.paymentMode}", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share Slip")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
