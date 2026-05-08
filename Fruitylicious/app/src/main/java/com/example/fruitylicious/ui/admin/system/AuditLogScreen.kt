package com.example.fruitylicious.ui.admin.system

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.ui.shared.BranchSelector
import com.example.fruitylicious.ui.shared.FruityDateFilterField
import com.example.fruitylicious.ui.shared.FruityDatePicker
import com.example.fruitylicious.ui.shared.FruitySearchField
import com.example.fruitylicious.ui.shared.OwnerSideBarContent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

private val AuditGreenPrimary = Color(0xFF2E7D32)
private val AuditBackgroundYellow = Color(0xFFFFEAA0)
private val AuditGrayText = Color(0xFF888888)
private val AuditDarkText = Color(0xFF1B1B1B)
private val AuditTagBackground = Color(0xFFF0F0F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    navController: NavController,
    adminName: String = "Admin User",
    onLogout: () -> Unit = {},
    viewModel: AuditLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    val datePickerState = rememberDatePickerState()

    val filteredLogs = remember(uiState.logs, searchQuery, selectedDateMillis) {
        uiState.logs.filter { log ->
            val matchesSearch =
                log.logId.contains(searchQuery, ignoreCase = true) ||
                        log.action.contains(searchQuery, ignoreCase = true) ||
                        log.description.contains(searchQuery, ignoreCase = true) ||
                        log.tableAffected.contains(searchQuery, ignoreCase = true) ||
                        log.userName.contains(searchQuery, ignoreCase = true) ||
                        log.username.contains(searchQuery, ignoreCase = true) ||
                        log.branchName.contains(searchQuery, ignoreCase = true)

            val matchesDate = if (selectedDateMillis != null) {
                val logCalendar = Calendar.getInstance().apply {
                    timeInMillis = log.timestamp
                }

                val filterCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = selectedDateMillis!!
                }

                logCalendar.get(Calendar.DAY_OF_YEAR) == filterCalendar.get(Calendar.DAY_OF_YEAR) &&
                        logCalendar.get(Calendar.YEAR) == filterCalendar.get(Calendar.YEAR)
            } else {
                true
            }

            matchesSearch && matchesDate
        }
    }

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
                    ownerName = adminName,
                    onLogout = onLogout
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                AuditHeader(
                    selectedBranchId = uiState.selectedBranchId,
                    branches = uiState.branches,
                    isAdmin = uiState.isAdmin,
                    isOnline = uiState.isOnline,
                    localBranchId = uiState.localBranchId,
                    onBranchSelect = { branchId ->
                        viewModel.selectBranch(branchId)
                    },
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(AuditBackgroundYellow)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (!uiState.error.isNullOrBlank()) {
                    Text(
                        text = uiState.error ?: "",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (uiState.isAdmin && !uiState.isOnline) {
                    Text(
                        text = "Offline mode: Only local branch audit logs are available.",
                        color = AuditGrayText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FruitySearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search logs..."
                    )

                    FruityDateFilterField(
                        selectedDateText = if (selectedDateMillis != null) {
                            SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(selectedDateMillis!!))
                        } else "",
                        onClick = { showDatePicker = true },
                        onClear = { selectedDateMillis = null }
                    )
                }

    if (showDatePicker) {
        FruityDatePicker(
            state = datePickerState,
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                selectedDateMillis = millis
            },
            onClear = {
                selectedDateMillis = null
            }
        )
    }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    when {
                        uiState.isLoading && filteredLogs.isEmpty() -> {
                            item {
                                EmptyAuditText("Loading audit logs...")
                            }
                        }

                        filteredLogs.isEmpty() -> {
                            item {
                                EmptyAuditText("No audit logs found")
                            }
                        }

                        else -> {
                            items(
                                items = filteredLogs,
                                key = { it.logId }
                            ) { log ->
                                AuditLogCard(log)
                            }

                            if (uiState.hasMore) {
                                item {
                                    Button(
                                        onClick = { viewModel.loadMore() },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !uiState.isLoadingMore,
                                        colors = ButtonDefaults.buttonColors(containerColor = AuditGreenPrimary)
                                    ) {
                                        Text(if (uiState.isLoadingMore) "Loading..." else "Load More")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditHeader(
    selectedBranchId: Int?,
    branches: List<BranchEntity>,
    isAdmin: Boolean,
    isOnline: Boolean,
    localBranchId: Int,
    onBranchSelect: (Int?) -> Unit,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuditGreenPrimary)
            .padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }

            Text(
                text = "AUDIT LOGS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (isAdmin) {
                BranchSelector(
                    selectedBranchId = selectedBranchId,
                    branches = branches,
                    isOnline = isOnline,
                    onBranchSelected = onBranchSelect,
                    activeColor = AuditGreenPrimary,
                    containerColor = Color(0xFFF5F5F5),
                    localBranchId = localBranchId
                )
            }
        }
    }
}

@Composable
private fun EmptyAuditText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AuditGrayText,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AuditLogCard(log: AuditLogRow) {
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AuditTagBackground)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = shortenLogId(log.logId),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AuditDarkText
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE0E0E0))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = log.action,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuditDarkText
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDate(log.timestamp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuditDarkText
                    )

                    Text(
                        text = formatTime(log.timestamp),
                        fontSize = 10.sp,
                        color = AuditGrayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = log.description,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AuditDarkText
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("User:", fontSize = 11.sp, color = AuditGrayText)

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "@${log.username}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuditDarkText
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text("Table:", fontSize = 11.sp, color = AuditGrayText)

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF1F8F1))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = log.tableAffected,
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Branch:", fontSize = 11.sp, color = AuditGrayText)

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AuditTagBackground)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = log.branchName.ifBlank { "B${log.branchId}" },
                        fontSize = 11.sp,
                        color = AuditDarkText,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text("Name:", fontSize = 11.sp, color = AuditGrayText)

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = log.userName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuditDarkText
                )
            }
        }
    }
}

private fun shortenLogId(logId: String): String {
    return if (logId.length <= 8) {
        logId
    } else {
        "LOG-${logId.takeLast(6).uppercase()}"
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestamp))
}