package com.receiptscanner.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.receiptscanner.app.util.MainViewModel
import com.receiptscanner.app.util.PdfExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptListScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onReceiptClick: (Long) -> Unit
) {
    val receipts by viewModel.receipts.collectAsState(emptyList())
    val categories by viewModel.allCategories.collectAsState(emptyList())
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSelecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val context = LocalContext.current

    fun toggleSelect(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelecting) {
                        Text("${selectedIds.size} selected")
                    } else {
                        Text("Receipts")
                    }
                },
                navigationIcon = {
                    if (isSelecting) {
                        IconButton(onClick = {
                            selectedIds = emptySet()
                            isSelecting = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isSelecting) {
                        IconButton(onClick = {
                            selectedIds = if (selectedIds.size == receipts.size) emptySet() else receipts.map { it.id }.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Toggle all")
                        }
                    } else {
                        IconButton(onClick = { isSelecting = true }) {
                            Icon(Icons.Default.Checklist, contentDescription = "Select")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (isSelecting && selectedIds.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 4.dp
                ) {
                    Button(
                        onClick = {
                            val selected = receipts.filter { it.id in selectedIds }
                            try {
                                val pdfFile = PdfExporter().exportMultipleReceipts(context, selected)
                                val uri = FileProvider.getUriForFile(
                                    context, "${context.packageName}.fileprovider", pdfFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Receipts"))
                            } catch (_: Exception) { }
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Download PDF (${selectedIds.size})")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.setSearchQuery(it)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search receipts...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory.isEmpty(),
                        onClick = { viewModel.setCategoryFilter("") },
                        label = { Text("All") }
                    )
                    categories.take(5).forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.setCategoryFilter(category) },
                            label = { Text(category) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (receipts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null,
                            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No receipts found", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(receipts) { receipt ->
                        ReceiptCard(
                            receipt = receipt,
                            viewModel = viewModel,
                            onClick = {
                                if (isSelecting) {
                                    toggleSelect(receipt.id)
                                } else {
                                    onReceiptClick(receipt.id)
                                }
                            },
                            showCheckbox = isSelecting,
                            isSelected = receipt.id in selectedIds,
                            onToggleSelect = { toggleSelect(receipt.id) }
                        )
                    }
                }
            }
        }
    }
}
