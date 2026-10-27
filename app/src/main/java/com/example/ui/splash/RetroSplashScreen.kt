package com.example.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaDark
import com.example.ui.theme.RetroCream
import com.example.ui.theme.RetroGold
import com.example.ui.theme.RetroRuby
import kotlinx.coroutines.delay

@Composable
fun RetroSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var countdown by remember { mutableIntStateOf(3) }

    // Film leader clock sweep animation (360 degrees per second)
    val sweepTransition = rememberInfiniteTransition(label = "countdown_sweep")
    val sweepAngle by sweepTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    // Projector flicker transition
    val flickerTransition = rememberInfiniteTransition(label = "flicker")
    val flickerAlpha by flickerTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flickerAlpha"
    )

    // Countdown sequence: 3 -> 2 -> 1 -> Finish
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Vintage Film Reel Countdown Target
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2 - 8.dp.toPx()

                    // Outer & Inner Target Circles
                    drawCircle(
                        color = RetroGold.copy(alpha = 0.35f * flickerAlpha),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawCircle(
                        color = RetroGold.copy(alpha = 0.5f * flickerAlpha),
                        radius = radius * 0.7f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Crosshairs
                    drawLine(
                        color = RetroGold.copy(alpha = 0.4f * flickerAlpha),
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = RetroGold.copy(alpha = 0.4f * flickerAlpha),
                        start = Offset(0f, center.y),
                        end = Offset(size.width, center.y),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Sweeping clock hand
                    drawArc(
                        color = RetroRuby.copy(alpha = 0.35f),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius * 0.7f, center.y - radius * 0.7f),
                        size = androidx.compose.ui.geometry.Size(radius * 1.4f, radius * 1.4f)
                    )
                }

                // Center Countdown Number
                Text(
                    text = if (countdown > 0) countdown.toString() else "★",
                    color = RetroCream,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Marquee Brand Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = RetroRuby,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "RETROTOON",
                    color = RetroGold,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "TV",
                    color = RetroRuby,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "1930s - 1950s PUBLIC DOMAIN THEATER",
                color = RetroGold.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Direct streaming from Internet Archive mirrors",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Skip / Enter Button
            Button(
                onClick = onFinished,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RetroGold,
                    contentColor = CinemaBlack
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("splash_enter_button")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enter Theater", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
