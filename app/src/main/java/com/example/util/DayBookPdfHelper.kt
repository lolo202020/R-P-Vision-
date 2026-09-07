package com.example.util

import android.content.ActivityNotFoundException
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
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntry
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Android PDF Generator, Viewer, and Sharer for Day Book statements.
 * Uses Android PdfDocument (API 19+) for high-resolution, vector-crisp multi-page PDF rendering.
 */
object DayBookPdfHelper {

    private val indianCurrencyFormat = DecimalFormat("##,##,##0.00", DecimalFormatSymbols(Locale("en", "IN")))

    fun formatCurrency(amount: Double): String {
        return "₹ " + indianCurrencyFormat.format(amount)
    }

    data class PdfExportResult(
        val file: File,
        val totalTransactions: Int,
        val totalIncome: Double,
        val totalExpense: Double,
        val netBalance: Double,
        val fromDate: String,
        val toDate: String,
        val siteName: String
    )

    /**
     * Generates a native PDF document for the provided transactions list and saves it to app storage.
     */
    fun generateDayBookPdf(
        context: Context,
        transactions: List<TransactionEntry>,
        fromDate: String?,
        toDate: String?,
        siteFilterName: String? = null
    ): File {
        val totalIncome = transactions.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        val periodStart = if (!fromDate.isNullOrBlank()) fromDate else (transactions.minOfOrNull { it.dateFormatted } ?: "All time")
        val periodEnd = if (!toDate.isNullOrBlank()) toDate else (transactions.maxOfOrNull { it.dateFormatted } ?: "All time")
        val activeSiteName = if (!siteFilterName.isNullOrBlank() && siteFilterName != "All") siteFilterName else "All Construction Sites"
        val generatedAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        // Standard A4 dimensions in PostScript points: 595 x 842
        val pageWidth = 595
        val pageHeight = 842
        val margin = 32f

        val pdfDocument = PdfDocument()

        // Paints
        val primaryPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(18, 53, 91) // Deep Blue
        }
        val darkTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(33, 33, 33)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(33, 33, 33)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(18, 53, 91)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(100, 116, 139)
            textSize = 9f
        }
        val incomePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(22, 101, 52) // Green
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val expensePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(153, 27, 27) // Red
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        // Table Column Specifications (Width total: 595 - 64 = 531)
        // Columns: Date (52), Type (42), Particular/Party (105), Category (80), Site (70), Mode (42), Debit (65), Credit (65)
        val colWidths = floatArrayOf(52f, 44f, 105f, 85f, 75f, 40f, 65f, 65f)
        val colTitles = arrayOf("Date", "Type", "Particular / Party", "Category", "Site", "Mode", "Debit (₹)", "Credit (₹)")

        val contentWidth = pageWidth - (margin * 2)

        // Pagination calculations
        val rowHeight = 24f
        val tableHeaderHeight = 22f
        val firstPageHeaderHeight = 175f
        val followPageHeaderHeight = 65f
        val footerHeight = 35f

        val firstPageAvailableHeight = pageHeight - firstPageHeaderHeight - footerHeight
        val followPageAvailableHeight = pageHeight - followPageHeaderHeight - footerHeight

        val firstPageRows = (firstPageAvailableHeight / rowHeight).toInt().coerceAtLeast(1)
        val followPageRows = (followPageAvailableHeight / rowHeight).toInt().coerceAtLeast(1)

        val totalRows = transactions.size
        val totalPages = if (totalRows <= firstPageRows) {
            1
        } else {
            1 + Math.ceil((totalRows - firstPageRows).toDouble() / followPageRows).toInt()
        }

        var currentTxIndex = 0

        for (pageNumber in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var currentY = margin

            if (pageNumber == 1) {
                // --- FIRST PAGE BRANDING & HEADER ---
                // Logo / Top Accent Bar
                val accentBar = RectF(margin, currentY, pageWidth - margin, currentY + 4f)
                canvas.drawRoundRect(accentBar, 2f, 2f, primaryPaint)
                currentY += 16f

                // Company / App Title
                canvas.drawText("RPVC Construction & Infrastructure", margin, currentY + 12f, headerTitlePaint)
                val daybookTitlePaint = Paint(headerTitlePaint).apply {
                    textSize = 11f
                    color = Color.rgb(18, 53, 91)
                }
                val reportLabel = "DAY BOOK FINANCIAL REGISTER"
                val labelWidth = daybookTitlePaint.measureText(reportLabel)
                canvas.drawText(reportLabel, pageWidth - margin - labelWidth, currentY + 10f, daybookTitlePaint)
                currentY += 26f

                // Subtitle metadata
                canvas.drawText("Site: $activeSiteName  •  Period: $periodStart to $periodEnd", margin, currentY + 4f, darkTextPaint)
                val genText = "Generated on: $generatedAt"
                val genWidth = subTitlePaint.measureText(genText)
                canvas.drawText(genText, pageWidth - margin - genWidth, currentY + 4f, subTitlePaint)
                currentY += 14f

                // Divider
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
                currentY += 12f

                // --- FINANCIAL SUMMARY CARDS ---
                val cardWidth = (contentWidth - 16f) / 3f
                val cardHeight = 44f

                // Card 1: Total Income
                drawSummaryCard(
                    canvas = canvas,
                    x = margin,
                    y = currentY,
                    width = cardWidth,
                    height = cardHeight,
                    title = "TOTAL INCOME (CREDIT)",
                    value = formatCurrency(totalIncome),
                    bgColor = Color.rgb(240, 253, 244),
                    borderColor = Color.rgb(187, 247, 208),
                    textColor = Color.rgb(22, 101, 52)
                )

                // Card 2: Total Expense
                drawSummaryCard(
                    canvas = canvas,
                    x = margin + cardWidth + 8f,
                    y = currentY,
                    width = cardWidth,
                    height = cardHeight,
                    title = "TOTAL EXPENSE (DEBIT)",
                    value = formatCurrency(totalExpense),
                    bgColor = Color.rgb(254, 242, 242),
                    borderColor = Color.rgb(254, 202, 202),
                    textColor = Color.rgb(153, 27, 27)
                )

                // Card 3: Net Balance
                val balanceColor = if (netBalance >= 0) Color.rgb(29, 78, 216) else Color.rgb(185, 28, 28)
                val balanceBg = if (netBalance >= 0) Color.rgb(239, 246, 255) else Color.rgb(254, 242, 242)
                val balanceBorder = if (netBalance >= 0) Color.rgb(191, 219, 254) else Color.rgb(254, 202, 202)
                drawSummaryCard(
                    canvas = canvas,
                    x = margin + (cardWidth * 2) + 16f,
                    y = currentY,
                    width = cardWidth,
                    height = cardHeight,
                    title = "NET CLOSING BALANCE",
                    value = formatCurrency(netBalance),
                    bgColor = balanceBg,
                    borderColor = balanceBorder,
                    textColor = balanceColor
                )

                currentY += cardHeight + 14f

                // Entries count indicator
                val entryInfo = "Showing ${transactions.size} entries"
                canvas.drawText(entryInfo, margin, currentY + 6f, boldTextPaint)
                currentY += 14f

            } else {
                // --- SUBSEQUENT PAGE COMPACT HEADER ---
                canvas.drawText("RPVC Construction & Infrastructure  |  Day Book (Contd.)", margin, currentY + 12f, boldTextPaint)
                val pageIndicator = "Period: $periodStart to $periodEnd"
                val pWidth = subTitlePaint.measureText(pageIndicator)
                canvas.drawText(pageIndicator, pageWidth - margin - pWidth, currentY + 12f, subTitlePaint)
                currentY += 20f
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
                currentY += 10f
            }

            // --- TABLE HEADER ---
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
                if (i == 6 || i == 7) {
                    // Right aligned for Debit and Credit
                    val textW = thTextPaint.measureText(title)
                    canvas.drawText(title, colX + width - textW - 6f, currentY + 14f, thTextPaint)
                } else {
                    canvas.drawText(title, colX + 6f, currentY + 14f, thTextPaint)
                }
                colX += width
            }
            currentY += tableHeaderHeight + 2f

            // --- TABLE ROWS ---
            val rowsForThisPage = if (pageNumber == 1) firstPageRows else followPageRows
            var rowsPrinted = 0

            if (transactions.isEmpty()) {
                // Graceful Empty State
                val emptyRect = RectF(margin, currentY + 10f, pageWidth - margin, currentY + 70f)
                val emptyBgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
                canvas.drawRoundRect(emptyRect, 8f, 8f, emptyBgPaint)
                val emptyBorderPaint = Paint().apply {
                    color = Color.rgb(203, 213, 225)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(emptyRect, 8f, 8f, emptyBorderPaint)
                val emptyMsg = "No transactions found for the selected date range / filters."
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
                    textSize = 8f
                }
                val boldCellPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.rgb(15, 23, 42)
                    textSize = 8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                while (currentTxIndex < transactions.size && rowsPrinted < rowsForThisPage) {
                    val tx = transactions[currentTxIndex]
                    val isIncome = tx.type.equals("INCOME", ignoreCase = true)

                    // Alternate background
                    if (currentTxIndex % 2 == 1) {
                        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, zebraPaint)
                    }
                    canvas.drawLine(margin, currentY + rowHeight, pageWidth - margin, currentY + rowHeight, rowLinePaint)

                    var cx = margin

                    // 1. Date
                    canvas.drawText(tx.dateFormatted, cx + 4f, currentY + 15f, cellTextPaint)
                    cx += colWidths[0]

                    // 2. Type (Badge style text)
                    if (isIncome) {
                        canvas.drawText("INCOME", cx + 4f, currentY + 15f, incomePaint)
                    } else {
                        canvas.drawText("EXPENSE", cx + 4f, currentY + 15f, expensePaint)
                    }
                    cx += colWidths[1]

                    // 3. Particular / Party Name
                    val partyDisplay = if (tx.partyName.isNotBlank()) tx.partyName else tx.description
                    val truncatedParty = truncateText(partyDisplay, boldCellPaint, colWidths[2] - 8f)
                    canvas.drawText(truncatedParty, cx + 4f, currentY + 15f, boldCellPaint)
                    cx += colWidths[2]

                    // 4. Category
                    val categoryDisplay = if (tx.subCategory.isNotBlank()) "${tx.category} / ${tx.subCategory}" else tx.category
                    val truncatedCat = truncateText(categoryDisplay, cellTextPaint, colWidths[3] - 8f)
                    canvas.drawText(truncatedCat, cx + 4f, currentY + 15f, cellTextPaint)
                    cx += colWidths[3]

                    // 5. Site
                    val truncatedSite = truncateText(tx.siteName, cellTextPaint, colWidths[4] - 8f)
                    canvas.drawText(truncatedSite, cx + 4f, currentY + 15f, cellTextPaint)
                    cx += colWidths[4]

                    // 6. Payment Mode
                    canvas.drawText(tx.paymentMode, cx + 4f, currentY + 15f, cellTextPaint)
                    cx += colWidths[5]

                    // 7. Debit (Expense Amount)
                    if (!isIncome) {
                        val amtStr = formatCurrency(tx.amount)
                        val textW = expensePaint.measureText(amtStr)
                        canvas.drawText(amtStr, cx + colWidths[6] - textW - 4f, currentY + 15f, expensePaint)
                    } else {
                        val dashW = cellTextPaint.measureText("-")
                        canvas.drawText("-", cx + (colWidths[6] - dashW) / 2f, currentY + 15f, cellTextPaint)
                    }
                    cx += colWidths[6]

                    // 8. Credit (Income Amount)
                    if (isIncome) {
                        val amtStr = formatCurrency(tx.amount)
                        val textW = incomePaint.measureText(amtStr)
                        canvas.drawText(amtStr, cx + colWidths[7] - textW - 4f, currentY + 15f, incomePaint)
                    } else {
                        val dashW = cellTextPaint.measureText("-")
                        canvas.drawText("-", cx + (colWidths[7] - dashW) / 2f, currentY + 15f, cellTextPaint)
                    }

                    currentY += rowHeight
                    rowsPrinted++
                    currentTxIndex++
                }

                // If on final page, draw bottom totals row
                if (pageNumber == totalPages && transactions.isNotEmpty()) {
                    val totalRowBg = Paint().apply { color = Color.rgb(241, 245, 249) }
                    canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight + 4f, totalRowBg)
                    canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
                    canvas.drawLine(margin, currentY + rowHeight + 4f, pageWidth - margin, currentY + rowHeight + 4f, primaryPaint)

                    canvas.drawText("TOTAL SUMMARY", margin + 6f, currentY + 16f, boldTextPaint)

                    // Debit Total
                    val debitTotalStr = formatCurrency(totalExpense)
                    val debitW = expensePaint.measureText(debitTotalStr)
                    val debitX = margin + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3] + colWidths[4] + colWidths[5]
                    canvas.drawText(debitTotalStr, debitX + colWidths[6] - debitW - 4f, currentY + 16f, expensePaint)

                    // Credit Total
                    val creditTotalStr = formatCurrency(totalIncome)
                    val creditW = incomePaint.measureText(creditTotalStr)
                    val creditX = debitX + colWidths[6]
                    canvas.drawText(creditTotalStr, creditX + colWidths[7] - creditW - 4f, currentY + 16f, incomePaint)
                }
            }

            // --- PAGE FOOTER ---
            val footerY = pageHeight - footerHeight + 10f
            canvas.drawLine(margin, footerY - 8f, pageWidth - margin, footerY - 8f, linePaint)

            val pageStr = "Page $pageNumber of $totalPages"
            val pageW = subTitlePaint.measureText(pageStr)
            canvas.drawText(pageStr, pageWidth - margin - pageW, footerY + 8f, subTitlePaint)

            canvas.drawText("RPVC Construction ERP • Confidential Financial Record", margin, footerY + 8f, subTitlePaint)

            pdfDocument.finishPage(page)
        }

        // Save PDF to App Documents / Cache Directory
        val reportsDir = File(context.cacheDir, "daybook_pdf_reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedSite = activeSiteName.replace(" ", "_").take(15)
        val file = File(reportsDir, "DayBook_${sanitizedSite}_$timeStamp.pdf")

        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return file
    }

    private fun drawSummaryCard(
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
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, x + 8f, y + 14f, titlePaint)

        val valPaint = Paint().apply {
            isAntiAlias = true
            color = textColor
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(value, x + 8f, y + 33f, valPaint)
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
     * Opens the PDF file directly in any installed native Android PDF viewer.
     * If no PDF viewer is found on the device, it informs the user and opens the Share chooser instead.
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
            // Gracefully offer share alternative so user is not stranded
            sharePdfFile(context, file, chooserTitle = "Share / Save Day Book PDF")
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            sharePdfFile(context, file, chooserTitle = "Share / Save Day Book PDF")
        }
    }

    /**
     * Shares the generated PDF file via standard Android share chooser (WhatsApp, Drive, Email, etc.).
     */
    fun sharePdfFile(context: Context, file: File, chooserTitle: String = "Share Day Book PDF Statement") {
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
                putExtra(Intent.EXTRA_TEXT, "Day Book Financial Statement (${file.name}) from RPVC Construction & Infra.")
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
