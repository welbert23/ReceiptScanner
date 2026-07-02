package com.receiptscanner.app.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.receiptscanner.app.data.Receipt
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PdfExporter {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH"))
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US)

    fun exportReceipt(context: Context, receipt: Receipt): File {
        val document = PdfDocument()
        drawReceiptPage(document, receipt, 1)
        val file = File(context.getExternalFilesDir("exports"), "receipt_${receipt.id}.pdf")
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    fun exportMultipleReceipts(context: Context, receipts: List<Receipt>): File {
        val document = PdfDocument()
        receipts.forEachIndexed { i, receipt ->
            drawReceiptPage(document, receipt, i + 1)
        }
        val file = File(context.getExternalFilesDir("exports"), "receipts_batch_${System.currentTimeMillis()}.pdf")
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    private fun drawReceiptPage(document: PdfDocument, receipt: Receipt, pageNumber: Int) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val m = 50f
        val r = 545f
        val pw = 595

        val pTitle = Paint().apply { textSize = 22f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT }
        val pMerchant = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT }
        val pLabel = Paint().apply { textSize = 12f; color = android.graphics.Color.GRAY; textAlign = Paint.Align.LEFT }
        val pValue = Paint().apply { textSize = 12f; textAlign = Paint.Align.LEFT }
        val pSection = Paint().apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT }
        val pTotalAmt = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT }
        val pTotalLbl = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT }
        val pLine = Paint().apply { color = android.graphics.Color.GRAY; strokeWidth = 0.5f }
        val pFooter = Paint().apply { textSize = 9f; color = android.graphics.Color.GRAY; textAlign = Paint.Align.LEFT }
        val pItem = Paint().apply { textSize = 12f }
        val pCenter = Paint().apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT }

        var y = 50f

        canvas.drawText("E-RECEIPT", (pw - pTitle.measureText("E-RECEIPT")) / 2f, y, pTitle); y += 32f
        canvas.drawLine(m, y, r, y, pLine); y += 22f

        canvas.drawText(receipt.merchantName.ifEmpty { "Unknown Merchant" }, m, y, pMerchant); y += 28f

        row(canvas, "Date", dateFormat.format(java.util.Date(receipt.date)), m, y, r, pLabel, pValue); y += 19f
        row(canvas, "Category", receipt.category.ifEmpty { "Uncategorized" }, m, y, r, pLabel, pValue); y += 19f
        if (receipt.invoiceNumber.isNotBlank()) { row(canvas, "Invoice #", receipt.invoiceNumber, m, y, r, pLabel, pValue); y += 19f }

        if (receipt.subtotal > 0) { row(canvas, "Subtotal", currencyFormat.format(receipt.subtotal), m, y, r, pLabel, pValue); y += 19f }
        if (receipt.tax > 0) { row(canvas, "Tax", currencyFormat.format(receipt.tax), m, y, r, pLabel, pValue); y += 19f }

        canvas.drawLine(m, y, r, y, pLine); y += 25f
        canvas.drawText("TOTAL", m, y, pTotalLbl)
        canvas.drawText(currencyFormat.format(receipt.total), r - pTotalAmt.measureText(currencyFormat.format(receipt.total)), y, pTotalAmt); y += 32f

        if (receipt.items.isNotBlank()) {
            canvas.drawLine(m, y, r, y, pLine); y += 16f
            canvas.drawText("ITEMS", m, y, pSection); y += 20f
            for (line in receipt.items.split("\n")) { canvas.drawText("•  $line", m + 12f, y, pItem); y += 17f }
            y += 3f
        }

        if (receipt.notes.isNotBlank()) {
            canvas.drawLine(m, y, r, y, pLine); y += 16f
            canvas.drawText("NOTES", m, y, pSection); y += 20f
            canvas.drawText(receipt.notes, m + 12f, y, pItem); y += 20f
        }

        if (receipt.imagePath.isNotBlank()) {
            canvas.drawLine(m, y, r, y, pLine); y += 16f
            val imgLabel = "RECEIPT IMAGE"
            canvas.drawText(imgLabel, (pw - pCenter.measureText(imgLabel)) / 2f, y, pCenter); y += 20f
            try {
                val bmp = BitmapFactory.decodeFile(receipt.imagePath)
                if (bmp != null) {
                    val s = minOf((r - m) / bmp.width, 400f / bmp.height).coerceAtMost(1f)
                    val sw = (bmp.width * s).toInt(); val sh = (bmp.height * s).toInt()
                    val imgX = (pw - sw) / 2f
                    canvas.drawBitmap(bmp, null, android.graphics.RectF(imgX, y, imgX + sw, y + sh), null)
                    y += sh + 12f
                }
            } catch (_: Exception) { }
        }

        y += 15f
        canvas.drawLine(m, y, r, y, pLine)
        y += 12f
        canvas.drawText("Generated by Receipt Scanner", (pw - pFooter.measureText("Generated by Receipt Scanner")) / 2f, y, pFooter)

        document.finishPage(page)
    }

    private fun row(canvas: Canvas, label: String, value: String, x: Float, y: Float, rx: Float, lp: Paint, vp: Paint) {
        canvas.drawText(label, x, y, lp)
        canvas.drawText(value, x + 130f, y, vp)
    }
}
