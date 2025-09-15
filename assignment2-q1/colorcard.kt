package com.example.colorcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.colorcard.ui.theme.ColorcardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColorcardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        ColorCardsScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun ColorCard(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.DarkGray
) {
    val textColor = if (color.luminance() < 0.5f) Color.White else Color.Black

    Box(
        modifier = modifier
            .background(color = color, shape = shape)
            .let { base ->
                if (borderWidth > 0.dp) {
                    base.border(width = borderWidth, color = borderColor, shape = shape)
                } else base
            }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
fun ColorCardsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // size + background
        ColorCard(
            color = Color(0xFF42A5F5), // Blue 400
            label = "A",
            modifier = Modifier.size(100.dp)
        )

        // size + border + background
        ColorCard(
            color = Color(0xFFEF5350), // Red 400
            label = "B",
            modifier = Modifier.size(120.dp),
            borderWidth = 2.dp,
            borderColor = Color(0xFFBDBDBD)
        )

        // padding + size + border + background
        ColorCard(
            color = Color(0xFF66BB6A), // Green 400
            label = "C",
            modifier = Modifier
                .padding(4.dp)
                .size(140.dp),
            borderWidth = 3.dp,
            borderColor = Color.DarkGray,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewColorCards() {
    ColorCardsScreen()
}