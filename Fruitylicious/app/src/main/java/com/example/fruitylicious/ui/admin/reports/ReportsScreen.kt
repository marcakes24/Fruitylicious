package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.ui.shared.AdminSideBarContent
import kotlinx.coroutines.launch

val RptGreen = Color(0xFF2E7D32)
val RptGreenDark = Color(0xFF1B5E20)
val RptPageBg = Color(0xFFFFEAA0)
val RptCardBg = Color.White
val RptRed = Color(0xFFD32F2F)
val RptTextMain = Color(0xFF1D2433)
val RptTextSub = Color(0xFF6B7280)
val RptBlue = Color(0xFF2196F3)

enum class ReportTab(val label: String) {
    SALES("Sales"),
    WASTE("Waste"),
    RESTOCK("Restock"),
    INVENTORY("Inventory")
}

@Composable
fun ReportsScreen(
    navController: NavController,
    onLogout: () -> Unit = {},
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(ReportTab.SALES) }
    var selectedBranch by remember { 
        mutableStateOf(if (uiState.isAdmin) "All" else uiState.userBranchId) 
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                AdminSideBarContent(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    adminName = uiState.adminName,
                    onLogout = onLogout
                )
            }
        }
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RptPageBg)
        ) {
            ReportsHeader(
                selectedBranch = selectedBranch,
                isAdmin = uiState.isAdmin,
                onBranchSelected = { selectedBranch = it },
                onMenuClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )

            ReportTabBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    ReportTab.SALES -> {
                        SalesTabContent(
                            branch = selectedBranch,
                            navController = navController
                        )
                    }

                    ReportTab.WASTE -> {
                        WasteTabContent(
                            branch = selectedBranch
                        )
                    }

                    ReportTab.RESTOCK -> {
                        RestockTabContent(
                            branch = selectedBranch
                        )
                    }

                    ReportTab.INVENTORY -> {
                        InventoryTabContent(
                            branch = selectedBranch
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsHeader(
    selectedBranch: String,
    isAdmin: Boolean,
    onBranchSelected: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RptGreen)
            .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Menu",
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clickable { onMenuClick() }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "REPORTS",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        if (isAdmin) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(2.dp)
            ) {
                listOf("B1", "B2", "All").forEach { branch ->
                    val isSelected = selectedBranch == branch

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) RptGreen else Color.Transparent)
                            .clickable { onBranchSelected(branch) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = branch,
                            color = if (isSelected) Color.White else RptTextSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportTabBar(
    selectedTab: ReportTab,
    onTabSelected: (ReportTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ReportTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab

            Surface(
                onClick = { onTabSelected(tab) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) RptGreen else Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (tab) {
                            ReportTab.SALES -> Icons.AutoMirrored.Outlined.TrendingUp
                            ReportTab.WASTE -> Icons.Outlined.DeleteOutline
                            ReportTab.RESTOCK -> Icons.Outlined.Inventory2
                            ReportTab.INVENTORY -> Icons.Outlined.Inventory
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) Color.White else RptTextSub
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else RptTextSub
                    )
                }
            }
        }
    }
}