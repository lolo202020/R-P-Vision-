package com.example.data.repository

import com.example.data.dao.AppDao
import com.example.data.model.AuditLog
import com.example.data.model.SalaryRecord
import com.example.data.model.Site
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.TransactionEntry
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

class ConstructionRepository(private val dao: AppDao) {

    val allUsers: Flow<List<User>> = dao.getAllUsers()
    val allSites: Flow<List<Site>> = dao.getAllSites()
    val allTransactions: Flow<List<TransactionEntry>> = dao.getAllTransactions()
    val allStaff: Flow<List<StaffMember>> = dao.getAllStaff()
    val allAttendance: Flow<List<StaffAttendance>> = dao.getAllAttendance()
    val allSalaryRecords: Flow<List<SalaryRecord>> = dao.getAllSalaryRecords()
    val allAuditLogs: Flow<List<AuditLog>> = dao.getAllAuditLogs()

    fun getTransactionsForSite(siteId: Long): Flow<List<TransactionEntry>> {
        return dao.getTransactionsBySite(siteId)
    }

    fun getTransactionsForUser(userId: Long): Flow<List<TransactionEntry>> {
        return dao.getTransactionsByUser(userId)
    }

    fun getStaffForSite(siteId: Long): Flow<List<StaffMember>> {
        return dao.getStaffBySite(siteId)
    }

    fun getAttendanceForSite(siteId: Long): Flow<List<StaffAttendance>> {
        return dao.getAttendanceBySite(siteId)
    }

    fun getAttendanceForSiteAndDate(siteId: Long, dateFormatted: String): Flow<List<StaffAttendance>> {
        return dao.getAttendanceBySiteAndDate(siteId, dateFormatted)
    }

    fun getSalaryRecordsForSite(siteId: Long): Flow<List<SalaryRecord>> {
        return dao.getSalaryRecordsBySite(siteId)
    }

    suspend fun getUserByMobile(mobile: String): User? {
        return dao.getUserByMobile(mobile)
    }

    suspend fun getUserById(id: Long): User? {
        return dao.getUserById(id)
    }

    suspend fun getSiteById(siteId: Long): Site? {
        return dao.getSiteById(siteId)
    }

    suspend fun addTransaction(entry: TransactionEntry): Long {
        return dao.insertTransaction(entry)
    }

    suspend fun insertTransaction(entry: TransactionEntry): Long {
        return dao.insertTransaction(entry)
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteTransactionById(id)
    }

    suspend fun insertSite(site: Site): Long {
        return dao.insertSite(site)
    }

    suspend fun deleteSite(id: Long) {
        dao.deleteSiteById(id)
    }

    suspend fun insertSites(sites: List<Site>) {
        dao.insertSites(sites)
    }

    suspend fun insertTransactions(entries: List<TransactionEntry>) {
        dao.insertTransactions(entries)
    }

    suspend fun insertUser(user: User): Long {
        return dao.insertUser(user)
    }

    suspend fun deleteUser(id: Long) {
        dao.deleteUserById(id)
    }

    suspend fun insertStaff(staff: StaffMember): Long {
        return dao.insertStaff(staff)
    }

    suspend fun deleteStaff(id: Long) {
        dao.deleteStaff(id)
    }

    suspend fun insertAttendance(attendance: StaffAttendance): Long {
        return dao.insertAttendance(attendance)
    }

    suspend fun deleteAttendance(id: Long) {
        dao.deleteAttendance(id)
    }

    suspend fun insertSalaryRecord(record: SalaryRecord): Long {
        return dao.insertSalaryRecord(record)
    }

    suspend fun deleteSalaryRecord(id: Long) {
        dao.deleteSalaryRecord(id)
    }

    suspend fun insertAuditLog(log: AuditLog): Long {
        return dao.insertAuditLog(log)
    }

    suspend fun insertAuditLogs(logs: List<AuditLog>) {
        dao.insertAuditLogs(logs)
    }

    suspend fun insertSalaryRecords(records: List<SalaryRecord>) {
        dao.insertSalaryRecords(records)
    }

    suspend fun insertAttendanceList(list: List<StaffAttendance>) {
        dao.insertAttendanceList(list)
    }

    suspend fun insertStaffList(list: List<StaffMember>) {
        dao.insertStaffList(list)
    }

    suspend fun getAllUsersList(): List<User> = dao.getAllUsersList()
    suspend fun getAllSitesList(): List<Site> = dao.getAllSitesList()
    suspend fun getAllTransactionsList(): List<TransactionEntry> = dao.getAllTransactionsList()
    suspend fun getAllStaffList(): List<StaffMember> = dao.getAllStaffList()
    suspend fun getAllAttendanceList(): List<StaffAttendance> = dao.getAllAttendanceList()
    suspend fun getAllSalaryRecordsList(): List<SalaryRecord> = dao.getAllSalaryRecordsList()
    suspend fun getAllAuditLogsList(): List<AuditLog> = dao.getAllAuditLogsList()

    suspend fun clearAllOperationalData() {
        dao.clearAllTransactions()
        dao.clearAllStaff()
        dao.clearAllAttendance()
        dao.clearAllSalaryRecords()
    }
}

