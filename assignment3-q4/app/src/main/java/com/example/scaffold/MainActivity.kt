package com.example.scaffold

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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.scaffold.ui.theme.ScaffoldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScaffoldTheme {
                AppScaffold()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    // Host for Snackbars shown by the FAB
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    //bottom navigation state
    var selectedIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Home", "Settings", "Profile")
    // Top bar: centered app title
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "App") }
            )
        },
        // Bottom navigation
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = { },   //text items
                        label = { Text(label) }
                    )
                }
            }
        },
        // FAB shows a Snackbar when clicked
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("FAB clicked on ${tabs[selectedIndex]}")
                    }
                }
            ) {
                Text("+")
            }
        },
        // Attach the SnackbarHost so Scaffold can place snackbars properly
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // apply innerPadding to avoid overlap with top/bottom bars & FAB
        ScreenContent(
            currentTab = tabs[selectedIndex],
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        )
    }
}

@Composable
//Shows the currently selected bottom tab.
private fun ScreenContent(currentTab: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Current tab: $currentTab")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAppScaffold() {
    ScaffoldTheme {
        AppScaffold()
    }
}