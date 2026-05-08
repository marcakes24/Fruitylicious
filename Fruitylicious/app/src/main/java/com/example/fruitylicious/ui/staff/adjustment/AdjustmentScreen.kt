package com.example.fruitylicious.ui.staff.adjustment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fruitylicious.ui.shared.FruityPrimaryButton
import com.example.fruitylicious.ui.shared.FruitySearchableDropdown
import com.example.fruitylicious.ui.shared.FruitySectionTitle
import com.example.fruitylicious.ui.shared.FruityTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustmentScreen(
    onBack: () -> Unit,
    viewModel: AdjustmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredIngredients = remember(uiState.ingredients, searchQuery) {
        if (searchQuery.isEmpty()) {
            uiState.ingredients
        } else {
            uiState.ingredients.filter {
                it.ingredientName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Adjustment") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FruitySectionTitle(
                title = "Adjust Stock",
                subtitle = "Use positive values to add and negative values to deduct"
            )

            FruitySearchableDropdown(
                value = searchQuery.ifEmpty { uiState.selectedIngredientName },
                onValueChange = {
                    searchQuery = it
                },
                options = filteredIngredients,
                onOptionClick = {
                    viewModel.onIngredientSelected(it.ingredientId)
                    searchQuery = it.ingredientName
                    expanded = false
                },
                label = "Ingredient",
                expanded = expanded,
                onExpandedChange = { expanded = it },
                placeholder = "Select Ingredient",
                itemContent = { ingredient ->
                    Text(ingredient.ingredientName)
                }
            )

            FruityTextField(
                value = uiState.adjustmentAmount,
                onValueChange = viewModel::onAdjustmentAmountChanged,
                label = "Adjustment Amount",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.adjustmentAmountError != null,
                errorText = uiState.adjustmentAmountError
            )

            FruityTextField(
                value = uiState.reason,
                onValueChange = viewModel::onReasonChanged,
                label = "Reason",
                singleLine = false,
                isError = uiState.reasonError != null,
                errorText = uiState.reasonError
            )

            val message = uiState.successMessage
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val error = uiState.error
            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            FruityPrimaryButton(
                text = "Save Adjustment",
                onClick = viewModel::saveAdjustment,
                isLoading = uiState.isSaving
            )
        }
    }
}