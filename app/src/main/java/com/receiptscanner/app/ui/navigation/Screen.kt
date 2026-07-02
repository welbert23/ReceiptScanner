package com.receiptscanner.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Scan : Screen("scan")
    data object ReceiptList : Screen("receipt_list")
    data object ReceiptDetail : Screen("receipt_detail/{receiptId}") {
        fun createRoute(receiptId: Long) = "receipt_detail/$receiptId"
    }
    data object Settings : Screen("settings")
}
