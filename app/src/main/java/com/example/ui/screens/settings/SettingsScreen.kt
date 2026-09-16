package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.local.SessionManager
import com.example.data.network.AppUpdateInfo
import com.example.data.network.UpdateChecker
import com.example.data.repository.AdminRepository
import com.example.ui.components.BtbdTopAppBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    adminRepository: AdminRepository,
    onNavigateTo: (String) -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val adminState by adminRepository.currentAdmin.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var supabaseUrl by remember { mutableStateOf(adminRepository.getSupabaseUrl()) }
    var anonKey by remember { mutableStateOf(adminRepository.getSupabaseAnonKey()) }
    var cloudName by remember { mutableStateOf(adminRepository.getCloudinaryCloudName()) }
    var uploadPreset by remember { mutableStateOf(adminRepository.getCloudinaryPreset()) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(true) }

    // Theme preference (light / dark / system-auto)
    val themeMode by adminRepository.themeMode.collectAsState()

    // Manual "check for update" button state
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var manualUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BtbdTopAppBar(
                title = "অ্যাডমিন সেটিংস ও সংযোগ",
                subtitle = "সার্ভার ও সিস্টেম কনফিগারেশন"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = EmeraldOnPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = adminState?.name ?: "অ্যাডমিনিস্ট্রেটর",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = adminState?.email ?: "admin@busterminalbd.com",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "ভূমিকা: ${adminState?.role ?: "Super Admin"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "লগআউট",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Quick Modules Navigation
            Text(
                text = "সকল ম্যানেজমেন্ট মডিউল",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    SettingsNavigationItem("বুকিং তালিকা", Icons.Default.ConfirmationNumber) { onNavigateTo(Screen.Bookings.route) }
                    Divider()
                    SettingsNavigationItem("বাস ব্যবস্থাপনা", Icons.Default.DirectionsBus) { onNavigateTo(Screen.Buses.route) }
                    Divider()
                    SettingsNavigationItem("বাস অপারেটর", Icons.Default.Business) { onNavigateTo(Screen.Operators.route) }
                    Divider()
                    SettingsNavigationItem("রুট ও দূরত্ব", Icons.Default.AltRoute) { onNavigateTo(Screen.Routes.route) }
                    Divider()
                    SettingsNavigationItem("শিডিউল ও ট্রিপ", Icons.Default.Schedule) { onNavigateTo(Screen.Schedules.route) }
                    Divider()
                    SettingsNavigationItem("ভাড়া তালিকা", Icons.Default.Payments) { onNavigateTo(Screen.Fares.route) }
                    Divider()
                    SettingsNavigationItem("কাউন্টার ও ফোন", Icons.Default.Storefront) { onNavigateTo(Screen.Counters.route) }
                    Divider()
                    SettingsNavigationItem("মিনি কোচ রেন্টাল", Icons.Default.AirportShuttle) { onNavigateTo(Screen.MiniCoaches.route) }
                    Divider()
                    SettingsNavigationItem("ট্যুর প্যাকেজ", Icons.Default.Luggage) { onNavigateTo(Screen.TourPackages.route) }
                    Divider()
                    SettingsNavigationItem("জেলা তালিকা", Icons.Default.LocationCity) { onNavigateTo(Screen.Districts.route) }
                    if (adminState?.role == "super_admin") {
                        Divider()
                        SettingsNavigationItem("অ্যাডমিন ইউজার", Icons.Default.AdminPanelSettings) { onNavigateTo(Screen.Admins.route) }
                    }
                }
            }

            // Manual update-check dialog (mirrors the automatic one shown at app launch)
            manualUpdateInfo?.let { info ->
                com.example.ui.components.UpdateAvailableDialog(
                    info = info,
                    onDismiss = { manualUpdateInfo = null }
                )
            }

            // Theme (Light / Dark / Auto) & App Update
            Text(
                text = "থিম ও অ্যাপ আপডেট",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "থিম মোড",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    val themeOptions = listOf(
                        Triple(SessionManager.THEME_LIGHT, "লাইট", Icons.Default.LightMode),
                        Triple(SessionManager.THEME_DARK, "ডার্ক", Icons.Default.DarkMode),
                        Triple(SessionManager.THEME_SYSTEM, "অটো", Icons.Default.BrightnessAuto)
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themeOptions.forEachIndexed { index, (value, label, icon) ->
                            SegmentedButton(
                                selected = themeMode == value,
                                onClick = { adminRepository.setThemeMode(value) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    Text(
                        text = "'অটো' নির্বাচন করলে ফোনের সিস্টেম সেটিংস অনুযায়ী থিম নিজে থেকে পরিবর্তন হবে।",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "অ্যাপ ভার্সন",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "সংস্করণ ${BuildConfig.VERSION_NAME} (Build #${BuildConfig.APP_BUILD_NUMBER})",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    OutlinedButton(
                        onClick = {
                            isCheckingUpdate = true
                            coroutineScope.launch {
                                val info = UpdateChecker.checkForUpdate()
                                isCheckingUpdate = false
                                if (info != null) {
                                    manualUpdateInfo = info
                                } else {
                                    snackbarHostState.showSnackbar("আপনি সর্বশেষ ভার্সন ব্যবহার করছেন")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCheckingUpdate
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("চেক করা হচ্ছে...")
                        } else {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("নতুন আপডেট চেক করুন")
                        }
                    }
                }
            }

            // Supabase & Cloudinary Configuration — super_admin only, since changing these
            // values re-points the entire app at a different backend/image host.
            if (adminState?.role == "super_admin") {
            Text(
                text = "সরাসরি API ও ক্লাউড সংযোগ",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Supabase কনফিগারেশন",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = supabaseUrl,
                        onValueChange = { supabaseUrl = it },
                        label = { Text("Supabase URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = anonKey,
                        onValueChange = { anonKey = it },
                        label = { Text("Supabase Anon Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Cloudinary ইমেজ হোস্ট কনফিগারেশন",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = cloudName,
                        onValueChange = { cloudName = it },
                        label = { Text("Cloud Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uploadPreset,
                        onValueChange = { uploadPreset = it },
                        label = { Text("Upload Preset (Unsigned)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (testResult != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (testSuccess) StatusConfirmedBg else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = testResult.orEmpty(),
                                color = if (testSuccess) StatusConfirmed else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                adminRepository.updateConfig(supabaseUrl, anonKey, cloudName, uploadPreset)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("কনফিগারেশন সফলভাবে সংরক্ষিত হয়েছে")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("সংরক্ষণ করুন")
                        }

                        OutlinedButton(
                            onClick = {
                                isTestingConnection = true
                                testResult = null
                                adminRepository.updateConfig(supabaseUrl, anonKey, cloudName, uploadPreset)
                                coroutineScope.launch {
                                    val res = adminRepository.testConnection()
                                    isTestingConnection = false
                                    res.fold(
                                        onSuccess = {
                                            testSuccess = true
                                            testResult = it
                                        },
                                        onFailure = {
                                            testSuccess = false
                                            testResult = it.localizedMessage ?: "সংযোগ ব্যর্থ হয়েছে"
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Text("সংযোগ পরীক্ষা")
                            }
                        }
                    }
                }
            }
            }

            // Security Notice Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "নিরাপত্তা নীতি: অ্যাপে কখনো service_role গোপন কি ব্যবহৃত হয় না। সকল ডাটাবেজ অ্যাক্সেস Supabase Auth ও Row Level Security (RLS) দ্বারা সংরক্ষিত।",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("অ্যাডমিন একাউন্ট থেকে লগআউট করুন")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsNavigationItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}
