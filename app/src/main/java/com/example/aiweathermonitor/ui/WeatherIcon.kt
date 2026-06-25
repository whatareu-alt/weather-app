package com.example.aiweathermonitor.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import androidx.annotation.RawRes
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.aiweathermonitor.R

/**
 * Maps a WMO weather code to a bundled meteocon Lottie animation (res/raw).
 * Clear and partly-cloudy conditions use night variants when [isDay] is false.
 */
@RawRes
fun getWeatherLottieRes(code: Int, isDay: Boolean = true): Int = when (code) {
    0 -> if (isDay) R.raw.clear_day else R.raw.clear_night
    1, 2, 3 -> if (isDay) R.raw.partly_cloudy_day else R.raw.partly_cloudy_night
    45, 48 -> R.raw.fog
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> R.raw.rain
    71, 73, 75 -> R.raw.snow
    95, 96, 99 -> R.raw.thunderstorms
    else -> R.raw.cloudy
}

@Composable
fun WeatherIcon(code: Int, modifier: Modifier = Modifier, isDay: Boolean = true) {
    val compositionResult = rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(getWeatherLottieRes(code, isDay))
    )
    val composition = compositionResult.value

    if (composition != null && !compositionResult.isFailure) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = modifier
        )
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "WeatherIconAnimation")

        // Sun Rotation Animation
        val sunRotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "SunRotation"
        )

        // Cloud Hovering Animation
        val cloudOffset by infiniteTransition.animateFloat(
            initialValue = -2f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "CloudOffset"
        )

        // Rain Drop Fall Animation
        val rainDropY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 24f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "RainDropY"
        )

        // Lightning Flash Animation
        val lightningAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutLinearInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "LightningAlpha"
        )

        CanvasFallback(
            code = code,
            modifier = modifier,
            sunRotationAngle = sunRotationAngle,
            cloudOffset = cloudOffset,
            rainDropY = rainDropY,
            lightningAlpha = lightningAlpha
        )
    }
}

