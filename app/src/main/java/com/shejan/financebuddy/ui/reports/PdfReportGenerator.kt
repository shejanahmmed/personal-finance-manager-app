package com.shejan.financebuddy.ui.reports

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.shejan.financebuddy.R
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 36f

    fun exportMonthlyReportPdf(
        context: Context,
        monthName: String,
        year: Int,
        transactions: List<TransactionEntity>,
        accountsMap: Map<Int, AccountEntity>,
        startingBalance: Double,
        totalIncome: Double,
        totalExpense: Double,
        remainingBalance: Double
    ) {
        try {
            val pdfDocument = PdfDocument()
            val currencyFormat = DecimalFormat("##,##,##0.00")
            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            val genDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            var currentY = drawHeader(
                canvas = canvas,
                context = context,
                reportName = "Monthly Financial Report",
                periodText = "$monthName $year",
                generatedDateStr = genDateStr,
                margin = MARGIN,
                pageWidth = PAGE_WIDTH.toFloat()
            )

            // Table Paints
            val headerBgPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
            val headerTextPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.parseColor("#334155")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyTextPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                color = Color.parseColor("#1E293B")
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }

            // Column X coordinates
            val colXDate = MARGIN + 8f
            val colXCat = MARGIN + 110f
            val colXType = MARGIN + 230f
            val colXAcc = MARGIN + 300f
            val colXAmount = PAGE_WIDTH - MARGIN - 8f

            fun drawTableHeader(c: android.graphics.Canvas, y: Float) {
                val rect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 24f)
                c.drawRoundRect(rect, 4f, 4f, headerBgPaint)

                c.drawText("Date & Time", colXDate, y + 16f, headerTextPaint)
                c.drawText("Category", colXCat, y + 16f, headerTextPaint)
                c.drawText("Type", colXType, y + 16f, headerTextPaint)
                c.drawText("Account", colXAcc, y + 16f, headerTextPaint)

                val amountHeaderPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
                c.drawText("Amount (৳)", colXAmount, y + 16f, amountHeaderPaint)
            }

            drawTableHeader(canvas, currentY)
            currentY += 28f

            // ─── Transactions List ───────────────────────────────────
            val rowHeight = 22f
            val maxTableY = PAGE_HEIGHT - 170f // Space for summary box & footer

            val sortedTxs = transactions.sortedByDescending { it.timestamp }

            for (tx in sortedTxs) {
                if (currentY > maxTableY) {
                    drawFooter(canvas, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), MARGIN)
                    pdfDocument.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = drawHeader(
                        canvas = canvas,
                        context = context,
                        reportName = "Monthly Financial Report [Contd.]",
                        periodText = "$monthName $year",
                        generatedDateStr = genDateStr,
                        margin = MARGIN,
                        pageWidth = PAGE_WIDTH.toFloat()
                    )

                    drawTableHeader(canvas, currentY)
                    currentY += 28f
                }

                val formattedDate = dateFormat.format(Date(tx.timestamp))
                val catText = if (tx.note.isNotBlank()) "${tx.category} (${tx.note})" else tx.category
                val accName = accountsMap[tx.fromAccountId]?.name ?: "Account #${tx.fromAccountId}"

                val (typeText, typeColor) = when (tx.type) {
                    "INCOME" -> "Income" to "#16A34A"
                    "EXPENSE" -> "Expense" to "#DC2626"
                    else -> "Transfer" to "#D97706"
                }

                val typePaint = Paint(bodyTextPaint).apply {
                    color = Color.parseColor(typeColor)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val amountPaint = Paint(bodyTextPaint).apply {
                    textAlign = Paint.Align.RIGHT
                    color = Color.parseColor(typeColor)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val safeCat = if (catText.length > 20) catText.take(18) + "…" else catText
                val safeAcc = if (accName.length > 16) accName.take(14) + "…" else accName

                canvas.drawText(formattedDate, colXDate, currentY + 14f, bodyTextPaint)
                canvas.drawText(safeCat, colXCat, currentY + 14f, bodyTextPaint)
                canvas.drawText(typeText, colXType, currentY + 14f, typePaint)
                canvas.drawText(safeAcc, colXAcc, currentY + 14f, bodyTextPaint)

                val prefix = if (tx.type == "INCOME") "+৳" else if (tx.type == "EXPENSE") "-৳" else "৳"
                canvas.drawText("$prefix${currencyFormat.format(tx.amount)}", colXAmount, currentY + 14f, amountPaint)

                currentY += rowHeight
                canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
                currentY += 4f
            }

            if (sortedTxs.isEmpty()) {
                canvas.drawText("No transactions recorded for $monthName $year.", colXDate, currentY + 16f, bodyTextPaint)
                currentY += 28f
            }

            // ─── Summary Box Card ─────────────────────────────────────
            val summaryBoxHeight = 115f
            if (currentY + summaryBoxHeight > PAGE_HEIGHT - 60f) {
                drawFooter(canvas, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), MARGIN)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = drawHeader(
                    canvas = canvas,
                    context = context,
                    reportName = "Monthly Financial Report [Summary]",
                    periodText = "$monthName $year",
                    generatedDateStr = genDateStr,
                    margin = MARGIN,
                    pageWidth = PAGE_WIDTH.toFloat()
                )
            } else {
                currentY += 12f
            }

            val boxRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + summaryBoxHeight)
            val boxBgPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }
            val boxBorderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }

            canvas.drawRoundRect(boxRect, 8f, 8f, boxBgPaint)
            canvas.drawRoundRect(boxRect, 8f, 8f, boxBorderPaint)

            val boxTitlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 11.5f
                color = Color.parseColor("#0F172A")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val boxLabelPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                color = Color.parseColor("#475569")
            }

            val boxValPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.parseColor("#0F172A")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            var boxY = currentY + 20f
            canvas.drawText("Financial Summary Overview ($monthName $year)", MARGIN + 14f, boxY, boxTitlePaint)
            boxY += 8f
            canvas.drawLine(MARGIN + 14f, boxY, PAGE_WIDTH - MARGIN - 14f, boxY, linePaint)
            boxY += 16f

            // Starting balance
            canvas.drawText("Starting Bank / Accounts Balance:", MARGIN + 14f, boxY, boxLabelPaint)
            canvas.drawText("৳${currencyFormat.format(startingBalance)}", PAGE_WIDTH - MARGIN - 14f, boxY, boxValPaint)
            boxY += 15f

            // Total Income
            val incomeValPaint = Paint(boxValPaint).apply { color = Color.parseColor("#16A34A") }
            canvas.drawText("Total Monthly Income (+):", MARGIN + 14f, boxY, boxLabelPaint)
            canvas.drawText("+৳${currencyFormat.format(totalIncome)}", PAGE_WIDTH - MARGIN - 14f, boxY, incomeValPaint)
            boxY += 15f

            // Total Expense
            val expenseValPaint = Paint(boxValPaint).apply { color = Color.parseColor("#DC2626") }
            canvas.drawText("Total Monthly Expenses (-):", MARGIN + 14f, boxY, boxLabelPaint)
            canvas.drawText("-৳${currencyFormat.format(totalExpense)}", PAGE_WIDTH - MARGIN - 14f, boxY, expenseValPaint)
            boxY += 16f

            // Remaining balance
            val remainingValPaint = Paint(boxValPaint).apply {
                textSize = 11f
                color = Color.parseColor("#00D4AA")
            }
            canvas.drawText("Net Remaining Balance (Closing):", MARGIN + 14f, boxY, boxTitlePaint)
            canvas.drawText("৳${currencyFormat.format(remainingBalance)}", PAGE_WIDTH - MARGIN - 14f, boxY, remainingValPaint)

            // Draw Footer
            drawFooter(canvas, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), MARGIN)

            pdfDocument.finishPage(page)

            // Save PDF File
            val fileName = "FinanceBuddy_Report_${monthName}_$year.pdf"
            val uri = savePdfToDownloads(context, pdfDocument, fileName)

            pdfDocument.close()

            if (uri != null) {
                Toast.makeText(context, "PDF Report exported successfully to Downloads!", Toast.LENGTH_LONG).show()
                openPdfViewer(context, uri)
            } else {
                Toast.makeText(context, "Failed to save PDF Report.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error exporting PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportYearlyReportPdf(
        context: Context,
        year: Int,
        monthlySummaries: List<MonthSummaryData>,
        annualStartingBalance: Double,
        totalAnnualIncome: Double,
        totalAnnualExpense: Double,
        annualRemainingBalance: Double
    ) {
        try {
            val pdfDocument = PdfDocument()
            val currencyFormat = DecimalFormat("##,##,##0.00")
            val genDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = drawHeader(
                canvas = canvas,
                context = context,
                reportName = "Annual Financial Report",
                periodText = "Calendar Year $year",
                generatedDateStr = genDateStr,
                margin = MARGIN,
                pageWidth = PAGE_WIDTH.toFloat()
            )

            // Table Headers
            val headerBgPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
            val headerTextPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.parseColor("#334155")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyTextPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                color = Color.parseColor("#1E293B")
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }

            val colXMonth = MARGIN + 8f
            val colXStart = MARGIN + 90f
            val colXIncome = MARGIN + 200f
            val colXExpense = MARGIN + 310f
            val colXRemaining = PAGE_WIDTH - MARGIN - 8f

            val rect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 24f)
            canvas.drawRoundRect(rect, 4f, 4f, headerBgPaint)

            canvas.drawText("Month", colXMonth, currentY + 16f, headerTextPaint)
            canvas.drawText("Starting Bal (৳)", colXStart, currentY + 16f, headerTextPaint)
            canvas.drawText("Income (৳)", colXIncome, currentY + 16f, headerTextPaint)
            canvas.drawText("Expense (৳)", colXExpense, currentY + 16f, headerTextPaint)

            val rightHeaderPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("Closing Bal (৳)", colXRemaining, currentY + 16f, rightHeaderPaint)

            currentY += 28f

            // Monthly Rows
            val rowHeight = 22f
            val incomeTextPaint = Paint(bodyTextPaint).apply { color = Color.parseColor("#16A34A"); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val expenseTextPaint = Paint(bodyTextPaint).apply { color = Color.parseColor("#DC2626"); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val rightValPaint = Paint(bodyTextPaint).apply { textAlign = Paint.Align.RIGHT; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

            for (summary in monthlySummaries) {
                canvas.drawText(summary.monthName, colXMonth, currentY + 14f, bodyTextPaint)
                canvas.drawText("৳${currencyFormat.format(summary.startingBalance)}", colXStart, currentY + 14f, bodyTextPaint)
                canvas.drawText("+৳${currencyFormat.format(summary.totalIncome)}", colXIncome, currentY + 14f, incomeTextPaint)
                canvas.drawText("-৳${currencyFormat.format(summary.totalExpense)}", colXExpense, currentY + 14f, expenseTextPaint)
                canvas.drawText("৳${currencyFormat.format(summary.remainingBalance)}", colXRemaining, currentY + 14f, rightValPaint)

                currentY += rowHeight
                canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
                currentY += 4f
            }

            currentY += 14f

            // Annual Summary Box
            val summaryBoxHeight = 115f
            val boxRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + summaryBoxHeight)
            val boxBgPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }
            val boxBorderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }

            canvas.drawRoundRect(boxRect, 8f, 8f, boxBgPaint)
            canvas.drawRoundRect(boxRect, 8f, 8f, boxBorderPaint)

            val boxTitlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 11.5f
                color = Color.parseColor("#0F172A")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val boxLabelPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                color = Color.parseColor("#475569")
            }

            val boxValPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.parseColor("#0F172A")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            var boxY = currentY + 20f
            canvas.drawText("Annual Financial Summary ($year)", MARGIN + 14f, boxY, boxTitlePaint)
            boxY += 8f
            canvas.drawLine(MARGIN + 14f, boxY, PAGE_WIDTH - MARGIN - 14f, boxY, linePaint)
            boxY += 16f

            canvas.drawText("Year-Start Opening Balance:", MARGIN + 14f, boxY, boxLabelPaint)
            canvas.drawText("৳${currencyFormat.format(annualStartingBalance)}", PAGE_WIDTH - MARGIN - 14f, boxY, boxValPaint)
            boxY += 15f

            val incBoxPaint = Paint(boxValPaint).apply { color = Color.parseColor("#16A34A") }
            canvas.drawText("Total Annual Income (+):", MARGIN + 14f, boxY, boxLabelPaint)
            canvas.drawText("+৳${currencyFormat.format(totalAnnualIncome)}", PAGE_WIDTH - MARGIN - 14f, boxY, incBoxPaint)
            boxY += 15f

            val expBoxPaint = Paint(boxValPaint).apply { color = Color.parseColor("#DC2626") }
            canvas.drawText("Total Annual Expenses (-):", MARGIN + 14f, boxY, boxLabelPaint)
            canvas.drawText("-৳${currencyFormat.format(totalAnnualExpense)}", PAGE_WIDTH - MARGIN - 14f, boxY, expBoxPaint)
            boxY += 16f

            val remBoxPaint = Paint(boxValPaint).apply {
                textSize = 11f
                color = Color.parseColor("#00D4AA")
            }
            canvas.drawText("Year-End Closing Balance:", MARGIN + 14f, boxY, boxTitlePaint)
            canvas.drawText("৳${currencyFormat.format(annualRemainingBalance)}", PAGE_WIDTH - MARGIN - 14f, boxY, remBoxPaint)

            // Draw Footer
            drawFooter(canvas, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), MARGIN)

            pdfDocument.finishPage(page)

            val fileName = "FinanceBuddy_Annual_Report_$year.pdf"
            val uri = savePdfToDownloads(context, pdfDocument, fileName)
            pdfDocument.close()

            if (uri != null) {
                Toast.makeText(context, "Annual PDF Report exported successfully to Downloads!", Toast.LENGTH_LONG).show()
                openPdfViewer(context, uri)
            } else {
                Toast.makeText(context, "Failed to save PDF Report.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error exporting PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // ─── Header Component ──────────────────────────────────────────
    private fun drawHeader(
        canvas: android.graphics.Canvas,
        context: Context,
        reportName: String,
        periodText: String,
        generatedDateStr: String,
        margin: Float,
        pageWidth: Float
    ): Float {
        val startY = margin

        val reportTitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 15f
            color = Color.parseColor("#0F172A") // Dark Slate
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.parseColor("#64748B") // Muted Slate
        }

        val rightAppNamePaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.parseColor("#0F172A") // Dark Slate
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val taglinePaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.parseColor("#64748B")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.RIGHT
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1.2f
        }

        // ── TOP LEFT COLUMN ─────────────────────────────────────────
        // Line 1: Report Name (e.g. "Monthly Financial Report" / "Annual Financial Report")
        canvas.drawText(reportName, margin, startY + 16f, reportTitlePaint)
        // Line 2: Time Period
        canvas.drawText("Period: $periodText", margin, startY + 34f, subtitlePaint)
        // Line 3: Generated Date
        canvas.drawText("Generated: $generatedDateStr", margin, startY + 48f, subtitlePaint)

        // ── TOP RIGHT COLUMN ────────────────────────────────────────
        val logoSize = 34f
        val rightX = pageWidth - margin
        val logoX = rightX - logoSize
        val logoY = startY

        val logoBitmap = try {
            val options = BitmapFactory.Options().apply {
                inScaled = false
            }
            BitmapFactory.decodeResource(context.resources, R.drawable.financebuddy, options)
                ?: BitmapFactory.decodeResource(context.resources, R.drawable.financebuddy)
        } catch (_: Exception) {
            null
        }

        if (logoBitmap != null) {
            val dstRect = RectF(logoX, logoY, logoX + logoSize, logoY + logoSize)
            val bitmapPaint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                isDither = true
            }
            canvas.drawBitmap(logoBitmap, null, dstRect, bitmapPaint)
        } else {
            val logoBgPaint = Paint().apply { color = Color.parseColor("#00D4AA") }
            val logoTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawCircle(logoX + logoSize / 2f, logoY + logoSize / 2f, logoSize / 2f, logoBgPaint)
            canvas.drawText("F", logoX + logoSize / 2f, logoY + logoSize / 2f + 5f, logoTextPaint)
        }

        // App Name under logo (clean native bold typeface, zero synthetic distortion)
        canvas.drawText("FinanceBuddy", rightX, logoY + logoSize + 13f, rightAppNamePaint)
        // Tagline under App Name (italicized, clean spacing)
        canvas.drawText("Track your every financial steps.", rightX, logoY + logoSize + 27f, taglinePaint)

        // Divider Line under Header
        val headerBottomY = startY + 76f
        canvas.drawLine(margin, headerBottomY, pageWidth - margin, headerBottomY, linePaint)

        return headerBottomY + 16f
    }

    // ─── Footer Component ──────────────────────────────────────────
    private fun drawFooter(
        canvas: android.graphics.Canvas,
        pageWidth: Float,
        pageHeight: Float,
        margin: Float
    ) {
        val footerY = pageHeight - margin - 10f
        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }
        val footerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.parseColor("#0F172A") // Solid Black / Dark Navy
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawLine(margin, footerY - 12f, pageWidth - margin, footerY - 12f, linePaint)
        canvas.drawText("Thank you for choosing our app", pageWidth / 2f, footerY + 2f, footerTextPaint)
    }

    private fun savePdfToDownloads(context: Context, pdfDocument: PdfDocument, fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
            }
            uri
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }

    private fun openPdfViewer(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF Financial Report"))
        } catch (e: Exception) {
            // PDF viewer app might not be installed
        }
    }
}

data class MonthSummaryData(
    val monthName: String,
    val startingBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val remainingBalance: Double
)
