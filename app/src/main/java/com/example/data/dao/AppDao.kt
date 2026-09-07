package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SalaryRecord
import com.example.data.model.AuditLog
import com.example.data.model.Site
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.TransactionEntry
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Users ---
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE mobile = :mobile LIMIT 1")
    suspend fun getUserByMobile(mobile: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    // --- Sites ---
    @Query("SELECT * FROM sites ORDER BY id ASC")
    fun getAllSites(): Flow<List<Site>>

    @Query("SELECT * FROM sites WHERE id = :id LIMIT 1")
    suspend fun getSiteById(id: Long): Site?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: Site): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<Site>)

    @Query("DELETE FROM sites WHERE id = :id")
    suspend fun deleteSiteById(id: Long)

    // --- Transactions ---
    // All transactions (For Admin)
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntry>>

    // Transactions filtered by site (For Site Incharge strictly)
    @Query("SELECT * FROM transactions WHERE siteId = :siteId ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsBySite(siteId: Long): Flow<List<TransactionEntry>>

    // Transactions by User
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsByUser(userId: Long): Flow<List<TransactionEntry>>

    // Transactions by Site and Type
    @Query("SELECT * FROM transactions WHERE siteId = :siteId AND type = :type ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsBySiteAndType(siteId: Long, type: String): Flow<List<TransactionEntry>>

    // Insert Entry (Site Incharge action)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(entry: TransactionEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(entries: List<TransactionEntry>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    // --- Staff Members ---
    @Query("SELECT * FROM staff_members ORDER BY id DESC")
    fun getAllStaff(): Flow<List<StaffMember>>

    @Query("SELECT * FROM staff_members WHERE siteId = :siteId ORDER BY id DESC")
    fun getStaffBySite(siteId: Long): Flow<List<StaffMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffList(list: List<StaffMember>)

    @Query("DELETE FROM staff_members WHERE id = :id")
    suspend fun deleteStaff(id: Long)

    // --- Staff Attendance ---
    @Query("SELECT * FROM staff_attendance ORDER BY dateMillis DESC, id DESC")
    fun getAllAttendance(): Flow<List<StaffAttendance>>

    @Query("SELECT * FROM staff_attendance WHERE siteId = :siteId ORDER BY dateMillis DESC, id DESC")
    fun getAttendanceBySite(siteId: Long): Flow<List<StaffAttendance>>

    @Query("SELECT * FROM staff_attendance WHERE siteId = :siteId AND dateFormatted = :dateFormatted ORDER BY id DESC")
    fun getAttendanceBySiteAndDate(siteId: Long, dateFormatted: String): Flow<List<StaffAttendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: StaffAttendance): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(list: List<StaffAttendance>)

    @Query("DELETE FROM staff_attendance WHERE id = :id")
    suspend fun deleteAttendance(id: Long)

    // --- Salary Records ---
    @Query("SELECT * FROM salary_records ORDER BY id DESC")
    fun getAllSalaryRecords(): Flow<List<SalaryRecord>>

    @Query("SELECT * FROM salary_records WHERE siteId = :siteId ORDER BY id DESC")
    fun getSalaryRecordsBySite(siteId: Long): Flow<List<SalaryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryRecord(record: SalaryRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryRecords(records: List<SalaryRecord>)

    @Query("DELETE FROM salary_records WHERE id = :id")
    suspend fun deleteSalaryRecord(id: Long)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLog>)

    // --- Direct List Access for Sync & Export ---
    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<User>

    @Query("SELECT * FROM sites")
    suspend fun getAllSitesList(): List<Site>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsList(): List<TransactionEntry>

    @Query("SELECT * FROM staff_members")
    suspend fun getAllStaffList(): List<StaffMember>

    @Query("SELECT * FROM staff_attendance")
    suspend fun getAllAttendanceList(): List<StaffAttendance>

    @Query("SELECT * FROM salary_records")
    suspend fun getAllSalaryRecordsList(): List<SalaryRecord>

    @Query("SELECT * FROM audit_logs")
    suspend fun getAllAuditLogsList(): List<AuditLog>

    // --- Bulk Clear Functions for Production Reset ---
    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM staff_members")
    suspend fun clearAllStaff()

    @Query("DELETE FROM staff_attendance")
    suspend fun clearAllAttendance()

    @Query("DELETE FROM salary_records")
    suspend fun clearAllSalaryRecords()
}