@Composable
fun CanvasFallback(
    code: Int,
    modifier: Modifier = Modifier,
    sunRotationAngle: Float,
    cloudOffset: Float,
    rainDropY: Float,
    lightningAlpha: Float
) {
    Canvas(modifier = modifier.size(48.dp)) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        when (code) {
            // 0: Clear Sky (Slowly rotating golden sun with soft outer glow)
            0 -> {
                // Outer warm glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFBBF24).copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = w * 0.45f
                    ),
                    radius = w * 0.45f
                )
                // Main sun body
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B)),
                        center = center,
                        radius = w * 0.25f
                    ),
                    radius = w * 0.25f
                )
                // Sun rays
                val numRays = 8
                val rayLength = w * 0.12f
                val innerRadius = w * 0.29f
                for (i in 0 until numRays) {
                    val angleRad = Math.toRadians((sunRotationAngle + (i * (360f / numRays))).toDouble())
                    val startX = center.x + innerRadius * cos(angleRad).toFloat()
                    val startY = center.y + innerRadius * sin(angleRad).toFloat()
                    val endX = center.x + (innerRadius + rayLength) * cos(angleRad).toFloat()
                    val endY = center.y + (innerRadius + rayLength) * sin(angleRad).toFloat()
                    drawLine(
                        color = Color(0xFFF59E0B),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // 1, 2, 3: Partly Cloudy (Sun peeking behind cloud)
            1, 2, 3 -> {
                // Sun in top-right background
                val sunCenter = Offset(w * 0.65f, h * 0.35f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B)),
                        center = sunCenter,
                        radius = w * 0.18f
                    ),
                    center = sunCenter,
                    radius = w * 0.18f
                )

                // Overlapping Cloud
                val cloudPath = Path().apply {
                    val baseLine = h * 0.72f + cloudOffset
                    moveTo(w * 0.2f, baseLine)
                    // Bottom curve
                    lineTo(w * 0.8f, baseLine)
                    // Right curve
                    cubicTo(w * 0.95f, baseLine, w * 0.95f, baseLine - h * 0.25f, w * 0.78f, baseLine - h * 0.22f)
                    // Top-right curve
                    cubicTo(w * 0.7f, baseLine - h * 0.48f, w * 0.45f, baseLine - h * 0.48f, w * 0.4f, baseLine - h * 0.28f)
                    // Left curve
                    cubicTo(w * 0.28f, baseLine - h * 0.32f, w * 0.1f, baseLine - h * 0.22f, w * 0.2f, baseLine)
                    close()
                }
                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFF1F5F9))
                    )
                )
            }

            // 45, 48: Fog / Rime Fog (Soft overlapping slate clouds with horizontal mist lines)
            45, 48 -> {
                // Cloud outline
                val cloudPath = Path().apply {
                    val baseLine = h * 0.62f + cloudOffset
                    moveTo(w * 0.25f, baseLine)
                    lineTo(w * 0.75f, baseLine)
                    cubicTo(w * 0.9f, baseLine, w * 0.9f, baseLine - h * 0.22f, w * 0.75f, baseLine - h * 0.2f)
                    cubicTo(w * 0.68f, baseLine - h * 0.42f, w * 0.45f, baseLine - h * 0.42f, w * 0.42f, baseLine - h * 0.25f)
                    cubicTo(w * 0.3f, baseLine - h * 0.28f, w * 0.15f, baseLine - h * 0.2f, w * 0.25f, baseLine)
                    close()
                }
                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
                    )
                )

                // Fog lines at the bottom
                val fogLines = listOf(
                    Pair(w * 0.2f, w * 0.8f) to h * 0.76f,
                    Pair(w * 0.3f, w * 0.7f) to h * 0.84f,
                    Pair(w * 0.4f, w * 0.6f) to h * 0.92f
                )
                fogLines.forEach { (range, yVal) ->
                    val (startX, endX) = range
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(startX, yVal),
                        end = Offset(endX, yVal),
                        strokeWidth = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // 51 to 65, 80 to 82: Rainy / Drizzle / Rain Showers (Soft blue cloud with falling animated raindrops)
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> {
                // Cloud
                val cloudPath = Path().apply {
                    val baseLine = h * 0.58f + cloudOffset
                    moveTo(w * 0.25f, baseLine)
                    lineTo(w * 0.75f, baseLine)
                    cubicTo(w * 0.9f, baseLine, w * 0.9f, baseLine - h * 0.22f, w * 0.75f, baseLine - h * 0.2f)
                    cubicTo(w * 0.68f, baseLine - h * 0.42f, w * 0.45f, baseLine - h * 0.42f, w * 0.42f, baseLine - h * 0.25f)
                    cubicTo(w * 0.3f, baseLine - h * 0.28f, w * 0.15f, baseLine - h * 0.2f, w * 0.25f, baseLine)
                    close()
                }
                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE2E8F0), Color(0xFF475569))
                    )
                )

                // 3 Falling Rain Drops
                val dropOffsetPx = rainDropY.dp.toPx()
                val dropWidth = 2.dp.toPx()
                val dropLength = 6.dp.toPx()
                val dropCols = listOf(w * 0.35f, w * 0.5f, w * 0.65f)
                dropCols.forEachIndexed { index, x ->
                    val individualY = h * 0.68f + ((dropOffsetPx + (index * (h * 0.12f))) % (h * 0.28f))
                    if (individualY < h * 0.95f) {
                        drawLine(
                            color = Color(0xFF60A5FA),
                            start = Offset(x, individualY),
                            end = Offset(x - 2.dp.toPx(), individualY + dropLength),
                            strokeWidth = dropWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }

            // 71, 73, 75: Snowy (Light frosty blue cloud with falling snowflakes)
            71, 73, 75 -> {
                // Cloud
                val cloudPath = Path().apply {
                    val baseLine = h * 0.58f + cloudOffset
                    moveTo(w * 0.25f, baseLine)
                    lineTo(w * 0.75f, baseLine)
                    cubicTo(w * 0.9f, baseLine, w * 0.9f, baseLine - h * 0.22f, w * 0.75f, baseLine - h * 0.2f)
                    cubicTo(w * 0.68f, baseLine - h * 0.42f, w * 0.45f, baseLine - h * 0.42f, w * 0.42f, baseLine - h * 0.25f)
                    cubicTo(w * 0.3f, baseLine - h * 0.28f, w * 0.15f, baseLine - h * 0.2f, w * 0.25f, baseLine)
                    close()
                }
                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8))
                    )
                )

                // Snow flakes
                val snowOffset = rainDropY.dp.toPx() * 0.7f
                val snowCols = listOf(w * 0.35f, w * 0.5f, w * 0.65f)
                snowCols.forEachIndexed { index, x ->
                    val y = h * 0.68f + ((snowOffset + (index * (h * 0.12f))) % (h * 0.28f))
                    if (y < h * 0.95f) {
                        drawCircle(
                            color = Color(0xFFE0F2FE),
                            radius = 2.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // 95, 96, 99: Thunderstorm (Dark stormy cloud with flashing bright yellow lightning)
            95, 96, 99 -> {
                // Cloud
                val cloudPath = Path().apply {
                    val baseLine = h * 0.56f + cloudOffset
                    moveTo(w * 0.25f, baseLine)
                    lineTo(w * 0.75f, baseLine)
                    cubicTo(w * 0.9f, baseLine, w * 0.9f, baseLine - h * 0.22f, w * 0.75f, baseLine - h * 0.2f)
                    cubicTo(w * 0.68f, baseLine - h * 0.42f, w * 0.45f, baseLine - h * 0.42f, w * 0.42f, baseLine - h * 0.25f)
                    cubicTo(w * 0.3f, baseLine - h * 0.28f, w * 0.15f, baseLine - h * 0.2f, w * 0.25f, baseLine)
                    close()
                }
                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF334155), Color(0xFF0F172A))
                    )
                )

                // Lightning bolt
                val boltPath = Path().apply {
                    moveTo(w * 0.52f, h * 0.58f)
                    lineTo(w * 0.43f, h * 0.76f)
                    lineTo(w * 0.53f, h * 0.76f)
                    lineTo(w * 0.45f, h * 0.96f)
                    lineTo(w * 0.59f, h * 0.72f)
                    lineTo(w * 0.49f, h * 0.72f)
                    close()
                }
                drawPath(
                    path = boltPath,
                    color = Color(0xFFFBBF24).copy(alpha = lightningAlpha)
                )
            }

            // Fallback (Partly cloudy layout)
            else -> {
                val cloudPath = Path().apply {
                    val baseLine = h * 0.65f + cloudOffset
                    moveTo(w * 0.25f, baseLine)
                    lineTo(w * 0.75f, baseLine)
                    cubicTo(w * 0.9f, baseLine, w * 0.9f, baseLine - h * 0.22f, w * 0.75f, baseLine - h * 0.2f)
                    cubicTo(w * 0.68f, baseLine - h * 0.42f, w * 0.45f, baseLine - h * 0.42f, w * 0.42f, baseLine - h * 0.25f)
                    cubicTo(w * 0.3f, baseLine - h * 0.28f, w * 0.15f, baseLine - h * 0.2f, w * 0.25f, baseLine)
                    close()
                }
                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFE2E8F0))
                    )
                )
            }
        }
    }
}
