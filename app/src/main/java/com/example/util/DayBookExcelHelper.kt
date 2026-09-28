package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Xml
import androidx.core.content.FileProvider
import com.example.data.model.CategoryConstants
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
import java.util.zip.ZipOutputStream

object DayBookExcelHelper {

    data class ImportResult(
        val validEntries: List<TransactionEntry>,
        val errors: List<String>,
        val totalRowsRead: Int,
        val totalDebit: Double,
        val totalCredit: Double
    )

    data class SampleRow(
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

    private val DEFAULT_ENTRY_TYPES = listOf("EXPENSE", "INCOME")
    private val DEFAULT_PAYMENT_MODES = listOf("Cash", "Bank", "UPI", "Cheque", "Online", "Other")

    fun getAllCategories(transactions: List<TransactionEntry> = emptyList()): List<String> {
        val set = linkedSetOf<String>()
        CategoryConstants.EXPENSE_CATEGORIES.forEach { set.add(it.mainCategory.trim()) }
        CategoryConstants.INCOME_CATEGORIES.forEach { set.add(it.mainCategory.trim()) }
        transactions.map { it.category.trim() }.filter { it.isNotBlank() }.forEach { set.add(it) }
        return set.filter { it.isNotBlank() }.toList()
    }

    fun getAllSubCategories(transactions: List<TransactionEntry> = emptyList()): List<String> {
        val set = linkedSetOf<String>()
        CategoryConstants.EXPENSE_CATEGORIES.forEach { cat ->
            cat.subCategories.forEach { set.add(it.trim()) }
        }
        CategoryConstants.INCOME_CATEGORIES.forEach { cat ->
            cat.subCategories.forEach { set.add(it.trim()) }
        }
        set.add("Fuel / Diesel")
        set.add("Machine Repairing")
        set.add("Machine Rent")
        set.add("Daily Wages")
        set.add("Monthly Salary")
        transactions.map { it.subCategory.trim() }
            .filter { it.isNotBlank() && it != "General" && it != "Income Entry" }
            .forEach { set.add(it) }
        return set.filter { it.isNotBlank() }.toList()
    }

    /**
     * Generates a modern Microsoft Excel (.xlsx) file with INTERACTIVE DROPDOWNS
     * for Category, Sub Category, Entry Type, and Payment Mode via Data Validation.
     */
    fun exportDayBookToXlsx(
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
            dateFromClean != null && dateToClean != null -> "DayBook_${dateFromClean}_to_${dateToClean}.xlsx"
            dateFromClean != null -> "DayBook_${dateFromClean}.xlsx"
            else -> "DayBook_${todayStr}.xlsx"
        }

        val file = File(exportDir, fileName)
        if (file.exists()) file.delete()

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

        val sortedList = transactions.sortedWith(compareBy({ it.dateMillis }, { it.id }))
        val categories = getAllCategories(transactions)
        val subCategories = getAllSubCategories(transactions)

        val rowList = mutableListOf<SampleRow>()
        var runningBal = 0.0
        var totDeb = 0.0
        var totCred = 0.0

        for (tx in sortedList) {
            val isIncome = tx.type.equals("INCOME", ignoreCase = true)
            val debit = if (!isIncome) tx.amount else 0.0
            val credit = if (isIncome) tx.amount else 0.0
            totDeb += debit
            totCred += credit
            runningBal += (credit - debit)

            rowList.add(
                SampleRow(
                    date = tx.dateFormatted,
                    type = if (isIncome) "INCOME" else "EXPENSE",
                    particular = tx.partyName.ifBlank { "-" },
                    category = tx.category.ifBlank { "General" },
                    subCategory = tx.subCategory.ifBlank { "General" },
                    site = tx.siteName.ifBlank { "Site" },
                    desc = tx.description.ifBlank { "-" },
                    mode = tx.paymentMode.ifBlank { "Cash" },
                    debit = debit,
                    credit = credit,
                    balance = runningBal
                )
            )
        }

        writeXlsxPackage(
            file = file,
            title = "DAY BOOK REGISTER REPORT",
            subtitle = periodText,
            rows = rowList,
            totalDebit = totDeb,
            totalCredit = totCred,
            closingBalance = runningBal,
            categories = categories,
            subCategories = subCategories
        )

        return file
    }

