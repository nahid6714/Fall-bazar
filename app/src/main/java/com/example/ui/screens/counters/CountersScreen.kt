package com.example.ui.screens.counters

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Bus
import com.example.data.model.Counter
import com.example.data.model.District
import com.example.data.network.CloudinaryUploader
import com.example.data.repository.BusRepository
import com.example.data.repository.CounterRepository
import com.example.data.repository.DistrictRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountersScreen(
    counterRepository: CounterRepository,
    busRepository: BusRepository,
    districtRepository: DistrictRepository,
    cloudinaryUploader: CloudinaryUploader,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var counters by remember { mutableStateOf<List<Counter>>(emptyList()) }
    var buses by remember { mutableStateOf<List<Bus>>(emptyList()) }
    var districts by remember { mutableStateOf<List<District>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDistrictId by remember { mutableStateOf("all") }
    var editingCounter by remember { mutableStateOf<Counter?>(null) }
    var deletingCounter by remember { mutableStateOf<Counter?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            val counterRes = counterRepository.getCounters()
            val busRes = busRepository.getBuses()
            val distRes = districtRepository.getDistricts()
            isLoading = false

            counterRes.fold(
                onSuccess = { counters = it },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: "কাউন্টার ডাটা লোড ব্যর্থ") }
            )
            busRes.fold(
                onSuccess = { buses = it },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: "বাস ডাটা লোড ব্যর্থ") }
            )
            distRes.fold(
                onSuccess = { districts = it },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: "জেলা ডাটা লোড ব্যর্থ") }
            )
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val busMap = remember(buses) { buses.associateBy { it.id } }
    val districtMap = remember(districts) { districts.associateBy { it.id } }

    val filtered = remember(counters, searchQuery, selectedDistrictId) {
        counters.filter { c ->
            val distName = districtMap[c.districtId]?.name.orEmpty()
            val busName = busMap[c.busId]?.name.orEmpty()
            val matchesQuery = searchQuery.isBlank() ||
                    c.counterName.contains(searchQuery, ignoreCase = true) ||
                    c.address.contains(searchQuery, ignoreCase = true) ||
                    c.phone.contains(searchQuery) ||
                    distName.contains(searchQuery, ignoreCase = true) ||
                    busName.contains(searchQuery, ignoreCase = true)

            val matchesDistrict = selectedDistrictId == "all" || c.districtId == selectedDistrictId
            matchesQuery && matchesDistrict
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BtbdTopAppBar(
                title = "বাস কাউন্টার ও বুকিং অফিস (Counters)",
                subtitle = "মোট ${filtered.size} টি কাউন্টার তথ্য",
                onRefresh = { loadData() },
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "কাউন্টার যোগ করুন")
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
                placeholder = { Text("কাউন্টারের নাম, ঠিকানা বা ফোন নম্বর খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // District Filter Row
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedDistrictId == "all",
                        onClick = { selectedDistrictId = "all" },
                        label = { Text("সকল জেলা") }
                    )
                }
                items(districts) { d ->
                    FilterChip(
                        selected = selectedDistrictId == d.id,
                        onClick = { selectedDistrictId = d.id },
                        label = { Text(d.name) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filtered.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Storefront,
                    title = "কোন কাউন্টার পাওয়া যায়নি",
                    subtitle = "নতুন কাউন্টার যোগ করতে '+' চাপুন।"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { counter ->
                        val bus = busMap[counter.busId]
                        val dist = districtMap[counter.districtId]

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
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Storefront,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(26.dp)
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
                                                text = counter.counterName,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.weight(1f)
                                            )
                                            StatusBadge(status = if (counter.active) "active" else "inactive")
                                        }

                                        Text(
                                            text = "${dist?.name ?: "বাংলাদেশ"} | বাস: ${bus?.name ?: "সাধারণ"}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "ঠিকানা: ${counter.address}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = counter.phone,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary
                                            )
                                        )
                                        if (counter.alternatePhone.isNotBlank()) {
                                            Text(
                                                text = ", ${counter.alternatePhone}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }

                                    if (counter.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${counter.phone}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldPrimaryContainer)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = "কল করুন",
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
                                            checked = counter.active,
                                            onCheckedChange = {
                                                coroutineScope.launch {
                                                    val result = counterRepository.toggleActive(counter)
                                                    result.fold(
                                                        onSuccess = { loadData() },
                                                        onFailure = { err -> snackbarHostState.showSnackbar(err.localizedMessage ?: "আপডেট ব্যর্থ হয়েছে") }
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { editingCounter = counter }) {
                                            Icon(Icons.Default.Edit, contentDescription = "এডিট")
                                        }
                                        IconButton(onClick = { deletingCounter = counter }) {
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

    if (showAddDialog || editingCounter != null) {
        val current = editingCounter ?: Counter()
        var counterName by remember { mutableStateOf(current.counterName) }
        var busId by remember { mutableStateOf(if (current.busId.isNotBlank()) current.busId else buses.firstOrNull()?.id.orEmpty()) }
        var districtId by remember { mutableStateOf(if (current.districtId.isNotBlank()) current.districtId else districts.firstOrNull()?.id.orEmpty()) }
        var address by remember { mutableStateOf(current.address) }
        var phone by remember { mutableStateOf(current.phone) }
        var alternatePhone by remember { mutableStateOf(current.alternatePhone) }
        var lat by remember { mutableStateOf(if (current.lat != 0.0) current.lat.toString() else "") }
        var lon by remember { mutableStateOf(if (current.lon != 0.0) current.lon.toString() else "") }
        var description by remember { mutableStateOf(current.description) }
        var active by remember { mutableStateOf(current.active) }

        var busDropdownExpanded by remember { mutableStateOf(false) }
        var distDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingCounter = null
            },
            title = { Text(if (editingCounter == null) "নতুন কাউন্টার যুক্ত করুন" else "কাউন্টার এডিট করুন") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = counterName,
                        onValueChange = { counterName = it },
                        label = { Text("কাউন্টারের নাম *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // District Dropdown
                    Column {
                        Text("জেলা নির্বাচন *", style = MaterialTheme.typography.labelSmall)
                        Box {
                            OutlinedButton(
                                onClick = { distDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(districtMap[districtId]?.name ?: "জেলা নির্বাচন করুন")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = distDropdownExpanded,
                                onDismissRequest = { distDropdownExpanded = false }
                            ) {
                                districts.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d.name) },
                                        onClick = {
                                            districtId = d.id
                                            distDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Bus Dropdown
                    Column {
                        Text("বাস / অপারেটর নির্বাচন", style = MaterialTheme.typography.labelSmall)
                        Box {
                            OutlinedButton(
                                onClick = { busDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(busMap[busId]?.name ?: "নির্দিষ্ট বাস নির্বাচন করুন")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = busDropdownExpanded,
                                onDismissRequest = { busDropdownExpanded = false }
                            ) {
                                buses.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b.name) },
                                        onClick = {
                                            busId = b.id
                                            busDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("বিস্তারিত ঠিকানা *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("ফোন নম্বর *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = alternatePhone,
                            onValueChange = { alternatePhone = it },
                            label = { Text("বিকল্প ফোন") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = lat,
                            onValueChange = { lat = it },
                            label = { Text("অক্ষাংশ (Lat)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lon,
                            onValueChange = { lon = it },
                            label = { Text("দ্রাঘিমাংশ (Lon)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("কাউন্টারের নোট বা বিবরণ") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("সক্রিয় কাউন্টার:")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (counterName.isNotBlank()) {
                            coroutineScope.launch {
                                val toSave = current.copy(
                                    busId = busId,
                                    districtId = districtId,
                                    counterName = counterName.trim(),
                                    address = address.trim(),
                                    phone = phone.trim(),
                                    alternatePhone = alternatePhone.trim(),
                                    lat = lat.toDoubleOrNull() ?: 0.0,
                                    lon = lon.toDoubleOrNull() ?: 0.0,
                                    description = description.trim(),
                                    active = active
                                )
                                val result = counterRepository.saveCounter(toSave)
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
                            editingCounter = null
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddDialog = false
                    editingCounter = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (deletingCounter != null) {
        ConfirmDeleteDialog(
            title = "কাউন্টার মুছে ফেলতে চান?",
            message = "${deletingCounter?.counterName} কাউন্টারটি স্থায়ীভাবে মুছে ফেলা হবে।",
            onConfirm = {
                deletingCounter?.let { c ->
                    coroutineScope.launch {
                        val result = counterRepository.deleteCounter(c.id)
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
                deletingCounter = null
            },
            onDismiss = { deletingCounter = null }
        )
    }
}
