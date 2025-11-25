package com.example.gyroscope

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.min
import com.example.gyroscope.ui.theme.GyroscopeTheme

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null

    // Velocity of the ball in pixels/second, driven by gyroscope tilt
    private val velXState = mutableStateOf(0f)
    private val velYState = mutableStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            GyroscopeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GyroBallGameScreen(
                        velX = velXState.value,
                        velY = velYState.value,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register gyroscope sensor with "game" delay for a responsive feeling
        gyroscope?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop receiving sensor events when activity is not visible
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            // event.values[0]: rotation around X axis (front/back tilt)
            // event.values[1]: rotation around Y axis (left/right tilt)
            val tiltX = event.values[1]  // 左右
            val tiltY = event.values[0]  // 前后

            val speedFactor = 800f // Adjust movement speed


            velXState.value = -tiltX * speedFactor
            velYState.value = tiltY * speedFactor
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 不需要处理
    }
}

data class Wall(
    val leftRatio: Float,
    val topRatio: Float,
    val rightRatio: Float,
    val bottomRatio: Float
)

@Composable
fun GyroBallGameScreen(
    velX: Float,
    velY: Float,
    modifier: Modifier = Modifier
) {
    // Ball position in pixels
    var ballX by remember { mutableStateOf(0f) }
    var ballY by remember { mutableStateOf(0f) }
    val ballRadius = 40f

    // Canvas size in pixels
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    var hasWon by remember { mutableStateOf(false) }
    // Simple maze
    val walls = remember {
        listOf(
            // Vertical side walls to keep the ball inside the main corridor
            Wall(0.0f, 0.20f, 0.05f, 0.95f),   // left side wall
            Wall(0.95f, 0.20f, 1.0f, 0.95f),   // right side wall

            // Row 1: horizontal wall, gap on the right (first bend)
            Wall(0.05f, 0.25f, 0.75f, 0.30f),

            // Row 2: horizontal wall, gap on the left (turn back left)
            Wall(0.25f, 0.40f, 0.95f, 0.45f),

            // Row 3: horizontal wall, gap on the right (turn right again)
            Wall(0.05f, 0.55f, 0.75f, 0.60f),

            // Row 4: horizontal wall, gap on the left (final bend near goal)
            Wall(0.25f, 0.70f, 0.95f, 0.75f)
        )
    }

    // Simple "game loop" using a coroutine
    LaunchedEffect(velX, velY, hasWon) {
        val frameMs = 16L
        val dt = frameMs / 1000f
        while (true) {
            if (canvasWidth > 0f && canvasHeight > 0f && !hasWon) {
                // Initialize ball position on first frame
                if (ballX == 0f && ballY == 0f) {
                    ballX = canvasWidth / 2f
                    ballY = canvasHeight * 0.15f
                }

                var newX = ballX + velX * dt
                var newY = ballY + velY * dt

                newX = newX.coerceIn(ballRadius, canvasWidth - ballRadius)
                newY = newY.coerceIn(ballRadius, canvasHeight - ballRadius)

                // Collision detection with walls
                walls.forEach { wall ->
                    val left = wall.leftRatio * canvasWidth
                    val right = wall.rightRatio * canvasWidth
                    val top = wall.topRatio * canvasHeight
                    val bottom = wall.bottomRatio * canvasHeight

                    if (newX in (left - ballRadius)..(right + ballRadius) &&
                        newY in (top - ballRadius)..(bottom + ballRadius)
                    ) {
                        newX = ballX
                        newY = ballY
                    }
                }

                // Goal area
                val goalLeft = canvasWidth * 0.425f
                val goalRight = canvasWidth * 0.575f        // 0.425 + 0.15
                val goalTop = canvasHeight * 0.86f
                val goalBottom = canvasHeight * 0.94f       // 0.86 + 0.08

                if (newX in goalLeft..goalRight && newY in goalTop..goalBottom) {
                    hasWon = true           // 设置获胜
                }

                ballX = newX
                ballY = newY
            }

            delay(frameMs)
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Gyroscope Ball Maze",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Tilt your phone to move the ball.",
                style = MaterialTheme.typography.bodyMedium
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        canvasWidth = size.width
                        canvasHeight = size.height

                        // 背景
                        drawRect(
                            color = Color(0xFF102027),
                            topLeft = Offset.Zero,
                            size = size
                        )

                        // Goal area
                        val goalSize = Size(width = size.width * 0.15f, height = size.height * 0.08f)
                        val goalTopLeft = Offset(
                            x = size.width * 0.425f,
                            y = size.height * 0.86f
                        )
                        drawRect(
                            color = Color(0xFF2E7D32),
                            topLeft = goalTopLeft,
                            size = goalSize
                        )

                        //  Walls
                        walls.forEach { wall ->
                            val left = wall.leftRatio * size.width
                            val right = wall.rightRatio * size.width
                            val top = wall.topRatio * size.height
                            val bottom = wall.bottomRatio * size.height

                            drawRect(
                                color = Color(0xFF455A64),
                                topLeft = Offset(left, top),
                                size = Size(right - left, bottom - top)
                            )
                        }

                        // Ball
                        if (ballX != 0f && ballY != 0f) {
                            drawCircle(
                                color = Color(0xFF29B6F6),
                                radius = ballRadius,
                                center = Offset(ballX, ballY)
                            )
                        }
                    }
                }
            }
            if (hasWon) {
                Text(
                    text = "You Win!",
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GyroBallPreview() {
    GyroscopeTheme {
        GyroBallGameScreen(
            velX = 0f,
            velY = 0f
        )
    }
}