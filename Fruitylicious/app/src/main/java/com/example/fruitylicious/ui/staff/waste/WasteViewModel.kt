package com.example.fruitylicious.ui.staff.waste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.local.entity.WasteLogEntity
import com.example.fruitylicious.data.repository.IngredientRepository
import com.example.fruitylicious.data.repository.WasteRepository
import com.example.fruitylicious.domain.usecase.waste.LogWasteUseCase
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

data class WasteUiState(
    val ingredients: List<IngredientEntity> = emptyList(),
    val wasteLogs: List<WasteLogEntity> = emptyList(),
    val selectedIngredientId: Int = 0,
    val selectedIngredientName: String = "",
    val quantity: String = "",
    val reason: String = "",
    val image: String = "",
    val quantityError: String? = null,
    val reasonError: String? = null,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class WasteViewModel @Inject constructor(
    private val ingredientRepository: IngredientRepository,
    private val wasteRepository: WasteRepository,
    private val logWasteUseCase: LogWasteUseCase,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val branchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(WasteUiState())
    val uiState: StateFlow<WasteUiState> = _uiState.asStateFlow()

    init {
        observeIngredients()
        observeWasteLogs()
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientRepository.observeIngredients().collectLatest { ingredients ->
                _uiState.update {
                    val selected = ingredients.firstOrNull { ingredient ->
                        ingredient.ingredientId == it.selectedIngredientId
                    }

                    it.copy(
                        ingredients = ingredients,
                        selectedIngredientName = selected?.ingredientName ?: it.selectedIngredientName
                    )
                }
            }
        }
    }

    private fun observeWasteLogs() {
        viewModelScope.launch {
            wasteRepository.observeWasteLogs(branchId).collectLatest { logs ->
                _uiState.update {
                    it.copy(wasteLogs = logs)
                }
            }
        }
    }

    fun onIngredientSelected(ingredientId: Int) {
        val ingredient = _uiState.value.ingredients.firstOrNull { it.ingredientId == ingredientId }

        _uiState.update {
            it.copy(
                selectedIngredientId = ingredientId,
                selectedIngredientName = ingredient?.ingredientName ?: "",
                error = null,
                successMessage = null
            )
        }
    }

    fun onQuantityChanged(value: String) {
        _uiState.update {
            it.copy(
                quantity = value,
                quantityError = null,
                error = null,
                successMessage = null
            )
        }
    }

    fun onReasonChanged(value: String) {
        _uiState.update {
            it.copy(
                reason = value,
                reasonError = null,
                error = null,
                successMessage = null
            )
        }
    }

    fun onImageChanged(value: String) {
        _uiState.update {
            it.copy(
                image = value,
                error = null,
                successMessage = null
            )
        }
    }

    fun saveWaste() {
        val state = _uiState.value
        val quantityValue = state.quantity.toDoubleOrNull()

        val quantityError = when {
            state.quantity.isBlank() -> "Quantity is required."
            quantityValue == null -> "Quantity must be numeric."
            quantityValue <= 0.0 -> "Quantity must be greater than zero."
            else -> null
        }

        val reasonError = if (state.reason.trim().isBlank()) {
            "Reason is required."
        } else {
            null
        }

        if (state.selectedIngredientId <= 0) {
            _uiState.update {
                it.copy(error = "Ingredient is required.")
            }
            return
        }

        if (quantityError != null || reasonError != null) {
            _uiState.update {
                it.copy(
                    quantityError = quantityError,
                    reasonError = reasonError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, error = null, successMessage = null)
            }

            val result = logWasteUseCase(
                ingredientId = state.selectedIngredientId,
                branchId = branchId,
                userId = sessionManager.getUserId(),
                quantity = quantityValue ?: 0.0,
                image = state.image.ifBlank { null },
                reason = state.reason
            )

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        quantity = "",
                        reason = "",
                        image = "",
                        isSaving = false,
                        successMessage = "Waste log saved.",
                        error = null
                    )
                } else {
                    it.copy(
                        isSaving = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }
}