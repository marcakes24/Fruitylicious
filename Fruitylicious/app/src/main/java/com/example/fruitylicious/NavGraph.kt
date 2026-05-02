package com.example.fruitylicious

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fruitylicious.ui.auth.LoginScreen

import com.example.fruitylicious.ui.admin.dashboard.AdminDashboardScreen
import com.example.fruitylicious.ui.admin.ingredients.ManageIngredientsScreen
import com.example.fruitylicious.ui.admin.products.ManageProductsScreen
import com.example.fruitylicious.ui.admin.recipes.RecipeManagementScreen
import com.example.fruitylicious.ui.admin.reports.ReportsScreen
import com.example.fruitylicious.ui.admin.reports.SalesSummaryScreen as AdminSalesSummaryScreen
import com.example.fruitylicious.ui.admin.staffmanagement.StaffLogScreen as AdminStaffLogScreen
import com.example.fruitylicious.ui.admin.system.AuditLogScreen
import com.example.fruitylicious.ui.admin.users.UserManagementScreen

import com.example.fruitylicious.ui.shared.SharedScreenMode
import com.example.fruitylicious.ui.shared.inventory.InventoryAdjustmentScreen
import com.example.fruitylicious.ui.shared.inventory.InventoryMonitoringScreen
import com.example.fruitylicious.ui.shared.notifications.NotificationsScreen as SharedNotificationsScreen
import com.example.fruitylicious.ui.shared.queue.QueueScreen
import com.example.fruitylicious.ui.shared.restock.RestockScreen
import com.example.fruitylicious.ui.shared.transaction.TransactionHistoryScreen as SharedTransactionHistoryScreen
import com.example.fruitylicious.ui.shared.waste.WasteManagementScreen

import com.example.fruitylicious.ui.staff.dashboard.StaffDashboardScreen
import com.example.fruitylicious.ui.staff.pos.CheckoutScreen
import com.example.fruitylicious.ui.staff.pos.PosScreen
import com.example.fruitylicious.ui.staff.sales.SalesSummaryScreen as StaffSalesSummaryScreen
import com.example.fruitylicious.ui.staff.stafflog.StaffLogScreen as StaffTimeLogScreen

const val LOGIN = "login"

const val STAFF_DASHBOARD = "staff_dashboard"
const val STAFF_POS = "staff_pos"
const val STAFF_CHECKOUT = "staff_checkout"
const val STAFF_RECEIPT = "staff_receipt"
const val STAFF_LOG = "staff_log"
const val STAFF_WASTE_HISTORY = "staff_waste_history"
const val STAFF_INVENTORY = "staff_inventory"
const val STAFF_ADJUSTMENT = "staff_adjustment"
const val STAFF_RESTOCK_HISTORY = "staff_restock_history"
const val STAFF_SALES_SUMMARY = "staff_sales_summary"
const val STAFF_TRANSACTION_HISTORY = "staff_transaction_history"
const val STAFF_QUEUE = "staff_queue"
const val STAFF_NOTIFICATIONS = "staff_notifications"

const val ADMIN_DASHBOARD = "admin_dashboard"
const val ADMIN_PRODUCTS = "admin_products"
const val ADMIN_INGREDIENTS = "admin_ingredients"
const val ADMIN_RECIPES = "admin_recipes"
const val ADMIN_INVENTORY = "admin_inventory"
const val ADMIN_ADJUSTMENT = "admin_adjustment"
const val ADMIN_WASTE_HISTORY = "admin_waste_history"
const val ADMIN_RESTOCK_HISTORY = "admin_restock_history"
const val ADMIN_USERS = "admin_users"
const val ADMIN_STAFF_LOGS = "admin_staff_logs"
const val ADMIN_REPORTS_DASHBOARD = "admin_reports_dashboard"
const val ADMIN_SALES_SUMMARY = "admin_sales_summary"
const val ADMIN_AUDIT_LOGS = "admin_audit_logs"
const val ADMIN_QUEUE = "admin_queue"
const val ADMIN_NOTIFICATIONS = "admin_notifications"

const val TRANSACTION_HISTORY = "transaction_history"

