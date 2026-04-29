package com.example.fruitylicious.ui.shared

import kotlinx.coroutines.launch
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

// ── Color constants ──────────────────────────────────────────────────────────
private val StaffSidebarBg     = Color(0xFF2E7D32)
private val StaffSidebarDarkBg = Color(0xFF1B5E20)
private val StaffActiveItemBg  = Color(0xFF43A047)
private val StaffWhiteFull     = Color.White
private val StaffWhiteMid      = Color.White.copy(alpha = 0.75f)
private val StaffWhiteDim      = Color.White.copy(alpha = 0.50f)
private val StaffWhiteFaint    = Color.White.copy(alpha = 0.20f)

// ── Data model ───────────────────────────────────────────────────────────────
private data class StaffNavItem(
    val icon:  ImageVector,
    val label: String,
    val route: String
)

@Composable
fun StaffSideBarContent(
    navController: NavController,
    drawerState:   DrawerState,
    scope:         CoroutineScope,
    staffName:     String = "Staff User",
    branchName:    String = "Branch 1"
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(StaffSidebarBg)
    ) {

        // ── Logo header ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StaffSidebarDarkBg)
                .padding(top = 52.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier.size(width = 140.dp, height = 72.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter            = painterResource(id = R.drawable.logo),
                    contentDescription = "Fruitylicious Logo",
                    modifier           = Modifier
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
            // ── Main items (staff-visible only) ──────────────────────────────
            // ... inside StaffSideBarContent function
            val mainItems = listOf(
                StaffNavItem(Icons.Outlined.Dashboard, "Dashboard", "home"),
                StaffNavItem(Icons.Outlined.PointOfSale, "POS Screen", "pos"),
                StaffNavItem(Icons.AutoMirrored.Outlined.ListAlt, "Order Queue", "queue_screen"), // Route added
                StaffNavItem(Icons.Outlined.BarChart, "Sales Summary", "SalesSummary"),
                StaffNavItem(Icons.Outlined.AccessTime, "Time Log", ""),
                StaffNavItem(Icons.Outlined.Search, "Inventory Monitoring", ""),
                StaffNavItem(Icons.Outlined.Notifications, "Notifications", "notification")
            )
// ... rest of code remains same
            mainItems.forEach { item ->
                StaffSidebarNavItem(
                    icon     = item.icon,
                    label    = item.label,
                    isActive = currentRoute == item.route,
                    onClick  = {
                        if (item.route.isNotEmpty())
                            staffNavigateTo(navController, drawerState, scope, item.route)
                        else
                            scope.launch { drawerState.close() }
                    }
                )
            }

            // ── Management section (staff-limited) ───────────────────────────
            StaffSidebarSectionHeader("Management")
            val managementItems = listOf(
                StaffNavItem(Icons.Outlined.Autorenew,   "Restock",              ""),
                StaffNavItem(Icons.Outlined.DeleteOutline,"Waste Management",    "")
            )
            managementItems.forEach { item ->
                StaffSidebarNavItem(
                    icon     = item.icon,
                    label    = item.label,
                    isActive = currentRoute == item.route,
                    onClick  = {
                        if (item.route.isNotEmpty())
                            staffNavigateTo(navController, drawerState, scope, item.route)
                        else
                            scope.launch { drawerState.close() }
                    }
                )
            }

            // ── Reports section (staff-limited) ──────────────────────────────
            StaffSidebarSectionHeader("Reports")
            val reportItems = listOf(
                StaffNavItem(Icons.Outlined.Receipt, "Transaction History", "")
            )
            reportItems.forEach { item ->
                StaffSidebarNavItem(
                    icon     = item.icon,
                    label    = item.label,
                    isActive = currentRoute == item.route,
                    onClick  = {
                        if (item.route.isNotEmpty())
                            staffNavigateTo(navController, drawerState, scope, item.route)
                        else
                            scope.launch { drawerState.close() }
                    }
                )
            }
        }

        // ── Bottom: Staff info + Log Out ─────────────────────────────────────
        HorizontalDivider(color = StaffWhiteFaint, thickness = 1.dp)

        // Staff User row
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle with initial "S"
            Box(
                modifier         = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFA000)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = staffName.firstOrNull()?.uppercaseChar()?.toString() ?: "S",
                    color      = StaffWhiteFull,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = staffName,
                    color      = StaffWhiteFull,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    // "Staff · Branch 1" as shown in Figma
                    text     = "Staff · $branchName",
                    color    = StaffWhiteMid,
                    fontSize = 12.sp
                )
            }
        }

        // Log Out row
        HorizontalDivider(color = StaffWhiteFaint, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = "Log Out",
                tint               = StaffWhiteMid,
                modifier           = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text       = "Log Out",
                color      = StaffWhiteFull,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────
@Composable
private fun StaffSidebarSectionHeader(title: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text          = title.uppercase(),
        color         = StaffWhiteDim,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier      = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 2.dp)
    )
}

// ── Single nav item ───────────────────────────────────────────────────────────
@Composable
private fun StaffSidebarNavItem(
    icon:     ImageVector,
    label:    String,
    isActive: Boolean,
    onClick:  () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) StaffActiveItemBg else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = if (isActive) StaffWhiteFull else StaffWhiteMid,
            modifier           = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text       = label,
            color      = if (isActive) StaffWhiteFull else StaffWhiteMid,
            fontSize   = 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ── Navigation helper ─────────────────────────────────────────────────────────
private fun staffNavigateTo(
    navController: NavController,
    drawerState:   DrawerState,
    scope:         CoroutineScope,
    route:         String
) {
    scope.launch { drawerState.close() }
    navController.navigate(route) {
        popUpTo("home") { saveState = true }
        launchSingleTop = true
        restoreState    = true
    }
}