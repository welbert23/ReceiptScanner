package com.receiptscanner.app.util

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.receiptscanner.app.data.Receipt
import com.receiptscanner.app.data.ReceiptRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReceiptRepository.getInstance(application)
    private val parser = ReceiptParser()
    private val prefs = application.getSharedPreferences("receipt_scanner", android.content.Context.MODE_PRIVATE)

    val allReceipts = repository.allReceipts
    val totalSpent = repository.totalSpent
    val receiptCount = repository.receiptCount
    val allCategories = repository.allCategories

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _receipts = MutableStateFlow<List<Receipt>>(emptyList())
    val receipts: StateFlow<List<Receipt>> = _receipts.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _scanMode = MutableStateFlow(prefs.getString("scan_mode", "offline") ?: "offline")
    val scanMode: StateFlow<String> = _scanMode.asStateFlow()

    init {
        viewModelScope.launch {
            combine(allReceipts, _searchQuery, _selectedCategory) { receipts, query, category ->
                receipts.filter { r ->
                    val matchesQuery = query.isEmpty() ||
                            r.merchantName.contains(query, ignoreCase = true) ||
                            r.category.contains(query, ignoreCase = true)
                    val matchesCategory = category.isEmpty() || r.category == category
                    matchesQuery && matchesCategory
                }
            }.collect { _receipts.value = it }
        }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCategoryFilter(category: String) { _selectedCategory.value = category }

    fun saveGeminiApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
        _geminiApiKey.value = key
    }

    fun setScanMode(mode: String) {
        prefs.edit().putString("scan_mode", mode).apply()
        _scanMode.value = mode
    }

    fun saveReceipt(receipt: Receipt, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insert(receipt)
            onSaved(id)
        }
    }

    fun updateReceipt(receipt: Receipt) {
        viewModelScope.launch {
            repository.update(receipt)
        }
    }

    fun getReceiptById(id: Long, callback: (Receipt?) -> Unit) {
        viewModelScope.launch {
            callback(repository.getById(id))
        }
    }

    fun formatCurrency(amount: Double): String = parser.formatCurrency(amount)
    fun formatDate(date: Long): String = parser.formatDate(date)
}
