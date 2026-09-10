package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.viewmodel.ChatViewModel

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.LoginScreen
import com.example.ui.ProfileScreen
import com.example.ui.util.AppLanguage
import com.example.ui.util.ProvideAppLanguage
import com.example.ui.util.LocalAppStrings

import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.AuthState
import com.example.viewmodel.ProfileViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.authState.collectAsState()

                NavHost(
                    navController = navController, 
                    startDestination = if (authState is AuthState.Success) "main" else "onboarding"
                ) {
                    composable("onboarding") {
                        com.example.ui.OnboardingScreen(onFinished = {
                            navController.navigate("auth") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        })
                    }
                    composable("auth") {
                        com.example.ui.AuthScreen(
                            onAuthSuccess = { isNewUser ->
                                if (isNewUser) {
                                    navController.navigate("profileSetup")
                                } else {
                                    navController.navigate("main") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            },
                            viewModel = authViewModel
                        )
                    }
                    composable("profileSetup") {
                        com.example.ui.ProfileSetupScreen(
                            onComplete = {
                                navController.navigate("main") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            },
                            viewModel = authViewModel
                        )
                    }
                    composable("main") {
                        WurieSuperApp(onVoiceModeOpen = {
                            navController.navigate("voiceMode")
                        })
                    }
                    composable("voiceMode") {
                        com.example.ui.VoiceModeScreen(onClose = {
                            navController.popBackStack()
                        })
                    }
                }
            }
        }
    }
}

sealed class NavItem(val title: String, val icon: ImageVector?) {
    object Activity : NavItem("Activity", Icons.Outlined.List)
    object Wallet : NavItem("Wallet", Icons.Outlined.AccountBalanceWallet)
    object WurieAI : NavItem("WurieAI", null) // Center item
    object Explore : NavItem("Explore", Icons.Outlined.Explore)
    object Profile : NavItem("Profile", Icons.Outlined.PersonOutline)
}

