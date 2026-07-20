package com.shejan.financebuddy.ui.reports

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

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

            val pageWidth = 595 // A4 standard width in points
            val pageHeight = 842 // A4 standard height in points
            val margin = 36f

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint()
            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 18f
                color = Color.parseColor("#0F172A") // Navy Primary
                isFakeBoldText = true
            }

            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                color = Color.parseColor("#64748B") // Muted Slate
            }

            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#F1F5F9") // Light Slate Gray
            }

            val headerTextPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.parseColor("#334155")
                isFakeBoldText = true
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

            var currentY = margin

            // ─── Document Header ─────────────────────────────────────
            canvas.drawText("FinanceBuddy — Monthly Financial Report", margin, currentY + 16f, titlePaint)
            currentY += 24f

            canvas.drawText(
                "Statement Period: $monthName $year | Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}",
                margin,
                currentY + 12f,
                subtitlePaint
            )
            currentY += 24f

            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
            currentY += 16f

            // ─── Table Headers ───────────────────────────────────────
            val colXDate = margin + 8f
            val colXCat = margin + 110f
            val colXType = margin + 230f
            val colXAcc = margin + 300f
            val colXAmount = pageWidth - margin - 8f

            fun drawTableHeader(c: android.graphics.Canvas, y: Float) {
                val rect = RectF(margin, y, pageWidth - margin, y + 24f)
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
            val maxTableY = pageHeight - 160f // Leave space for summary box

            val sortedTxs = transactions.sortedByDescending { it.timestamp }

            for (tx in sortedTxs) {
                if (currentY > maxTableY) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = margin

                    canvas.drawText("FinanceBuddy — Monthly Financial Report ($monthName $year) [Contd.]", margin, currentY + 14f, subtitlePaint)
                    currentY += 24f
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
                    isFakeBoldText = true
                }

                val amountPaint = Paint(bodyTextPaint).apply {
                    textAlign = Paint.Align.RIGHT
                    color = Color.parseColor(typeColor)
                    isFakeBoldText = true
                }

                // Truncate strings to prevent overlapping in table columns
                val safeCat = if (catText.length > 20) catText.take(18) + "…" else catText
                val safeAcc = if (accName.length > 16) accName.take(14) + "…" else accName

                canvas.drawText(formattedDate, colXDate, currentY + 14f, bodyTextPaint)
                canvas.drawText(safeCat, colXCat, currentY + 14f, bodyTextPaint)
                canvas.drawText(typeText, colXType, currentY + 14f, typePaint)
                canvas.drawText(safeAcc, colXAcc, currentY + 14f, bodyTextPaint)

                val prefix = if (tx.type == "INCOME") "+৳" else if (tx.type == "EXPENSE") "-৳" else "৳"
                canvas.drawText("$prefix${currencyFormat.format(tx.amount)}", colXAmount, currentY + 14f, amountPaint)

                currentY += rowHeight
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
                currentY += 4f
            }

            if (sortedTxs.isEmpty()) {
                canvas.drawText("No transactions recorded for $monthName $year.", colXDate, currentY + 16f, bodyTextPaint)
                currentY += 28f
            }

            // ─── Check Summary Box Space ──────────────────────────────
            val summaryBoxHeight = 120f
            if (currentY + summaryBoxHeight > pageHeight - margin) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin + 20f
            } else {
                currentY += 16f
            }

            // ─── Summary Box Card ─────────────────────────────────────
            val boxRect = RectF(margin, currentY, pageWidth - margin, currentY + summaryBoxHeight)
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
                textSize = 12f
                color = Color.parseColor("#0F172A")
                isFakeBoldText = true
            }

            val boxLabelPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.parseColor("#475569")
            }

            val boxValPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10.5f
                color = Color.parseColor("#0F172A")
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }

            var boxY = currentY + 22f
            canvas.drawText("Financial Summary Overview ($monthName $year)", margin + 16f, boxY, boxTitlePaint)
            boxY += 8f
            canvas.drawLine(margin + 16f, boxY, pageWidth - margin - 16f, boxY, linePaint)
            boxY += 18f

            // Row 1: Starting Balance
            canvas.drawText("Starting Bank / Accounts Balance:", margin + 16f, boxY, boxLabelPaint)
            canvas.drawText("৳${currencyFormat.format(startingBalance)}", pageWidth - margin - 16f, boxY, boxValPaint)
            boxY += 16f

            // Row 2: Total Income
            val incomeValPaint = Paint(boxValPaint).apply { color = Color.parseColor("#16A34A") }
            canvas.drawText("Total Monthly Income (+):", margin + 16f, boxY, boxLabelPaint)
            canvas.drawText("+৳${currencyFormat.format(totalIncome)}", pageWidth - margin - 16f, boxY, incomeValPaint)
            boxY += 16f

            // Row 3: Total Expense
            val expenseValPaint = Paint(boxValPaint).apply { color = Color.parseColor("#DC2626") }
            canvas.drawText("Total Monthly Expenses (-):", margin + 16f, boxY, boxLabelPaint)
            canvas.drawText("-৳${currencyFormat.format(totalExpense)}", pageWidth - margin - 16f, boxY, expenseValPaint)
            boxY += 18f

            // Row 4: Remaining / Closing Balance
            val remainingValPaint = Paint(boxValPaint).apply {
                textSize = 11.5f
                color = Color.parseColor("#0284C7") // Teal/Blue Highlight
            }
            canvas.drawText("Net Remaining Balance (Closing):", margin + 16f, boxY, boxTitlePaint)
            canvas.drawText("৳${currencyFormat.format(remainingBalance)}", pageWidth - margin - 16f, boxY, remainingValPaint)

            pdfDocument.finishPage(page)

            // ─── Save PDF File ────────────────────────────────────────
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

            val pageWidth = 595
            val pageHeight = 842
            val margin = 36f

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 18f
                color = Color.parseColor("#0F172A")
                isFakeBoldText = true
            }

            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                color = Color.parseColor("#64748B")
            }

            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#F1F5F9")
            }

            val headerTextPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.parseColor("#334155")
                isFakeBoldText = true
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

            var currentY = margin

            // ─── Header ──────────────────────────────────────────────
            canvas.drawText("FinanceBuddy — Annual Financial Report", margin, currentY + 16f, titlePaint)
            currentY += 24f

            canvas.drawText(
                "Statement Period: Calendar Year $year | Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}",
                margin,
                currentY + 12f,
                subtitlePaint
            )
            currentY += 24f

            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
            currentY += 16f

            // ─── Table Headers ───────────────────────────────────────
            val colXMonth = margin + 8f
            val colXStart = margin + 90f
            val colXIncome = margin + 200f
            val colXExpense = margin + 310f
            val colXRemaining = pageWidth - margin - 8f

            val rect = RectF(margin, currentY, pageWidth - margin, currentY + 24f)
            canvas.drawRoundRect(rect, 4f, 4f, headerBgPaint)

            canvas.drawText("Month", colXMonth, currentY + 16f, headerTextPaint)
            canvas.drawText("Starting Bal (৳)", colXStart, currentY + 16f, headerTextPaint)
            canvas.drawText("Income (৳)", colXIncome, currentY + 16f, headerTextPaint)
            canvas.drawText("Expense (৳)", colXExpense, currentY + 16f, headerTextPaint)

            val rightHeaderPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("Closing Bal (৳)", colXRemaining, currentY + 16f, rightHeaderPaint)

            currentY += 28f

            // ─── Monthly Rows ────────────────────────────────────────
            val rowHeight = 22f
            val incomeTextPaint = Paint(bodyTextPaint).apply { color = Color.parseColor("#16A34A"); isFakeBoldText = true }
            val expenseTextPaint = Paint(bodyTextPaint).apply { color = Color.parseColor("#DC2626"); isFakeBoldText = true }
            val rightValPaint = Paint(bodyTextPaint).apply { textAlign = Paint.Align.RIGHT; isFakeBoldText = true }

            for (summary in monthlySummaries) {
                canvas.drawText(summary.monthName, colXMonth, currentY + 14f, bodyTextPaint)
                canvas.drawText("৳${currencyFormat.format(summary.startingBalance)}", colXStart, currentY + 14f, bodyTextPaint)
                canvas.drawText("+৳${currencyFormat.format(summary.totalIncome)}", colXIncome, currentY + 14f, incomeTextPaint)
                canvas.drawText("-৳${currencyFormat.format(summary.totalExpense)}", colXExpense, currentY + 14f, expenseTextPaint)
                canvas.drawText("৳${currencyFormat.format(summary.remainingBalance)}", colXRemaining, currentY + 14f, rightValPaint)

                currentY += rowHeight
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
                currentY += 4f
            }

            currentY += 16f

            // ─── Annual Summary Box ──────────────────────────────────
            val summaryBoxHeight = 120f
            val boxRect = RectF(margin, currentY, pageWidth - margin, currentY + summaryBoxHeight)
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
                textSize = 12f
                color = Color.parseColor("#0F172A")
                isFakeBoldText = true
            }

            val boxLabelPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.parseColor("#475569")
            }

            val boxValPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10.5f
                color = Color.parseColor("#0F172A")
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }

            var boxY = currentY + 22f
            canvas.drawText("Annual Financial Summary ($year)", margin + 16f, boxY, boxTitlePaint)
            boxY += 8f
            canvas.drawLine(margin + 16f, boxY, pageWidth - margin - 16f, boxY, linePaint)
            boxY += 18f

            canvas.drawText("Year-Start Opening Balance:", margin + 16f, boxY, boxLabelPaint)
            canvas.drawText("৳${currencyFormat.format(annualStartingBalance)}", pageWidth - margin - 16f, boxY, boxValPaint)
            boxY += 16f

            val incBoxPaint = Paint(boxValPaint).apply { color = Color.parseColor("#16A34A") }
            canvas.drawText("Total Annual Income (+):", margin + 16f, boxY, boxLabelPaint)
            canvas.drawText("+৳${currencyFormat.format(totalAnnualIncome)}", pageWidth - margin - 16f, boxY, incBoxPaint)
            boxY += 16f

            val expBoxPaint = Paint(boxValPaint).apply { color = Color.parseColor("#DC2626") }
            canvas.drawText("Total Annual Expenses (-):", margin + 16f, boxY, boxLabelPaint)
            canvas.drawText("-৳${currencyFormat.format(totalAnnualExpense)}", pageWidth - margin - 16f, boxY, expBoxPaint)
            boxY += 18f

            val remBoxPaint = Paint(boxValPaint).apply {
                textSize = 11.5f
                color = Color.parseColor("#0284C7")
            }
            canvas.drawText("Year-End Closing Balance:", margin + 16f, boxY, boxTitlePaint)
            canvas.drawText("৳${currencyFormat.format(annualRemainingBalance)}", pageWidth - margin - 16f, boxY, remBoxPaint)

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
            // PDF viewer app might not be installed, notification Toast is already shown
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

