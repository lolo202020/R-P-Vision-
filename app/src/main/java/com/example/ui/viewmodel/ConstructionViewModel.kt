package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLog
import com.example.data.model.CategoryConstants
import com.example.data.model.SalaryRecord
import com.example.data.model.Site
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.TransactionEntry
import com.example.data.model.User
import com.example.data.repository.ConstructionRepository
import com.example.util.SupabaseSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SiteSummaryStats(
    val site: Site,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val entriesCount: Int
)

data class CategorySummaryStats(
    val category: String,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int,
    val isIncome: Boolean
)

data class PaymentModeStats(
    val mode: String,
    val incomeAmount: Double,
    val expenseAmount: Double,
    val netAmount: Double
)

data class InchargeSummaryStats(
    val user: User,
    val siteName: String,
    val totalEntries: Int,
    val totalIncomeEntered: Double,
    val totalExpenseEntered: Double,
    val lastEntryDate: String?
)

data class OverallFinancialSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val bankBalance: Double = 0.0,
    val upiBalance: Double = 0.0,
    val otherBalance: Double = 0.0,
    val loanReceived: Double = 0.0,
    val loanReturn: Double = 0.0,
    val materialExpense: Double = 0.0,
    val labourExpense: Double = 0.0,
    val machineryExpense: Double = 0.0,
    val fuelExpense: Double = 0.0
)

data class AdminFilterCriteria(
    val siteId: Long? = null,
    val category: String? = null,
    val paymentMode: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val query: String = ""
)

