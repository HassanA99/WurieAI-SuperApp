package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleAccent
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.util.LocalAppStrings

@Composable
fun WalletScreen() {
    val strings = LocalAppStrings.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            // Header
            Text(
                text = "Wallet",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        
        item {
            // Balance Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BrandPurpleAccent)
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Total Balance",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SLE 14,500.00",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WalletAction(icon = Icons.Filled.Add, label = "Top Up")
                        WalletAction(icon = Icons.Filled.ArrowUpward, label = "Send")
                        WalletAction(icon = Icons.Filled.ArrowDownward, label = "Receive")
                        WalletAction(icon = Icons.Filled.SwapHoriz, label = "Swap")
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
            AgenticInsightCard()
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
            ExchangeRateCard()
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See All",
                    color = BrandPurpleAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        item { TransactionItem(title = "Keke Ride", amount = "-SLE 15.00", date = "Today, 10:30 AM", isPositive = false) }
        item { TransactionItem(title = "Market Deposit", amount = "+SLE 500.00", date = "Yesterday, 4:15 PM", isPositive = true) }
        item { TransactionItem(title = "Electrician Services", amount = "-SLE 150.00", date = "Sep 1, 2:00 PM", isPositive = false) }
        item { TransactionItem(title = "Received from Fatima", amount = "+SLE 200.00", date = "Aug 29, 9:45 AM", isPositive = true) }
    }
}

@Composable
fun WalletAction(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TransactionItem(title: String, amount: String, date: String, isPositive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BrandPurpleLight)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isPositive) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFE91E63).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPositive) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                contentDescription = null,
                tint = if (isPositive) Color(0xFF4CAF50) else Color(0xFFE91E63)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(date, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        
        Text(
            text = amount,
            color = if (isPositive) Color(0xFF4CAF50) else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AgenticInsightCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandPurpleLight.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = "AI Insight",
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "WurieAI Insight",
                color = Color(0xFFFFC107),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You've spent SLE 165.00 on Artisan services this week. Consider comparing rates in the Hire section to maximize your savings on your next booking.",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun ExchangeRateCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandPurpleLight.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Text(
            text = "Live Exchange Rates",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ExchangeRateItem(currency = "USD", rate = "22.50")
            ExchangeRateItem(currency = "EUR", rate = "24.30")
            ExchangeRateItem(currency = "GBP", rate = "28.10")
        }
    }
}

@Composable
fun ExchangeRateItem(currency: String, rate: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = currency, 
            color = Color.White.copy(alpha = 0.6f), 
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = rate, 
            color = Color.White, 
            fontSize = 16.sp, 
            fontWeight = FontWeight.Bold
        )
    }
}