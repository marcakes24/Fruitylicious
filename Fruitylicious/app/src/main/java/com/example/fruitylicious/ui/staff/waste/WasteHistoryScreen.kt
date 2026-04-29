package com.example.fruitylicious.ui.staff.waste

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.fruitylicious.STAFF_WASTE_ENTRY
import com.example.fruitylicious.data.local.entity.WasteLogEntity
import com.example.fruitylicious.ui.shared.FruityEmptyState
import com.example.fruitylicious.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteHistoryScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: WasteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Waste History") },
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(STAFF_WASTE_ENTRY) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("New Waste") },
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White
            )
        }
    ) { padding ->
        if (uiState.wasteLogs.isEmpty()) {
            FruityEmptyState(
                title = "No waste logs",
                message = "Waste records will appear here.",
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFDF6))
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFDF6))
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.wasteLogs, key = { it.wasteId }) { log ->
                    WasteCard(log = log)
                }
            }
        }
    }
}

@Composable
private fun WasteCard(
    log: WasteLogEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!log.image.isNullOrBlank()) {
                AsyncImage(
                    model = log.image,
                    contentDescription = "Waste proof image",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = "Ingredient ID: ${log.ingredientId}",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Quantity: ${log.quantity}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Reason: ${log.reason}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = DateTimeUtil.formatDateTime(log.dateTime),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D6D6D)
            )
        }
    }
}