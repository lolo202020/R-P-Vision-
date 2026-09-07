package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Android PDF Generator, Viewer, and Sharer for Daily Attendance & Wage Reports.
 * Uses Android PdfDocument (API 19+) for high-resolution, vector-crisp multi-page PDF rendering.
 */
object AttendancePdfHelper {

    private val indianCurrencyFormat = DecimalFormat("##,##,##0.00", DecimalFormatSymbols(Locale("en", "IN")))

    fun formatCurrency(amount: Double): String {
        return "₹ " + indianCurrencyFormat.format(amount)
    }

    data class AttendanceReportItem(
        val staffId: Long,
        val staffName: String,
        val designation: String,
        val siteName: String,
        val status: String, // PRESENT, HALF_DAY, ABSENT, LEAVE
        val wageAmount: Double,
        val overtimeHours: Double = 0.0,
        val remarks: String = ""
    )

    /**
     * Builds report items combining staff directory with marked attendance records for a specific date.
     */
    fun buildReportItems(
        staffList: List<StaffMember>,
        attendanceList: List<StaffAttendance>,
        dateFormatted: String,
        siteId: Long?
    ): List<AttendanceReportItem> {
        val filteredStaff = if (siteId == null) staffList else staffList.filter { it.siteId == siteId }
        val attendanceMap = attendanceList
            .filter { it.dateFormatted == dateFormatted && (siteId == null || it.siteId == siteId) }
            .associateBy { it.staffId }

        return filteredStaff.map { staff ->
            val att = attendanceMap[staff.id]
            val status = att?.status ?: "PRESENT"
            val baseWage = staff.dailyWage
            val defaultWage = when (status) {
                "PRESENT" -> if (staff.paymentType == "MONTHLY") baseWage / 30.0 else baseWage
                "HALF_DAY" -> (if (staff.paymentType == "MONTHLY") baseWage / 30.0 else baseWage) / 2.0
                "LEAVE" -> (if (staff.paymentType == "MONTHLY") baseWage / 30.0 else baseWage) // Paid leave
                else -> 0.0 // ABSENT
            }
            val wage = att?.wageAmount ?: defaultWage
            val remarks = att?.remarks ?: (if (status == "HALF_DAY") "Half day duty" else status.lowercase().replaceFirstChar { it.uppercase() })

            AttendanceReportItem(
                staffId = staff.id,
                staffName = staff.name,
                designation = staff.designation,
                siteName = staff.siteName.ifBlank { "Main Site" },
                status = status,
                wageAmount = wage,
                overtimeHours = att?.overtimeHours ?: 0.0,
                remarks = remarks
            )
        }
    }

