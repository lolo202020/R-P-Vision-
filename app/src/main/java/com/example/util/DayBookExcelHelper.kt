package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Xml
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntry
import jxl.Cell
import jxl.Sheet
import jxl.Workbook
import jxl.WorkbookSettings
import jxl.format.Alignment
import jxl.format.Border
import jxl.format.BorderLineStyle
import jxl.format.Colour
import jxl.format.UnderlineStyle
import jxl.format.VerticalAlignment
import jxl.write.Label
import jxl.write.Number
import jxl.write.NumberFormats
import jxl.write.WritableCellFormat
import jxl.write.WritableFont
import jxl.write.WritableSheet
import jxl.write.WritableWorkbook
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object DayBookExcelHelper {

    data class ImportResult(
        val validEntries: List<TransactionEntry>,
        val errors: List<String>,
        val totalRowsRead: Int,
        val totalDebit: Double,
        val totalCredit: Double
    )

    /**
     * Generates a REAL binary Microsoft Excel (.xls) BIFF8 file for filtered transactions.
     */
    fun exportDayBookToXls(
        context: Context,
        transactions: List<TransactionEntry>,
        fromDate: String? = null,
        toDate: String? = null,
        siteName: String? = null
    ): File {
        val exportDir = File(context.cacheDir, "excel_exports").apply { mkdirs() }
        val dateFromClean = fromDate?.replace("/", "-")?.replace(" ", "")?.ifBlank { null }
        val dateToClean = toDate?.replace("/", "-")?.replace(" ", "")?.ifBlank { null }
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val fileName = when {
            dateFromClean != null && dateToClean != null -> "DayBook_${dateFromClean}_to_${dateToClean}.xls"
            dateFromClean != null -> "DayBook_${dateFromClean}.xls"
            else -> "DayBook_${todayStr}.xls"
        }

        val file = File(exportDir, fileName)
        if (file.exists()) file.delete()

        val settings = WorkbookSettings().apply {
            encoding = "UTF-8"
        }
        val workbook: WritableWorkbook = Workbook.createWorkbook(file, settings)
        val sheet: WritableSheet = workbook.createSheet("DayBook", 0)

        // Fonts
        val titleFont = WritableFont(WritableFont.ARIAL, 13, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.DARK_BLUE)
        val titleFormat = WritableCellFormat(titleFont).apply {
            alignment = Alignment.CENTRE
            verticalAlignment = VerticalAlignment.CENTRE
        }

        val subTitleFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.GRAY_80)
        val subTitleFormat = WritableCellFormat(subTitleFont).apply {
            alignment = Alignment.CENTRE
        }

        val headerFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.WHITE)
        val headerFormat = WritableCellFormat(headerFont).apply {
            setBackground(Colour.OCEAN_BLUE)
            alignment = Alignment.CENTRE
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.THIN, Colour.GRAY_50)
        }

        val dataFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD)
        val dataFormatLeft = WritableCellFormat(dataFont).apply {
            alignment = Alignment.LEFT
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.THIN, Colour.GRAY_25)
        }
        val dataFormatCenter = WritableCellFormat(dataFont).apply {
            alignment = Alignment.CENTRE
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.THIN, Colour.GRAY_25)
        }
        val numFormat = WritableCellFormat(dataFont, NumberFormats.FORMAT3).apply {
            alignment = Alignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.THIN, Colour.GRAY_25)
        }

        val debitFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.DARK_RED)
        val debitFormat = WritableCellFormat(debitFont, NumberFormats.FORMAT3).apply {
            alignment = Alignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.THIN, Colour.GRAY_25)
        }

        val creditFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.DARK_GREEN)
        val creditFormat = WritableCellFormat(creditFont, NumberFormats.FORMAT3).apply {
            alignment = Alignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.THIN, Colour.GRAY_25)
        }

        val totalFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.BLACK)
        val totalLabelFormat = WritableCellFormat(totalFont).apply {
            setBackground(Colour.GRAY_25)
            alignment = Alignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.MEDIUM, Colour.BLACK)
        }
        val totalNumFormat = WritableCellFormat(totalFont, NumberFormats.FORMAT3).apply {
            setBackground(Colour.GRAY_25)
            alignment = Alignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.MEDIUM, Colour.BLACK)
        }

        // Header Title Block
        sheet.mergeCells(0, 0, 10, 0)
        sheet.addCell(Label(0, 0, "DAY BOOK REGISTER REPORT", titleFormat))

        sheet.mergeCells(0, 1, 10, 1)
        val periodText = buildString {
            if (!dateFromClean.isNullOrBlank() || !dateToClean.isNullOrBlank()) {
                append("Period: ${dateFromClean ?: "Start"} to ${dateToClean ?: "End"}")
            } else {
                append("All Records (Up to $todayStr)")
            }
            if (!siteName.isNullOrBlank() && siteName != "ALL") {
                append(" | Site: $siteName")
            }
        }
        sheet.addCell(Label(0, 1, periodText, subTitleFormat))

        val headers = arrayOf(
            "Date", "Entry Type", "Party / Vendor Name", "Category", "Sub Category",
            "Site", "Description", "Payment Mode", "Debit (₹)", "Credit (₹)", "Balance (₹)"
        )

        val headerRowIndex = 3
        for (i in headers.indices) {
            sheet.addCell(Label(i, headerRowIndex, headers[i], headerFormat))
        }

        var currentRow = headerRowIndex + 1
        var runningBalance = 0.0
        var totalDebit = 0.0
        var totalCredit = 0.0

        val sortedList = transactions.sortedWith(compareBy({ it.dateMillis }, { it.id }))

        for (tx in sortedList) {
            val isIncome = tx.type.equals("INCOME", ignoreCase = true)
            val debit = if (!isIncome) tx.amount else 0.0
            val credit = if (isIncome) tx.amount else 0.0

            totalDebit += debit
            totalCredit += credit
            runningBalance += (credit - debit)

            sheet.addCell(Label(0, currentRow, tx.dateFormatted, dataFormatCenter))
            sheet.addCell(Label(1, currentRow, if (isIncome) "INCOME" else "EXPENSE", if (isIncome) creditFormat else debitFormat))
            sheet.addCell(Label(2, currentRow, tx.partyName.ifBlank { "-" }, dataFormatLeft))
            sheet.addCell(Label(3, currentRow, tx.category.ifBlank { "General" }, dataFormatLeft))
            sheet.addCell(Label(4, currentRow, tx.subCategory.ifBlank { "General" }, dataFormatLeft))
            sheet.addCell(Label(5, currentRow, tx.siteName.ifBlank { "Site" }, dataFormatLeft))
            sheet.addCell(Label(6, currentRow, tx.description.ifBlank { "-" }, dataFormatLeft))
            sheet.addCell(Label(7, currentRow, tx.paymentMode.ifBlank { "Cash" }, dataFormatCenter))
            sheet.addCell(Number(8, currentRow, debit, if (debit > 0) debitFormat else numFormat))
            sheet.addCell(Number(9, currentRow, credit, if (credit > 0) creditFormat else numFormat))
            sheet.addCell(Number(10, currentRow, runningBalance, numFormat))

            currentRow++
        }

        // Summary Total Row
        sheet.mergeCells(0, currentRow, 7, currentRow)
        sheet.addCell(Label(0, currentRow, "TOTAL / NET CLOSING BALANCE", totalLabelFormat))
        sheet.addCell(Number(8, currentRow, totalDebit, totalNumFormat))
        sheet.addCell(Number(9, currentRow, totalCredit, totalNumFormat))
        sheet.addCell(Number(10, currentRow, runningBalance, totalNumFormat))

        // Column widths (approx character count)
        sheet.setColumnView(0, 13) // Date
        sheet.setColumnView(1, 14) // Entry Type
        sheet.setColumnView(2, 22) // Particular
        sheet.setColumnView(3, 18) // Category
        sheet.setColumnView(4, 18) // Sub Category
        sheet.setColumnView(5, 20) // Site
        sheet.setColumnView(6, 28) // Description
        sheet.setColumnView(7, 15) // Payment Mode
        sheet.setColumnView(8, 16) // Debit
        sheet.setColumnView(9, 16) // Credit
        sheet.setColumnView(10, 18) // Balance

        workbook.write()
        workbook.close()

        return file
    }

    /**
     * Generates an official REAL .xls sample template with 4 realistic construction transactions.
     */
    fun generateSampleXlsFile(context: Context): File {
        val exportDir = File(context.cacheDir, "excel_exports").apply { mkdirs() }
        val file = File(exportDir, "DayBook_Sample_Template.xls")
        if (file.exists()) file.delete()

        val settings = WorkbookSettings().apply {
            encoding = "UTF-8"
        }
        val workbook: WritableWorkbook = Workbook.createWorkbook(file, settings)
        val sheet: WritableSheet = workbook.createSheet("DayBook_Sample", 0)

        // Fonts & Formats
        val titleFont = WritableFont(WritableFont.ARIAL, 13, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.DARK_BLUE)
        val titleFormat = WritableCellFormat(titleFont).apply {
            alignment = Alignment.CENTRE
            verticalAlignment = VerticalAlignment.CENTRE
        }

        val subTitleFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.GRAY_80)
        val subTitleFormat = WritableCellFormat(subTitleFont).apply {
            alignment = Alignment.CENTRE
        }

        val headerFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.WHITE)
        val headerFormat = WritableCellFormat(headerFont).apply {
            setBackground(Colour.OCEAN_BLUE)
            alignment = Alignment.CENTRE
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.THIN, Colour.GRAY_50)
        }

        val dataFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD)
        val dataFormatLeft = WritableCellFormat(dataFont).apply {
            alignment = Alignment.LEFT
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.THIN, Colour.GRAY_25)
        }
        val dataFormatCenter = WritableCellFormat(dataFont).apply {
            alignment = Alignment.CENTRE
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.THIN, Colour.GRAY_25)
        }
        val numFormat = WritableCellFormat(dataFont, NumberFormats.FORMAT3).apply {
            alignment = Alignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.THIN, Colour.GRAY_25)
        }

        val debitFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.DARK_RED)
        val debitFormat = WritableCellFormat(debitFont, NumberFormats.FORMAT3).apply {
            alignment = Alignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.THIN, Colour.GRAY_25)
        }

        val creditFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.DARK_GREEN)
        val creditFormat = WritableCellFormat(creditFont, NumberFormats.FORMAT3).apply {
            alignment = Alignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.THIN, Colour.GRAY_25)
        }

        val totalFont = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.BLACK)
        val totalLabelFormat = WritableCellFormat(totalFont).apply {
            setBackground(Colour.GRAY_25)
            alignment = Alignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.MEDIUM, Colour.BLACK)
        }
        val totalNumFormat = WritableCellFormat(totalFont, NumberFormats.FORMAT3).apply {
            setBackground(Colour.GRAY_25)
            alignment = Alignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTRE
            setBorder(Border.ALL, BorderLineStyle.MEDIUM, Colour.BLACK)
        }

        sheet.mergeCells(0, 0, 10, 0)
        sheet.addCell(Label(0, 0, "DAY BOOK OFFICIAL SAMPLE TEMPLATE", titleFormat))

        sheet.mergeCells(0, 1, 10, 1)
        sheet.addCell(Label(0, 1, "Fill in your records following this column layout and import directly into Day Book", subTitleFormat))

        val headers = arrayOf(
            "Date", "Entry Type", "Party / Vendor Name", "Category", "Sub Category",
            "Site", "Description", "Payment Mode", "Debit (₹)", "Credit (₹)", "Balance (₹)"
        )

        for (i in headers.indices) {
            sheet.addCell(Label(i, 3, headers[i], headerFormat))
        }

        val sampleRows = listOf(
            SampleRow("2026-08-01", "INCOME", "Apex Realty Ltd", "Client Advance", "Advance Payment", "Metro City Tower", "Initial mobilisation advance", "Bank Transfer", 0.0, 500000.0, 500000.0),
            SampleRow("2026-08-02", "EXPENSE", "UltraTech Cement", "Material", "Cement & Steel", "Metro City Tower", "100 bags 53 grade cement", "UPI", 42000.0, 0.0, 458000.0),
            SampleRow("2026-08-03", "EXPENSE", "Shramik Labor Union", "Labor", "Daily Wages", "Metro City Tower", "Daily wages for 10 masons & 15 helpers", "Cash", 18500.0, 0.0, 439500.0),
            SampleRow("2026-08-04", "EXPENSE", "Metro Fuel Station", "Equipment / Machine", "Fuel / Diesel", "Metro City Tower", "Diesel 75L for JCB excavator", "UPI", 6750.0, 0.0, 432750.0)
        )

        var r = 4
        var totDeb = 0.0
        var totCred = 0.0

        for (row in sampleRows) {
            totDeb += row.debit
            totCred += row.credit

            sheet.addCell(Label(0, r, row.date, dataFormatCenter))
            sheet.addCell(Label(1, r, row.type, if (row.type == "INCOME") creditFormat else debitFormat))
            sheet.addCell(Label(2, r, row.particular, dataFormatLeft))
            sheet.addCell(Label(3, r, row.category, dataFormatLeft))
            sheet.addCell(Label(4, r, row.subCategory, dataFormatLeft))
            sheet.addCell(Label(5, r, row.site, dataFormatLeft))
            sheet.addCell(Label(6, r, row.desc, dataFormatLeft))
            sheet.addCell(Label(7, r, row.mode, dataFormatCenter))
            sheet.addCell(Number(8, r, row.debit, if (row.debit > 0) debitFormat else numFormat))
            sheet.addCell(Number(9, r, row.credit, if (row.credit > 0) creditFormat else numFormat))
            sheet.addCell(Number(10, r, row.balance, numFormat))
            r++
        }

        sheet.mergeCells(0, r, 7, r)
        sheet.addCell(Label(0, r, "TOTAL / NET BALANCE", totalLabelFormat))
        sheet.addCell(Number(8, r, totDeb, totalNumFormat))
        sheet.addCell(Number(9, r, totCred, totalNumFormat))
        sheet.addCell(Number(10, r, totCred - totDeb, totalNumFormat))

        sheet.setColumnView(0, 13)
        sheet.setColumnView(1, 14)
        sheet.setColumnView(2, 22)
        sheet.setColumnView(3, 18)
        sheet.setColumnView(4, 18)
        sheet.setColumnView(5, 20)
        sheet.setColumnView(6, 28)
        sheet.setColumnView(7, 15)
        sheet.setColumnView(8, 16)
        sheet.setColumnView(9, 16)
        sheet.setColumnView(10, 18)

        workbook.write()
        workbook.close()

        return file
    }

    private data class SampleRow(
        val date: String,
        val type: String,
        val particular: String,
        val category: String,
        val subCategory: String,
        val site: String,
        val desc: String,
        val mode: String,
        val debit: Double,
        val credit: Double,
        val balance: Double
    )

    /**
     * Parses an uploaded Excel (.xls or .xlsx) or tabular file from Uri.
     */
    fun parseExcelFile(
        context: Context,
        uri: Uri,
        defaultSiteId: Long,
        defaultSiteName: String,
        userId: Long,
        userName: String,
        userMobile: String
    ): ImportResult {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Could not read file data.")

        if (rawBytes.isEmpty()) {
            throw IllegalArgumentException("The selected Excel file is empty.")
        }

        val rawRows = mutableListOf<List<String>>()

        // Strategy 1: Try binary .xls using JExcelAPI
        var parsedAsJxl = false
        try {
            val bis = ByteArrayInputStream(rawBytes)
            val settings = WorkbookSettings().apply {
                encoding = "UTF-8"
                setSuppressWarnings(true)
            }
            val workbook = Workbook.getWorkbook(bis, settings)
            if (workbook.numberOfSheets > 0) {
                val sheet: Sheet = workbook.getSheet(0)
                for (rowIndex in 0 until sheet.rows) {
                    val rowCells = mutableListOf<String>()
                    for (colIndex in 0 until sheet.columns) {
                        val cell: Cell = sheet.getCell(colIndex, rowIndex)
                        rowCells.add(cell.contents?.trim() ?: "")
                    }
                    if (rowCells.any { it.isNotBlank() }) {
                        rawRows.add(rowCells)
                    }
                }
                parsedAsJxl = true
            }
            workbook.close()
        } catch (e: Exception) {
            // Not a binary XLS file, fallback to next parser
            parsedAsJxl = false
        }

        // Strategy 2: If not jxl, check if it's a zipped .xlsx OOXML file
        if (!parsedAsJxl && rawBytes.size > 4 && rawBytes[0] == 0x50.toByte() && rawBytes[1] == 0x4B.toByte()) {
            try {
                val xlsxRows = parseXlsxFromBytes(rawBytes)
                if (xlsxRows.isNotEmpty()) {
                    rawRows.addAll(xlsxRows)
                    parsedAsJxl = true
                }
            } catch (e: Exception) {
                parsedAsJxl = false
            }
        }

        // Strategy 3: Text / HTML / CSV fallback
        if (!parsedAsJxl) {
            val textContent = String(rawBytes, Charsets.UTF_8)
            if (textContent.contains("<tr", ignoreCase = true)) {
                val rowRegex = "<tr[^>]*>(.*?)</tr>".toRegex(RegexOption.IGNORE_CASE)
                val cellRegex = "<t[dh][^>]*>(.*?)</t[dh]>".toRegex(RegexOption.IGNORE_CASE)
                val rows = rowRegex.findAll(textContent).toList()
                for (r in rows) {
                    val rowHtml = r.groupValues[1]
                    val cells = cellRegex.findAll(rowHtml).map { it.groupValues[1].replace("<[^>]*>".toRegex(), "").trim() }.toList()
                    if (cells.isNotEmpty()) rawRows.add(cells)
                }
            } else {
                val lines = textContent.lines().filter { it.isNotBlank() }
                for (l in lines) {
                    rawRows.add(parseCsvRow(l))
                }
            }
        }

        if (rawRows.isEmpty()) {
            return ImportResult(emptyList(), listOf("No data rows found in the Excel file."), 0, 0.0, 0.0)
        }

        // Find Header Row Index
        var headerRowIndex = -1
        for (i in 0 until minOf(8, rawRows.size)) {
            val row = rawRows[i].map { it.lowercase() }
            if (row.any { it.contains("date") } && (row.any { it.contains("particular") } || row.any { it.contains("amount") } || row.any { it.contains("debit") } || row.any { it.contains("category") } || row.any { it.contains("type") })) {
                headerRowIndex = i
                break
            }
        }

        val headerRow = if (headerRowIndex >= 0) rawRows[headerRowIndex] else emptyList()
        val dataRows = if (headerRowIndex >= 0) rawRows.subList(headerRowIndex + 1, rawRows.size) else rawRows

        // Map column indices
        var colDate = -1
        var colType = -1
        var colParticular = -1
        var colCategory = -1
        var colSubCategory = -1
        var colSite = -1
        var colDescription = -1
        var colMode = -1
        var colDebit = -1
        var colCredit = -1
        var colAmount = -1
        var colRemarks = -1

        for ((idx, name) in headerRow.withIndex()) {
            val n = name.lowercase().trim()
            when {
                n.contains("date") && colDate == -1 -> colDate = idx
                (n == "type" || n.contains("entry type")) && colType == -1 -> colType = idx
                (n.contains("particular") || n.contains("party") || n.contains("vendor")) && colParticular == -1 -> colParticular = idx
                (n == "sub category" || n.contains("sub-category") || n.contains("subcategory")) && colSubCategory == -1 -> colSubCategory = idx
                n.contains("category") && colCategory == -1 -> colCategory = idx
                n.contains("site") && colSite == -1 -> colSite = idx
                (n.contains("description") || n.contains("desc") || n.contains("details")) && colDescription == -1 -> colDescription = idx
                (n.contains("mode") || n.contains("payment")) && colMode == -1 -> colMode = idx
                n.contains("debit") && colDebit == -1 -> colDebit = idx
                n.contains("credit") && colCredit == -1 -> colCredit = idx
                n.contains("amount") && colAmount == -1 -> colAmount = idx
                n.contains("remark") && colRemarks == -1 -> colRemarks = idx
            }
        }

        // Defaults if headers weren't found or mapped standard
        if (colDate == -1) colDate = 0
        if (colType == -1 && colDebit == -1 && colCredit == -1) colType = 1
        if (colParticular == -1) colParticular = 2
        if (colCategory == -1) colCategory = 3
        if (colSubCategory == -1) colSubCategory = 4
        if (colSite == -1) colSite = 5
        if (colDescription == -1) colDescription = 6
        if (colMode == -1) colMode = 7
        if (colDebit == -1 && colAmount == -1) colDebit = 8
        if (colCredit == -1 && colAmount == -1) colCredit = 9

        val validEntries = mutableListOf<TransactionEntry>()
        val errors = mutableListOf<String>()
        var sumDebit = 0.0
        var sumCredit = 0.0

        for ((rowIdx, row) in dataRows.withIndex()) {
            val humanRowNum = (if (headerRowIndex >= 0) headerRowIndex + 1 else 0) + rowIdx + 1
            if (row.all { it.isBlank() }) continue

            // Skip total summary rows
            val firstCell = row.firstOrNull()?.trim()?.lowercase() ?: ""
            val secondCell = row.getOrNull(1)?.trim()?.lowercase() ?: ""
            if (firstCell.startsWith("total") || firstCell.startsWith("closing") || secondCell.startsWith("total") || firstCell.contains("net balance")) {
                continue
            }

            val dateRaw = row.getOrNull(colDate)?.trim() ?: ""
            if (dateRaw.isBlank()) {
                errors.add("Row $humanRowNum: Missing date.")
                continue
            }

            val (dateFormatted, dateMillis) = parseFlexibleDate(dateRaw)

            val typeRaw = if (colType >= 0) row.getOrNull(colType)?.trim()?.uppercase() ?: "" else ""
            val debitRaw = if (colDebit >= 0) row.getOrNull(colDebit)?.trim() else null
            val creditRaw = if (colCredit >= 0) row.getOrNull(colCredit)?.trim() else null
            val amountRaw = if (colAmount >= 0) row.getOrNull(colAmount)?.trim() else null

            val debitVal = debitRaw?.replace(",", "")?.replace("₹", "")?.toDoubleOrNull() ?: 0.0
            val creditVal = creditRaw?.replace(",", "")?.replace("₹", "")?.toDoubleOrNull() ?: 0.0
            val amountVal = amountRaw?.replace(",", "")?.replace("₹", "")?.toDoubleOrNull() ?: 0.0

            val finalType: String
            val finalAmount: Double

            when {
                debitVal > 0.0 && creditVal == 0.0 -> {
                    finalType = "EXPENSE"
                    finalAmount = debitVal
                }
                creditVal > 0.0 && debitVal == 0.0 -> {
                    finalType = "INCOME"
                    finalAmount = creditVal
                }
                amountVal > 0.0 -> {
                    finalType = if (typeRaw.contains("INC") || typeRaw.contains("CREDIT")) "INCOME" else "EXPENSE"
                    finalAmount = amountVal
                }
                typeRaw.contains("INC") || typeRaw.contains("CREDIT") -> {
                    finalType = "INCOME"
                    finalAmount = maxOf(creditVal, amountVal)
                }
                else -> {
                    finalType = "EXPENSE"
                    finalAmount = maxOf(debitVal, amountVal)
                }
            }

            if (finalAmount <= 0.0) {
                errors.add("Row $humanRowNum: Invalid or zero amount.")
                continue
            }

            val particular = if (colParticular >= 0) row.getOrNull(colParticular)?.trim() ?: "" else ""
            val category = (if (colCategory >= 0) row.getOrNull(colCategory)?.trim() else null)?.ifBlank { "General" } ?: "General"
            val subCategory = (if (colSubCategory >= 0) row.getOrNull(colSubCategory)?.trim() else null)?.ifBlank { "General" } ?: "General"
            val site = (if (colSite >= 0) row.getOrNull(colSite)?.trim() else null)?.ifBlank { defaultSiteName } ?: defaultSiteName
            val desc = if (colDescription >= 0) row.getOrNull(colDescription)?.trim() ?: "" else ""
            val mode = (if (colMode >= 0) row.getOrNull(colMode)?.trim() else null)?.ifBlank { "Cash" } ?: "Cash"
            val remarks = if (colRemarks >= 0) row.getOrNull(colRemarks)?.trim()?.ifBlank { null } else null

            if (finalType == "INCOME") {
                sumCredit += finalAmount
            } else {
                sumDebit += finalAmount
            }

            validEntries.add(
                TransactionEntry(
                    id = 0L,
                    type = finalType,
                    dateMillis = dateMillis,
                    dateFormatted = dateFormatted,
                    siteId = defaultSiteId,
                    siteName = site,
                    userId = userId,
                    userName = userName,
                    userMobile = userMobile,
                    category = category,
                    subCategory = subCategory,
                    partyName = particular,
                    description = desc,
                    amount = finalAmount,
                    paymentMode = mode,
                    receiptPhotoUri = null,
                    remarks = remarks,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        return ImportResult(
            validEntries = validEntries,
            errors = errors,
            totalRowsRead = dataRows.size,
            totalDebit = sumDebit,
            totalCredit = sumCredit
        )
    }

    private fun parseFlexibleDate(raw: String): Pair<String, Long> {
        val clean = raw.trim()
        val formats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
            SimpleDateFormat("d-M-yyyy", Locale.getDefault()),
            SimpleDateFormat("d/M/yyyy", Locale.getDefault()),
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        )

        for (fmt in formats) {
            try {
                fmt.isLenient = false
                val date = fmt.parse(clean)
                if (date != null) {
                    val standardFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                    return Pair(standardFormat, date.time)
                }
            } catch (e: Exception) {
                // continue
            }
        }

        // Try Excel Serial Date Number (e.g. 45500)
        val serialDouble = clean.toDoubleOrNull()
        if (serialDouble != null && serialDouble > 30000 && serialDouble < 60000) {
            val millis = ((serialDouble - 25569.0) * 86400.0 * 1000.0).toLong()
            val date = Date(millis)
            val standardFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            return Pair(standardFormat, millis)
        }

        val now = System.currentTimeMillis()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
        return Pair(today, now)
    }

    private fun parseCsvRow(line: String): List<String> {
        val list = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()
        for (c in line) {
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
        }
        list.add(sb.toString().trim())
        return list
    }

    /**
     * Parses OOXML (.xlsx) zipped XML format directly.
     */
    private fun parseXlsxFromBytes(bytes: ByteArray): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val sharedStrings = mutableListOf<String>()
        var sheetBytes: ByteArray? = null

        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name.equals("xl/sharedStrings.xml", ignoreCase = true)) {
                    sharedStrings.addAll(parseSharedStringsXml(zis))
                } else if (entry.name.startsWith("xl/worksheets/sheet1.xml", ignoreCase = true) || (entry.name.startsWith("xl/worksheets/sheet", ignoreCase = true) && sheetBytes == null)) {
                    sheetBytes = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (sheetBytes != null) {
            rows.addAll(parseSheetXml(sheetBytes!!, sharedStrings))
        }

        return rows
    }

    private fun parseSharedStringsXml(input: InputStream): List<String> {
        val list = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        var eventType = parser.eventType
        var currentText = StringBuilder()
        var insideT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("t", ignoreCase = true)) {
                        insideT = true
                    } else if (parser.name.equals("si", ignoreCase = true)) {
                        currentText = StringBuilder()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideT) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("t", ignoreCase = true)) {
                        insideT = false
                    } else if (parser.name.equals("si", ignoreCase = true)) {
                        list.add(currentText.toString())
                    }
                }
            }
            eventType = parser.next()
        }
        return list
    }

    private fun parseSheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var eventType = parser.eventType

        var currentRow = mutableListOf<String>()
        var currentCellType = ""
        var currentCellValue = StringBuilder()
        var insideV = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "row" -> {
                            currentRow = mutableListOf()
                        }
                        "c" -> {
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            currentCellValue = StringBuilder()
                        }
                        "v" -> {
                            insideV = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideV) {
                        currentCellValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name.lowercase()) {
                        "v" -> {
                            insideV = false
                        }
                        "c" -> {
                            val v = currentCellValue.toString().trim()
                            val resolved = if (currentCellType == "s") {
                                val idx = v.toIntOrNull()
                                if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else v
                            } else {
                                v
                            }
                            currentRow.add(resolved)
                        }
                        "row" -> {
                            if (currentRow.any { it.isNotBlank() }) {
                                rows.add(currentRow)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    /**
     * Opens Android Native Save/Share Chooser with real .xls file.
     */
    fun shareExcelFile(context: Context, file: File, chooserTitle: String = "Share / Save Day Book Excel") {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.ms-excel"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
