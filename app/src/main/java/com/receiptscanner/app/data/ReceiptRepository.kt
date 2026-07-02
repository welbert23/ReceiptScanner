package com.receiptscanner.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class ReceiptRepository(private val receiptDao: ReceiptDao) {
    val allReceipts: Flow<List<Receipt>> = receiptDao.getAllReceipts()
    val totalSpent: Flow<Double> = receiptDao.getTotalSpent()
    val receiptCount: Flow<Int> = receiptDao.getReceiptCount()
    val allCategories: Flow<List<String>> = receiptDao.getAllCategories()

    suspend fun insert(receipt: Receipt): Long = receiptDao.insertReceipt(receipt)
    suspend fun update(receipt: Receipt) = receiptDao.updateReceipt(receipt)
    suspend fun delete(receipt: Receipt) = receiptDao.deleteReceipt(receipt)
    suspend fun getById(id: Long): Receipt? = receiptDao.getReceiptById(id)

    companion object {
        @Volatile
        private var INSTANCE: ReceiptRepository? = null

        fun getInstance(context: Context): ReceiptRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ReceiptRepository(AppDatabase.getInstance(context).receiptDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
