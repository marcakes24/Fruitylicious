package com.example.fruitylicious.ui.admin.restock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.RestockLogEntity
import com.example.fruitylicious.data.repository.IngredientRepository
import com.example.fruitylicious.data.repository.RestockRepository
import com.example.fruitylicious.domain.usecase.restock.RestockUseCase
import com.example.fruitylicious.util.BranchConfig
import com.example.fruitylicious.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminRestockUiState(
    val ingredients: List<IngredientEntity> = emptyList(),
    val restockLogs: List<RestockLogEntity> = emptyList(),
    val ingredientNames: Map<Int, String> = emptyMap(),
    val ingredientUnits: Map<Int, String> = emptyMap(),

    val selectedIngredientId: Int = 0,
    val selectedIngredientName: String = "",

    val quantityAdded: String = "",
    val supplier: String = "",

    val ingredientError: String? = null,
    val quantityAddedError: String? = null,
    val supplierError: String? = null,

    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class AdminRestockViewModel @Inject constructor(
    private val ingredientRepository: IngredientRepository,
    private val restockRepository: RestockRepository,
    private val restockUseCase: RestockUseCase,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val branchId: Int =
        sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(AdminRestockUiState())
    val uiState: StateFlow<AdminRestockUiState> = _uiState.asStateFlow()

    init {
        observeIngredients()
        observeRestockLogs()
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientRepository.observeIngredients().collectLatest { ingredients ->
                _uiState.update { state ->
                    val selectedIngredient = ingredients.firstOrNull {
                        it.ingredientId == state.selectedIngredientId
                    }

                    state.copy(
                        ingredients = ingredients,
                        ingredientNames = ingredients.associate {
                            it.ingredientId to it.ingredientName
                        },
                        ingredientUnits = ingredients.associate {
                            it.ingredientId to it.unitType
                        },
                        selectedIngredientName = selectedIngredient?.ingredientName
                            ?: state.selectedIngredientName
                    )
                }
            }
        }
    }

    private fun observeRestockLogs() {
        viewModelScope.launch {
            restockRepository.observeRestockLogs(branchId).collectLatest { logs ->
                _uiState.update {
                    it.copy(
                        restockLogs = logs,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onIngredientSelected(ingredientId: Int) {
        val ingredient = _uiState.value.ingredients.firstOrNull {
            it.ingredientId == ingredientId
        }

        _uiState.update {
            it.copy(
                selectedIngredientId = ingredientId,
                selectedIngredientName = ingredient?.ingredientName ?: "",
                ingredientError = null,
                error = null,
                successMessage = null
            )
        }
    }

    fun onQuantityAddedChanged(value: String) {
        _uiState.update {
            it.copy(
                quantityAdded = value,
                quantityAddedError = null,
                error = null,
                successMessage = null
            )
        }
    }

    fun onSupplierChanged(value: String) {
        _uiState.update {
            it.copy(
                supplier = value,
                supplierError = null,
                error = null,
                successMessage = null
            )
        }
    }

    fun saveRestock() {
        val state = _uiState.value
        val quantityValue = state.quantityAdded.toDoubleOrNull()

        val ingredientError = if (state.selectedIngredientId <= 0) {
            "Ingredient is required."
        } else {
            null
        }

        val quantityError = when {
            state.quantityAdded.isBlank() -> "Quantity added is required."
            quantityValue == null -> "Quantity added must be numeric."
            quantityValue <= 0.0 -> "Quantity added must be greater than zero."
            else -> null
        }

        val supplierError = if (state.supplier.trim().isBlank()) {
            "Supplier is required."
        } else {
            null
        }

        if (ingredientError != null || quantityError != null || supplierError != null) {
            _uiState.update {
                it.copy(
                    ingredientError = ingredientError,
                    quantityAddedError = quantityError,
                    supplierError = supplierError,
                    successMessage = null,
                    error = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    successMessage = null,
                    error = null
                )
            }

            val result = restockUseCase(
                ingredientId = state.selectedIngredientId,
                branchId = branchId,
                userId = sessionManager.getUserId(),
                quantityAdded = quantityValue ?: 0.0,
                supplier = state.supplier
            )

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        quantityAdded = "",
                        supplier = "",
                        quantityAddedError = null,
                        supplierError = null,
                        ingredientError = null,
                        isSaving = false,
                        successMessage = "Restock saved.",
                        error = null
                    )
                } else {
                    it.copy(
                        isSaving = false,
                        successMessage = null,
                        error = result.exceptionOrNull()?.message ?: "Failed to save restock."
                    )
                }
            }
        }
    }

    fun clearForm() {
        _uiState.update {
            it.copy(
                selectedIngredientId = 0,
                selectedIngredientName = "",
                quantityAdded = "",
                supplier = "",
                ingredientError = null,
                quantityAddedError = null,
                supplierError = null,
                successMessage = null,
                error = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                successMessage = null,
                error = null
            )
        }
    }
}