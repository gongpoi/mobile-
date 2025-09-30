package com.example.login

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextFieldDefaults
import kotlinx.coroutines.launch
import com.example.login.ui.theme.LoginTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoginTheme {
                LoginScreen()
            }
        }
    }
}

@Composable
fun LoginScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    // Error messages: null means "no error"
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    // Toggle show/hide password without adding icon deps
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)     // 正确应用 innerPadding，避免遮挡
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Title styled with Material 3 typography/color
            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(24.dp))

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    if (!it.isBlank()) usernameError = null
                },
                singleLine = true,
                isError = usernameError != null,
                label = {
                    Text(
                        text = "Username",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                ),
                modifier = Modifier.fillMaxWidth()
            )
            // Inline error message under the field
            if (usernameError != null) {
                Text(
                    text = usernameError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (!it.isBlank()) passwordError = null
                },
                singleLine = true,
                isError = passwordError != null,
                label = {
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                // Toggle between hidden & plain text without icon dependency
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    // Text-based toggle ("Show"/"Hide") for simplicity
                    Text(
                        text = if (showPassword) "Hide" else "Show",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { showPassword = !showPassword }
                    )
                },
                colors = TextFieldDefaults.colors(),
                modifier = Modifier.fillMaxWidth()
            )
            // Inline error message under the field
            if (passwordError != null) {
                Text(
                    text = passwordError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            //Submit button
            Button(
                onClick = {
                    // Basic validation: mark empty fields with error messages
                    var hasError = false
                    if (username.isBlank()) {
                        usernameError = "Username cannot be empty"
                        hasError = true
                    }
                    if (password.isBlank()) {
                        passwordError = "Password cannot be empty"
                        hasError = true
                    }
                    // On success, show a brief Snackbar as feedback
                    if (!hasError) {
                        scope.launch {
                            snackbarHostState.showSnackbar("submitted")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Submit",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewLogin() {
    LoginTheme {
        LoginScreen()
    }
}