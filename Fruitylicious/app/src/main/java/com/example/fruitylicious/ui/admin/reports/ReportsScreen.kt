package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.ui.shared.OwnerSideBarContent
import androidx.compose.runtime.collectAsState
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.ui.shared.BranchSelector
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
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(ReportTab.SALES) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                OwnerSideBarContent(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    ownerName = uiState.adminName,
                    onLogout = onLogout
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RptPageBg)
        ) {
        ReportsHeader(
            selectedBranchId = uiState.selectedBranchId,
            canAccessCrossBranch = uiState.canAccessCrossBranch,
            isOnline = uiState.isOnline,
            branches = uiState.branches,
            localBranchId = uiState.localBranchId,
            onBranchSelected = { viewModel.onBranchSelected(it) },
            onMenuClick = {
                scope.launch {
                    drawerState.open()
                }
            }
        )

        if (uiState.isAdmin && !uiState.isOnline) {
            Text(
                text = "Offline mode: Only local branch reports are available.",
                color = RptTextSub,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        ReportTabBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    ReportTab.SALES -> {
                        SalesTabContent(
                            branchId = uiState.selectedBranchId,
                            navController = navController
                        )
                    }

                    ReportTab.WASTE -> {
                        WasteTabContent(
                            branchId = uiState.selectedBranchId
                        )
                    }

                    ReportTab.RESTOCK -> {
                        RestockTabContent(
                            branchId = uiState.selectedBranchId
                        )
                    }

                    ReportTab.INVENTORY -> {
                        InventoryTabContent(
                            branchId = uiState.selectedBranchId
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsHeader(
    selectedBranchId: Int?,
    canAccessCrossBranch: Boolean,
    isOnline: Boolean,
    branches: List<BranchEntity>,
    localBranchId: Int,
    onBranchSelected: (Int?) -> Unit,
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

        if (canAccessCrossBranch) {
            BranchSelector(
                selectedBranchId = selectedBranchId,
                branches = branches,
                isOnline = isOnline,
                onBranchSelected = onBranchSelected,
                activeColor = RptGreen,
                containerColor = Color.White,
                localBranchId = localBranchId
            )
        } else {
            // Display only local branch name if restricted
            selectedBranchId?.let { id ->
                Text(
                    text = "B$id",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )
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

@Composable
fun ReportPeriodSelector(
    period: String,
    rangeText: String,
    onPeriodSelected: (String) -> Unit,
    onNavigate: (Int) -> Unit,
    onDateClick: () -> Unit,
    activeColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F5F5))
                .padding(4.dp)
        ) {
            listOf("daily", "weekly", "monthly").forEach { p ->
                val isSelected = period == p

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onPeriodSelected(p) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = p.replaceFirstChar { it.uppercase() },
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) activeColor else RptTextSub
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onNavigate(-1) }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous",
                    tint = activeColor
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDateClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = activeColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = rangeText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RptTextMain
                )
            }

            IconButton(onClick = { onNavigate(1) }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    tint = activeColor
                )
            }
        }
    }
}
