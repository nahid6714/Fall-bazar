package com.example.ui.screens.tours

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.TourPackage
import com.example.data.network.CloudinaryUploader
import com.example.data.repository.TourPackageRepository
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourPackagesScreen(
    tourPackageRepository: TourPackageRepository,
    cloudinaryUploader: CloudinaryUploader,
    onNavigateBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var packages by remember { mutableStateOf<List<TourPackage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var editingPackage by remember { mutableStateOf<TourPackage?>(null) }
    var deletingPackage by remember { mutableStateOf<TourPackage?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            val res = tourPackageRepository.getTourPackages()
            isLoading = false
            res.fold(
                onSuccess = { packages = it },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: "ট্যুর প্যাকেজ লোড ব্যর্থ") }
            )
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val filtered = remember(packages, searchQuery) {
        packages.filter {
            searchQuery.isBlank() ||
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.destination.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BtbdTopAppBar(
                title = "ট্যুর প্যাকেজ (Tour Packages)",
                subtitle = "মোট ${filtered.size} টি ভ্রমণ প্যাকেজ",
                onRefresh = { loadData() },
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "প্যাকেজ যোগ করুন")
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
                placeholder = { Text("প্যাকেজের নাম বা গন্তব্য খুঁজুন...") },
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
                    icon = Icons.Outlined.Luggage,
                    title = "কোন ট্যুর প্যাকেজ পাওয়া যায়নি",
                    subtitle = "নতুন প্যাকেজ তৈরি করতে '+' চাপুন।"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (item.image.isNotBlank()) {
                                    AsyncImage(
                                        model = item.image,
                                        contentDescription = item.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusBadge(status = if (item.active) "active" else "inactive")
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📍 ${item.destination}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "৳${item.pricePerPerson.toInt()}/জন",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("${item.durationDays} দিন, ${item.durationNights} রাত") }
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("নূন্যতম ${item.minimumPeople} জন") }
                                    )
                                }

                                if (item.shortDescription.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.shortDescription,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                        maxLines = 2
                                    )
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
                                            checked = item.active,
                                            onCheckedChange = {
                                                coroutineScope.launch {
                                                    val result = tourPackageRepository.toggleActive(item)
                                                    result.fold(
                                                        onSuccess = { loadData() },
                                                        onFailure = { err -> snackbarHostState.showSnackbar(err.localizedMessage ?: "আপডেট ব্যর্থ হয়েছে") }
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { editingPackage = item }) {
                                            Icon(Icons.Default.Edit, contentDescription = "এডিট")
                                        }
                                        IconButton(onClick = { deletingPackage = item }) {
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

    if (showAddDialog || editingPackage != null) {
        val current = editingPackage ?: TourPackage()
        var title by remember { mutableStateOf(current.title) }
        var slug by remember {
            mutableStateOf(
                if (editingPackage != null) current.slug
                else title.lowercase().trim().replace(Regex("\\s+"), "-")
            )
        }
        var destination by remember { mutableStateOf(current.destination) }
        var durationDays by remember { mutableStateOf(current.durationDays.toString()) }
        var durationNights by remember { mutableStateOf(current.durationNights.toString()) }
        var pricePerPerson by remember { mutableStateOf(if (current.pricePerPerson > 0) current.pricePerPerson.toInt().toString() else "") }
        var minimumPeople by remember { mutableStateOf(current.minimumPeople.toString()) }
        var imageUrl by remember { mutableStateOf(current.image) }
        var shortDescription by remember { mutableStateOf(current.shortDescription) }
        var fullDescription by remember { mutableStateOf(current.fullDescription) }
        var itinerary by remember { mutableStateOf(current.itinerary) }
        var included by remember { mutableStateOf(current.included) }
        var excluded by remember { mutableStateOf(current.excluded) }
        var terms by remember { mutableStateOf(current.terms) }
        var phone by remember { mutableStateOf(current.phone) }
        var active by remember { mutableStateOf(current.active) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingPackage = null
            },
            title = { Text(if (editingPackage == null) "নতুন ট্যুর প্যাকেজ যুক্ত করুন" else "প্যাকেজ এডিট করুন") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImageUploadBox(
                        imageUrl = imageUrl,
                        cloudinaryUploader = cloudinaryUploader,
                        folder = com.example.data.network.CloudinaryFolders.TOUR_IMAGES,
                        label = "প্যাকেজের কভার ছবি (Cloudinary)",
                        onImageUploaded = { imageUrl = it }
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (editingPackage == null) {
                                slug = it.lowercase().trim().replace(Regex("\\s+"), "-")
                            }
                        },
                        label = { Text("প্যাকেজের শিরোনাম *") },
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

                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("গন্তব্য * (যেমন: সাজেক ভ্যালি, কক্সবাজার)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = durationDays,
                            onValueChange = { durationDays = it },
                            label = { Text("দিন সংখ্যা") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = durationNights,
                            onValueChange = { durationNights = it },
                            label = { Text("রাত সংখ্যা") },
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
                            value = pricePerPerson,
                            onValueChange = { pricePerPerson = it },
                            label = { Text("মূল্য (জনপ্রতি টাকা) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = minimumPeople,
                            onValueChange = { minimumPeople = it },
                            label = { Text("নূন্যতম যাত্রী") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("বুকিং হটলাইন") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = shortDescription,
                        onValueChange = { shortDescription = it },
                        label = { Text("সংক্ষিপ্ত বর্ণনা") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    OutlinedTextField(
                        value = itinerary,
                        onValueChange = { itinerary = it },
                        label = { Text("ভ্রমণসূচি (Itinerary)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = included,
                        onValueChange = { included = it },
                        label = { Text("যা যা অন্তর্ভুক্ত (Included)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    OutlinedTextField(
                        value = excluded,
                        onValueChange = { excluded = it },
                        label = { Text("যা অন্তর্ভুক্ত নয় (Excluded)") },
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
                        if (title.isNotBlank()) {
                            coroutineScope.launch {
                                val toSave = current.copy(
                                    title = title.trim(),
                                    slug = if (editingPackage != null && current.slug.isNotBlank()) current.slug else slug.trim(),
                                    destination = destination.trim(),
                                    durationDays = durationDays.toIntOrNull() ?: 3,
                                    durationNights = durationNights.toIntOrNull() ?: 2,
                                    pricePerPerson = pricePerPerson.toDoubleOrNull() ?: 0.0,
                                    minimumPeople = minimumPeople.toIntOrNull() ?: 2,
                                    image = imageUrl,
                                    shortDescription = shortDescription.trim(),
                                    fullDescription = fullDescription.trim(),
                                    itinerary = itinerary.trim(),
                                    included = included.trim(),
                                    excluded = excluded.trim(),
                                    terms = terms.trim(),
                                    phone = phone.trim(),
                                    active = active
                                )
                                val result = tourPackageRepository.savePackage(toSave)
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
                            editingPackage = null
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddDialog = false
                    editingPackage = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (deletingPackage != null) {
        ConfirmDeleteDialog(
            title = "ট্যুর প্যাকেজ মুছে ফেলতে চান?",
            message = "${deletingPackage?.title} প্যাকেজটি স্থায়ীভাবে মুছে ফেলা হবে।",
            onConfirm = {
                deletingPackage?.let { p ->
                    coroutineScope.launch {
                        val result = tourPackageRepository.deletePackage(p.id)
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
                deletingPackage = null
            },
            onDismiss = { deletingPackage = null }
        )
    }
}
