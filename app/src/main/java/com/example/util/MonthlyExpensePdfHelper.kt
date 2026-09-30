package com.example.util

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Site
import com.example.data.model.TransactionEntry
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-resolution Native Android PDF Generator, Downloader, and Direct Share Helper
 * for Monthly Site-Wise Expense Summary Reports.
 * Categorizes and aggregates all construction expenses by Site and by Expense Category.
 */
object MonthlyExpensePdfHelper {

    private val indianCurrencyFormat = DecimalFormat("##,##,##0.00", DecimalFormatSymbols(Locale("en", "IN")))

    fun formatCurrency(amount: Double): String {
        return "₹ " + indianCurrencyFormat.format(amount)
    }

    data class CategorySummary(
        val category: String,
        val totalAmount: Double,
        val percentageOfSite: Double,
        val count: Int
    )

    data class SiteMonthlySummary(
        val siteId: Long,
        val siteName: String,
        val siteCode: String,
        val siteLocation: String,
        val totalExpense: Double,
        val percentageOfTotal: Double,
        val txCount: Int,
        val categoryBreakdown: List<CategorySummary>,
        val transactions: List<TransactionEntry>
    )

    data class MonthlyReportData(
        val yearMonth: String,       // e.g. "2026-09"
        val monthDisplay: String,    // e.g. "September 2026"
        val totalExpense: Double,
        val totalTransactions: Int,
        val totalSitesCount: Int,
        val siteSummaries: List<SiteMonthlySummary>,
        val overallCategoryTotals: List<CategorySummary>,
        val generatedAtFormatted: String
    )

    /**
     * Builds structured data for the monthly site-wise expense report.
     */
    fun buildMonthlyReportData(
        transactions: List<TransactionEntry>,
        sites: List<Site>,
        yearMonth: String, // "YYYY-MM"
        siteIdFilter: Long? = null // null means All Sites
    ): MonthlyReportData {
        // Filter expenses for the given month
        val monthExpenses = transactions.filter { tx ->
            tx.type.equals("EXPENSE", ignoreCase = true) &&
                    tx.dateFormatted.startsWith(yearMonth) &&
                    (siteIdFilter == null || siteIdFilter <= 0L || tx.siteId == siteIdFilter)
        }

        val totalMonthExpense = monthExpenses.sumOf { it.amount }
        val siteMap = sites.associateBy { it.id }

        // Group by site
        val groupedBySite = monthExpenses.groupBy { it.siteId }
        val siteSummaries = mutableListOf<SiteMonthlySummary>()

        for ((sId, sTxs) in groupedBySite) {
            val siteInfo = siteMap[sId]
            val sName = siteInfo?.name ?: sTxs.firstOrNull()?.siteName ?: "Site #$sId"
            val sCode = siteInfo?.code ?: "PRJ-$sId"
            val sLoc = siteInfo?.location ?: "Site Location"
            val siteTotal = sTxs.sumOf { it.amount }
            val sitePct = if (totalMonthExpense > 0) (siteTotal / totalMonthExpense) * 100.0 else 0.0

            // Category breakdown for this site
            val catGrouped = sTxs.groupBy { it.category.ifBlank { "General" } }
            val catList = catGrouped.map { (catName, catTxs) ->
                val catSum = catTxs.sumOf { it.amount }
                val catPct = if (siteTotal > 0) (catSum / siteTotal) * 100.0 else 0.0
                CategorySummary(
                    category = catName,
                    totalAmount = catSum,
                    percentageOfSite = catPct,
                    count = catTxs.size
                )
            }.sortedByDescending { it.totalAmount }

            siteSummaries.add(
                SiteMonthlySummary(
                    siteId = sId,
                    siteName = sName,
                    siteCode = sCode,
                    siteLocation = sLoc,
                    totalExpense = siteTotal,
                    percentageOfTotal = sitePct,
                    txCount = sTxs.size,
                    categoryBreakdown = catList,
                    transactions = sTxs.sortedByDescending { it.dateMillis }
                )
            )
        }

        // Sort sites by total expense descending
        siteSummaries.sortByDescending { it.totalExpense }

        // Overall category totals across all selected sites
        val overallCatGrouped = monthExpenses.groupBy { it.category.ifBlank { "General" } }
        val overallCategoryTotals = overallCatGrouped.map { (catName, catTxs) ->
            val catSum = catTxs.sumOf { it.amount }
            val catPct = if (totalMonthExpense > 0) (catSum / totalMonthExpense) * 100.0 else 0.0
            CategorySummary(
                category = catName,
                totalAmount = catSum,
                percentageOfSite = catPct,
                count = catTxs.size
            )
        }.sortedByDescending { it.totalAmount }

        val monthDisplay = try {
            val ymDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(yearMonth)
            if (ymDate != null) SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(ymDate) else yearMonth
        } catch (e: Exception) {
            yearMonth
        }

        val generatedAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        return MonthlyReportData(
            yearMonth = yearMonth,
            monthDisplay = monthDisplay,
            totalExpense = totalMonthExpense,
            totalTransactions = monthExpenses.size,
            totalSitesCount = siteSummaries.size,
            siteSummaries = siteSummaries,
            overallCategoryTotals = overallCategoryTotals,
            generatedAtFormatted = generatedAt
        )
    }

