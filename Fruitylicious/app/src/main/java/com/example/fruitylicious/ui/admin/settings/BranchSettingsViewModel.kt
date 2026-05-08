package com.example.fruitylicious.ui.admin.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.dao.BranchDao
import com.example.fruitylicious.data.local.entity.BranchEntity
import com.example.fruitylicious.util.BranchConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val error: String? = null
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
}

@HiltViewModel
class BranchSettingsViewModel @Inject constructor(
    private val branchDao: BranchDao,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(BranchSettingsUiState())
    val uiState: StateFlow<BranchSettingsUiState> = _uiState.asStateFlow()

    init {
        loadBranchData()
    }

    private fun loadBranchData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val branch = branchDao.getBranchById(branchConfig.branchId)
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

    fun clearMessages() {
        _uiState.update { it.copy(saveSuccess = false, error = null) }
    }
}
