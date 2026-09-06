package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BotBubbleGreen
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight

@Composable
fun ProviderRegistrationScreen(onBack: () -> Unit, onSubmitSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var serviceCategory by remember { mutableStateOf("") }
    var offeredServices by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandPurple)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BrandPurpleLight)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Become a Provider",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "Join the WurieAI Network",
                color = BotBubbleGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Register as a service provider, upload your ID for verification, and start getting hired directly through the app.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Photo Upload Placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(BrandPurpleLight)
                    .align(Alignment.CenterHorizontally)
                    .clickable { /* Trigger Photo Picker */ },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Upload Profile Photo", tint = Color.White)
                    Text("Add Photo", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ID Upload Section
            Text(
                text = "Identity Verification",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { /* Trigger ID Photo Picker */ },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Description, contentDescription = "Upload ID", tint = BotBubbleGreen, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap to upload National ID or Passport", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Service Details",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Form Fields
            RegistrationTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            RegistrationTextField(
                value = serviceCategory,
                onValueChange = { serviceCategory = it },
                label = "Primary Category (e.g., Plumber, Cleaner)",
                icon = { Icon(Icons.Filled.Build, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = offeredServices,
                onValueChange = { offeredServices = it },
                label = { Text("Specific Services (Comma separated)", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Filled.List, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BotBubbleGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                ),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))

            RegistrationTextField(
                value = location,
                onValueChange = { location = it },
                label = "Service Area / City",
                icon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) }
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { 
                    // Simulation of Firestore call:
                    // val db = Firebase.firestore
                    // db.collection("providers").add(hashMapOf(
                    //    "name" to name, "category" to serviceCategory, "status" to "pending_verification"
                    // ))
                    showSuccessDialog = true 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BotBubbleGreen),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank() && serviceCategory.isNotBlank() && location.isNotBlank()
            ) {
                Text("Submit for Verification", color = BrandPurple, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = BrandPurpleLight,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Success", tint = BotBubbleGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ID Submitted", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Your profile and ID have been securely submitted to Firestore for verification. Once approved by admins, you will appear in user searches for $serviceCategory in $location.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    showSuccessDialog = false
                    onSubmitSuccess()
                }) {
                    Text("Go to Dashboard", color = BotBubbleGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun RegistrationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: @Composable () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.6f)) },
        leadingIcon = icon,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BotBubbleGreen,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
        ),
        singleLine = true
    )
}
