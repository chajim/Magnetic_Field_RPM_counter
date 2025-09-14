package com.example.magneticfieldrpmcounter

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class MainViewModel : ViewModel() {

    // Stavy, které UI sleduje
    private val _magneticFieldStrength = MutableStateFlow(0.0f)
    val magneticFieldStrength = _magneticFieldStrength.asStateFlow()

    private val _rotationCount = MutableStateFlow(0)
    val rotationCount = _rotationCount.asStateFlow()

    private val _isCounting = MutableStateFlow(false)
    val isCounting = _isCounting.asStateFlow()

    private val _threshold = MutableStateFlow<Float?>(null)
    val threshold = _threshold.asStateFlow()

    // Pomocná proměnná pro detekci jednoho průchodu
    private var isPeakDetected = false

    fun processSensorData(values: FloatArray) {
        if (!_isCounting.value || _threshold.value == null) return

        val x = values[0]
        val y = values[1]
        val z = values[2]

        // Vypočítá celkovou sílu magnetického pole
        val magnitude = sqrt(x * x + y * y + z * z)
        _magneticFieldStrength.value = magnitude

        // Logika pro detekci a počítání otáček
        val currentThreshold = _threshold.value!!

        if (magnitude > currentThreshold && !isPeakDetected) {
            isPeakDetected = true
        }

        if (magnitude < currentThreshold && isPeakDetected) {
            _rotationCount.value++
            isPeakDetected = false
        }
    }

    fun calibrate(recentReadings: List<Float>) {
        if (recentReadings.isEmpty()) return
        val average = recentReadings.average()
        // Nastaví práh o 10 (30) µT výše, než je průměrný šum
        // Tuto hodnotu můžete upravit pro lepší citlivost
        _threshold.value = (average + 10.0f).toFloat()
    }

    fun toggleCounting() {
        _isCounting.value = !_isCounting.value
        // Resetujeme detektor špičky při každém startu/stopu
        if (!_isCounting.value) {
            isPeakDetected = false
        }
    }

    fun resetCount() {
        _rotationCount.value = 0
        isPeakDetected = false
    }
}