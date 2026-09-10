package com.example.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.ProfileSettings
import com.example.api.UserProfile
import com.example.ui.theme.*
import com.example.ui.util.AppLanguage
import com.example.ui.util.LocalAppStrings
import com.example.viewmodel.ProfileUiState

data class BookingRecord(
    val id: String,
    val title: String,
    val providerName: String,
    val category: String,
    val dateTime: String,
    val fare: String,
    val status: String,
    val isLive: Boolean = false,
    val destination: String? = null
)

data class SavedPlace(
    val id: String,
    val title: String,
    val address: String,
    val icon: ImageVector
)

data class EmergencyContact(
    val id: String,
    val name: String,
    val relation: String,
    val phone: String
)

data class MobileMoneyAccount(
    val id: String,
    val provider: String,
    val phone: String,
    val isPrimary: Boolean
)

@Composable
fun ProfileScreen(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onProviderPortal: () -> Unit = {},
    onAdminPortal: () -> Unit = {},
    profileUiState: ProfileUiState = ProfileUiState(),
    onProfileSave: (String, String?, String?) -> Unit = { _, _, _ -> },
    onSettingsSave: (Boolean?, Boolean?, Boolean?, Boolean?, String?) -> Unit = { _, _, _, _, _ -> },
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("wurie_user_session", Context.MODE_PRIVATE) }

    val profile: UserProfile? = profileUiState.profile
    val settings: ProfileSettings? = profileUiState.settings

    val userName = profile?.fullName ?: prefs.getString("user_name", "Foday Kamara") ?: "Foday Kamara"
    val userEmail = profile?.email ?: prefs.getString("user_email", "foday.k@wurie.ai") ?: "foday.k@wurie.ai"
    val userPhone = profile?.phone ?: prefs.getString("user_phone", "+232 78 450 892") ?: "+232 78 450 892"
    val userCity = profile?.city ?: prefs.getString("user_city", "Freetown, Sierra Leone") ?: "Freetown, Sierra Leone"

    // Dialog states
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showKycDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showAddPlaceDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showLinkMomoDialog by remember { mutableStateOf(false) }
    var selectedBookingForReceipt by remember { mutableStateOf<BookingRecord?>(null) }
    var showSafetyTestToast by remember { mutableStateOf(false) }

    // Security & feature toggles
    var biometricEnabled by remember(profileUiState.settings) { mutableStateOf(settings?.biometricEnabled ?: true) }
    var twoFactorEnabled by remember(profileUiState.settings) { mutableStateOf(settings?.pushEnabled ?: true) }
    var offlineCacheEnabled by remember(profileUiState.settings) { mutableStateOf(settings?.offlineCacheEnabled ?: true) }
    var pushNotificationsEnabled by remember(profileUiState.settings) { mutableStateOf(settings?.notificationsEnabled ?: true) }

    LaunchedEffect(settings) {
        biometricEnabled = settings?.biometricEnabled ?: true
        twoFactorEnabled = settings?.pushEnabled ?: true
        offlineCacheEnabled = settings?.offlineCacheEnabled ?: true
        pushNotificationsEnabled = settings?.notificationsEnabled ?: true
    }

    // Interactive lists
    var bookings by remember {
        mutableStateOf(
            listOf(
                BookingRecord(
                    id = "BK-9021",
                    title = "Keke Ride (Bajaj)",
                    providerName = "Alusine Sesay",
                    category = "Transport",
                    dateTime = "Today, 2:45 PM",
                    fare = "SLE 15.00",
                    status = "Driver En Route",
                    isLive = true,
                    destination = "Lumley Roundabout → Congo Cross"
                ),
                BookingRecord(
                    id = "BK-8842",
                    title = "Emergency Plumbing",
                    providerName = "Abu Koroma",
                    category = "Artisan",
                    dateTime = "Yesterday, 10:30 AM",
                    fare = "SLE 180.00",
                    status = "Completed",
                    destination = "Wilkinson Road, Main Flat"
                ),
                BookingRecord(
                    id = "BK-7619",
                    title = "Electrical Diagnostics",
                    providerName = "Fatu Turay",
                    category = "Artisan",
                    dateTime = "Sep 3, 2026",
                    fare = "SLE 120.00",
                    status = "Completed",
                    destination = "Kroo Town Road Workshop"
                )
            )
        )
    }

    var savedPlaces by remember {
        mutableStateOf(
            listOf(
                SavedPlace("1", "Home", "34 Wilkinson Road, Freetown", Icons.Filled.Home),
                SavedPlace("2", "Office / Shop", "12 Siaka Stevens Street, Central Freetown", Icons.Filled.Business),
                SavedPlace("3", "Market Stall", "Dove Cut Market, Guard Street", Icons.Filled.Storefront)
            )
        )
    }

    var emergencyContacts by remember {
        mutableStateOf(
            listOf(
                EmergencyContact("1", "Momodu Bah", "Brother", "+232 76 991 223"),
                EmergencyContact("2", "Aminata Sesay", "Partner", "+232 88 123 456")
            )
        )
    }

    var momoAccounts by remember {
        mutableStateOf(
            listOf(
                MobileMoneyAccount("1", "Orange Money", "+232 78 ••• 892", isPrimary = true),
                MobileMoneyAccount("2", "Africell Afrimoney", "+232 88 ••• 104", isPrimary = false)
            )
        )
    }

    var selectedTab by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.profile,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { showEditProfileDialog = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BrandPurpleLight)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit Profile", tint = Color.White)
            }
        }

        // Profile Identity Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(BrandPurpleLight)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(BrandPurpleAccent)
                    .border(2.dp, BotBubbleGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(BotBubbleGreen)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Verified", tint = BrandPurple, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(userName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(userPhone, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Text(userCity, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)

            Spacer(modifier = Modifier.height(12.dp))

            // KYC Status Pill
            Surface(
                onClick = { showKycDialog = true },
                shape = RoundedCornerShape(20.dp),
                color = BotBubbleGreen.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BotBubbleGreen.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = BotBubbleGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tier 2: National ID Verified", color = BotBubbleGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BotBubbleGreen, modifier = Modifier.size(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStat(count = "14", label = "AI Tasks")
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.1f)))
                ProfileStat(count = "5", label = "Bookings")
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.1f)))
                ProfileStat(count = "4.9 ★", label = "Rating")
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.1f)))
                ProfileStat(count = "420", label = "WuriePts")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = BotBubbleGreen,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BotBubbleGreen
                    )
                }
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Bookings", color = if (selectedTab == 0) BotBubbleGreen else Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Places & Safety", color = if (selectedTab == 1) BotBubbleGreen else Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Settings", color = if (selectedTab == 2) BotBubbleGreen else Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tab Content
        when (selectedTab) {
            0 -> {
                // TAB 0: BOOKINGS (Live & History)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active & Past Bookings", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${bookings.size} total", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    }

                    bookings.forEach { record ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBookingForReceipt = record },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandPurpleLight)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (record.isLive) BotBubbleGreen.copy(alpha = 0.2f) else BrandPurpleAccent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                if (record.category == "Transport") Icons.Filled.DirectionsCar else Icons.Filled.Handyman,
                                                contentDescription = null,
                                                tint = if (record.isLive) BotBubbleGreen else Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(record.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(record.providerName, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(record.fare, color = BotBubbleGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (record.isLive) BotBubbleGreen else Color.White.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                record.status,
                                                color = if (record.isLive) BrandPurple else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                if (record.destination != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Place, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(record.destination, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(record.dateTime, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = BotBubbleGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("View Receipt", color = BotBubbleGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // TAB 1: SAVED PLACES & SAFETY TOOLKIT
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Saved Places Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandPurpleLight)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Saved Locations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            TextButton(onClick = { showAddPlaceDialog = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = BotBubbleGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", color = BotBubbleGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        savedPlaces.forEachIndexed { index, place ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BrandPurpleAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(place.icon, contentDescription = null, tint = BotBubbleGreen, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(place.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(place.address, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = { savedPlaces = savedPlaces.filter { it.id != place.id } },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                                }
                            }
                            if (index < savedPlaces.size - 1) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            }
                        }
                    }

                    // Emergency Contacts Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandPurpleLight)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Emergency SOS Contacts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Alerted during transit emergencies", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                            TextButton(onClick = { showAddContactDialog = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = BotBubbleGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", color = BotBubbleGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        emergencyContacts.forEachIndexed { index, contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53935).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Shield, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("${contact.relation} • ${contact.phone}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = { emergencyContacts = emergencyContacts.filter { it.id != contact.id } },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                                }
                            }
                            if (index < emergencyContacts.size - 1) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showSafetyTestToast = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BotBubbleGreen),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BotBubbleGreen.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Outlined.EmergencyShare, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Safety Protocol Broadcast", fontSize = 13.sp)
                        }
                    }
                }
            }
            2 -> {
                // TAB 2: SETTINGS, MONEY & SECURITY
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    // Linked Mobile Money
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandPurpleLight)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Payment & Mobile Money", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Used for Ride & Artisan Escrow", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                            TextButton(onClick = { showLinkMomoDialog = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = BotBubbleGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Link", color = BotBubbleGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        momoAccounts.forEachIndexed { index, momo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (momo.provider.contains("Orange")) Color(0xFFFF6600).copy(alpha = 0.2f) else BotBubbleGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(momo.provider, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        if (momo.isPrimary) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = BotBubbleGreen.copy(alpha = 0.2f)
                                            ) {
                                                Text("Default", color = BotBubbleGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Text(momo.phone, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                            if (index < momoAccounts.size - 1) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            }
                        }
                    }

                    // Security & Toggles
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandPurpleLight)
                            .padding(18.dp)
                    ) {
                        Text("Security & Performance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biometric App Lock", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Require fingerprint on app resume", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = {
                                    biometricEnabled = it
                                    onSettingsSave(
                                        notificationsEnabled = pushNotificationsEnabled,
                                        biometricEnabled = it,
                                        pushEnabled = twoFactorEnabled,
                                        offlineCacheEnabled = offlineCacheEnabled,
                                        language = settings?.language ?: "en"
                                    )
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = BotBubbleGreen, checkedTrackColor = BotBubbleGreen.copy(alpha = 0.3f))
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Two-Factor SMS Escrow Release", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("OTP verification for high-value payouts", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                            Switch(
                                checked = twoFactorEnabled,
                                onCheckedChange = {
                                    twoFactorEnabled = it
                                    onSettingsSave(
                                        notificationsEnabled = pushNotificationsEnabled,
                                        biometricEnabled = biometricEnabled,
                                        pushEnabled = it,
                                        offlineCacheEnabled = offlineCacheEnabled,
                                        language = settings?.language ?: "en"
                                    )
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = BotBubbleGreen, checkedTrackColor = BotBubbleGreen.copy(alpha = 0.3f))
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Offline Low-Bandwidth Mode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Caches market rates & contacts locally", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                            Switch(
                                checked = offlineCacheEnabled,
                                onCheckedChange = {
                                    offlineCacheEnabled = it
                                    onSettingsSave(
                                        notificationsEnabled = pushNotificationsEnabled,
                                        biometricEnabled = biometricEnabled,
                                        pushEnabled = twoFactorEnabled,
                                        offlineCacheEnabled = it,
                                        language = settings?.language ?: "en"
                                    )
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = BotBubbleGreen, checkedTrackColor = BotBubbleGreen.copy(alpha = 0.3f))
                            )
                        }
                    }

                    // Preferences & Portals
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandPurpleLight)
                    ) {
                        var languageMenuExpanded by remember { mutableStateOf(false) }

                        Box {
                            ProfileOptionRow(
                                icon = Icons.Outlined.Translate,
                                title = strings.language,
                                subtitle = when (currentLanguage) {
                                    AppLanguage.ENGLISH -> "English"
                                    AppLanguage.FULANI_ADLAM -> "𞤊𞤵𞤤𞤢𞤲𞤭 (ADLaM)"
                                    AppLanguage.FRENCH -> "Français"
                                },
                                onClick = { languageMenuExpanded = true }
                            )

                            DropdownMenu(
                                expanded = languageMenuExpanded,
                                onDismissRequest = { languageMenuExpanded = false },
                                modifier = Modifier.background(BrandPurpleLight)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("English", color = Color.White) },
                                    onClick = {
                                        onLanguageChange(AppLanguage.ENGLISH)
                                        languageMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("𞤊𞤵𞤤𞤢𞤲𞤭 (ADLaM)", color = Color.White) },
                                    onClick = {
                                        onLanguageChange(AppLanguage.FULANI_ADLAM)
                                        languageMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Français", color = Color.White) },
                                    onClick = {
                                        onLanguageChange(AppLanguage.FRENCH)
                                        languageMenuExpanded = false
                                    }
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))
                        ProfileOptionRow(icon = Icons.Filled.Build, title = "Provider Portal", subtitle = "Switch to Artisan/Driver mode", onClick = onProviderPortal)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))
                        ProfileOptionRow(icon = Icons.Filled.Security, title = "Admin Verification Portal", subtitle = "Verify pending artisan licenses", onClick = onAdminPortal)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Logout Button
        Button(
            onClick = { showLogoutConfirmDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
        ) {
            Icon(Icons.Outlined.Logout, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.logout, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Version Footnote
        Text(
            text = "WurieAI Super App • v1.4.2 MRU Regional Build",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))
    }

    // ======================== DIALOGS ========================

    // 1. Edit Profile Dialog
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempPhone by remember { mutableStateOf(userPhone) }
        var tempCity by remember { mutableStateOf(userCity) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Full Name", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BotBubbleGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("Phone Number", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BotBubbleGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempCity,
                        onValueChange = { tempCity = it },
                        label = { Text("City / Region", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BotBubbleGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.edit()
                            .putString("user_name", tempName)
                            .putString("user_phone", tempPhone)
                            .putString("user_city", tempCity)
                            .apply()
                        onProfileSave(tempName, tempPhone, tempCity)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BotBubbleGreen)
                ) {
                    Text("Save", color = BrandPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = BrandPurpleDark
        )
    }

    // 2. KYC Verification Details Dialog
    if (showKycDialog) {
        AlertDialog(
            onDismissRequest = { showKycDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = BotBubbleGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KYC Verification Status", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your identity has been authenticated against the National Civil Registration Authority (NCRA) database.", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Identity Document: Sierra Leone National ID Card", color = Color.White, fontSize = 13.sp)
                    Text("• Verification Number: SL-NIN-9281-XXXX", color = Color.White, fontSize = 13.sp)
                    Text("• Tier 2 Limit: SLE 25,000.00 / day", color = BotBubbleGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("• Escrow Protection: Active on all bookings", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showKycDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BotBubbleGreen)
                ) {
                    Text("Done", color = BrandPurple, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = BrandPurpleDark
        )
    }

    // 3. Add Saved Place Dialog
    if (showAddPlaceDialog) {
        var placeLabel by remember { mutableStateOf("") }
        var placeAddress by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPlaceDialog = false },
            title = { Text("Add Saved Location", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = placeLabel,
                        onValueChange = { placeLabel = it },
                        placeholder = { Text("Label (e.g., Mom's House, Gym)", color = Color.White.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BotBubbleGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = placeAddress,
                        onValueChange = { placeAddress = it },
                        placeholder = { Text("Full Address / Landmark", color = Color.White.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BotBubbleGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (placeLabel.isNotBlank() && placeAddress.isNotBlank()) {
                            savedPlaces = savedPlaces + SavedPlace(
                                id = java.util.UUID.randomUUID().toString(),
                                title = placeLabel,
                                address = placeAddress,
                                icon = Icons.Filled.Place
                            )
                            showAddPlaceDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BotBubbleGreen),
                    enabled = placeLabel.isNotBlank() && placeAddress.isNotBlank()
                ) {
                    Text("Add Place", color = BrandPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlaceDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = BrandPurpleDark
        )
    }

    // 4. Add Emergency Contact Dialog
    if (showAddContactDialog) {
        var contactName by remember { mutableStateOf("") }
        var contactRelation by remember { mutableStateOf("") }
        var contactPhone by remember { mutableStateOf("+232 ") }

        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("Add Emergency Contact", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        placeholder = { Text("Contact Name", color = Color.White.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BotBubbleGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contactRelation,
                        onValueChange = { contactRelation = it },
                        placeholder = { Text("Relationship (e.g., Sister, Colleague)", color = Color.White.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BotBubbleGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        placeholder = { Text("Phone Number", color = Color.White.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BotBubbleGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contactName.isNotBlank() && contactPhone.isNotBlank()) {
                            emergencyContacts = emergencyContacts + EmergencyContact(
                                id = java.util.UUID.randomUUID().toString(),
                                name = contactName,
                                relation = contactRelation.ifBlank { "Contact" },
                                phone = contactPhone
                            )
                            showAddContactDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BotBubbleGreen),
                    enabled = contactName.isNotBlank() && contactPhone.length > 5
                ) {
                    Text("Save Contact", color = BrandPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = BrandPurpleDark
        )
    }

    // 5. Link Mobile Money Dialog
    if (showLinkMomoDialog) {
        var selectedProvider by remember { mutableStateOf("Orange Money") }
        var momoNumber by remember { mutableStateOf("+232 ") }

        AlertDialog(
            onDismissRequest = { showLinkMomoDialog = false },
            title = { Text("Link Mobile Money", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Select Mobile Network Operator:", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Orange Money", "Africell Afrimoney").forEach { prov ->
                            val isSel = selectedProvider == prov
                            Surface(
                                onClick = { selectedProvider = prov },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) BotBubbleGreen else Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    prov,
                                    color = if (isSel) BrandPurple else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = momoNumber,
                        onValueChange = { momoNumber = it },
                        placeholder = { Text("Account Phone Number", color = Color.White.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BotBubbleGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (momoNumber.length > 6) {
                            momoAccounts = momoAccounts + MobileMoneyAccount(
                                id = java.util.UUID.randomUUID().toString(),
                                provider = selectedProvider,
                                phone = momoNumber,
                                isPrimary = false
                            )
                            showLinkMomoDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BotBubbleGreen),
                    enabled = momoNumber.length > 6
                ) {
                    Text("Link Account", color = BrandPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkMomoDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = BrandPurpleDark
        )
    }

    // 6. Booking Receipt Modal
    selectedBookingForReceipt?.let { record ->
        AlertDialog(
            onDismissRequest = { selectedBookingForReceipt = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = BotBubbleGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Booking Receipt", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reference: ${record.id}", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Service:", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        Text(record.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Provider:", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        Text(record.providerName, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Status:", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        Text(record.status, color = BotBubbleGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Date & Time:", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        Text(record.dateTime, color = Color.White, fontSize = 14.sp)
                    }
                    if (record.destination != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Location:", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                            Text(record.destination, color = Color.White, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount Paid:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(record.fare, color = BotBubbleGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("Protected by Wurie Smart Escrow • Funds securely held until completion", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedBookingForReceipt = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BotBubbleGreen)
                ) {
                    Text("Close", color = BrandPurple, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = BrandPurpleDark
        )
    }

    // 7. Safety Alert Test Toast Dialog
    if (showSafetyTestToast) {
        AlertDialog(
            onDismissRequest = { showSafetyTestToast = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = BotBubbleGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Safety Protocol Tested", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Simulated SMS and GPS coordinate broadcast dispatched to your 2 registered emergency contacts:\n\n• Momodu Bah (+232 76 991 223)\n• Aminata Sesay (+232 88 123 456)\n\nIn a real emergency, tapping SOS during any ride shares your live transit route instantly.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSafetyTestToast = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BotBubbleGreen)
                ) {
                    Text("Dismiss", color = BrandPurple, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = BrandPurpleDark
        )
    }

    // 8. Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Log Out of WurieAI?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("You will need to sign in again to access your wallet, bookings, and verified identity.", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = BrandPurpleDark
        )
    }
}

@Composable
fun ProfileStat(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

@Composable
fun ProfileOptionRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f))
    }
}
