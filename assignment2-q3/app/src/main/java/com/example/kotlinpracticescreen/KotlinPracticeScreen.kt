package com.example.kotlinpracticescreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.kotlinpracticescreen.ui.theme.KotlinPracticeScreenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KotlinPracticeScreenTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    KotlinPracticeScreen()
                }
            }
        }
    }
}

@Composable
fun KotlinPracticeScreen() {
    var input by rememberSaveable { mutableStateOf("cat") }
    var counter by rememberSaveable { mutableStateOf(0) }
    var nullableMessage: String? by rememberSaveable { mutableStateOf("Hello") }

    val whenResult = when (input.lowercase()) {
        "cat" -> "You chose a Cat 🐱"
        "dog" -> "You chose a Dog 🐶"
        "fish" -> "You chose a Fish 🐟"
        else -> "Unknown animal"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("when : $whenResult")

        nullableMessage?.let { Text(" $it") }

        Text("counter: $counter")

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(onClick = { if (counter < 5) counter++ }) {
                Text("counter +1")
            }
            Button(onClick = {
                input = when (input) {
                    "cat" -> "dog"
                    "dog" -> "fish"
                    else -> "cat"
                }
            }) { Text(" $input") }
            Button(onClick = { nullableMessage = if (nullableMessage == null) "Hello again!" else null }) {
                Text("nullable strings")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewKotlinPractice() {
    KotlinPracticeScreenTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            KotlinPracticeScreen()
        }
    }
}