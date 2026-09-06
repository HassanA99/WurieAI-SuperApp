package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Search
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

data class Commodity(
    val name: String,
    val category: String,
    val unit: String,
    val price: String,
    val location: String,
    val trend: Trend
)

enum class Trend { UP, DOWN, STABLE }

@Composable
fun MarketPricesScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // Mock data for Mano River Union (MRU) market prices
    val allCommodities = listOf(
        Commodity("Rice (Imported)", "Food", "50kg bag", "NLe 850", "Dove Cut, Sierra Leone", Trend.UP),
        Commodity("Palm Oil", "Food", "20 Liters", "250,000 GNF", "Madina Market, Guinea", Trend.DOWN),
        Commodity("Cassava", "Food", "Per Bundle", "$400 LRD", "Red Light Market, Liberia", Trend.STABLE),
        Commodity("Onions", "Food", "Bag", "NLe 350", "Dove Cut, Sierra Leone", Trend.UP),
        Commodity("Coffee Beans", "Agriculture", "1kg", "45,000 GNF", "N'Zérékoré, Guinea", Trend.STABLE),
        Commodity("Charcoal", "Fuel", "Large Bag", "$600 LRD", "Waterside, Liberia", Trend.UP)
    )
    
    val categories = listOf("All", "Food", "Fuel", "Agriculture", "Building")
    
    val filteredCommodities = allCommodities.filter {
        (selectedCategory == "All" || it.category == selectedCategory) &&
        (it.name.contains(searchQuery, ignoreCase = true) || it.location.contains(searchQuery, ignoreCase = true))
    }

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
                text = "Market Prices",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search commodities...", color = Color.White.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BotBubbleGreen,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = BotBubbleGreen,
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Category Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) BotBubbleGreen else BrandPurpleLight)
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) BrandPurple else Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredCommodities) { item ->
                CommodityCard(item)
            }
        }
    }
}

@Composable
fun CommodityCard(item: Commodity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BrandPurpleLight)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.unit} • ${item.location}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.price,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val trendColor = when (item.trend) {
                    Trend.UP -> Color(0xFFF44336) // Red (inflation)
                    Trend.DOWN -> Color(0xFF4CAF50) // Green (cheaper)
                    Trend.STABLE -> Color(0xFF9E9E9E) // Grey
                }
                val trendIcon = when (item.trend) {
                    Trend.UP -> Icons.AutoMirrored.Outlined.TrendingUp
                    Trend.DOWN -> Icons.AutoMirrored.Outlined.TrendingDown
                    Trend.STABLE -> Icons.AutoMirrored.Outlined.TrendingFlat
                }
                Icon(
                    imageVector = trendIcon,
                    contentDescription = item.trend.name,
                    tint = trendColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.trend.name,
                    color = trendColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
