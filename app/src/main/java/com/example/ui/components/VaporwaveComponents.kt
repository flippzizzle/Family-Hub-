package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class Star(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val color: Color,
    val speedFactor: Float
)

@Composable
fun VaporwaveStarfieldBackground(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val stars = remember {
        val colors = listOf(
            Color(0xFF00F5FF),
            Color(0xFFFF007F),
            Color(0xFFB026FF),
            Color(0xFFFAF7FF),
            Color(0xFFFFE600)
        )
        List(70) {
            Star(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                radius = Random.nextFloat() * 2f + 1f,
                color = colors[Random.nextInt(colors.size)],
                speedFactor = Random.nextFloat() * 0.8f + 0.2f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "star_twinkle")
    val twinkle by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (enabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw retro perspective grid at bottom
                val gridBottom = height
                val horizonY = height * 0.75f
                val gridColor = Color(0x2200F5FF)

                // Horizontal grid lines
                var curY = horizonY
                var step = 8f
                while (curY < gridBottom) {
                    drawLine(
                        color = gridColor.copy(alpha = (curY - horizonY) / (gridBottom - horizonY) * 0.25f),
                        start = Offset(0f, curY),
                        end = Offset(width, curY),
                        strokeWidth = 1.2f
                    )
                    curY += step
                    step *= 1.35f
                }

                // Vanishing perspective lines
                val vp = Offset(width / 2f, horizonY)
                val lineCount = 12
                for (i in -lineCount..lineCount) {
                    val x = vp.x + i * (width / (lineCount * 1.5f))
                    drawLine(
                        color = gridColor.copy(alpha = 0.18f),
                        start = vp,
                        end = Offset(x, gridBottom),
                        strokeWidth = 1f
                    )
                }

                // Draw twinkling galaxy stars
                stars.forEachIndexed { index, star ->
                    val alpha = if (index % 2 == 0) twinkle * star.speedFactor else (1f - twinkle) * star.speedFactor
                    val x = star.xRatio * width
                    val y = star.yRatio * height
                    drawCircle(
                        color = star.color.copy(alpha = alpha.coerceIn(0.15f, 0.95f)),
                        radius = star.radius,
                        center = Offset(x, y)
                    )
                }
            }
        }

        content()
    }
}

@Composable
fun NeonGlowCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    borderColor: Color = MaterialTheme.colorScheme.primary,
    borderWidth: Dp = 1.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(shape)
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.85f),
                        borderColor.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
                    )
                ),
                shape = shape
            ),
        shape = shape,
        color = containerColor,
        content = content
    )
}

@Composable
fun VaporwaveHeaderBadge(
    text: String,
    modifier: Modifier = Modifier,
    glowColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(glowColor.copy(alpha = 0.15f))
            .border(1.dp, glowColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = glowColor
        )
    }
}
