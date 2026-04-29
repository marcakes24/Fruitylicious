package com.example.fruitylicious.ui.admin.staffmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.domain.usecase.admin.ManageUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.absoluteValue

data class UserUiState(
    val users: List<UserEntity> = emptyList(),
    val name: String = "",
    val role: String = "staff",
    val username: String = "",
    val password: String = "",
    val nameError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val manageUserUseCase: ManageUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        observeUsers()
    }

    private fun observeUsers() {
        viewModelScope.launch {
            manageUserUseCase.observeUsers().collectLatest { users ->
                _uiState.update {
                    it.copy(users = users)
                }
            }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update {
            it.copy(
                name = value,
                nameError = null,
                error = null,
                successMessage = null
            )
        }
    }

    fun onRoleChanged(value: String) {
        _uiState.update {
            it.copy(
                role = value.lowercase(),
                error = null,
                successMessage = null
            )
        }
    }

    fun onUsernameChanged(value: String) {
        _uiState.update {
            it.copy(
                username = value,
                usernameError = null,
                error = null,
                successMessage = null
            )
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                error = null,
                successMessage = null
            )
        }
    }

    fun saveUser() {
        val state = _uiState.value

        val nameError = if (state.name.trim().isBlank()) {
            "Name is required."
        } else {
            null
        }

        val usernameError = if (state.username.trim().isBlank()) {
            "Username is required."
        } else {
            null
        }

        val passwordError = if (state.password.isBlank()) {
            "Password is required."
        } else {
            null
        }

        if (nameError != null || usernameError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    usernameError = usernameError,
                    passwordError = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, error = null, successMessage = null)
            }

            val result = manageUserUseCase.saveUser(
                userId = generateUserId(),
                name = state.name,
                role = state.role,
                username = state.username,
                password = state.password
            )

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        name = "",
                        role = "staff",
                        username = "",
                        password = "",
                        isSaving = false,
                        successMessage = "User saved.",
                        error = null
                    )
                } else {
                    it.copy(
                        isSaving = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to save user."
                    )
                }
            }
        }
    }

    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            val result = manageUserUseCase.deleteUser(userId)

            _uiState.update {
                it.copy(
                    error = result.exceptionOrNull()?.message,
                    successMessage = if (result.isSuccess) "User deleted." else null
                )
            }
        }
    }

    private fun generateUserId(): Int {
        return UUID.randomUUID()
            .mostSignificantBits
            .hashCode()
            .absoluteValue
    }
}