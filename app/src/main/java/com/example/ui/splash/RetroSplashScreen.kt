package com.example.ui.splash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaBorder
import com.example.ui.theme.CinemaDark
import com.example.ui.theme.RetroCream
import com.example.ui.theme.RetroGold
import com.example.ui.theme.RetroRuby
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RetroSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var countdown by remember { mutableIntStateOf(3) }

    // Film leader clock sweep animation (360 degrees per second)
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    // Projector flicker & bulb chase transition
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(90, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flickerAlpha"
    )

    // Scrolling 35mm film sprocket offset
    val sprocketPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sprocketPhase"
    )

    // Marquee light pulse
    val marqueePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "marqueePulse"
    )

    // Countdown sequence: 3 -> 2 -> 1 -> ★ -> Finish
    LaunchedEffect(Unit) {
        delay(700)
        countdown = 2
        delay(700)
        countdown = 1
        delay(700)
        countdown = 0
        delay(400)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Tap anywhere to skip straight to catalog
                onFinished()
            }
            .testTag("retro_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Warm projector radial spotlight + vintage vertical film scratches
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        RetroGold.copy(alpha = 0.14f * flickerAlpha),
                        RetroRuby.copy(alpha = 0.05f * flickerAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.72f
                )
            )

            // Subtle vintage film scratches
            val scratchX1 = size.width * (0.28f + 0.02f * sin(sweepAngle * 0.05f))
            val scratchX2 = size.width * (0.73f + 0.015f * cos(sweepAngle * 0.04f))
            drawLine(
                color = RetroCream.copy(alpha = 0.06f * flickerAlpha),
                start = Offset(scratchX1, 0f),
                end = Offset(scratchX1, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = RetroGold.copy(alpha = 0.05f * flickerAlpha),
                start = Offset(scratchX2, 0f),
                end = Offset(scratchX2, size.height),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // Left & Right 35mm Film Strip Sprocket Rails
        FilmSprocketRail(
            phase = sprocketPhase,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(28.dp)
        )
        FilmSprocketRail(
            phase = sprocketPhase,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(28.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 24.dp)
        ) {
            // Top Theater Presenting Pill
            Surface(
                color = CinemaDark.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RetroGold.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = RetroRuby.copy(alpha = 0.6f + 0.4f * marqueePulse),
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = "35MM ARCHIVAL PROJECTION • SOUND ON FILM",
                        color = RetroCream.copy(alpha = 0.85f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Vintage SMPTE Film Reel Countdown Target
            Box(
                modifier = Modifier.size(196.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f - 8.dp.toPx()

                    // Soft sepia frame backing inside leader circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2B2218).copy(alpha = 0.7f * flickerAlpha),
                                CinemaDark.copy(alpha = 0.9f)
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )

                    // Sweeping radar/clock sector
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                RetroRuby.copy(alpha = 0.22f),
                                RetroGold.copy(alpha = 0.48f * flickerAlpha)
                            ),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f)
                    )

                    // Degree tick marks around outer circumference
                    for (deg in 0 until 360 step 15) {
                        val rad = Math.toRadians(deg.toDouble())
                        val isMajor = deg % 45 == 0
                        val innerTickRadius = radius * (if (isMajor) 0.86f else 0.92f)
                        val start = Offset(
                            x = center.x + (innerTickRadius * cos(rad)).toFloat(),
                            y = center.y + (innerTickRadius * sin(rad)).toFloat()
                        )
                        val end = Offset(
                            x = center.x + (radius * cos(rad)).toFloat(),
                            y = center.y + (radius * sin(rad)).toFloat()
                        )
                        drawLine(
                            color = RetroGold.copy(alpha = (if (isMajor) 0.7f else 0.35f) * flickerAlpha),
                            start = start,
                            end = end,
                            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Outer & Inner SMPTE Target Rings
                    drawCircle(
                        color = RetroGold.copy(alpha = 0.75f * flickerAlpha),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3.5.dp.toPx())
                    )
                    drawCircle(
                        color = RetroCream.copy(alpha = 0.55f * flickerAlpha),
                        radius = radius * 0.72f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = RetroRuby.copy(alpha = 0.45f * flickerAlpha),
                        radius = radius * 0.42f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Crosshairs
                    drawLine(
                        color = RetroGold.copy(alpha = 0.45f * flickerAlpha),
                        start = Offset(center.x, center.y - radius),
                        end = Offset(center.x, center.y + radius),
                        strokeWidth = 1.8.dp.toPx()
                    )
                    drawLine(
                        color = RetroGold.copy(alpha = 0.45f * flickerAlpha),
                        start = Offset(center.x - radius, center.y),
                        end = Offset(center.x + radius, center.y),
                        strokeWidth = 1.8.dp.toPx()
                    )

                    // Leading sweep arm needle
                    val needleRad = Math.toRadians((sweepAngle - 90f).toDouble())
                    drawLine(
                        color = RetroGold,
                        start = center,
                        end = Offset(
                            x = center.x + (radius * cos(needleRad)).toFloat(),
                            y = center.y + (radius * sin(needleRad)).toFloat()
                        ),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Animated Center Countdown Number
                AnimatedContent(
                    targetState = countdown,
                    transitionSpec = {
                        (scaleIn(
                            initialScale = 0.5f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeIn(tween(180))) togetherWith
                            (scaleOut(targetScale = 1.35f, animationSpec = tween(180)) + fadeOut(tween(150)))
                    },
                    label = "countdown_digit"
                ) { currentCount ->
                    Text(
                        text = if (currentCount > 0) currentCount.toString() else "★",
                        color = if (currentCount > 0) RetroCream else RetroGold,
                        fontSize = 68.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Golden Age Marquee Brand Frame
            Surface(
                color = CinemaDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, RetroGold.copy(alpha = 0.65f)),
                tonalElevation = 8.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = RetroRuby,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RetroGold.copy(alpha = 0.8f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = RetroCream,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "RETROTOON",
                            color = RetroGold,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = RetroRuby,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "TV",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "1930s – 1950s PUBLIC DOMAIN THEATER",
                        color = RetroGold.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Fleischer Superman • Popeye • Betty Boop • Felix the Cat",
                color = RetroCream.copy(alpha = 0.65f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Skip / Enter Button
            Button(
                onClick = onFinished,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RetroGold,
                    contentColor = CinemaBlack
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(44.dp)
                    .focusable()
                    .testTag("splash_enter_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Enter Theater",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

@Composable
private fun FilmSprocketRail(
    phase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(CinemaDark.copy(alpha = 0.85f))
    ) {
        val holeSpacing = 34.dp.toPx()
        val holeWidth = 12.dp.toPx()
        val holeHeight = 16.dp.toPx()
        val offsetPx = phase * holeSpacing

        // Rail inner border line
        drawLine(
            color = CinemaBorder,
            start = Offset(0f, 0f),
            end = Offset(0f, size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = CinemaBorder,
            start = Offset(size.width, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx()
        )

        var y = -holeSpacing + offsetPx
        while (y < size.height + holeSpacing) {
            drawRoundRect(
                color = CinemaBlack,
                topLeft = Offset((size.width - holeWidth) / 2f, y),
                size = Size(holeWidth, holeHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            drawRoundRect(
                color = RetroGold.copy(alpha = 0.25f),
                topLeft = Offset((size.width - holeWidth) / 2f, y),
                size = Size(holeWidth, holeHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            y += holeSpacing
        }
    }
}
