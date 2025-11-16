package com.timerbuddies.app.ui.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun CircularTimerDisplay(
    totalSeconds: Int,
    remainingSeconds: Int,
    revealProgress: Float,
    isComplete: Boolean,
    customImageUri: Uri? = null
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    
    // Confetti animation
    val confettiPieces = remember { List(50) { ConfettiPiece() } }
    var animationStarted by remember { mutableStateOf(false) }
    
    // Start confetti animation when complete
    LaunchedEffect(isComplete) {
        if (isComplete && !animationStarted) {
            animationStarted = true
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val confettiProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti"
    )
    
    // Image rotation animation when complete
    val rotation by animateFloatAsState(
        targetValue = if (isComplete) 360f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(320.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Show custom image if provided, otherwise show gradient
        if (customImageUri != null) {
            android.util.Log.d("TimerBuddies", "CircularTimerDisplay - customImageUri: $customImageUri")
            AsyncImage(
                model = customImageUri,
                contentDescription = "Reward Image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .rotate(rotation),
                contentScale = ContentScale.Crop,
                onError = { error ->
                    android.util.Log.e("TimerBuddies", "Image load error: ${error.result.throwable.message}")
                },
                onSuccess = {
                    android.util.Log.d("TimerBuddies", "Image loaded successfully")
                }
            )
        }
        
        // Background with revealed image effect
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Draw the reward image area (gradually revealed) - only show gradient if no custom image
            val revealPath = Path().apply {
                addOval(Rect(0f, 0f, canvasWidth, canvasHeight))
            }

            if (customImageUri == null) {
                clipPath(revealPath) {
                    // Background gradient - Fun Rainbow Theme
                    val gradientColors = if (isComplete) {
                        listOf(
                            Color(0xFFFFBE0B), // Sunny Yellow
                            Color(0xFFFF6B35), // Bright Orange
                            Color(0xFFFF006E), // Bubblegum Pink
                            Color(0xFF8338EC), // Playful Purple
                            Color(0xFF06D6A0)  // Fun Green
                        )
                    } else {
                        listOf(
                            Color(0xFF3A86FF), // Happy Blue
                            Color(0xFF8338EC), // Playful Purple
                            Color(0xFFFF006E), // Bubblegum Pink
                        Color(0xFF06D6A0)  // Fun Green
                    )
                }

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = gradientColors,
                            center = Offset(canvasWidth / 2, canvasHeight / 2),
                            radius = canvasWidth / 2 * (0.3f + revealProgress * 0.7f)
                        )
                    )

                    // Add colorful sparkle effect when revealing
                    if (revealProgress > 0.3f) {
                        repeat(8) { index ->
                            val angle = (index * 45f) * (Math.PI / 180f)
                            val distance = canvasWidth * 0.35f * revealProgress
                            val x = (canvasWidth / 2) + (distance * cos(angle)).toFloat()
                            val y = (canvasHeight / 2) + (distance * sin(angle)).toFloat()
                            val starSize = 20f * revealProgress

                            // Colorful sparkles rotating through rainbow
                            val sparkleColor = when (index % 4) {
                                0 -> Color(0xFFFFBE0B) // Yellow
                                1 -> Color(0xFFFF6B35) // Orange
                                2 -> Color(0xFFFF006E) // Pink
                                else -> Color(0xFF06D6A0) // Green
                            }
                            
                            drawCircle(
                                color = sparkleColor.copy(alpha = revealProgress * 0.8f),
                                radius = starSize,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }

            // Draw the overlay circle that "hides" the image (shrinks as time passes)
            if (!isComplete) {
                val overlayRadius = canvasWidth / 2 * (1f - revealProgress)
                drawCircle(
                    color = Color.White,
                    radius = overlayRadius,
                    center = Offset(canvasWidth / 2, canvasHeight / 2)
                )
            }

            // Draw the circular progress indicator - Rainbow gradient! 🌈
            if (!isComplete) {
                val strokeWidth = 12f
                val sweepAngle = 360f * (remainingSeconds.toFloat() / totalSeconds.toFloat())

                // Create rainbow gradient brush
                val rainbowBrush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFF4747), // Red
                        Color(0xFFFF9500), // Orange
                        Color(0xFFFFD600), // Yellow
                        Color(0xFF00E676), // Green
                        Color(0xFF2979FF), // Blue
                        Color(0xFF536DFE), // Indigo
                        Color(0xFF9C27B0), // Violet
                        Color(0xFFFF4747)  // Back to red
                    )
                )

                drawArc(
                    brush = rainbowBrush,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(canvasWidth - strokeWidth, canvasHeight - strokeWidth),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                )
            }
        }

        // Time display or completion message
        if (isComplete) {
            // Auto-hide text after 3 seconds
            var showText by remember { mutableStateOf(true) }
            
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000) // Show for 3 seconds
                showText = false
            }
            
            if (showText) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎉",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Awesome!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "⭐",
                        fontSize = 40.sp
                    )
                }
            }
        } else {
            Text(
                text = if (remainingSeconds < 60) {
                    seconds.toString()
                } else {
                    String.format("%02d:%02d", minutes, seconds)
                },
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Draw confetti when complete
        if (isComplete && animationStarted) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                confettiPieces.forEach { piece ->
                    val y = (size.height * confettiProgress + piece.startY) % size.height
                    val x = piece.startX + sin(confettiProgress * 6.28f * piece.frequency) * piece.amplitude
                    
                    drawCircle(
                        color = piece.color,
                        radius = piece.size,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

data class ConfettiPiece(
    val startX: Float = Random.nextFloat() * 320 * 3, // density pixels
    val startY: Float = Random.nextFloat() * -200f,
    val size: Float = Random.nextFloat() * 8f + 4f,
    val color: Color = listOf(
        Color(0xFFFF4747), // Rainbow Red
        Color(0xFFFF9500), // Rainbow Orange
        Color(0xFFFFD600), // Rainbow Yellow
        Color(0xFF00E676), // Rainbow Green
        Color(0xFF2979FF), // Rainbow Blue
        Color(0xFF536DFE), // Rainbow Indigo
        Color(0xFF9C27B0), // Rainbow Violet
        Color(0xFFFF4081)  // Rainbow Pink
    ).random(),
    val amplitude: Float = Random.nextFloat() * 50f + 20f,
    val frequency: Float = Random.nextFloat() * 2f + 1f
)
