package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.*
import com.example.ui.util.LocalAppStrings

data class ServiceItem(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

data class FeedComment(
    val id: String,
    val username: String,
    val comment: String,
    val timeAgo: String
)

data class NotificationEntry(
    val id: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val unread: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    comments: List<FeedComment> = emptyList(),
    notifications: List<NotificationEntry> = emptyList(),
    onAddComment: (String) -> Unit = {},
    onMarkNotificationRead: (String) -> Unit = {},
    onServiceClick: (String) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp) // Padding for bottom nav
        ) {
        item {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Good Afternoon, ✨",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Explore The World",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BrandPurpleLight)
                        .clickable { showNotificationsSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Color.White)
                    if (notifications.any { it.unread }) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }

        item {
            // Smart Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandPurpleLight)
                    .clickable { /* Handle Search */ }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Ask WurieAI to find anything...",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Search", tint = Color(0xFFFFC107))
                }
            }
        }

        item {
            ExploreInsightCard(
                title = "Market Update",
                subtitle = "2h ago • Commodity Prices",
                content = "Cocoa prices have risen by 5% in the local market today. Great time to review your selling strategy.",
                icon = Icons.Outlined.TrendingUp,
                iconTint = Color(0xFF4CAF50),
                actionText = "View Prices",
                onActionClick = { onServiceClick(strings.marketPrices) }
            )
        }

        item {
            SocialFeedCard(
                username = "Musa Kamara",
                timeAgo = "1h ago",
                content = "Great news for Freetown! The new transport regulations are helping reduce traffic in the CBD. Anyone noticed the difference during morning commute?",
                initialLikes = 89,
                initialComments = comments.size,
                onCommentClick = { showCommentsSheet = true }
            )
        }
        
        item {
            ExploreInsightCard(
                title = "Artisan Spotlight",
                subtitle = "5h ago • Home Services",
                content = "Looking for reliable electricians in Lumley? Check out today's top-rated professionals with verified reviews.",
                icon = Icons.Outlined.Handyman,
                iconTint = Color(0xFF2196F3),
                actionText = "Hire Artisan",
                onActionClick = { onServiceClick(strings.bookArtisan) }
            )
        }

        item {
            SocialFeedCard(
                username = "David Chen",
                timeAgo = "5h ago",
                content = "Finally finished my new portfolio website. I went with a clean, minimal design this time around. Check it out!",
                initialLikes = 128,
                initialComments = comments.size + 2,
                onCommentClick = { showCommentsSheet = true }
            )
        }
        
        item {
            ExploreInsightCard(
                title = "WurieAI System",
                subtitle = "1d ago • System Update",
                content = "Voice Mode is now faster and more accurate. Try asking WurieAI to book a ride or check your wallet balance using just your voice.",
                icon = Icons.Filled.AutoAwesome,
                iconTint = Color(0xFFFFC107),
                actionText = "Try Voice Mode",
                onActionClick = { }
            )
        }
    }

        if (showCommentsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCommentsSheet = false },
                sheetState = sheetState,
                containerColor = BrandPurple
            ) {
                CommentsSection(
                    comments = comments,
                    onAddComment = { text ->
                        if (text.isNotBlank()) {
                            onAddComment(text)
                        }
                    }
                )
            }
        }

        if (showNotificationsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNotificationsSheet = false },
                sheetState = sheetState,
                containerColor = BrandPurple
            ) {
                NotificationSection(
                    notifications = notifications,
                    onMarkRead = { id ->
                        onMarkNotificationRead(id)
                    }
                )
            }
        }
    }
}

@Composable
fun SocialFeedCard(
    username: String,
    timeAgo: String,
    content: String,
    initialLikes: Int,
    initialComments: Int,
    onCommentClick: () -> Unit
) {
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableStateOf(initialLikes) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(BrandPurpleLight)
            .padding(20.dp)
    ) {
        // User Info Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = username,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeAgo,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "More options",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Post Content
        Text(
            text = content,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Actions (Like, Comment, Share)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            SocialAction(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                text = likesCount.toString(),
                tint = if (isLiked) Color(0xFFE91E63) else Color.White.copy(alpha = 0.7f),
                onClick = {
                    isLiked = !isLiked
                    likesCount = if (isLiked) likesCount + 1 else likesCount - 1
                }
            )
            Spacer(modifier = Modifier.width(24.dp))
            SocialAction(
                icon = Icons.Outlined.ChatBubbleOutline,
                text = initialComments.toString(),
                onClick = onCommentClick
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Outlined.Share,
                contentDescription = "Share",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SocialAction(
    icon: ImageVector,
    text: String,
    tint: Color = Color.White.copy(alpha = 0.7f),
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CommentsSection(
    comments: List<FeedComment>,
    onAddComment: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp)
            .padding(24.dp)
    ) {
        Text(
            text = "Comments",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        comments.forEach { item ->
            CommentItem(username = item.username, comment = item.comment, timeAgo = item.timeAgo)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text("Write a comment...", color = Color.White.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPurpleAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onAddComment(draft)
                        draft = ""
                    }
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = BrandPurpleAccent)
                }
            }
        )
    }
}

@Composable
fun CommentItem(username: String, comment: String, timeAgo: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(timeAgo, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        }
    }
}

@Composable
fun NotificationSection(
    notifications: List<NotificationEntry>,
    onMarkRead: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "Notifications",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        notifications.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (item.unread) BrandPurpleLight else Color.White.copy(alpha = 0.04f))
                    .clickable { onMarkRead(item.id) }
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (item.unread) BotBubbleGreen else Color.White.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.body, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(item.timeAgo, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ExploreInsightCard(
    title: String,
    subtitle: String,
    content: String,
    icon: ImageVector,
    iconTint: Color,
    actionText: String,
    onActionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(BrandPurpleLight)
            .padding(20.dp)
    ) {
        // User Info Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "More options",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Post Content
        Text(
            text = content,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurpleAccent),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(actionText, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// EOF
