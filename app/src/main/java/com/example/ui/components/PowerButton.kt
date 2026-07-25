package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.Slate700
import com.example.ui.theme.TelegramBlue

@Composable
fun PowerButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRunning) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isRunning) 0.0f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val activeColor = NeonGreen
    val inactiveColor = Slate700
    val buttonGlow = if (isRunning) activeColor else TelegramBlue

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(170.dp)
                .testTag("power_button")
        ) {
            // Pulse outer ring when running
            if (isRunning) {
                Canvas(modifier = Modifier.size(170.dp)) {
                    drawCircle(
                        color = activeColor.copy(alpha = pulseAlpha),
                        radius = (size.minDimension / 2) * pulseScale,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }

            // Outer decorative ring
            Canvas(modifier = Modifier.size(140.dp)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = if (isRunning) listOf(NeonGreen, CyanPrimary, NeonGreen) else listOf(Slate700, Slate700)
                    ),
                    style = Stroke(width = 6.dp.toPx())
                )
            }

            // Central Power Button Body
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (isRunning) {
                            Brush.radialGradient(
                                colors = listOf(activeColor.copy(alpha = 0.3f), Color(0xFF0F172A))
                            )
                        } else {
                            Brush.radialGradient(
                                colors = listOf(Slate700, Color(0xFF0F172A))
                            )
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = if (isRunning) "Stop Proxy" else "Start Proxy",
                    tint = if (isRunning) activeColor else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isRunning) "PROXY CONNECTED" else "TAP TO CONNECT",
            color = if (isRunning) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.2.sp
        )
    }
}
