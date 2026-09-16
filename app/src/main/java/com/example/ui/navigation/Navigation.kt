package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val titleBn: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val bottomLabel: String = titleBn // shorter label for the bottom nav bar, falls back to the full title
) {
    object Dashboard : Screen("dashboard", "ড্যাশবোর্ড", Icons.Outlined.Dashboard, Icons.Filled.Dashboard)
    object Bookings : Screen("bookings", "বুকিং সমূহ", Icons.Outlined.ConfirmationNumber, Icons.Filled.ConfirmationNumber)
    object Buses : Screen("buses", "বাস ব্যবস্থাপনা", Icons.Outlined.DirectionsBus, Icons.Filled.DirectionsBus, bottomLabel = "বাস")
    object Operators : Screen("operators", "বাস অপারেটর", Icons.Outlined.Business, Icons.Filled.Business)
    object Routes : Screen("routes", "রুট ব্যবস্থাপনা", Icons.Outlined.AltRoute, Icons.Filled.AltRoute)
    object Schedules : Screen("schedules", "বাস শিডিউল", Icons.Outlined.Schedule, Icons.Filled.Schedule)
    object Counters : Screen("counters", "কাউন্টার", Icons.Outlined.Storefront, Icons.Filled.Storefront)
    object Fares : Screen("fares", "ভাড়া তালিকা", Icons.Outlined.Payments, Icons.Filled.Payments)
    object MiniCoaches : Screen("mini_coaches", "মিনি কোচ", Icons.Outlined.AirportShuttle, Icons.Filled.AirportShuttle)
    object TourPackages : Screen("tour_packages", "ট্যুর প্যাকেজ", Icons.Outlined.Luggage, Icons.Filled.Luggage)
    object Districts : Screen("districts", "জেলা তালিকা", Icons.Outlined.LocationCity, Icons.Filled.LocationCity)
    object Admins : Screen("admins", "অ্যাডমিন ইউজার", Icons.Outlined.AdminPanelSettings, Icons.Filled.AdminPanelSettings)
    object Settings : Screen("settings", "সেটিংস", Icons.Outlined.Settings, Icons.Filled.Settings)
    object Login : Screen("login", "লগইন", Icons.Outlined.Lock, Icons.Filled.Lock)
}

val BottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Bookings,
    Screen.Buses,
    Screen.Counters,
    Screen.Settings
)
