package com.example.ui.screens.minicoaches

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.MiniCoach
import com.example.data.network.CloudinaryUploader
import com.example.data.repository.MiniCoachRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniCoachesScreen(
    miniCoachRepository: MiniCoachRepository,
    cloudinaryUploader: CloudinaryUploader,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var coaches by remember { mutableStateOf<List<MiniCoach>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var editingCoach by remember { mutableStateOf<MiniCoach?>(null) }
    var deletingCoach by remember { mutableStateOf<MiniCoach?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            val res = miniCoachRepository.getMiniCoaches()
            isLoading = false
            res.fold(
                onSuccess = { coaches = it },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: "মিনি কোচ ডাটা লোড ব্যর্থ") }
            )
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val filtered = remember(coaches, searchQuery) {
        coaches.filter {
            searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.vehicleType.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BtbdTopAppBar(
                title = "মিনি কোচ ও রেন্টাল (Mini Coaches)",
                subtitle = "মোট ${filtered.size} টি ট্যুরিজম ও পিকনিক কোচ",
                onRefresh = { loadData() },
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "মিনি কোচ যোগ করুন")
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { loadData() },
            modifier = Modifier.fillMaxSize()
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("মডেল, গাড়ির ধরন বা ফোন নম্বর দিয়ে খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filtered.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.AirportShuttle,
                    title = "কোন মিনি কোচ পাওয়া যায়নি",
                    subtitle = "নতুন মিনি কোচ বা কোস্টার যোগ করতে '+' চাপুন।"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { coach ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (coach.image.isNotBlank()) {
                                        AsyncImage(
                                            model = coach.image,
                                            contentDescription = coach.name,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = coach.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.weight(1f)
                                            )
                                            StatusBadge(status = if (coach.active) "active" else "inactive")
                                        }

                                        Text(
                                            text = "${coach.vehicleType}  •  ${coach.capacity} আসন",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(if (coach.isAc) "❄️ AC কোচ" else "Non-AC") },
                                                modifier = Modifier.height(28.dp)
                                            )
                                            if (coach.perDayRate > 0) {
                                                AssistChip(
                                                    onClick = {},
                                                    label = { Text("প্রতিদিন: ৳${coach.perDayRate.toInt()}") },
                                                    modifier = Modifier.height(28.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (coach.perKmRate > 0) {
                                        Text(
                                            text = "প্রতি কিমি: ৳${coach.perKmRate.toInt()}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                                        )
                                    }
                                    if (coach.driverCharge > 0) {
                                        Text(
                                            text = "ড্রাইভার চার্জ: ৳${coach.driverCharge.toInt()}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                                        )
                                    }
                                }

                                if (coach.phone.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "বুকিং হটলাইন: ${coach.phone}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${coach.phone}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldPrimaryContainer)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = "কল",
                                                tint = EmeraldOnPrimaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("সক্রিয়:", style = MaterialTheme.typography.labelSmall)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = coach.active,
                                            onCheckedChange = {
                                                coroutineScope.launch {
                                                    val result = miniCoachRepository.toggleActive(coach)
                                                    result.fold(
                                                        onSuccess = { loadData() },
                                                        onFailure = { err -> snackbarHostState.showSnackbar(err.localizedMessage ?: "আপডেট ব্যর্থ হয়েছে") }
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { editingCoach = coach }) {
                                            Icon(Icons.Default.Edit, contentDescription = "এডিট")
                                        }
                                        IconButton(onClick = { deletingCoach = coach }) {
                                            Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    
        }}

    if (showAddDialog || editingCoach != null) {
        val current = editingCoach ?: MiniCoach()
        var name by remember { mutableStateOf(current.name) }
        var slug by remember {
            mutableStateOf(
                if (editingCoach != null) current.slug
                else name.lowercase().trim().replace(Regex("\\s+"), "-")
            )
        }
        var vehicleType by remember { mutableStateOf(current.vehicleType) }
        var capacity by remember { mutableStateOf(current.capacity.toString()) }
        var isAc by remember { mutableStateOf(current.isAc) }
        var imageUrl by remember { mutableStateOf(current.image) }
        var description by remember { mutableStateOf(current.description) }
        var perDayRate by remember { mutableStateOf(if (current.perDayRate > 0) current.perDayRate.toInt().toString() else "") }
        var perKmRate by remember { mutableStateOf(if (current.perKmRate > 0) current.perKmRate.toInt().toString() else "") }
        var driverCharge by remember { mutableStateOf(if (current.driverCharge > 0) current.driverCharge.toInt().toString() else "") }
        var extraDayRate by remember { mutableStateOf(if (current.extraDayRate > 0) current.extraDayRate.toInt().toString() else "") }
        var phone by remember { mutableStateOf(current.phone) }
        var active by remember { mutableStateOf(current.active) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingCoach = null
            },
            title = { Text(if (editingCoach == null) "নতুন মিনি কোচ যুক্ত করুন" else "মিনি কোচ এডিট করুন") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImageUploadBox(
                        imageUrl = imageUrl,
                        cloudinaryUploader = cloudinaryUploader,
                        folder = com.example.data.network.CloudinaryFolders.COACH_IMAGES,
                        label = "মিনি কোচের ছবি (Cloudinary)",
                        onImageUploaded = { imageUrl = it }
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (editingCoach == null) {
                                slug = it.lowercase().trim().replace(Regex("\\s+"), "-")
                            }
                        },
                        label = { Text("মিনি কোচের নাম *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = slug,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("স্লাগ (Slug)") },
                        supportingText = { Text("প্রথমবার সংরক্ষণ করার সময় স্বয়ংক্রিয়ভাবে তৈরি হবে এবং পরে পরিবর্তন হবে না") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = vehicleType,
                            onValueChange = { vehicleType = it },
                            label = { Text("মডেল (Toyota Coaster / ইত্যাদি)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = capacity,
                            onValueChange = { capacity = it },
                            label = { Text("আসন সংখ্যা") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = perDayRate,
                            onValueChange = { perDayRate = it },
                            label = { Text("দৈনিক ভাড়া (টাকা)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = perKmRate,
                            onValueChange = { perKmRate = it },
                            label = { Text("প্রতি কিমি (টাকা)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = driverCharge,
                            onValueChange = { driverCharge = it },
                            label = { Text("চালক খরচ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("বুকিং ফোন") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("শীতাতপ নিয়ন্ত্রিত (AC):")
                        Switch(checked = isAc, onCheckedChange = { isAc = it })
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("গাড়ির বিবরণ ও শর্তাবলী") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ওয়েবসাইটে সক্রিয় রাখুন:")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            coroutineScope.launch {
                                val toSave = current.copy(
                                    name = name.trim(),
                                    slug = if (editingCoach != null && current.slug.isNotBlank()) current.slug else slug.trim(),
                                    vehicleType = vehicleType.trim(),
                                    capacity = capacity.toIntOrNull() ?: 29,
                                    isAc = isAc,
                                    image = imageUrl,
                                    description = description.trim(),
                                    perDayRate = perDayRate.toDoubleOrNull() ?: 0.0,
                                    perKmRate = perKmRate.toDoubleOrNull() ?: 0.0,
                                    driverCharge = driverCharge.toDoubleOrNull() ?: 0.0,
                                    extraDayRate = extraDayRate.toDoubleOrNull() ?: 0.0,
                                    phone = phone.trim(),
                                    active = active
                                )
                                val result = miniCoachRepository.saveMiniCoach(toSave)
                                result.fold(
                                    onSuccess = {
                                        loadData()
                                        snackbarHostState.showSnackbar("সফলভাবে সংরক্ষণ করা হয়েছে")
                                    },
                                    onFailure = { err ->
                                        snackbarHostState.showSnackbar(err.localizedMessage ?: "সংরক্ষণ ব্যর্থ হয়েছে")
                                    }
                                )
                            }
                            showAddDialog = false
                            editingCoach = null
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddDialog = false
                    editingCoach = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (deletingCoach != null) {
        ConfirmDeleteDialog(
            title = "মিনি কোচ মুছে ফেলতে চান?",
            message = "${deletingCoach?.name} স্থায়ীভাবে মুছে ফেলা হবে।",
            onConfirm = {
                deletingCoach?.let { c ->
                    coroutineScope.launch {
                        val result = miniCoachRepository.deleteMiniCoach(c.id)
                        result.fold(
                            onSuccess = {
                                loadData()
                                snackbarHostState.showSnackbar("মুছে ফেলা হয়েছে")
                            },
                            onFailure = { err ->
                                snackbarHostState.showSnackbar(err.localizedMessage ?: "মুছে ফেলা যায়নি")
                            }
                        )
                    }
                }
                deletingCoach = null
            },
            onDismiss = { deletingCoach = null }
        )
    }
}
