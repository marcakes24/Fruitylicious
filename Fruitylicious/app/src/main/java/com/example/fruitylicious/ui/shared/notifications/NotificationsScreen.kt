package com.example.fruitylicious.ui.shared.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.ADMIN_ADJUSTMENT
import com.example.fruitylicious.STAFF_ADJUSTMENT
import com.example.fruitylicious.ui.shared.SharedDrawerContent
import com.example.fruitylicious.ui.shared.SharedScreenMode
import java.util.Locale
import kotlinx.coroutines.launch

private val NotifGreen = Color(0xFF2E7D32)
private val NotifPageBg = Color(0xFFFFEAA0)
private val NotifTextMain = Color(0xFF1E293B)
private val NotifTextSub = Color.Gray
private val CriticalRed = Color(0xFFEF5350)
private val WarningYellow = Color(0xFFFBC02D)

@Composable
fun NotificationsScreen(
    navController: NavController,
    mode: SharedScreenMode = SharedScreenMode.ADMIN,
    userName: String = "User",
    branchName: String = "",
    onLogout: () -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedBranch by remember(uiState.isAdmin, uiState.userBranchId) { 
        mutableStateOf(if (uiState.isAdmin) "All" else uiState.userBranchId) 
    }

    val filteredNotifications = uiState.notifications.filter { item ->
        when (selectedBranch) {
            "B1" -> item.branchId == 1
            "B2" -> item.branchId == 2
            else -> true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                SharedDrawerContent(
                    mode = mode,
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    userName = userName,
                    branchName = branchName,
                    onLogout = onLogout
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NotifPageBg)
        ) {
            Header(
                selectedBranch = selectedBranch,
                isAdmin = uiState.isAdmin,
                onBranchSelected = { selectedBranch = it },
                onMenuClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        EmptyNotificationText("Loading notifications...")
                    }

                    filteredNotifications.isEmpty() -> {
                        EmptyNotificationText("No low-stock notifications.")
                    }

                    else -> {
                        filteredNotifications.forEach { notification ->
                            NotificationItem(
                                notification = notification,
                                onViewDetails = {
                                    val route = if (mode == SharedScreenMode.ADMIN) {
                                        ADMIN_ADJUSTMENT
                                    } else {
                                        STAFF_ADJUSTMENT
                                    }

                                    navController.navigate(route)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Header(
    selectedBranch: String,
    isAdmin: Boolean,
    onBranchSelected: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NotifGreen)
            .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }

            Text(
                text = "NOTIFICATION",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (isAdmin) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(4.dp)
                ) {
                    listOf("B1", "B2", "All").forEach { branch ->
                        val isSelected = selectedBranch == branch

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NotifGreen else Color.Transparent)
                                .clickable { onBranchSelected(branch) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = branch,
                                color = if (isSelected) Color.White else Color(0xFF666E7A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = NotifTextSub,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun NotificationItem(
    notification: NotificationRow,
    onViewDetails: () -> Unit
) {
    val isCritical = notification.status == "Critical"

    val badgeColor = if (isCritical) {
        Color(0xFFFFEBEE)
    } else {
        Color(0xFFFFF9C4)
    }

    val statusColor = if (isCritical) {
        CriticalRed
    } else {
        WarningYellow
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Inventory",
                        tint = NotifGreen,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = NotifTextMain
                    )

                    Text(
                        text = "Branch ${notification.branchId}",
                        color = NotifTextSub,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = notification.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current: ${formatQuantity(notification.currentStock)} ${notification.unitType}",
                    color = Color(0xFF795548),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Text(
                    text = "Min: ${formatQuantity(notification.lowStockThreshold)} ${notification.unitType}",
                    color = Color(0xFF795548),
                    fontSize = 14.sp
                )
            }

            LinearProgressIndicator(
                progress = { notification.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = Color(0xFFEEEEEE)
            )

            Button(
                onClick = onViewDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NotifGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "View Details",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}