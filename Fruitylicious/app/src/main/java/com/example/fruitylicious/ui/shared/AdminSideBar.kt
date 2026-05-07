package com.example.fruitylicious.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AssignmentLate
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.fruitylicious.ADMIN_ADJUSTMENT
import com.example.fruitylicious.ADMIN_AUDIT_LOGS
import com.example.fruitylicious.ADMIN_DASHBOARD
import com.example.fruitylicious.ADMIN_INGREDIENTS
import com.example.fruitylicious.ADMIN_INVENTORY
import com.example.fruitylicious.ADMIN_NOTIFICATIONS
import com.example.fruitylicious.ADMIN_PRODUCTS
import com.example.fruitylicious.ADMIN_QUEUE
import com.example.fruitylicious.ADMIN_RECIPES
import com.example.fruitylicious.ADMIN_REPORTS_DASHBOARD
import com.example.fruitylicious.ADMIN_RESTOCK_HISTORY
import com.example.fruitylicious.ADMIN_STAFF_LOGS
import com.example.fruitylicious.ADMIN_USERS
import com.example.fruitylicious.ADMIN_WASTE_HISTORY
import com.example.fruitylicious.LOGIN
import com.example.fruitylicious.R
import com.example.fruitylicious.STAFF_POS
import com.example.fruitylicious.TRANSACTION_HISTORY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val SidebarBg = Color(0xFF2C8C44)
private val SidebarDarkBg = Color(0xFF1B5E20)
private val ActiveItemBg = Color(0xFF43A047)

private val WhiteFull = Color.White
private val WhiteMid = Color.White.copy(alpha = 0.75f)
private val WhiteDim = Color.White.copy(alpha = 0.50f)
private val WhiteFaint = Color.White.copy(alpha = 0.20f)

private data class AdminNavItem(
    val icon: ImageVector,
    val label: String,
    val route: String
)

@Composable
fun OwnerSideBarContent(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    ownerName: String,
    onLogout: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(SidebarBg)
    ) {
        OwnerSidebarHeader()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            OwnerMainNavigation(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    ownerNavigateTo(
                        navController = navController,
                        drawerState = drawerState,
                        scope = scope,
                        route = route,
                        currentRoute = currentRoute
                    )
                }
            )

            OwnerSidebarSectionHeader("Management")

            OwnerManagementNavigation(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    ownerNavigateTo(
                        navController = navController,
                        drawerState = drawerState,
                        scope = scope,
                        route = route,
                        currentRoute = currentRoute
                    )
                }
            )

            OwnerSidebarSectionHeader("Reports")

            OwnerReportsNavigation(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    ownerNavigateTo(
                        navController = navController,
                        drawerState = drawerState,
                        scope = scope,
                        route = route,
                        currentRoute = currentRoute
                    )
                }
            )
        }

        OwnerSidebarUserInfo(
            ownerName = ownerName
        )

        OwnerSidebarLogout(
            navController = navController,
            drawerState = drawerState,
            scope = scope,
            onLogout = onLogout
        )
    }
}

@Composable
private fun OwnerSidebarHeader() {
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
}

@Composable
private fun OwnerMainNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        AdminNavItem(Icons.Outlined.Dashboard, "Dashboard", ADMIN_DASHBOARD),
        AdminNavItem(Icons.Outlined.PointOfSale, "POS Screen", STAFF_POS),
        AdminNavItem(Icons.AutoMirrored.Outlined.ListAlt, "Order Queue", ADMIN_QUEUE),
        AdminNavItem(Icons.Outlined.Notifications, "Notifications", ADMIN_NOTIFICATIONS)
    )

    items.forEach { item ->
        OwnerSidebarNavItem(
            icon = item.icon,
            label = item.label,
            isActive = currentRoute == item.route,
            onClick = { onNavigate(item.route) }
        )
    }
}

@Composable
private fun OwnerManagementNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        AdminNavItem(Icons.Outlined.Inventory2, "Manage Products", ADMIN_PRODUCTS),
        AdminNavItem(Icons.Outlined.SetMeal, "Manage Ingredients", ADMIN_INGREDIENTS),
        AdminNavItem(Icons.AutoMirrored.Outlined.MenuBook, "Recipe Management", ADMIN_RECIPES),
        AdminNavItem(Icons.Outlined.Group, "User Management", ADMIN_USERS),
        AdminNavItem(Icons.Outlined.Search, "Inventory Monitoring", ADMIN_INVENTORY),
        AdminNavItem(Icons.Outlined.Tune, "Inventory Adjustment", ADMIN_ADJUSTMENT),
        AdminNavItem(Icons.Outlined.Autorenew, "Restock", ADMIN_RESTOCK_HISTORY),
        AdminNavItem(Icons.Outlined.DeleteOutline, "Waste Management", ADMIN_WASTE_HISTORY)
    )

    items.forEach { item ->
        OwnerSidebarNavItem(
            icon = item.icon,
            label = item.label,
            isActive = currentRoute == item.route,
            onClick = { onNavigate(item.route) }
        )
    }
}

@Composable
private fun OwnerReportsNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        AdminNavItem(Icons.Outlined.Receipt, "Transaction History", TRANSACTION_HISTORY),
        AdminNavItem(Icons.Outlined.BarChart, "Reports", ADMIN_REPORTS_DASHBOARD),
        AdminNavItem(Icons.Outlined.AssignmentLate, "Audit Logs", ADMIN_AUDIT_LOGS),
        AdminNavItem(Icons.Outlined.Groups, "Staff Logs", ADMIN_STAFF_LOGS)
    )

    items.forEach { item ->
        OwnerSidebarNavItem(
            icon = item.icon,
            label = item.label,
            isActive = currentRoute == item.route,
            onClick = { onNavigate(item.route) }
        )
    }
}

@Composable
private fun OwnerSidebarSectionHeader(title: String) {
    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = title.uppercase(),
        color = WhiteDim,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(
            start = 20.dp,
            top = 8.dp,
            bottom = 2.dp
        )
    )
}

@Composable
private fun OwnerSidebarNavItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) ActiveItemBg else Color.Transparent)
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

@Composable
private fun OwnerSidebarUserInfo(
    ownerName: String
) {
    HorizontalDivider(
        color = WhiteFaint,
        thickness = 1.dp
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFA000)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ownerName.firstOrNull()?.uppercaseChar()?.toString() ?: "O",
                color = WhiteFull,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ownerName.ifBlank { "Admin User" },
                color = WhiteFull,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Admin",
                color = WhiteMid,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun OwnerSidebarLogout(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    onLogout: () -> Unit
) {
    HorizontalDivider(
        color = WhiteFaint,
        thickness = 1.dp
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onLogout()

                scope.launch {
                    drawerState.close()
                }

                navController.navigate(LOGIN) {
                    popUpTo(0) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Logout,
            contentDescription = "Log Out",
            tint = WhiteMid,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = "Log Out",
            color = WhiteFull,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun ownerNavigateTo(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    route: String,
    currentRoute: String?
) {
    scope.launch {
        drawerState.close()
    }

    if (route == currentRoute) return

    navController.navigate(route) {
        popUpTo(ADMIN_DASHBOARD) {
            saveState = (route != ADMIN_DASHBOARD)
        }
        launchSingleTop = true
        restoreState = (route != ADMIN_DASHBOARD)
    }
}