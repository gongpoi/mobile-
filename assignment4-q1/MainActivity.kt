package com.example.lifetracker

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.lifetracker.ui.theme.LifeTrackerTheme

enum class LifeType(val label: String) {
    ON_CREATE("onCreate"),
    ON_START("onStart"),
    ON_RESUME("onResume"),
    ON_PAUSE("onPause"),
    ON_STOP("onStop"),
    ON_DESTROY("onDestroy")
}

data class LifeEvent(
    val type: LifeType,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

//ViewModel
class LifeTrackerViewModel : ViewModel() {
    // Setting: show a snackbar
    private val _events = MutableStateFlow<List<LifeEvent>>(emptyList())
    val events: StateFlow<List<LifeEvent>> = _events.asStateFlow()

    private val _showSnackOnTransition = MutableStateFlow(true)
    val showSnackOnTransition: StateFlow<Boolean> = _showSnackOnTransition.asStateFlow()

    fun log(type: LifeType) {
        viewModelScope.launch {
            _events.value = _events.value + LifeEvent(type)
        }
    }

    // Clear the log list
    fun clear() { _events.value = emptyList() }
    // Toggle snackbar setting
    fun setSnackEnabled(enabled: Boolean) { _showSnackOnTransition.value = enabled }

    val currentState: LifeType?
        get() = _events.value.lastOrNull()?.type
}

//  Lifecycle
class ActivityLifeObserver(private val onEvent: (LifeType) -> Unit) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) { onEvent(LifeType.ON_START) }
    override fun onResume(owner: LifecycleOwner) { onEvent(LifeType.ON_RESUME) }
    override fun onPause(owner: LifecycleOwner) { onEvent(LifeType.ON_PAUSE) }
    override fun onStop(owner: LifecycleOwner) { onEvent(LifeType.ON_STOP) }
    override fun onDestroy(owner: LifecycleOwner) { onEvent(LifeType.ON_DESTROY) }
}

class MainActivity : ComponentActivity() {
    private val vm: LifeTrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        vm.log(LifeType.ON_CREATE)
        lifecycle.addObserver(ActivityLifeObserver { vm.log(it) })

        setContent {
            LifeTrackerTheme {
                LifeTrackerApp(vm)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeTrackerApp(vm: LifeTrackerViewModel) {
    val events by vm.events.collectAsState()
    val snackEnabled by vm.showSnackOnTransition.collectAsState()
    val snackHostState = remember { SnackbarHostState() }
    val lastEvent = events.lastOrNull()

    //  when a new event arrives and setting is enabled, show a snackbar.
    LaunchedEffect(lastEvent, snackEnabled) {
        if (snackEnabled && lastEvent != null) {
            snackHostState.showSnackbar("Lifecycle: ${lastEvent.type.label}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LifeTracker") },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("Snackbar", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        //enable/disable snackbars
                        Switch(checked = snackEnabled, onCheckedChange = vm::setSnackEnabled)
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = vm::clear) { Text("Clear") }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackHostState) }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            CurrentStateHeader(current = vm.currentState)
            EventList(events = events, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun CurrentStateHeader(current: LifeType?) {
    val (label, color) = when (current) {
        // Map lifecycle state to a label
        LifeType.ON_CREATE -> "Created" to MaterialTheme.colorScheme.primary
        LifeType.ON_START -> "Started" to MaterialTheme.colorScheme.primaryContainer
        LifeType.ON_RESUME -> "Resumed" to MaterialTheme.colorScheme.tertiary
        LifeType.ON_PAUSE -> "Paused" to MaterialTheme.colorScheme.secondary
        LifeType.ON_STOP -> "Stopped" to MaterialTheme.colorScheme.errorContainer
        LifeType.ON_DESTROY -> "Destroyed" to MaterialTheme.colorScheme.error
        null -> "Unknown" to MaterialTheme.colorScheme.outline
    }

    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            // Colored square
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(color)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Current state: $label",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EventList(events: List<LifeEvent>, modifier: Modifier = Modifier) {
    // Time format
    val fmt = remember { DateTimeFormatter.ofPattern("HH:mm:ss.SSS") }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(events) { ev ->
            EventRow(ev, fmt)
            Divider()
        }
    }
}

@Composable
fun EventRow(ev: LifeEvent, fmt: DateTimeFormatter) {
    val color = when (ev.type) {
        LifeType.ON_CREATE -> MaterialTheme.colorScheme.primary
        LifeType.ON_START -> MaterialTheme.colorScheme.primaryContainer
        LifeType.ON_RESUME -> MaterialTheme.colorScheme.tertiary
        LifeType.ON_PAUSE -> MaterialTheme.colorScheme.secondary
        LifeType.ON_STOP -> MaterialTheme.colorScheme.errorContainer
        LifeType.ON_DESTROY -> MaterialTheme.colorScheme.error
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            // Event label
            Text(ev.type.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                ev.timestamp.format(fmt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewLifeTracker() {
    LifeTrackerTheme {
        val vm = LifeTrackerViewModel().apply {
            log(LifeType.ON_CREATE); log(LifeType.ON_START); log(LifeType.ON_RESUME)
        }
        LifeTrackerApp(vm)
    }
}
