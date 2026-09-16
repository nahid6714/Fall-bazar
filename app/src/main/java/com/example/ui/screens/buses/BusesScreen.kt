package com.example.ui.screens.buses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Bus
import com.example.data.model.BusOperator
import com.example.data.network.CloudinaryUploader
import com.example.data.repository.BusOperatorRepository
import com.example.data.repository.BusRepository
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusesScreen(
    busRepository: BusRepository,
    operatorRepository: BusOperatorRepository,
    cloudinaryUploader: CloudinaryUploader,
    onNavigateBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var buses by remember { mutableStateOf<List<Bus>>(emptyList()) }
    var operators by remember { mutableStateOf<List<BusOperator>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedOperatorId by remember { mutableStateOf("all") }
    var editingBus by remember { mutableStateOf<Bus?>(null) }
    var deletingBus by remember { mutableStateOf<Bus?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val busesRes = busRepository.getBuses()
            val opsRes = operatorRepository.getOperators()
            isLoading = false
            busesRes.fold(
                onSuccess = { buses = it },
                onFailure = { 
                    errorMessage = it.localizedMessage ?: "বাস ডাটা লোড ব্যর্থ"
                    snackbarHostState.showSnackbar(it.localizedMessage ?: "বাস ডাটা লোড ব্যর্থ") 
                }
            )
            opsRes.fold(
                onSuccess = { operators = it },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: "অপারেটর ডাটা লোড ব্যর্থ") }
            )
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val operatorMap = remember(operators) { operators.associateBy { it.id } }

    val filtered = remember(buses, searchQuery, selectedOperatorId) {
        buses.filter {
            val matchesQuery = searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.busType.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
            val matchesOp = selectedOperatorId == "all" || it.operatorId == selectedOperatorId
            matchesQuery && matchesOp
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BtbdTopAppBar(
                title = "বাস ব্যবস্থাপনা (Buses)",
                subtitle = "মোট ${filtered.size} টি বাস নিবন্ধিত",
                onRefresh = { loadData() },
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "নতুন বাস যোগ করুন")
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
                placeholder = { Text("বাসের নাম, ধরণ বা ক্যাটাগরি খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Operator Filter Row
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedOperatorId == "all",
                        onClick = { selectedOperatorId = "all" },
                        label = { Text("সকল অপারেটর") }
                    )
                }
                items(operators) { op ->
                    FilterChip(
                        selected = selectedOperatorId == op.id,
                        onClick = { selectedOperatorId = op.id },
                        label = { Text(op.name) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filtered.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.DirectionsBus,
                    title = "কোন বাস পাওয়া যায়নি",
                    subtitle = "নতুন বাস যুক্ত করতে '+' বাটনে চাপুন।"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { bus ->
                        val op = operatorMap[bus.operatorId]
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (bus.image.isNotBlank()) {
                                        AsyncImage(
                                            model = bus.image,
                                            contentDescription = bus.name,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsBus,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = bus.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            StatusBadge(status = if (bus.active) "active" else "inactive")
                                        }

                                        Text(
                                            text = op?.name ?: "অপারেটর: সাধারণ",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(if (bus.isAc) "❄️ AC" else "Non-AC") },
                                                modifier = Modifier.height(28.dp)
                                            )
                                            AssistChip(
                                                onClick = {},
                                                label = { Text("${bus.seatCount} সিট") },
                                                modifier = Modifier.height(28.dp)
                                            )
                                            if (bus.category.isNotBlank()) {
                                                AssistChip(
                                                    onClick = {},
                                                    label = { Text(bus.category) },
                                                    modifier = Modifier.height(28.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (bus.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = bus.description,
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
                                            checked = bus.active,
                                            onCheckedChange = {
                                                coroutineScope.launch {
                                                    val result = busRepository.toggleActive(bus)
                                                    result.fold(
                                                        onSuccess = { loadData() },
                                                        onFailure = { err -> snackbarHostState.showSnackbar(err.localizedMessage ?: "আপডেট ব্যর্থ হয়েছে") }
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { editingBus = bus }) {
                                            Icon(Icons.Default.Edit, contentDescription = "এডিট")
                                        }
                                        IconButton(onClick = { deletingBus = bus }) {
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

    if (showAddDialog || editingBus != null) {
        val current = editingBus ?: Bus()
        var name by remember { mutableStateOf(current.name) }
        var slug by remember {
            mutableStateOf(
                if (editingBus != null) current.slug
                else name.lowercase().trim().replace(Regex("\\s+"), "-")
            )
        }
        var operatorId by remember { mutableStateOf(if (current.operatorId.isNotBlank()) current.operatorId else operators.firstOrNull()?.id.orEmpty()) }
        var category by remember { mutableStateOf(current.category) }
        var busType by remember { mutableStateOf(current.busType) }
        var description by remember { mutableStateOf(current.description) }
        var imageUrl by remember { mutableStateOf(current.image) }
        var phone by remember { mutableStateOf(current.phone) }
        var isAc by remember { mutableStateOf(current.isAc) }
        var seatCount by remember { mutableStateOf(current.seatCount.toString()) }
        var active by remember { mutableStateOf(current.active) }

        var opDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingBus = null
            },
            title = { Text(if (editingBus == null) "নতুন বাস যুক্ত করুন" else "বাস তথ্য এডিট") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImageUploadBox(
                        imageUrl = imageUrl,
                        cloudinaryUploader = cloudinaryUploader,
                        folder = com.example.data.network.CloudinaryFolders.BUS_IMAGES,
                        label = "বাসের ছবি (Cloudinary)",
                        onImageUploaded = { imageUrl = it }
                    )

                    // Operator Dropdown
                    Column {
                        Text("বাস অপারেটর নির্বাচন *", style = MaterialTheme.typography.labelSmall)
                        Box {
                            OutlinedButton(
                                onClick = { opDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(operatorMap[operatorId]?.name ?: "অপারেটর নির্বাচন করুন")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = opDropdownExpanded,
                                onDismissRequest = { opDropdownExpanded = false }
                            ) {
                                operators.forEach { op ->
                                    DropdownMenuItem(
                                        text = { Text(op.name) },
                                        onClick = {
                                            operatorId = op.id
                                            opDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (editingBus == null) {
                                slug = it.lowercase().trim().replace(Regex("\\s+"), "-")
                            }
                        },
                        label = { Text("বাসের নাম / মডেল *") },
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
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("ক্যাটাগরি (যেমন: First Class, Business, Sleeper)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = busType,
                        onValueChange = { busType = it },
                        label = { Text("বাসের ধরণ (যেমন: Scania Multi-Axle, Volvo, Hino)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = seatCount,
                            onValueChange = { seatCount = it },
                            label = { Text("মোট আসন সংখ্যা") },
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
                        Text("এয়ার কন্ডিশনার (AC Bus):")
                        Switch(checked = isAc, onCheckedChange = { isAc = it })
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("বাসের সুবিধা ও বিবরণ") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
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
                                    operatorId = operatorId,
                                    name = name.trim(),
                                    slug = if (editingBus != null && current.slug.isNotBlank()) current.slug else slug.trim(),
                                    category = category.trim(),
                                    busType = busType.trim(),
                                    description = description.trim(),
                                    image = imageUrl,
                                    phone = phone.trim(),
                                    isAc = isAc,
                                    seatCount = seatCount.toIntOrNull() ?: 36,
                                    active = active
                                )
                                val result = busRepository.saveBus(toSave)
                                result.fold(
                                    onSuccess = {
                                        loadData()
                                        snackbarHostState.showSnackbar("সফলভাবে সংরক্ষণ করা হয়েছে")
                                    },
                                    onFailure = { err ->
                                        snackbarHostState.showSnackbar(
                                            err.localizedMessage ?: "সংরক্ষণ ব্যর্থ হয়েছে"
                                        )
                                    }
                                )
                            }
                            showAddDialog = false
                            editingBus = null
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddDialog = false
                    editingBus = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (deletingBus != null) {
        ConfirmDeleteDialog(
            title = "বাসটি মুছে ফেলতে চান?",
            message = "${deletingBus?.name} বাসটি মুছে ফেলা হবে।",
            onConfirm = {
                deletingBus?.let { b ->
                    coroutineScope.launch {
                        val result = busRepository.deleteBus(b.id)
                        result.fold(
                            onSuccess = {
                                loadData()
                                snackbarHostState.showSnackbar("মুছে ফেলা হয়েছে")
                            },
                            onFailure = { err ->
                                snackbarHostState.showSnackbar(
                                    err.localizedMessage ?: "মুছে ফেলা যায়নি"
                                )
                            }
                        )
                    }
                }
                deletingBus = null
            },
            onDismiss = { deletingBus = null }
        )
    }
}
