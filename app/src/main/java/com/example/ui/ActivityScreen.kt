package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ActivityLogEntity
import com.example.ui.theme.*
import com.example.ui.util.LocalAppStrings
import com.example.viewmodel.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(viewModel: ActivityViewModel = viewModel(factory = ActivityViewModel.Factory)) {
    val strings = LocalAppStrings.current
    val dbActivities by viewModel.logs.collectAsState()

    // Combining real DB activities with some mock social activities to demonstrate the unified hub
    val mockSocialActivities = listOf(
        ActivityLogEntity(
            id = "s1",
            title = "Sarah Jenkins liked your post",
            description = "\"Just arrived in Bali...\"",
            timestamp = System.currentTimeMillis() - 1000 * 60 * 5, // 5 mins ago
            iconName = "favorite",
            colorHex = 0xFFE91E63.toLong()
        ),
        ActivityLogEntity(
            id = "s2",
            title = "David Chen commented",
            description = "\"Have a great trip!\"",
            timestamp = System.currentTimeMillis() - 1000 * 60 * 30, // 30 mins ago
            iconName = "comment",
            colorHex = 0xFF2196F3.toLong()
        ),
        ActivityLogEntity(
            id = "f1",
            title = "Wallet Top-Up Successful",
            description = "Added SLE 500.00 via Bank Transfer",
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2, // 2 hours ago
            iconName = "wallet",
            colorHex = 0xFF4CAF50.toLong()
        )
    )

    val activities = (dbActivities + mockSocialActivities).sortedByDescending { it.timestamp }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Task History",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            if (activities.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("Clear", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        if (activities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No recent activity.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp
                )
            }
        } else {
            // Activity List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(activities) { activity ->
                    ActivityCard(activity = activity)
                }
            }
        }
    }
}

@Composable
fun ActivityCard(activity: ActivityLogEntity) {
    val icon = when (activity.iconName) {
        "navigateTo" -> Icons.Outlined.Explore
        "storefront" -> Icons.Outlined.Storefront
        "car" -> Icons.Outlined.DirectionsCar
        "wallet" -> Icons.Outlined.AccountBalanceWallet
        "bolt" -> Icons.Outlined.Bolt
        "favorite" -> Icons.Filled.Favorite
        "comment" -> Icons.Outlined.ChatBubbleOutline
        else -> Icons.Outlined.Notifications
    }
    val color = Color(activity.colorHex)
    val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(activity.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BrandPurpleLight)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Container
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = timeFormatted,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activity.description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
