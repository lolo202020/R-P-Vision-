package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.AuditLog
import com.example.data.model.SalaryRecord
import com.example.data.model.Site
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.TransactionEntry
import com.example.data.model.User
import com.example.data.repository.ConstructionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SyncSummary(
    val transactionsPulled: Int = 0,
    val transactionsPushed: Int = 0,
    val sitesPulled: Int = 0,
    val staffPulled: Int = 0,
    val attendancePulled: Int = 0,
    val salaryPulled: Int = 0,
    val usersPulled: Int = 0,
    val logsPulled: Int = 0,
    val message: String = ""
)

class SupabaseSyncManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("supabase_sync_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun getSupabaseUrl(): String = prefs.getString("supabase_url", "") ?: ""
    fun getSupabaseKey(): String = prefs.getString("supabase_key", "") ?: ""
    fun isAutoSyncEnabled(): Boolean = prefs.getBoolean("auto_sync_enabled", true)
    fun getLastSyncTime(): Long = prefs.getLong("last_sync_time", 0L)

    fun saveConfig(url: String, key: String, autoSync: Boolean = true) {
        var cleanUrl = url.trim()
        if (cleanUrl.endsWith("/")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - 1)
        }
        prefs.edit()
            .putString("supabase_url", cleanUrl)
            .putString("supabase_key", key.trim())
            .putBoolean("auto_sync_enabled", autoSync)
            .apply()
    }

    fun isConfigured(): Boolean {
        return getSupabaseUrl().isNotBlank() && getSupabaseKey().isNotBlank()
    }

    private fun markSyncSuccess() {
        prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
    }

    private fun getBaseUrl(): String = getSupabaseUrl()
    private fun getKey(): String = getSupabaseKey()

    // -------------------------------------------------------------
    // Test Connection
    // -------------------------------------------------------------
    suspend fun testConnection(testUrl: String? = null, testKey: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            val url = (testUrl ?: getBaseUrl()).trim().removeSuffix("/")
            val key = (testKey ?: getKey()).trim()

            if (url.isBlank() || key.isBlank()) {
                return@withContext Result.failure(Exception("Supabase URL and API Key are required."))
            }

            try {
                val request = Request.Builder()
                    .url("$url/rest/v1/users?select=count")
                    .addHeader("apikey", key)
                    .addHeader("Authorization", "Bearer $key")
                    .addHeader("Range", "0-0")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful || response.code == 200 || response.code == 206) {
                    Result.success("Connection Successful! Connected to Supabase cloud.")
                } else {
                    val body = response.body?.string() ?: ""
                    Result.failure(Exception("Connection failed (HTTP ${response.code}): $body"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseSync", "Test connection error: ${e.message}", e)
                Result.failure(Exception("Network error: ${e.localizedMessage ?: "Could not connect to Supabase"}"))
            }
        }

    // -------------------------------------------------------------
    // HTTP Helpers
    // -------------------------------------------------------------
    private fun executeGet(table: String): JSONArray? {
        val url = getBaseUrl()
        val key = getKey()
        if (url.isBlank() || key.isBlank()) return null

        return try {
            val request = Request.Builder()
                .url("$url/rest/v1/$table?select=*")
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val str = response.body?.string() ?: "[]"
                JSONArray(str)
            } else {
                Log.w("SupabaseSync", "GET $table returned HTTP ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "GET $table error: ${e.message}")
            null
        }
    }

    private fun executeUpsert(table: String, jsonArray: JSONArray): Boolean {
        val url = getBaseUrl()
        val key = getKey()
        if (url.isBlank() || key.isBlank() || jsonArray.length() == 0) return false

        return try {
            val requestBody = jsonArray.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$url/rest/v1/$table")
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("SupabaseSync", "UPSERT $table error: ${e.message}")
            false
        }
    }

    private fun executeDelete(table: String, id: Long): Boolean {
        val url = getBaseUrl()
        val key = getKey()
        if (url.isBlank() || key.isBlank()) return false

        return try {
            val request = Request.Builder()
                .url("$url/rest/v1/$table?id=eq.$id")
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .delete()
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("SupabaseSync", "DELETE $table/$id error: ${e.message}")
            false
        }
    }

    // -------------------------------------------------------------
    // Full Two-Way Sync (Pull + Push)
    // -------------------------------------------------------------
    suspend fun syncAll(repository: ConstructionRepository): Result<SyncSummary> =
        withContext(Dispatchers.IO) {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Supabase credentials not configured."))
            }

            try {
                // 1. Sync Sites
                val remoteSitesJson = executeGet("sites")
                var sitesPulled = 0
                if (remoteSitesJson != null) {
                    val remoteSites = mutableListOf<Site>()
                    for (i in 0 until remoteSitesJson.length()) {
                        val obj = remoteSitesJson.getJSONObject(i)
                        remoteSites.add(
                            Site(
                                id = obj.optLong("id"),
                                name = obj.optString("name"),
                                code = obj.optString("code"),
                                location = obj.optString("location"),
                                clientName = obj.optString("client_name", obj.optString("clientName")),
                                budget = obj.optDouble("budget", 0.0),
                                inchargeName = obj.optString("incharge_name", obj.optString("inchargeName")),
                                mobile = obj.optString("mobile", ""),
                                password = obj.optString("password", "123456")
                            )
                        )
                    }
                    if (remoteSites.isNotEmpty()) {
                        repository.insertSites(remoteSites)
                        sitesPulled = remoteSites.size
                    }
                }
                // Push local sites if cloud is empty or missing
                val localSites = repository.getAllSitesList()
                if (localSites.isNotEmpty()) {
                    val arr = JSONArray()
                    for (s in localSites) {
                        arr.put(siteToJson(s))
                    }
                    executeUpsert("sites", arr)
                }

                // 2. Sync Users
                val remoteUsersJson = executeGet("users")
                var usersPulled = 0
                if (remoteUsersJson != null) {
                    val remoteUsers = mutableListOf<User>()
                    for (i in 0 until remoteUsersJson.length()) {
                        val obj = remoteUsersJson.getJSONObject(i)
                        remoteUsers.add(
                            User(
                                id = obj.optLong("id"),
                                name = obj.optString("name"),
                                mobile = obj.optString("mobile"),
                                password = obj.optString("password", "123456"),
                                role = obj.optString("role", "SITE_INCHARGE"),
                                assignedSiteId = if (obj.has("assigned_site_id") && !obj.isNull("assigned_site_id")) obj.optLong("assigned_site_id") else null,
                                assignedSiteName = if (obj.has("assigned_site_name") && !obj.isNull("assigned_site_name")) obj.optString("assigned_site_name") else null,
                                designation = obj.optString("designation", "Site Incharge")
                            )
                        )
                    }
                    if (remoteUsers.isNotEmpty()) {
                        for (u in remoteUsers) repository.insertUser(u)
                        usersPulled = remoteUsers.size
                    }
                }
                val localUsers = repository.getAllUsersList()
                if (localUsers.isNotEmpty()) {
                    val arr = JSONArray()
                    for (u in localUsers) {
                        arr.put(userToJson(u))
                    }
                    executeUpsert("users", arr)
                }

                // 3. Sync Transactions
                val remoteTxJson = executeGet("transactions")
                var txPulled = 0
                if (remoteTxJson != null) {
                    val remoteTx = mutableListOf<TransactionEntry>()
                    for (i in 0 until remoteTxJson.length()) {
                        val obj = remoteTxJson.getJSONObject(i)
                        remoteTx.add(
                            TransactionEntry(
                                id = obj.optLong("id"),
                                type = obj.optString("type"),
                                dateMillis = obj.optLong("date_millis", obj.optLong("dateMillis", System.currentTimeMillis())),
                                dateFormatted = obj.optString("date_formatted", obj.optString("dateFormatted")),
                                siteId = obj.optLong("site_id", obj.optLong("siteId")),
                                siteName = obj.optString("site_name", obj.optString("siteName")),
                                userId = obj.optLong("user_id", obj.optLong("userId")),
                                userName = obj.optString("user_name", obj.optString("userName")),
                                userMobile = obj.optString("user_mobile", obj.optString("userMobile")),
                                category = obj.optString("category"),
                                subCategory = obj.optString("sub_category", obj.optString("subCategory")),
                                partyName = obj.optString("party_name", obj.optString("partyName")),
                                description = obj.optString("description"),
                                amount = obj.optDouble("amount", 0.0),
                                paymentMode = obj.optString("payment_mode", obj.optString("paymentMode", "Cash")),
                                receiptPhotoUri = if (obj.has("receipt_photo_uri") && !obj.isNull("receipt_photo_uri")) obj.optString("receipt_photo_uri") else null,
                                remarks = if (obj.has("remarks") && !obj.isNull("remarks")) obj.optString("remarks") else null,
                                createdAt = obj.optLong("created_at_millis", System.currentTimeMillis())
                            )
                        )
                    }
                    if (remoteTx.isNotEmpty()) {
                        repository.insertTransactions(remoteTx)
                        txPulled = remoteTx.size
                    }
                }
                val localTx = repository.getAllTransactionsList()
                var txPushed = 0
                if (localTx.isNotEmpty()) {
                    val arr = JSONArray()
                    for (t in localTx) {
                        arr.put(transactionToJson(t))
                    }
                    if (executeUpsert("transactions", arr)) {
                        txPushed = localTx.size
                    }
                }

                // 4. Sync Staff Members
                val remoteStaffJson = executeGet("staff_members")
                var staffPulled = 0
                if (remoteStaffJson != null) {
                    val remoteStaff = mutableListOf<StaffMember>()
                    for (i in 0 until remoteStaffJson.length()) {
                        val obj = remoteStaffJson.getJSONObject(i)
                        remoteStaff.add(
                            StaffMember(
                                id = obj.optLong("id"),
                                name = obj.optString("name"),
                                mobile = obj.optString("mobile"),
                                designation = obj.optString("designation"),
                                siteId = obj.optLong("site_id", obj.optLong("siteId")),
                                siteName = obj.optString("site_name", obj.optString("siteName")),
                                dailyWage = obj.optDouble("daily_wage", obj.optDouble("dailyWage", 0.0)),
                                paymentType = obj.optString("payment_type", obj.optString("paymentType", "DAILY"))
                            )
                        )
                    }
                    if (remoteStaff.isNotEmpty()) {
                        repository.insertStaffList(remoteStaff)
                        staffPulled = remoteStaff.size
                    }
                }
                val localStaff = repository.getAllStaffList()
                if (localStaff.isNotEmpty()) {
                    val arr = JSONArray()
                    for (s in localStaff) {
                        arr.put(staffToJson(s))
                    }
                    executeUpsert("staff_members", arr)
                }

                // 5. Sync Attendance
                val remoteAttJson = executeGet("staff_attendance")
                var attPulled = 0
                if (remoteAttJson != null) {
                    val remoteAtt = mutableListOf<StaffAttendance>()
                    for (i in 0 until remoteAttJson.length()) {
                        val obj = remoteAttJson.getJSONObject(i)
                        remoteAtt.add(
                            StaffAttendance(
                                id = obj.optLong("id"),
                                staffId = obj.optLong("staff_id", obj.optLong("staffId")),
                                staffName = obj.optString("staff_name", obj.optString("staffName")),
                                siteId = obj.optLong("site_id", obj.optLong("siteId")),
                                siteName = obj.optString("site_name", obj.optString("siteName")),
                                dateMillis = obj.optLong("date_millis", obj.optLong("dateMillis")),
                                dateFormatted = obj.optString("date_formatted", obj.optString("dateFormatted")),
                                status = obj.optString("status"),
                                wageAmount = obj.optDouble("wage_amount", obj.optDouble("wageAmount", 0.0)),
                                overtimeHours = obj.optDouble("overtime_hours", obj.optDouble("overtimeHours", 0.0)),
                                checkInTime = if (obj.has("check_in_time") && !obj.isNull("check_in_time")) obj.optString("check_in_time") else null,
                                checkOutTime = if (obj.has("check_out_time") && !obj.isNull("check_out_time")) obj.optString("check_out_time") else null,
                                remarks = if (obj.has("remarks") && !obj.isNull("remarks")) obj.optString("remarks") else null
                            )
                        )
                    }
                    if (remoteAtt.isNotEmpty()) {
                        repository.insertAttendanceList(remoteAtt)
                        attPulled = remoteAtt.size
                    }
                }
                val localAtt = repository.getAllAttendanceList()
                if (localAtt.isNotEmpty()) {
                    val arr = JSONArray()
                    for (a in localAtt) {
                        arr.put(attendanceToJson(a))
                    }
                    executeUpsert("staff_attendance", arr)
                }

                // 6. Sync Salary Records
                val remoteSalaryJson = executeGet("salary_records")
                var salaryPulled = 0
                if (remoteSalaryJson != null) {
                    val remoteSalary = mutableListOf<SalaryRecord>()
                    for (i in 0 until remoteSalaryJson.length()) {
                        val obj = remoteSalaryJson.getJSONObject(i)
                        remoteSalary.add(
                            SalaryRecord(
                                id = obj.optLong("id"),
                                staffId = obj.optLong("staff_id", obj.optLong("staffId")),
                                staffName = obj.optString("staff_name", obj.optString("staffName")),
                                siteId = obj.optLong("site_id", obj.optLong("siteId")),
                                siteName = obj.optString("site_name", obj.optString("siteName")),
                                monthYear = obj.optString("month_year", obj.optString("monthYear")),
                                basicSalary = obj.optDouble("basic_salary", obj.optDouble("basicSalary", 0.0)),
                                presentDays = obj.optInt("present_days", obj.optInt("presentDays", 0)),
                                absentDays = obj.optInt("absent_days", obj.optInt("absentDays", 0)),
                                halfDays = obj.optInt("half_days", obj.optInt("halfDays", 0)),
                                leaveDays = obj.optInt("leave_days", obj.optInt("leaveDays", 0)),
                                overtimeHours = obj.optDouble("overtime_hours", obj.optDouble("overtimeHours", 0.0)),
                                overtimeAmount = obj.optDouble("overtime_amount", obj.optDouble("overtimeAmount", 0.0)),
                                bonus = obj.optDouble("bonus", 0.0),
                                advance = obj.optDouble("advance", 0.0),
                                deduction = obj.optDouble("deduction", 0.0),
                                netSalary = obj.optDouble("net_salary", obj.optDouble("netSalary", 0.0)),
                                paymentStatus = obj.optString("payment_status", obj.optString("paymentStatus", "PENDING")),
                                paymentMode = obj.optString("payment_mode", obj.optString("paymentMode", "Cash")),
                                paidDate = if (obj.has("paid_date") && !obj.isNull("paid_date")) obj.optString("paid_date") else null,
                                remarks = if (obj.has("remarks") && !obj.isNull("remarks")) obj.optString("remarks") else null
                            )
                        )
                    }
                    if (remoteSalary.isNotEmpty()) {
                        repository.insertSalaryRecords(remoteSalary)
                        salaryPulled = remoteSalary.size
                    }
                }
                val localSalary = repository.getAllSalaryRecordsList()
                if (localSalary.isNotEmpty()) {
                    val arr = JSONArray()
                    for (sal in localSalary) {
                        arr.put(salaryToJson(sal))
                    }
                    executeUpsert("salary_records", arr)
                }

                // 7. Sync Audit Logs
                val remoteLogsJson = executeGet("audit_logs")
                var logsPulled = 0
                if (remoteLogsJson != null) {
                    val remoteLogs = mutableListOf<AuditLog>()
                    for (i in 0 until remoteLogsJson.length()) {
                        val obj = remoteLogsJson.getJSONObject(i)
                        remoteLogs.add(
                            AuditLog(
                                id = obj.optLong("id"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                userName = obj.optString("user_name", obj.optString("userName", "System")),
                                action = obj.optString("action"),
                                details = obj.optString("details"),
                                siteId = obj.optLong("site_id", obj.optLong("siteId", 0L))
                            )
                        )
                    }
                    if (remoteLogs.isNotEmpty()) {
                        repository.insertAuditLogs(remoteLogs)
                        logsPulled = remoteLogs.size
                    }
                }

                markSyncSuccess()

                Result.success(
                    SyncSummary(
                        transactionsPulled = txPulled,
                        transactionsPushed = txPushed,
                        sitesPulled = sitesPulled,
                        staffPulled = staffPulled,
                        attendancePulled = attPulled,
                        salaryPulled = salaryPulled,
                        usersPulled = usersPulled,
                        logsPulled = logsPulled,
                        message = "Sync complete! Data updated across all devices."
                    )
                )
            } catch (e: Exception) {
                Log.e("SupabaseSync", "syncAll error: ${e.message}", e)
                Result.failure(Exception("Sync error: ${e.localizedMessage ?: "Network or database timeout"}"))
            }
        }

    // -------------------------------------------------------------
    // Granular Push & Delete Methods (Called on every change)
    // -------------------------------------------------------------
    suspend fun pushTransaction(entry: TransactionEntry) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val arr = JSONArray().put(transactionToJson(entry))
        executeUpsert("transactions", arr)
    }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        executeDelete("transactions", id)
    }

    suspend fun pushSite(site: Site) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val arr = JSONArray().put(siteToJson(site))
        executeUpsert("sites", arr)
    }

    suspend fun deleteSite(id: Long) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        executeDelete("sites", id)
    }

    suspend fun pushStaff(staff: StaffMember) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val arr = JSONArray().put(staffToJson(staff))
        executeUpsert("staff_members", arr)
    }

    suspend fun deleteStaff(id: Long) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        executeDelete("staff_members", id)
    }

    suspend fun pushAttendance(att: StaffAttendance) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val arr = JSONArray().put(attendanceToJson(att))
        executeUpsert("staff_attendance", arr)
    }

    suspend fun deleteAttendance(id: Long) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        executeDelete("staff_attendance", id)
    }

    suspend fun pushSalaryRecord(rec: SalaryRecord) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val arr = JSONArray().put(salaryToJson(rec))
        executeUpsert("salary_records", arr)
    }

    suspend fun deleteSalaryRecord(id: Long) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        executeDelete("salary_records", id)
    }

    suspend fun pushUser(user: User) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val arr = JSONArray().put(userToJson(user))
        executeUpsert("users", arr)
    }

    suspend fun deleteUser(id: Long) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        executeDelete("users", id)
    }

    suspend fun pushAuditLog(log: AuditLog) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val arr = JSONArray().put(auditLogToJson(log))
        executeUpsert("audit_logs", arr)
    }

    // -------------------------------------------------------------
    // JSON Serializers
    // -------------------------------------------------------------
    private fun siteToJson(site: Site): JSONObject {
        val obj = JSONObject()
        if (site.id > 0) obj.put("id", site.id)
        obj.put("name", site.name)
        obj.put("code", site.code)
        obj.put("location", site.location)
        obj.put("client_name", site.clientName)
        obj.put("budget", site.budget)
        obj.put("incharge_name", site.inchargeName)
        obj.put("mobile", site.mobile)
        obj.put("password", site.password)
        return obj
    }

    private fun userToJson(user: User): JSONObject {
        val obj = JSONObject()
        if (user.id > 0) obj.put("id", user.id)
        obj.put("name", user.name)
        obj.put("mobile", user.mobile)
        obj.put("password", user.password)
        obj.put("role", user.role)
        if (user.assignedSiteId != null) obj.put("assigned_site_id", user.assignedSiteId)
        if (user.assignedSiteName != null) obj.put("assigned_site_name", user.assignedSiteName)
        obj.put("designation", user.designation)
        return obj
    }

    private fun transactionToJson(tx: TransactionEntry): JSONObject {
        val obj = JSONObject()
        if (tx.id > 0) obj.put("id", tx.id)
        obj.put("type", tx.type)
        obj.put("date_millis", tx.dateMillis)
        obj.put("date_formatted", tx.dateFormatted)
        obj.put("site_id", tx.siteId)
        obj.put("site_name", tx.siteName)
        obj.put("user_id", tx.userId)
        obj.put("user_name", tx.userName)
        obj.put("user_mobile", tx.userMobile)
        obj.put("category", tx.category)
        obj.put("sub_category", tx.subCategory)
        obj.put("party_name", tx.partyName)
        obj.put("description", tx.description)
        obj.put("amount", tx.amount)
        obj.put("payment_mode", tx.paymentMode)
        if (tx.receiptPhotoUri != null) obj.put("receipt_photo_uri", tx.receiptPhotoUri)
        if (tx.remarks != null) obj.put("remarks", tx.remarks)
        return obj
    }

    private fun staffToJson(s: StaffMember): JSONObject {
        val obj = JSONObject()
        if (s.id > 0) obj.put("id", s.id)
        obj.put("name", s.name)
        obj.put("mobile", s.mobile)
        obj.put("designation", s.designation)
        obj.put("site_id", s.siteId)
        obj.put("site_name", s.siteName)
        obj.put("daily_wage", s.dailyWage)
        obj.put("payment_type", s.paymentType)
        return obj
    }

    private fun attendanceToJson(a: StaffAttendance): JSONObject {
        val obj = JSONObject()
        if (a.id > 0) obj.put("id", a.id)
        obj.put("staff_id", a.staffId)
        obj.put("staff_name", a.staffName)
        obj.put("site_id", a.siteId)
        obj.put("site_name", a.siteName)
        obj.put("date_millis", a.dateMillis)
        obj.put("date_formatted", a.dateFormatted)
        obj.put("status", a.status)
        obj.put("wage_amount", a.wageAmount)
        obj.put("overtime_hours", a.overtimeHours)
        if (a.checkInTime != null) obj.put("check_in_time", a.checkInTime)
        if (a.checkOutTime != null) obj.put("check_out_time", a.checkOutTime)
        if (a.remarks != null) obj.put("remarks", a.remarks)
        return obj
    }

    private fun salaryToJson(sal: SalaryRecord): JSONObject {
        val obj = JSONObject()
        if (sal.id > 0) obj.put("id", sal.id)
        obj.put("staff_id", sal.staffId)
        obj.put("staff_name", sal.staffName)
        obj.put("site_id", sal.siteId)
        obj.put("site_name", sal.siteName)
        obj.put("month_year", sal.monthYear)
        obj.put("basic_salary", sal.basicSalary)
        obj.put("present_days", sal.presentDays)
        obj.put("absent_days", sal.absentDays)
        obj.put("half_days", sal.halfDays)
        obj.put("leave_days", sal.leaveDays)
        obj.put("overtime_hours", sal.overtimeHours)
        obj.put("overtime_amount", sal.overtimeAmount)
        obj.put("bonus", sal.bonus)
        obj.put("advance", sal.advance)
        obj.put("deduction", sal.deduction)
        obj.put("net_salary", sal.netSalary)
        obj.put("payment_status", sal.paymentStatus)
        obj.put("payment_mode", sal.paymentMode)
        if (sal.paidDate != null) obj.put("paid_date", sal.paidDate)
        if (sal.remarks != null) obj.put("remarks", sal.remarks)
        return obj
    }

    private fun auditLogToJson(log: AuditLog): JSONObject {
        val obj = JSONObject()
        if (log.id > 0) obj.put("id", log.id)
        obj.put("timestamp", log.timestamp)
        obj.put("user_name", log.userName)
        obj.put("action", log.action)
        obj.put("details", log.details)
        obj.put("site_id", log.siteId)
        return obj
    }

    companion object {
        const val SQL_SETUP_SCRIPT = """-- ================================================================
-- RPVC SITE KHATA - SUPABASE DATABASE TABLES
-- ================================================================

CREATE TABLE IF NOT EXISTS public.users (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    mobile TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL DEFAULT '123456',
    role TEXT NOT NULL CHECK (role IN ('ADMIN', 'SITE_INCHARGE')),
    assigned_site_id BIGINT,
    assigned_site_name TEXT,
    designation TEXT DEFAULT 'Site Incharge',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.sites (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    code TEXT NOT NULL,
    location TEXT NOT NULL,
    client_name TEXT NOT NULL,
    budget NUMERIC(15, 2) DEFAULT 0.00,
    incharge_name TEXT NOT NULL,
    mobile TEXT DEFAULT '',
    password TEXT DEFAULT '123456',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.transactions (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    type TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    date_millis BIGINT NOT NULL,
    date_formatted DATE NOT NULL,
    site_id BIGINT NOT NULL,
    site_name TEXT NOT NULL,
    user_id BIGINT NOT NULL,
    user_name TEXT NOT NULL,
    user_mobile TEXT,
    category TEXT NOT NULL,
    sub_category TEXT,
    party_name TEXT NOT NULL,
    description TEXT,
    amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    payment_mode TEXT NOT NULL DEFAULT 'Cash',
    receipt_photo_uri TEXT,
    remarks TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.staff_members (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    mobile TEXT NOT NULL,
    designation TEXT NOT NULL,
    site_id BIGINT NOT NULL,
    site_name TEXT NOT NULL,
    daily_wage NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    payment_type TEXT NOT NULL DEFAULT 'DAILY',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.staff_attendance (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    staff_name TEXT NOT NULL,
    site_id BIGINT NOT NULL,
    site_name TEXT NOT NULL,
    date_millis BIGINT NOT NULL,
    date_formatted DATE NOT NULL,
    status TEXT NOT NULL,
    wage_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    overtime_hours NUMERIC(5, 2) DEFAULT 0.00,
    check_in_time TEXT,
    check_out_time TEXT,
    remarks TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.salary_records (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    staff_name TEXT NOT NULL,
    site_id BIGINT NOT NULL,
    site_name TEXT NOT NULL,
    month_year TEXT NOT NULL,
    basic_salary NUMERIC(12, 2) DEFAULT 0.00,
    present_days INT DEFAULT 0,
    absent_days INT DEFAULT 0,
    half_days INT DEFAULT 0,
    leave_days INT DEFAULT 0,
    overtime_hours NUMERIC(6, 2) DEFAULT 0.00,
    overtime_amount NUMERIC(12, 2) DEFAULT 0.00,
    bonus NUMERIC(12, 2) DEFAULT 0.00,
    advance NUMERIC(12, 2) DEFAULT 0.00,
    deduction NUMERIC(12, 2) DEFAULT 0.00,
    net_salary NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    payment_status TEXT NOT NULL DEFAULT 'PENDING',
    payment_mode TEXT DEFAULT 'Cash',
    paid_date DATE,
    remarks TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.audit_logs (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_name TEXT NOT NULL,
    action TEXT NOT NULL,
    details TEXT NOT NULL,
    site_id BIGINT DEFAULT 0,
    timestamp BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable full read/write for Anon Key
ALTER TABLE public.users DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.sites DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.staff_members DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.staff_attendance DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.salary_records DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs DISABLE ROW LEVEL SECURITY;
"""
    }
}
