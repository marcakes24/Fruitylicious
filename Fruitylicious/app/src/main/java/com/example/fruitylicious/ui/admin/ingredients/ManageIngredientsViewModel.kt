package com.example.fruitylicious.ui.admin.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitylicious.data.local.entity.IngredientEntity
import com.example.fruitylicious.domain.usecase.admin.ManageIngredientUseCase
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

data class ManageIngredientsUiState(
    val ingredients: List<IngredientEntity> = emptyList(),
    val isAdmin: Boolean = false,
    val userBranchId: String = "B1",
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ManageIngredientsViewModel @Inject constructor(
    private val manageIngredientUseCase: ManageIngredientUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ManageIngredientsUiState(
            isAdmin = sessionManager.getRole()?.equals("admin", ignoreCase = true) == true,
            userBranchId = "B${sessionManager.getBranchId()}"
        )
    )
    val uiState: StateFlow<ManageIngredientsUiState> = _uiState.asStateFlow()

    init {
        observeIngredients()
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            manageIngredientUseCase.observeIngredients().collectLatest { ingredients ->
                _uiState.update {
                    it.copy(
                        ingredients = ingredients,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun saveIngredient(
        existingIngredientId: Int?,
        name: String,
        unitType: String,
        estimatedWeightPerUnit: Double,
        lowStockThreshold: Double,
        isPackaging: Boolean,
        image: String?
    ) {
        val cleanName = name.trim()
        val cleanUnit = unitType.trim()

        if (cleanName.isBlank()) {
            setError("Ingredient name is required.")
            return
        }

        if (cleanUnit.isBlank()) {
            setError("Unit type is required.")
            return
        }

        if (estimatedWeightPerUnit < 0.0) {
            setError("Estimated weight cannot be negative.")
            return
        }

        if (lowStockThreshold < 0.0) {
            setError("Low stock threshold cannot be negative.")
            return
        }

        viewModelScope.launch {
            val result = manageIngredientUseCase.saveIngredient(
                ingredientId = existingIngredientId ?: generateId(),
                image = image?.takeIf { it.isNotBlank() },
                ingredientName = cleanName,
                unitType = cleanUnit,
                estimatedWeightPerUnit = estimatedWeightPerUnit,
                isPackaging = isPackaging,
                lowStockThreshold = lowStockThreshold
            )

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        successMessage = "Ingredient saved.",
                        error = null
                    )
                } else {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to save ingredient.",
                        successMessage = null
                    )
                }
            }
        }
    }

    fun deleteIngredient(ingredientId: Int) {
        viewModelScope.launch {
            val result = manageIngredientUseCase.deleteIngredient(ingredientId)

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        successMessage = "Ingredient deleted.",
                        error = null
                    )
                } else {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to delete ingredient.",
                        successMessage = null
                    )
                }
            }
        }
    }

    fun updateIngredientImage(ingredientId: Int, imagePath: String?) {
        viewModelScope.launch {
            manageIngredientUseCase.updateIngredientImage(ingredientId, imagePath)
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                error = null,
                successMessage = null
            )
        }
    }

    private fun setError(message: String) {
        _uiState.update {
            it.copy(
                error = message,
                successMessage = null
            )
        }
    }

    private fun generateId(): Int {
        return System.currentTimeMillis().hashCode().absoluteValue
    }
}