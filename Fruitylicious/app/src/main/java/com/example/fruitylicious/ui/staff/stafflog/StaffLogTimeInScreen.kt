package com.example.fruitylicious.ui.staff.stafflog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.fruitylicious.ui.admin.staffmanagement.StaffLogViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val StaffLogGreen = Color(0xFF2E7D32)
private val StaffLogBg = Color(0xFFFFEAA0)
private val StaffLogTextMain = Color(0xFF1E293B)
private val StaffLogTextSub = Color.Gray

@Composable
fun StaffLogTimeInScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope = rememberCoroutineScope(),
    viewModel: StaffLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(
        uiState.logs,
        searchQuery
    ) {
        uiState.logs.filter { log ->
            log.name.contains(searchQuery, ignoreCase = true) ||
                    log.username.contains(searchQuery, ignoreCase = true) ||
                    log.branch.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StaffLogBg)
    ) {
        StaffLogHeader(
            onMenuClick = {
                scope.launch {
                    drawerState.open()
                }
            }
        )

        StaffLogSearchBar(
            searchQuery = searchQuery,
            onSearchChange = {
                searchQuery = it
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    item {
                        EmptyStaffLogText("Loading staff logs...")
                    }
                }

                filteredLogs.isEmpty() -> {
                    item {
                        EmptyStaffLogText("No staff logs found")
                    }
                }

                else -> {
                    items(
                        items = filteredLogs,
                        key = { it.logId }
                    ) { log ->
                        StaffLogCard(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffLogHeader(
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(StaffLogGreen)
            .padding(
                top = 40.dp,
                bottom = 12.dp,
                start = 16.dp,
                end = 16.dp
            )
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

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "STAFF LOG",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StaffLogSearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
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
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search staff name or username...",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Gray
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}

@Composable
private fun EmptyStaffLogText(
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = StaffLogTextSub,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun StaffLogCard(
    log: StaffLogEntry
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Staff",
                        tint = StaffLogGreen,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
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
                                color = StaffLogTextMain
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.username,
                                    fontSize = 13.sp,
                                    color = StaffLogTextSub
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    color = Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = log.branch,
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        ),
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
                            color = StaffLogTextSub
                        )
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    StaffLogTimeBox(log)
                }
            }
        }
    }
}

@Composable
private fun StaffLogTimeBox(
    log: StaffLogEntry
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            StaffLogTimeRow(
                label = "Clock In:",
                value = log.clockIn,
                indicatorColor = Color(0xFF22C55E)
            )

            Spacer(modifier = Modifier.size(8.dp))

            StaffLogTimeRow(
                label = "Clock Out:",
                value = log.clockOut,
                indicatorColor = if (log.clockOut == "Active") {
                    Color(0xFF22C55E)
                } else {
                    Color.Gray
                },
                active = log.clockOut == "Active"
            )
        }
    }
}

@Composable
private fun StaffLogTimeRow(
    label: String,
    value: String,
    indicatorColor: Color,
    active: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            fontSize = 13.sp,
            color = StaffLogTextSub
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) {
                Color(0xFF22C55E)
            } else {
                StaffLogTextMain
            }
        )
    }
}