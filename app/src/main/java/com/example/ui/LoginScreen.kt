package com.example.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleAccent
import com.example.viewmodel.AuthState
import com.example.viewmodel.AuthViewModel

fun authenticateWithBiometrics(context: Context, onSuccess: () -> Unit) {
    val fragmentActivity = context as? FragmentActivity
    if (fragmentActivity == null) {
        Toast.makeText(context, "Biometric authentication not supported here", Toast.LENGTH_SHORT).show()
        // Fallback for preview mode if needed
        onSuccess()
        return
    }

    val executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt = BiometricPrompt(fragmentActivity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(context, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                // Let the user fallback to guest/preview if they cancel
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(context, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric login for WurieAI")
        .setSubtitle("Log in using your biometric credential")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    biometricPrompt.authenticate(promptInfo)
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, authViewModel: AuthViewModel = viewModel()) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandPurple),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_applogo_transparent),
                    contentDescription = "WurieAI Logo",
                    modifier = Modifier.size(64.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Welcome to WurieAI",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Africa's First Agentic Super App.\nSign in to get started.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            when (authState) {
                is AuthState.Loading -> {
                    CircularProgressIndicator(color = Color.White)
                }
                is AuthState.Error -> {
                    val errorMessage = (authState as AuthState.Error).message
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    GoogleSignInButton(onClick = { authViewModel.signInWithGoogle(context) })
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { authenticateWithBiometrics(context, onLoginSuccess) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurpleAccent),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Unlock with Biometrics", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { authViewModel.previewBypass() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text("Continue in Preview Mode", color = Color.White)
                    }
                }
                else -> {
                    GoogleSignInButton(onClick = { authViewModel.signInWithGoogle(context) })
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { authenticateWithBiometrics(context, onLoginSuccess) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurpleAccent),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Unlock with Biometrics", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { authViewModel.previewBypass() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text("Continue as Guest (Preview)", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleSignInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google Logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Continue with Google",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F1F1F)
            )
        }
    }
}
