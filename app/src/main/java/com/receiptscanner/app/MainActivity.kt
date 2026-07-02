package com.receiptscanner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.receiptscanner.app.ui.navigation.NavGraph
import com.receiptscanner.app.ui.theme.ReceiptScannerTheme
import com.receiptscanner.app.util.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReceiptScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReceiptScannerApp()
                }
            }
        }
    }
}

@Composable
fun ReceiptScannerApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    NavGraph(navController = navController, viewModel = viewModel)
}
