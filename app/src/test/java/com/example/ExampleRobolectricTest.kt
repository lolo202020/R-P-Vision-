package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CategoryConstants
import com.example.data.model.Site
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.TransactionEntry
import com.example.util.AttendancePdfHelper
import com.example.util.DayBookExcelHelper
import com.example.util.DayBookPdfHelper
import com.example.util.MonthlyExpensePdfHelper
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

    @Test
    fun `verify DayBook Excel export generates valid xlsx file with categories master`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val txList = listOf(
            TransactionEntry(
                id = 1L,
                type = "EXPENSE",
                dateMillis = 1700000000000L,
                dateFormatted = "2026-08-30",
                siteId = 1L,
                siteName = "Test Site",
                userId = 2L,
                userName = "Ramesh",
                userMobile = "9876500002",
                category = "Material Purchase",
                subCategory = "Cement",
                partyName = "UltraTech",
                description = "50 bags",
                amount = 18000.0,
                paymentMode = "UPI"
            ),
            TransactionEntry(
                id = 2L,
                type = "INCOME",
                dateMillis = 1700000100000L,
                dateFormatted = "2026-08-30",
                siteId = 1L,
                siteName = "Test Site",
                userId = 2L,
                userName = "Ramesh",
                userMobile = "9876500002",
                category = "Head Office",
                subCategory = "HO Advance",
                partyName = "HO",
                description = "Advance",
                amount = 50000.0,
                paymentMode = "Bank"
            )
        )

        val file = DayBookExcelHelper.exportDayBookToXlsx(context, txList, "2026-08-01", "2026-08-30", "Test Site")
        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 500)
        assertTrue(file.name.endsWith(".xlsx"))
    }

    @Test
    fun `verify DayBook sample template generates valid xlsx file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sampleFile = DayBookExcelHelper.generateSampleXlsxFile(context)
        assertNotNull(sampleFile)
        assertTrue(sampleFile.exists())
        assertTrue(sampleFile.length() > 500)
        assertTrue(sampleFile.name.endsWith(".xlsx"))
    }

    @Test
    fun `verify category expense breakdown groups and filters entries correctly`() {
        val tx1 = TransactionEntry(
            id = 1L,
            type = "EXPENSE",
            dateMillis = 1700000000000L,
            dateFormatted = "2026-09-01",
            siteId = 1L,
            siteName = "Site A",
            userId = 1L,
            userName = "Incharge",
            userMobile = "9876543210",
            category = "Material Purchase",
            subCategory = "General",
            partyName = "UltraTech Cement",
            description = "50 bags cement",
            amount = 18000.0,
            paymentMode = "Cash"
        )
        val tx2 = TransactionEntry(
            id = 2L,
            type = "EXPENSE",
            dateMillis = 1700001000000L,
            dateFormatted = "2026-09-02",
            siteId = 1L,
            siteName = "Site A",
            userId = 1L,
            userName = "Incharge",
            userMobile = "9876543210",
            category = "Material Purchase",
            subCategory = "General",
            partyName = "Tata Steel",
            description = "TMT bars",
            amount = 32000.0,
            paymentMode = "Bank"
        )
        val tx3 = TransactionEntry(
            id = 3L,
            type = "EXPENSE",
            dateMillis = 1700002000000L,
            dateFormatted = "2026-09-03",
            siteId = 1L,
            siteName = "Site A",
            userId = 1L,
            userName = "Incharge",
            userMobile = "9876543210",
            category = "Site Labour",
            subCategory = "General",
            partyName = "Raju Mason",
            description = "Daily labour wages",
            amount = 10000.0,
            paymentMode = "Cash"
        )

        val transactions = listOf(tx1, tx2, tx3)
        val materialTxs = transactions.filter { it.type == "EXPENSE" && it.category == "Material Purchase" }
        assertEquals(2, materialTxs.size)
        assertEquals(50000.0, materialTxs.sumOf { it.amount }, 0.01)

        val labourTxs = transactions.filter { it.type == "EXPENSE" && it.category == "Site Labour" }
        assertEquals(1, labourTxs.size)
        assertEquals(10000.0, labourTxs.sumOf { it.amount }, 0.01)
    }

    @Test
    fun `verify monthly expense report data builder calculates site and category summaries`() {
        val sites = listOf(
            Site(id = 1, name = "Tower Alpha", code = "TWR-A", location = "Sector 62", clientName = "Apex", budget = 5000000.0, inchargeName = "Incharge 1"),
            Site(id = 2, name = "Bridge Beta", code = "BRG-B", location = "Zone 4", clientName = "City Infra", budget = 8000000.0, inchargeName = "Incharge 2")
        )

        val txs = listOf(
            TransactionEntry(id = 1, type = "EXPENSE", dateMillis = 1000L, dateFormatted = "2026-09-05", siteId = 1, siteName = "Tower Alpha", userId = 1, userName = "U1", userMobile = "1", category = "Material Purchase", subCategory = "Cement", partyName = "P1", description = "D1", amount = 60000.0, paymentMode = "Bank"),
            TransactionEntry(id = 2, type = "EXPENSE", dateMillis = 2000L, dateFormatted = "2026-09-10", siteId = 1, siteName = "Tower Alpha", userId = 1, userName = "U1", userMobile = "1", category = "Site Labour", subCategory = "Wages", partyName = "P2", description = "D2", amount = 40000.0, paymentMode = "Cash"),
            TransactionEntry(id = 3, type = "EXPENSE", dateMillis = 3000L, dateFormatted = "2026-09-15", siteId = 2, siteName = "Bridge Beta", userId = 2, userName = "U2", userMobile = "2", category = "Machinery & Equipment", subCategory = "Diesel", partyName = "P3", description = "D3", amount = 50000.0, paymentMode = "UPI"),
            TransactionEntry(id = 4, type = "EXPENSE", dateMillis = 4000L, dateFormatted = "2026-08-20", siteId = 1, siteName = "Tower Alpha", userId = 1, userName = "U1", userMobile = "1", category = "Material Purchase", subCategory = "Steel", partyName = "P4", description = "Old month", amount = 99999.0, paymentMode = "Cash"),
            TransactionEntry(id = 5, type = "INCOME", dateMillis = 5000L, dateFormatted = "2026-09-01", siteId = 1, siteName = "Tower Alpha", userId = 1, userName = "U1", userMobile = "1", category = "Head Office", subCategory = "Advance", partyName = "HO", description = "Income should be ignored", amount = 100000.0, paymentMode = "Bank")
        )

        val report = MonthlyExpensePdfHelper.buildMonthlyReportData(
            transactions = txs,
            sites = sites,
            yearMonth = "2026-09",
            siteIdFilter = null
        )

        assertEquals("2026-09", report.yearMonth)
        assertEquals(150000.0, report.totalExpense, 0.01)
        assertEquals(3, report.totalTransactions)
        assertEquals(2, report.totalSitesCount)

        // Site 1 had 60k + 40k = 100k
        val site1Summary = report.siteSummaries.find { it.siteId == 1L }
        assertNotNull(site1Summary)
        assertEquals(100000.0, site1Summary!!.totalExpense, 0.01)
        assertEquals(2, site1Summary.txCount)
        assertEquals(66.67, site1Summary.percentageOfTotal, 0.5)

        // Site 2 had 50k
        val site2Summary = report.siteSummaries.find { it.siteId == 2L }
        assertNotNull(site2Summary)
        assertEquals(50000.0, site2Summary!!.totalExpense, 0.01)
        assertEquals(1, site2Summary.txCount)
        assertEquals(33.33, site2Summary.percentageOfTotal, 0.5)

        // Top category across all sites should be Material Purchase (60k)
        val topCategory = report.overallCategoryTotals.firstOrNull()
        assertNotNull(topCategory)
        assertEquals("Material Purchase", topCategory?.category)
        assertEquals(60000.0, topCategory?.totalAmount ?: 0.0, 0.01)
    }

    @Test
    fun `verify monthly expense currency format and report calculations`() {
        val formatted = MonthlyExpensePdfHelper.formatCurrency(45000.0)
        assertTrue(formatted.contains("45,000.00"))
        assertTrue(formatted.contains("₹"))

        val sites = listOf(
            Site(id = 1, name = "Tower Alpha", code = "TWR-A", location = "Sector 62", clientName = "Apex", budget = 5000000.0, inchargeName = "Incharge 1")
        )
        val txs = listOf(
            TransactionEntry(id = 1, type = "EXPENSE", dateMillis = 1000L, dateFormatted = "2026-09-05", siteId = 1, siteName = "Tower Alpha", userId = 1, userName = "U1", userMobile = "1", category = "Material Purchase", subCategory = "Cement", partyName = "UltraTech", description = "Cement bags", amount = 45000.0, paymentMode = "Bank")
        )

        val reportData = MonthlyExpensePdfHelper.buildMonthlyReportData(
            transactions = txs,
            sites = sites,
            yearMonth = "2026-09",
            siteIdFilter = 1L
        )

        assertEquals("September 2026", reportData.monthDisplay)
        assertEquals(45000.0, reportData.totalExpense, 0.01)
        assertEquals(1, reportData.siteSummaries.size)
        assertEquals("Tower Alpha", reportData.siteSummaries[0].siteName)
        assertEquals(1, reportData.siteSummaries[0].categoryBreakdown.size)
        assertEquals("Material Purchase", reportData.siteSummaries[0].categoryBreakdown[0].category)
    }
}
