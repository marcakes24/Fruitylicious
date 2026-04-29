package com.example.fruitylicious.ui.staff.adjustment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.data.repository.IngredientRepository
import com.example.fruitylicious.domain.usecase.inventory.AdjustInventoryUseCase
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

data class AdjustmentUiState(
    val ingredients: List<IngredientEntity> = emptyList(),
    val selectedIngredientId: Int = 0,
    val selectedIngredientName: String = "",
    val adjustmentAmount: String = "",
    val reason: String = "",
    val adjustmentAmountError: String? = null,
    val reasonError: String? = null,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class AdjustmentViewModel @Inject constructor(
    private val ingredientRepository: IngredientRepository,
    private val adjustInventoryUseCase: AdjustInventoryUseCase,
    private val sessionManager: SessionManager,
    private val branchConfig: BranchConfig
) : ViewModel() {

    private val branchId = sessionManager.getBranchId().takeIf { it > 0 } ?: branchConfig.branchId

    private val _uiState = MutableStateFlow(AdjustmentUiState())
    val uiState: StateFlow<AdjustmentUiState> = _uiState.asStateFlow()

    init {
        observeIngredients()
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

    fun onAdjustmentAmountChanged(value: String) {
        _uiState.update {
            it.copy(
                adjustmentAmount = value,
                adjustmentAmountError = null,
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

    fun saveAdjustment() {
        val state = _uiState.value
        val amountValue = state.adjustmentAmount.toDoubleOrNull()

        val amountError = when {
            state.adjustmentAmount.isBlank() -> "Adjustment amount is required."
            amountValue == null -> "Adjustment amount must be numeric."
            amountValue == 0.0 -> "Adjustment amount cannot be zero."
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

        if (amountError != null || reasonError != null) {
            _uiState.update {
                it.copy(
                    adjustmentAmountError = amountError,
                    reasonError = reasonError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, error = null, successMessage = null)
            }

            val result = adjustInventoryUseCase(
                ingredientId = state.selectedIngredientId,
                branchId = branchId,
                userId = sessionManager.getUserId(),
                adjustmentAmount = amountValue ?: 0.0,
                reason = state.reason
            )

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        adjustmentAmount = "",
                        reason = "",
                        isSaving = false,
                        successMessage = "Inventory adjustment saved.",
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