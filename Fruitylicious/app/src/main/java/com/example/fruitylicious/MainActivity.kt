package com.example.fruitylicious

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

import com.example.fruitylicious.ui.admin.dashboard.AdminDashboardScreen
import com.example.fruitylicious.ui.admin.ingredients.ManageIngredientsScreen
import com.example.fruitylicious.ui.admin.products.ManageProductsScreen
import com.example.fruitylicious.ui.auth.FruityliciousLoginScreen
import com.example.fruitylicious.ui.shared.AdminSideBarContent
import com.example.fruitylicious.ui.shared.GuestsScreen
import com.example.fruitylicious.ui.shared.StaffSideBarContent
import com.example.fruitylicious.ui.staff.dashboard.StaffDashboardScreen
import com.example.fruitylicious.ui.staff.pos.CheckoutScreen
import com.example.fruitylicious.ui.staff.pos.POSScreen
import com.example.fruitylicious.ui.staff.transaction.TransactionHistoryScreen
import com.example.fruitylicious.ui.staff.waste.WasteManagementScreen

// Corrected Imports
import com.example.fruitylicious.QueueScreen
import com.example.fruitylicious.UserManagementScreen
import com.example.fruitylicious.ui.admin.reports.ReportsScreen
import com.example.fruitylicious.ui.admin.reports.SalesSummaryScreen
import com.example.fruitylicious.ui.admin.system.AuditLogScreen
import com.example.fruitylicious.ui.shared.AttendanceState
import com.example.fruitylicious.ui.shared.TimeLogScreen
import com.example.fruitylicious.ui.shared.TimeLogUiState

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
        val staffRoutes = listOf(
            "home",
            "transacHistory",
            "pos",
            "checkout",
            "waste_management",
            "queue_screen",
            "time_log",
            "inventory_monitoring",
            "sales_summary"
        )

        staffRoutes.forEach { route ->
            composable(route) { MainScaffold(navController, route) }
        }

        // ADMIN ROUTES
        val adminRoutes = listOf(
            "admin_home",
            "admin_pos",
            "admin_transacHistory",
            "admin_checkout",
            "admin_waste_management",
            "admin_manage_products",
            "admin_manage_ingredients",
            "admin_queue",
            "admin_users",
            "admin_time_log",
            "admin_recipe_management",
            "admin_inventory_monitoring",
            "admin_reports",
            "admin_sales_summary",
            "admin_audit_logs"
        )

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
            "time_log" -> TimeLogRoute(drawerState, scope)
            "inventory_monitoring" -> InventoryMonitoringScreen(navController, drawerState, scope)
            "sales_summary" -> SalesSummaryScreen(navController, drawerState, scope)
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
            "admin_transacHistory" -> TransactionHistoryScreen(navController, drawerState, scope)
            "admin_checkout" -> CheckoutScreen(navController, drawerState, scope)
            "admin_waste_management" -> WasteManagementScreen(navController, drawerState, scope)
            "admin_queue" -> QueueScreen(navController, drawerState, scope)
            "admin_manage_products" -> ManageProductsScreen(drawerState, scope)
            "admin_manage_ingredients" -> ManageIngredientsScreen(drawerState, scope)
            "admin_users" -> UserManagementScreen(navController, drawerState, scope)
            "admin_time_log" -> TimeLogRoute(drawerState, scope)
            "admin_recipe_management" -> RecipeManagementScreen(navController, drawerState, scope)
            "admin_inventory_monitoring" -> InventoryMonitoringScreen(navController, drawerState, scope)
            "admin_reports" -> ReportsScreen(navController, drawerState, scope)
            "admin_sales_summary" -> SalesSummaryScreen(navController, drawerState, scope)
            "admin_audit_logs" -> AuditLogScreen(drawerState)
        }
    }
}

@Composable
fun TimeLogRoute(
    drawerState: androidx.compose.material3.DrawerState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var attendanceState by remember {
        mutableStateOf(AttendanceState.CLOCKED_OUT)
    }

    var clockedInSince by remember {
        mutableStateOf("10:34 AM")
    }

    TimeLogScreen(
        uiState = TimeLogUiState(
            fullName = "Eula Valdez",
            username = "@staff",
            attendanceState = attendanceState,
            clockedInSince = clockedInSince
        ),
        onMenuClick = {
            scope.launch { drawerState.open() }
        },
        onClockActionClick = {
            if (attendanceState == AttendanceState.CLOCKED_OUT) {
                attendanceState = AttendanceState.CLOCKED_IN
                clockedInSince = java.text.SimpleDateFormat(
                    "hh:mm a",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())
            } else {
                attendanceState = AttendanceState.CLOCKED_OUT
            }
        }
    )
}
