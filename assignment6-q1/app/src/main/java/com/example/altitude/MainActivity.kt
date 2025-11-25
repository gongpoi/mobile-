package com.example.altitude

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import com.example.altitude.ui.theme.AltitudeTheme

private const val P0 = 1013.25f   // Sea level standard pressure in hPa

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null

    private val pressureState = mutableStateOf(P0)
    private val simulateModeState = mutableStateOf(false)
    private val simulatedPressureState = mutableStateOf(P0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        setContent {
            AltitudeTheme {
                val pressure by pressureState
                val simulateMode by simulateModeState
                val simulatedPressure by simulatedPressureState

                AltitudeScreen(
                    pressure = pressure,
                    simulateMode = simulateMode,
                    simulatedPressure = simulatedPressure,
                    onToggleSimulate = { simulateModeState.value = !simulateModeState.value },
                    onChangeSimPressure = { simulatedPressureState.value = it }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register pressure sensor listener when activity is in foreground
        pressureSensor?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop receiving sensor events to save battery
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!simulateModeState.value && event.sensor.type == Sensor.TYPE_PRESSURE) {
            pressureState.value = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }
}

fun pressureToAltitude(pressureHpa: Float): Float {
    val ratio = pressureHpa / P0
    val exponent = 1.0 / 5.255
    val power = ratio.toDouble().pow(exponent).toFloat()
    return 44330f * (1f - power)
}

fun backgroundForAltitude(altitude: Float): Color {
    val maxAlt = 4000f
    val clamped = altitude.coerceIn(0f, maxAlt)
    val t = clamped / maxAlt
    val level = (255 * (1 - t)).toInt()
    return Color(level, level, level)
}

@Composable
fun AltitudeScreen(
    pressure: Float,
    simulateMode: Boolean,
    simulatedPressure: Float,
    onToggleSimulate: () -> Unit,
    onChangeSimPressure: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val usedPressure = if (simulateMode) simulatedPressure else pressure
    val altitude = pressureToAltitude(usedPressure)

    // Use the first altitude we see as a baseline to compute Δh.
    var baseAltitude by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(altitude) {
        if (baseAltitude == null) {
            baseAltitude = altitude
        }
    }
    val delta = baseAltitude?.let { altitude - it } ?: 0f

    val bgColor = backgroundForAltitude(altitude)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Altitude: %.1f m".format(altitude),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(text = "Δ Altitude: %.1f m".format(delta))
            Text(text = "Pressure: %.1f hPa".format(usedPressure))

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Simulate")
                Switch(
                    checked = simulateMode,
                    onCheckedChange = { onToggleSimulate() }
                )
            }

            if (simulateMode) {
                Text("Simulated pressure (hPa)")
                Slider(
                    value = simulatedPressure,
                    onValueChange = onChangeSimPressure,
                    valueRange = 800f..1050f
                )
                Text("Sim pressure: %.1f hPa".format(simulatedPressure))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AltitudePreview() {
    AltitudeTheme {
        AltitudeScreen(
            pressure = 1013.25f,
            simulateMode = true,
            simulatedPressure = 900f,
            onToggleSimulate = {},
            onChangeSimPressure = {}
        )
    }
}