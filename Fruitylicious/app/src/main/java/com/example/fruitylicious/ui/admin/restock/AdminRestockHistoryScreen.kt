package com.example.fruitylicious.ui.admin.restock

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
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
import com.example.fruitylicious.ADMIN_RESTOCK_ENTRY
import com.example.fruitylicious.data.local.entity.RestockLogEntity
import com.example.fruitylicious.ui.shared.FruityEmptyState
import com.example.fruitylicious.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRestockHistoryScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: AdminRestockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Restock History") },
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
                onClick = { onNavigate(ADMIN_RESTOCK_ENTRY) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("New Restock") },
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White
            )
        }
    ) { padding ->
        if (uiState.restockLogs.isEmpty()) {
            FruityEmptyState(
                title = "No restock logs",
                message = "Restock records will appear here.",
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
                items(uiState.restockLogs, key = { it.restockId }) { log ->
                    AdminRestockCard(
                        log = log,
                        ingredientName = uiState.ingredientNames[log.ingredientId] ?: "Ingredient ${log.ingredientId}",
                        unit = uiState.ingredientUnits[log.ingredientId] ?: ""
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminRestockCard(
    log: RestockLogEntity,
    ingredientName: String,
    unit: String
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
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = "Restock",
                tint = Color(0xFF2E7D32)
            )

            Text(
                text = ingredientName,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Quantity Added: ${log.quantityAdded} $unit",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Supplier: ${log.supplier}",
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