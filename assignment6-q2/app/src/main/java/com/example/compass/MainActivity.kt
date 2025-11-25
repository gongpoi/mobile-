package com.example.compass

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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.pow
import com.example.compass.ui.theme.CompassTheme

class MainActivity : ComponentActivity(), SensorEventListener {

    // Android sensor manager and the three sensors we use
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var gyroscope: Sensor? = null

    // Raw sensor data buffers
    private val accelValues = FloatArray(3)
    private val magnetValues = FloatArray(3)
    private var hasAccel = false
    private var hasMagnet = false

    // Compose state for heading (compass) and roll / pitch (digital level)
    // heading:   0–360 degrees
    // roll/pitch: -90–90 degrees (clamped)
    private val headingState = mutableStateOf(0f)
    private val rollState = mutableStateOf(0f)
    private val pitchState = mutableStateOf(0f)

    // To integrate gyroscope readings over time we track the last timestamp
    private var lastGyroTimestamp: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            CompassTheme {
                CompassAndLevelScreen(
                    heading = headingState.value,
                    roll = rollState.value,
                    pitch = pitchState.value
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register all required sensors with "game" delay (fast, real-time feeling)
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        // Always unregister when the activity is not visible to save battery
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            // Accelerometer: used together with magnetometer for orientation matrix
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelValues, 0, 3)
                hasAccel = true
            }
            // Magnetometer: used with accelerometer to compute heading (azimuth)
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetValues, 0, 3)
                hasMagnet = true
            }
            // Gyroscope: used for roll / pitch integration
            Sensor.TYPE_GYROSCOPE -> {
                handleGyro(event)
            }
        }

        // When we have both accelerometer and magnetometer, we can compute heading
        if (hasAccel && hasMagnet) {
            val R = FloatArray(9)
            val I = FloatArray(9)
            if (SensorManager.getRotationMatrix(R, I, accelValues, magnetValues)) {
                val orientations = FloatArray(3)
                SensorManager.getOrientation(R, orientations)

                val azimuthRad = orientations[0]                  // heading in radians
                val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                val heading = (azimuthDeg + 360f) % 360f          // normalize to 0–360°
                headingState.value = heading
            }
        }
    }


    private fun handleGyro(event: SensorEvent) {
        val now = event.timestamp // nanoseconds
        val last = lastGyroTimestamp
        if (last != null) {
            val dt = (now - last) / 1_000_000_000f // convert to seconds

            // X-axis rotation -> pitch (front/back tilt)
            val wx = event.values[0]
            // Y-axis rotation -> roll  (left/right tilt)
            val wy = event.values[1]

            val radToDeg = 180f / Math.PI.toFloat()

            val newPitch = pitchState.value + wx * dt * radToDeg
            val newRoll  = rollState.value  + wy * dt * radToDeg

            // Clamp angles into a reasonable range so they don't drift forever
            rollState.value = newRoll.coerceIn(-90f, 90f)
            pitchState.value = newPitch.coerceIn(-90f, 90f)
        }
        lastGyroTimestamp = now
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used in this demo
    }
}

@Composable
fun CompassAndLevelScreen(
    heading: Float,
    roll: Float,
    pitch: Float,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Compass card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Compass",
                        style = MaterialTheme.typography.titleLarge
                    )
                    CompassView(heading = heading)
                    Text(
                        text = "Heading: %.1f°".format(heading),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Digital level card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Digital Level",
                        style = MaterialTheme.typography.titleLarge
                    )
                    LevelView(roll = roll, pitch = pitch)
                    Text("Roll: %.1f°".format(roll))
                    Text("Pitch: %.1f°".format(pitch))
                }
            }
        }
    }
}

@Composable
fun CompassView(heading: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = min(size.width, size.height) / 2f * 0.85f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer ring
            drawCircle(
                color = Color(0xFF263238),
                radius = radius,
                center = center
            )
            // Inner ring
            drawCircle(
                color = Color(0xFF37474F),
                radius = radius * 0.9f,
                center = center
            )

            // Crosshair (N–S and E–W lines)
            drawLine(
                color = Color.LightGray,
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.LightGray,
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 3f
            )

            // Needles: rotate around center by -heading so red points to north
            rotate(degrees = -heading, pivot = center) {
                // North needle (red)
                drawLine(
                    color = Color.Red,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.75f),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
                // South needle (white)
                drawLine(
                    color = Color.White,
                    start = center,
                    end = Offset(center.x, center.y + radius * 0.5f),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
            }

            // Center cap
            drawCircle(
                color = Color.Black,
                radius = radius * 0.08f,
                center = center
            )
        }
    }
}

@Composable
fun LevelView(roll: Float, pitch: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)
            val radius = min(width, height) / 3f

            // Outer circle
            drawCircle(
                color = Color(0xFF263238),
                radius = radius,
                center = center
            )
            // Inner circle
            drawCircle(
                color = Color(0xFF37474F),
                radius = radius * 0.9f,
                center = center
            )

            // Crosshair
            drawLine(
                color = Color.LightGray,
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.LightGray,
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 3f
            )

            // Normalize roll / pitch into [-1, 1] based on a max angle
            val maxAngle = 45f
            val normX = (roll.coerceIn(-maxAngle, maxAngle)) / maxAngle   // left/right
            val normY = (pitch.coerceIn(-maxAngle, maxAngle)) / maxAngle  // up/down

            // Bubble position inside the circle (0.7 * radius to keep inside)
            val bubbleX = center.x + normX * radius * 0.7f
            val bubbleY = center.y + normY * radius * 0.7f

            // Bubble body
            drawCircle(
                color = Color(0xFF80DEEA),
                radius = radius * 0.18f,
                center = Offset(bubbleX, bubbleY)
            )
            // Small highlight on the bubble for a nicer look
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = radius * 0.08f,
                center = Offset(bubbleX - radius * 0.05f, bubbleY - radius * 0.05f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompassPreview() {
    CompassTheme {
        CompassAndLevelScreen(
            heading = 45f,
            roll = 10f,
            pitch = -5f
        )
    }
}