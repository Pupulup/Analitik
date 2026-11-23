package com.dota.analyzer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dota.analyzer.data.Match
import com.dota.analyzer.data.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MatchState {
    data object Idle : MatchState
    data object Loading : MatchState
    data class Success(val match: Match) : MatchState
    data class Error(val message: String) : MatchState
}

class MatchViewModel : ViewModel() {
    private val repository = MatchRepository()
    private val _state = MutableStateFlow<MatchState>(MatchState.Idle)
    val state: StateFlow<MatchState> = _state.asStateFlow()

    fun loadMatch(rawId: String) {
        val id = rawId.trim().toLongOrNull()
        if (id == null || id <= 0) {
            _state.value = MatchState.Error("ID матча должен быть положительным числом.")
            return
        }
        viewModelScope.launch {
            _state.value = MatchState.Loading
            _state.value = try {
                MatchState.Success(repository.getMatch(id))
            } catch (e: Exception) {
                MatchState.Error(
                    when {
                        e.message?.contains("404") == true -> "Матч не найден или ещё не обработан OpenDota."
                        e.message?.contains("Unable to resolve host", true) == true -> "Нет подключения к интернету."
                        else -> "Не удалось загрузить матч. Проверьте ID и повторите попытку."
                    }
                )
            }
        }
    }
}
