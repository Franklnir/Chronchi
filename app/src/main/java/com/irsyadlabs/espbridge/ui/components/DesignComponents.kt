package com.irsyadlabs.espbridge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.irsyadlabs.espbridge.core.model.AppTheme
import com.irsyadlabs.espbridge.ui.theme.*

object UiTokens {
    val HorizontalPadding = 20.dp
    val SectionSpacing = 24.dp
    val CardGap = 16.dp
    val CardRadius = 24.dp
    val InnerRadius = 18.dp
    val SmallRadius = 16.dp
    val PrimaryButtonHeight = 56.dp
    val SecondaryButtonHeight = 48.dp
    val BottomBarHeight = 84.dp
}

@Composable
fun PlayfulCard(
    modifier: Modifier = Modifier,
    background: Color? = null,
    borderColor: Color = SketchBorder,
    shadowElevation: Dp = 0.dp,
    innerPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val containerColor = background ?: if (theme == AppTheme.COMIC) ComicTeal.copy(alpha = 0.15f) else SketchSurface
    val borderSize = if (theme == AppTheme.COMIC) 2.dp else 1.2.dp

    Surface(
        modifier = modifier.shadow(shadowElevation, RoundedCornerShape(UiTokens.CardRadius)),
        shape = RoundedCornerShape(UiTokens.CardRadius),
        color = containerColor,
        border = BorderStroke(borderSize, borderColor)
    ) {
        Box(
            modifier = Modifier
                .padding(6.dp)
                .border(borderSize, borderColor.copy(alpha = 0.5f), RoundedCornerShape(UiTokens.InnerRadius))
                .padding(innerPadding)
        ) {
            content()
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, accent: Color? = null) {
    val theme = LocalAppTheme.current
    val actualAccent = accent ?: if (theme == AppTheme.COMIC) ComicPink else SketchTeal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (theme == AppTheme.COMIC) ComicText else SketchBorder
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (theme == AppTheme.COMIC) ComicMuted else SketchMuted
                )
            }
        }
        Box(
            Modifier
                .size(10.dp)
                .background(actualAccent, CircleShape)
                .border(1.2.dp, SketchBorder, CircleShape)
        )
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    color: Color? = null
) {
    val theme = LocalAppTheme.current
    val actualColor = color ?: if (theme == AppTheme.COMIC) ComicPink else SketchTeal
    val borderSize = 1.5.dp
    val borderColor = if (theme == AppTheme.COMIC) ComicBorder else SketchBorder

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(UiTokens.PrimaryButtonHeight),
        shape = RoundedCornerShape(UiTokens.SmallRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = actualColor,
            contentColor = Color.White,
            disabledContainerColor = SketchMuted.copy(alpha = 0.12f),
            disabledContentColor = SketchMuted
        ),
        border = BorderStroke(borderSize, borderColor),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun StatusPill(text: String, state: StatusTone) {
    val bg = when (state) {
        StatusTone.GOOD -> SketchTeal.copy(alpha = 0.1f)
        StatusTone.INFO -> SketchTeal.copy(alpha = 0.08f)
        StatusTone.WARN -> SketchYellow.copy(alpha = 0.2f)
        StatusTone.ERROR -> SketchPink.copy(alpha = 0.1f)
    }
    val fg = when (state) {
        StatusTone.GOOD -> SketchTeal
        StatusTone.INFO -> SketchTeal
        StatusTone.WARN -> Color(0xFF8B6E00)
        StatusTone.ERROR -> SketchPink
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(UiTokens.SmallRadius),
        border = BorderStroke(1.2.dp, SketchBorder.copy(alpha = 0.3f)),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

enum class StatusTone { GOOD, INFO, WARN, ERROR }

@Composable
fun MainScreenColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val bgColor = MaterialTheme.colorScheme.background

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .then(
                if (theme == AppTheme.COMIC) {
                    Modifier.drawWithCache {
                        val gridSize = 30.dp.toPx()
                        val color = ComicBorder.copy(alpha = 0.05f)
                        val strokeWidth = 1.dp.toPx()
                        onDrawBehind {
                            for (x in 0..size.width.toInt() step gridSize.toInt()) {
                                drawLine(color, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = strokeWidth)
                            }
                            for (y in 0..size.height.toInt() step gridSize.toInt()) {
                                drawLine(color, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = strokeWidth)
                            }
                        }
                    }
                } else Modifier
            ),
        content = content
    )
}
