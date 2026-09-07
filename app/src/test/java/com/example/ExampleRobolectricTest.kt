package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CategoryConstants
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.TransactionEntry
import com.example.util.AttendancePdfHelper
import com.example.util.DayBookPdfHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name resource is RPVC Pvt Ltd`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RPVC Pvt Ltd", appName)
    }

    @Test
    fun `verify all 3 income master categories exist`() {
        val incomeCategories = CategoryConstants.INCOME_CATEGORIES.map { it.mainCategory }
        assertEquals(3, incomeCategories.size)
        assertTrue(incomeCategories.contains("Head Office"))
        assertTrue(incomeCategories.contains("Loan Received"))
        assertTrue(incomeCategories.contains("Income – Machinery & Site"))
    }

    @Test
    fun `verify payment modes include Cash, Bank, UPI, Other`() {
        val modes = CategoryConstants.PAYMENT_MODES
        assertEquals(listOf("Cash", "Bank", "UPI", "Other"), modes)
    }

    @Test
    fun `verify expense master categories exist`() {
        val expenseCategories = CategoryConstants.EXPENSE_CATEGORIES.map { it.mainCategory }
        assertEquals(12, expenseCategories.size)
        assertTrue(expenseCategories.contains("Material Purchase"))
        assertTrue(expenseCategories.contains("Site Labour"))
        assertTrue(expenseCategories.contains("Machinery & Equipment"))
        assertTrue(expenseCategories.contains("Transportation"))
        assertTrue(expenseCategories.contains("Site Expenses"))
        assertTrue(expenseCategories.contains("Tools & Consumables"))
        assertTrue(expenseCategories.contains("Sub-Contractor"))
        assertTrue(expenseCategories.contains("Staff Expenses"))
        assertTrue(expenseCategories.contains("Office Expenses"))
        assertTrue(expenseCategories.contains("Mess"))
        assertTrue(expenseCategories.contains("Miscellaneous"))
        assertTrue(expenseCategories.contains("Loan Return"))
    }

    @Test
    fun `verify currency formatting helper produces proper INR symbols`() {
        val formattedDayBook = DayBookPdfHelper.formatCurrency(50000.0)
        assertTrue(formattedDayBook.contains("50,000.00"))
        assertTrue(formattedDayBook.contains("₹"))

        val formattedAttendance = AttendancePdfHelper.formatCurrency(1250.50)
        assertTrue(formattedAttendance.contains("1,250.50"))
        assertTrue(formattedAttendance.contains("₹"))
    }

    @Test
    fun `verify attendance report items builder correctly calculates wages`() {
        val staffList = listOf(
            StaffMember(id = 101, name = "Ramu", mobile = "9876543210", designation = "Mason", siteId = 1, siteName = "Tower A", dailyWage = 800.0, paymentType = "DAILY"),
            StaffMember(id = 102, name = "Shyam", mobile = "9876543211", designation = "Helper", siteId = 1, siteName = "Tower A", dailyWage = 500.0, paymentType = "DAILY")
        )
        val attendanceList = listOf(
            StaffAttendance(staffId = 101, staffName = "Ramu", siteId = 1, siteName = "Tower A", dateMillis = 1000L, dateFormatted = "2026-08-30", status = "HALF_DAY", wageAmount = 400.0, remarks = "Half day duty")
        )

        val reportItems = AttendancePdfHelper.buildReportItems(
            staffList = staffList,
            attendanceList = attendanceList,
            dateFormatted = "2026-08-30",
            siteId = 1
        )

        assertEquals(2, reportItems.size)
        val ramu = reportItems.find { it.staffId == 101L }
        assertNotNull(ramu)
        assertEquals("HALF_DAY", ramu?.status)
        assertEquals(400.0, ramu?.wageAmount ?: 0.0, 0.01)

        val shyam = reportItems.find { it.staffId == 102L }
        assertNotNull(shyam)
        assertEquals("PRESENT", shyam?.status)
        assertEquals(500.0, shyam?.wageAmount ?: 0.0, 0.01)
    }

    @Test
    fun `verify transaction entry model defaults and fields`() {
        val tx = TransactionEntry(
            id = 1L,
            type = "EXPENSE",
            dateMillis = 1700000000000L,
            dateFormatted = "2026-08-30",
            siteId = 1L,
            siteName = "Metro City Tower",
            userId = 2L,
            userName = "Ramesh Kumar",
            userMobile = "9876500002",
            category = "Diesel",
            subCategory = "Machinery Diesel",
            partyName = "IOCL Station",
            description = "100L Diesel for Crane",
            amount = 9500.0,
            paymentMode = "UPI",
            receiptPhotoUri = null,
            remarks = "Paid via GPay"
        )
        assertEquals("EXPENSE", tx.type)
        assertEquals(9500.0, tx.amount, 0.01)
        assertEquals("UPI", tx.paymentMode)
        assertEquals(1L, tx.siteId)
    }

    @Test
    fun `verify user models and role definitions`() {
        val adminUser = com.example.data.model.User(
            id = 1L,
            mobile = "9621803006",
            password = "80808080",
            name = "RPVC Admin",
            role = "ADMIN",
            assignedSiteId = null,
            assignedSiteName = null
        )
        assertEquals("ADMIN", adminUser.role)
        assertEquals("9621803006", adminUser.mobile)
        assertEquals("80808080", adminUser.password)

        val inchargeUser = com.example.data.model.User(
            id = 2L,
            mobile = "9876500002",
            password = "123",
            name = "Ramesh Kumar",
            role = "SITE_INCHARGE",
            assignedSiteId = 1L,
            assignedSiteName = "Metro City Tower"
        )
        assertEquals("SITE_INCHARGE", inchargeUser.role)
        assertEquals(1L, inchargeUser.assignedSiteId)
    }
}
