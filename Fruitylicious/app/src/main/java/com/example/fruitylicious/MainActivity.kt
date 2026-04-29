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

import com.example.fruitylicious.GuestsScreen //
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
        // ✅ Login Screen
        composable("login") {
            FruityliciousLoginScreen(navController)
        }

        // ✅ Guest Screen (Added this)
        composable("guests_screen") {
            GuestsScreen(navController)
        }

        // STAFF ROUTES
        composable("home") {
            MainScaffold(navController, "home")
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
        composable("SalesSummary") {
            MainScaffold(navController, "SalesSummary")
        }

        // ADMIN ROUTES
        composable("admin_home") {
            AdminScaffold(navController, "admin_home")
        }
        composable("admin_pos") {
            AdminScaffold(navController, "pos")
        }
        composable("admin_transacHistory") {
            AdminScaffold(navController, "transacHistory")
        }
        composable("admin_checkout") {
            AdminScaffold(navController, "checkout")
        }
        composable("admin_restock") {
            AdminScaffold(navController, "restock")
        }
        composable("admin_notification") {
            AdminScaffold(navController, "notification")
        }
        composable("admin_waste_management") {
            AdminScaffold(navController, "waste_management")
        }
        composable("admin_inventory_adjustment") {
            AdminScaffold(navController, "inventory_adjustment")
        }
        composable("admin_SalesSummary") {
            AdminScaffold(navController, "SalesSummary")
        }
        // Staff screens
        composable("home")              { MainScaffold(navController, "home") }
        composable("transacHistory")    { MainScaffold(navController, "transacHistory") }
        composable("pos")               { MainScaffold(navController, "pos") }
        composable("checkout")          { MainScaffold(navController, "checkout") }
        composable("restock")           { MainScaffold(navController, "restock") }
        composable("notification")      { MainScaffold(navController, "notification") }
        composable("waste_management")  { MainScaffold(navController, "waste_management") }
        composable("inventory_adjustment") { MainScaffold(navController, "inventory_adjustment") }
        composable("SalesSummary")      { MainScaffold(navController, "SalesSummary") }

        // Admin screens
        composable("admin_home")                    { AdminScaffold(navController, "admin_home") }
        composable("admin_pos")                     { AdminScaffold(navController, "pos") }
        composable("admin_transacHistory")          { AdminScaffold(navController, "transacHistory") }
        composable("admin_checkout")                { AdminScaffold(navController, "checkout") }
        composable("admin_restock")                 { AdminScaffold(navController, "restock") }
        composable("admin_notification")            { AdminScaffold(navController, "notification") }
        composable("admin_waste_management")        { AdminScaffold(navController, "waste_management") }
        composable("admin_inventory_adjustment")    { AdminScaffold(navController, "inventory_adjustment") }
        composable("admin_SalesSummary")            { AdminScaffold(navController, "SalesSummary") }
    }
}



@Composable
fun MainScaffold(navController: NavController, startScreen: String) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            StaffSideBarContent(navController, drawerState, scope)
        }
    ) {
        when (startScreen) {
            "home"              -> StaffDashboardScreen(navController, drawerState, scope)
            "transacHistory"    -> TransactionHistoryScreen(navController, drawerState, scope)
            "pos"               -> POSScreen(navController, drawerState, scope)
            "checkout"          -> CheckoutScreen(navController, drawerState, scope)
            "restock"           -> RestockScreen(navController, drawerState, scope)
            "notification"      -> NotificationsScreen(navController, drawerState, scope)
            "SalesSummary"      -> SalesSummaryScreen(navController, drawerState, scope)
            "waste_management"  -> WasteManagementScreen(navController, drawerState, scope)
            "inventory_adjustment" -> InventoryAdjustmentScreen(navController, drawerState, scope)
        }
    }
}

@Composable
fun AdminScaffold(navController: NavController, startScreen: String) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminSideBarContent(navController, drawerState, scope)  // updated file
        }
    ) {
        when (startScreen) {
            "admin_home"           -> AdminDashboardScreen(navController, drawerState, scope) // updated file
            "pos"               -> POSScreen(navController, drawerState, scope)
            "checkout"          -> CheckoutScreen(navController, drawerState, scope)
            "restock"           -> RestockScreen(navController, drawerState, scope)
            "notification"      -> NotificationsScreen(navController, drawerState, scope)
            "SalesSummary"      -> SalesSummaryScreen(navController, drawerState, scope)
            "waste_management"  -> WasteManagementScreen(navController, drawerState, scope)
            "inventory_adjustment"  -> InventoryAdjustmentScreen(navController, drawerState, scope)
            "transacHistory"    -> TransactionHistoryScreen(navController, drawerState, scope)
        }
    }
}