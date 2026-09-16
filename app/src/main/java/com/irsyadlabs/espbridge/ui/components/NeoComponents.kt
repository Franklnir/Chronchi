package com.irsyadlabs.espbridge.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.ui.theme.NeoTokens

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeoTokens.White,
    borderColor: Color = NeoTokens.Black,
    shadowColor: Color = NeoTokens.Black,
    shadowOffset: Dp = NeoTokens.ShadowOffset,
    cornerRadius: Dp = NeoTokens.CardCorner,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        // Hard Shadow layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor, RoundedCornerShape(cornerRadius))
        )
        // Main Surface layer
        Box(
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(cornerRadius))
                .border(NeoTokens.BorderWidth, borderColor, RoundedCornerShape(cornerRadius))
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    color: Color = NeoTokens.Yellow,
    textColor: Color = NeoTokens.Black,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val offset = if (isPressed) 1.dp else NeoTokens.ShadowOffset

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            )
    ) {
        // Shadow layer
        if (!isPressed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = offset, y = offset)
                    .background(NeoTokens.Black, RoundedCornerShape(NeoTokens.ButtonCorner))
            )
        }
        // Button surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = if (isPressed) 3.dp else 0.dp, y = if (isPressed) 3.dp else 0.dp)
                .background(if (enabled) color else NeoTokens.Gray, RoundedCornerShape(NeoTokens.ButtonCorner))
                .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(NeoTokens.ButtonCorner)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = textColor,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.width(10.dp))
                } else if (icon != null) {
                    Icon(icon, null, tint = textColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = textColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    error: String? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            color = NeoTokens.Black,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box {
            // Shadow behind input
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 3.dp, y = 3.dp)
                    .background(NeoTokens.Black, RoundedCornerShape(NeoTokens.ButtonCorner))
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = NeoTokens.Muted) },
                leadingIcon = leadingIcon?.let { { Icon(it, null, tint = NeoTokens.Black) } },
                trailingIcon = trailingIcon,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                singleLine = true,
                shape = RoundedCornerShape(NeoTokens.ButtonCorner),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeoTokens.Black,
                    unfocusedBorderColor = NeoTokens.Black,
                    focusedContainerColor = NeoTokens.White,
                    unfocusedContainerColor = NeoTokens.White,
                    focusedTextColor = NeoTokens.Black,
                    unfocusedTextColor = NeoTokens.Black
                )
            )
        }
        if (error != null) {
            Text(
                text = error,
                color = NeoTokens.Coral,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun NeoBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeoTokens.Yellow,
    textColor: Color = NeoTokens.Black
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(NeoTokens.PillCorner))
            .border(2.dp, NeoTokens.Black, RoundedCornerShape(NeoTokens.PillCorner))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun NeoPulseIndicator(
    active: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = NeoTokens.Emerald,
    inactiveColor: Color = NeoTokens.Coral
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = modifier.size(16.dp), contentAlignment = Alignment.Center) {
        if (active) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(scale)
                    .background(activeColor.copy(alpha = 0.35f), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (active) activeColor else inactiveColor, CircleShape)
                .border(1.5.dp, NeoTokens.Black, CircleShape)
        )
    }
}

@Composable
fun NeoProgressBar(
    label: String,
    percentage: Int,
    modifier: Modifier = Modifier,
    color: Color = NeoTokens.Emerald
) {
    val clamped = percentage.coerceIn(0, 100)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeoTokens.Black)
            Text("$clamped%", fontWeight = FontWeight.Black, fontSize = 13.sp, color = NeoTokens.Black)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(NeoTokens.White, RoundedCornerShape(8.dp))
                .border(2.dp, NeoTokens.Black, RoundedCornerShape(8.dp))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped / 100f)
                    .background(color, RoundedCornerShape(6.dp))
            )
        }
    }
}
