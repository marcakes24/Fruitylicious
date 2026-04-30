package com.example.fruitylicious.ui.admin.reports

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fruitylicious.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── DATA MODELS ──────────────────────────────────────────────────────────────

data class StaffLogEntry(
    val name: String,
    val username: String,
    val branch: String,
    val clockIn: String,
    val clockOut: String, // "Active" if still clocked in
    val date: String,
    val imageRes: Int
)

@Composable
fun StaffLogScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    var selectedBranch by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    // Mock Data
    val allLogs = listOf(
        StaffLogEntry(
            name = "Eula Valdez",
            username = "@staff1",
            branch = "B1",
            clockIn = "09:58 AM",
            clockOut = "Active",
            date = "Apr 26, 2026",
            imageRes = R.drawable.eula
        ),
        StaffLogEntry(
            name = "Axel Villareyt",
            username = "@staff 2",
            branch = "B2",
            clockIn = "09:57 AM",
            clockOut = "Active",
            date = "Apr 26, 2026",
            imageRes = R.drawable.axel
        ),
        StaffLogEntry(
            name = "Eula Valdez",
            username = "@staff1",
            branch = "B1",
            clockIn = "08:30 AM",
            clockOut = "05:00 PM",
            date = "Apr 25, 2026",
            imageRes = R.drawable.eula
        )
    )

    // Filtering logic
    val filteredLogs = allLogs.filter { log ->
        val matchesBranch = if (selectedBranch == "All") true else log.branch == selectedBranch
        val matchesSearch = log.name.contains(searchQuery, ignoreCase = true) || 
                          log.username.contains(searchQuery, ignoreCase = true)
        matchesBranch && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFEAA0)) // Pale yellow background
    ) {
        // ── TOP GREEN HEADER ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E7D32)) // Brand Green
                .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Title - White Text
                Text(
                    text = "STAFF LOG",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                // Branch Selector Toggle
                Surface(
                    color = Color(0xFF1B5E20), // Darker green
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("B1", "B2", "All").forEach { branch ->
                            val isSelected = selectedBranch == branch
                            Surface(
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clickable { selectedBranch = branch }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = branch,
                                        color = if (isSelected) Color(0xFF2E7D32) else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── SEARCH BAR ───────────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search staff name or username...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        }

        // ── LOG LIST ──────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredLogs) { log ->
                StaffLogCard(log)
            }
        }
    }
}

@Composable
fun StaffLogCard(log: StaffLogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Profile Image
                Image(
                    painter = painterResource(id = log.imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = log.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF1E293B)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = log.username,
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = log.branch,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                        Text(
                            text = log.date,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Log Info Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clock In:", fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(log.clockIn, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clock Out:", fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = log.clockOut,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (log.clockOut == "Active") Color(0xFF22C55E) else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
