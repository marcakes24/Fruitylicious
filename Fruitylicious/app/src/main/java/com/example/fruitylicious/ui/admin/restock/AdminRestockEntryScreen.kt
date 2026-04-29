package com.example.fruitylicious.ui.admin.restock

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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
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
import com.example.fruitylicious.ui.shared.FruitySectionTitle
import com.example.fruitylicious.ui.shared.FruityTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRestockEntryScreen(
    onBack: () -> Unit,
    viewModel: AdminRestockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Restock Entry") },
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
                title = "Add Restock",
                subtitle = "Restocking increases current branch inventory"
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = uiState.selectedIngredientName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ingredient") },
                    leadingIcon = {
                        Icon(Icons.Default.Inventory2, contentDescription = "Ingredient")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    uiState.ingredients.forEach { ingredient ->
                        DropdownMenuItem(
                            text = { Text(ingredient.ingredientName) },
                            onClick = {
                                viewModel.onIngredientSelected(ingredient.ingredientId)
                                expanded = false
                            }
                        )
                    }
                }
            }

            val ingredientError = uiState.ingredientError
            if (!ingredientError.isNullOrBlank()) {
                Text(
                    text = ingredientError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            FruityTextField(
                value = uiState.quantityAdded,
                onValueChange = viewModel::onQuantityAddedChanged,
                label = "Quantity Added",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.quantityAddedError != null,
                errorText = uiState.quantityAddedError
            )

            FruityTextField(
                value = uiState.supplier,
                onValueChange = viewModel::onSupplierChanged,
                label = "Supplier",
                isError = uiState.supplierError != null,
                errorText = uiState.supplierError,
                leadingIcon = {
                    Icon(Icons.Default.LocalShipping, contentDescription = "Supplier")
                }
            )

            val success = uiState.successMessage
            if (!success.isNullOrBlank()) {
                Text(
                    text = success,
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
                text = "Save Restock",
                onClick = viewModel::saveRestock,
                isLoading = uiState.isSaving
            )
        }
    }
}