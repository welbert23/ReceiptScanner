package com.receiptscanner.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantName: String = "",
    val date: Long = System.currentTimeMillis(),
    val total: Double = 0.0,
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val category: String = "",
    val items: String = "",
    val notes: String = "",
    val rawText: String = "",
    val imagePath: String = "",
    val invoiceNumber: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ReceiptItem(
    val name: String,
    val price: Double,
    val quantity: Int = 1
)

data class ScanResult(
    val rawText: String,
    val merchantName: String,
    val total: Double,
    val subtotal: Double,
    val tax: Double,
    val date: Long,
    val items: List<ReceiptItem>,
    val invoiceNumber: String = "",
    val confidence: Float
)
