package com.example.fruitylicious

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope

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
        // ✅ Login has no drawer
        composable("login") {
            FruityliciousLoginScreen(navController)
        }

        // ✅ All other screens go through MainScaffold
        composable("home") {
            MainScaffold(navController, "home")
        }
        composable("inventory") {
            MainScaffold(navController, "inventory")
        }
        composable("purchased_orders") {
            MainScaffold(navController, "purchased_orders")
        }
        composable("stocks_reports") {
            MainScaffold(navController, "stocks_reports")
        }
        composable("suppliers") {
            MainScaffold(navController, "suppliers")
        }
        composable("stock_alerts") {
            MainScaffold(navController, "stock_alerts")
        }
        composable("stocks_report_main") {
            MainScaffold(navController, "stocks_report_main")
        }
        composable("movements") {
            MainScaffold(navController, "movements")
        }
        composable("add_stocks") {
            MainScaffold(navController, "add_stocks")
        }
    }
}

@Composable
fun MainScaffold(navController: NavController, startScreen: String) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideBarContent(
                navController = navController,
                drawerState = drawerState,
                scope = scope
            )
        }
    ) {
        when (startScreen) {
            "home"              -> MyDashboard(navController, drawerState, scope)
            "inventory"         -> InventoryScreen(navController, drawerState, scope)
            "purchased_orders"  -> PurchasedOrdersScreen(navController, drawerState, scope)
            "stocks_reports"    -> StocksReportScreen(navController, drawerState, scope)
            "suppliers"         -> SuppliersScreen(navController, drawerState, scope)
            "stock_alerts"      -> StockAlerts(navController, drawerState, scope)
            "stocks_report_main"-> StocksReportScreenMain(navController, drawerState, scope)
            "movements" -> MovementsScreen(navController, drawerState, scope)
            "add_stocks" -> AddStocksScreen(navController, drawerState, scope)
        }
    }
}