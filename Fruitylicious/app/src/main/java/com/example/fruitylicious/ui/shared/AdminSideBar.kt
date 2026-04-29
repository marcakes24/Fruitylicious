package com.example.fruitylicious.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.fruitylicious.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── Color constants ──────────────────────────────────────────────────────────
private val SidebarBg       = Color(0xFF2E7D32)
private val SidebarDarkBg   = Color(0xFF1B5E20)
private val ActiveItemBg    = Color(0xFF43A047)
private val WhiteFull       = Color.White
private val WhiteMid        = Color.White.copy(alpha = 0.75f)
private val WhiteDim        = Color.White.copy(alpha = 0.50f)
private val WhiteFaint      = Color.White.copy(alpha = 0.20f)

// ── Data model ───────────────────────────────────────────────────────────────
private data class NavItem(
    val icon: ImageVector,
    val label: String,
    val route: String
)

@Composable
fun AdminSideBarContent(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(SidebarBg)
    ) {

        // ── Logo header ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SidebarDarkBg)
                .padding(top = 52.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(width = 140.dp, height = 72.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Fruitylicious Logo",
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(2f)
                )
            }
        }

        // ── Scrollable nav items ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {

            // Main items
            val mainItems = listOf(
                NavItem(Icons.Outlined.Dashboard, "Dashboard", "admin_home"),
                NavItem(Icons.Outlined.PointOfSale, "POS Screen", "admin_pos"),
                NavItem(Icons.AutoMirrored.Outlined.ListAlt, "Order Queue", "admin_queue"),
                NavItem(Icons.Outlined.AccessTime, "Time Log", "admin_time_log"),
                NavItem(Icons.Outlined.Notifications, "Notifications", "admin_notification")
            )

            mainItems.forEach { item ->
                SidebarNavItem(
                    icon      = item.icon,
                    label     = item.label,
                    isActive  = currentRoute == item.route,
                    onClick   = {
                        if (item.route.isNotEmpty())
                            navigateTo(navController, drawerState, scope, item.route)
                        else
                            scope.launch { drawerState.close() }
                    }
                )
            }

            // Management section
            SidebarSectionHeader("Management")
            val managementItems = listOf(
                NavItem(Icons.Outlined.Inventory2,           "Manage Products",       "admin_manage_products"),
                NavItem(Icons.Outlined.SetMeal,              "Manage Ingredients",    "admin_manage_ingredients"),
                NavItem(Icons.AutoMirrored.Outlined.MenuBook,"Recipe Management",     "admin_recipe_management"),
                NavItem(Icons.Outlined.Group,                "User Management",       "admin_users"),
                NavItem(Icons.Outlined.Search,               "Inventory Monitoring",  "admin_inventory_monitoring"),
                NavItem(Icons.Outlined.Tune,                 "Inventory Adjustment",  "admin_inventory_adjustment"),
                NavItem(Icons.Outlined.Autorenew,            "Restock",               "admin_restock"),
                NavItem(Icons.Outlined.DeleteOutline,        "Waste Management",      "admin_waste_management")
            )

            managementItems.forEach { item ->
                SidebarNavItem(
                    icon     = item.icon,
                    label    = item.label,
                    isActive = currentRoute == item.route,
                    onClick  = {
                        if (item.route.isNotEmpty())
                            navigateTo(navController, drawerState, scope, item.route)
                        else
                            scope.launch { drawerState.close() }
                    }
                )
            }

            // Reports section
            SidebarSectionHeader("Reports")
            val reportItems = listOf(
                NavItem(Icons.Outlined.Receipt,       "Transaction History", "admin_transacHistory"),
                NavItem(Icons.Outlined.BarChart,      "Reports",            "admin_SalesSummary"),
                NavItem(Icons.Outlined.AssignmentLate,"Audit Logs",         ""),
                NavItem(Icons.Outlined.Groups,        "Staff Logs",         "")
            )

            reportItems.forEach { item ->
                SidebarNavItem(
                    icon     = item.icon,
                    label    = item.label,
                    isActive = currentRoute == item.route,
                    onClick  = {
                        if (item.route.isNotEmpty())
                            navigateTo(navController, drawerState, scope, item.route)
                        else
                            scope.launch { drawerState.close() }
                    }
                )
            }
        }

        // ── Bottom Section ───────────────────────────────────────────────────
        HorizontalDivider(color = WhiteFaint, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFFFA000)),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = WhiteFull, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Admin User", color = WhiteFull, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("admin@fruity.com", color = WhiteMid, fontSize = 12.sp)
            }
        }

        HorizontalDivider(color = WhiteFaint, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = WhiteMid, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text("Log Out", color = WhiteFull, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SidebarSectionHeader(title: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = title.uppercase(),
        color = WhiteDim,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) ActiveItemBg else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) WhiteFull else WhiteMid,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = if (isActive) WhiteFull else WhiteMid,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun navigateTo(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    route: String
) {
    scope.launch { drawerState.close() }
    navController.navigate(route) {
        popUpTo("admin_home") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}