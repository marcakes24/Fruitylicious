package com.example.fruitylicious.ui.admin.recipes

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
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Kitchen
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
fun RecipeFormScreen(
    onBack: () -> Unit,
    viewModel: RecipeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var productExpanded by remember { mutableStateOf(false) }
    var ingredientExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipe Form") },
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
                title = "Create Recipe Line",
                subtitle = "Each line defines one ingredient requirement for a product"
            )

            // ===================== PRODUCT DROPDOWN =====================
            ExposedDropdownMenuBox(
                expanded = productExpanded,
                onExpandedChange = { productExpanded = !productExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.selectedProductName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Product") },
                    leadingIcon = {
                        Icon(Icons.Default.Fastfood, contentDescription = "Product")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded)
                    },
                    modifier = Modifier.menuAnchor(),
                    isError = uiState.productError != null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                ExposedDropdownMenu(
                    expanded = productExpanded,
                    onDismissRequest = { productExpanded = false }
                ) {
                    uiState.products.forEach { product ->
                        DropdownMenuItem(
                            text = { Text(product.productName) },
                            onClick = {
                                viewModel.onProductSelected(product.productId)
                                productExpanded = false
                            }
                        )
                    }
                }
            }

            val productError = uiState.productError
            if (!productError.isNullOrBlank()) {
                Text(
                    text = productError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ===================== INGREDIENT DROPDOWN =====================
            ExposedDropdownMenuBox(
                expanded = ingredientExpanded,
                onExpandedChange = { ingredientExpanded = !ingredientExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.selectedIngredientName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ingredient") },
                    leadingIcon = {
                        Icon(Icons.Default.Kitchen, contentDescription = "Ingredient")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = ingredientExpanded)
                    },
                    modifier = Modifier.menuAnchor(),
                    isError = uiState.ingredientError != null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                ExposedDropdownMenu(
                    expanded = ingredientExpanded,
                    onDismissRequest = { ingredientExpanded = false }
                ) {
                    uiState.ingredients.forEach { ingredient ->
                        DropdownMenuItem(
                            text = { Text(ingredient.ingredientName) },
                            onClick = {
                                viewModel.onIngredientSelected(ingredient.ingredientId)
                                ingredientExpanded = false
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

            // ===================== QUANTITY FIELD =====================
            FruityTextField(
                value = uiState.quantityRequired,
                onValueChange = viewModel::onQuantityRequiredChanged,
                label = "Quantity Required",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.quantityRequiredError != null,
                errorText = uiState.quantityRequiredError
            )

            // ===================== SUCCESS MESSAGE =====================
            val message = uiState.successMessage
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // ===================== GENERAL ERROR =====================
            val error = uiState.error
            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // ===================== SAVE BUTTON =====================
            FruityPrimaryButton(
                text = "Save Recipe Line",
                onClick = viewModel::saveRecipe,
                isLoading = uiState.isSaving
            )
        }
    }
}