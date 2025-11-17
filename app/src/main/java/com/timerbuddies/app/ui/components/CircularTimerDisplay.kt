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
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Fullscreen background image
        if (customImageUri != null) {
            android.util.Log.d("TimerBuddies", "CircularTimerDisplay - customImageUri: $customImageUri")
            AsyncImage(
                model = customImageUri,
                contentDescription = "Reward Image",
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation),
                contentScale = ContentScale.Crop,
                onError = { error ->
                    android.util.Log.e("TimerBuddies", "Image load error: ${error.result.throwable.message}")
                },
                onSuccess = {
                    android.util.Log.d("TimerBuddies", "Image loaded successfully")
                }
            )
        } else {
            // Gradient background if no image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFE0B2),
                                Color(0xFFFFCC80)
                            )
                        )
                    )
            )
        }

        
        // Overlay canvas for reveal effect and progress indicator
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2
            
            // Calculate circular reveal radius based on screen size
            val maxRadius = kotlin.math.sqrt((canvasWidth * canvasWidth + canvasHeight * canvasHeight).toDouble()).toFloat() / 2
            
            // Draw overlay that shrinks as time passes (revealing the image)
            if (!isComplete) {
                val overlayRadius = maxRadius * (1f - revealProgress)
                drawCircle(
                    color = Color.White,
                    radius = overlayRadius,
                    center = Offset(centerX, centerY)
                )
            }
        }

        // Time display or completion message - centered on screen
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
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp)
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text(
                        text = "🎉",
                        fontSize = 80.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Awesome!",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(4f, 4f),
                                blurRadius = 8f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⭐",
                        fontSize = 64.sp
                    )
                }
            }
        } else {
            // Timer countdown display - centered on screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp)
                    .wrapContentSize(Alignment.Center)
            ) {
                // Background box for better contrast
                Box(
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = if (remainingSeconds < 60) {
                            seconds.toString()
                        } else {
                            String.format("%02d:%02d", minutes, seconds)
                        },
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.9f),
                                offset = Offset(6f, 6f),
                                blurRadius = 12f
                            )
                        )
                    )
                }
            }
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
