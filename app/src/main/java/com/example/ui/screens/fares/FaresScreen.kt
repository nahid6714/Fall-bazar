package com.example.ui.screens.fares

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Bus
import com.example.data.model.District
import com.example.data.model.Fare
import com.example.data.model.Route
import com.example.data.repository.BusRepository
import com.example.data.repository.DistrictRepository
import com.example.data.repository.FareRepository
import com.example.data.repository.RouteRepository
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaresScreen(
    fareRepository: FareRepository,
    busRepository: BusRepository,
    routeRepository: RouteRepository,
    districtRepository: DistrictRepository,
    onNavigateBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var fares by remember { mutableStateOf<List<Fare>>(emptyList()) }
    var buses by remember { mutableStateOf<List<Bus>>(emptyList()) }
    var routes by remember { mutableStateOf<List<Route>>(emptyList()) }
    var districts by remember { mutableStateOf<List<District>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var editingFare by remember { mutableStateOf<Fare?>(null) }
    var deletingFare by remember { mutableStateOf<Fare?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            val fRes = fareRepository.getFares()
            val bRes = busRepository.getBuses()
            val rRes = routeRepository.getRoutes()
            val dRes = districtRepository.getDistricts()
            isLoading = false

            fRes.fold(
                onSuccess = { fares = it },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: "ভাড়া ডাটা লোড ব্যর্থ") }
            )
            bRes.fold(
                onSuccess = { buses = it },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: "বাস ডাটা লোড ব্যর্থ") }
            )
            rRes.fold(
                onSuccess = { routes = it },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: "রুট ডাটা লোড ব্যর্থ") }
            )
            dRes.fold(
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
        val r = routeMap[routeId] ?: return "সাধারণ রুট"
        val from = districtMap[r.fromDistrictId]?.name ?: "ঢাকা"
        val to = districtMap[r.toDistrictId]?.name ?: "চট্টগ্রাম"
        return "$from ➔ $to"
    }

    val filtered = remember(fares, searchQuery, busMap) {
        fares.filter { f ->
            val busName = busMap[f.busId]?.name.orEmpty()
            searchQuery.isBlank() ||
                    busName.contains(searchQuery, ignoreCase = true) ||
                    f.fareType.contains(searchQuery, ignoreCase = true) ||
                    f.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BtbdTopAppBar(
                title = "ভাড়া তালিকা (Fare Management)",
                subtitle = "মোট ${filtered.size} টি ভাড়া তালিকাভুক্ত",
                onRefresh = { loadData() },
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "ভাড়া যোগ করুন")
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
                placeholder = { Text("বাসের নাম, ভাড়ার ধরন বা নোট খুঁজুন...") },
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
                    icon = Icons.Outlined.Payments,
                    title = "কোন ভাড়ার তথ্য পাওয়া যায়নি",
                    subtitle = "নতুন ভাড়ার হার তৈরি করতে '+' চাপুন।"
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
                                    Text(
                                        text = "৳${item.fareAmount.toInt()}",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (item.fareType.isNotBlank()) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(item.fareType) }
                                        )
                                    }
                                    if (item.effectiveDate.isNotBlank()) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("কার্যকর: ${item.effectiveDate}") }
                                        )
                                    }
                                }

                                if (item.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.notes,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                    val result = fareRepository.toggleActive(item)
                                                    result.fold(
                                                        onSuccess = { loadData() },
                                                        onFailure = { err -> snackbarHostState.showSnackbar(err.localizedMessage ?: "আপডেট ব্যর্থ হয়েছে") }
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { editingFare = item }) {
                                            Icon(Icons.Default.Edit, contentDescription = "এডিট")
                                        }
                                        IconButton(onClick = { deletingFare = item }) {
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

    if (showAddDialog || editingFare != null) {
        val current = editingFare ?: Fare()
        var busId by remember { mutableStateOf(if (current.busId.isNotBlank()) current.busId else buses.firstOrNull()?.id.orEmpty()) }
        var routeId by remember { mutableStateOf(if (current.routeId.isNotBlank()) current.routeId else routes.firstOrNull()?.id.orEmpty()) }
        var fareAmount by remember { mutableStateOf(if (current.fareAmount > 0) current.fareAmount.toInt().toString() else "") }
        var fareType by remember { mutableStateOf(current.fareType) }
        var effectiveDate by remember { mutableStateOf(if (current.effectiveDate.isNotBlank()) current.effectiveDate else "2026-01-01") }
        var notes by remember { mutableStateOf(current.notes) }
        var active by remember { mutableStateOf(current.active) }

        var busDropdownExpanded by remember { mutableStateOf(false) }
        var routeDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingFare = null
            },
            title = { Text(if (editingFare == null) "নতুন ভাড়া যুক্ত করুন" else "ভাড়া এডিট করুন") },
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

                    OutlinedTextField(
                        value = fareAmount,
                        onValueChange = { fareAmount = it },
                        label = { Text("ভাড়ার পরিমাণ (টাকা) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = fareType,
                        onValueChange = { fareType = it },
                        label = { Text("ভাড়ার ধরণ (যেমন: Regular, VIP Sleeper, Economy)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = effectiveDate,
                        onValueChange = { effectiveDate = it },
                        label = { Text("কার্যকরের তারিখ (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("অতিরিক্ত নোট / শর্তাবলী") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("সক্রিয় ভাড়া:")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (busId.isNotBlank() && routeId.isNotBlank() && fareAmount.isNotBlank()) {
                            coroutineScope.launch {
                                val toSave = current.copy(
                                    busId = busId,
                                    routeId = routeId,
                                    fareAmount = fareAmount.toDoubleOrNull() ?: 0.0,
                                    fareType = fareType.trim(),
                                    effectiveDate = effectiveDate.trim(),
                                    notes = notes.trim(),
                                    active = active
                                )
                                val result = fareRepository.saveFare(toSave)
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
                            editingFare = null
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddDialog = false
                    editingFare = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (deletingFare != null) {
        ConfirmDeleteDialog(
            title = "ভাড়া মুছে ফেলতে চান?",
            message = "এই ভাড়ার তালিকা মুছে ফেলা হবে।",
            onConfirm = {
                deletingFare?.let { f ->
                    coroutineScope.launch {
                        val result = fareRepository.deleteFare(f.id)
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
                deletingFare = null
            },
            onDismiss = { deletingFare = null }
        )
    }
}
