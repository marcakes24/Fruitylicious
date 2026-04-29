package com.example.fruitylicious.ui.admin.ingredients

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.fruitylicious.ui.shared.FruityPrimaryButton
import com.example.fruitylicious.ui.shared.FruitySectionTitle
import com.example.fruitylicious.ui.shared.FruityTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientFormScreen(
    onBack: () -> Unit,
    viewModel: IngredientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ingredient Form") },
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
                title = "Create Ingredient",
                subtitle = "Ingredients are synced as global table records"
            )

            FruityTextField(
                value = uiState.ingredientName,
                onValueChange = viewModel::onIngredientNameChanged,
                label = "Ingredient Name",
                isError = uiState.ingredientNameError != null,
                errorText = uiState.ingredientNameError,
                leadingIcon = {
                    Icon(Icons.Default.Kitchen, contentDescription = "Ingredient")
                }
            )

            FruityTextField(
                value = uiState.unitType,
                onValueChange = viewModel::onUnitTypeChanged,
                label = "Unit Type",
                isError = uiState.unitTypeError != null,
                errorText = uiState.unitTypeError
            )

            FruityTextField(
                value = uiState.estimatedWeightPerUnit,
                onValueChange = viewModel::onEstimatedWeightChanged,
                label = "Estimated Weight Per Unit",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.estimatedWeightError != null,
                errorText = uiState.estimatedWeightError
            )

            FruityTextField(
                value = uiState.lowStockThreshold,
                onValueChange = viewModel::onLowStockThresholdChanged,
                label = "Low Stock Threshold",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.lowStockThresholdError != null,
                errorText = uiState.lowStockThresholdError
            )

            FruityTextField(
                value = uiState.image,
                onValueChange = viewModel::onImageChanged,
                label = "Image URI",
                leadingIcon = {
                    Icon(Icons.Default.Image, contentDescription = "Image")
                }
            )

            if (uiState.image.isNotBlank()) {
                AsyncImage(
                    model = uiState.image,
                    contentDescription = "Ingredient image preview"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Is packaging?",
                    style = MaterialTheme.typography.bodyLarge
                )

                Switch(
                    checked = uiState.isPackaging,
                    onCheckedChange = viewModel::onIsPackagingChanged
                )
            }

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
                text = "Save Ingredient",
                onClick = viewModel::saveIngredient,
                isLoading = uiState.isSaving
            )
        }
    }
}