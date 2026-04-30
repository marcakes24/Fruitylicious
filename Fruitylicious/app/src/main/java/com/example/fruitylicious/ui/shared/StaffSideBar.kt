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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
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
import com.example.fruitylicious.LOGIN
import com.example.fruitylicious.R
import com.example.fruitylicious.STAFF_DASHBOARD
import com.example.fruitylicious.STAFF_INVENTORY
import com.example.fruitylicious.STAFF_POS
import com.example.fruitylicious.STAFF_RESTOCK_HISTORY
import com.example.fruitylicious.STAFF_SALES_SUMMARY
import com.example.fruitylicious.STAFF_TRANSACTION_HISTORY
import com.example.fruitylicious.STAFF_WASTE_HISTORY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val StaffSidebarBg = Color(0xFF2E7D32)
private val StaffSidebarDarkBg = Color(0xFF1B5E20)
private val StaffActiveItemBg = Color(0xFF43A047)
private val StaffWhiteFull = Color.White
private val StaffWhiteMid = Color.White.copy(alpha = 0.75f)
private val StaffWhiteDim = Color.White.copy(alpha = 0.50f)
private val StaffWhiteFaint = Color.White.copy(alpha = 0.20f)

private data class StaffNavItem(
    val icon: ImageVector,
    val label: String,
    val route: String
)

@Composable
fun StaffSideBarContent(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    staffName: String,
    branchName: String,
    onLogout: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(StaffSidebarBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StaffSidebarDarkBg)
                .padding(top = 52.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Fruitylicious Logo",
                modifier = Modifier
                    .width(140.dp)
                    .height(72.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            val mainItems = listOf(
                StaffNavItem(Icons.Outlined.Dashboard, "Dashboard", STAFF_DASHBOARD),
                StaffNavItem(Icons.Outlined.PointOfSale, "POS Screen", STAFF_POS),
                StaffNavItem(Icons.AutoMirrored.Outlined.ListAlt, "Order Queue", STAFF_TRANSACTION_HISTORY),
                StaffNavItem(Icons.Outlined.BarChart, "Sales Summary", STAFF_SALES_SUMMARY),
                StaffNavItem(Icons.Outlined.AccessTime, "Time Log", STAFF_DASHBOARD),
                StaffNavItem(Icons.Outlined.Search, "Inventory Monitoring", STAFF_INVENTORY),
                StaffNavItem(Icons.Outlined.Notifications, "Notifications", STAFF_DASHBOARD)
            )

            mainItems.forEach { item ->
                StaffSidebarNavItem(
                    icon = item.icon,
                    label = item.label,
                    isActive = currentRoute == item.route,
                    onClick = {
                        staffNavigateTo(
                            navController = navController,
                            drawerState = drawerState,
                            scope = scope,
                            route = item.route
                        )
                    }
                )
            }

            StaffSidebarSectionHeader("Management")

            val managementItems = listOf(
                StaffNavItem(Icons.Outlined.Autorenew, "Restock", STAFF_RESTOCK_HISTORY),
                StaffNavItem(Icons.Outlined.DeleteOutline, "Waste Management", STAFF_WASTE_HISTORY)
            )

            managementItems.forEach { item ->
                StaffSidebarNavItem(
                    icon = item.icon,
                    label = item.label,
                    isActive = currentRoute == item.route,
                    onClick = {
                        staffNavigateTo(
                            navController = navController,
                            drawerState = drawerState,
                            scope = scope,
                            route = item.route
                        )
                    }
                )
            }

            StaffSidebarSectionHeader("Reports")

            val reportItems = listOf(
                StaffNavItem(Icons.Outlined.Receipt, "Transaction History", STAFF_TRANSACTION_HISTORY)
            )

            reportItems.forEach { item ->
                StaffSidebarNavItem(
                    icon = item.icon,
                    label = item.label,
                    isActive = currentRoute == item.route,
                    onClick = {
                        staffNavigateTo(
                            navController = navController,
                            drawerState = drawerState,
                            scope = scope,
                            route = item.route
                        )
                    }
                )
            }
        }

        HorizontalDivider(color = StaffWhiteFaint, thickness = 1.dp)

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
                    text = staffName.firstOrNull()?.uppercaseChar()?.toString() ?: "S",
                    color = StaffWhiteFull,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = staffName.ifBlank { "Staff User" },
                    color = StaffWhiteFull,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Staff · $branchName",
                    color = StaffWhiteMid,
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider(color = StaffWhiteFaint, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onLogout()
                    scope.launch { drawerState.close() }
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
                tint = StaffWhiteMid,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = "Log Out",
                color = StaffWhiteFull,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StaffSidebarSectionHeader(title: String) {
    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = title.uppercase(),
        color = StaffWhiteDim,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun StaffSidebarNavItem(
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
            .background(if (isActive) StaffActiveItemBg else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) StaffWhiteFull else StaffWhiteMid,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = label,
            color = if (isActive) StaffWhiteFull else StaffWhiteMid,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun staffNavigateTo(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    route: String
) {
    scope.launch {
        drawerState.close()
    }

    navController.navigate(route) {
        popUpTo(STAFF_DASHBOARD) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}