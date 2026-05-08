package com.example.fruitylicious

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fruitylicious.ui.auth.LoginScreen
import com.example.fruitylicious.util.SessionManager
import kotlinx.coroutines.delay

import com.example.fruitylicious.ui.admin.dashboard.AdminDashboardScreen
import com.example.fruitylicious.ui.admin.ingredients.ManageIngredientsScreen
import com.example.fruitylicious.ui.admin.products.ManageProductsScreen
import com.example.fruitylicious.ui.admin.recipes.RecipeManagementScreen
import com.example.fruitylicious.ui.admin.settings.BranchSettingsScreen
import com.example.fruitylicious.ui.admin.reports.ReportsScreen
import com.example.fruitylicious.ui.admin.staffmanagement.StaffLogScreen as AdminStaffLogScreen
import com.example.fruitylicious.ui.admin.system.AuditLogScreen
import com.example.fruitylicious.ui.admin.users.UserManagementScreen

import com.example.fruitylicious.ui.shared.GuestsScreen
import com.example.fruitylicious.ui.shared.SharedScreenMode
import com.example.fruitylicious.ui.shared.TimeLogScreen
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

const val LOGIN = "login"

const val STAFF_DASHBOARD = "staff_dashboard"
const val STAFF_POS = "staff_pos"
const val STAFF_CHECKOUT = "staff_checkout"
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
const val ADMIN_AUDIT_LOGS = "admin_audit_logs"
const val ADMIN_QUEUE = "admin_queue"
const val ADMIN_NOTIFICATIONS = "admin_notifications"
const val ADMIN_BRANCH_SETTINGS = "admin_branch_settings"
const val TRANSACTION_HISTORY = "transaction_history"
const val GUEST_SCREEN = "guest_screen"

