package com.irsyadlabs.espbridge.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.core.model.AppTheme
import com.irsyadlabs.espbridge.ui.components.PrimaryActionButton
import com.irsyadlabs.espbridge.ui.components.StatusPill
import com.irsyadlabs.espbridge.ui.components.StatusTone
import com.irsyadlabs.espbridge.ui.components.UiTokens
import com.irsyadlabs.espbridge.ui.components.PlayfulCard
import com.irsyadlabs.espbridge.ui.components.MainScreenColumn
import com.irsyadlabs.espbridge.ui.theme.*

@Composable
fun LoginScreen(
    busy: Boolean,
    firebaseReady: Boolean,
    message: String?,
    onLogin: (String, String) -> Unit,
    onGoogle: () -> Unit,
    onRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }

    MainScreenColumn(
        modifier = Modifier.padding(horizontal = UiTokens.HorizontalPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))
            
            // Professional Illustrative Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(SketchTeal, RoundedCornerShape(UiTokens.CardRadius))
                    .border(1.5.dp, SketchBorder, RoundedCornerShape(UiTokens.CardRadius))
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(UiTokens.InnerRadius))
                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(UiTokens.InnerRadius)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Bluetooth,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                "Chronchi Login",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Authorized access to your hardware bridge.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 40.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            PlayfulCard(
                modifier = Modifier.fillMaxWidth(),
                innerPadding = PaddingValues(20.dp)
            ) {
                Column {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            validationMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Protocol ID / Email") },
                        leadingIcon = { Icon(Icons.Rounded.Mail, null, tint = SketchTeal) },
                        singleLine = true,
                        shape = RoundedCornerShape(UiTokens.SmallRadius),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SketchTeal,
                            unfocusedBorderColor = SketchBorder.copy(alpha = 0.2f),
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
                        label = { Text("Security Key") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = SketchTeal) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = SketchTeal
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(UiTokens.SmallRadius),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SketchTeal,
                            unfocusedBorderColor = SketchBorder.copy(alpha = 0.2f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    
                    val visibleMessage = validationMessage ?: message
                    if (!visibleMessage.isNullOrBlank()) {
                        Text(
                            visibleMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    PrimaryActionButton(
                        text = if (busy) "CONNECTING..." else "ENTER HUB",
                        enabled = !busy,
                        loading = busy,
                        onClick = {
                            validationMessage = when {
                                email.isBlank() || !email.contains('@') -> "Valid identity required."
                                password.length < 6 -> "Security key too short."
                                else -> null
                            }
                            if (validationMessage == null) onLogin(email, password)
                        }
                    )
                }
            }
            
            Spacer(Modifier.height(48.dp))
            
            TextButton(onClick = onRegister, enabled = !busy) {
                Text(
                    "Request Device Authorization", 
                    color = SketchTeal, 
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(
    name = "Login",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun LoginScreenPreview() {
    XichiTheme {
        LoginScreen(
            busy = false,
            firebaseReady = false,
            message = null,
            onLogin = { _, _ -> },
            onGoogle = {},
            onRegister = {}
        )
    }
}
