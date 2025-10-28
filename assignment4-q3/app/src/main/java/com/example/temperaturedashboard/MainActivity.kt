package com.example.temperaturedashboard

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
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.random.Random
import com.example.temperaturedashboard.ui.theme.TemperatureDashboardTheme

// ------------------ Data models ------------------
data class TempReading(
    val time: LocalDateTime,
    val valueF: Float
)
// UI state for the dashboard.
data class DashboardState(
    val readings: List<TempReading> = emptyList(),
    val paused: Boolean = false
) {
    val current: Float? get() = readings.lastOrNull()?.valueF
    val min: Float? get() = readings.minOfOrNull { it.valueF }
    val max: Float? get() = readings.maxOfOrNull { it.valueF }
    val avg: Float? get() = if (readings.isNotEmpty())
        readings.map { it.valueF }.average().toFloat() else null
}

//ViewModel
class TempViewModel : ViewModel() {
    private val _ui = MutableStateFlow(DashboardState())
    val ui: StateFlow<DashboardState> = _ui.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var genJob: Job? = null

    init {
        startGenerating()
    }
    // Pause/resume data generation
    fun togglePause(paused: Boolean) {
        _ui.value = _ui.value.copy(paused = paused)
        if (paused) stopGenerating() else startGenerating()
    }
    // Clear the readings list
    fun clear() {
        _ui.value = DashboardState(paused = _ui.value.paused)
    }
    // Launch a single generator job
    private fun startGenerating() {
        if (genJob?.isActive == true) return
        genJob = scope.launch {
            while (isActive && !_ui.value.paused) {
                delay(2000L) // 每 2 秒生成一次
                if (_ui.value.paused) break
                pushRandom()
            }
        }
    }
    // Cancel the generator job if running
    private fun stopGenerating() {
        genJob?.cancel()
        genJob = null
    }
    // Append one random reading
    private fun pushRandom() {
        val value = Random.nextDouble(65.0, 85.0).toFloat()
        val now = LocalDateTime.now()
        val updated = (_ui.value.readings + TempReading(now, value)).takeLast(20)
        _ui.value = _ui.value.copy(readings = updated)
    }

    override fun onCleared() {
        stopGenerating()
        scope.cancel()
        super.onCleared()
    }
}

class MainActivity : ComponentActivity() {
    private val vm: TempViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemperatureDashboardTheme {
                DashboardApp(vm)
            }
        }
    }
}
//a scaffold with a top bar and the dashboard content
@Composable
fun DashboardApp(vm: TempViewModel) {
    val state by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            SimpleAppBar(
                title = "Temperature Dashboard",
                actions = {
                    TextButton(onClick = { vm.clear() }) { Text("Clear") }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary
            StatsRow(state)

            // Simple line chart of the recent readings
            TemperatureChart(
                readings = state.readings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            // Pause / resume switch for the generator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Generate:", modifier = Modifier.padding(end = 8.dp))
                Switch(
                    checked = !state.paused,
                    onCheckedChange = { vm.togglePause(!state.paused) }
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (!state.paused) "Streaming..." else "Paused",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // List of readings
            ReadingList(
                readings = state.readings,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

//built from Surface + Row.
@Composable
fun SimpleAppBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            Row(content = actions)
        }
    }
}

// Summary statistics row
@Composable
fun StatsRow(state: DashboardState) {
    fun fmt(v: Float?) = v?.let { "${it.roundToInt()}°F" } ?: "--"
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatChip("Current", fmt(state.current))
            StatChip("Avg", fmt(state.avg))
            StatChip("Min", fmt(state.min))
            StatChip("Max", fmt(state.max))
        }
    }
}
// Small labeled chip
@Composable
fun StatChip(label: String, value: String) {
    Surface(shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

// Simple line chart drawn on a Canvas
@Composable
fun TemperatureChart(readings: List<TempReading>, modifier: Modifier = Modifier) {
    val values = readings.map { it.valueF }
    val min = values.minOrNull() ?: 65f
    val max = values.maxOrNull() ?: 85f
    val padding = 12f

    // Capture theme colors in @Composable scope
    val outlineColor = MaterialTheme.colorScheme.outline
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas

        val w = size.width
        val h = size.height
        val n = values.size
        val stepX = (w - 2 * padding) / (n - 1).coerceAtLeast(1)
        val range = (max - min).takeIf { it > 0f } ?: 1f

        // Axes
        drawLine(
            color = outlineColor,
            start = Offset(padding, h - padding),
            end = Offset(w - padding, h - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = outlineColor,
            start = Offset(padding, padding),
            end = Offset(padding, h - padding),
            strokeWidth = 2f
        )
        // Build a path connecting all points from left to right
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = padding + i * stepX
            // Y increases downward
            val y = padding + (1f - (v - min) / range) * (h - 2 * padding)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
    }
}

// Scrollable list of readings with timestamp and value
@Composable
fun ReadingList(readings: List<TempReading>, modifier: Modifier = Modifier) {
    val fmt = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(readings) { r ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(r.time.format(fmt), modifier = Modifier.width(80.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.tertiary, shape = MaterialTheme.shapes.extraSmall)
                )
                Spacer(Modifier.width(8.dp))
                Text("${"%.1f".format(r.valueF)} °F", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }

        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewDashboard() {
    TemperatureDashboardTheme {
        val now = LocalDateTime.now()
        val readings = List(10) { i -> TempReading(now.minusSeconds((10 - i) * 2L), 65 + i * 2f) }
        val mock = DashboardState(readings = readings, paused = true)
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            StatsRow(mock)
            TemperatureChart(readings, Modifier.fillMaxWidth().height(180.dp))
            ReadingList(readings, Modifier.weight(1f))
        }
    }
}