@Composable
fun FruityliciousNavGraph(
    navController: NavHostController = rememberNavController()
) {
    fun safeBack(fallbackRoute: String) {
        val popped = navController.popBackStack()

        if (!popped) {
            navController.navigate(fallbackRoute) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = LOGIN,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        composable(LOGIN) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val destination = if (role.equals("admin", ignoreCase = true)) {
                        ADMIN_DASHBOARD
                    } else {
                        STAFF_DASHBOARD
                    }

                    navController.navigate(destination) {
                        popUpTo(LOGIN) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(STAFF_DASHBOARD) {
            StaffDashboardScreen(
                navController = navController
            )
        }

        composable(STAFF_POS) {
            PosScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onBack = {
                    safeBack(STAFF_DASHBOARD)
                }
            )
        }

        composable(STAFF_CHECKOUT) {
            CheckoutScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(STAFF_CHECKOUT) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onBack = {
                    safeBack(STAFF_POS)
                }
            )
        }

        composable(STAFF_LOG) {
            StaffTimeLogScreen(
                navController = navController
            )
        }

        composable(STAFF_INVENTORY) {
            InventoryMonitoringScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF
            )
        }

        composable(STAFF_ADJUSTMENT) {
            InventoryAdjustmentScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF
            )
        }

        composable(STAFF_RESTOCK_HISTORY) {
            RestockScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF
            )
        }

        composable(STAFF_WASTE_HISTORY) {
            WasteManagementScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF
            )
        }

        composable(STAFF_SALES_SUMMARY) {
            StaffSalesSummaryScreen(
                navController = navController
            )
        }

        composable(STAFF_TRANSACTION_HISTORY) {
            SharedTransactionHistoryScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF
            )
        }

        composable(STAFF_QUEUE) {
            QueueScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF
            )
        }

        composable(STAFF_NOTIFICATIONS) {
            SharedNotificationsScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF
            )
        }

        composable(ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                navController = navController
            )
        }

        composable(ADMIN_PRODUCTS) {
            ManageProductsScreen(
                navController = navController
            )
        }

        composable(ADMIN_INGREDIENTS) {
            ManageIngredientsScreen(
                navController = navController
            )
        }

        composable(ADMIN_RECIPES) {
            RecipeManagementScreen(
                navController = navController
            )
        }

        composable(ADMIN_INVENTORY) {
            InventoryMonitoringScreen(
                navController = navController,
                mode = SharedScreenMode.ADMIN
            )
        }

        composable(ADMIN_ADJUSTMENT) {
            InventoryAdjustmentScreen(
                navController = navController,
                mode = SharedScreenMode.ADMIN
            )
        }

        composable(ADMIN_WASTE_HISTORY) {
            WasteManagementScreen(
                navController = navController,
                mode = SharedScreenMode.ADMIN
            )
        }

        composable(ADMIN_RESTOCK_HISTORY) {
            RestockScreen(
                navController = navController,
                mode = SharedScreenMode.ADMIN
            )
        }

        composable(ADMIN_USERS) {
            UserManagementScreen(
                navController = navController
            )
        }

        composable(ADMIN_STAFF_LOGS) {
            AdminStaffLogScreen(
                navController = navController
            )
        }

        composable(ADMIN_REPORTS_DASHBOARD) {
            ReportsScreen(
                navController = navController
            )
        }

        composable(ADMIN_AUDIT_LOGS) {
            AuditLogScreen(
                navController = navController
            )
        }

        composable(ADMIN_SALES_SUMMARY) {
            AdminSalesSummaryScreen(
                navController = navController
            )
        }

        composable(ADMIN_QUEUE) {
            QueueScreen(
                navController = navController,
                mode = SharedScreenMode.ADMIN
            )
        }

        composable(ADMIN_NOTIFICATIONS) {
            SharedNotificationsScreen(
                navController = navController,
                mode = SharedScreenMode.ADMIN
            )
        }

        composable(TRANSACTION_HISTORY) {
            SharedTransactionHistoryScreen(
                navController = navController,
                mode = SharedScreenMode.ADMIN
            )
        }
    }
}