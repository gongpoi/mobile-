package com.example.poly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.poly.ui.theme.PolyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PolyTheme {
                PolylinePolygonScreen()
            }
        }
    }
}

@Composable
fun PolylinePolygonScreen(modifier: Modifier = Modifier) {
    // Map center
    val center = LatLng(42.0, -71.0)

    // Hiking trail
    val trailPoints = remember {
        listOf(
            LatLng(41.9995, -71.0015),
            LatLng(41.9998, -71.0010),
            LatLng(42.0002, -71.0006),
            LatLng(42.0007, -71.0003),
            LatLng(42.0011, -71.0001)
        )
    }

    // 2️⃣ Park area
    val parkPolygon = remember {
        listOf(
            // top-left
            LatLng(42.0016, -71.0022),
            // top-right
            LatLng(42.0016, -71.0012),
            // bottom-right
            LatLng(42.0008, -71.0012),
            // bottom-left
            LatLng(42.0008, -71.0022)
        )
    }

    // line width for trail and polygon border
    var trailWidth by remember { mutableStateOf(12f) }
    var polygonStrokeWidth by remember { mutableStateOf(10f) }

    // colors
    var trailColor by remember { mutableStateOf(Color(0xFF1E88E5)) }           // 蓝色路线
    var polygonStrokeColor by remember { mutableStateOf(Color(0xFF43A047)) }   // 绿色边框
    var polygonFillColor by remember { mutableStateOf(Color(0x5534C759)) }     // 半透明绿色填充

    // nfo text that changes when the user clicks on the line or polygon
    var overlayInfo by remember { mutableStateOf("Tap the trail or park area for info") }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Polyline & Polygon Demo",
                style = MaterialTheme.typography.titleLarge
            )

            // Control panel for width & color settings
            ControlPanel(
                trailWidth = trailWidth,
                onTrailWidthChange = { trailWidth = it },
                polygonStrokeWidth = polygonStrokeWidth,
                onPolygonStrokeWidthChange = { polygonStrokeWidth = it },
                onTrailColorBlue = { trailColor = Color(0xFF1E88E5) },
                onTrailColorRed = { trailColor = Color(0xFFE53935) },
                onPolygonColorGreen = {
                    polygonStrokeColor = Color(0xFF43A047)
                    polygonFillColor = Color(0x5534C759)
                },
                onPolygonColorPurple = {
                    polygonStrokeColor = Color(0xFF8E24AA)
                    polygonFillColor = Color(0x558E24AA)
                }
            )

            // Map container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                HikingMap(
                    center = center,
                    trailPoints = trailPoints,
                    parkPolygon = parkPolygon,
                    trailColor = trailColor,
                    trailWidth = trailWidth,
                    polygonStrokeColor = polygonStrokeColor,
                    polygonFillColor = polygonFillColor,
                    polygonStrokeWidth = polygonStrokeWidth,
                    // Click callback when user taps the polyline
                    onTrailClick = {
                        overlayInfo = "Trail A: Scenic hiking route · 2.5 km · Easy"
                    },
                    // Click callback when user taps the polygon
                    onAreaClick = {
                        overlayInfo = "Greenwood Park: Picnic area, playground, lake"
                    }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFECEFF1)
                )
            ) {
                Text(
                    text = overlayInfo,
                    modifier = Modifier
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ControlPanel(
    trailWidth: Float,
    onTrailWidthChange: (Float) -> Unit,
    polygonStrokeWidth: Float,
    onPolygonStrokeWidthChange: (Float) -> Unit,
    onTrailColorBlue: () -> Unit,
    onTrailColorRed: () -> Unit,
    onPolygonColorGreen: () -> Unit,
    onPolygonColorPurple: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "Trail width: ${trailWidth.toInt()} px")
        Slider(
            value = trailWidth,
            onValueChange = onTrailWidthChange,
            valueRange = 5f..30f
        )

        Text(text = "Polygon border width: ${polygonStrokeWidth.toInt()} px")
        Slider(
            value = polygonStrokeWidth,
            onValueChange = onPolygonStrokeWidthChange,
            valueRange = 3f..25f
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Trail color:")
            Button(onClick = onTrailColorBlue) {
                Text("Blue")
            }
            Button(onClick = onTrailColorRed) {
                Text("Red")
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Park color:")
            Button(onClick = onPolygonColorGreen) {
                Text("Green")
            }
            Button(onClick = onPolygonColorPurple) {
                Text("Purple")
            }
        }
    }
}

@Composable
fun HikingMap(
    center: LatLng,
    trailPoints: List<LatLng>,
    parkPolygon: List<LatLng>,
    trailColor: Color,
    trailWidth: Float,
    polygonStrokeColor: Color,
    polygonFillColor: Color,
    polygonStrokeWidth: Float,
    onTrailClick: () -> Unit,
    onAreaClick: () -> Unit
) {
    // CameraPositionState centers the map on the given LatLng and zoom level
    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 15f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Hiking trail polyline with click listener
        Polyline(
            points = trailPoints,
            color = trailColor,
            width = trailWidth,
            clickable = true,
            onClick = {
                onTrailClick()
            }
        )

        // Park area polygon with click listener and styling
        Polygon(
            points = parkPolygon,
            fillColor = polygonFillColor,
            strokeColor = polygonStrokeColor,
            strokeWidth = polygonStrokeWidth,
            clickable = true,
            onClick = {
                onAreaClick()
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PolyPreview() {
    PolyTheme {
        PolylinePolygonScreen()
    }
}