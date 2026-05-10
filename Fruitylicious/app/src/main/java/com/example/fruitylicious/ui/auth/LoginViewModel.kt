package com.example.fruitylicious.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.repository.LoginResult
import com.example.fruitylicious.data.repository.SyncRepository
import com.example.fruitylicious.domain.usecase.auth.LoginUseCase
import com.example.fruitylicious.util.BranchConfigManager
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val branchName: String = "",
    val isLoading: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val termsError: String? = null,
    val error: String? = null,
    val loggedInRole: String? = null,
    val agreedToTerms: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val syncRepository: SyncRepository,
    private val sessionManager: SessionManager,
    branchConfigManager: BranchConfigManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            branchName = branchConfigManager.branchName
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

    fun onTermsAgreedChanged(agreed: Boolean) {
        _uiState.update {
            it.copy(
                agreedToTerms = agreed,
                termsError = null
            )
        }
    }

    fun login() {
        val currentState = _uiState.value
        val username = currentState.username.trim().lowercase()
        val password = currentState.password

        val usernameError = if (username.isBlank()) {
            "Username is required."
        } else {
            null
        }

        val passwordError = if (password.isBlank()) {
            "Password is required."
        } else {
            null
        }

        val termsError = if (!currentState.agreedToTerms) {
            "You must agree to the Terms and Conditions."
        } else {
            null
        }

        if (usernameError != null || passwordError != null || termsError != null) {
            _uiState.update {
                it.copy(
                    usernameError = usernameError,
                    passwordError = passwordError,
                    termsError = termsError,
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
                    passwordError = null,
                    termsError = null
                )
            }

            when (val result = loginUseCase(username, password)) {
                is LoginResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loggedInRole = result.response.role,
                            error = null
                        )
                    }

                    launchSyncInBackground()
                }

                is LoginResult.OfflineSuccess -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loggedInRole = result.user.role,
                            error = null
                        )
                    }

                    launchSyncInBackground()
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

    private fun launchSyncInBackground() {
        viewModelScope.launch {
            runCatching {
                runImmediateSync()
            }.onFailure { error ->
                val now = System.currentTimeMillis()

                sessionManager.saveSyncStatus(
                    syncedAt = now,
                    success = false,
                    message = error.message ?: "Background sync failed."
                )

                println("FRUITY_SYNC_LOGIN_BACKGROUND_ERROR: ${error.message}")
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

        println(
            "FRUITY_SYNC_LOGIN_BACKGROUND: " +
                    "success=${syncResult.success}, " +
                    "pushed=${syncResult.pushedCount}, " +
                    "pulled=${syncResult.pulledCount}, " +
                    "message=${syncResult.message}"
        )
    }

    fun consumeLoginNavigation() {
        _uiState.update {
            it.copy(loggedInRole = null)
        }
    }
}