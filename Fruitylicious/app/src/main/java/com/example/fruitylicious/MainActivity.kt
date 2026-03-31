package com.example.fruitylicious

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            FruityliciousLoginScreen(navController)
        }
        composable("home") {
            MyDashboard(navController)
        }
        composable("stocks_report") {
            StocksReportScreen(navController)
        }
        composable("stock_alerts") {
            StockAlerts(navController)
        }
        composable("inventory") {
            InventoryScreen(navController)
        }

    }
}