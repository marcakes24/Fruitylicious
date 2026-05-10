package com.example.fruitylicious.ui.admin.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.data.remote.api.HealthApi
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.BranchConfigManager
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class BranchSettingsUiState(
    val branchId: Int = 0,
    val branchName: String = "",
    val address: String = "",
    val contactNumber: String = "",
    val gcashAccountName: String = "",
    val gcashAccountNumber: String = "",
    val gcashQrImage: String? = null,
    val gcashQrImageType: String? = null,
    val originalBranch: BranchEntity? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    
    // Branch Configuration fields
    val configBranchIdText: String = "",
    val configBranchName: String = "",
    val configApiBaseUrl: String = "",
    val configApiKey: String = "",
    val isApiKeyVisible: Boolean = false,
    val isTestingConnection: Boolean = false,
    val testConnectionSuccess: String? = null,
    val testConnectionError: String? = null,
    val isAdmin: Boolean = false,
    val requiresRestart: Boolean = false
) {
    val hasChanges: Boolean
        get() {
            val original = originalBranch ?: return false
            return branchName != original.branchName ||
                    address != original.address ||
                    contactNumber != original.contactNumber ||
                    gcashAccountName != (original.gcashAccountName ?: "") ||
                    gcashAccountNumber != (original.gcashAccountNumber ?: "") ||
                    gcashQrImage != original.gcashQrImage ||
                    gcashQrImageType != original.gcashQrImageType
        }
        
    val hasConfigChanges: Boolean = false // This will be handled by comparing with current config
}

