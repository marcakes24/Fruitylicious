package com.example.fruitylicious

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.fruitylicious.ui.admin.dashboard.AdminDashboardScreen
import com.example.fruitylicious.ui.admin.ingredients.IngredientFormScreen
import com.example.fruitylicious.ui.admin.ingredients.IngredientListScreen
import com.example.fruitylicious.ui.admin.inventory.AdminAdjustmentScreen
import com.example.fruitylicious.ui.admin.inventory.AdminInventoryScreen
import com.example.fruitylicious.ui.admin.products.ProductFormScreen
import com.example.fruitylicious.ui.admin.products.ProductListScreen
import com.example.fruitylicious.ui.admin.recipes.RecipeFormScreen
import com.example.fruitylicious.ui.admin.recipes.RecipeListScreen
import com.example.fruitylicious.ui.admin.reports.InventoryReportScreen
import com.example.fruitylicious.ui.admin.reports.ReportsDashboardScreen
import com.example.fruitylicious.ui.admin.reports.RestockReportScreen
import com.example.fruitylicious.ui.admin.reports.SalesReportScreen
import com.example.fruitylicious.ui.admin.reports.TransactionReportScreen
import com.example.fruitylicious.ui.admin.reports.WasteReportScreen
import com.example.fruitylicious.ui.admin.restock.AdminRestockEntryScreen
import com.example.fruitylicious.ui.admin.restock.AdminRestockHistoryScreen
import com.example.fruitylicious.ui.admin.staffmanagement.StaffLogsScreen
import com.example.fruitylicious.ui.admin.staffmanagement.UserFormScreen
import com.example.fruitylicious.ui.admin.staffmanagement.UserListScreen
import com.example.fruitylicious.ui.admin.waste.AdminWasteEntryScreen
import com.example.fruitylicious.ui.admin.waste.AdminWasteHistoryScreen
import com.example.fruitylicious.ui.auth.LoginScreen
import com.example.fruitylicious.ui.staff.adjustment.AdjustmentScreen
import com.example.fruitylicious.ui.staff.dashboard.StaffDashboardScreen
import com.example.fruitylicious.ui.staff.pos.CheckoutScreen
import com.example.fruitylicious.ui.staff.pos.PosScreen
import com.example.fruitylicious.ui.staff.pos.ReceiptScreen
import com.example.fruitylicious.ui.staff.restock.RestockEntryScreen
import com.example.fruitylicious.ui.staff.restock.RestockHistoryScreen
import com.example.fruitylicious.ui.staff.sales.SalesSummaryScreen
import com.example.fruitylicious.ui.staff.stafflog.StaffLogScreen
import com.example.fruitylicious.ui.staff.transactions.TransactionHistoryScreen
import com.example.fruitylicious.ui.staff.waste.WasteEntryScreen
import com.example.fruitylicious.ui.staff.waste.WasteHistoryScreen

const val LOGIN = "login"

const val STAFF_GRAPH = "staff_graph"
const val STAFF_DASHBOARD = "staff_dashboard"
const val STAFF_POS = "staff_pos"
const val STAFF_CHECKOUT = "staff_checkout"
const val STAFF_RECEIPT = "staff_receipt"
const val STAFF_LOG = "staff_log"
const val STAFF_WASTE_ENTRY = "staff_waste_entry"
const val STAFF_WASTE_HISTORY = "staff_waste_history"
const val STAFF_INVENTORY = "staff_inventory"
const val STAFF_ADJUSTMENT = "staff_adjustment"
const val STAFF_RESTOCK_ENTRY = "staff_restock_entry"
const val STAFF_RESTOCK_HISTORY = "staff_restock_history"
const val STAFF_SALES_SUMMARY = "staff_sales_summary"
const val STAFF_TRANSACTION_HISTORY = "staff_transaction_history"

