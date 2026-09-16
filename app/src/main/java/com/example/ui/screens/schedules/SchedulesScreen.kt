package com.example.ui.screens.schedules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Bus
import com.example.data.model.BusRoute
import com.example.data.model.District
import com.example.data.model.Route
import com.example.data.repository.BusRepository
import com.example.data.repository.DistrictRepository
import com.example.data.repository.RouteRepository
import com.example.data.repository.ScheduleRepository
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    scheduleRepository: ScheduleRepository,
    busRepository: BusRepository,
    routeRepository: RouteRepository,
    districtRepository: DistrictRepository,
    onNavigateBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var schedules by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var buses by remember { mutableStateOf<List<Bus>>(emptyList()) }
    var routes by remember { mutableStateOf<List<Route>>(emptyList()) }
    var districts by remember { mutableStateOf<List<District>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedBusFilter by remember { mutableStateOf("all") }
    var editingSchedule by remember { mutableStateOf<BusRoute?>(null) }
    var deletingSchedule by remember { mutableStateOf<BusRoute?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            val schedRes = scheduleRepository.getSchedules()
            val busRes = busRepository.getBuses()
            val routeRes = routeRepository.getRoutes()
            val distRes = districtRepository.getDistricts()
            isLoading = false

            schedRes.fold(
                onSuccess = { schedules = it },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: "শিডিউল লোড ব্যর্থ") }
            )
            busRes.fold(
                onSuccess = { buses = it },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: "বাস ডাটা লোড ব্যর্থ") }
            )
            routeRes.fold(
                onSuccess = { routes = it },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: "রুট ডাটা লোড ব্যর্থ") }
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
    val routeMap = remember(routes) { routes.associateBy { it.id } }

    fun getRouteName(routeId: String): String {
        val r = routeMap[routeId] ?: return "রুট"
        val from = districtMap[r.fromDistrictId]?.name ?: "ঢাকা"
        val to = districtMap[r.toDistrictId]?.name ?: "কক্সবাজার"
        return "$from ➔ $to"
    }

    val filtered = remember(schedules, searchQuery, selectedBusFilter, busMap) {
        schedules.filter { s ->
            val busName = busMap[s.busId]?.name.orEmpty()
            val matchesQuery = searchQuery.isBlank() ||
                    busName.contains(searchQuery, ignoreCase = true) ||
                    s.boardingPoint.contains(searchQuery, ignoreCase = true) ||
                    s.droppingPoint.contains(searchQuery, ignoreCase = true)
            val matchesBus = selectedBusFilter == "all" || s.busId == selectedBusFilter
            matchesQuery && matchesBus
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BtbdTopAppBar(
                title = "বাস শিডিউল ও রুট (Schedules)",
                subtitle = "মোট ${filtered.size} টি দৈনিক শিডিউল",
                onRefresh = { loadData() },
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "শিডিউল যোগ করুন")
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
                placeholder = { Text("বাসের নাম, বোর্ডিং বা ড্রপিং পয়েন্ট খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Bus Filter Chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedBusFilter == "all",
                        onClick = { selectedBusFilter = "all" },
                        label = { Text("সকল বাস") }
                    )
                }
                items(buses) { b ->
                    FilterChip(
                        selected = selectedBusFilter == b.id,
                        onClick = { selectedBusFilter = b.id },
                        label = { Text(b.name) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filtered.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.Schedule,
                    title = "কোন শিডিউল পাওয়া যায়নি",
                    subtitle = "নতুন বাস শিডিউল তৈরি করতে '+' চাপুন।"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        val bus = busMap[item.busId]
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bus?.name ?: "বাস নির্বাচনহীন",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = getRouteName(item.routeId),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                    StatusBadge(status = if (item.active) "active" else "inactive")
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ছাড়ার সময়: ${item.departureTime}  |  পৌঁছাবে: ${item.arrivalTime}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                        )
                                    }
                                    Text(
                                        text = "ভাড়া: ৳${item.fare.toInt()}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "বোর্ডিং: ${item.boardingPoint} ➔ ড্রপিং: ${item.droppingPoint}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )

                                if (item.serviceDays.isNotBlank()) {
                                    Text(
                                        text = "সার্ভিস দিন: ${item.serviceDays}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
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
                                                    val result = scheduleRepository.toggleActive(item)
                                                    result.fold(
                                                        onSuccess = { loadData() },
                                                        onFailure = { err -> snackbarHostState.showSnackbar(err.localizedMessage ?: "আপডেট ব্যর্থ হয়েছে") }
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { editingSchedule = item }) {
                                            Icon(Icons.Default.Edit, contentDescription = "এডিট")
                                        }
                                        IconButton(onClick = { deletingSchedule = item }) {
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

    if (showAddDialog || editingSchedule != null) {
        val current = editingSchedule ?: BusRoute()
        var busId by remember { mutableStateOf(if (current.busId.isNotBlank()) current.busId else buses.firstOrNull()?.id.orEmpty()) }
        var routeId by remember { mutableStateOf(if (current.routeId.isNotBlank()) current.routeId else routes.firstOrNull()?.id.orEmpty()) }
        var departureTime by remember { mutableStateOf(current.departureTime) }
        var arrivalTime by remember { mutableStateOf(current.arrivalTime) }
        var fare by remember { mutableStateOf(if (current.fare > 0) current.fare.toInt().toString() else "") }
        var boardingPoint by remember { mutableStateOf(current.boardingPoint) }
        var droppingPoint by remember { mutableStateOf(current.droppingPoint) }
        var serviceDays by remember { mutableStateOf(if (current.serviceDays.isNotBlank()) current.serviceDays else "প্রতিদিন") }
        var active by remember { mutableStateOf(current.active) }

        var busDropdownExpanded by remember { mutableStateOf(false) }
        var routeDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingSchedule = null
            },
            title = { Text(if (editingSchedule == null) "নতুন বাস শিডিউল তৈরি" else "শিডিউল এডিট") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bus Dropdown
                    Column {
                        Text("বাস নির্বাচন *", style = MaterialTheme.typography.labelSmall)
                        Box {
                            OutlinedButton(
                                onClick = { busDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(busMap[busId]?.name ?: "বাস নির্বাচন করুন")
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

                    // Route Dropdown
                    Column {
                        Text("রুট নির্বাচন *", style = MaterialTheme.typography.labelSmall)
                        Box {
                            OutlinedButton(
                                onClick = { routeDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(getRouteName(routeId))
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = routeDropdownExpanded,
                                onDismissRequest = { routeDropdownExpanded = false }
                            ) {
                                routes.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(getRouteName(r.id)) },
                                        onClick = {
                                            routeId = r.id
                                            routeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = departureTime,
                            onValueChange = { departureTime = it },
                            label = { Text("ছাড়ার সময় *") },
                            placeholder = { Text("10:30 PM") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = arrivalTime,
                            onValueChange = { arrivalTime = it },
                            label = { Text("পৌঁছানোর সময় *") },
                            placeholder = { Text("06:00 AM") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = fare,
                        onValueChange = { fare = it },
                        label = { Text("ভাড়া (টাকা) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = boardingPoint,
                        onValueChange = { boardingPoint = it },
                        label = { Text("বোর্ডিং পয়েন্ট / কাউন্টার") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = droppingPoint,
                        onValueChange = { droppingPoint = it },
                        label = { Text("ড্রপিং পয়েন্ট / গন্তব্য স্টপেজ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = serviceDays,
                        onValueChange = { serviceDays = it },
                        label = { Text("চলাচলের দিন (যেমন: প্রতিদিন, শুক্রবার বাদে)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("সক্রিয় শিডিউল:")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (busId.isNotBlank() && routeId.isNotBlank()) {
                            coroutineScope.launch {
                                val toSave = current.copy(
                                    busId = busId,
                                    routeId = routeId,
                                    departureTime = departureTime.trim(),
                                    arrivalTime = arrivalTime.trim(),
                                    fare = fare.toDoubleOrNull() ?: 0.0,
                                    boardingPoint = boardingPoint.trim(),
                                    droppingPoint = droppingPoint.trim(),
                                    serviceDays = serviceDays.trim(),
                                    active = active
                                )
                                val result = scheduleRepository.saveSchedule(toSave)
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
                            editingSchedule = null
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddDialog = false
                    editingSchedule = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (deletingSchedule != null) {
        ConfirmDeleteDialog(
            title = "শিডিউলটি মুছে ফেলতে চান?",
            message = "এই ট্রিপ শিডিউলটি তালিকা থেকে সরানো হবে।",
            onConfirm = {
                deletingSchedule?.let { s ->
                    coroutineScope.launch {
                        val result = scheduleRepository.deleteSchedule(s.id)
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
                deletingSchedule = null
            },
            onDismiss = { deletingSchedule = null }
        )
    }
}