@Composable
fun WurieSuperApp(onVoiceModeOpen: () -> Unit) {
    var currentLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }

    ProvideAppLanguage(language = currentLanguage) {
        val strings = LocalAppStrings.current
        
        // Note: Icons.Outlined.AccountBalanceWallet and Explore might not exist in base, we'll see
        // If build fails, we'll swap to safe ones. But standard compose has them.
        val items: List<Pair<NavItem, String>> = listOf(
            NavItem.Activity to strings.activity,
            NavItem.Wallet to strings.wallet,
            NavItem.WurieAI to "", // Remove label for center item
            NavItem.Explore to strings.services,
            NavItem.Profile to strings.profile
        )
        var currentRoute by remember { mutableStateOf<NavItem>(NavItem.Explore) }
        var selectedService by remember { mutableStateOf<String?>(null) }
        var selectedTradeForArtisan by remember { mutableStateOf<String?>(null) }
        var activeProviderChat by remember { mutableStateOf<Pair<String, String>?>(null) }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = BrandPurpleDark,
                    contentColor = Color.White,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).background(BrandPurpleDark),
                    tonalElevation = 0.dp
                ) {
                    items.forEach { pair ->
                        val item = pair.first
                        val label = pair.second
                        val isSelected = currentRoute == item && activeProviderChat == null && selectedService == null
                        NavigationBarItem(
                            icon = {
                                if (item == NavItem.WurieAI) {
                                    // Center logo with a subtle highlighted background if selected
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.9f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = R.drawable.ic_launcher_applogo_transparent,
                                            contentDescription = label,
                                            modifier = Modifier.size(30.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = item.icon ?: Icons.Outlined.Star,
                                        contentDescription = label,
                                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            label = {
                                if (item != NavItem.WurieAI) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            label,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            },
                            selected = isSelected,
                            onClick = { 
                                currentRoute = item 
                                selectedService = null
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BrandPurple) // Base purple theme
                    .padding(innerPadding)
            ) {
                if (activeProviderChat != null) {
                    com.example.ui.ProviderChatScreen(
                        providerName = activeProviderChat!!.first,
                        profession = activeProviderChat!!.second,
                        onBack = { activeProviderChat = null }
                    )
                } else if (selectedService != null) {
                    when (selectedService) {
                        strings.marketPrices -> com.example.ui.MarketPricesScreen(onBack = { selectedService = null })
                        strings.bookArtisan -> com.example.ui.HireProviderScreen(onBack = { selectedService = null }, onTrackProvider = { selectedService = "ProviderTracking" }, initialCategory = selectedTradeForArtisan)
                        "ProviderTracking" -> com.example.ui.ProviderTrackingScreen(onBack = { selectedService = strings.bookArtisan })
                        "ProviderDashboard" -> com.example.ui.ProviderDashboardScreen(onBack = { selectedService = null })
                        "ProviderRegistration" -> com.example.ui.ProviderRegistrationScreen(onBack = { selectedService = null }, onSubmitSuccess = { selectedService = "ProviderDashboard" })
                        "AdminDashboard" -> com.example.ui.AdminDashboardScreen(onBack = { selectedService = null })
                        strings.rideKeke -> com.example.ui.RideBookingScreen(onBack = { selectedService = null })
                        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("$selectedService (Coming Soon)", color = Color.White)
                        }
                    }
                } else {
                    when (currentRoute) {
                        NavItem.Activity -> com.example.ui.ActivityScreen()
                        NavItem.Wallet -> com.example.ui.WalletScreen()
                        NavItem.WurieAI -> {
                            val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory)
                            
                            val navigateTo: (String) -> Unit = { navString ->
                                val parts = navString.split(":")
                                val screenName = parts[0]
                                val param = if (parts.size > 1) parts[1] else null
                                
                                when (screenName) {
                                    "Market Prices" -> {
                                        currentRoute = NavItem.Explore
                                        selectedService = strings.marketPrices
                                    }
                                    "Hire Provider", "Hire Provider" -> {
                                        currentRoute = NavItem.Explore
                                        selectedService = strings.bookArtisan
                                        selectedTradeForArtisan = param
                                    }
                                    "Wallet" -> {
                                        currentRoute = NavItem.Wallet
                                        selectedService = null
                                    }
                                    "Activity" -> {
                                        currentRoute = NavItem.Activity
                                        selectedService = null
                                    }
                                    "Profile" -> {
                                        currentRoute = NavItem.Profile
                                        selectedService = null
                                    }
                                }
                            }
                            
                            LaunchedEffect(Unit) {
                                viewModel.navigationEvents.collect { screenName ->
                                    navigateTo(screenName)
                                }
                            }
                            
                            WurieAssistantScreen(
                                currentLanguage = currentLanguage,
                                onLanguageChange = { currentLanguage = it },
                            
                                viewModel = viewModel,
                                onChatWithProvider = { name, profession ->
                                    activeProviderChat = name to profession
                                },
                                onNavigate = navigateTo,
                                onVoiceModeOpen = onVoiceModeOpen
                            )
                        }
                        NavItem.Explore -> {
                            val exploreViewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.Factory)
                            val exploreUiState by exploreViewModel.uiState.collectAsState()

                            LaunchedEffect(Unit) {
                                exploreViewModel.loadSocialFeed()
                            }

                            com.example.ui.ExploreScreen(
                                comments = exploreUiState.comments,
                                notifications = exploreUiState.notifications,
                                onAddComment = { text -> exploreViewModel.addComment(text) },
                                onMarkNotificationRead = { id -> exploreViewModel.markNotificationRead(id) },
                                onServiceClick = { selectedService = it }
                            )
                        }
                        NavItem.Profile -> {
                            val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
                            val profileUiState by profileViewModel.uiState.collectAsState()

                            LaunchedEffect(Unit) {
                                profileViewModel.loadProfile()
                            }

                            com.example.ui.ProfileScreen(
                                onProviderPortal = { selectedService = "ProviderRegistration" },
                                onAdminPortal = { selectedService = "AdminDashboard" },
                                currentLanguage = currentLanguage,
                                onLanguageChange = { currentLanguage = it },
                                profileUiState = profileUiState,
                                onProfileSave = { name, phone, city ->
                                    profileViewModel.saveProfile(name, phone, city)
                                },
                                onSettingsSave = { notifications, biometric, push, offline, language ->
                                    profileViewModel.saveSettings(
                                        notificationsEnabled = notifications,
                                        biometricEnabled = biometric,
                                        pushEnabled = push,
                                        offlineCacheEnabled = offline,
                                        language = language
                                    )
                                },
                                onLogout = { authViewModel.signOut() }
                            )
                        }
                        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            val routeName = items.firstOrNull { it.first == currentRoute }?.second ?: ""
                            Text("$routeName (Coming Soon)", color = Color.White)
                        }
                    }
                
                }
            }
        }
    }
}

