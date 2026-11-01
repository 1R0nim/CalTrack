package com.example.caltrack.ui.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.caltrack.CalTrackApp
import com.example.caltrack.data.FoodRepository
import com.example.caltrack.data.LoggedFood
import com.example.caltrack.data.local.entity.SavedFood
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MealsUiState(
    /** Saved foods matching the search, most used first. */
    val foods: List<SavedFood> = emptyList(),
    /** Every saved food, regardless of the search. */
    val allFoods: List<SavedFood> = emptyList(),
    val isLoading: Boolean = true,
)

class MealsViewModel(private val repository: FoodRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    private val searchableFoods = repository.observeSavedFoods()
        .map { foods -> foods.map { it to SavedFood.searchKeyOf(it.name) } }

    val uiState: StateFlow<MealsUiState> =
        combine(searchableFoods, query) { foods, query ->
            val key = SavedFood.searchKeyOf(query)
            MealsUiState(
                foods = foods.filter { (_, name) -> name.contains(key) }.map { it.first },
                allFoods = foods.map { it.first },
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MealsUiState())

    /** The search field's text lives in the UI; this only filters. */
    fun setQuery(text: String) {
        query.value = text
    }

    /** Logs [amount] of [food] in its saved unit on [date]; the result is what [undoAdd] needs. */
    fun add(food: SavedFood, amount: Double, date: LocalDate): Deferred<LoggedFood> =
        viewModelScope.async { repository.logSavedFood(food, amount, date) }

    fun undoAdd(added: Deferred<LoggedFood>) {
        viewModelScope.launch { repository.undoLog(added.await()) }
    }

    fun delete(food: SavedFood) {
        viewModelScope.launch { repository.deleteSavedFood(food) }
    }

    fun restore(food: SavedFood) {
        viewModelScope.launch { repository.restoreSavedFood(food) }
    }

    /** Completes with false when the new name is already taken by another saved food. */
    fun update(food: SavedFood): Deferred<Boolean> =
        viewModelScope.async { repository.updateSavedFood(food) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalTrackApp
                MealsViewModel(app.repository)
            }
        }
    }
}
