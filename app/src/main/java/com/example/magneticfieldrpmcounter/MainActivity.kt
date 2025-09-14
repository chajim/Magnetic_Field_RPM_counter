package com.example.magneticfieldrpmcounter


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.magneticfieldrpmcounter.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {


    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var sensorManager: SensorManager
    private var magneticFieldSensor: Sensor? = null

    // Ukládá posledních 50 hodnot pro kalibraci
    private val calibrationReadings = mutableListOf<Float>()
    private val maxCalibrationReadings = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializace senzorů
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (magneticFieldSensor == null) {
            binding.textFieldStrength.text = "Senzor není dostupný"
            binding.buttonStartStop.isEnabled = false
            binding.buttonCalibrate.isEnabled = false
        }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.buttonStartStop.setOnClickListener {
            viewModel.toggleCounting()
        }
        binding.buttonReset.setOnClickListener {
            viewModel.resetCount()
        }
        binding.buttonCalibrate.setOnClickListener {
            viewModel.calibrate(calibrationReadings)
            Toast.makeText(this, "Kalibrace dokončena", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.magneticFieldStrength.onEach { strength ->
            binding.textFieldStrength.text = String.format(Locale.US, "%.2f µT", strength)
        }.launchIn(lifecycleScope)

        viewModel.rotationCount.onEach { count ->
            binding.textRotationCount.text = count.toString()
        }.launchIn(lifecycleScope)

        viewModel.isCounting.onEach { isCounting ->
            binding.buttonStartStop.text = if (isCounting) "Stop" else "Start"
        }.launchIn(lifecycleScope)

        viewModel.threshold.onEach { threshold ->
            binding.textThreshold.text = if (threshold != null) {
                String.format(Locale.US, "%.2f µT", threshold)
            } else {
                "Nekalibrováno"
            }
        }.launchIn(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        magneticFieldSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            // Ukládáme hodnoty pro kalibraci
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt(x * x + y * y + z * z)

            if (calibrationReadings.size >= maxCalibrationReadings) {
                calibrationReadings.removeAt(0)
            }
            calibrationReadings.add(magnitude)

            // Posíláme data do ViewModelu, pokud je počítání aktivní
            viewModel.processSensorData(event.values.clone())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Není potřeba implementovat
    }
}