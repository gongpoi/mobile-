package com.example.myapplication

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
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RCLayout(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun RCLayout(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        // 25% width of the Row
        Box(
            modifier = Modifier
                .weight(0.25f)
                .fillMaxHeight()
                .background(Color.Yellow),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Pokemon")
        }

        // 75% width of the Row
        Column(
            modifier = Modifier
                .weight(0.75f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .weight(2f)  // 2 parts of total 10
                    .fillMaxWidth()
                    .background(Color.Green),
                contentAlignment = Alignment.Center
            ) {
                Text("Bulbasaur")
            }
            Box(
                modifier = Modifier
                    .weight(3f) // 3 parts of total 10
                    .fillMaxWidth()
                    .background(Color.Blue),
                contentAlignment = Alignment.Center
            ) {
                Text("Squirtle")
            }
            Box(
                modifier = Modifier
                    .weight(5f) // 5 parts of total 10
                    .fillMaxWidth()
                    .background(Color.Red),
                contentAlignment = Alignment.Center
            ) {
                Text("Charmander")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRowColumnWeightSplitLayout() {
    MyApplicationTheme {
        RCLayout(Modifier.fillMaxSize())
    }
}