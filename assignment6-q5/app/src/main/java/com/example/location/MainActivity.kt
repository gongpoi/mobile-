package com.example.location

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
import android.location.Geocoder
import androidx.compose.ui.unit.dp
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale
import com.example.location.ui.theme.LocationTheme

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Compose states
    private val hasLocationPermission = mutableStateOf(false)
    private val userLocationState = mutableStateOf<LatLng?>(null)
    private val addressState = mutableStateOf("")

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasLocationPermission.value = isGranted
            if (isGranted) {
                fetchUserLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize fused location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            LocationTheme {
                LocationScreen(
                    hasPermission = hasLocationPermission.value,
                    userLocation = userLocationState.value,
                    address = addressState.value
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Each time we come to the foreground, re-check permission and location
        checkAndRequestLocationPermission()
    }

    private fun checkAndRequestLocationPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            hasLocationPermission.value = true
            fetchUserLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchUserLocation() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                userLocationState.value = latLng
                // Reverse geocoding from coordinates to address string
                addressState.value = getAddressFromLatLng(latLng) ?: "Address unavailable"
            }
        }
    }

    private fun getAddressFromLatLng(latLng: LatLng): String? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!list.isNullOrEmpty()) {
                list[0].getAddressLine(0)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun LocationScreen(
    hasPermission: Boolean,
    userLocation: LatLng?,
    address: String,
    modifier: Modifier = Modifier
) {
    // List of custom markers added by tapping on the map
    val customMarkers = remember { mutableStateListOf<LatLng>() }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Location Information",
                style = MaterialTheme.typography.titleLarge
            )
            // Shows whether location permission is granted
            Text(
                text = if (hasPermission) {
                    "Location permission: GRANTED"
                } else {
                    "Location permission: REQUIRED"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            // Shows the user's address if available
            Text(
                text = "Address: ${if (address.isNotEmpty()) address else "Unknown"}",
                style = MaterialTheme.typography.bodySmall
            )

            // Button to clear all
            if (customMarkers.isNotEmpty()) {
                androidx.compose.material3.Button(
                    onClick = { customMarkers.clear() }
                ) {
                    Text("Clear custom markers")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (hasPermission && userLocation != null) {
                    MapWithMarkers(
                        userLocation = userLocation,
                        customMarkers = customMarkers   // ✅ 传进去
                    )
                } else {
                    Text("Waiting for location / permission...")
                }
            }
        }
    }
}
@Composable
fun MapWithMarkers(
    userLocation: LatLng,
    customMarkers: MutableList<LatLng>   // List backed by mutableStateListOf from parent
) {
    // Camera position state centered on user's location
    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 16f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        // When the map is tapped, add a new custom marker at the tapped location
        onMapClick = { latLng ->
            customMarkers.add(latLng)
        }
    ) {
        // Marker for the user's current location
        Marker(
            state = MarkerState(position = userLocation),
            title = "You are here"
        )

        // Custom markers added by the user
        customMarkers.forEach { latLng ->
            Marker(
                state = MarkerState(position = latLng),
                title = "Custom Marker"
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
fun LocationPreview() {
    LocationTheme {
        LocationScreen(
            hasPermission = true,
            userLocation = LatLng(40.0, -74.0),
            address = "Preview Address"
        )
    }
}