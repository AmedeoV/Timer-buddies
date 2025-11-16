package com.timerbuddies.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.timerbuddies.app.ui.components.CircularTimerDisplay
import com.timerbuddies.app.ui.components.TimeSelectionButton
import com.timerbuddies.app.ui.components.SavePresetButton
import com.timerbuddies.app.ui.components.SavePresetDialog
import com.timerbuddies.app.ui.components.PresetsManagementDialog
import com.timerbuddies.app.ui.components.formatTime
import com.timerbuddies.app.viewmodel.TimerViewModel
import kotlinx.coroutines.delay

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val timerState by viewModel.timerState.collectAsState()
    val savedImages by viewModel.savedImages.collectAsState()
    val timerPresets by viewModel.timerPresets.collectAsState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (timerState.totalSeconds == 0) {
            // Time selection screen
            TimeSelectionScreen(
                selectedImageUri = selectedImageUri,
                savedImages = savedImages,
                timerPresets = timerPresets,
                onImageSelected = { uri -> 
                    android.util.Log.d("TimerBuddies", "Image selected: $uri")
                    selectedImageUri = uri 
                },
                onSaveImage = { url -> viewModel.saveGeneratedImage(url) },
                onDeleteImage = { url -> viewModel.removeSavedImage(url) },
                onTimeSelected = { seconds ->
                    android.util.Log.d("TimerBuddies", "Time selected: $seconds seconds, imageUri: $selectedImageUri")
                    // If no image selected, pick a random kid-friendly drawable
                    val imageToUse = selectedImageUri ?: run {
                        val drawableId = getRandomKidFriendlyImageDrawable()
                        Uri.parse("android.resource://${context.packageName}/$drawableId")
                    }
                    android.util.Log.d("TimerBuddies", "Using image URI: $imageToUse")
                    viewModel.setTimeInSeconds(seconds, imageToUse)
                },
                onSavePreset = { name, seconds, imageUrl ->
                    viewModel.saveTimerPreset(name, seconds, imageUrl)
                },
                onDeletePreset = { presetId ->
                    viewModel.deleteTimerPreset(presetId)
                },
                onPresetSelected = { preset ->
                    selectedImageUri = preset.imageUrl?.let { Uri.parse(it) }
                    viewModel.setTimeInSeconds(preset.seconds, selectedImageUri)
                }
            )
        } else {
            // Timer running screen
            TimerRunningScreen(
                timerState = timerState,
                onStart = { viewModel.startTimer() },
                onPause = { viewModel.pauseTimer() },
                onReset = { viewModel.resetTimer() },
                onBack = { 
                    viewModel.backToSelection()
                    selectedImageUri = null
                },
                onSaveImage = { url -> 
                    viewModel.saveGeneratedImage(url)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSelectionScreen(
    selectedImageUri: Uri?,
    savedImages: List<String>,
    timerPresets: List<com.timerbuddies.app.viewmodel.TimerPreset>,
    onImageSelected: (Uri?) -> Unit,
    onSaveImage: (String) -> Unit,
    onDeleteImage: (String) -> Unit,
    onTimeSelected: (Int) -> Unit,
    onSavePreset: (String, Int, String?) -> Unit,
    onDeletePreset: (String) -> Unit,
    onPresetSelected: (com.timerbuddies.app.viewmodel.TimerPreset) -> Unit
) {
    // Preset times in seconds: 10s, 1min, 5min, 10min, 15min
    val presetTimes = listOf(
        10 to "10s",
        60 to "1 min",
        300 to "5 min",
        600 to "10 min",
        900 to "15 min"
    )
    
    // Rainbow colors for each button 🌈
    val rainbowColors = listOf(
        com.timerbuddies.app.ui.theme.RainbowRed,
        com.timerbuddies.app.ui.theme.RainbowOrange,
        com.timerbuddies.app.ui.theme.RainbowYellow,
        com.timerbuddies.app.ui.theme.RainbowGreen,
        com.timerbuddies.app.ui.theme.RainbowBlue
    )
    
    var showCustomDialog by remember { mutableStateOf(false) }
    var showAiImageDialog by remember { mutableStateOf(false) }
    var showSavedImagesDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showPresetsDialog by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⏰ Timer Buddies 🎮",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Image selection buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selectedImageUri != null) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        Color.Transparent
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("📷", fontSize = 24.sp)
                    Text(
                        text = if (selectedImageUri != null) "Selected ✓" else "Gallery",
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = { showAiImageDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("🤖", fontSize = 24.sp)
                    Text("AI Image", fontSize = 12.sp)
                }
            }

            if (savedImages.isNotEmpty()) {
                Button(
                    onClick = { showSavedImagesDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("💾", fontSize = 24.sp)
                        Text("Saved (${savedImages.size})", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Show saved images button if available
        if (savedImages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showSavedImagesDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("💾 My Saved Images (${savedImages.size})")
                }
            }
        }

        // Show presets button if available
        if (timerPresets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showPresetsDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("⭐ My Timer Presets (${timerPresets.size})")
                }
            }
        }

        Text(
            text = "How long do you need? 🚀",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            itemsIndexed(presetTimes) { index, (seconds, label) ->
                TimeSelectionButton(
                    seconds = seconds,
                    label = label,
                    onClick = { onTimeSelected(seconds) },
                    containerColor = rainbowColors[index]
                )
            }
            
            // Custom time button
            item {
                CustomTimeButton(
                    onClick = { showCustomDialog = true }
                )
            }

            // Save as preset button
            item {
                SavePresetButton(
                    onClick = { showSavePresetDialog = true }
                )
            }
        }
    }

    if (showSavePresetDialog) {
        SavePresetDialog(
            selectedImageUri = selectedImageUri,
            onDismiss = { showSavePresetDialog = false },
            onSave = { name, seconds, imageUrl ->
                showSavePresetDialog = false
                onSavePreset(name, seconds, imageUrl)
            }
        )
    }

    if (showPresetsDialog) {
        PresetsManagementDialog(
            presets = timerPresets,
            onDismiss = { showPresetsDialog = false },
            onPresetSelected = { preset ->
                showPresetsDialog = false
                onPresetSelected(preset)
            },
            onDeletePreset = onDeletePreset
        )
    }

    if (showCustomDialog) {
        CustomTimeDialog(
            onDismiss = { showCustomDialog = false },
            onTimeSelected = { minutes ->
                showCustomDialog = false
                onTimeSelected(minutes)
            }
        )
    }

    if (showAiImageDialog) {
        AiImageDialog(
            onDismiss = { showAiImageDialog = false },
            onImageGenerated = { uri ->
                android.util.Log.d("TimerBuddies", "onImageGenerated called with URI: $uri")
                showAiImageDialog = false
                onImageSelected(uri)
                android.util.Log.d("TimerBuddies", "selectedImageUri updated")
            },
            onSaveImage = { url ->
                onSaveImage(url)
            }
        )
    }

    if (showSavedImagesDialog) {
        SavedImagesDialog(
            savedImages = savedImages,
            onDismiss = { showSavedImagesDialog = false },
            onImageSelected = { url ->
                showSavedImagesDialog = false
                onImageSelected(Uri.parse(url))
            },
            onDeleteImage = onDeleteImage
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimeButton(
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "✨",
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Custom",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun CustomTimeDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (Int) -> Unit
) {
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Custom Timer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { 
                            minutes = it
                            showError = false
                        },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = showError,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = seconds,
                        onValueChange = { 
                            seconds = it
                            showError = false
                        },
                        label = { Text("Seconds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = showError,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (showError) {
                    Text(
                        text = "Please enter valid time (0-999 min, 0-59 sec)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val minutesValue = minutes.toIntOrNull() ?: 0
                            val secondsValue = seconds.toIntOrNull() ?: 0
                            
                            if ((minutesValue in 0..999 && secondsValue in 0..59) && 
                                (minutesValue > 0 || secondsValue > 0)) {
                                val totalSeconds = (minutesValue * 60) + secondsValue
                                onTimeSelected(totalSeconds)
                            } else {
                                showError = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start")
                    }
                }
            }
        }
    }
}

@Composable
fun TimerRunningScreen(
    timerState: com.timerbuddies.app.viewmodel.TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    onSaveImage: (String) -> Unit
) {
    val context = LocalContext.current
    var imageSaved by remember { mutableStateOf(false) }
    
    // Clock ticking sound using MP3 file
    val tickingPlayer = remember {
        android.media.MediaPlayer.create(context, com.timerbuddies.app.R.raw.clock_ticking).apply {
            isLooping = true  // Loop the ticking sound
            setVolume(0.7f, 0.7f)  // Set volume to 70%
        }
    }
    
    // Control ticking sound based on timer state
    LaunchedEffect(timerState.isRunning, timerState.isComplete) {
        if (timerState.isRunning && !timerState.isComplete) {
            // Start playing ticking sound when timer is running
            if (!tickingPlayer.isPlaying) {
                tickingPlayer.start()
            }
        } else {
            // Stop playing when timer is paused or complete
            if (tickingPlayer.isPlaying) {
                tickingPlayer.pause()
                tickingPlayer.seekTo(0)  // Reset to beginning
            }
        }
    }
    
    // Keep reference to cheering player to prevent garbage collection
    var cheeringPlayerRef by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    
    // Play celebration sound when complete
    LaunchedEffect(timerState.isComplete) {
        if (timerState.isComplete) {
            try {
                // Ensure ticking is fully stopped
                if (tickingPlayer.isPlaying) {
                    tickingPlayer.pause()
                    tickingPlayer.seekTo(0)
                }
                
                // Small delay to ensure ticking is stopped
                delay(100)
                
                // Play kids cheering sound from resources
                cheeringPlayerRef?.release()  // Release any previous player
                cheeringPlayerRef = android.media.MediaPlayer.create(
                    context,
                    com.timerbuddies.app.R.raw.kids_cheering
                )?.apply {
                    setOnCompletionListener { player ->
                        player.release()
                        cheeringPlayerRef = null
                    }
                    setOnErrorListener { player, what, extra ->
                        android.util.Log.e("TimerBuddies", "MediaPlayer error: what=$what, extra=$extra")
                        player.release()
                        cheeringPlayerRef = null
                        true
                    }
                    start()
                }
            } catch (e: Exception) {
                android.util.Log.e("TimerBuddies", "Error playing sound: ${e.message}")
            }
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            tickingPlayer.release()
            cheeringPlayerRef?.release()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Circular timer with image reveal
            CircularTimerDisplay(
                totalSeconds = timerState.totalSeconds,
                remainingSeconds = timerState.remainingSeconds,
                revealProgress = timerState.revealProgress,
                isComplete = timerState.isComplete,
                customImageUri = timerState.customImageUri
            )

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 32.dp)
            ) {
                if (!timerState.isComplete) {
                    if (!timerState.isRunning) {
                        Button(
                            onClick = onStart,
                            modifier = Modifier
                                .height(64.dp)
                                .width(140.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = if (timerState.remainingSeconds == timerState.totalSeconds) "Start" else "Resume",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = onPause,
                            modifier = Modifier
                                .height(64.dp)
                                .width(140.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(
                                text = "Pause",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Button(
                    onClick = onReset,
                    modifier = Modifier
                        .height(64.dp)
                        .width(140.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timerState.isComplete) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(
                        text = if (timerState.isComplete) "New Timer" else "Reset",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Save image button when complete
            if (timerState.isComplete && timerState.customImageUri != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        timerState.customImageUri?.toString()?.let { url ->
                            onSaveImage(url)
                            imageSaved = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (imageSaved) 
                            MaterialTheme.colorScheme.tertiary 
                        else 
                            MaterialTheme.colorScheme.secondary
                    ),
                    enabled = !imageSaved
                ) {
                    Text(
                        text = if (imageSaved) "💾 Image Saved!" else "💾 Save This Image",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Back button in top left corner
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "← Back",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AiImageDialog(
    onDismiss: () -> Unit,
    onImageGenerated: (Uri) -> Unit,
    onSaveImage: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedImageUrl by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var imageLoaded by remember { mutableStateOf(false) }
    var imageSaved by remember { mutableStateOf(false) }
    
    // List of keywords that suggest non-kid-friendly content
    val inappropriateKeywords = listOf(
        "scary", "horror", "violent", "blood", "gore", "weapon", "gun", "knife",
        "zombie", "ghost", "demon", "devil", "evil", "dark", "creepy", "spooky",
        "monster", "skull", "death", "kill", "fight", "war", "angry", "mean"
    )
    
    fun isPromptKidFriendly(text: String): Boolean {
        val lowerText = text.lowercase()
        return !inappropriateKeywords.any { keyword -> 
            lowerText.contains(keyword)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🤖 Generate AI Image",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (generatedImageUrl != null) {
                    val context = LocalContext.current
                    var imageValidated by remember { mutableStateOf(false) }
                    var validationFailed by remember { mutableStateOf(false) }
                    
                    // Validate that the image is actually accessible
                    LaunchedEffect(generatedImageUrl) {
                        imageLoaded = false
                        imageValidated = false
                        validationFailed = false
                        
                        kotlinx.coroutines.delay(3000) // Give AI time to generate
                        
                        // Try to load the image to validate it's ready
                        try {
                            val loader = coil.ImageLoader(context)
                            val request = coil.request.ImageRequest.Builder(context)
                                .data(generatedImageUrl)
                                .build()
                            val result = loader.execute(request)
                            
                            if (result is coil.request.SuccessResult) {
                                android.util.Log.d("TimerBuddies", "Image validated successfully")
                                imageValidated = true
                                imageLoaded = true
                            } else {
                                android.util.Log.e("TimerBuddies", "Image validation failed")
                                validationFailed = true
                                imageLoaded = true
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TimerBuddies", "Image validation error: ${e.message}")
                            validationFailed = true
                            imageLoaded = true
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            !imageValidated && !validationFailed -> {
                                // Still validating
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "AI is creating your image...",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "This takes a few seconds ✨",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            imageValidated -> {
                                // Image is ready - show the actual preview!
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Show the actual image
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(generatedImageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Generated Image Preview",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "✨ Ready to Use! ✨",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            validationFailed -> {
                                // Validation failed but still allow using it
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = "⚠️",
                                        fontSize = 48.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Image Generated",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Preview not available, but you can still use it!",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (imageLoaded) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Save button
                            Button(
                                onClick = { 
                                    generatedImageUrl?.let { url ->
                                        onSaveImage(url)
                                        imageSaved = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !imageSaved,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (imageSaved) 
                                        MaterialTheme.colorScheme.surfaceVariant 
                                    else 
                                        MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Text(if (imageSaved) "💾 Saved!" else "💾 Save for Later")
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { 
                                        generatedImageUrl = null
                                        prompt = ""
                                        imageLoaded = false
                                        imageSaved = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Try Again")
                                }

                                Button(
                                    onClick = { 
                                        generatedImageUrl?.let { url ->
                                            android.util.Log.d("TimerBuddies", "Using image URL: $url")
                                            val uri = Uri.parse(url)
                                            android.util.Log.d("TimerBuddies", "Parsed URI: $uri")
                                            onImageGenerated(uri)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Use This!")
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Loading preview... ⏳",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { 
                            prompt = it
                            showError = false
                            errorMessage = ""
                        },
                        label = { Text("Describe your reward image") },
                        placeholder = { Text("happy puppy, rainbow unicorn, smiling dinosaur...") },
                        singleLine = false,
                        maxLines = 3,
                        isError = showError,
                        supportingText = {
                            if (showError) {
                                Text(
                                    text = errorMessage.ifEmpty { "Please describe what you want to see!" },
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text("All images are kid-friendly! 🎨")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGenerating
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Creating your image... ✨",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    when {
                                        prompt.isBlank() -> {
                                            showError = true
                                            errorMessage = "Please describe what you want to see!"
                                        }
                                        !isPromptKidFriendly(prompt) -> {
                                            showError = true
                                            errorMessage = "⚠️ That doesn't sound kid-friendly! Try something fun and happy instead! 😊"
                                        }
                                        else -> {
                                            showError = false
                                            errorMessage = ""
                                            isGenerating = true
                                            imageLoaded = false  // Reset image loaded state
                                            // Generate Pollinations.ai URL with kid-friendly filters
                                            // Keep it simple - shorter prompts load faster
                                            val safePrompt = "cute cartoon $prompt"
                                            val encodedPrompt = java.net.URLEncoder.encode(safePrompt, "UTF-8")
                                            // Pollinations.ai simple URL format
                                            val url = "https://image.pollinations.ai/prompt/$encodedPrompt"
                                            android.util.Log.d("TimerBuddies", "Generated URL: $url")
                                            generatedImageUrl = url
                                            isGenerating = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Generate!")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedImagesDialog(
    savedImages: List<String>,
    onDismiss: () -> Unit,
    onImageSelected: (String) -> Unit,
    onDeleteImage: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "💾 Saved Images",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (savedImages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved images yet!\nGenerate and save AI images here.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(savedImages.size) { index ->
                            val imageUrl = savedImages[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(imageUrl),
                                        contentDescription = "Saved Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    // Select button
                                    Button(
                                        onClick = { onImageSelected(imageUrl) },
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("Use", fontSize = 12.sp)
                                    }
                                    
                                    // Delete button
                                    IconButton(
                                        onClick = { onDeleteImage(imageUrl) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(32.dp)
                                    ) {
                                        Text("❌", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePresetButton(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().height(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "", fontSize = 32.sp)
            Text(text = "Save Preset", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer, textAlign = TextAlign.Center)
        }
    }
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0 && secs > 0) "${mins}m ${secs}s" else if (mins > 0) "${mins}m" else "${secs}s"
}

fun getRandomKidFriendlyImageDrawable(): Int {
    // Kid-friendly owl images embedded in the app
    // These load instantly with no network delay
    val kidFriendlyDrawables = listOf(
        com.timerbuddies.app.R.drawable.reward_owl_1,
        com.timerbuddies.app.R.drawable.reward_owl_2,
        com.timerbuddies.app.R.drawable.reward_owl_3,
        com.timerbuddies.app.R.drawable.reward_owl_4,
        com.timerbuddies.app.R.drawable.reward_owl_5,
        com.timerbuddies.app.R.drawable.reward_owl_6,
        com.timerbuddies.app.R.drawable.reward_owl_7,
        com.timerbuddies.app.R.drawable.reward_owl_8,
        com.timerbuddies.app.R.drawable.reward_owl_9,
        com.timerbuddies.app.R.drawable.reward_owl_10
    )
    
    return kidFriendlyDrawables.random()
}
