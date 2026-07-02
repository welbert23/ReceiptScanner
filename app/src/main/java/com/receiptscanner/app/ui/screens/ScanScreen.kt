package com.receiptscanner.app.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
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
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.receiptscanner.app.data.Receipt
import com.receiptscanner.app.data.ReceiptItem
import com.receiptscanner.app.util.GeminiHelper
import com.receiptscanner.app.util.MainViewModel
import com.receiptscanner.app.util.OcrHelper
import com.receiptscanner.app.util.ReceiptParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onSaveComplete: (Long) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val parser = remember { ReceiptParser() }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scannedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var capturedImagePath by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var parsedItems by remember { mutableStateOf<List<ReceiptItem>>(emptyList()) }
    val scanMode by viewModel.scanMode.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setScannerMode(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setGalleryImportAllowed(false)
            .build()
    }
    val scannerClient = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isProcessing = true
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
                if (bitmap == null) {
                    isProcessing = false
                    return@launch
                }
                val savedPath = withContext(Dispatchers.IO) {
                    saveBitmapToFile(context, bitmap)
                }
                capturedImagePath = savedPath
                if (scanMode == "ai" && geminiApiKey.isNotBlank()) {
                    val gemini = GeminiHelper()
                    val geminiResult = gemini.parseReceipt(bitmap, geminiApiKey)
                    scannedText = geminiResult.rawText
                    merchant = geminiResult.merchantName
                    total = String.format("%.2f", geminiResult.total)
                    val sdf = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US)
                    date = sdf.format(java.util.Date(geminiResult.date))
                    invoiceNumber = geminiResult.invoiceNumber
                    parsedItems = geminiResult.items
                } else {
                    val ocrHelper = OcrHelper()
                    val text = ocrHelper.recognizeText(bitmap)
                    scannedText = text
                    val result = parser.parse(text)
                    merchant = result.merchantName
                    total = String.format("%.2f", result.total)
                    val sdf = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US)
                    date = sdf.format(java.util.Date(result.date))
                    invoiceNumber = result.invoiceNumber
                    parsedItems = result.items
                }
                capturedBitmap = bitmap
            } catch (e: Exception) {
                    scannedText = "Error: ${e.message}"
                }
                isProcessing = false
            }
        }

    val scannerLauncher = rememberLauncherForActivityResult<IntentSenderRequest, ActivityResult>(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uri = scanResult?.pages?.firstOrNull()?.imageUri
            if (uri != null) {
                scope.launch {
                    isProcessing = true
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                BitmapFactory.decodeStream(input)
                            } ?: return@withContext null
                        }
                        if (bitmap != null) {
                            val savedPath = withContext(Dispatchers.IO) {
                                saveBitmapToFile(context, bitmap)
                            }
                            capturedImagePath = savedPath
                            if (scanMode == "ai" && geminiApiKey.isNotBlank()) {
                                val gemini = GeminiHelper()
                                val geminiResult = gemini.parseReceipt(bitmap, geminiApiKey)
                                scannedText = geminiResult.rawText
                                merchant = geminiResult.merchantName
                                total = String.format("%.2f", geminiResult.total)
                                val sdf = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US)
                                date = sdf.format(java.util.Date(geminiResult.date))
                                invoiceNumber = geminiResult.invoiceNumber
                                parsedItems = geminiResult.items
                            } else {
                                val ocrHelper = OcrHelper()
                                val text = ocrHelper.recognizeText(bitmap)
                                scannedText = text
                                val result = parser.parse(text)
                                merchant = result.merchantName
                                total = String.format("%.2f", result.total)
                                val sdf = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US)
                                date = sdf.format(java.util.Date(result.date))
                                invoiceNumber = result.invoiceNumber
                                parsedItems = result.items
                            }
                            capturedBitmap = bitmap
                        }
                    } catch (e: Exception) {
                        scannedText = "Error: ${e.message}"
                    }
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (capturedBitmap != null) "Review Scan" else "Scan Receipt") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (capturedBitmap == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Place your receipt on a flat surface",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The scanner will auto-detect and crop it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = {
                            activity?.let { act ->
                                scannerClient.getStartScanIntent(act)
                                    .addOnSuccessListener { intentSender ->
                                        scannerLauncher.launch(
                                            androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        scannedText = "Scanner unavailable: ${e.message}"
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Scanner")
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pick from Gallery")
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = if (scanMode == "ai") "Mode: AI (Gemini)" else "Mode: Offline OCR",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Change in Settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Scanned Receipt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Auto-detected values — edit if needed", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(16.dp))

                if (capturedImagePath.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Receipt Image", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                            ComposeImage(
                                bitmap = BitmapFactory.decodeFile(capturedImagePath).asImageBitmap(),
                                contentDescription = "Receipt",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant Name") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("Invoice / Receipt #") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = total,
                    onValueChange = { total = it },
                    label = { Text("Total Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (parsedItems.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Detected Items", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            parsedItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.name, modifier = Modifier.weight(1f))
                                    Text(String.format("Php %.2f", item.price),
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            capturedBitmap = null
                            scannedText = ""
                            capturedImagePath = ""
                            merchant = ""
                            total = ""
                            date = ""
                            category = ""
                            notes = ""
                            invoiceNumber = ""
                            parsedItems = emptyList()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Re-scan")
                    }
                    Button(
                        onClick = {
                            val totalVal = total.replace("[^\\d.]".toRegex(), "").toDoubleOrNull() ?: 0.0
                            val sdf = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US)
                            val dateVal = try { sdf.parse(date)?.time ?: System.currentTimeMillis() } catch (_: Exception) { System.currentTimeMillis() }
                            val itemsText = parsedItems.joinToString("\n") { "${it.name} - Php ${String.format("%.2f", it.price)}" }
                            val receipt = Receipt(
                                merchantName = merchant,
                                date = dateVal,
                                total = totalVal,
                                category = category,
                                items = itemsText,
                                notes = notes,
                                rawText = scannedText,
                                imagePath = capturedImagePath,
                                invoiceNumber = invoiceNumber
                            )
                            viewModel.saveReceipt(receipt) { id ->
                                onSaveComplete(id)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Save Receipt")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun saveBitmapToFile(context: android.content.Context, bitmap: Bitmap): String {
    val dir = File(context.filesDir, "receipts")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return file.absolutePath
}
