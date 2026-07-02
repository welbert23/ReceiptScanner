package com.receiptscanner.app.util

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.receiptscanner.app.data.ReceiptItem
import com.receiptscanner.app.data.ScanResult
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

data class GeminiReceipt(
    @SerializedName("merchant_name") val merchantName: String = "",
    val date: String = "",
    val total: Double = 0.0,
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    @SerializedName("invoice_number") val invoiceNumber: String = "",
    val category: String = "",
    val items: List<GeminiItem> = emptyList()
)

data class GeminiItem(
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1
)

class GeminiHelper {

    private val gson = Gson()

    suspend fun parseReceipt(bitmap: Bitmap, apiKey: String): ScanResult = withContext(Dispatchers.IO) {
        try {
            val model = GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = apiKey
            )

            val prompt = """
Extract receipt information from this image and return ONLY valid JSON (no markdown, no code blocks).
Use this exact structure:
{
  "merchant_name": "",
  "date": "MM/dd/yyyy",
  "total": 0.0,
  "subtotal": 0.0,
  "tax": 0.0,
  "invoice_number": "",
  "category": "",
  "items": [{"name": "", "price": 0.0, "quantity": 1}]
}

Rules:
- merchant_name: the store/business name
- date: in MM/dd/yyyy format. If year is not visible, assume current year.
- total: the final total amount (largest number, usually at bottom)
- subtotal: amount before tax
- tax: tax amount only (not total)
- invoice_number: receipt/invoice number if visible
- category: best guess category (e.g. Groceries, Restaurant, Electronics, etc.)
- items: list of items purchased with name, price, and quantity
- If a field is not visible, use empty string or 0.0
- Return ONLY the JSON object, nothing else.
""".trimIndent()

            val response = model.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            val text = response.text?.trim() ?: ""
            val json = extractJson(text)
            val parsed = gson.fromJson(json, GeminiReceipt::class.java)

            val dateMs = parseDate(parsed.date)
            val items = parsed.items.map { ReceiptItem(name = it.name, price = it.price, quantity = it.quantity) }

            ScanResult(
                rawText = text,
                merchantName = parsed.merchantName,
                total = parsed.total,
                subtotal = parsed.subtotal,
                tax = parsed.tax,
                date = dateMs,
                items = items,
                invoiceNumber = parsed.invoiceNumber,
                confidence = 1.0f
            )
        } catch (e: Exception) {
            ScanResult(
                rawText = "Gemini error: ${e.message}",
                merchantName = "Error",
                total = 0.0,
                subtotal = 0.0,
                tax = 0.0,
                date = System.currentTimeMillis(),
                items = emptyList(),
                invoiceNumber = "",
                confidence = 0f
            )
        }
    }

    private fun extractJson(text: String): String {
        val clean = text.trim()
        if (clean.startsWith("```")) {
            val lines = clean.lines()
            val body = lines.drop(1).dropLastWhile { it.trim().startsWith("```") }.joinToString("\n")
            return body.trim()
        }
        val start = clean.indexOf('{')
        val end = clean.lastIndexOf('}')
        return if (start >= 0 && end > start) clean.substring(start, end + 1) else clean
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        val formats = listOf("MM/dd/yyyy", "yyyy-MM-dd", "MM/dd/yy", "MMMM dd, yyyy", "MM-dd-yyyy")
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.isLenient = false
                return sdf.parse(dateStr)?.time ?: continue
            } catch (_: Exception) { }
        }
        return System.currentTimeMillis()
    }
}
