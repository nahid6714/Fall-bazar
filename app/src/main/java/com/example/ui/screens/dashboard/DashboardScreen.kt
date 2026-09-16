package com.example.ui.screens.dashboard

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Booking
import com.example.data.model.DashboardStats
import com.example.data.repository.DashboardRepository
import com.example.ui.components.BtbdMetricCard
import com.example.ui.components.BtbdTopAppBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboardRepository: DashboardRepository,
    onNavigateTo: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var stats by remember { mutableStateOf(DashboardStats()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = dashboardRepository.getStats()
            isLoading = false
            result.fold(
                onSuccess = { stats = it },
                onFailure = { err -> errorMessage = err.localizedMessage }
            )
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            BtbdTopAppBar(
                title = "Bus Terminal BD Admin",
                subtitle = "কেন্দ্রীয় ম্যানেজমেন্ট ড্যাশবোর্ড",
                onRefresh = { loadData() }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { loadData() },
            modifier = Modifier.fillMaxSize()
        ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ড্যাশবোর্ড তথ্য লোড হচ্ছে...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.outline)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
            ) {
                // Booking Overview Row
                item {
                    Text(
                        text = "বুকিং স্ট্যাটাস সারসংক্ষেপ",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BtbdMetricCard(
                            title = "অপেক্ষমাণ বুকিং",
                            count = stats.pendingBookings,
                            icon = Icons.Outlined.HourglassTop,
                            containerColor = StatusPendingBg,
                            contentColor = StatusPending,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTo(Screen.Bookings.route) }
                        )
                        BtbdMetricCard(
                            title = "নিশ্চিত বুকিং",
                            count = stats.confirmedBookings,
                            icon = Icons.Outlined.CheckCircle,
                            containerColor = StatusConfirmedBg,
                            contentColor = StatusConfirmed,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateTo(Screen.Bookings.route) }
                        )
                    }
                }

                // Fleet & Service Metrics Grid
                item {
                    Text(
                        text = "ফ্লিট ও রুট ব্যবস্থাপনা পরিসংখ্যান",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BtbdMetricCard(
                                title = "মোট বাস",
                                count = stats.totalBuses,
                                icon = Icons.Outlined.DirectionsBus,
                                containerColor = EmeraldPrimaryContainer.copy(alpha = 0.5f),
                                contentColor = EmeraldPrimary,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateTo(Screen.Buses.route) }
                            )
                            BtbdMetricCard(
                                title = "বাস অপারেটর",
                                count = stats.totalOperators,
                                icon = Icons.Outlined.Business,
                                containerColor = TealSecondaryContainer.copy(alpha = 0.6f),
                                contentColor = TealSecondary,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateTo(Screen.Operators.route) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BtbdMetricCard(
                                title = "সক্রিয় রুট",
                                count = stats.totalRoutes,
                                icon = Icons.Outlined.AltRoute,
                                containerColor = Color(0xFFE0F2FE),
                                contentColor = Color(0xFF0284C7),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateTo(Screen.Routes.route) }
                            )
                            BtbdMetricCard(
                                title = "কাউন্টার সমূহ",
                                count = stats.totalCounters,
                                icon = Icons.Outlined.Storefront,
                                containerColor = Color(0xFFF3E8FF),
                                contentColor = Color(0xFF9333EA),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateTo(Screen.Counters.route) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BtbdMetricCard(
                                title = "মিনি কোচ",
                                count = stats.totalMiniCoaches,
                                icon = Icons.Outlined.AirportShuttle,
                                containerColor = AmberTertiaryContainer.copy(alpha = 0.6f),
                                contentColor = AmberTertiary,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateTo(Screen.MiniCoaches.route) }
                            )
                            BtbdMetricCard(
                                title = "ট্যুর প্যাকেজ",
                                count = stats.totalTourPackages,
                                icon = Icons.Outlined.Luggage,
                                containerColor = Color(0xFFFFEDD5),
                                contentColor = Color(0xFFEA580C),
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateTo(Screen.TourPackages.route) }
                            )
                        }
                    }
                }

                // Quick Navigation Shortcut Chips
                item {
                    Text(
                        text = "সকল এডমিন মডিউল দ্রুত ব্রাউজ",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        item {
                            QuickModuleChip(
                                title = "বাস",
                                icon = Icons.Default.DirectionsBus,
                                onClick = { onNavigateTo(Screen.Buses.route) }
                            )
                        }
                        item {
                            QuickModuleChip(
                                title = "অপারেটর",
                                icon = Icons.Default.Business,
                                onClick = { onNavigateTo(Screen.Operators.route) }
                            )
                        }
                        item {
                            QuickModuleChip(
                                title = "রুট",
                                icon = Icons.Default.AltRoute,
                                onClick = { onNavigateTo(Screen.Routes.route) }
                            )
                        }
                        item {
                            QuickModuleChip(
                                title = "শিডিউল",
                                icon = Icons.Default.Schedule,
                                onClick = { onNavigateTo(Screen.Schedules.route) }
                            )
                        }
                        item {
                            QuickModuleChip(
                                title = "ভাড়া তালিকা",
                                icon = Icons.Default.Payments,
                                onClick = { onNavigateTo(Screen.Fares.route) }
                            )
                        }
                        item {
                            QuickModuleChip(
                                title = "কাউন্টার",
                                icon = Icons.Default.Storefront,
                                onClick = { onNavigateTo(Screen.Counters.route) }
                            )
                        }
                        item {
                            QuickModuleChip(
                                title = "মিনি কোচ",
                                icon = Icons.Default.AirportShuttle,
                                onClick = { onNavigateTo(Screen.MiniCoaches.route) }
                            )
                        }
                        item {
                            QuickModuleChip(
                                title = "ট্যুর প্যাকেজ",
                                icon = Icons.Default.Luggage,
                                onClick = { onNavigateTo(Screen.TourPackages.route) }
                            )
                        }
                        item {
                            QuickModuleChip(
                                title = "জেলা",
                                icon = Icons.Default.LocationCity,
                                onClick = { onNavigateTo(Screen.Districts.route) }
                            )
                        }
                        item {
                            QuickModuleChip(
                                title = "অ্যাডমিন",
                                icon = Icons.Default.AdminPanelSettings,
                                onClick = { onNavigateTo(Screen.Admins.route) }
                            )
                        }
                    }
                }

                // Recent Bookings Header & List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সাম্প্রতিক বুকিং অনুরোধ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = { onNavigateTo(Screen.Bookings.route) }) {
                            Text("সব দেখুন")
                        }
                    }
                }

                if (stats.recentBookings.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.ConfirmationNumber,
                            title = "এখনো কোন নতুন বুকিং নেই",
                            subtitle = "গ্রাহক বুকিং করলে এখানে তালিকা প্রদর্শিত হবে।"
                        )
                    }
                } else {
                    items(stats.recentBookings) { booking ->
                        RecentBookingCard(
                            booking = booking,
                            onCallCustomer = { phone ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            },
                            onClick = { onNavigateTo(Screen.Bookings.route) }
                        )
                    }
                }
            }
        }
    
        }}
}

@Composable
fun QuickModuleChip(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
fun RecentBookingCard(
    booking: Booking,
    onCallCustomer: (String) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = booking.bookingId,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = booking.status)
                }
                Text(
                    text = "৳${booking.estimatedPrice.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.customerName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${booking.pickup} → ${booking.destination}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                if (booking.phone.isNotBlank()) {
                    IconButton(
                        onClick = { onCallCustomer(booking.phone) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "ফোন করুন",
                            tint = EmeraldOnPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "তারিখ: ${booking.travelDate} (${booking.passengers} সিট)",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                )
                Text(
                    text = when (booking.bookingType) {
                        "bus" -> "বাস টিকেট"
                        "mini_coach" -> "মিনি কোচ রেন্ট"
                        "tour_package" -> "ট্যুর প্যাকেজ"
                        else -> booking.bookingType
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