    /**
     * Backward-compatible alias that outputs the XLSX file with Category/Subcategory dropdowns.
     */
    fun exportDayBookToXls(
        context: Context,
        transactions: List<TransactionEntry>,
        fromDate: String? = null,
        toDate: String? = null,
        siteName: String? = null
    ): File {
        return exportDayBookToXlsx(context, transactions, fromDate, toDate, siteName)
    }

    /**
     * Generates an official REAL Excel sample template (.xlsx) with interactive dropdowns
     * for Category, Sub Category, Entry Type, and Payment Mode.
     */
    fun generateSampleXlsxFile(context: Context): File {
        val exportDir = File(context.cacheDir, "excel_exports").apply { mkdirs() }
        val file = File(exportDir, "DayBook_Sample_Template.xlsx")
        if (file.exists()) file.delete()

        val sampleRows = listOf(
            SampleRow("2026-08-01", "INCOME", "Apex Realty Ltd", "Head Office", "HO se received amount", "Metro City Tower", "Initial mobilisation advance", "Bank", 0.0, 500000.0, 500000.0),
            SampleRow("2026-08-02", "EXPENSE", "UltraTech Cement", "Material Purchase", "Cement", "Metro City Tower", "100 bags 53 grade cement", "UPI", 42000.0, 0.0, 458000.0),
            SampleRow("2026-08-03", "EXPENSE", "Shramik Labor Union", "Site Labour", "Daily Wages", "Metro City Tower", "Daily wages for 10 masons & 15 helpers", "Cash", 18500.0, 0.0, 439500.0),
            SampleRow("2026-08-04", "EXPENSE", "Metro Fuel Station", "Machinery & Equipment", "Fuel / Diesel", "Metro City Tower", "Diesel 75L for JCB excavator", "UPI", 6750.0, 0.0, 432750.0)
        )

        var totDeb = 0.0
        var totCred = 0.0
        sampleRows.forEach {
            totDeb += it.debit
            totCred += it.credit
        }

        val categories = getAllCategories()
        val subCategories = getAllSubCategories()

        writeXlsxPackage(
            file = file,
            title = "DAY BOOK OFFICIAL SAMPLE TEMPLATE",
            subtitle = "Fill your records below using the Category & Sub-Category Dropdown menus, then import directly into Day Book",
            rows = sampleRows,
            totalDebit = totDeb,
            totalCredit = totCred,
            closingBalance = totCred - totDeb,
            categories = categories,
            subCategories = subCategories
        )

        return file
    }

