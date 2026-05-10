package com.example.fruitylicious.ui.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.UserEntity
import com.example.fruitylicious.data.repository.UserRepository
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue

data class UserManagementUiState(
    val users: List<UserEntity> = emptyList(),
    val currentUserId: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState(
        currentUserId = sessionManager.getUserId()
    ))
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        observeUsers()
    }

    private fun observeUsers() {
        viewModelScope.launch {
            userRepository.observeUsers().collectLatest { users ->
                _uiState.update {
                    it.copy(
                        users = users,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun saveUser(
        existingUserId: Int?,
        name: String,
        username: String,
        password: String,
        role: String
    ) {
        viewModelScope.launch {
            val existingUser = existingUserId?.let { id ->
                _uiState.value.users.firstOrNull { it.userId == id }
            }

            val result = userRepository.saveUser(
                userId = existingUserId ?: generateId(),
                name = name,
                role = role,
                username = username,
                password = if (password.isBlank()) {
                    existingUser?.password ?: ""
                } else {
                    password
                }
            )

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        successMessage = "User saved.",
                        error = null
                    )
                }
            }.onFailure { e ->
                setError(e.message ?: "Failed to save user.")
            }
        }
    }

    fun deleteUser(userId: Int) {
        if (userId == sessionManager.getUserId()) {
            setError("You cannot delete your own account.")
            return
        }

        viewModelScope.launch {
            userRepository.deleteUser(userId).onSuccess {
                _uiState.update {
                    it.copy(
                        successMessage = "User deleted.",
                        error = null
                    )
                }
            }.onFailure { e ->
                setError(e.message ?: "Failed to delete user.")
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(error = null, successMessage = null)
        }
    }

    private fun setError(message: String) {
        _uiState.update {
            it.copy(error = message, successMessage = null)
        }
    }

    private fun generateId(): Int {
        return System.currentTimeMillis().hashCode().absoluteValue
    }
}