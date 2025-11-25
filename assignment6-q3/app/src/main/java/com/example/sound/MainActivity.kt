package com.example.sound

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
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.log10
import kotlin.math.sqrt
import com.example.sound.ui.theme.SoundTheme

class MainActivity : ComponentActivity() {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false

    // 当前 dB 数值 + 权限/错误信息
    private val soundDbState = mutableStateOf(0.0)
    private val errorMessageState = mutableStateOf<String?>(null)
    private val hasAudioPermissionState = mutableStateOf(false)

    // 运行时权限请求器
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            hasAudioPermissionState.value = isGranted
            if (isGranted) {
                errorMessageState.value = null
                startRecordingSafely()
            } else {
                errorMessageState.value = "Microphone permission denied."
                stopRecording()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoundTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SoundMeterScreen(
                        db = soundDbState.value,
                        hasPermission = hasAudioPermissionState.value,
                        errorMessage = errorMessageState.value,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        // Check or request permission whenever activity comes to foreground
        super.onResume()
        checkAndRequestPermission()
    }

    override fun onPause() {
        super.onPause()
        // Stop recording to free the microphone and background thread
        stopRecording()
    }

    private fun checkAndRequestPermission() {
        val hasPermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            hasAudioPermissionState.value = true
            errorMessageState.value = null
            startRecordingSafely()
        } else {
            // Show system permission dialog for microphone access
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecordingSafely() {
        if (isRecording) return

        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize <= 0) {
            errorMessageState.value = "Unable to get buffer size for AudioRecord."
            return
        }

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            // Initialization failed
            errorMessageState.value = "AudioRecord initialization failed."
            recorder.release()
            return
        }

        audioRecord = recorder

        try {
            recorder.startRecording()
        } catch (e: SecurityException) {
            errorMessageState.value = "No RECORD_AUDIO permission."
            recorder.release()
            audioRecord = null
            return
        } catch (e: IllegalStateException) {
            errorMessageState.value = "Failed to start recording."
            recorder.release()
            audioRecord = null
            return
        }

        isRecording = true
        errorMessageState.value = null

        val buffer = ShortArray(bufferSize)
        recordingThread = Thread {
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val rms = calculateRms(buffer, read)
                    val db = rmsToDb(rms)
                    runOnUiThread {
                        soundDbState.value = db
                    }
                }
            }
        }.apply { start() }
    }

    private fun stopRecording() {
        if (!isRecording) return
        isRecording = false

        try {
            recordingThread?.join()
        } catch (_: InterruptedException) {
        }

        audioRecord?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        audioRecord = null
        recordingThread = null
    }

    // Compute RMS
    private fun calculateRms(buffer: ShortArray, read: Int): Double {
        if (read <= 0) return 0.0
        var sum = 0.0
        for (i in 0 until read) {
            val v = buffer[i].toDouble()
            sum += v * v
        }
        val mean = sum / read
        return sqrt(mean)
    }

    // Convert RMS value
    private fun rmsToDb(rms: Double): Double {
        if (rms <= 0.0) return 0.0
        return 20 * log10(rms)
    }
}

@Composable
fun SoundMeterScreen(
    db: Double,
    hasPermission: Boolean,
    errorMessage: String?,
    threshold: Double = 70.0,
    modifier: Modifier = Modifier
) {
    // Clamp dB into a reasonable display range
    val clamped = db.coerceIn(0.0, 90.0)
    val progress = (clamped / 90.0).toFloat()
    // Color of the bar depending on dB
    val barColor = when {
        clamped < 50 -> Color(0xFF4CAF50)
        clamped < threshold -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sound Meter",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            !hasPermission -> {
                Text(
                    text = "Microphone permission is required.",
                    color = Color.Red
                )
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage,
                    color = Color.Red
                )
            }
            else -> {
                Text(
                    text = "%.1f dB".format(clamped),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(barColor)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (clamped >= threshold) {
                    Text(
                        text = "Too loud! Noise exceeds threshold.",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        text = "Safe level.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Threshold: %.0f dB".format(threshold),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SoundMeterPreview() {
    SoundTheme {
        SoundMeterScreen(
            db = 65.0,
            hasPermission = true,
            errorMessage = null
        )
    }
}