package com.receiptscanner.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.receiptscanner.app.data.Receipt
import com.receiptscanner.app.util.MainViewModel
import com.receiptscanner.app.util.PdfExporter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    receiptId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var receipt by remember { mutableStateOf<Receipt?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    var editMerchant by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }
    var editItems by remember { mutableStateOf("") }

    LaunchedEffect(receiptId) {
        viewModel.getReceiptById(receiptId) { r ->
            receipt = r
            r?.let {
                editMerchant = it.merchantName
                editCategory = it.category
                editNotes = it.notes
                editItems = it.items
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(receipt?.merchantName ?: "Receipt Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            receipt?.let { r ->
                                val updated = r.copy(
                                    merchantName = editMerchant.trim(),
                                    category = editCategory.trim(),
                                    notes = editNotes.trim(),
                                    items = editItems.trim()
                                )
                                viewModel.updateReceipt(updated)
                                receipt = updated
                                isEditing = false
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    } else {
                        IconButton(onClick = {
                            receipt?.let { r ->
                                editMerchant = r.merchantName
                                editCategory = r.category
                                editNotes = r.notes
                                editItems = r.items
                                isEditing = true
                            }
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                    IconButton(onClick = {
                        receipt?.let { r ->
                            try {
                                val pdfFile = PdfExporter().exportReceipt(context, r)
                                val uri = FileProvider.getUriForFile(
                                    context, "${context.packageName}.fileprovider", pdfFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
                            } catch (_: Exception) { }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (receipt == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val r = receipt!!
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (isEditing) {
                            OutlinedTextField(
                                value = editMerchant,
                                onValueChange = { editMerchant = it },
                                label = { Text("Merchant") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Store, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(r.merchantName.ifEmpty { "Unknown Merchant" },
                                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                        }

                        if (isEditing) {
                            OutlinedTextField(
                                value = editCategory,
                                onValueChange = { editCategory = it },
                                label = { Text("Category") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                        } else {
                            DetailRow("Date", viewModel.formatDate(r.date))
                            DetailRow("Category", r.category.ifEmpty { "Uncategorized" })
                        }

                        if (r.invoiceNumber.isNotBlank()) DetailRow("Invoice #", r.invoiceNumber)

                        if (!isEditing) {
                            if (r.subtotal > 0) DetailRow("Subtotal", viewModel.formatCurrency(r.subtotal))
                            if (r.tax > 0) DetailRow("Tax", viewModel.formatCurrency(r.tax))

                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text(viewModel.formatCurrency(r.total),
                                    fontWeight = FontWeight.Bold, fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                if (r.imagePath.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Receipt Image", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                            ComposeImage(
                                bitmap = android.graphics.BitmapFactory.decodeFile(r.imagePath).asImageBitmap(),
                                contentDescription = "Receipt",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                if (isEditing || r.items.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Items", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editItems,
                                    onValueChange = { editItems = it },
                                    label = { Text("Items (one per line)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3
                                )
                            } else {
                                r.items.split("\n").forEach { item ->
                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("•  $item", fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (isEditing || r.notes.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Notes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editNotes,
                                    onValueChange = { editNotes = it },
                                    label = { Text("Notes") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
                            } else {
                                Text(r.notes, fontSize = 14.sp)
                            }
                        }
                    }
                }

                if (!isEditing) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            try {
                                val pdfFile = PdfExporter().exportReceipt(context, r)
                                val uri = FileProvider.getUriForFile(
                                    context, "${context.packageName}.fileprovider", pdfFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Receipt"))
                            } catch (_: Exception) { }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export as PDF")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.outline)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
