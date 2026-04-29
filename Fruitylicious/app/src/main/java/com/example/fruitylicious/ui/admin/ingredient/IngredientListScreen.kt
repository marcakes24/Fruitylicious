package com.example.fruitylicious.ui.admin.ingredients

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.fruitylicious.ADMIN_INGREDIENT_FORM
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.ui.shared.FruityEmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientListScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: IngredientViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Ingredients") },
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
                onClick = { onNavigate(ADMIN_INGREDIENT_FORM) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("New Ingredient") },
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White
            )
        }
    ) { padding ->
        if (uiState.ingredients.isEmpty()) {
            FruityEmptyState(
                title = "No ingredients",
                message = "Add ingredients used by recipes and inventory.",
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
                items(uiState.ingredients, key = { it.ingredientId }) { ingredient ->
                    IngredientAdminCard(
                        ingredient = ingredient,
                        onDelete = { viewModel.deleteIngredient(ingredient.ingredientId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientAdminCard(
    ingredient: IngredientEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!ingredient.image.isNullOrBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(ingredient.image),
                    contentDescription = ingredient.ingredientName,
                    modifier = Modifier
                        .heightIn(min = 72.dp)
                        .weight(0.22f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Kitchen,
                    contentDescription = ingredient.ingredientName,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.weight(0.12f)
                )
            }

            Column(
                modifier = Modifier.weight(0.70f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = ingredient.ingredientName,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Unit: ${ingredient.unitType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D6D6D)
                )

                Text(
                    text = "Low stock threshold: ${ingredient.lowStockThreshold}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D6D6D)
                )

                Text(
                    text = if (ingredient.isPackaging) "Packaging" else "Ingredient",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1B5E20)
                )
            }

            FilledTonalIconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}