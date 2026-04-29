package com.example.fruitylicious.ui.admin.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.domain.usecase.admin.ManageIngredientUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue

data class IngredientUiState(
    val ingredients: List<IngredientEntity> = emptyList(),
    val ingredientName: String = "",
    val unitType: String = "",
    val estimatedWeightPerUnit: String = "",
    val lowStockThreshold: String = "",
    val image: String = "",
    val isPackaging: Boolean = false,
    val ingredientNameError: String? = null,
    val unitTypeError: String? = null,
    val estimatedWeightError: String? = null,
    val lowStockThresholdError: String? = null,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class IngredientViewModel @Inject constructor(
    private val manageIngredientUseCase: ManageIngredientUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(IngredientUiState())
    val uiState: StateFlow<IngredientUiState> = _uiState.asStateFlow()

    init {
        observeIngredients()
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            manageIngredientUseCase.observeIngredients().collectLatest { ingredients ->
                _uiState.update {
                    it.copy(ingredients = ingredients)
                }
            }
        }
    }

    fun onIngredientNameChanged(value: String) {
        _uiState.update { it.copy(ingredientName = value, ingredientNameError = null, error = null, successMessage = null) }
    }

    fun onUnitTypeChanged(value: String) {
        _uiState.update { it.copy(unitType = value, unitTypeError = null, error = null, successMessage = null) }
    }

    fun onEstimatedWeightChanged(value: String) {
        _uiState.update { it.copy(estimatedWeightPerUnit = value, estimatedWeightError = null, error = null, successMessage = null) }
    }

    fun onLowStockThresholdChanged(value: String) {
        _uiState.update { it.copy(lowStockThreshold = value, lowStockThresholdError = null, error = null, successMessage = null) }
    }

    fun onImageChanged(value: String) {
        _uiState.update { it.copy(image = value, error = null, successMessage = null) }
    }

    fun onIsPackagingChanged(value: Boolean) {
        _uiState.update { it.copy(isPackaging = value) }
    }

    fun saveIngredient() {
        val state = _uiState.value
        val estimatedWeight = state.estimatedWeightPerUnit.toDoubleOrNull()
        val threshold = state.lowStockThreshold.toDoubleOrNull()

        val nameError = if (state.ingredientName.trim().isBlank()) "Ingredient name is required." else null
        val unitError = if (state.unitType.trim().isBlank()) "Unit type is required." else null
        val weightError = when {
            state.estimatedWeightPerUnit.isBlank() -> "Estimated weight is required."
            estimatedWeight == null -> "Estimated weight must be numeric."
            estimatedWeight < 0.0 -> "Estimated weight cannot be negative."
            else -> null
        }
        val thresholdError = when {
            state.lowStockThreshold.isBlank() -> "Low stock threshold is required."
            threshold == null -> "Low stock threshold must be numeric."
            threshold < 0.0 -> "Low stock threshold cannot be negative."
            else -> null
        }

        if (nameError != null || unitError != null || weightError != null || thresholdError != null) {
            _uiState.update {
                it.copy(
                    ingredientNameError = nameError,
                    unitTypeError = unitError,
                    estimatedWeightError = weightError,
                    lowStockThresholdError = thresholdError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, error = null, successMessage = null)
            }

            val generatedId = System.currentTimeMillis().hashCode().absoluteValue

            val result = manageIngredientUseCase.saveIngredient(
                ingredientId = generatedId,
                image = state.image.ifBlank { null },
                ingredientName = state.ingredientName,
                unitType = state.unitType,
                estimatedWeightPerUnit = estimatedWeight ?: 0.0,
                isPackaging = state.isPackaging,
                lowStockThreshold = threshold ?: 0.0
            )

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        ingredientName = "",
                        unitType = "",
                        estimatedWeightPerUnit = "",
                        lowStockThreshold = "",
                        image = "",
                        isPackaging = false,
                        isSaving = false,
                        successMessage = "Ingredient saved.",
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

    fun deleteIngredient(ingredientId: Int) {
        viewModelScope.launch {
            val result = manageIngredientUseCase.deleteIngredient(ingredientId)
            _uiState.update {
                it.copy(error = result.exceptionOrNull()?.message)
            }
        }
    }
}