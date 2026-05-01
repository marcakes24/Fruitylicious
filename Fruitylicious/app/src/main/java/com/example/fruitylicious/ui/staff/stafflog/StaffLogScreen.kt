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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.ui.shared.StaffSideBarContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val StaffLogGreen = Color(0xFF2E7D32)
private val StaffLogPageBg = Color(0xFFFFEAA0)
private val StaffLogTextMain = Color(0xFF1E293B)
private val StaffLogTextSub = Color.Gray
private val ActiveGreen = Color(0xFF22C55E)

@Composable
fun StaffLogScreen(
    navController: NavController,
    staffName: String = "Staff User",
    onLogout: () -> Unit = {},
    viewModel: StaffTimeLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerTonalElevation = 0.dp
            ) {
                StaffSideBarContent(
                    navController = navController,
                    drawerState = drawerState,
                    scope = scope,
                    staffName = uiState.staffName.ifBlank { staffName },
                    branchName = uiState.branchName.ifBlank { "B${uiState.branchId}" },
                    onLogout = onLogout
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(StaffLogPageBg)
        ) {
            StaffLogHeader(
                onMenuClick = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!uiState.error.isNullOrBlank()) {
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                if (!uiState.successMessage.isNullOrBlank()) {
                    Text(
                        text = uiState.successMessage ?: "",
                        color = StaffLogGreen,
                        fontSize = 13.sp
                    )
                }

                CurrentStatusCard(
                    uiState = uiState,
                    onClockIn = {
                        viewModel.clearMessages()
                        viewModel.clockIn()
                    },
                    onClockOut = {
                        viewModel.clearMessages()
                        viewModel.clockOut()
                    }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        item {
                            EmptyStaffLogText("Loading time logs...")
                        }
                    }

                    uiState.logs.isEmpty() -> {
                        item {
                            EmptyStaffLogText("No time logs found")
                        }
                    }

                    else -> {
                        items(
                            items = uiState.logs,
                            key = { it.logId }
                        ) { log ->
                            StaffLogCard(
                                log = log,
                                staffName = uiState.staffName.ifBlank { staffName },
                                username = uiState.username
                            )
                        }
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

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "TIME LOG",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CurrentStatusCard(
    uiState: StaffTimeLogUiState,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = StaffLogGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.staffName.ifBlank { "Staff User" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = StaffLogTextMain
                    )

                    Text(
                        text = "Branch ${uiState.branchId}",
                        fontSize = 12.sp,
                        color = StaffLogTextSub
                    )
                }

                Surface(
                    color = if (uiState.hasActiveLog) Color(0xFFE8F5E9) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (uiState.hasActiveLog) "Active" else "Not active",
                        color = if (uiState.hasActiveLog) ActiveGreen else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = if (uiState.hasActiveLog) onClockOut else onClockIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.hasActiveLog) Color(0xFFE53935) else StaffLogGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (uiState.hasActiveLog) Icons.Default.Logout else Icons.Default.Login,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (uiState.hasActiveLog) "Clock Out" else "Clock In",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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
    log: StaffTimeLogRow,
    staffName: String,
    username: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = StaffLogGreen,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = staffName.ifBlank { "Staff User" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = StaffLogTextMain
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@${username.ifBlank { "staff" }}",
                                    fontSize = 13.sp,
                                    color = StaffLogTextSub
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    color = Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "B${log.branchId}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Text(
                            text = formatDate(log.clockIn),
                            fontSize = 13.sp,
                            color = StaffLogTextSub
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            StaffLogTimeRow(
                                label = "Clock In:",
                                value = formatTime(log.clockIn),
                                indicatorColor = ActiveGreen
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            StaffLogTimeRow(
                                label = "Clock Out:",
                                value = log.clockOut?.let { formatTime(it) } ?: "Active",
                                indicatorColor = if (log.clockOut == null) ActiveGreen else Color.Gray,
                                active = log.clockOut == null
                            )
                        }
                    }

                    log.clockOut?.let { clockOut ->
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Duration: ${formatDuration(log.clockIn, clockOut)}",
                            fontSize = 12.sp,
                            color = StaffLogTextSub
                        )
                    }
                }
            }
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
    Row(verticalAlignment = Alignment.CenterVertically) {
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
            color = if (active) ActiveGreen else StaffLogTextMain
        )
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestamp))
}

private fun formatDuration(clockIn: Long, clockOut: Long): String {
    val durationMillis = (clockOut - clockIn).coerceAtLeast(0L)
    val totalMinutes = durationMillis / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L

    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}