const val ADMIN_GRAPH = "admin_graph"
const val ADMIN_DASHBOARD = "admin_dashboard"
const val ADMIN_PRODUCTS = "admin_products"
const val ADMIN_PRODUCT_FORM = "admin_product_form"
const val ADMIN_INGREDIENTS = "admin_ingredients"
const val ADMIN_INGREDIENT_FORM = "admin_ingredient_form"
const val ADMIN_RECIPES = "admin_recipes"
const val ADMIN_RECIPE_FORM = "admin_recipe_form"
const val ADMIN_INVENTORY = "admin_inventory"
const val ADMIN_ADJUSTMENT = "admin_adjustment"
const val ADMIN_WASTE_ENTRY = "admin_waste_entry"
const val ADMIN_WASTE_HISTORY = "admin_waste_history"
const val ADMIN_RESTOCK_ENTRY = "admin_restock_entry"
const val ADMIN_RESTOCK_HISTORY = "admin_restock_history"
const val ADMIN_USERS = "admin_users"
const val ADMIN_USER_FORM = "admin_user_form"
const val ADMIN_STAFF_LOGS = "admin_staff_logs"
const val ADMIN_REPORTS_DASHBOARD = "admin_reports_dashboard"
const val ADMIN_REPORT_SALES = "admin_report_sales"
const val ADMIN_REPORT_INVENTORY = "admin_report_inventory"
const val ADMIN_REPORT_WASTE = "admin_report_waste"
const val ADMIN_REPORT_RESTOCK = "admin_report_restock"
const val ADMIN_REPORT_TRANSACTIONS = "admin_report_transactions"
const val ADMIN_AUDIT_LOGS = "admin_audit_logs"

@Composable
fun FruityliciousNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = LOGIN
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

        navigation(
            route = STAFF_GRAPH,
            startDestination = STAFF_DASHBOARD
        ) {
            composable(STAFF_DASHBOARD) {
                StaffDashboardScreen(
                    onNavigate = navController::navigate,
                    onLogout = {
                        navController.navigate(LOGIN) {
                            popUpTo(0)
                        }
                    }
                )
            }

            composable(STAFF_POS) {
                PosScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(STAFF_CHECKOUT) {
                CheckoutScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(STAFF_RECEIPT) {
                ReceiptScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(STAFF_LOG) {
                StaffLogScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(STAFF_WASTE_ENTRY) {
                WasteEntryScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(STAFF_WASTE_HISTORY) {
                WasteHistoryScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(STAFF_INVENTORY) {
                InventoryScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(STAFF_ADJUSTMENT) {
                AdjustmentScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(STAFF_RESTOCK_ENTRY) {
                RestockEntryScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(STAFF_RESTOCK_HISTORY) {
                RestockHistoryScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(STAFF_SALES_SUMMARY) {
                SalesSummaryScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(STAFF_TRANSACTION_HISTORY) {
                TransactionHistoryScreen(
                    onBack = navController::popBackStack
                )
            }
        }

        navigation(
            route = ADMIN_GRAPH,
            startDestination = ADMIN_DASHBOARD
        ) {
            composable(ADMIN_DASHBOARD) {
                AdminDashboardScreen(
                    onNavigate = navController::navigate,
                    onLogout = {
                        navController.navigate(LOGIN) {
                            popUpTo(0)
                        }
                    }
                )
            }

            composable(ADMIN_PRODUCTS) {
                ProductListScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_PRODUCT_FORM) {
                ProductFormScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_INGREDIENTS) {
                IngredientListScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_INGREDIENT_FORM) {
                IngredientFormScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_RECIPES) {
                RecipeListScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_RECIPE_FORM) {
                RecipeFormScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_INVENTORY) {
                AdminInventoryScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_ADJUSTMENT) {
                AdminAdjustmentScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_WASTE_ENTRY) {
                AdminWasteEntryScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_WASTE_HISTORY) {
                AdminWasteHistoryScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_RESTOCK_ENTRY) {
                AdminRestockEntryScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_RESTOCK_HISTORY) {
                AdminRestockHistoryScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_USERS) {
                UserListScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_USER_FORM) {
                UserFormScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_STAFF_LOGS) {
                StaffLogsScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_REPORTS_DASHBOARD) {
                ReportsDashboardScreen(
                    onNavigate = navController::navigate,
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_REPORT_SALES) {
                SalesReportScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_REPORT_INVENTORY) {
                InventoryReportScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_REPORT_WASTE) {
                WasteReportScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_REPORT_RESTOCK) {
                RestockReportScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_REPORT_TRANSACTIONS) {
                TransactionReportScreen(
                    onBack = navController::popBackStack
                )
            }

            composable(ADMIN_AUDIT_LOGS) {
                AuditLogsScreen(
                    onBack = navController::popBackStack
                )
            }
        }
    }
}