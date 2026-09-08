package com.irsyadlabs.espbridge.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.irsyadlabs.espbridge.core.model.AppTheme
import com.irsyadlabs.espbridge.ui.components.MainScreenColumn
import com.irsyadlabs.espbridge.ui.components.PrimaryActionButton
import com.irsyadlabs.espbridge.ui.components.UiTokens
import com.irsyadlabs.espbridge.ui.theme.*

@Composable
fun RegisterScreen(
    busy: Boolean,
    message: String?,
    onRegister: (String, String) -> Unit,
    onGoogle: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val matches = password == confirm && password.length >= 6

    MainScreenColumn(
        modifier = Modifier.padding(horizontal = UiTokens.HorizontalPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) { 
                Icon(Icons.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary) 
            }
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                "Create Account",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Join the Chronchi ecosystem today.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    validationMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Mail, null, tint = MaterialTheme.colorScheme.primary) },
                label = { Text("Email Address") },
                singleLine = true,
                shape = RoundedCornerShape(UiTokens.SmallRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    validationMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(UiTokens.SmallRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = {
                    confirm = it
                    validationMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = confirm.isNotEmpty() && !matches,
                supportingText = {
                    if (confirm.isNotEmpty() && !matches) Text("Passwords must match and be at least 6 characters.")
                },
                shape = RoundedCornerShape(UiTokens.SmallRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            
            val visibleMessage = validationMessage ?: message
            if (!visibleMessage.isNullOrBlank()) {
                Text(
                    visibleMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(Modifier.height(36.dp))
            
            PrimaryActionButton(
                text = if (busy) "CREATING ACCOUNT..." else "REGISTER",
                enabled = !busy,
                loading = busy,
                onClick = {
                    validationMessage = when {
                        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address."
                        password.length < 6 -> "Password must be at least 6 characters."
                        password != confirm -> "Passwords do not match."
                        else -> null
                    }
                    if (validationMessage == null) onRegister(email, password)
                }
            )
            
            Spacer(Modifier.height(24.dp))
            
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Already have an account? Login", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(name = "Register", showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenPreview() {
    ChronchiTheme {
        RegisterScreen(
            busy = false,
            message = null,
            onRegister = { _, _ -> },
            onGoogle = {},
            onBack = {}
        )
    }
}