@Composable
fun WurieAssistantScreen(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory),
    onChatWithProvider: (String, String) -> Unit,
    onNavigate: (String) -> Unit,
    onVoiceModeOpen: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val strings = LocalAppStrings.current
    var showNotifications by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Top Bar (Welcome)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Wurie",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)) // Active Green
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "I am Wurie, your intelligent agent for the MRU. How can I facilitate your journey today?",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (messages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { viewModel.clearChat() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Clear Chat", tint = Color.White)
                    }
                }
                // Notification Badge Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BrandPurpleLight)
                        .clickable { showNotifications = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Color.White)
                    // Red badge indicator
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

        // Action Grid or Chat History
        if (messages.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Left (Green)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF8BC34A)) // Green
                            .clickable { }
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("frank.onatraiq", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("$20", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    // Bottom Left (Teal)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF4DB6AC)) // Teal
                            .clickable { }
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("Today's\nMatches", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.fillMaxWidth(0.7f).height(4.dp).background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.5f).height(4.dp).background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                
                // Right Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Right (Purple)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF7E57C2)) // Light Purple
                            .clickable { }
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("Abu - Plumber\nfor fix your\nleaking pipe", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("3 d ago", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                    
                    // Bottom Right (Orange)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFF8A65)) // Orange
                            .clickable { }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Check today\nfish price at\nLumley?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                    }
                }
            }
        } else {
            // Chat Messages
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(messages) { message ->
                    if (message.isUser) {
                        UserBubble(text = message.text)
                    } else {
                        BotBubble(
                            text = message.text,
                            onChatWithProvider = onChatWithProvider,
                            onNavigate = onNavigate,
                            isLoading = message.isLoading,
                            isError = message.isError
                        )
                    }
                }
            }
        }
        
        if (messages.isEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            // Always show prominent Voice Mode when empty
            VoiceModeCard(onClick = onVoiceModeOpen)
            Spacer(modifier = Modifier.weight(1f))
        }

        if (showNotifications) {
            AlertDialog(
                onDismissRequest = { showNotifications = false },
                title = { Text("Notifications", color = Color.White) },
                text = {
                    Column {
                        Text("Sarah Jenkins liked your post", color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Wallet Top-Up Successful", color = Color.White.copy(alpha = 0.8f))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showNotifications = false }) {
                        Text("Close", color = BotBubbleGreen)
                    }
                },
                containerColor = BrandPurpleDark
            )
        }

        // Input Field
        ChatInputField(onSend = { text, base64Image -> viewModel.sendMessage(text, base64Image) })
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun UserBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp))
                .background(UserBubbleLight)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(text, color = TextPrimaryLight, fontSize = 16.sp)
        }
    }
}

@Composable
fun BotBubble(
    text: String,
    onChatWithProvider: (String, String) -> Unit,
    onNavigate: (String) -> Unit,
    isLoading: Boolean = false,
    isError: Boolean = false
) {
    val chatProviderRegex = Regex("\\[CHAT_PROVIDER:(.*?):(.*?)\\]")
    val navigateToRegex = Regex("\\[NAVIGATE_TO:(.*?)\\]")
    
    val providerMatch = chatProviderRegex.find(text)
    val navigateMatch = navigateToRegex.find(text)
    
    val cleanedText = text.replace(chatProviderRegex, "").replace(navigateToRegex, "").trim()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp))
                .background(if (isError) Color(0xFFD32F2F) else BotBubbleGreen)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(0.9f)
        ) {
            Column {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(cleanedText, color = Color.White, fontSize = 16.sp, lineHeight = 24.sp)
                    
                    if (providerMatch != null) {
                        val providerName = providerMatch.groupValues[1]
                        val profession = providerMatch.groupValues[2]
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { onChatWithProvider(providerName, profession) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chat with $providerName ($profession)", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                    
                    if (navigateMatch != null) {
                        val navString = navigateMatch.groupValues[1]
                        val screenName = navString.split(":")[0]
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { onNavigate(navString) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Explore, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View $screenName", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceModeCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(VoiceModeGradientStart, VoiceModeGradientEnd)
                )
            )
            .clickable { onClick() }
            .padding(vertical = 24.dp, horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice Mode", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Voice Mode",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "READY",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Speak naturally, get instant help",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun ChatInputField(onSend: (String, String?) -> Unit) {
    var text by remember { mutableStateOf("") }
    val strings = LocalAppStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val byteArrayOutputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64Image = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
            
            // Send default text if empty, otherwise use current text
            val messageText = if (text.isNotBlank()) text else "What's in this image?"
            onSend(messageText, base64Image)
            text = ""
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(BrandPurpleLight)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.PhotoCamera,
            contentDescription = "Camera",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.clickable { cameraLauncher.launch(null) }
        )
        Spacer(modifier = Modifier.width(16.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text(strings.typeOrSpeak, color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                }
                innerTextField()
            }
        )
        if (text.isNotEmpty()) {
            Icon(
                Icons.Filled.Send,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.clickable {
                    onSend(text, null)
                    text = ""
                }
            )
        } else {
            val speechRecognizerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    val data = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = data?.get(0) ?: ""
                    if (spokenText.isNotEmpty()) {
                        onSend(spokenText, null)
                    }
                }
            }
            Icon(
                Icons.Outlined.Mic,
                contentDescription = "Mic",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.clickable {
                    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    }
                    try {
                        speechRecognizerLauncher.launch(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            )
        }
    }
}