@HiltViewModel
class BranchSettingsViewModel @Inject constructor(
    private val branchDao: BranchDao,
    private val branchConfigManager: BranchConfigManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BranchSettingsUiState())
    val uiState: StateFlow<BranchSettingsUiState> = _uiState.asStateFlow()

    init {
        loadBranchData()
        loadBranchConfig()
        checkAdminStatus()
    }

    private fun checkAdminStatus() {
        _uiState.update { it.copy(isAdmin = sessionManager.isAdmin()) }
    }

    private fun loadBranchConfig() {
        val config = branchConfigManager.getBranchConfig()
        _uiState.update {
            it.copy(
                configBranchIdText = config.branchId.toString(),
                configBranchName = config.branchName,
                configApiBaseUrl = config.apiBaseUrl,
                configApiKey = config.apiKey
            )
        }
    }

    private fun loadBranchData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val branch = branchDao.getBranchById(branchConfigManager.branchId)
            if (branch != null) {
                _uiState.update {
                    it.copy(
                        branchId = branch.branchId,
                        branchName = branch.branchName,
                        address = branch.address,
                        contactNumber = branch.contactNumber,
                        gcashAccountName = branch.gcashAccountName ?: "",
                        gcashAccountNumber = branch.gcashAccountNumber ?: "",
                        gcashQrImage = branch.gcashQrImage,
                        gcashQrImageType = branch.gcashQrImageType,
                        originalBranch = branch,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Branch not found in database") }
            }
        }
    }

    fun onBranchNameChange(value: String) {
        _uiState.update { it.copy(branchName = value) }
    }

    fun onAddressChange(value: String) {
        _uiState.update { it.copy(address = value) }
    }

    fun onContactNumberChange(value: String) {
        _uiState.update { it.copy(contactNumber = value) }
    }

    fun onGcashAccountNameChange(value: String) {
        _uiState.update { it.copy(gcashAccountName = value) }
    }

    fun onGcashAccountNumberChange(value: String) {
        _uiState.update { it.copy(gcashAccountNumber = value) }
    }

    fun onQrImageSelected(base64: String, mimeType: String) {
        _uiState.update {
            it.copy(
                gcashQrImage = base64,
                gcashQrImageType = mimeType
            )
        }
    }

    fun onRemoveQrImage() {
        _uiState.update {
            it.copy(
                gcashQrImage = null,
                gcashQrImageType = null
            )
        }
    }

    // Config change handlers
    fun onConfigBranchIdChanged(value: String) {
        _uiState.update { it.copy(configBranchIdText = value) }
    }

    fun onConfigBranchNameChanged(value: String) {
        _uiState.update { it.copy(configBranchName = value) }
    }

    fun onConfigApiBaseUrlChanged(value: String) {
        _uiState.update { it.copy(configApiBaseUrl = value) }
    }

    fun onConfigApiKeyChanged(value: String) {
        _uiState.update { it.copy(configApiKey = value) }
    }

    fun toggleApiKeyVisibility() {
        _uiState.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }
    }

    fun saveSettings() {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val updatedBranch = BranchEntity(
                    branchId = currentState.branchId,
                    branchName = currentState.branchName,
                    address = currentState.address,
                    contactNumber = currentState.contactNumber,
                    gcashAccountName = currentState.gcashAccountName.ifBlank { null },
                    gcashAccountNumber = currentState.gcashAccountNumber.ifBlank { null },
                    gcashQrImage = currentState.gcashQrImage,
                    gcashQrImageType = currentState.gcashQrImageType,
                    lastModified = System.currentTimeMillis(),
                    isSynced = false,
                    syncedAt = null
                )
                branchDao.upsertBranches(listOf(updatedBranch))
                _uiState.update { 
                    it.copy(
                        isSaving = false, 
                        saveSuccess = true,
                        originalBranch = updatedBranch 
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Failed to save: ${e.message}") }
            }
        }
    }

    fun saveConfig() {
        val state = _uiState.value
        if (state.isSaving) return

        val branchId = state.configBranchIdText.toIntOrNull()
        if (branchId == null || branchId <= 0) {
            _uiState.update { it.copy(error = "Invalid Branch ID") }
            return
        }
        if (state.configBranchName.isBlank()) {
            _uiState.update { it.copy(error = "Branch Name cannot be empty") }
            return
        }
        if (state.configApiBaseUrl.isBlank() || (!state.configApiBaseUrl.startsWith("http://") && !state.configApiBaseUrl.startsWith("https://"))) {
            _uiState.update { it.copy(error = "Invalid API Base URL. Must start with http:// or https://") }
            return
        }
        if (state.configApiKey.isBlank()) {
            _uiState.update { it.copy(error = "API Key cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val newConfig = BranchConfig(
                    branchId = branchId,
                    branchName = state.configBranchName.trim(),
                    apiBaseUrl = state.configApiBaseUrl.trim(),
                    apiKey = state.configApiKey.trim()
                )
                branchConfigManager.saveBranchConfig(newConfig)
                _uiState.update { 
                    it.copy(
                        isSaving = false, 
                        saveSuccess = true,
                        requiresRestart = true 
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Failed to save config: ${e.message}") }
            }
        }
    }

    fun resetConfigToDefault() {
        branchConfigManager.resetBranchConfig()
        loadBranchConfig()
        _uiState.update { it.copy(saveSuccess = true) }
    }

    fun testConnection() {
        val state = _uiState.value
        if (state.isTestingConnection) return

        val baseUrl = state.configApiBaseUrl.trim()
        if (baseUrl.isBlank() || (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://"))) {
            _uiState.update { it.copy(testConnectionError = "Invalid URL") }
            return
        }

        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testConnectionError = null, testConnectionSuccess = null) }
            try {
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build()

                val tempRetrofit = Retrofit.Builder()
                    .baseUrl(normalizedUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val healthApi = tempRetrofit.create(HealthApi::class.java)
                val response = healthApi.health()

                if (response.isSuccessful && response.body()?.status == "UP") {
                    _uiState.update { it.copy(isTestingConnection = false, testConnectionSuccess = "Connection successful. Server is UP.") }
                } else {
                    val msg = if (response.isSuccessful) "Backend responded but health status is not UP." 
                             else "Server returned error: ${response.code()}"
                    _uiState.update { it.copy(isTestingConnection = false, testConnectionError = msg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isTestingConnection = false, testConnectionError = "Cannot connect to backend. Check URL, Wi-Fi/hotspot, firewall, or port.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(saveSuccess = false, error = null, testConnectionError = null, testConnectionSuccess = null) }
    }
}