    /**
     * Generates a pristine, vector-rendered A4 PDF report for monthly site expenses.
     */
    fun generateMonthlyExpensePdf(
        context: Context,
        reportData: MonthlyReportData,
        selectedSiteName: String? = null
    ): File {
        val exportDir = File(context.cacheDir, "monthly_expense_reports").apply { mkdirs() }
        val cleanMonth = reportData.yearMonth.replace("-", "_")
        val cleanSite = (selectedSiteName ?: "All_Sites").replace(" ", "_").replace("/", "_")
        val fileName = "Monthly_Expense_Report_${cleanMonth}_${cleanSite}.pdf"
        val file = File(exportDir, fileName)
        if (file.exists()) file.delete()

        val pageWidth = 595
        val pageHeight = 842
        val margin = 32f
        val contentWidth = pageWidth - (2 * margin)

        val pdfDocument = PdfDocument()

        // Color definitions
        val colorPrimary = Color.rgb(15, 44, 89)      // Royal Navy Blue
        val colorPrimaryLight = Color.rgb(235, 242, 252) // Very light blue
        val colorExpenseRed = Color.rgb(185, 28, 28)  // Crimson Red
        val colorExpenseRedLight = Color.rgb(254, 242, 242) // Light red
        val colorDarkText = Color.rgb(30, 41, 59)     // Slate 800
        val colorMutedText = Color.rgb(100, 116, 139) // Slate 500
        val colorBorder = Color.rgb(226, 232, 240)    // Slate 200
        val colorCardBg = Color.rgb(248, 250, 252)    // Slate 50
        val colorAccentAmber = Color.rgb(217, 119, 6) // Amber 600

        // Paints
        val brandPaint = Paint().apply {
            isAntiAlias = true
            color = colorPrimary
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val reportTitlePaint = Paint().apply {
            isAntiAlias = true
            color = colorExpenseRed
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subTitlePaint = Paint().apply {
            isAntiAlias = true
            color = colorMutedText
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val sectionHeaderPaint = Paint().apply {
            isAntiAlias = true
            color = colorPrimary
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            color = colorDarkText
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldBodyPaint = Paint().apply {
            isAntiAlias = true
            color = colorDarkText
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val amountPaint = Paint().apply {
            isAntiAlias = true
            color = colorExpenseRed
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerThPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = colorBorder
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
        }
        val bgPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        var currentPageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeaderAndFooter(pageNum: Int) {
            // Header Top Bar
            bgPaint.color = colorPrimary
            canvas.drawRect(margin, 24f, pageWidth - margin, 27f, bgPaint)

            canvas.drawText("RPVC INFRA & PROJECTS PVT. LTD.", margin, 42f, brandPaint)
            canvas.drawText("MONTHLY SITE-WISE EXPENSE AUDIT REPORT", margin, 58f, reportTitlePaint)
            val subHeader = "Month: ${reportData.monthDisplay} | Target: ${selectedSiteName ?: "All Sites"} | Generated: ${reportData.generatedAtFormatted}"
            canvas.drawText(subHeader, margin, 71f, subTitlePaint)

            // Header separator
            borderPaint.color = colorBorder
            canvas.drawLine(margin, 78f, pageWidth - margin, 78f, borderPaint)

            // Footer
            canvas.drawLine(margin, pageHeight - 34f, pageWidth - margin, pageHeight - 34f, borderPaint)
            val footerNote = "Confidential • RPVC Construction ERP • For internal finance and audit purpose only"
            canvas.drawText(footerNote, margin, pageHeight - 22f, subTitlePaint)
            val pageStr = "Page $pageNum"
            val pageStrWidth = subTitlePaint.measureText(pageStr)
            canvas.drawText(pageStr, pageWidth - margin - pageStrWidth, pageHeight - 22f, subTitlePaint)
        }

        fun startNewPage() {
            pdfDocument.finishPage(page)
            currentPageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            drawHeaderAndFooter(currentPageNumber)
        }

        // Draw First Page Header
        drawHeaderAndFooter(currentPageNumber)
        var currentY = 92f

        // 1. Executive Summary KPI Cards Row (Total Expenses, Active Sites, Entries, Top Category)
        val cardHeight = 48f
        val cardWidth = (contentWidth - 18f) / 3f

        // Card 1: Total Monthly Expenses
        bgPaint.color = colorExpenseRedLight
        canvas.drawRoundRect(RectF(margin, currentY, margin + cardWidth, currentY + cardHeight), 6f, 6f, bgPaint)
        borderPaint.color = colorExpenseRed
        canvas.drawRoundRect(RectF(margin, currentY, margin + cardWidth, currentY + cardHeight), 6f, 6f, borderPaint)
        subTitlePaint.color = colorExpenseRed
        canvas.drawText("TOTAL MONTHLY EXPENSES", margin + 8f, currentY + 14f, subTitlePaint)
        val kpiAmountPaint = Paint(amountPaint).apply { textSize = 13f }
        canvas.drawText(formatCurrency(reportData.totalExpense), margin + 8f, currentY + 34f, kpiAmountPaint)

        // Card 2: Sites & Entries
        val card2X = margin + cardWidth + 9f
        bgPaint.color = colorPrimaryLight
        canvas.drawRoundRect(RectF(card2X, currentY, card2X + cardWidth, currentY + cardHeight), 6f, 6f, bgPaint)
        borderPaint.color = colorPrimary
        canvas.drawRoundRect(RectF(card2X, currentY, card2X + cardWidth, currentY + cardHeight), 6f, 6f, borderPaint)
        subTitlePaint.color = colorPrimary
        canvas.drawText("SITES & TRANSACTIONS", card2X + 8f, currentY + 14f, subTitlePaint)
        val kpiSitesPaint = Paint(boldBodyPaint).apply { textSize = 12f; color = colorPrimary }
        canvas.drawText("${reportData.totalSitesCount} Sites  •  ${reportData.totalTransactions} Entries", card2X + 8f, currentY + 34f, kpiSitesPaint)

        // Card 3: Top Expense Head
        val card3X = card2X + cardWidth + 9f
        bgPaint.color = colorCardBg
        canvas.drawRoundRect(RectF(card3X, currentY, card3X + cardWidth, currentY + cardHeight), 6f, 6f, bgPaint)
        borderPaint.color = colorBorder
        canvas.drawRoundRect(RectF(card3X, currentY, card3X + cardWidth, currentY + cardHeight), 6f, 6f, borderPaint)
        subTitlePaint.color = colorMutedText
        canvas.drawText("TOP EXPENSE HEAD", card3X + 8f, currentY + 14f, subTitlePaint)
        val topCat = reportData.overallCategoryTotals.firstOrNull()?.category ?: "None"
        val topCatAmt = reportData.overallCategoryTotals.firstOrNull()?.totalAmount ?: 0.0
        val topCatStr = if (topCatAmt > 0) "$topCat (${formatCurrency(topCatAmt)})" else "No Expenses"
        canvas.drawText(truncateText(topCatStr, cardWidth - 16f, boldBodyPaint), card3X + 8f, currentY + 34f, boldBodyPaint)

        currentY += cardHeight + 16f

        // 2. Site-Wise Summary Comparison Table
        canvas.drawText("1. SITE-WISE EXPENSE ALLOCATION & COMPARISON", margin, currentY, sectionHeaderPaint)
        currentY += 8f

        // Table Header
        val colSiteName = margin
        val colSiteNameW = 160f
        val colLocation = colSiteName + colSiteNameW
        val colLocationW = 100f
        val colTxCount = colLocation + colLocationW
        val colTxCountW = 55f
        val colSiteShare = colTxCount + colTxCountW
        val colSiteShareW = 55f
        val colSiteTotal = colSiteShare + colSiteShareW
        val colSiteTotalW = contentWidth - (colSiteNameW + colLocationW + colTxCountW + colSiteShareW)

        val thHeight = 18f
        bgPaint.color = colorPrimary
        canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + thHeight), 4f, 4f, bgPaint)

        canvas.drawText("Site / Project Name", colSiteName + 6f, currentY + 12f, headerThPaint)
        canvas.drawText("Location", colLocation + 4f, currentY + 12f, headerThPaint)
        canvas.drawText("Entries", colTxCount + 4f, currentY + 12f, headerThPaint)
        canvas.drawText("Share (%)", colSiteShare + 4f, currentY + 12f, headerThPaint)
        val thAmtStr = "Total Expense (₹)"
        val thAmtW = headerThPaint.measureText(thAmtStr)
        canvas.drawText(thAmtStr, colSiteTotal + colSiteTotalW - thAmtW - 6f, currentY + 12f, headerThPaint)
        currentY += thHeight

        // Rows
        for ((idx, siteSum) in reportData.siteSummaries.withIndex()) {
            val rowH = 17f
            if (currentY + rowH > pageHeight - 50f) {
                startNewPage()
                currentY = 92f
            }

            bgPaint.color = if (idx % 2 == 0) Color.WHITE else Color.rgb(248, 250, 252)
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowH, bgPaint)

            canvas.drawText(truncateText(siteSum.siteName, colSiteNameW - 10f, boldBodyPaint), colSiteName + 6f, currentY + 12f, boldBodyPaint)
            canvas.drawText(truncateText(siteSum.siteLocation, colLocationW - 8f, bodyPaint), colLocation + 4f, currentY + 12f, bodyPaint)
            canvas.drawText("${siteSum.txCount}", colTxCount + 8f, currentY + 12f, bodyPaint)
            canvas.drawText(String.format(Locale.US, "%.1f %%", siteSum.percentageOfTotal), colSiteShare + 4f, currentY + 12f, boldBodyPaint)

            val amtText = formatCurrency(siteSum.totalExpense)
            val amtW = amountPaint.measureText(amtText)
            canvas.drawText(amtText, colSiteTotal + colSiteTotalW - amtW - 6f, currentY + 12f, amountPaint)

            borderPaint.color = Color.rgb(241, 245, 249)
            canvas.drawLine(margin, currentY + rowH, pageWidth - margin, currentY + rowH, borderPaint)
            currentY += rowH
        }

        // Summary Total Row
        val totalRowH = 18f
        bgPaint.color = colorPrimaryLight
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + totalRowH, bgPaint)
        borderPaint.color = colorPrimary
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, borderPaint)
        canvas.drawLine(margin, currentY + totalRowH, pageWidth - margin, currentY + totalRowH, borderPaint)

        canvas.drawText("TOTAL EXPENSES (ALL SITES)", colSiteName + 6f, currentY + 12f, boldBodyPaint)
        canvas.drawText("${reportData.totalTransactions}", colTxCount + 8f, currentY + 12f, boldBodyPaint)
        canvas.drawText("100.0 %", colSiteShare + 4f, currentY + 12f, boldBodyPaint)
        val grandAmtStr = formatCurrency(reportData.totalExpense)
        val grandAmtW = amountPaint.measureText(grandAmtStr)
        canvas.drawText(grandAmtStr, colSiteTotal + colSiteTotalW - grandAmtW - 6f, currentY + 12f, amountPaint)
        currentY += totalRowH + 18f

        // 3. Overall Category Breakdown Across Month
        if (currentY + 100f > pageHeight - 50f) {
            startNewPage()
            currentY = 92f
        }

        canvas.drawText("2. OVERALL EXPENSE CATEGORY DISTRIBUTION", margin, currentY, sectionHeaderPaint)
        currentY += 8f

        // Category Table
        val colCatName = margin
        val colCatNameW = 200f
        val colCatCount = colCatName + colCatNameW
        val colCatCountW = 75f
        val colCatShare = colCatCount + colCatCountW
        val colCatShareW = 75f
        val colCatTotal = colCatShare + colCatShareW
        val colCatTotalW = contentWidth - (colCatNameW + colCatCountW + colCatShareW)

        bgPaint.color = Color.rgb(51, 65, 85) // Slate 700
        canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + thHeight), 4f, 4f, bgPaint)
        canvas.drawText("Expense Category / Head", colCatName + 6f, currentY + 12f, headerThPaint)
        canvas.drawText("Transactions", colCatCount + 4f, currentY + 12f, headerThPaint)
        canvas.drawText("Share (%)", colCatShare + 4f, currentY + 12f, headerThPaint)
        val catThAmtStr = "Total Amount (₹)"
        val catThAmtW = headerThPaint.measureText(catThAmtStr)
        canvas.drawText(catThAmtStr, colCatTotal + colCatTotalW - catThAmtW - 6f, currentY + 12f, headerThPaint)
        currentY += thHeight

        for ((idx, catItem) in reportData.overallCategoryTotals.withIndex()) {
            val rowH = 16f
            if (currentY + rowH > pageHeight - 50f) {
                startNewPage()
                currentY = 92f
            }

            bgPaint.color = if (idx % 2 == 0) Color.WHITE else Color.rgb(248, 250, 252)
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowH, bgPaint)

            canvas.drawText(truncateText(catItem.category, colCatNameW - 10f, boldBodyPaint), colCatName + 6f, currentY + 11.5f, boldBodyPaint)
            canvas.drawText("${catItem.count} entries", colCatCount + 4f, currentY + 11.5f, bodyPaint)
            canvas.drawText(String.format(Locale.US, "%.1f %%", catItem.percentageOfSite), colCatShare + 4f, currentY + 11.5f, boldBodyPaint)

            val cAmt = formatCurrency(catItem.totalAmount)
            val cAmtW = amountPaint.measureText(cAmt)
            canvas.drawText(cAmt, colCatTotal + colCatTotalW - cAmtW - 6f, currentY + 11.5f, amountPaint)

            borderPaint.color = Color.rgb(241, 245, 249)
            canvas.drawLine(margin, currentY + rowH, pageWidth - margin, currentY + rowH, borderPaint)
            currentY += rowH
        }

        currentY += 20f

        // 4. Detailed Site-by-Site Categorized Breakdown & Entry Audit
        if (currentY + 80f > pageHeight - 50f) {
            startNewPage()
            currentY = 92f
        }

        canvas.drawText("3. DETAILED SITE CATEGORIZATION & RECENT PAYMENTS", margin, currentY, sectionHeaderPaint)
        currentY += 12f

        for (siteSum in reportData.siteSummaries) {
            // Header for this site
            if (currentY + 120f > pageHeight - 50f) {
                startNewPage()
                currentY = 92f
            }

            // Site Box Header
            val siteBannerH = 22f
            bgPaint.color = colorPrimary
            canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + siteBannerH), 4f, 4f, bgPaint)

            val siteTitleText = "SITE: ${siteSum.siteName.uppercase()} (${siteSum.siteCode})"
            canvas.drawText(siteTitleText, margin + 8f, currentY + 15f, headerThPaint)

            val siteTotalStr = "Site Total: ${formatCurrency(siteSum.totalExpense)} (${String.format(Locale.US, "%.1f%%", siteSum.percentageOfTotal)})"
            val siteTotalW = headerThPaint.measureText(siteTotalStr)
            canvas.drawText(siteTotalStr, pageWidth - margin - siteTotalW - 8f, currentY + 15f, headerThPaint)
            currentY += siteBannerH + 4f

            // Category summary line chips for this site
            val catBreakdownText = buildString {
                append("Category Heads: ")
                siteSum.categoryBreakdown.take(4).forEachIndexed { cIdx, c ->
                    if (cIdx > 0) append("  •  ")
                    append("${c.category}: ${formatCurrency(c.totalAmount)}")
                }
            }
            canvas.drawText(truncateText(catBreakdownText, contentWidth - 10f, subTitlePaint), margin + 4f, currentY + 9f, subTitlePaint)
            currentY += 16f

            // Table of expenses for this site
            val colTxDate = margin
            val colTxDateW = 60f
            val colTxParty = colTxDate + colTxDateW
            val colTxPartyW = 140f
            val colTxCat = colTxParty + colTxPartyW
            val colTxCatW = 110f
            val colTxMode = colTxCat + colTxCatW
            val colTxModeW = 55f
            val colTxAmt = colTxMode + colTxModeW
            val colTxAmtW = contentWidth - (colTxDateW + colTxPartyW + colTxCatW + colTxModeW)

            // Sub table header
            bgPaint.color = colorCardBg
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 15f, bgPaint)
            borderPaint.color = colorBorder
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 15f, borderPaint)

            val subThPaint = Paint(boldBodyPaint).apply { textSize = 7.5f; color = colorMutedText }
            canvas.drawText("Date", colTxDate + 4f, currentY + 10.5f, subThPaint)
            canvas.drawText("Party / Vendor", colTxParty + 4f, currentY + 10.5f, subThPaint)
            canvas.drawText("Category", colTxCat + 4f, currentY + 10.5f, subThPaint)
            canvas.drawText("Mode", colTxMode + 4f, currentY + 10.5f, subThPaint)
            val subThAmt = "Amount (₹)"
            val subThAmtW = subThPaint.measureText(subThAmt)
            canvas.drawText(subThAmt, colTxAmt + colTxAmtW - subThAmtW - 6f, currentY + 10.5f, subThPaint)
            currentY += 15f

            // List up to 15 transactions per site in this monthly summary
            val displayTxs = siteSum.transactions.take(15)
            for ((tIdx, tx) in displayTxs.withIndex()) {
                val tRowH = 15f
                if (currentY + tRowH > pageHeight - 50f) {
                    startNewPage()
                    currentY = 92f
                }

                bgPaint.color = if (tIdx % 2 == 0) Color.WHITE else Color.rgb(250, 250, 250)
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + tRowH, bgPaint)

                canvas.drawText(tx.dateFormatted, colTxDate + 4f, currentY + 10.5f, bodyPaint)
                canvas.drawText(truncateText(tx.partyName.ifBlank { tx.description }.ifBlank { "-" }, colTxPartyW - 8f, boldBodyPaint), colTxParty + 4f, currentY + 10.5f, boldBodyPaint)
                canvas.drawText(truncateText(tx.category, colTxCatW - 8f, bodyPaint), colTxCat + 4f, currentY + 10.5f, bodyPaint)
                canvas.drawText(tx.paymentMode, colTxMode + 4f, currentY + 10.5f, bodyPaint)

                val tAmtStr = formatCurrency(tx.amount)
                val tAmtW = amountPaint.measureText(tAmtStr)
                canvas.drawText(tAmtStr, colTxAmt + colTxAmtW - tAmtW - 6f, currentY + 10.5f, amountPaint)

                borderPaint.color = Color.rgb(245, 245, 245)
                canvas.drawLine(margin, currentY + tRowH, pageWidth - margin, currentY + tRowH, borderPaint)
                currentY += tRowH
            }

            if (siteSum.transactions.size > 15) {
                val moreText = "+ ${siteSum.transactions.size - 15} more expense transactions for ${siteSum.siteName} (See complete Day Book)"
                canvas.drawText(moreText, margin + 4f, currentY + 10f, subTitlePaint)
                currentY += 14f
            }

            currentY += 12f
        }

        pdfDocument.finishPage(page)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    private fun truncateText(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length - 1
        while (end > 0 && paint.measureText(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end) + "..." else text
    }

    /**
     * Copies / Downloads the generated PDF to the device public Downloads folder or external files directory,
     * triggering a system notification and Toast with file location.
     */
    fun downloadPdfToPublicFolder(context: Context, sourceFile: File): File {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RPVC_Reports")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    Toast.makeText(context, "Saved to Downloads/RPVC_Reports/${sourceFile.name}", Toast.LENGTH_LONG).show()
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "RPVC_Reports").apply { mkdirs() }
                val targetFile = File(targetDir, sourceFile.name)
                sourceFile.copyTo(targetFile, overwrite = true)
                Toast.makeText(context, "Saved to Downloads: ${targetFile.absolutePath}", Toast.LENGTH_LONG).show()
                return targetFile
            }
            sourceFile
        } catch (e: Exception) {
            Toast.makeText(context, "Saved locally in app storage: ${sourceFile.name}", Toast.LENGTH_SHORT).show()
            sourceFile
        }
    }

    /**
     * Opens the PDF in any installed native PDF viewer.
     */
    fun openPdfFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No PDF viewer app installed. Please choose Share to send or view.",
                Toast.LENGTH_LONG
            ).show()
            sharePdfFile(context, file, "Share Monthly Expense PDF Report")
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            sharePdfFile(context, file, "Share Monthly Expense PDF Report")
        }
    }

    /**
     * Standard Share Chooser for Email, WhatsApp, Drive, Bluetooth, etc.
     */
    fun sharePdfFile(
        context: Context,
        file: File,
        chooserTitle: String = "Share Monthly Expense PDF Report",
        customSubject: String? = null,
        customBody: String? = null
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val subject = customSubject ?: "Monthly Site-Wise Expense Summary Report (${file.name})"
            val body = customBody ?: "Attached is the Monthly Site-Wise Expense Summary Audit Report from RPVC Infra & Projects."

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Direct Share via Email apps (Gmail, Outlook, etc.).
     */
    fun shareViaEmail(
        context: Context,
        file: File,
        monthDisplay: String,
        totalExpenseFormatted: String
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val subject = "Monthly Site-Wise Expense Report - $monthDisplay (Total: $totalExpenseFormatted)"
            val body = """
                Respected Sir/Madam,
                
                Please find attached the official Monthly Site-Wise Expense Summary Report for $monthDisplay.
                
                • Total Monthly Expenses: $totalExpenseFormatted
                • Document: ${file.name}
                
                This report contains detailed site-wise expense allocations and category distributions.
                
                Regards,
                RPVC Infra & Projects Pvt. Ltd.
            """.trimIndent()

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(emailIntent, "Send Monthly Report via Email").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to launch email: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Direct Share via Messaging apps (WhatsApp, Telegram, etc.).
     */
    fun shareViaMessaging(
        context: Context,
        file: File,
        monthDisplay: String,
        totalExpenseFormatted: String
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val message = "📊 *RPVC Infra & Projects - Monthly Site-Wise Expense Report*\n\n" +
                    "🗓 *Period:* $monthDisplay\n" +
                    "💰 *Total Expenses:* $totalExpenseFormatted\n" +
                    "📄 *Report:* ${file.name}\n\n" +
                    "Please find the complete PDF report attached."

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share via WhatsApp / Messaging").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
