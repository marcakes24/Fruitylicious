package com.example.fruitylicious.ui.admin.staffmanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.fruitylicious.data.local.entity.StaffLogEntity
import com.example.fruitylicious.ui.shared.FruityEmptyState
import com.example.fruitylicious.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffLogsScreen(
    onBack: () -> Unit,
    viewModel: StaffLogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFDF6),
                    titleContentColor = Color(0xFF1B5E20)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFDF6))
                .padding(padding)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.showActiveOnly,
                    onClick = { viewModel.toggleActiveOnly() },
                    label = { Text("Active only") }
                )

                FilterChip(
                    selected = !uiState.showActiveOnly,
                    onClick = { viewModel.showAll() },
                    label = { Text("All logs") }
                )
            }

            if (uiState.filteredLogs.isEmpty()) {
                FruityEmptyState(
                    title = "No staff logs",
                    message = "Clock in and clock out records will appear here.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredLogs, key = { it.logId }) { log ->
                        AdminStaffLogCard(
                            log = log,
                            userName = uiState.userNames[log.userId] ?: "User ${log.userId}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStaffLogCard(
    log: StaffLogEntity,
    userName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!log.image.isNullOrBlank()) {
                AsyncImage(
                    model = log.image,
                    contentDescription = "Clock proof",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, contentDescription = "User", tint = Color(0xFF2E7D32))
                Text(userName, style = MaterialTheme.typography.titleMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Login, contentDescription = "Clock in", tint = Color(0xFF2E7D32))
                Text("Clock In: ${DateTimeUtil.formatDateTime(log.clockIn)}")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Logout, contentDescription = "Clock out", tint = Color(0xFFEF6C00))
                Text("Clock Out: ${log.clockOut?.let { DateTimeUtil.formatDateTime(it) } ?: "Active"}")
            }

            Text(
                text = "Duration: ${DateTimeUtil.durationText(log.clockIn, log.clockOut)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D6D6D)
            )
        }
    }
}