    /**
     * Generates a native multi-page vector PDF for the daily attendance and wage report.
     */
    fun generateAttendancePdf(
        context: Context,
        reportItems: List<AttendanceReportItem>,
        dateFormatted: String,
        siteName: String,
        contractorOrManager: String
    ): File {
        val totalStaff = reportItems.size
        val presentCount = reportItems.count { it.status.equals("PRESENT", ignoreCase = true) }
        val halfDayCount = reportItems.count { it.status.equals("HALF_DAY", ignoreCase = true) }
        val absentCount = reportItems.count { it.status.equals("ABSENT", ignoreCase = true) }
        val leaveCount = reportItems.count { it.status.equals("LEAVE", ignoreCase = true) }
        val totalWageAmount = reportItems.sumOf { it.wageAmount }

        val generatedAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        // Standard A4 dimensions in PostScript points: 595 x 842
        val pageWidth = 595
        val pageHeight = 842
        val margin = 32f

        val pdfDocument = PdfDocument()

        // Core Paints
        val primaryPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(18, 53, 91) // Deep Corporate Navy
        }
        val darkTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(33, 33, 33)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(33, 33, 33)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(18, 53, 91)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(100, 116, 139)
            textSize = 9f
        }
        val linePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        // Status Badge Text Paints
        val presentBadgePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(22, 101, 52) // Green
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val halfDayBadgePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(180, 83, 9) // Amber/Orange
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val absentBadgePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(185, 28, 28) // Red
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val leaveBadgePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(29, 78, 216) // Blue
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Table Column Specifications (Width total: 595 - 64 = 531)
        // Columns: S.No (32), Staff Name & Role (135), Site (95), Status (65), Wage (₹) (75), Remarks (129)
        val colWidths = floatArrayOf(32f, 135f, 95f, 65f, 75f, 129f)
        val colTitles = arrayOf("S.No", "Staff Name & Role", "Site", "Status", "Wage (₹)", "Remarks")

        val contentWidth = pageWidth - (margin * 2)

        // Pagination calculations
        val rowHeight = 24f
        val tableHeaderHeight = 22f
        val firstPageHeaderHeight = 185f
        val followPageHeaderHeight = 65f
        val footerHeight = 35f

        val firstPageAvailableHeight = pageHeight - firstPageHeaderHeight - footerHeight
        val followPageAvailableHeight = pageHeight - followPageHeaderHeight - footerHeight

        val firstPageRows = (firstPageAvailableHeight / rowHeight).toInt().coerceAtLeast(1)
        val followPageRows = (followPageAvailableHeight / rowHeight).toInt().coerceAtLeast(1)

        val totalRows = reportItems.size
        val totalPages = if (totalRows <= firstPageRows) {
            1
        } else {
            1 + Math.ceil((totalRows - firstPageRows).toDouble() / followPageRows).toInt()
        }

        var currentItemIndex = 0

        for (pageNumber in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var currentY = margin

            if (pageNumber == 1) {
                // --- 1. BRANDING & REPORT HEADER ---
                // Accent Bar
                val accentBar = RectF(margin, currentY, pageWidth - margin, currentY + 4f)
                canvas.drawRoundRect(accentBar, 2f, 2f, primaryPaint)
                currentY += 16f

                // Project & Company Name
                val projectDisplayName = if (siteName.isNotBlank() && siteName != "All Sites") siteName else "Metro City Tower"
                canvas.drawText("$projectDisplayName • RPVC Construction", margin, currentY + 12f, headerTitlePaint)

                // Report Title Label on top right
                val reportLabelPaint = Paint(headerTitlePaint).apply {
                    textSize = 10.5f
                    color = Color.rgb(18, 53, 91)
                }
                val reportLabel = "DAILY ATTENDANCE & WAGE REPORT"
                val labelWidth = reportLabelPaint.measureText(reportLabel)
                canvas.drawText(reportLabel, pageWidth - margin - labelWidth, currentY + 10f, reportLabelPaint)
                currentY += 24f

                // Contractor/Manager & Date info
                val managerDisplay = if (contractorOrManager.isNotBlank()) contractorOrManager else "R P V C - Ramesh Kumar"
                canvas.drawText("Manager/Contractor: $managerDisplay  •  Site: $siteName", margin, currentY + 4f, darkTextPaint)
                val dateStampText = "Date: $dateFormatted  |  $generatedAt"
                val dateStampWidth = subTitlePaint.measureText(dateStampText)
                canvas.drawText(dateStampText, pageWidth - margin - dateStampWidth, currentY + 4f, subTitlePaint)
                currentY += 14f

                // Divider line
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
                currentY += 12f

                // --- 2. SUMMARY BADGES & METRICS SECTION ---
                val numCards = 5
                val gap = 6f
                val cardWidth = (contentWidth - (gap * (numCards - 1))) / numCards.toFloat()
                val cardHeight = 42f

                // Card 1: Total Staff
                drawMetricBadge(
                    canvas = canvas,
                    x = margin,
                    y = currentY,
                    width = cardWidth,
                    height = cardHeight,
                    title = "TOTAL STAFF",
                    value = totalStaff.toString(),
                    bgColor = Color.rgb(241, 245, 249),
                    borderColor = Color.rgb(203, 213, 225),
                    textColor = Color.rgb(30, 41, 59)
                )

                // Card 2: Present
                drawMetricBadge(
                    canvas = canvas,
                    x = margin + (cardWidth + gap),
                    y = currentY,
                    width = cardWidth,
                    height = cardHeight,
                    title = "PRESENT",
                    value = presentCount.toString(),
                    bgColor = Color.rgb(240, 253, 244),
                    borderColor = Color.rgb(187, 247, 208),
                    textColor = Color.rgb(22, 101, 52)
                )

                // Card 3: Half Day
                drawMetricBadge(
                    canvas = canvas,
                    x = margin + (cardWidth + gap) * 2,
                    y = currentY,
                    width = cardWidth,
                    height = cardHeight,
                    title = "HALF DAY",
                    value = halfDayCount.toString(),
                    bgColor = Color.rgb(254, 243, 199),
                    borderColor = Color.rgb(253, 230, 138),
                    textColor = Color.rgb(180, 83, 9)
                )

                // Card 4: Absent / Leave
                drawMetricBadge(
                    canvas = canvas,
                    x = margin + (cardWidth + gap) * 3,
                    y = currentY,
                    width = cardWidth,
                    height = cardHeight,
                    title = "ABSENT / LEAVE",
                    value = "$absentCount / $leaveCount",
                    bgColor = Color.rgb(254, 242, 242),
                    borderColor = Color.rgb(254, 202, 202),
                    textColor = Color.rgb(185, 28, 28)
                )

                // Card 5: Total Wages
                drawMetricBadge(
                    canvas = canvas,
                    x = margin + (cardWidth + gap) * 4,
                    y = currentY,
                    width = cardWidth,
                    height = cardHeight,
                    title = "TOTAL WAGES",
                    value = formatCurrency(totalWageAmount),
                    bgColor = Color.rgb(239, 246, 255),
                    borderColor = Color.rgb(191, 219, 254),
                    textColor = Color.rgb(29, 78, 216)
                )

                currentY += cardHeight + 14f

                val rosterInfo = "Showing $totalStaff Staff Attendance Records for $dateFormatted"
                canvas.drawText(rosterInfo, margin, currentY + 6f, boldTextPaint)
                currentY += 14f

            } else {
                // Subsequent Page Compact Header
                canvas.drawText("$siteName • Daily Attendance & Wage Report (Contd.)", margin, currentY + 12f, boldTextPaint)
                val pageIndicator = "Date: $dateFormatted"
                val pWidth = subTitlePaint.measureText(pageIndicator)
                canvas.drawText(pageIndicator, pageWidth - margin - pWidth, currentY + 12f, subTitlePaint)
                currentY += 20f
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
                currentY += 10f
            }

            // --- 3. ATTENDANCE TABLE HEADER ---
            val thBgPaint = Paint().apply {
                color = Color.rgb(18, 53, 91)
                style = Paint.Style.FILL
            }
            val thTextPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val thRect = RectF(margin, currentY, pageWidth - margin, currentY + tableHeaderHeight)
            canvas.drawRoundRect(thRect, 4f, 4f, thBgPaint)

            var colX = margin
            for (i in colTitles.indices) {
                val title = colTitles[i]
                val width = colWidths[i]
                if (i == 4) {
                    // Right aligned for Wage
                    val textW = thTextPaint.measureText(title)
                    canvas.drawText(title, colX + width - textW - 6f, currentY + 14f, thTextPaint)
                } else {
                    canvas.drawText(title, colX + 6f, currentY + 14f, thTextPaint)
                }
                colX += width
            }
            currentY += tableHeaderHeight + 2f

            // --- 4. ATTENDANCE TABLE ROWS ---
            val rowsForThisPage = if (pageNumber == 1) firstPageRows else followPageRows
            var rowsPrinted = 0

            if (reportItems.isEmpty()) {
                // Empty State
                val emptyRect = RectF(margin, currentY + 10f, pageWidth - margin, currentY + 70f)
                val emptyBgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
                canvas.drawRoundRect(emptyRect, 8f, 8f, emptyBgPaint)
                val emptyBorderPaint = Paint().apply {
                    color = Color.rgb(203, 213, 225)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(emptyRect, 8f, 8f, emptyBorderPaint)
                val emptyMsg = "No staff members or attendance records found for this date & site."
                val emptyPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.rgb(100, 116, 139)
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val msgW = emptyPaint.measureText(emptyMsg)
                canvas.drawText(emptyMsg, (pageWidth - msgW) / 2f, currentY + 45f, emptyPaint)
                currentY += 80f
            } else {
                val zebraPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
                val rowLinePaint = Paint().apply {
                    color = Color.rgb(241, 245, 249)
                    strokeWidth = 0.8f
                }
                val cellTextPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.rgb(30, 41, 59)
                    textSize = 8.5f
                }
                val boldCellPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.rgb(15, 23, 42)
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val wagePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.rgb(18, 53, 91)
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                while (currentItemIndex < reportItems.size && rowsPrinted < rowsForThisPage) {
                    val item = reportItems[currentItemIndex]

                    // Alternate row background
                    if (currentItemIndex % 2 == 1) {
                        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, zebraPaint)
                    }
                    canvas.drawLine(margin, currentY + rowHeight, pageWidth - margin, currentY + rowHeight, rowLinePaint)

                    var cx = margin

                    // 1. S.No
                    val snoText = "${currentItemIndex + 1}"
                    canvas.drawText(snoText, cx + 4f, currentY + 15f, cellTextPaint)
                    cx += colWidths[0]

                    // 2. Staff Name & Role (e.g. Santosh (Carpenter))
                    val staffNameAndRole = if (item.designation.isNotBlank()) "${item.staffName} (${item.designation})" else item.staffName
                    val truncatedName = truncateText(staffNameAndRole, boldCellPaint, colWidths[1] - 8f)
                    canvas.drawText(truncatedName, cx + 4f, currentY + 15f, boldCellPaint)
                    cx += colWidths[1]

                    // 3. Site Name
                    val truncatedSite = truncateText(item.siteName, cellTextPaint, colWidths[2] - 8f)
                    canvas.drawText(truncatedSite, cx + 4f, currentY + 15f, cellTextPaint)
                    cx += colWidths[2]

                    // 4. Status Badge
                    val statusText = item.status.uppercase()
                    val (statusPaint, badgeBgColor) = when (statusText) {
                        "PRESENT" -> Pair(presentBadgePaint, Color.rgb(220, 252, 231))
                        "HALF_DAY" -> Pair(halfDayBadgePaint, Color.rgb(254, 243, 199))
                        "ABSENT" -> Pair(absentBadgePaint, Color.rgb(254, 226, 226))
                        else -> Pair(leaveBadgePaint, Color.rgb(219, 234, 254)) // LEAVE
                    }

                    val badgeW = statusPaint.measureText(statusText) + 8f
                    val badgeRect = RectF(cx + 2f, currentY + 3f, cx + 2f + badgeW, currentY + 19f)
                    val badgeBgPaint = Paint().apply { color = badgeBgColor; style = Paint.Style.FILL }
                    canvas.drawRoundRect(badgeRect, 4f, 4f, badgeBgPaint)
                    canvas.drawText(statusText, cx + 6f, currentY + 15f, statusPaint)
                    cx += colWidths[3]

                    // 5. Wage Amount (₹)
                    val wageStr = formatCurrency(item.wageAmount)
                    val wageW = wagePaint.measureText(wageStr)
                    canvas.drawText(wageStr, cx + colWidths[4] - wageW - 4f, currentY + 15f, wagePaint)
                    cx += colWidths[4]

                    // 6. Remarks
                    val truncatedRemarks = truncateText(item.remarks.ifBlank { "-" }, cellTextPaint, colWidths[5] - 8f)
                    canvas.drawText(truncatedRemarks, cx + 4f, currentY + 15f, cellTextPaint)

                    currentY += rowHeight
                    rowsPrinted++
                    currentItemIndex++
                }

                // If on final page, draw bottom totals row
                if (pageNumber == totalPages && reportItems.isNotEmpty()) {
                    val totalRowBg = Paint().apply { color = Color.rgb(241, 245, 249) }
                    canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight + 4f, totalRowBg)
                    canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
                    canvas.drawLine(margin, currentY + rowHeight + 4f, pageWidth - margin, currentY + rowHeight + 4f, primaryPaint)

                    canvas.drawText("TOTAL DAILY WAGES", margin + 6f, currentY + 16f, boldTextPaint)

                    // Total Wages Amount
                    val totalWageStr = formatCurrency(totalWageAmount)
                    val totalWageW = wagePaint.measureText(totalWageStr)
                    val totalWageX = margin + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3]
                    canvas.drawText(totalWageStr, totalWageX + colWidths[4] - totalWageW - 4f, currentY + 16f, wagePaint)
                }
            }

            // --- 5. PAGE FOOTER ---
            val footerY = pageHeight - footerHeight + 10f
            canvas.drawLine(margin, footerY - 8f, pageWidth - margin, footerY - 8f, linePaint)

            val pageStr = "Page $pageNumber of $totalPages"
            val pageW = subTitlePaint.measureText(pageStr)
            canvas.drawText(pageStr, pageWidth - margin - pageW, footerY + 8f, subTitlePaint)

            canvas.drawText("RPVC Construction ERP • Daily Attendance & Wage Record", margin, footerY + 8f, subTitlePaint)

            pdfDocument.finishPage(page)
        }

        // Save PDF to App Documents / Cache Directory
        val reportsDir = File(context.cacheDir, "attendance_pdf_reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedSite = siteName.replace(" ", "_").replace("/", "_").take(15)
        val file = File(reportsDir, "Attendance_Report_${sanitizedSite}_${dateFormatted}_$timeStamp.pdf")

        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return file
    }

    private fun drawMetricBadge(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        bgColor: Int,
        borderColor: Int,
        textColor: Int
    ) {
        val rect = RectF(x, y, x + width, y + height)

        val bgPaint = Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        val borderPaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(100, 116, 139)
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, x + 6f, y + 13f, titlePaint)

        val valPaint = Paint().apply {
            isAntiAlias = true
            color = textColor
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(value, x + 6f, y + 31f, valPaint)
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length - 1
        while (end > 0 && paint.measureText(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end) + "..." else text
    }

    /**
     * Opens the Attendance PDF file directly in any installed native Android PDF viewer.
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
                "No PDF viewer app is installed. Please install a PDF viewer or use Share.",
                Toast.LENGTH_LONG
            ).show()
            sharePdfFile(context, file, chooserTitle = "Share Attendance & Wage PDF")
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            sharePdfFile(context, file, chooserTitle = "Share Attendance & Wage PDF")
        }
    }

    /**
     * Shares the generated Attendance PDF file via standard Android share sheet.
     */
    fun sharePdfFile(context: Context, file: File, chooserTitle: String = "Share Attendance & Wage Report") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                putExtra(Intent.EXTRA_TEXT, "Daily Attendance & Wage Statement (${file.name}) from RPVC Construction.")
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
}
