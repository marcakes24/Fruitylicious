package com.example.fruitylicious.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.repository.LoginResult
import com.example.fruitylicious.data.repository.SyncRepository
import com.example.fruitylicious.domain.usecase.auth.LoginUseCase
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val branchName: String = "",
    val isLoading: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val error: String? = null,
    val loggedInRole: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val syncRepository: SyncRepository,
    private val sessionManager: SessionManager,
    branchConfig: BranchConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            branchName = branchConfig.branchName
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(value: String) {
        _uiState.update {
            it.copy(
                username = value,
                usernameError = null,
                error = null
            )
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                error = null
            )
        }
    }

    fun login() {
        val currentState = _uiState.value
        val username = currentState.username.trim()
        val password = currentState.password

        val usernameError = if (username.isBlank()) "Username is required." else null
        val passwordError = if (password.isBlank()) "Password is required." else null

        if (usernameError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    usernameError = usernameError,
                    passwordError = passwordError,
                    error = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    usernameError = null,
                    passwordError = null
                )
            }

            when (val result = loginUseCase(username, password)) {
                is LoginResult.Success -> {
                    runImmediateSync()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loggedInRole = result.response.role
                        )
                    }
                }

                is LoginResult.OfflineSuccess -> {
                    runImmediateSync()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loggedInRole = result.user.role
                        )
                    }
                }

                is LoginResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    private suspend fun runImmediateSync() {
        val lastPulledAt = sessionManager.getLastPulledAt()
        val syncResult = syncRepository.sync(lastPulledAt)
        val now = System.currentTimeMillis()

        sessionManager.saveSyncStatus(
            syncedAt = now,
            success = syncResult.success,
            message = syncResult.message
        )

        if (syncResult.success) {
            sessionManager.saveLastPulledAt(now)
        }

        println("FRUITY_SYNC_LOGIN: success=${syncResult.success}, pushed=${syncResult.pushedCount}, pulled=${syncResult.pulledCount}, message=${syncResult.message}")
    }

    fun consumeLoginNavigation() {
        _uiState.update {
            it.copy(loggedInRole = null)
        }
    }
}