@Composable
fun FruityliciousNavGraph(
    navController: NavHostController = rememberNavController(),
    sessionManager: SessionManager
) {
    val startDestination = remember {
        if (sessionManager.isLoggedIn() && !sessionManager.isSessionExpired()) {
            if (sessionManager.isAdmin()) {
                ADMIN_DASHBOARD
            } else {
                STAFF_DASHBOARD
            }
        } else {
            LOGIN
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // Check every minute
            if (sessionManager.isLoggedIn() && sessionManager.isSessionExpired()) {
                sessionManager.clearSession()
                navController.navigate(LOGIN) {
                    popUpTo(0)
                }
            }
        }
    }

    fun safeBack(fallbackRoute: String) {
        val popped = navController.popBackStack()

        if (!popped) {
            navController.navigate(fallbackRoute) {
                launchSingleTop = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var lastUpdateTime = 0L
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val currentTime = System.currentTimeMillis()
                        // Only update activity on Press events and throttle to once per 30 seconds
                        if (event.type == PointerEventType.Press && currentTime - lastUpdateTime > 30_000L) {
                            sessionManager.updateActivity()
                            lastUpdateTime = currentTime
                        }
                    }
                }
            }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
        composable(LOGIN) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val destination = if (sessionManager.isAdmin()) {
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
                },
                onGuestClick = {
                    navController.navigate(GUEST_SCREEN)
                }
            )
        }

        composable(GUEST_SCREEN) {
            GuestsScreen(navController = navController)
        }

        composable(STAFF_DASHBOARD) {
            StaffDashboardScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF
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
                    val fallback = if (sessionManager.isAdmin()) {
                        ADMIN_DASHBOARD
                    } else {
                        STAFF_DASHBOARD
                    }
                    safeBack(fallback)
                }
            )
        }

        composable(STAFF_CHECKOUT) {
            CheckoutScreen(
                onNavigate = { route ->
                    val destination = if (route == STAFF_QUEUE && sessionManager.isAdmin()) {
                        ADMIN_QUEUE
                    } else {
                        route
                    }

                    navController.navigate(destination) {
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
            TimeLogScreen(
                navController = navController,
                userName = sessionManager.getUserName(),
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(STAFF_INVENTORY) {
            InventoryMonitoringScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF,
                userName = sessionManager.getUserName(),
                branchName = "Branch ${sessionManager.getBranchId()}",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(STAFF_ADJUSTMENT) {
            InventoryAdjustmentScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF,
                userName = sessionManager.getUserName(),
                branchName = "Branch ${sessionManager.getBranchId()}",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(STAFF_RESTOCK_HISTORY) {
            RestockScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF,
                userName = sessionManager.getUserName(),
                branchName = "Branch ${sessionManager.getBranchId()}",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(STAFF_WASTE_HISTORY) {
            WasteManagementScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF,
                userName = sessionManager.getUserName(),
                branchName = "Branch ${sessionManager.getBranchId()}",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(STAFF_SALES_SUMMARY) {
            StaffSalesSummaryScreen(
                navController = navController,
                staffName = sessionManager.getUserName(),
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(STAFF_TRANSACTION_HISTORY) {
            SharedTransactionHistoryScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF,
                userName = sessionManager.getUserName(),
                branchName = "Branch ${sessionManager.getBranchId()}",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(STAFF_QUEUE) {
            QueueScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF,
                userName = sessionManager.getUserName(),
                branchName = "Branch ${sessionManager.getBranchId()}",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(STAFF_NOTIFICATIONS) {
            SharedNotificationsScreen(
                navController = navController,
                mode = SharedScreenMode.STAFF,
                userName = sessionManager.getUserName(),
                branchName = "Branch ${sessionManager.getBranchId()}",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                navController = navController
            )
        }

        composable(ADMIN_PRODUCTS) {
            ManageProductsScreen(
                navController = navController,
                adminName = sessionManager.getUserName(),
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_INGREDIENTS) {
            ManageIngredientsScreen(
                navController = navController,
                adminName = sessionManager.getUserName(),
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_RECIPES) {
            RecipeManagementScreen(
                navController = navController,
                adminName = sessionManager.getUserName(),
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_INVENTORY) {
            InventoryMonitoringScreen(
                navController = navController,
                mode = SharedScreenMode.OWNER,
                userName = sessionManager.getUserName(),
                branchName = "All",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_ADJUSTMENT) {
            InventoryAdjustmentScreen(
                navController = navController,
                mode = SharedScreenMode.OWNER,
                userName = sessionManager.getUserName(),
                branchName = "All",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_WASTE_HISTORY) {
            WasteManagementScreen(
                navController = navController,
                mode = SharedScreenMode.OWNER,
                userName = sessionManager.getUserName(),
                branchName = "All",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_RESTOCK_HISTORY) {
            RestockScreen(
                navController = navController,
                mode = SharedScreenMode.OWNER,
                userName = sessionManager.getUserName(),
                branchName = "All",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_USERS) {
            UserManagementScreen(
                navController = navController,
                adminName = sessionManager.getUserName(),
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_STAFF_LOGS) {
            AdminStaffLogScreen(
                navController = navController,
                adminName = sessionManager.getUserName(),
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_REPORTS_DASHBOARD) {
            ReportsScreen(
                navController = navController,
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_AUDIT_LOGS) {
            AuditLogScreen(
                navController = navController,
                adminName = sessionManager.getUserName(),
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_QUEUE) {
            QueueScreen(
                navController = navController,
                mode = SharedScreenMode.OWNER,
                userName = sessionManager.getUserName(),
                branchName = "All",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_NOTIFICATIONS) {
            SharedNotificationsScreen(
                navController = navController,
                mode = SharedScreenMode.OWNER,
                userName = sessionManager.getUserName(),
                branchName = "All",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(ADMIN_BRANCH_SETTINGS) {
            BranchSettingsScreen(
                navController = navController,
                adminName = sessionManager.getUserName(),
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(TRANSACTION_HISTORY) {
            SharedTransactionHistoryScreen(
                navController = navController,
                mode = SharedScreenMode.OWNER,
                userName = sessionManager.getUserName(),
                branchName = "All",
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(LOGIN) { popUpTo(0) }
                }
            )
        }
        }
    }
}
