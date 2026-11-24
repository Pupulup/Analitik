package com.dota.analyzer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dota.analyzer.data.Match
import com.dota.analyzer.data.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import com.google.gson.JsonParseException

sealed interface MatchState {
    data object Idle : MatchState
    data object Loading : MatchState
    data class Success(
        val match: Match,
        val heroNames: Map<Int, String>,
        val heroImageNames: Map<Int, String>
    ) : MatchState
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
                val match = repository.getMatch(id)
                if (match.players.isEmpty()) {
                    MatchState.Error(
                        "OpenDota вернул матч $id, но не вернул статистику игроков. " +
                            "Возможно, реплей ещё не обработан или подробные данные недоступны."
                    )
                } else {
                    val heroes = runCatching { repository.getHeroNames() }.getOrDefault(emptyMap())
                    val heroImages = runCatching { repository.getHeroImageNames() }.getOrDefault(emptyMap())
                    MatchState.Success(match, heroes, heroImages)
                }
            } catch (e: Exception) {
                MatchState.Error(describeError(e, id))
            }
        }
    }

    private fun describeError(error: Exception, matchId: Long): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                404 -> "OpenDota вернул HTTP 404 для матча $matchId. " +
                    "Это не доказывает, что матча не существует: сервис может не иметь записи о нём."
                403 -> "OpenDota отклонил запрос (HTTP 403). Возможна блокировка доступа со стороны сервиса или сети."
                429 -> "OpenDota ограничил частоту запросов (HTTP 429). Подождите немного и попробуйте снова."
                in 500..599 -> "Сервер OpenDota временно недоступен (HTTP ${error.code()}). Попробуйте позже."
                else -> "OpenDota вернул HTTP ${error.code()}: ${error.message()}"
            }
            is SocketTimeoutException, is InterruptedIOException -> {
                val phase = repository.lastFailureDiagnostic ?: "этап запроса определить не удалось"
                "Таймаут при обращении к OpenDota (матч $matchId). Этап: $phase. " +
                    "Проверьте API в браузере телефона: https://api.opendota.com/api/matches/$matchId"
            }
            is IOException -> "Сетевая ошибка при обращении к OpenDota: ${error.localizedMessage ?: "соединение прервано"}"
            is JsonParseException -> "OpenDota вернул ответ, который приложение не смогло разобрать: ${error.localizedMessage ?: "ошибка JSON"}"
            else -> "Ошибка ${error.javaClass.simpleName}: ${error.localizedMessage ?: "подробности отсутствуют"}"
        }
    }
}