    /**
     * Backward-compatible alias for sample template.
     */
    fun generateSampleXlsFile(context: Context): File {
        return generateSampleXlsxFile(context)
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun writeXlsxPackage(
        file: File,
        title: String,
        subtitle: String,
        rows: List<SampleRow>,
        totalDebit: Double,
        totalCredit: Double,
        closingBalance: Double,
        categories: List<String>,
        subCategories: List<String>
    ) {
        val entryTypes = DEFAULT_ENTRY_TYPES
        val paymentModes = DEFAULT_PAYMENT_MODES

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 4. xl/workbook.xml
            zos.putNextEntry(ZipEntry("xl/workbook.xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="DayBook" sheetId="1" r:id="rId1"/>
    <sheet name="Categories_Master" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 5. xl/styles.xml
            zos.putNextEntry(ZipEntry("xl/styles.xml"))
            zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="1">
    <numFmt numFmtId="164" formatCode="#,##0.00"/>
  </numFmts>
  <fonts count="6">
    <font><name val="Calibri"/><sz val="10"/></font>
    <font><b/><name val="Calibri"/><sz val="13"/><color rgb="FF0D47A1"/></font>
    <font><name val="Calibri"/><sz val="9"/><color rgb="FF555555"/></font>
    <font><b/><name val="Calibri"/><sz val="10"/><color rgb="FFFFFFFF"/></font>
    <font><b/><name val="Calibri"/><sz val="10"/><color rgb="FFB71C1C"/></font>
    <font><b/><name val="Calibri"/><sz val="10"/><color rgb="FF1B5E20"/></font>
  </fonts>
  <fills count="5">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1565C0"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFE0E0E0"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF5F5F5"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="3">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFCCCCCC"/></left>
      <right style="thin"><color rgb="FFCCCCCC"/></right>
      <top style="thin"><color rgb="FFCCCCCC"/></top>
      <bottom style="thin"><color rgb="FFCCCCCC"/></bottom>
      <diagonal/>
    </border>
    <border>
      <left style="thin"><color rgb="FF000000"/></left>
      <right style="thin"><color rgb="FF000000"/></right>
      <top style="medium"><color rgb="FF000000"/></top>
      <bottom style="double"><color rgb="FF000000"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="10">
    <!-- 0: Data Left -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
    <!-- 1: Data Center -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    <!-- 2: Title Center -->
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    <!-- 3: Subtitle Center -->
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    <!-- 4: Header (Blue, White bold) -->
    <xf numFmtId="0" fontId="3" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    <!-- 5: Number Right (#,##0.00) -->
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <!-- 6: Debit Amount (Bold Red) -->
    <xf numFmtId="164" fontId="4" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <!-- 7: Credit Amount (Bold Green) -->
    <xf numFmtId="164" fontId="5" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <!-- 8: Total Label (Gray, Bold) -->
    <xf numFmtId="0" fontId="3" fillId="3" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <!-- 9: Total Number (Gray, Bold #,##0.00) -->
    <xf numFmtId="164" fontId="1" fillId="3" borderId="2" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
  </cellXfs>
</styleSheet>""".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 6. xl/worksheets/sheet2.xml (Categories_Master)
            zos.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
            val s2Builder = StringBuilder()
            s2Builder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="16" customWidth="1"/>
    <col min="2" max="2" width="28" customWidth="1"/>
    <col min="3" max="3" width="30" customWidth="1"/>
    <col min="4" max="4" width="18" customWidth="1"/>
  </cols>
  <sheetData>
    <row r="1" ht="24" customHeight="1">
      <c r="A1" s="4" t="inlineStr"><is><t>Entry Type</t></is></c>
      <c r="B1" s="4" t="inlineStr"><is><t>Category</t></is></c>
      <c r="C1" s="4" t="inlineStr"><is><t>Sub Category</t></is></c>
      <c r="D1" s="4" t="inlineStr"><is><t>Payment Mode</t></is></c>
    </row>
""")

            val maxMasterRows = maxOf(entryTypes.size, categories.size, subCategories.size, paymentModes.size)
            for (idx in 0 until maxMasterRows) {
                val rNum = idx + 2
                s2Builder.append("    <row r=\"$rNum\" ht=\"19\" customHeight=\"1\">\n")
                if (idx < entryTypes.size) {
                    s2Builder.append("      <c r=\"A$rNum\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(entryTypes[idx])}</t></is></c>\n")
                }
                if (idx < categories.size) {
                    s2Builder.append("      <c r=\"B$rNum\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(categories[idx])}</t></is></c>\n")
                }
                if (idx < subCategories.size) {
                    s2Builder.append("      <c r=\"C$rNum\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(subCategories[idx])}</t></is></c>\n")
                }
                if (idx < paymentModes.size) {
                    s2Builder.append("      <c r=\"D$rNum\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(paymentModes[idx])}</t></is></c>\n")
                }
                s2Builder.append("    </row>\n")
            }
            s2Builder.append("""  </sheetData>
</worksheet>""")
            zos.write(s2Builder.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 7. xl/worksheets/sheet1.xml (DayBook)
            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val s1Builder = StringBuilder()
            s1Builder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="14" customWidth="1"/>
    <col min="2" max="2" width="15" customWidth="1"/>
    <col min="3" max="3" width="24" customWidth="1"/>
    <col min="4" max="4" width="22" customWidth="1"/>
    <col min="5" max="5" width="22" customWidth="1"/>
    <col min="6" max="6" width="20" customWidth="1"/>
    <col min="7" max="7" width="28" customWidth="1"/>
    <col min="8" max="8" width="16" customWidth="1"/>
    <col min="9" max="9" width="17" customWidth="1"/>
    <col min="10" max="10" width="17" customWidth="1"/>
    <col min="11" max="11" width="18" customWidth="1"/>
  </cols>
  <sheetData>
    <row r="1" ht="26" customHeight="1">
      <c r="A1" s="2" t="inlineStr"><is><t>${escapeXml(title)}</t></is></c>
    </row>
    <row r="2" ht="20" customHeight="1">
      <c r="A2" s="3" t="inlineStr"><is><t>${escapeXml(subtitle)}</t></is></c>
    </row>
    <row r="3" ht="10" customHeight="1"/>
    <row r="4" ht="24" customHeight="1">
      <c r="A4" s="4" t="inlineStr"><is><t>Date</t></is></c>
      <c r="B4" s="4" t="inlineStr"><is><t>Entry Type</t></is></c>
      <c r="C4" s="4" t="inlineStr"><is><t>Party / Vendor Name</t></is></c>
      <c r="D4" s="4" t="inlineStr"><is><t>Category</t></is></c>
      <c r="E4" s="4" t="inlineStr"><is><t>Sub Category</t></is></c>
      <c r="F4" s="4" t="inlineStr"><is><t>Site</t></is></c>
      <c r="G4" s="4" t="inlineStr"><is><t>Description</t></is></c>
      <c r="H4" s="4" t="inlineStr"><is><t>Payment Mode</t></is></c>
      <c r="I4" s="4" t="inlineStr"><is><t>Debit (₹)</t></is></c>
      <c r="J4" s="4" t="inlineStr"><is><t>Credit (₹)</t></is></c>
      <c r="K4" s="4" t="inlineStr"><is><t>Balance (₹)</t></is></c>
    </row>
""")

            var curRow = 5
            for (row in rows) {
                s1Builder.append("    <row r=\"$curRow\" ht=\"20\" customHeight=\"1\">\n")
                s1Builder.append("      <c r=\"A$curRow\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(row.date)}</t></is></c>\n")
                s1Builder.append("      <c r=\"B$curRow\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(row.type)}</t></is></c>\n")
                s1Builder.append("      <c r=\"C$curRow\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(row.particular)}</t></is></c>\n")
                s1Builder.append("      <c r=\"D$curRow\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(row.category)}</t></is></c>\n")
                s1Builder.append("      <c r=\"E$curRow\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(row.subCategory)}</t></is></c>\n")
                s1Builder.append("      <c r=\"F$curRow\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(row.site)}</t></is></c>\n")
                s1Builder.append("      <c r=\"G$curRow\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(row.desc)}</t></is></c>\n")
                s1Builder.append("      <c r=\"H$curRow\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(row.mode)}</t></is></c>\n")
                s1Builder.append("      <c r=\"I$curRow\" s=\"6\"><v>${String.format(Locale.US, "%.2f", row.debit)}</v></c>\n")
                s1Builder.append("      <c r=\"J$curRow\" s=\"7\"><v>${String.format(Locale.US, "%.2f", row.credit)}</v></c>\n")
                s1Builder.append("      <c r=\"K$curRow\" s=\"5\"><v>${String.format(Locale.US, "%.2f", row.balance)}</v></c>\n")
                s1Builder.append("    </row>\n")
                curRow++
            }

            // Summary row
            val totalRowNum = curRow
            s1Builder.append("    <row r=\"$totalRowNum\" ht=\"22\" customHeight=\"1\">\n")
            s1Builder.append("      <c r=\"A$totalRowNum\" s=\"8\" t=\"inlineStr\"><is><t>TOTAL / NET CLOSING BALANCE</t></is></c>\n")
            s1Builder.append("      <c r=\"I$totalRowNum\" s=\"9\"><v>${String.format(Locale.US, "%.2f", totalDebit)}</v></c>\n")
            s1Builder.append("      <c r=\"J$totalRowNum\" s=\"9\"><v>${String.format(Locale.US, "%.2f", totalCredit)}</v></c>\n")
            s1Builder.append("      <c r=\"K$totalRowNum\" s=\"9\"><v>${String.format(Locale.US, "%.2f", closingBalance)}</v></c>\n")
            s1Builder.append("    </row>\n")

            s1Builder.append("""  </sheetData>
  <mergeCells count="3">
    <mergeCell ref="A1:K1"/>
    <mergeCell ref="A2:K2"/>
    <mergeCell ref="A$totalRowNum:H$totalRowNum"/>
  </mergeCells>
  <dataValidations count="4">
    <dataValidation type="list" allowBlank="1" showInputMessage="1" showErrorMessage="1" sqref="B5:B1000">
      <formula1>&apos;Categories_Master&apos;!${'$'}A${'$'}2:${'$'}A${'$'}${entryTypes.size + 1}</formula1>
    </dataValidation>
    <dataValidation type="list" allowBlank="1" showInputMessage="1" showErrorMessage="1" sqref="D5:D1000">
      <formula1>&apos;Categories_Master&apos;!${'$'}B${'$'}2:${'$'}B${'$'}${categories.size + 1}</formula1>
    </dataValidation>
    <dataValidation type="list" allowBlank="1" showInputMessage="1" showErrorMessage="1" sqref="E5:E1000">
      <formula1>&apos;Categories_Master&apos;!${'$'}C${'$'}2:${'$'}C${'$'}${subCategories.size + 1}</formula1>
    </dataValidation>
    <dataValidation type="list" allowBlank="1" showInputMessage="1" showErrorMessage="1" sqref="H5:H1000">
      <formula1>&apos;Categories_Master&apos;!${'$'}D${'$'}2:${'$'}D${'$'}${paymentModes.size + 1}</formula1>
    </dataValidation>
  </dataValidations>
</worksheet>""")

            zos.write(s1Builder.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
    }

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

    private fun parseColumnIndex(cellRef: String): Int {
        var col = 0
        for (char in cellRef) {
            if (char in 'A'..'Z') {
                col = col * 26 + (char - 'A' + 1)
            } else if (char in 'a'..'z') {
                col = col * 26 + (char - 'a' + 1)
            } else {
                break
            }
        }
        return if (col > 0) col - 1 else 0
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
                val entryName = entry.name.lowercase()
                if (entryName == "xl/sharedstrings.xml") {
                    val ssBytes = zis.readBytes()
                    sharedStrings.addAll(parseSharedStringsXml(ByteArrayInputStream(ssBytes)))
                } else if (entryName == "xl/worksheets/sheet1.xml" || (entryName.startsWith("xl/worksheets/sheet") && sheetBytes == null)) {
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

        var currentRowMap = mutableMapOf<Int, String>()
        var currentColIdx = 0
        var currentCellType = ""
        var currentCellValue = StringBuilder()
        var insideV = false
        var insideT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "row" -> {
                            currentRowMap = mutableMapOf()
                            currentColIdx = 0
                        }
                        "c" -> {
                            val rAttr = parser.getAttributeValue(null, "r")
                            if (!rAttr.isNullOrBlank()) {
                                currentColIdx = parseColumnIndex(rAttr)
                            }
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            currentCellValue = StringBuilder()
                        }
                        "v" -> insideV = true
                        "t" -> insideT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideV || insideT) {
                        currentCellValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name.lowercase()) {
                        "v" -> insideV = false
                        "t" -> insideT = false
                        "c" -> {
                            val raw = currentCellValue.toString().trim()
                            val resolved = when (currentCellType) {
                                "s" -> {
                                    val idx = raw.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else raw
                                }
                                else -> raw
                            }
                            currentRowMap[currentColIdx] = resolved
                            currentColIdx++
                        }
                        "row" -> {
                            if (currentRowMap.isNotEmpty()) {
                                val maxCol = (currentRowMap.keys.maxOrNull() ?: -1)
                                if (maxCol >= 0) {
                                    val rowList = MutableList(maxCol + 1) { "" }
                                    for ((col, text) in currentRowMap) {
                                        rowList[col] = text
                                    }
                                    if (rowList.any { it.isNotBlank() }) {
                                        rows.add(rowList)
                                    }
                                }
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
     * Opens Android Native Save/Share Chooser with real Excel file (.xlsx or .xls).
     */
    fun shareExcelFile(context: Context, file: File, chooserTitle: String = "Share / Save Day Book Excel") {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val mimeType = if (file.name.endsWith(".xlsx", ignoreCase = true)) {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        } else {
            "application/vnd.ms-excel"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
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
