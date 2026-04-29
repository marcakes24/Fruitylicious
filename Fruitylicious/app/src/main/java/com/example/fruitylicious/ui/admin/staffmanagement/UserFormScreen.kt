package com.example.fruitylicious.ui.admin.staffmanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fruitylicious.ui.shared.FruityPrimaryButton
import com.example.fruitylicious.ui.shared.FruitySectionTitle
import com.example.fruitylicious.ui.shared.FruityTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormScreen(
    onBack: () -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Form") },
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
                title = "Create User",
                subtitle = "Users are stored locally first and synced later"
            )

            FruityTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                label = "Name",
                isError = uiState.nameError != null,
                errorText = uiState.nameError,
                leadingIcon = {
                    Icon(Icons.Default.Badge, contentDescription = "Name")
                }
            )

            FruityTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChanged,
                label = "Username",
                isError = uiState.usernameError != null,
                errorText = uiState.usernameError,
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = "Username")
                }
            )

            FruityTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChanged,
                label = "Password",
                isError = uiState.passwordError != null,
                errorText = uiState.passwordError,
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = "Password")
                }
            )

            Text(
                text = "Role",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1B5E20)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("staff", "admin").forEach { role ->
                    FilterChip(
                        selected = uiState.role == role,
                        onClick = { viewModel.onRoleChanged(role) },
                        label = { Text(role.replaceFirstChar { it.uppercase() }) },
                        leadingIcon = if (uiState.role == role) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = role
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }

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
                text = "Save User",
                onClick = viewModel::saveUser,
                isLoading = uiState.isSaving
            )
        }
    }
}