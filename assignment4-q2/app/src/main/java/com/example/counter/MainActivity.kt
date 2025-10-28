package com.example.counter

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.counter.ui.theme.CounterTheme
// ---------- State ----------
data class CounterState(
    val count: Int = 0,
    val autoEnabled: Boolean = false,
    val intervalSec: Int = 3
)

class CounterViewModel : ViewModel() {
    private val _ui = MutableStateFlow(CounterState())
    val ui: StateFlow<CounterState> = _ui.asStateFlow()

    private val vmScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var autoJob: Job? = null

    fun increment() { _ui.value = _ui.value.copy(count = _ui.value.count + 1) }
    fun decrement() { _ui.value = _ui.value.copy(count = _ui.value.count - 1) }
    fun reset() { _ui.value = _ui.value.copy(count = 0) }

    fun toggleAuto(enabled: Boolean) {
        _ui.value = _ui.value.copy(autoEnabled = enabled)
        restartAutoIfNeeded()
    }

    // Update the interval
    fun setIntervalSec(sec: Int) {
        val clamped = sec.coerceIn(1, 10)
        _ui.value = _ui.value.copy(intervalSec = clamped)
        restartAutoIfNeeded()
    }

    //launch the auto-increment loop if auto mode is ON
    private fun restartAutoIfNeeded() {
        autoJob?.cancel()
        autoJob = null
        if (_ui.value.autoEnabled) {
            autoJob = vmScope.launch {
                while (isActive && _ui.value.autoEnabled) {
                    delay(_ui.value.intervalSec * 1000L)
                    if (!_ui.value.autoEnabled) break
                    increment()
                }
            }
        }
    }

    override fun onCleared() {
        autoJob?.cancel()
        vmScope.cancel()
        super.onCleared()
    }
}

class MainActivity : ComponentActivity() {
    private val vm: CounterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CounterTheme {
                CounterApp(vm)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterApp(vm: CounterViewModel) {
    val state by vm.ui.collectAsState()
    //settings panel toggle
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Counter++") },
                actions = {
                    TextButton(onClick = { showSettings = !showSettings }) {
                        Text(if (showSettings) "Close" else "Settings")
                    }
                }
            )
        }
    ) { inner ->
        //show Settings or the Home controls
        if (showSettings) {
            SettingsPanel(
                interval = state.intervalSec,
                auto = state.autoEnabled,
                onIntervalChange = { vm.setIntervalSec(it) },
                onToggleAuto = { vm.toggleAuto(it) },
                modifier = Modifier.padding(inner)
            )
        } else {
            HomePanel(
                count = state.count,
                auto = state.autoEnabled,
                interval = state.intervalSec,
                onInc = { vm.increment() },
                onDec = { vm.decrement() },
                onReset = { vm.reset() },
                onToggleAuto = { vm.toggleAuto(it) },
                modifier = Modifier.padding(inner)
            )
        }
    }
}
@Composable
fun HomePanel(
    count: Int,
    auto: Boolean,
    interval: Int,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onReset: () -> Unit,
    onToggleAuto: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current value + status text
        Text("Count: $count", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Auto mode: " + if (auto) "ON" else "OFF", style = MaterialTheme.typography.titleMedium)
        Text("Interval: ${interval}s", color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Manual controls
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onDec) { Text("-1") }
            Button(onClick = onReset) { Text("Reset") }
            Button(onClick = onInc) { Text("+1") }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Auto", modifier = Modifier.padding(end = 8.dp))
            Switch(checked = auto, onCheckedChange = onToggleAuto)
        }

        Spacer(Modifier.height(8.dp))

    }
}

@Composable
fun SettingsPanel(
    interval: Int,
    auto: Boolean,
    onIntervalChange: (Int) -> Unit,
    onToggleAuto: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local editable text mirrors the current interval
    var text by remember(interval) { mutableStateOf(interval.toString()) }
    val parsed = text.toIntOrNull()
    val isValid = parsed != null && parsed in 1..10
    val helper = if (isValid) "1–10 seconds" else " Enter 1–10"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Auto-increment interval", style = MaterialTheme.typography.titleMedium)
        // Numeric input for interval
        OutlinedTextField(
            value = text,
            onValueChange = { new ->
                // Keep only digits;
                text = new.filter { it.isDigit() }
            },
            label = { Text("Seconds (1–10)") },
            supportingText = { Text(helper) },
            isError = !isValid && text.isNotEmpty(),
            singleLine = true
        )
        // Save
        Button(
            onClick = {
                val value = (parsed ?: interval).coerceIn(1, 10)
                onIntervalChange(value)
            },
            enabled = text.isNotEmpty() && parsed != null
        ) {
            Text("Save")
        }

    }
}

// ---------- Preview（可选） ----------
@Preview(showBackground = true)
@Composable
fun PreviewCounter() {
    CounterTheme {
        val vm = CounterViewModel()
        CounterApp(vm)
    }
}