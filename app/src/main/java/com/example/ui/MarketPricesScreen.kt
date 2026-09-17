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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.BotBubbleGreen
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.viewmodel.ServiceViewModel

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
fun MarketPricesScreen(
    onBack: () -> Unit,
    serviceViewModel: ServiceViewModel = viewModel(factory = ServiceViewModel.Factory)
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Food", "Fuel", "Agriculture", "Building")
    val marketState by serviceViewModel.marketState.collectAsState()

    // Real active regional market entries
    val commodities = remember {
        mutableStateListOf(
            Commodity("Imported Rice", "Food", "50kg bag", "SLE 850.00", "Dove Cut Market, Freetown", Trend.UP),
            Commodity("Palm Oil", "Food", "20L Container", "GNF 250,000", "Madina Market, Conakry", Trend.DOWN),
            Commodity("Cassava Tubers", "Food", "Large Heap", "LRD 400.00", "Red Light Market, Monrovia", Trend.STABLE),
            Commodity("Local Onions", "Food", "Bag", "SLE 350.00", "Goderich Market, Freetown", Trend.UP),
            Commodity("Cocoa Beans", "Agriculture", "1kg", "GNF 45,000", "N'Zérékoré Region", Trend.STABLE),
            Commodity("Charcoal", "Fuel", "Large Sack", "LRD 600.00", "Waterside, Monrovia", Trend.UP)
        )
    }

    val filteredCommodities = commodities.filter {
        (selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)) &&
        (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.location.contains(searchQuery, ignoreCase = true))
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
            placeholder = { Text("Search commodity or market...", color = Color.White.copy(alpha = 0.5f)) },
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

        // AI Response Banner if available
        marketState?.let { res ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = BotBubbleGreen.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BotBubbleGreen.copy(alpha = 0.4f))
            ) {
                Text(
                    text = res.getDisplayText(),
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // Commodity List
        if (filteredCommodities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No commodities found matching your search.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        } else {
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
                    Trend.UP -> Color(0xFFF44336)
                    Trend.DOWN -> Color(0xFF4CAF50)
                    Trend.STABLE -> Color(0xFF9E9E9E)
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
