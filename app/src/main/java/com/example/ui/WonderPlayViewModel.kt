package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundSynthesizer
import com.example.data.AppSettings
import com.example.data.DifficultyLevel
import com.example.data.GameSession
import com.example.data.GameType
import com.example.data.PlayerProfile
import com.example.data.SettingsRepository
import com.example.data.StickerItem
import com.example.data.UiState
import com.example.data.WonderPlayRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WonderPlayViewModel(
    private val repository: WonderPlayRepository,
    private val settingsRepository: SettingsRepository,
    val sound: SoundSynthesizer,
    private val currentUserId: String,
    private val initialDisplayName: String?
) : ViewModel() {

    val profileState: StateFlow<UiState<PlayerProfile>> = repository.observeProfile(currentUserId)
        .map<PlayerProfile?, UiState<PlayerProfile>> { profile ->
            UiState.Success(
                profile ?: PlayerProfile(
                    userId = currentUserId,
                    displayName = initialDisplayName?.takeIf { it.isNotBlank() } ?: "Little Explorer"
                )
            )
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing player profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load player profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val recentSessionsState: StateFlow<UiState<List<GameSession>>> =
        repository.observeRecentSessions(currentUserId)
            .map<List<GameSession>, UiState<List<GameSession>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing recent sessions", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load game history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .onEach { appSettings ->
            sound.enabled = appSettings.soundEnabled
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppSettings()
        )

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureProfileExists(initialDisplayName).onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not sync cloud profile"
            }
        }
    }

    fun recordGameResult(
        gameType: GameType,
        score: Int,
        starsEarned: Int
    ) {
        val diff = settings.value.difficulty
        viewModelScope.launch {
            repository.recordGameResult(
                gameType = gameType,
                score = score,
                starsEarned = starsEarned,
                difficulty = diff
            ).onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to save score"
            }
        }
    }

    fun unlockSticker(sticker: StickerItem) {
        viewModelScope.launch {
            repository.unlockSticker(sticker)
                .onSuccess {
                    sound.playFanfare()
                    _actionMessage.value = "Unlocked ${sticker.name}!"
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Could not unlock sticker"
                }
        }
    }

    fun updatePlayerName(name: String) {
        viewModelScope.launch {
            repository.updateDisplayName(name)
                .onSuccess {
                    _actionMessage.value = "Player name updated!"
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Could not update name"
                }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        sound.enabled = enabled
        viewModelScope.launch {
            settingsRepository.setSoundEnabled(enabled)
        }
    }

    fun setDifficulty(difficulty: DifficultyLevel) {
        viewModelScope.launch {
            settingsRepository.setDifficulty(difficulty)
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    private companion object {
        const val TAG = "WonderPlayVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
