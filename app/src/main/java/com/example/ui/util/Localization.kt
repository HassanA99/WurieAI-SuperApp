package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage {
    ENGLISH,
    FULANI_ADLAM,
    FRENCH
}

data class AppStrings(
    val appName: String,
    val services: String,
    val activity: String,
    val wallet: String,
    val profile: String,
    val nexusOnline: String,
    val awaitingDirective: String,
    val typeOrSpeak: String,
    val settings: String,
    val language: String,
    val notifications: String,
    val security: String,
    val help: String,
    val logout: String,
    // Explore Screen
    val exploreTitle: String,
    val marketPrices: String,
    val bookArtisan: String,
    val rideKeke: String,
    val education: String,
    val health: String,
    val agriculture: String,
    val recentActivity: String
)

val englishStrings = AppStrings(
    appName = "WurieAI",
    services = "Explore",
    activity = "Activity",
    wallet = "Wallet",
    profile = "Profile",
    nexusOnline = "Welcome Explorer",
    awaitingDirective = "What can I help you achieve today?",
    typeOrSpeak = "Type or speak...",
    settings = "Settings",
    language = "Language",
    notifications = "Notifications",
    security = "Privacy & Security",
    help = "Help & Support",
    logout = "Log Out",
    exploreTitle = "Explore Services",
    marketPrices = "Market Prices",
    bookArtisan = "Hire Provider",
    rideKeke = "Ride (Keke)",
    education = "Education",
    health = "Healthcare",
    agriculture = "Agriculture",
    recentActivity = "Recent Activity"
)

// ADLaM Translations (Fulani)
val adlamStrings = AppStrings(
    appName = "𞤏𞤵𞤪𞤭𞤀𞤋", // WurieAI
    services = "𞤑𞤵𞤵𞤺𞤢𞤤", // Explore
    activity = "𞤘𞤮𞤤𞤤𞤫", // Activity
    wallet = "𞤐𞤶𞤢𞤱𞤣𞤭", // Wallet
    profile = "𞤅𞤭𞤬𞤢𞤢", // Profile
    nexusOnline = "𞤐𞤶𞤢𞤦𞤢𞤼𞤵 𞤉𞤿𞤨𞤤𞤮𞤪𞤫𞤪", // Welcome Explorer
    awaitingDirective = "𞤖𞤮𞤤 𞤳𞤮 𞤥𞤦𞤢𞤱𞤥𞤭 𞤱𞤢𞤤𞤤𞤵𞤣𞤫 𞤥𞤢 𞤸𞤢𞤲𞤣𞤫?", // What can I help you achieve today?
    typeOrSpeak = "𞤏𞤭𞤲𞤣𞤵 𞤥𞤢𞤢 𞤸𞤢𞤤𞤢...", // Type or speak
    settings = "𞤉𞤩𞤩𞤮𞤮𞤶𞤭", // Settings
    language = "𞤊𞤭𞤤𞤢 𞤖𞤢𞤤𞤢", // Language
    notifications = "𞤑𞤢𞤦𞤢𞤪𞤵𞤶𞤭", // Notifications
    security = "𞤑𞤭𞤧𞤢𞤤", // Security
    help = "𞤄𞤢𞤤𞤤𞤢𞤤", // Help
    logout = "𞤒𞤢𞤤𞤼𞤵", // Log Out
    exploreTitle = "𞤒𞤭𞤴𞤢𞤲𞤣𞤫 𞤅𞤫𞤪𞤱𞤭𞤧", // Explore Services
    marketPrices = "𞤆𞤪𞤭𞤶𞤭 𞤃𞤢𞤪𞤧𞤫", // Market Prices
    bookArtisan = "𞤐𞤮𞤣𞤣𞤵 𞤘𞤮𞤤𞤤𞤮𞤱𞤮", // Book Artisan
    rideKeke = "𞤑𞤫𞤳𞤫", // Ride (Keke)
    education = "𞤔𞤢𞤲𞤣𞤫", // Education
    health = "𞤕𞤫𞤤𞤤𞤢𞤤", // Healthcare
    agriculture = "𞤐𞤣𞤫𞤥𞤢", // Agriculture
    recentActivity = "𞤘𞤮𞤤𞤤𞤫 𞤇𞤢𞤣𞤭𞤲𞤣𞤫" // Recent Activity
)

// French Translations
val frenchStrings = AppStrings(
    appName = "WurieAI",
    services = "Explorer",
    activity = "Activité",
    wallet = "Portefeuille",
    profile = "Profil",
    nexusOnline = "Bienvenue Explorateur",
    awaitingDirective = "Comment puis-je vous aider aujourd'hui ?",
    typeOrSpeak = "Tapez ou parlez...",
    settings = "Paramètres",
    language = "Langue",
    notifications = "Notifications",
    security = "Confidentialité et sécurité",
    help = "Aide et support",
    logout = "Déconnexion",
    exploreTitle = "Explorer les services",
    marketPrices = "Prix du marché",
    bookArtisan = "Trouver un prestataire",
    rideKeke = "Trajet (Keke)",
    education = "Éducation",
    health = "Soins de santé",
    agriculture = "Agriculture",
    recentActivity = "Activité récente"
)

val LocalAppStrings = compositionLocalOf { englishStrings }

@Composable
fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    val strings = when (language) {
        AppLanguage.ENGLISH -> englishStrings
        AppLanguage.FULANI_ADLAM -> adlamStrings
        AppLanguage.FRENCH -> frenchStrings
    }
    
    // ADLaM is a Right-To-Left (RTL) script. We override the layout direction.
    val layoutDirection = when (language) {
        AppLanguage.ENGLISH -> LayoutDirection.Ltr
        AppLanguage.FULANI_ADLAM -> LayoutDirection.Rtl
        AppLanguage.FRENCH -> LayoutDirection.Ltr
    }

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        content()
    }
}
