package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
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

data class PendingProvider(val id: String, val name: String, val category: String, val status: String)

@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    pendingProviders: List<PendingProvider> = listOf(
        PendingProvider("1", "Abu Koroma", "Mechanic", "Pending"),
        PendingProvider("2", "Fatu Turay", "Plumber", "Pending")
    ),
    onApprove: (String) -> Unit = {},
    onReject: (String) -> Unit = {}
) {

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
                text = "Admin: Verification",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(pendingProviders) { provider ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandPurpleLight)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(provider.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${provider.category} • ID Uploaded", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        }
                    }
                    Row {
                        IconButton(
                            onClick = { onReject(provider.id) },
                            modifier = Modifier.clip(CircleShape).background(Color(0xFFF44336).copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Reject", tint = Color(0xFFF44336))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { onApprove(provider.id) },
                            modifier = Modifier.clip(CircleShape).background(BotBubbleGreen.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = "Approve", tint = BotBubbleGreen)
                        }
                    }
                }
            }
            if (pendingProviders.isEmpty()) {
                item {
                    Text("No pending verifications.", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 32.dp))
                }
            }
        }
    }
}
