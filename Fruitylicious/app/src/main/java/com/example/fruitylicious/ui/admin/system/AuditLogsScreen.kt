package com.example.fruitylicious.ui.admin.system

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FactCheck
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
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fruitylicious.data.local.entity.AuditLogEntity
import com.example.fruitylicious.ui.shared.FruityEmptyState
import com.example.fruitylicious.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogsScreen(
    onBack: () -> Unit,
    viewModel: AuditLogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit Logs") },
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
                    selected = uiState.selectedSyncFilter == AuditSyncFilter.UNSYNCED_ONLY,
                    onClick = { viewModel.toggleUnsyncedOnly() },
                    label = { Text("Unsynced only") }
                )

                FilterChip(
                    selected = uiState.selectedSyncFilter == AuditSyncFilter.ALL,
                    onClick = { viewModel.showAll() },
                    label = { Text("All logs") }
                )
            }

            if (uiState.filteredLogs.isEmpty()) {
                FruityEmptyState(
                    title = "No audit logs",
                    message = "User actions and system writes will appear here.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredLogs, key = { it.logId }) { log ->
                        AuditLogCard(log = log)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogCard(
    log: AuditLogEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (log.isSynced) Color.White else Color(0xFFFFF8E1)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FactCheck,
                contentDescription = "Audit log",
                tint = Color(0xFF2E7D32)
            )

            Text(
                text = log.action,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Table: ${log.tableAffected}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6D6D6D)
            )

            Text(
                text = "User ID: ${log.userId} • Branch ID: ${log.branchId}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D6D6D)
            )

            Text(
                text = DateTimeUtil.formatDateTime(log.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D6D6D)
            )

            Text(
                text = if (log.isSynced) "Synced" else "Pending sync",
                style = MaterialTheme.typography.bodySmall,
                color = if (log.isSynced) Color(0xFF2E7D32) else Color(0xFFF57F17)
            )
        }
    }
}