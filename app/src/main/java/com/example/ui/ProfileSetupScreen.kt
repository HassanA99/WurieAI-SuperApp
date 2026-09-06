package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleAccent
import com.example.ui.theme.BotBubbleGreen

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.AuthState

@Composable
fun ProfileSetupScreen(
    onComplete: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onComplete()
        }
    }

    Scaffold(
        containerColor = BrandPurple
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = BotBubbleGreen)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Personalize Your Profile",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Avatar Placeholder
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                    }
                    
                    if (authState is AuthState.Error) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    AuthTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = "First Name",
                        icon = Icons.Default.Person
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AuthTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = "Last Name",
                        icon = Icons.Default.Person
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AuthTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Phone Number",
                        icon = Icons.Default.Phone
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AuthTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = "City (e.g. Freetown, Conakry)",
                        icon = Icons.Default.LocationOn
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = {
                            viewModel.completeProfile(firstName, lastName, phone, city)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BotBubbleGreen),
                        enabled = firstName.isNotEmpty() && lastName.isNotEmpty()
                    ) {
                        Text(
                            text = "Complete Setup",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