class ConstructionViewModel(
    private val repository: ConstructionRepository,
    val syncManager: SupabaseSyncManager? = null,
    private val context: Context? = null
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _cloudConfigured = MutableStateFlow(syncManager?.isConfigured() == true)
    val isCloudConfigured: StateFlow<Boolean> = _cloudConfigured.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(syncManager?.getLastSyncTime() ?: 0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Ensure Admin user has password 20262026
            val adminUser = repository.getUserByMobile("9621803006")
            if (adminUser != null) {
                if (adminUser.password != "20262026") {
                    repository.insertUser(adminUser.copy(password = "20262026"))
                }
            } else {
                val defaultAdmin = User(
                    id = 1,
                    name = "Admin (Director)",
                    mobile = "9621803006",
                    password = "20262026",
                    role = "ADMIN",
                    assignedSiteId = null,
                    assignedSiteName = null,
                    designation = "Managing Director / Admin"
                )
                repository.insertUser(defaultAdmin)
            }

            // Remove any demo project sites and demo users if present
            val existingSites = repository.getAllSitesList()
            val demoSiteCodes = setOf("PRJ-A", "PRJ-B", "PRJ-C")
            val demoSiteNames = setOf("Metro City Tower", "NH-48 Highway Bypass", "Riverfront Commercial Hub")
            for (site in existingSites) {
                if (site.code in demoSiteCodes || site.name in demoSiteNames) {
                    repository.deleteSite(site.id)
                }
            }

            val existingUsers = repository.getAllUsersList()
            val demoMobiles = setOf("9876500002", "9876500003", "9876500004")
            for (u in existingUsers) {
                if (u.mobile in demoMobiles && u.role == "SITE_INCHARGE") {
                    repository.deleteUser(u.id)
                }
            }

            // Restore user session if previously logged in (persists across app restarts until logout)
            if (context != null) {
                val prefs = context.getSharedPreferences("rpvc_user_session", Context.MODE_PRIVATE)
                val savedUserId = prefs.getLong("saved_user_id", -1L)
                val savedMobile = prefs.getString("saved_user_mobile", null)

                if (savedUserId > 0 || !savedMobile.isNullOrBlank()) {
                    var restoredUser = if (savedUserId > 0) {
                        repository.getUserById(savedUserId) ?: (if (!savedMobile.isNullOrBlank()) repository.getUserByMobile(savedMobile) else null)
                    } else {
                        repository.getUserByMobile(savedMobile!!)
                    }

                    if (restoredUser != null) {
                        if (restoredUser.role == "SITE_INCHARGE" && restoredUser.assignedSiteId == null) {
                            val sites = repository.getAllSitesList()
                            val matchingSite = sites.firstOrNull { it.mobile.trim() == restoredUser.mobile.trim() }
                            if (matchingSite != null) {
                                restoredUser = restoredUser.copy(
                                    assignedSiteId = matchingSite.id,
                                    assignedSiteName = matchingSite.name
                                )
                                repository.insertUser(restoredUser)
                            }
                        }
                        _currentUser.value = restoredUser
                    }
                }
            }
        }

        // Background sync loop - fully asynchronous on IO dispatcher with delayed start
        viewModelScope.launch(Dispatchers.IO) {
            // Delay initial background sync so UI loads instantly without network lag
            kotlinx.coroutines.delay(2000)
            if (syncManager != null && syncManager.isConfigured() && syncManager.isAutoSyncEnabled()) {
                try {
                    syncManager.syncAll(repository)
                    _lastSyncTime.value = syncManager.getLastSyncTime()
                } catch (e: Exception) {
                    // silent initial sync
                }
            }

            while (true) {
                kotlinx.coroutines.delay(20000)
                if (syncManager != null && syncManager.isConfigured() && syncManager.isAutoSyncEnabled() && !_isSyncing.value) {
                    try {
                        syncManager.syncAll(repository)
                        _lastSyncTime.value = syncManager.getLastSyncTime()
                    } catch (e: Exception) {
                        // silent background loop
                    }
                }
            }
        }
    }

    fun saveCloudConfig(url: String, key: String, autoSync: Boolean = true) {
        syncManager?.saveConfig(url, key, autoSync)
        _cloudConfigured.value = syncManager?.isConfigured() == true
        _lastSyncTime.value = syncManager?.getLastSyncTime() ?: 0L
        if (_cloudConfigured.value) {
            syncNow()
        }
    }

    fun testCloudConnection(url: String, key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (syncManager == null) {
                onResult(false, "Sync manager is not initialized.")
                return@launch
            }
            val res = syncManager.testConnection(url, key)
            if (res.isSuccess) {
                onResult(true, res.getOrNull() ?: "Connected successfully!")
            } else {
                onResult(false, res.exceptionOrNull()?.localizedMessage ?: "Connection failed")
            }
        }
    }

    fun syncNow(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            if (syncManager == null || !syncManager.isConfigured()) {
                val msg = "Please configure Supabase URL & Key in settings."
                _uiMessage.value = msg
                onComplete?.invoke(false, msg)
                return@launch
            }

            _isSyncing.value = true
            val result = syncManager.syncAll(repository)
            _isSyncing.value = false

            if (result.isSuccess) {
                _lastSyncTime.value = syncManager.getLastSyncTime()
                val summary = result.getOrNull()
                val msg = "Sync successful! (Tx: ${summary?.transactionsPulled ?: 0} pulled, Sites: ${summary?.sitesPulled ?: 0})"
                _uiMessage.value = msg
                onComplete?.invoke(true, msg)
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Sync failed"
                _uiMessage.value = "Sync error: $err"
                onComplete?.invoke(false, err)
            }
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSites: StateFlow<List<Site>> = repository.allSites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTransactions: StateFlow<List<TransactionEntry>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- Authentication & Session State ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val allStaff: StateFlow<List<StaffMember>> = repository.allStaff
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allAttendance: StateFlow<List<StaffAttendance>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSalaryRecords: StateFlow<List<SalaryRecord>> = repository.allSalaryRecords
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allAuditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val inchargeStaff: StateFlow<List<StaffMember>> = combine(_currentUser, allStaff) { user, staff ->
        if (user == null) emptyList()
        else if (user.role == "ADMIN") staff
        else staff.filter { it.siteId == user.assignedSiteId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inchargeAttendance: StateFlow<List<StaffAttendance>> = combine(_currentUser, allAttendance) { user, att ->
        if (user == null) emptyList()
        else if (user.role == "ADMIN") att
        else att.filter { it.siteId == user.assignedSiteId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inchargeSalaryRecords: StateFlow<List<SalaryRecord>> = combine(_currentUser, allSalaryRecords) { user, records ->
        if (user == null) emptyList()
        else if (user.role == "ADMIN") records
        else records.filter { it.siteId == user.assignedSiteId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // --- Incharge View Filters & Search ---
    private val _inchargeSearchQuery = MutableStateFlow("")
    val inchargeSearchQuery: StateFlow<String> = _inchargeSearchQuery.asStateFlow()

    private val _inchargeTypeFilter = MutableStateFlow("ALL") // ALL, INCOME, EXPENSE
    val inchargeTypeFilter: StateFlow<String> = _inchargeTypeFilter.asStateFlow()

    private val _inchargeFromDate = MutableStateFlow<String?>(null) // YYYY-MM-DD or null
    val inchargeFromDate: StateFlow<String?> = _inchargeFromDate.asStateFlow()

    private val _inchargeToDate = MutableStateFlow<String?>(null) // YYYY-MM-DD or null
    val inchargeToDate: StateFlow<String?> = _inchargeToDate.asStateFlow()

    // --- Admin Filters ---
    private val _adminSelectedSiteId = MutableStateFlow<Long?>(null) // null = All Sites
    val adminSelectedSiteId: StateFlow<Long?> = _adminSelectedSiteId.asStateFlow()

    private val _adminSelectedCategory = MutableStateFlow<String?>(null)
    val adminSelectedCategory: StateFlow<String?> = _adminSelectedCategory.asStateFlow()

    private val _adminSelectedPaymentMode = MutableStateFlow<String?>(null)
    val adminSelectedPaymentMode: StateFlow<String?> = _adminSelectedPaymentMode.asStateFlow()

    private val _adminSearchQuery = MutableStateFlow("")
    val adminSearchQuery: StateFlow<String> = _adminSearchQuery.asStateFlow()

    private val _adminFromDate = MutableStateFlow<String?>(null) // YYYY-MM-DD or null
    val adminFromDate: StateFlow<String?> = _adminFromDate.asStateFlow()

    private val _adminToDate = MutableStateFlow<String?>(null) // YYYY-MM-DD or null
    val adminToDate: StateFlow<String?> = _adminToDate.asStateFlow()

    // --- UI Success / Message Feedback ---
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private data class InchargeFilterState(
        val query: String = "",
        val typeFilter: String = "ALL",
        val fromDate: String? = null,
        val toDate: String? = null
    )

    private val inchargeFilterState: StateFlow<InchargeFilterState> = combine(
        _inchargeSearchQuery,
        _inchargeTypeFilter,
        _inchargeFromDate,
        _inchargeToDate
    ) { query, typeFilter, fromDate, toDate ->
        InchargeFilterState(query, typeFilter, fromDate, toDate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InchargeFilterState())

    // Incharge specific entries (strictly for their assigned site)
    val inchargeTransactions: StateFlow<List<TransactionEntry>> = combine(
        allTransactions,
        _currentUser,
        inchargeFilterState
    ) { txList, user, filter ->
        if (user == null || user.role != "SITE_INCHARGE") return@combine emptyList()
        val siteId = user.assignedSiteId ?: return@combine emptyList()

        txList.filter { tx ->
            tx.siteId == siteId &&
            (filter.typeFilter == "ALL" || tx.type == filter.typeFilter) &&
            (filter.fromDate == null || tx.dateFormatted >= filter.fromDate) &&
            (filter.toDate == null || tx.dateFormatted <= filter.toDate) &&
            (filter.query.isEmpty() ||
                tx.partyName.contains(filter.query, ignoreCase = true) ||
                tx.category.contains(filter.query, ignoreCase = true) ||
                tx.subCategory.contains(filter.query, ignoreCase = true) ||
                tx.description.contains(filter.query, ignoreCase = true) ||
                tx.amount.toString().contains(filter.query))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Incharge Summary Stats
    val inchargeStats: StateFlow<OverallFinancialSummary> = combine(
        allTransactions,
        _currentUser
    ) { txList, user ->
        if (user == null || user.role != "SITE_INCHARGE") return@combine OverallFinancialSummary()
        val siteId = user.assignedSiteId ?: return@combine OverallFinancialSummary()
        val siteTx = txList.filter { it.siteId == siteId }
        computeSummary(siteTx)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallFinancialSummary())

    private data class AdminDateAndSearch(
        val fromDate: String? = null,
        val toDate: String? = null,
        val query: String = ""
    )

    private val adminDateAndSearch: StateFlow<AdminDateAndSearch> = combine(
        _adminFromDate,
        _adminToDate,
        _adminSearchQuery
    ) { fromDate, toDate, query ->
        AdminDateAndSearch(fromDate, toDate, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDateAndSearch())

    // Admin Filter Combined
    private val adminFilterCriteria: StateFlow<AdminFilterCriteria> = combine(
        _adminSelectedSiteId,
        _adminSelectedCategory,
        _adminSelectedPaymentMode,
        adminDateAndSearch
    ) { siteId, category, paymentMode, dateAndSearch ->
        AdminFilterCriteria(
            siteId = siteId,
            category = category,
            paymentMode = paymentMode,
            fromDate = dateAndSearch.fromDate,
            toDate = dateAndSearch.toDate,
            query = dateAndSearch.query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminFilterCriteria())

    // Admin Filtered Transactions
    val adminFilteredTransactions: StateFlow<List<TransactionEntry>> = combine(
        allTransactions,
        adminFilterCriteria
    ) { txList, filter ->
        txList.filter { tx ->
            (filter.siteId == null || tx.siteId == filter.siteId) &&
            (filter.category == null || tx.category == filter.category) &&
            (filter.paymentMode == null || tx.paymentMode == filter.paymentMode) &&
            (filter.fromDate == null || tx.dateFormatted >= filter.fromDate) &&
            (filter.toDate == null || tx.dateFormatted <= filter.toDate) &&
            (filter.query.isEmpty() ||
                tx.partyName.contains(filter.query, ignoreCase = true) ||
                tx.siteName.contains(filter.query, ignoreCase = true) ||
                tx.userName.contains(filter.query, ignoreCase = true) ||
                tx.category.contains(filter.query, ignoreCase = true) ||
                tx.subCategory.contains(filter.query, ignoreCase = true) ||
                tx.description.contains(filter.query, ignoreCase = true) ||
                tx.paymentMode.contains(filter.query, ignoreCase = true) ||
                tx.amount.toString().contains(filter.query))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Overall Summary (respecting site/date filters)
    val adminSummary: StateFlow<OverallFinancialSummary> = adminFilteredTransactions.map { txList ->
        computeSummary(txList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallFinancialSummary())

    // Admin Site-wise Stats
    val adminSiteStats: StateFlow<List<SiteSummaryStats>> = combine(
        allSites,
        allTransactions
    ) { sites, txList ->
        sites.map { site ->
            val siteTxs = txList.filter { it.siteId == site.id }
            val income = siteTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = siteTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            SiteSummaryStats(
                site = site,
                totalIncome = income,
                totalExpense = expense,
                netBalance = income - expense,
                entriesCount = siteTxs.size
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Category-wise Expense Stats
    val adminCategoryExpenseStats: StateFlow<List<CategorySummaryStats>> = adminFilteredTransactions.map { txList ->
        val expenseTxs = txList.filter { it.type == "EXPENSE" }
        val totalExpense = expenseTxs.sumOf { it.amount }
        if (totalExpense <= 0.0) {
            emptyList()
        } else {
            val grouped = expenseTxs.groupBy { it.category }
            grouped.map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                CategorySummaryStats(
                    category = cat,
                    totalAmount = sum,
                    percentage = ((sum / totalExpense) * 100).toFloat(),
                    count = list.size,
                    isIncome = false
                )
            }.sortedByDescending { it.totalAmount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Payment Mode Stats
    val adminPaymentModeStats: StateFlow<List<PaymentModeStats>> = adminFilteredTransactions.map { txList ->
        CategoryConstants.PAYMENT_MODES.map { mode ->
            val modeTxs = txList.filter { it.paymentMode.equals(mode, ignoreCase = true) }
            val income = modeTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = modeTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            PaymentModeStats(
                mode = mode,
                incomeAmount = income,
                expenseAmount = expense,
                netAmount = income - expense
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin User & Incharge Performance / Activity
    val adminInchargeStats: StateFlow<List<InchargeSummaryStats>> = combine(
        allUsers,
        allTransactions
    ) { users, txList ->
        users.map { user ->
            val userTxs = txList.filter { it.userId == user.id }
            val income = userTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = userTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val latest = userTxs.maxByOrNull { it.dateMillis }?.dateFormatted
            InchargeSummaryStats(
                user = user,
                siteName = user.assignedSiteName ?: if (user.role == "ADMIN") "All Sites (Admin)" else "Not Assigned",
                totalEntries = userTxs.size,
                totalIncomeEntered = income,
                totalExpenseEntered = expense,
                lastEntryDate = latest
            )
        }.sortedWith(compareBy<InchargeSummaryStats> { if (it.user.role == "ADMIN") 0 else 1 }.thenBy { it.user.name.lowercase() })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun persistUserSession(user: User?) {
        if (context != null) {
            val prefs = context.getSharedPreferences("rpvc_user_session", Context.MODE_PRIVATE)
            if (user != null) {
                prefs.edit()
                    .putLong("saved_user_id", user.id)
                    .putString("saved_user_mobile", user.mobile)
                    .putString("saved_user_role", user.role)
                    .apply()
            } else {
                prefs.edit().clear().apply()
            }
        }
    }

    // --- Authentication Actions ---
    fun login(
        mobile: String,
        pass: String,
        selectedSiteId: Long? = null,
        onRoleDecided: (String) -> Unit
    ) {
        viewModelScope.launch {
            _loginError.value = null

            if (mobile.isBlank() || pass.isBlank()) {
                _loginError.value = "Please enter both mobile number and password"
                return@launch
            }

            val trimmedMobile = mobile.trim()
            val trimmedPass = pass.trim()

            var user: User? = null
            val users = repository.getAllUsersList()
            val sites = repository.getAllSitesList()

            // 1. Check admin credentials explicitly (Password: 20262026 or admin)
            if ((trimmedMobile == "9621803006" || trimmedMobile == "9876500001") && (trimmedPass == "20262026" || trimmedPass == "admin")) {
                user = users.firstOrNull { it.role == "ADMIN" && it.mobile == trimmedMobile } ?: User(
                    id = 1,
                    name = "Admin (Director)",
                    mobile = trimmedMobile,
                    password = trimmedPass,
                    role = "ADMIN",
                    assignedSiteId = null,
                    assignedSiteName = null,
                    designation = "Managing Director / Admin"
                )
            } else {
                // 2. Check if user exists in database with matching mobile and password
                user = users.firstOrNull { it.mobile.trim() == trimmedMobile && it.password.trim() == trimmedPass }

                // 3. Fallback: Check if any site has this incharge mobile and password
                if (user == null) {
                    val matchingSite = sites.firstOrNull {
                        it.mobile.trim() == trimmedMobile && it.password.trim() == trimmedPass
                    }
                    if (matchingSite != null) {
                        val newUser = User(
                            id = 0L,
                            name = matchingSite.inchargeName.ifBlank { "Site Incharge" },
                            mobile = matchingSite.mobile.trim(),
                            password = matchingSite.password.trim(),
                            role = "SITE_INCHARGE",
                            assignedSiteId = matchingSite.id,
                            assignedSiteName = matchingSite.name,
                            designation = "Site Manager"
                        )
                        val newUserId = repository.insertUser(newUser)
                        val createdUser = newUser.copy(id = newUserId)
                        user = createdUser
                        viewModelScope.launch { syncManager?.pushUser(createdUser) }
                    }
                }
            }

            if (user == null) {
                _loginError.value = "Invalid mobile number or password. Please verify credentials."
                return@launch
            }

            // If user is a Site Incharge, ensure assignedSiteId and assignedSiteName are accurately set
            if (user.role == "SITE_INCHARGE") {
                if (user.assignedSiteId == null) {
                    val matchingSite = sites.firstOrNull { it.mobile.trim() == user.mobile.trim() }
                        ?: (if (selectedSiteId != null) sites.firstOrNull { it.id == selectedSiteId } else sites.firstOrNull())
                    if (matchingSite != null) {
                        user = user.copy(assignedSiteId = matchingSite.id, assignedSiteName = matchingSite.name)
                        repository.insertUser(user)
                    }
                } else {
                    val matchingSite = sites.firstOrNull { it.id == user.assignedSiteId }
                    if (matchingSite != null && user.assignedSiteName != matchingSite.name) {
                        user = user.copy(assignedSiteName = matchingSite.name)
                        repository.insertUser(user)
                    }
                }
            }

            persistUserSession(user)
            _currentUser.value = user
            _loginError.value = null
            onRoleDecided(user.role)
        }
    }

    fun selectUserProfile(user: User, onRoleDecided: (String) -> Unit) {
        persistUserSession(user)
        _currentUser.value = user
        _loginError.value = null
        onRoleDecided(user.role)
    }

    fun demoLogin(user: User, onRoleDecided: (String) -> Unit) {
        selectUserProfile(user, onRoleDecided)
    }

    fun clearAllOperationalData(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllOperationalData()
            withContext(Dispatchers.Main) {
                onSuccess?.invoke()
            }
        }
    }

    fun logout() {
        persistUserSession(null)
        _currentUser.value = null
        _loginError.value = null
        _inchargeSearchQuery.value = ""
        _inchargeTypeFilter.value = "ALL"
        _inchargeFromDate.value = null
        _inchargeToDate.value = null
        _adminSelectedSiteId.value = null
        _adminSelectedCategory.value = null
        _adminSelectedPaymentMode.value = null
        _adminFromDate.value = null
        _adminToDate.value = null
        _adminSearchQuery.value = ""
    }

    // --- Incharge Actions ---
    fun setInchargeSearch(query: String) {
        _inchargeSearchQuery.value = query
    }

    fun setInchargeType(type: String) {
        _inchargeTypeFilter.value = type
    }

    fun setInchargeDate(dateStr: String?) {
        _inchargeFromDate.value = dateStr
        _inchargeToDate.value = dateStr
    }

    fun setInchargeFromDate(dateStr: String?) {
        _inchargeFromDate.value = dateStr
    }

    fun setInchargeToDate(dateStr: String?) {
        _inchargeToDate.value = dateStr
    }

    fun setInchargeDateRange(fromDate: String?, toDate: String?) {
        _inchargeFromDate.value = fromDate
        _inchargeToDate.value = toDate
    }

    fun clearInchargeDateFilter() {
        _inchargeFromDate.value = null
        _inchargeToDate.value = null
    }

    fun submitExpense(
        dateMillis: Long,
        category: String,
        subCategory: String,
        partyName: String,
        description: String,
        amount: Double,
        paymentMode: String,
        receiptPhotoUri: String?,
        remarks: String?,
        onSuccess: () -> Unit
    ) {
        val user = _currentUser.value ?: return
        if (user.role != "SITE_INCHARGE") {
            _uiMessage.value = "Admin cannot add or edit site entries (View Only mode)"
            return
        }
        val siteId = user.assignedSiteId ?: return

        viewModelScope.launch {
            val dateStr = dateFormat.format(Date(dateMillis))
            val entry = TransactionEntry(
                type = "EXPENSE",
                dateMillis = dateMillis,
                dateFormatted = dateStr,
                siteId = siteId,
                siteName = user.assignedSiteName ?: "Assigned Site",
                userId = user.id,
                userName = user.name,
                userMobile = user.mobile,
                category = category,
                subCategory = subCategory,
                partyName = partyName,
                description = description,
                amount = amount,
                paymentMode = paymentMode,
                receiptPhotoUri = receiptPhotoUri,
                remarks = remarks
            )
            val insertedId = repository.insertTransaction(entry)
            val effectiveTx = entry.copy(id = insertedId)
            viewModelScope.launch { syncManager?.pushTransaction(effectiveTx) }
            _uiMessage.value = "Expense of ₹$amount recorded successfully for ${user.assignedSiteName}"
            onSuccess()
        }
    }

    fun submitIncome(
        dateMillis: Long,
        category: String,
        sourceParty: String,
        description: String,
        amount: Double,
        paymentMode: String,
        receiptPhotoUri: String?,
        remarks: String?,
        onSuccess: () -> Unit
    ) {
        val user = _currentUser.value ?: return
        if (user.role != "SITE_INCHARGE") {
            _uiMessage.value = "Admin cannot add site income entries (View Only mode)"
            return
        }
        val siteId = user.assignedSiteId ?: return

        viewModelScope.launch {
            val dateStr = dateFormat.format(Date(dateMillis))
            val entry = TransactionEntry(
                type = "INCOME",
                dateMillis = dateMillis,
                dateFormatted = dateStr,
                siteId = siteId,
                siteName = user.assignedSiteName ?: "Assigned Site",
                userId = user.id,
                userName = user.name,
                userMobile = user.mobile,
                category = category,
                subCategory = "Income Entry",
                partyName = sourceParty,
                description = description,
                amount = amount,
                paymentMode = paymentMode,
                receiptPhotoUri = receiptPhotoUri,
                remarks = remarks
            )
            val insertedId = repository.insertTransaction(entry)
            val effectiveTx = entry.copy(id = insertedId)
            viewModelScope.launch { syncManager?.pushTransaction(effectiveTx) }
            _uiMessage.value = "Income of ₹$amount added successfully for ${user.assignedSiteName}"
            onSuccess()
        }
    }

    fun updateTransaction(
        updatedTx: TransactionEntry,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.insertTransaction(updatedTx)
                viewModelScope.launch { syncManager?.pushTransaction(updatedTx) }
                _uiMessage.value = "${if (updatedTx.type == "INCOME") "Income" else "Expense"} entry #${updatedTx.id} updated successfully"
                onSuccess()
            } catch (e: Exception) {
                _uiMessage.value = "Failed to update transaction: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTransaction(
        txId: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(txId)
                viewModelScope.launch { syncManager?.deleteTransaction(txId) }
                _uiMessage.value = "Transaction #$txId deleted successfully"
                onSuccess()
            } catch (e: Exception) {
                _uiMessage.value = "Failed to delete transaction: ${e.localizedMessage}"
            }
        }
    }

    // --- Admin Actions ---
    fun setAdminSiteFilter(siteId: Long?) {
        _adminSelectedSiteId.value = siteId
    }

    fun setAdminCategoryFilter(category: String?) {
        _adminSelectedCategory.value = category
    }

    fun setAdminPaymentModeFilter(paymentMode: String?) {
        _adminSelectedPaymentMode.value = paymentMode
    }

    fun setAdminDateFilter(dateStr: String?) {
        _adminFromDate.value = dateStr
        _adminToDate.value = dateStr
    }

    fun setAdminFromDate(dateStr: String?) {
        _adminFromDate.value = dateStr
    }

    fun setAdminToDate(dateStr: String?) {
        _adminToDate.value = dateStr
    }

    fun setAdminDateRange(fromDate: String?, toDate: String?) {
        _adminFromDate.value = fromDate
        _adminToDate.value = toDate
    }

    fun clearAdminDateRange() {
        _adminFromDate.value = null
        _adminToDate.value = null
    }

    fun clearAdminDateFilter() {
        _adminFromDate.value = null
        _adminToDate.value = null
    }

    fun setAdminSearch(query: String) {
        _adminSearchQuery.value = query
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    private fun computeSummary(txList: List<TransactionEntry>): OverallFinancialSummary {
        var incomeTotal = 0.0
        var expenseTotal = 0.0

        var cashBal = 0.0
        var bankBal = 0.0
        var upiBal = 0.0
        var otherBal = 0.0

        var loanRec = 0.0
        var loanRet = 0.0

        var matExp = 0.0
        var labExp = 0.0
        var machExp = 0.0
        var fuelExp = 0.0

        for (tx in txList) {
            val amt = tx.amount
            val isInc = tx.type == "INCOME"

            if (isInc) {
                incomeTotal += amt
            } else {
                expenseTotal += amt
            }

            // Payment modes
            val signedAmt = if (isInc) amt else -amt
            when (tx.paymentMode.uppercase(Locale.ROOT)) {
                "CASH" -> cashBal += signedAmt
                "BANK" -> bankBal += signedAmt
                "UPI" -> upiBal += signedAmt
                else -> otherBal += signedAmt
            }

            // Loan tracking
            if (isInc && tx.category.equals("Loan Received", ignoreCase = true)) {
                loanRec += amt
            }
            if (!isInc && tx.category.equals("Loan Return", ignoreCase = true)) {
                loanRet += amt
            }

            // Key expense categories
            if (!isInc) {
                when {
                    tx.category.equals("Material Purchase", ignoreCase = true) -> matExp += amt
                    tx.category.equals("Site Labour", ignoreCase = true) -> labExp += amt
                    tx.category.equals("Machinery & Equipment", ignoreCase = true) -> machExp += amt
                    tx.category.equals("Machinery Fuel", ignoreCase = true) -> fuelExp += amt
                }
            }
        }

        return OverallFinancialSummary(
            totalIncome = incomeTotal,
            totalExpense = expenseTotal,
            netBalance = incomeTotal - expenseTotal,
            cashBalance = cashBal,
            bankBalance = bankBal,
            upiBalance = upiBal,
            otherBalance = otherBal,
            loanReceived = loanRec,
            loanReturn = loanRet,
            materialExpense = matExp,
            labourExpense = labExp,
            machineryExpense = machExp,
            fuelExpense = fuelExp
        )
    }

    fun insertImportedTransactions(txList: List<TransactionEntry>, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                if (txList.isEmpty()) {
                    onResult(false, "No transactions to import.")
                    return@launch
                }
                repository.insertTransactions(txList)
                _uiMessage.value = "Day Book Excel imported successfully."
                onResult(true, "Day Book Excel imported successfully. (${txList.size} records added)")
            } catch (e: Exception) {
                onResult(false, "Import failed: ${e.localizedMessage}")
            }
        }
    }


    fun exportDatabaseJson(): String {
        return try {
            val root = org.json.JSONObject()
            root.put("version", 1)
            root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

            val sitesArray = org.json.JSONArray()
            for (s in allSites.value) {
                val obj = org.json.JSONObject()
                obj.put("id", s.id)
                obj.put("name", s.name)
                obj.put("code", s.code)
                obj.put("location", s.location)
                obj.put("clientName", s.clientName)
                obj.put("budget", s.budget)
                obj.put("inchargeName", s.inchargeName)
                sitesArray.put(obj)
            }
            root.put("sites", sitesArray)

            val txArray = org.json.JSONArray()
            for (t in allTransactions.value) {
                val obj = org.json.JSONObject()
                obj.put("id", t.id)
                obj.put("type", t.type)
                obj.put("dateMillis", t.dateMillis)
                obj.put("dateFormatted", t.dateFormatted)
                obj.put("siteId", t.siteId)
                obj.put("siteName", t.siteName)
                obj.put("userId", t.userId)
                obj.put("userName", t.userName)
                obj.put("userMobile", t.userMobile)
                obj.put("category", t.category)
                obj.put("subCategory", t.subCategory)
                obj.put("partyName", t.partyName)
                obj.put("description", t.description)
                obj.put("amount", t.amount)
                obj.put("paymentMode", t.paymentMode)
                obj.put("receiptPhotoUri", t.receiptPhotoUri ?: "")
                obj.put("remarks", t.remarks ?: "")
                obj.put("createdAt", t.createdAt)
                txArray.put(obj)
            }
            root.put("transactions", txArray)

            root.toString(2)
        } catch (e: Exception) {
            "{\"error\": \"${e.message}\"}"
        }
    }

    fun importDatabaseJson(jsonStr: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(jsonStr)
                val sitesArray = root.optJSONArray("sites") ?: org.json.JSONArray()
                val txArray = root.optJSONArray("transactions") ?: org.json.JSONArray()

                val importedSites = mutableListOf<Site>()
                for (i in 0 until sitesArray.length()) {
                    val obj = sitesArray.getJSONObject(i)
                    importedSites.add(
                        Site(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            code = obj.getString("code"),
                            location = obj.getString("location"),
                            clientName = obj.getString("clientName"),
                            budget = obj.optDouble("budget", 0.0),
                            inchargeName = obj.getString("inchargeName")
                        )
                    )
                }

                val importedTxs = mutableListOf<TransactionEntry>()
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    importedTxs.add(
                        TransactionEntry(
                            id = obj.optLong("id", 0L),
                            type = obj.getString("type"),
                            dateMillis = obj.getLong("dateMillis"),
                            dateFormatted = obj.getString("dateFormatted"),
                            siteId = obj.getLong("siteId"),
                            siteName = obj.getString("siteName"),
                            userId = obj.getLong("userId"),
                            userName = obj.getString("userName"),
                            userMobile = obj.getString("userMobile"),
                            category = obj.getString("category"),
                            subCategory = obj.getString("subCategory"),
                            partyName = obj.optString("partyName", ""),
                            description = obj.optString("description", ""),
                            amount = obj.getDouble("amount"),
                            paymentMode = obj.getString("paymentMode"),
                            receiptPhotoUri = obj.optString("receiptPhotoUri", "").ifEmpty { null },
                            remarks = obj.optString("remarks", "").ifEmpty { null },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }

                repository.insertSites(importedSites)
                repository.insertTransactions(importedTxs)
                onResult(true, "Successfully imported ${importedSites.size} sites and ${importedTxs.size} transactions!")
            } catch (e: Exception) {
                onResult(false, "Import failed: ${e.localizedMessage}")
            }
        }
    }

    fun addStaffMember(id: Long = 0, name: String, mobile: String, designation: String, dailyWage: Double, paymentType: String, siteId: Long, siteName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val staff = StaffMember(
                id = id,
                name = name,
                mobile = mobile,
                designation = designation,
                siteId = siteId,
                siteName = siteName,
                dailyWage = dailyWage,
                paymentType = paymentType
            )
            val staffId = repository.insertStaff(staff)
            val effectiveStaff = if (id == 0L) staff.copy(id = staffId) else staff
            viewModelScope.launch { syncManager?.pushStaff(effectiveStaff) }
            _uiMessage.value = if (id == 0L) "Staff member $name added successfully!" else "Staff member $name updated successfully!"
            onSuccess()
        }
    }

    fun deleteStaff(id: Long) {
        viewModelScope.launch {
            repository.deleteStaff(id)
            viewModelScope.launch { syncManager?.deleteStaff(id) }
            _uiMessage.value = "Staff member deleted successfully."
        }
    }

    fun markAttendance(
        staffId: Long,
        staffName: String,
        siteId: Long,
        siteName: String,
        dateMillis: Long,
        dateFormatted: String,
        status: String,
        wageAmount: Double,
        overtimeHours: Double = 0.0,
        checkInTime: String? = null,
        checkOutTime: String? = null,
        remarks: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val record = StaffAttendance(
                staffId = staffId,
                staffName = staffName,
                siteId = siteId,
                siteName = siteName,
                dateMillis = dateMillis,
                dateFormatted = dateFormatted,
                status = status,
                wageAmount = wageAmount,
                overtimeHours = overtimeHours,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                remarks = remarks
            )
            val attId = repository.insertAttendance(record)
            val effectiveAtt = record.copy(id = attId)
            viewModelScope.launch { syncManager?.pushAttendance(effectiveAtt) }
            _uiMessage.value = "Attendance marked for $staffName ($status)"
            onSuccess()
        }
    }

    fun deleteAttendance(id: Long) {
        viewModelScope.launch {
            repository.deleteAttendance(id)
            viewModelScope.launch { syncManager?.deleteAttendance(id) }
            _uiMessage.value = "Attendance record deleted."
        }
    }

    fun exportStaffAttendanceCsv(attendanceList: List<StaffAttendance>): String {
        val sb = StringBuilder()
        sb.append("ID,StaffId,StaffName,SiteName,Date,Status,WageAmount,Remarks\n")
        for (a in attendanceList) {
            val safeName = "\"${a.staffName.replace("\"", "\"\"")}\""
            val safeSite = "\"${a.siteName.replace("\"", "\"\"")}\""
            val safeRem = "\"${(a.remarks ?: "").replace("\"", "\"\"")}\""
            sb.append("${a.id},${a.staffId},${safeName},${safeSite},${a.dateFormatted},${a.status},${a.wageAmount},${safeRem}\n")
        }
        return sb.toString()
    }

    fun saveUser(
        id: Long = 0,
        name: String,
        mobile: String,
        password: String,
        role: String,
        assignedSiteId: Long?,
        assignedSiteName: String?,
        designation: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank() || mobile.isBlank() || password.isBlank()) {
                _uiMessage.value = "Please fill name, mobile number, and password"
                return@launch
            }
            val user = User(
                id = id,
                name = name.trim(),
                mobile = mobile.trim(),
                password = password.trim(),
                role = role,
                assignedSiteId = if (role == "ADMIN") null else assignedSiteId,
                assignedSiteName = if (role == "ADMIN") null else assignedSiteName,
                designation = designation.ifBlank { if (role == "ADMIN") "Managing Director / Admin" else "Site Manager" }
            )
            val savedId = repository.insertUser(user)
            val effectiveUserId = if (id > 0) id else savedId
            val effectiveUser = user.copy(id = effectiveUserId)

            // If current logged-in user is updated, refresh state
            if (_currentUser.value?.id == effectiveUserId) {
                _currentUser.value = effectiveUser
            }

            // Sync with assigned site incharge credentials if applicable
            if (role == "SITE_INCHARGE" && assignedSiteId != null) {
                val site = repository.getSiteById(assignedSiteId)
                if (site != null) {
                    val updatedSite = site.copy(
                        inchargeName = effectiveUser.name,
                        mobile = effectiveUser.mobile,
                        password = effectiveUser.password
                    )
                    repository.insertSite(updatedSite)
                    viewModelScope.launch { syncManager?.pushSite(updatedSite) }
                }
            }

            viewModelScope.launch { syncManager?.pushUser(effectiveUser) }
            logAudit(
                action = if (id > 0) "UPDATE_USER" else "CREATE_USER",
                details = "${if (id > 0) "Updated" else "Created"} user: ${effectiveUser.name} (${effectiveUser.role}) - ID: $effectiveUserId",
                siteId = assignedSiteId ?: 0
            )

            _uiMessage.value = if (id > 0) "User '${user.name}' updated successfully!" else "User '${user.name}' created successfully!"
            onSuccess()
        }
    }

    fun createUser(
        name: String,
        mobile: String,
        password: String,
        role: String,
        assignedSiteId: Long?,
        assignedSiteName: String?,
        designation: String,
        onSuccess: () -> Unit
    ) {
        saveUser(
            id = 0,
            name = name,
            mobile = mobile,
            password = password,
            role = role,
            assignedSiteId = assignedSiteId,
            assignedSiteName = assignedSiteName,
            designation = designation,
            onSuccess = onSuccess
        )
    }

    fun saveSite(
        id: Long = 0,
        name: String,
        code: String,
        location: String,
        clientName: String,
        budgetStr: String,
        inchargeName: String,
        inchargeMobile: String,
        inchargePassword: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank() || code.isBlank()) {
                _uiMessage.value = "Please enter site name and code"
                return@launch
            }
            val budget = budgetStr.toDoubleOrNull() ?: 0.0
            val site = Site(
                id = id,
                name = name.trim(),
                code = code.trim().uppercase(),
                location = location.trim(),
                clientName = clientName.trim(),
                budget = budget,
                inchargeName = inchargeName.trim(),
                mobile = inchargeMobile.trim(),
                password = inchargePassword.trim().ifBlank { "123456" }
            )
            val siteId = repository.insertSite(site)
            val effectiveSiteId = if (id == 0L) siteId else id
            val effectiveSite = site.copy(id = effectiveSiteId)
            viewModelScope.launch { syncManager?.pushSite(effectiveSite) }

            if (inchargeMobile.isNotBlank()) {
                val existingUsers = repository.getAllUsersList()
                val existingUser = existingUsers.firstOrNull {
                    it.assignedSiteId == effectiveSiteId || it.mobile.trim() == inchargeMobile.trim()
                }
                val user = User(
                    id = existingUser?.id ?: 0L,
                    name = inchargeName.trim().ifBlank { "Site Incharge" },
                    mobile = inchargeMobile.trim(),
                    password = inchargePassword.trim().ifBlank { "123456" },
                    role = "SITE_INCHARGE",
                    assignedSiteId = effectiveSiteId,
                    assignedSiteName = name.trim(),
                    designation = existingUser?.designation ?: "Site Manager"
                )
                val userId = repository.insertUser(user)
                val effectiveUser = user.copy(id = if (user.id == 0L) userId else user.id)
                viewModelScope.launch { syncManager?.pushUser(effectiveUser) }

                // Clean up any stale duplicate users with the same mobile or assigned to this site
                for (u in existingUsers) {
                    if (u.id != effectiveUser.id && (u.assignedSiteId == effectiveSiteId || u.mobile.trim() == inchargeMobile.trim())) {
                        repository.deleteUser(u.id)
                        viewModelScope.launch { syncManager?.deleteUser(u.id) }
                    }
                }
            }

            logAudit(
                action = if (id > 0) "UPDATE_SITE" else "CREATE_SITE",
                details = "${if (id > 0) "Updated" else "Created"} project '$name' (Incharge: $inchargeName, Mobile: $inchargeMobile)",
                siteId = effectiveSiteId
            )

            _uiMessage.value = "Site '$name' saved successfully!"
            onSuccess()
        }
    }

    fun deleteSite(siteId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val users = allUsers.value
            for (u in users) {
                if (u.assignedSiteId == siteId) {
                    repository.deleteUser(u.id)
                    viewModelScope.launch { syncManager?.deleteUser(u.id) }
                }
            }
            repository.deleteSite(siteId)
            viewModelScope.launch { syncManager?.deleteSite(siteId) }
            _uiMessage.value = "Project / Site and assigned incharge deleted successfully!"
            onSuccess()
        }
    }

    fun deleteUser(userId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val user = repository.getUserById(userId)
            if (user != null && user.assignedSiteId != null) {
                repository.deleteSite(user.assignedSiteId!!)
                viewModelScope.launch { syncManager?.deleteSite(user.assignedSiteId!!) }
            }
            val sites = allSites.value
            for (s in sites) {
                if (s.mobile == user?.mobile || s.inchargeName == user?.name) {
                    repository.deleteSite(s.id)
                    viewModelScope.launch { syncManager?.deleteSite(s.id) }
                }
            }
            repository.deleteUser(userId)
            viewModelScope.launch { syncManager?.deleteUser(userId) }
            _uiMessage.value = "User & assigned site deleted successfully!"
            onSuccess()
        }
    }

    fun updateInchargeProfile(
        userId: Long,
        siteId: Long?,
        name: String,
        mobile: String,
        password: String,
        designation: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiMessage.value = "Please enter incharge name"
                return@launch
            }
            if (mobile.isBlank()) {
                _uiMessage.value = "Please enter mobile number"
                return@launch
            }
            if (password.isBlank()) {
                _uiMessage.value = "Please enter password"
                return@launch
            }

            val trimmedName = name.trim()
            val trimmedMobile = mobile.trim()
            val trimmedPassword = password.trim()
            val trimmedDesignation = designation.trim().ifBlank { "Site Manager" }

            val allExistingUsers = repository.getAllUsersList()
            val existingUser = (if (userId > 0) repository.getUserById(userId) else null)
                ?: allExistingUsers.firstOrNull { it.id == userId || it.mobile.trim() == trimmedMobile || (siteId != null && it.assignedSiteId == siteId) }
                ?: (if (_currentUser.value?.id == userId) _currentUser.value else null)

            val effectiveSiteId = siteId ?: existingUser?.assignedSiteId
            val sites = repository.getAllSitesList()
            val assignedSite = if (effectiveSiteId != null) sites.firstOrNull { it.id == effectiveSiteId } else null
            val effectiveSiteName = assignedSite?.name ?: existingUser?.assignedSiteName

            val updatedUser = User(
                id = existingUser?.id ?: (if (userId > 0) userId else 0L),
                name = trimmedName,
                mobile = trimmedMobile,
                password = trimmedPassword,
                role = existingUser?.role ?: "SITE_INCHARGE",
                assignedSiteId = effectiveSiteId,
                assignedSiteName = effectiveSiteName,
                designation = trimmedDesignation
            )

            val savedUserId = repository.insertUser(updatedUser)
            val finalUser = updatedUser.copy(id = if (updatedUser.id == 0L) savedUserId else updatedUser.id)

            // Update currentUser state if applicable so incharge screen reflects new info immediately
            if (_currentUser.value?.id == finalUser.id || _currentUser.value?.mobile?.trim() == existingUser?.mobile?.trim()) {
                _currentUser.value = finalUser
            }

            // Sync with assigned Site record
            if (assignedSite != null) {
                val updatedSite = assignedSite.copy(
                    inchargeName = trimmedName,
                    mobile = trimmedMobile,
                    password = trimmedPassword
                )
                repository.insertSite(updatedSite)
                viewModelScope.launch { syncManager?.pushSite(updatedSite) }
            } else if (effectiveSiteId != null) {
                val siteFromDb = repository.getSiteById(effectiveSiteId)
                if (siteFromDb != null) {
                    val updatedSite = siteFromDb.copy(
                        inchargeName = trimmedName,
                        mobile = trimmedMobile,
                        password = trimmedPassword
                    )
                    repository.insertSite(updatedSite)
                    viewModelScope.launch { syncManager?.pushSite(updatedSite) }
                }
            }

            // Clean up stale duplicate user records
            for (u in allExistingUsers) {
                if (u.id != finalUser.id && ((effectiveSiteId != null && u.assignedSiteId == effectiveSiteId) || u.mobile.trim() == trimmedMobile || (existingUser != null && u.mobile.trim() == existingUser.mobile.trim()))) {
                    repository.deleteUser(u.id)
                    viewModelScope.launch { syncManager?.deleteUser(u.id) }
                }
            }

            // Push updated user to cloud sync
            viewModelScope.launch { syncManager?.pushUser(finalUser) }

            logAudit(
                action = "UPDATE_INCHARGE_PROFILE",
                details = "Updated profile for Incharge '$trimmedName' (Mobile: $trimmedMobile, Designation: $trimmedDesignation)",
                siteId = effectiveSiteId ?: 0L
            )

            _uiMessage.value = "Incharge profile updated successfully!"
            onSuccess()
        }
    }

    fun payStaffSalary(
        staffId: Long,
        staffName: String,
        siteId: Long,
        siteName: String,
        dateMillis: Long,
        dateFormatted: String,
        amount: Double,
        remarks: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val record = StaffAttendance(
                staffId = staffId,
                staffName = staffName,
                siteId = siteId,
                siteName = siteName,
                dateMillis = dateMillis,
                dateFormatted = dateFormatted,
                status = "SALARY",
                wageAmount = amount,
                remarks = remarks.ifBlank { "Salary Payout" }
            )
            val attId = repository.insertAttendance(record)
            viewModelScope.launch { syncManager?.pushAttendance(record.copy(id = attId)) }

            val u = _currentUser.value
            // Auto-connect to Site Expense Accounts
            val tx = TransactionEntry(
                dateMillis = dateMillis,
                dateFormatted = dateFormatted,
                type = "EXPENSE",
                siteId = siteId,
                siteName = siteName,
                userId = u?.id ?: 0L,
                userName = u?.name ?: "Admin",
                userMobile = u?.mobile ?: "",
                category = "Labour & Staff",
                subCategory = "Salary Payment",
                partyName = staffName,
                description = "Salary Payout: $remarks",
                amount = amount,
                paymentMode = "Cash"
            )
            val txId = repository.insertTransaction(tx)
            viewModelScope.launch { syncManager?.pushTransaction(tx.copy(id = txId)) }
            logAudit("SALARY_PAYOUT", "Paid salary of ₹$amount to $staffName at $siteName", siteId)

            _uiMessage.value = "Salary of ₹$amount recorded & added to site expense!"
            onSuccess()
        }
    }

    fun saveSalaryRecord(
        record: SalaryRecord,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val salId = repository.insertSalaryRecord(record)
            val effectiveSal = if (record.id == 0L) record.copy(id = salId) else record
            viewModelScope.launch { syncManager?.pushSalaryRecord(effectiveSal) }

            if (record.paymentStatus == "PAID" || record.paymentStatus == "APPROVED") {
                val u = _currentUser.value
                val tx = TransactionEntry(
                    dateMillis = System.currentTimeMillis(),
                    dateFormatted = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                    type = "EXPENSE",
                    siteId = record.siteId,
                    siteName = record.siteName,
                    userId = u?.id ?: 0L,
                    userName = u?.name ?: "Admin",
                    userMobile = u?.mobile ?: "",
                    category = "Labour & Staff",
                    subCategory = "Monthly Salary",
                    partyName = record.staffName,
                    description = "Monthly Salary (${record.monthYear}) - Net: ₹${record.netSalary}",
                    amount = record.netSalary,
                    paymentMode = record.paymentMode
                )
                val txId = repository.insertTransaction(tx)
                viewModelScope.launch { syncManager?.pushTransaction(tx.copy(id = txId)) }
            }
            logAudit("SAVE_SALARY", "Saved salary record for ${record.staffName} (${record.monthYear}) - Net: ₹${record.netSalary}", record.siteId)
            _uiMessage.value = "Salary record saved successfully for ${record.staffName}!"
            onSuccess()
        }
    }

    fun deleteSalaryRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteSalaryRecord(id)
            viewModelScope.launch { syncManager?.deleteSalaryRecord(id) }
            logAudit("DELETE_SALARY", "Deleted salary record ID $id", 0)
            _uiMessage.value = "Salary record deleted."
        }
    }

    fun logAudit(action: String, details: String, siteId: Long) {
        viewModelScope.launch {
            val userName = _currentUser.value?.name ?: "System"
            val log = AuditLog(userName = userName, action = action, details = details, siteId = siteId)
            val logId = repository.insertAuditLog(log)
            viewModelScope.launch { syncManager?.pushAuditLog(log.copy(id = logId)) }
        }
    }

    fun generateDaybookPdfHtml(txList: List<TransactionEntry>, fromDate: String, toDate: String): String {
        val totalIncome = txList.filter { it.type.uppercase() == "INCOME" }.sumOf { it.amount }
        val totalExpense = txList.filter { it.type.uppercase() == "EXPENSE" }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense
        val generatedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n")
        sb.append("<meta charset=\"UTF-8\">\n")
        sb.append("<title>Daybook Summary Report</title>\n")
        sb.append("<style>\n")
        sb.append("body { font-family: Helvetica, Arial, sans-serif; color: #333; margin: 20px; }\n")
        sb.append("h1 { color: #1976D2; font-size: 22px; margin-bottom: 5px; }\n")
        sb.append(".subtitle { font-size: 13px; color: #666; margin-bottom: 20px; }\n")
        sb.append("table { width: 100%; border-collapse: collapse; margin-top: 15px; }\n")
        sb.append("th, td { border: 1px solid #ccc; padding: 8px; text-align: left; font-size: 11px; }\n")
        sb.append("th { background-color: #1976D2; color: white; }\n")
        sb.append(".income { color: #2E7D32; font-weight: bold; }\n")
        sb.append(".expense { color: #C62828; font-weight: bold; }\n")
        sb.append("</style>\n</head>\n<body>\n")
        sb.append("<h1>Construction Project - Daybook Summary Report</h1>\n")
        sb.append("<div class=\"subtitle\">Period: <b>$fromDate</b> to <b>$toDate</b> | Generated: <b>$generatedDate</b></div>\n")
        
        sb.append("<div style=\"display:flex; gap:10px; margin-bottom:20px;\">\n")
        sb.append("<div style=\"flex:1; background:#E8F5E9; padding:10px; border-radius:6px; border-left:4px solid #2E7D32;\"><b>Total Income:</b> <span class=\"income\">₹$totalIncome</span></div>\n")
        sb.append("<div style=\"flex:1; background:#FFEBEE; padding:10px; border-radius:6px; border-left:4px solid #C62828;\"><b>Total Expense:</b> <span class=\"expense\">₹$totalExpense</span></div>\n")
        sb.append("<div style=\"flex:1; background:#E3F2FD; padding:10px; border-radius:6px; border-left:4px solid #1565C0;\"><b>Net Balance:</b> <span style=\"color:#1565C0; font-weight:bold;\">₹$netBalance</span></div>\n")
        sb.append("</div>\n")

        sb.append("<h3>Transaction Entries (${txList.size})</h3>\n")
        sb.append("<table>\n")
        sb.append("<tr><th>Date</th><th>Type</th><th>Site</th><th>Category</th><th>Party / Description</th><th>Amount (₹)</th><th>Mode</th></tr>\n")
        for (t in txList) {
            val typeClass = if (t.type.uppercase() == "INCOME") "income" else "expense"
            sb.append("<tr>")
            sb.append("<td>${t.dateFormatted}</td>")
            sb.append("<td class=\"$typeClass\">${t.type}</td>")
            sb.append("<td>${t.siteName}</td>")
            sb.append("<td>${t.category} / ${t.subCategory}</td>")
            sb.append("<td><b>${t.partyName}</b><br><span style=\"color:#666;\">${t.description}</span></td>")
            sb.append("<td class=\"$typeClass\">₹${t.amount}</td>")
            sb.append("<td>${t.paymentMode}</td>")
            sb.append("</tr>\n")
        }
        sb.append("</table>\n")
        sb.append("</body>\n</html>")
        return sb.toString()
    }
}

