package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── Brand colors ──────────────────────────────────────────────────────────────
private val RptGreen  = Color(0xFF2C8C44)
private val RptPageBg = Color(0xFFFFEAA0)

// ── Tab enum ──────────────────────────────────────────────────────────────────
private enum class ReportTab(val label: String) {
    SALES("Sales"), WASTE("Waste"), RESTOCK("Restock"), INVENTORY("Inventory")
}

// ── Root screen ───────────────────────────────────────────────────────────────
@Composable
fun ReportsScreen(
    navController: NavController,
    drawerState:   DrawerState,
    scope:         CoroutineScope
) {
    var selectedTab    by remember { mutableStateOf(ReportTab.SALES) }
    var selectedBranch by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RptPageBg)
    ) {
        // ── Top header ────────────────────────────────────────────────────────
        ReportsHeader(
            selectedBranch = selectedBranch,
            onBranchSelect = { selectedBranch = it },
            onMenuClick    = { scope.launch { drawerState.open() } }
        )

        // ── Tab row ───────────────────────────────────────────────────────────
        ReportsTabRow(
            selected = selectedTab,
            onSelect = { selectedTab = it }
        )

        // ── Tab content ───────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                ReportTab.SALES     -> SalesTabContent(selectedBranch, navController)
                ReportTab.WASTE     -> WasteTabContent(selectedBranch)
                ReportTab.RESTOCK   -> RestockTabContent(selectedBranch)
                ReportTab.INVENTORY -> InventoryTabContent(selectedBranch)
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun ReportsHeader(
    selectedBranch: String,
    onBranchSelect: (String) -> Unit,
    onMenuClick:    () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RptGreen)
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 14.dp)
    ) {
        // Hamburger menu
        Column(
            modifier            = Modifier
                .align(Alignment.CenterStart)
                .clickable { onMenuClick() },
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(2.5.dp)
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
            }
        }

        Text(
            text       = "REPORTS",
            color      = Color.White,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.align(Alignment.Center)
        )

        // Branch pills
        Row(
            modifier              = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("B1", "B2", "All").forEach { branch ->
                val isActive = selectedBranch == branch
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isActive) Color.White else Color.White.copy(alpha = 0.25f))
                        .clickable { onBranchSelect(branch) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = branch,
                        color      = if (isActive) RptGreen else Color.White,
                        fontSize   = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Tab row ───────────────────────────────────────────────────────────────────
@Composable
private fun ReportsTabRow(
    selected: ReportTab,
    onSelect: (ReportTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RptGreen)
            .padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val tabIcons = mapOf(
            ReportTab.SALES     to Icons.AutoMirrored.Outlined.TrendingUp,
            ReportTab.WASTE     to Icons.Outlined.DeleteOutline,
            ReportTab.RESTOCK   to Icons.Outlined.Autorenew,
            ReportTab.INVENTORY to Icons.Outlined.Inventory2
        )

        ReportTab.entries.forEach { tab ->
            val isActive = selected == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) Color.White else Color.White.copy(alpha = 0.15f))
                    .clickable { onSelect(tab) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector        = tabIcons[tab]!!,
                        contentDescription = null,
                        tint               = if (isActive) RptGreen else Color.White,
                        modifier           = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text       = tab.label,
                        color      = if (isActive) RptGreen else Color.White,
                        fontSize   = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ReportsScreenPreview() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    ReportsScreen(
        navController = rememberNavController(),
        drawerState   = drawerState,
        scope         = scope
    )
}
