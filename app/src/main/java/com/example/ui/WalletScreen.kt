package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
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
import com.example.api.WalletBalanceResponse
import com.example.ui.theme.BotBubbleGreen
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleAccent
import com.example.ui.theme.BrandPurpleDark

@Composable
fun WalletScreen(walletState: WalletBalanceResponse? = null) {
    var notified by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandPurple)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(BrandPurpleAccent.copy(alpha = 0.2f))
                    .border(2.dp, BrandPurpleAccent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = "WuriePay",
                    tint = BrandPurpleAccent,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Coming Soon Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BotBubbleGreen.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BotBubbleGreen.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "COMING SOON",
                    color = BotBubbleGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "WuriePay",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle Description
            Text(
                text = "Instant digital wallet, local currency top-ups, and secure escrow payments across Sierra Leone & the MRU region.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Feature Highlights Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = BrandPurpleDark.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeatureRow(
                        title = "Zero-Fee Local Payments",
                        description = "Direct mobile money and bank transfers with instant settlement."
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    FeatureRow(
                        title = "Smart Escrow Protection",
                        description = "Funds are released only when service completion is verified."
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    FeatureRow(
                        title = "Multi-Currency MRU Wallet",
                        description = "Seamless exchange between SLE, GNF, LRD, and USD."
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Get Notified Button
            Button(
                onClick = { notified = !notified },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (notified) BotBubbleGreen else BrandPurpleAccent
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (notified) Icons.Outlined.Lock else Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = if (notified) BrandPurple else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (notified) "Notification Set!" else "Notify Me at Launch",
                        color = if (notified) BrandPurple else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(title: String, description: String) {
    Column {
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
    }
}
