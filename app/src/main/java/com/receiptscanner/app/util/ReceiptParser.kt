package com.receiptscanner.app.util

import com.receiptscanner.app.data.ReceiptItem
import com.receiptscanner.app.data.ScanResult
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptParser {

    fun parse(rawText: String): ScanResult {
        val lines = rawText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val merchantName = extractMerchant(lines)
        val date = extractDate(lines)
        val items = extractItems(lines)
        val subtotal = extractSubtotal(lines)
        val tax = extractTax(lines)
        val total = extractTotal(lines)
        val invoiceNumber = extractInvoiceNumber(lines)
        val confidence = calculateConfidence(merchantName, total, date, items)

        return ScanResult(
            rawText = rawText,
            merchantName = merchantName,
            total = total,
            subtotal = subtotal,
            tax = tax,
            date = date,
            items = items,
            invoiceNumber = invoiceNumber,
            confidence = confidence
        )
    }

    private fun extractMerchant(lines: List<String>): String {
        val skipWords = setOf(
            "receipt", "invoice", "cashier", "teller", "pos ", "branch",
            "store no", "store#", "date", "time", "tel ", "telephone",
            "address", "order", "table", "thank", "change", "total",
            "subtotal", "tax", "vat", "qty", "quantity", "item",
            "description", "price", "amount", "balance", "cash",
            "tender", "card", "visa", "mastercard", "amex", "debit",
            "credit", "account", "reference", "transaction",
            "page", "sale", "register", "receipt no", "serial",
            "or no", "order no", "invoice no", "bill no",
            "official receipt", "sales invoice", "print", "online"
        )
        for (line in lines.take(10)) {
            val cleaned = line.replace(Regex("[^a-zA-Z0-9\\s&.,'()-]"), "").trim()
            if (cleaned.length < 3 || cleaned.length > 60) continue
            val lower = cleaned.lowercase()
            if (skipWords.any { lower.startsWith(it) }) continue
            if (skipWords.any { lower.contains(" $it") }) continue
            val letterCount = cleaned.count { it.isLetter() }
            val digitCount = cleaned.count { it.isDigit() }
            if (letterCount < 3) continue
            val digitRatio = digitCount.toFloat() / cleaned.length
            if (digitRatio < 0.3f) return cleaned
        }
        for (line in lines.take(3)) {
            val cleaned = line.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim()
            if (cleaned.length >= 3) return cleaned.take(50)
        }
        return "Unknown"
    }

    private fun extractDate(lines: List<String>): Long {
        val datePatterns = listOf(
            Regex("""(\d{1,2})\s*[/\-.]\s*(\d{1,2})\s*[/\-.]\s*(\d{2,4})"""),
            Regex("""(\d{4})\s*[/\-.]\s*(\d{1,2})\s*[/\-.]\s*(\d{1,2})""")
        )
        val formatsMMDD = listOf(
            "MM/dd/yyyy", "M/dd/yyyy", "MM/d/yyyy", "M/d/yyyy",
            "MM-dd-yyyy", "M-dd-yyyy",
            "MM.dd.yyyy", "M.d.yyyy",
            "MM/dd/yy", "MM-dd-yy"
        )
        val formatsDDMM = listOf(
            "dd/MM/yyyy", "d/MM/yyyy", "dd/M/yyyy", "d/M/yyyy",
            "dd-MM-yyyy", "d-MM-yyyy"
        )
        val formatsText = listOf(
            "MMM dd, yyyy", "MMMM dd, yyyy", "MMM dd yyyy", "MMMM dd yyyy",
            "dd MMM yyyy", "dd MMMM yyyy", "dd-MMM-yyyy",
            "yyyy MMM dd", "yyyy MMMM dd"
        )

        for (line in lines) {
            val cleaned = line.replace(Regex("[•\\-–—|_\"'&#*@!~`]"), " ").trim()
            for (pattern in datePatterns) {
                val matches = pattern.findAll(cleaned)
                for (match in matches) {
                    val groups = match.groupValues.drop(1)
                    for (formats in listOf(formatsMMDD, formatsDDMM)) {
                        for (format in formats) {
                            try {
                                val sdf = SimpleDateFormat(format, Locale.US)
                                sdf.isLenient = false
                                val parts = if (format.contains("yyyy")) {
                                    if (groups[2].length == 4) groups
                                    else listOf(groups[0], groups[1], "20${groups[2]}")
                                } else groups
                                val d = sdf.parse(parts.joinToString("/"))
                                if (d != null && d.time > 946684800000L) return d.time
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }

        for (line in lines) {
            val cleaned = line.replace(Regex("[•\\-–—|_\"'&#*@!~`,]"), " ").trim()
            for (format in formatsText) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    sdf.isLenient = false
                    val d = sdf.parse(cleaned)
                    if (d != null && d.time > 946684800000L) return d.time
                } catch (_: Exception) { }
                val words = cleaned.split(" ")
                for (i in 0..words.size - 3) {
                    val chunk = words.subList(i, i + 3).joinToString(" ")
                    try {
                        val sdf = SimpleDateFormat(format, Locale.US)
                        sdf.isLenient = false
                        val d = sdf.parse(chunk)
                        if (d != null && d.time > 946684800000L) return d.time
                    } catch (_: Exception) { }
                }
            }
        }
        return System.currentTimeMillis()
    }

    private fun extractItems(lines: List<String>): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()
        val priceRegex = Regex("""(\d+[.,]\d{2})""")
        val skipWords = setOf(
            "total", "subtotal", "tax", "vat", "cash", "change", "due",
            "tender", "amount", "balance", "discount", "charge", "fee"
        )

        var startIndex = 0
        for ((i, line) in lines.withIndex()) {
            val lower = line.lowercase()
            if (lower.contains(Regex("qty|quantity|description|price|item"))) {
                if (lower.contains("total")) break
                startIndex = i + 1
                break
            }
        }

        for (i in startIndex until lines.size) {
            val line = lines[i]
            val lower = line.lowercase()
            if (skipWords.any { lower.contains(it) }) break
            if (line.length < 3) continue
            val priceMatch = priceRegex.find(line)
            if (priceMatch != null) {
                val name = line.replace(priceMatch.value, "").trim()
                    .replace(Regex("[^a-zA-Z0-9\\s]"), "").trim()
                val price = priceMatch.value.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (name.isNotEmpty() && price > 0 && price < 100000) {
                    items.add(ReceiptItem(name = name, price = price))
                }
            }
        }
        return items
    }

    private fun extractSubtotal(lines: List<String>): Double {
        return extractValue(lines, "subtotal")
    }

    private fun extractTax(lines: List<String>): Double {
        return extractValue(lines, "tax|vat|gst|pst|hst")
    }

    private fun extractValue(lines: List<String>, keyword: String): Double {
        val regex = Regex("""(?:$keyword)[\s:;]*[₱Pp]?\s*([\d,]+\.\d{2})""", RegexOption.IGNORE_CASE)
        for (line in lines) {
            val match = regex.find(line)
            if (match != null) {
                return match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            }
        }
        return 0.0
    }

    private fun extractTotal(lines: List<String>): Double {
        val excludeKeywords = listOf("vat", "tax", "gst", "hst", "pst", "subtotal", "discount", "service charge")
        val currencyPrefix = """[₱Pp]?"""
        val decimalPrice = """([\d,]+\.\d{2})"""
        val wholePrice = """([\d,]+)\s*$"""

        val passes = listOf(
            listOf("total", "amount due", "balance due", "grand total", "total amount",
                   "total due", "total sale", "sale amount", "net total",
                   "amount payable", "total payable", "net amount",
                   "total price", "total amt", "total:"),
            listOf("amount", "due", "payable"),
            listOf("total")
        )

        for (pass in passes) {
            val regex = Regex(
                """(?:${pass.joinToString("|")})[\s:;]*${currencyPrefix}\s*${decimalPrice}""",
                RegexOption.IGNORE_CASE
            )
            for (line in lines.reversed()) {
                val lower = line.lowercase()
                if (excludeKeywords.any { lower.contains(it) }) continue
                val match = regex.find(line)
                if (match != null) {
                    val v = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                    if (v > 0) return v
                }
            }
        }

        val allPrices = Regex("""(\d+[.,]\d{2})""").findAll(lines.joinToString(" "))
            .map { it.value.replace(",", ".").toDouble() }
            .filter { it > 0 && it < 1000000 }
            .toList()
        if (allPrices.isNotEmpty()) return allPrices.last()
        return 0.0
    }

    private fun extractInvoiceNumber(lines: List<String>): String {
        val regex = Regex(
            """(?:invoice no|receipt no|or no|sales invoice|invoice#|receipt#|inv no|inv#|ref no|order no|bill no|transaction no|serial no|reference no|tran no|si no|si#)[\s:#\-]*(\w[\w\-/]+)""",
            RegexOption.IGNORE_CASE
        )
        for (line in lines) {
            val match = regex.find(line)
            if (match != null) {
                val num = match.groupValues[1].trim()
                if (num.length in 3..30) return num
            }
        }
        return ""
    }

    private fun calculateConfidence(merchant: String, total: Double, date: Long, items: List<ReceiptItem>): Float {
        var score = 0f
        if (merchant.isNotEmpty() && merchant != "Unknown") score += 0.3f
        if (total > 0) score += 0.3f
        if (date > 946684800000L) score += 0.2f
        if (items.isNotEmpty()) score += 0.2f
        return score
    }

    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH"))
        return format.format(amount)
    }

    fun formatDate(date: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US)
        return sdf.format(java.util.Date(date))
    }
}
