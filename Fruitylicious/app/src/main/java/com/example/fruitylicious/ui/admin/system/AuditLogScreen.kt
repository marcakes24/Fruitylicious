package com.example.fruitylicious.ui.admin.system

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ── Constants (Consistent with POSScreen) ───────────────────────────────────
val GreenPrimary = Color(0xFF2E7D32)
val BackgroundYellow = Color(0xFFFFEAA0)
val GrayText = Color(0xFF888888)
val DarkText = Color(0xFF1B1B1B)
val TagBackground = Color(0xFFF0F0F0)
val GreenTagText = Color(0xFF2E7D32)

// ── Data Model ──────────────────────────────────────────────────────────────
data class AuditLog(
    val id: String,
    val action: String,
    val description: String,
    val user: String,
    val table: String,
    val branch: String,
    val date: String,
    val time: String
)

val sampleLogs = listOf(
    AuditLog("LOG-000001", "update_recipe", "Updated Mango shake recipe", "@staff", "recipe", "B1", "Apr 26, 2026", "08:40 AM"),
    AuditLog("LOG-000002", "add_waste", "Added 2 pcs of spoiled Mango", "@staff", "waste_entries", "B1", "Apr 26, 2026", "09:40 AM"),
    AuditLog("LOG-000003", "add_ingredient", "Added new ingredient: Chia Seeds", "@staff", "ingredients", "B2", "Apr 25, 2026", "10:40 AM"),
    AuditLog("LOG-000004", "adjust_inventory", "Adjusted Milk stock -2 due to spillage", "@staff", "inventory_adjustments", "B2", "Apr 25, 2026", "05:40 AM"),
    AuditLog("LOG-000005", "update_recipe", "Updated Avocado shake recipe", "@staff", "recipes", "B1", "Apr 24, 2026", "10:40 AM"),
    AuditLog("LOG-000006", "add_restock", "Restocked 50 pcs of Banana", "@staff", "restock_entries", "B1", "Apr 24, 2026", "02:40 AM")
)

// ── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("All") }

    val filteredLogs = sampleLogs.filter { log ->
        val matchesBranch = selectedBranch == "All" || log.branch == selectedBranch
        val matchesSearch = log.id.contains(searchQuery, ignoreCase = true) ||
                log.action.contains(searchQuery, ignoreCase = true) ||
                log.description.contains(searchQuery, ignoreCase = true) ||
                log.table.contains(searchQuery, ignoreCase = true)
        matchesBranch && matchesSearch
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenPrimary)
                    .padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                    Text(
                        text = "AUDIT LOGS",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Branch Filter Segmented Controls - Consistent with WasteManagement
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
                                    .background(if (isSelected) GreenPrimary else Color.Transparent)
                                    .clickable { selectedBranch = branch }
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundYellow)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.White, RoundedCornerShape(12.dp)),
                placeholder = { Text("Search ID, user, action, table...", color = Color.LightGray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = GreenPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Audit Logs List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredLogs) { log ->
                    AuditLogCard(log)
                }
            }
        }
    }
}

@Composable
fun AuditLogCard(log: AuditLog) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Log ID Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TagBackground)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(log.id, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Action Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE0E0E0))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(log.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkText)
                    }
                }
                
                // Date/Time
                Column(horizontalAlignment = Alignment.End) {
                    Text(log.date, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
                    Text(log.time, fontSize = 10.sp, color = GrayText)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = log.description,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("User:", fontSize = 11.sp, color = GrayText)
                Spacer(modifier = Modifier.width(4.dp))
                Text(log.user, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkText)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text("Table:", fontSize = 11.sp, color = GrayText)
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF1F8F1))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(log.table, fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Branch:", fontSize = 11.sp, color = GrayText)
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(TagBackground)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(log.branch, fontSize = 11.sp, color = DarkText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuditLogScreenPreview() {
    AuditLogScreen()
}
