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
        composable("staff_dashboard") {
            MainScaffold(navController, "staff_dashboard")
        }
        composable("transacHistory") {
            MainScaffold(navController, "transacHistory")
        }
        composable("pos") {
            MainScaffold(navController, "pos")
        }
        composable("checkout") {
            MainScaffold(navController, "checkout")
        }
        composable("restock") {
            MainScaffold(navController, "restock")
        }
        composable("notification") {
            MainScaffold(navController, "notification")
        }
        composable("waste_management") {
            MainScaffold(navController, "waste_management")
        }
        composable("inventory_adjustment") {
            MainScaffold(navController, "inventory_adjustment")
        }
        composable("sales_summary") {
            MainScaffold(navController, "sales_summary")
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
        // Here is where the specific screen is loaded based on the Sidebar route
        when (startScreen) {
            "home"              -> StaffDashboardScreen(navController, drawerState, scope)
            "inventory"         -> InventoryScreen(navController, drawerState, scope)
            "transacHistory"    -> TransactionHistoryScreen(navController, drawerState, scope)
            "pos"               -> POSScreen(navController, drawerState, scope)
            "checkout"          -> CheckoutScreen(navController, drawerState, scope)
            "restock"           -> RestockScreen(navController, drawerState, scope)
            "notification"      -> NotificationsScreen(navController, drawerState, scope)
            "sales_summary"      -> SalesSummaryScreen(navController, drawerState, scope)
            "waste_management"  -> WasteManagementScreen(navController, drawerState, scope)
            "inventory_adjustment"  -> InventoryAdjustmentScreen(navController, drawerState, scope)
        }
    }
}