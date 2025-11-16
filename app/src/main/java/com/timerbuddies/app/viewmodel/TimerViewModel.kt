package com.timerbuddies.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class TimerState(
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
    val revealProgress: Float = 0f,
    val customImageUri: Uri? = null
)

data class TimerPreset(
    val id: String,
    val name: String,
    val seconds: Int,
    val imageUrl: String?
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val prefs = application.getSharedPreferences("timer_buddies_prefs", Context.MODE_PRIVATE)
    
    private val _savedImages = MutableStateFlow<List<String>>(emptyList())
    val savedImages: StateFlow<List<String>> = _savedImages.asStateFlow()

    private val _timerPresets = MutableStateFlow<List<TimerPreset>>(emptyList())
    val timerPresets: StateFlow<List<TimerPreset>> = _timerPresets.asStateFlow()

    private var timerJob: Job? = null

    init {
        // Load saved images from SharedPreferences
        loadSavedImages()
        loadTimerPresets()
    }

    private fun loadSavedImages() {
        val savedImagesJson = prefs.getString("saved_images", null)
        if (savedImagesJson != null) {
            val images = savedImagesJson.split("|||").filter { it.isNotEmpty() }
            _savedImages.value = images
        }
    }

    private fun loadTimerPresets() {
        val presetsJson = prefs.getString("timer_presets", null)
        if (presetsJson != null) {
            try {
                val jsonArray = JSONArray(presetsJson)
                val presets = mutableListOf<TimerPreset>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    presets.add(
                        TimerPreset(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            seconds = obj.getInt("seconds"),
                            imageUrl = if (obj.has("imageUrl") && !obj.isNull("imageUrl")) 
                                obj.getString("imageUrl") else null
                        )
                    )
                }
                _timerPresets.value = presets
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun savePresetsToPersistentStorage() {
        try {
            val jsonArray = JSONArray()
            _timerPresets.value.forEach { preset ->
                val obj = JSONObject()
                obj.put("id", preset.id)
                obj.put("name", preset.name)
                obj.put("seconds", preset.seconds)
                obj.put("imageUrl", preset.imageUrl ?: JSONObject.NULL)
                jsonArray.put(obj)
            }
            prefs.edit().putString("timer_presets", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveTimerPreset(name: String, seconds: Int, imageUrl: String?) {
        val preset = TimerPreset(
            id = System.currentTimeMillis().toString(),
            name = name,
            seconds = seconds,
            imageUrl = imageUrl
        )
        val currentPresets = _timerPresets.value.toMutableList()
        currentPresets.add(0, preset)
        _timerPresets.value = currentPresets
        savePresetsToPersistentStorage()
    }

    fun deleteTimerPreset(presetId: String) {
        _timerPresets.value = _timerPresets.value.filter { it.id != presetId }
        savePresetsToPersistentStorage()
    }

    private fun saveToPersistentStorage() {
        val imagesString = _savedImages.value.joinToString("|||")
        prefs.edit().putString("saved_images", imagesString).apply()
    }

    fun saveGeneratedImage(imageUrl: String) {
        val currentImages = _savedImages.value.toMutableList()
        if (!currentImages.contains(imageUrl)) {
            currentImages.add(0, imageUrl) // Add to beginning
            if (currentImages.size > 20) { // Keep only last 20 images
                currentImages.removeAt(currentImages.size - 1)
            }
            _savedImages.value = currentImages
            saveToPersistentStorage()
        }
    }

    fun removeSavedImage(imageUrl: String) {
        _savedImages.value = _savedImages.value.filter { it != imageUrl }
        saveToPersistentStorage()
    }

    fun setTime(minutes: Int, imageUri: Uri? = null) {
        val seconds = minutes * 60
        _timerState.value = TimerState(
            totalSeconds = seconds,
            remainingSeconds = seconds,
            isRunning = false,
            isComplete = false,
            revealProgress = 0f,
            customImageUri = imageUri
        )
    }

    fun setTimeInSeconds(seconds: Int, imageUri: Uri? = null) {
        _timerState.value = TimerState(
            totalSeconds = seconds,
            remainingSeconds = seconds,
            isRunning = false,
            isComplete = false,
            revealProgress = 0f,
            customImageUri = imageUri
        )
    }

    fun startTimer() {
        if (_timerState.value.remainingSeconds <= 0) return

        _timerState.value = _timerState.value.copy(isRunning = true)

        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning) {
                delay(1000)
                val newRemaining = _timerState.value.remainingSeconds - 1
                val progress = 1f - (newRemaining.toFloat() / _timerState.value.totalSeconds.toFloat())

                _timerState.value = _timerState.value.copy(
                    remainingSeconds = newRemaining,
                    revealProgress = progress,
                    isComplete = newRemaining == 0
                )

                if (newRemaining == 0) {
                    _timerState.value = _timerState.value.copy(isRunning = false)
                }
            }
        }
    }

    fun pauseTimer() {
        _timerState.value = _timerState.value.copy(isRunning = false)
        timerJob?.cancel()
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState(
            totalSeconds = _timerState.value.totalSeconds,
            remainingSeconds = _timerState.value.totalSeconds,
            isRunning = false,
            isComplete = false,
            revealProgress = 0f,
            customImageUri = _timerState.value.customImageUri
        )
    }

    fun backToSelection() {
        timerJob?.cancel()
        _timerState.value = TimerState()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
