package com.example.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.MealLogEntity
import com.example.data.local.db.MealTemplateWithItems
import com.example.data.repository.MealTemplateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MealTemplateViewModel(
    private val templateRepo: MealTemplateRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val templates: StateFlow<List<MealTemplateWithItems>> =
        _searchQuery
            .debounce(200)
            .flatMapLatest { query ->
                if (query.isBlank()) templateRepo.allTemplates
                else templateRepo.search(query)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favourites = templateRepo.favourites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = _saveState.asStateFlow()

    private val _logState = MutableStateFlow<LogState>(LogState.Idle)
    val logState = _logState.asStateFlow()

    fun onSearchChanged(q: String) { _searchQuery.value = q }

    fun saveCurrentMealAsTemplate(
        name: String,
        mealType: String,
        items: List<MealLogEntity>
    ) = viewModelScope.launch {
        if (name.isBlank()) {
            _saveState.value = SaveState.Error("Please give your meal a name")
            return@launch
        }
        if (items.isEmpty()) {
            _saveState.value = SaveState.Error("Add some food items first")
            return@launch
        }
        _saveState.value = SaveState.Saving
        try {
            templateRepo.saveAsTemplate(name, mealType, items)
            _saveState.value = SaveState.Success
        } catch (e: Exception) {
            _saveState.value = SaveState.Error("Could not save template")
        }
    }

    fun logTemplate(templateId: Long, targetMeal: String, selectedDate: String) = viewModelScope.launch {
        _logState.value = LogState.Logging
        try {
            templateRepo.logTemplate(templateId, targetMeal, selectedDate)
            _logState.value = LogState.Success
        } catch (e: Exception) {
            _logState.value = LogState.Error("Could not log meal")
        }
    }

    fun toggleFavourite(id: Long, current: Boolean) = viewModelScope.launch {
        templateRepo.toggleFavourite(id, current)
    }

    fun deleteTemplate(id: Long) = viewModelScope.launch {
        templateRepo.deleteTemplate(id)
    }

    fun resetSaveState() { _saveState.value = SaveState.Idle }
    fun resetLogState()  { _logState.value  = LogState.Idle  }
}

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}

sealed class LogState {
    object Idle : LogState()
    object Logging : LogState()
    object Success : LogState()
    data class Error(val message: String) : LogState()
}
