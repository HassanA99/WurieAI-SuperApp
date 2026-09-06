package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.delay

@Composable
fun VoiceModeScreen(onClose: () -> Unit, chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory)) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("WurieAI is ready to help you") }
    
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = data?.get(0) ?: ""
            if (spokenText.isNotEmpty()) {
                recognizedText = "You said: \"$spokenText\"\n\nProcessing..."
                // Send the message to chatViewModel and go back to chat
                chatViewModel.sendMessage(spokenText)
                // Short delay so user can see it before closing
                onClose()
            }
        } else {
            recognizedText = "Tap the mic and speak"
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
            recognizedText = "Listening..."
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            }
            try {
                speechRecognizerLauncher.launch(intent)
            } catch (e: Exception) {
                isListening = false
                recognizedText = "Speech recognition not supported on this device"
            }
        } else {
            Toast.makeText(context, "Microphone permission required for Voice Mode", Toast.LENGTH_SHORT).show()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.5f else 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 800 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val opacity by infiniteTransition.animateFloat(
        initialValue = if (isListening) 0.6f else 0.3f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 800 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacity"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrandPurple, BrandPurpleDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Top Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isListening) "Listening..." else "Tap Mic to Speak",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = recognizedText,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(80.dp))

            // Pulsing Voice Circle
            Box(contentAlignment = Alignment.Center) {
                // Outer Pulse
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(VoiceModeGradientStart.copy(alpha = opacity))
                )
                
                // Inner Pulse
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale * 0.8f)
                        .clip(CircleShape)
                        .background(VoiceModeGradientEnd.copy(alpha = opacity * 1.5f))
                )

                // Central Mic Button
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(if (isListening) BotBubbleGreen else Color.White)
                        .clickable {
                            if (!isListening) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = BrandPurple,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
            
            // Subtitle hint
            Text(
                text = "Try saying:\n\"Book me a taxi to the airport\"",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
        }
        
        // Visual Waveform (Simulated)
        if (isListening) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(15) { index ->
                    val waveHeight by infiniteTransition.animateFloat(
                        initialValue = 10f,
                        targetValue = (30..80).random().toFloat(),
                        animationSpec = infiniteRepeatable(
                            animation = tween((300..700).random(), easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "wave_$index"
                    )
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(waveHeight.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BotBubbleGreen.copy(alpha = 0.8f))
                    )
                }
            }
        }
    }
}
