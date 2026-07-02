package com.receiptscanner.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.receiptscanner.app.ui.screens.*
import com.receiptscanner.app.util.MainViewModel

@Composable
fun NavGraph(navController: NavHostController, viewModel: MainViewModel) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                onNavigateToList = { navController.navigate(Screen.ReceiptList.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onReceiptClick = { id -> navController.navigate(Screen.ReceiptDetail.createRoute(id)) }
            )
        }
        composable(Screen.Scan.route) {
            ScanScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSaveComplete = { id ->
                    navController.navigate(Screen.ReceiptDetail.createRoute(id)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
        composable(Screen.ReceiptList.route) {
            ReceiptListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onReceiptClick = { id -> navController.navigate(Screen.ReceiptDetail.createRoute(id)) }
            )
        }
        composable(
            route = Screen.ReceiptDetail.route,
            arguments = listOf(navArgument("receiptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val receiptId = backStackEntry.arguments?.getLong("receiptId") ?: 0L
            ReceiptDetailScreen(
                receiptId = receiptId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
