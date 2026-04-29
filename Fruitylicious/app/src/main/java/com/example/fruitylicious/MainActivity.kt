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

import com.example.fruitylicious.ui.admin.dashboard.AdminDashboardScreen
import com.example.fruitylicious.ui.auth.FruityliciousLoginScreen
import com.example.fruitylicious.ui.shared.AdminSideBarContent
import com.example.fruitylicious.ui.shared.GuestsScreen
import com.example.fruitylicious.ui.shared.StaffSideBarContent
import com.example.fruitylicious.ui.staff.dashboard.StaffDashboardScreen
import com.example.fruitylicious.ui.staff.pos.CheckoutScreen
import com.example.fruitylicious.ui.staff.pos.POSScreen
import com.example.fruitylicious.ui.staff.transaction.TransactionHistoryScreen
import com.example.fruitylicious.ui.staff.waste.WasteManagementScreen

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

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { FruityliciousLoginScreen(navController) }
        composable("guests_screen") { GuestsScreen(navController) }

        // STAFF ROUTES
        val staffRoutes = listOf("home", "transacHistory", "pos", "checkout", "restock",
            "notification", "waste_management", "inventory_adjustment",
            "SalesSummary", "queue_screen")

        staffRoutes.forEach { route ->
            composable(route) { MainScaffold(navController, route) }
        }

        // ADMIN ROUTES
        val adminRoutes = listOf("admin_home", "admin_pos", "admin_transacHistory",
            "admin_checkout", "admin_restock", "admin_notification",
            "admin_waste_management", "admin_inventory_adjustment",
            "admin_SalesSummary", "admin_queue", "admin_users") // ADDED admin_users route

        adminRoutes.forEach { route ->
            composable(route) { AdminScaffold(navController, route) }
        }
    }
}

@Composable
fun MainScaffold(navController: NavController, startScreen: String) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { StaffSideBarContent(navController, drawerState, scope) }
    ) {
        when (startScreen) {
            "home" -> StaffDashboardScreen(navController, drawerState, scope)
            "transacHistory" -> TransactionHistoryScreen(navController, drawerState, scope)
            "pos" -> POSScreen(navController, drawerState, scope)
            "checkout" -> CheckoutScreen(navController, drawerState, scope)
            "waste_management" -> WasteManagementScreen(navController, drawerState, scope)
            "queue_screen" -> QueueScreen(navController, drawerState, scope)
        }
    }
}

@Composable
fun AdminScaffold(navController: NavController, startScreen: String) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { AdminSideBarContent(navController, drawerState, scope) }
    ) {
        when (startScreen) {
            "admin_home" -> AdminDashboardScreen(navController, drawerState, scope)
            "admin_pos" -> POSScreen(navController, drawerState, scope)
            "admin_queue" -> QueueScreen(navController, drawerState, scope)
            "admin_users" -> UserManagementScreen(navController, drawerState, scope) // ADDED this line
        }
    }
}