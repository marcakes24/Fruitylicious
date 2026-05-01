package com.example.fruitylicious.ui.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.UserDao
import com.example.fruitylicious.data.local.entity.UserEntity
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
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        observeUsers()
    }

    private fun observeUsers() {
        viewModelScope.launch {
            userDao.observeUsers().collectLatest { users ->
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
        val cleanName = name.trim()
        val cleanUsername = username.trim()
        val cleanRole = role.lowercase()

        if (cleanName.isBlank()) {
            setError("Name is required.")
            return
        }

        if (cleanUsername.isBlank()) {
            setError("Username is required.")
            return
        }

        if (existingUserId == null && password.isBlank()) {
            setError("Password is required.")
            return
        }

        viewModelScope.launch {
            val existingUser = existingUserId?.let { id ->
                _uiState.value.users.firstOrNull { it.userId == id }
            }

            val now = System.currentTimeMillis()

            userDao.upsertUser(
                UserEntity(
                    userId = existingUserId ?: generateId(),
                    name = cleanName,
                    role = cleanRole,
                    username = cleanUsername,
                    password = if (password.isBlank()) {
                        existingUser?.password ?: ""
                    } else {
                        password
                    },
                    lastModified = now,
                    isSynced = false,
                    syncedAt = null
                )
            )

            _uiState.update {
                it.copy(
                    successMessage = "User saved.",
                    error = null
                )
            }
        }
    }

    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            userDao.deleteUser(userId)

            _uiState.update {
                it.copy(
                    successMessage = "User deleted.",
                    error = null
                )
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