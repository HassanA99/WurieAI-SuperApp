package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RideBookingScreen(onBack: () -> Unit) {
    var pickup by remember { mutableStateOf("Lumley Roundabout") }
    var dropoff by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var driverFound by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandPurple)
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
                text = "Request a Ride",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Map Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = BotBubbleGreen, modifier = Modifier.size(48.dp))
        }

        // Booking Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(BrandPurpleLight)
                .padding(24.dp)
        ) {
            if (isSearching) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(color = BotBubbleGreen)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Finding nearby Keke drivers...", color = Color.White)
                }
            } else if (driverFound) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Driver Found!", color = BotBubbleGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Alusine (Yellow Bajaj) is 3 minutes away.", color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { driverFound = false },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel Ride", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                OutlinedTextField(
                    value = pickup,
                    onValueChange = { pickup = it },
                    leadingIcon = { Icon(Icons.Filled.MyLocation, contentDescription = null, tint = BotBubbleGreen) },
                    placeholder = { Text("Pickup Location", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = BotBubbleGreen, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = dropoff,
                    onValueChange = { dropoff = it },
                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.Red.copy(alpha = 0.8f)) },
                    placeholder = { Text("Where to?", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = BotBubbleGreen, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimated Fare:", color = Color.White.copy(alpha = 0.7f))
                    Text("SLE 15.00", color = BotBubbleGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        isSearching = true
                        scope.launch {
                            delay(3000)
                            isSearching = false
                            driverFound = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BotBubbleGreen),
                    shape = RoundedCornerShape(16.dp),
                    enabled = pickup.isNotBlank() && dropoff.isNotBlank()
                ) {
                    Text("Request Keke", color = BrandPurple, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
