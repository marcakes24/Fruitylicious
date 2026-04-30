package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Menu
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

// ── Design Tokens ─────────────────────────────────────────────────────────────
val RptGreen      = Color(0xFF2E7D32)
val RptGreenDark  = Color(0xFF1B5E20)
val RptPageBg     = Color(0xFFFFEAA0) // Yellowish background
val RptCardBg     = Color.White
val RptRed        = Color(0xFFD32F2F)
val RptTextMain   = Color(0xFF1D2433)
val RptTextSub    = Color(0xFF6B7280)
val RptBlue       = Color(0xFF2196F3)

enum class ReportTab(val label: String) {
    SALES("Sales"), WASTE("Waste"), RESTOCK("Restock"), INVENTORY("Inventory")
}

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
        // ── Top Header (Green) ────────────────────────────────────────────────
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
                    .clickable { scope.launch { drawerState.open() } }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "REPORTS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            
            // Branch selector container (White pill)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(2.dp)
            ) {
                listOf("B1", "B2", "All").forEach { b ->
                    val isB = selectedBranch == b
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isB) RptGreen else Color.Transparent)
                            .clickable { selectedBranch = b }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = b,
                            color = if (isB) Color.White else RptTextSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Secondary Tab Bar (White) ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val icons = mapOf(
                ReportTab.SALES to Icons.AutoMirrored.Outlined.TrendingUp,
                ReportTab.WASTE to Icons.Outlined.DeleteOutline,
                ReportTab.RESTOCK to Icons.Outlined.Inventory2,
                ReportTab.INVENTORY to Icons.Outlined.Inventory
            )
            ReportTab.entries.forEach { tab ->
                val isSel = selectedTab == tab
                Surface(
                    onClick = { selectedTab = tab },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) RptGreen else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icons[tab]!!,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSel) Color.White else RptTextSub
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else RptTextSub
                        )
                    }
                }
            }
        }

        // ── Content Area ─────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                ReportTab.SALES     -> SalesTabContent(selectedBranch, navController)
                ReportTab.WASTE     -> WasteTabContent(selectedBranch)
                ReportTab.RESTOCK   -> RestockTabContent(selectedBranch)
                ReportTab.INVENTORY -> InventoryTabContent(selectedBranch)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportsScreenPreview() {
    val ds = rememberDrawerState(DrawerValue.Closed)
    val sc = rememberCoroutineScope()
    ReportsScreen(rememberNavController(), ds, sc)
}
