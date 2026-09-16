package com.example.ui.screens.routes

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
import com.example.data.model.District
import com.example.data.model.Route
import com.example.data.repository.DistrictRepository
import com.example.data.repository.RouteRepository
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    routeRepository: RouteRepository,
    districtRepository: DistrictRepository,
    onNavigateBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var routes by remember { mutableStateOf<List<Route>>(emptyList()) }
    var districts by remember { mutableStateOf<List<District>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var editingRoute by remember { mutableStateOf<Route?>(null) }
    var deletingRoute by remember { mutableStateOf<Route?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            val routesRes = routeRepository.getRoutes()
            val distRes = districtRepository.getDistricts()
            isLoading = false
            routesRes.fold(
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

    val districtMap = remember(districts) { districts.associateBy { it.id } }

    val filtered = remember(routes, districtMap, searchQuery) {
        routes.filter { r ->
            val fromName = districtMap[r.fromDistrictId]?.name.orEmpty()
            val toName = districtMap[r.toDistrictId]?.name.orEmpty()
            searchQuery.isBlank() ||
                    fromName.contains(searchQuery, ignoreCase = true) ||
                    toName.contains(searchQuery, ignoreCase = true) ||
                    r.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BtbdTopAppBar(
                title = "রুট ব্যবস্থাপনা (Routes)",
                subtitle = "মোট ${filtered.size} টি মহাসড়ক রুট",
                onRefresh = { loadData() },
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "নতুন রুট যোগ করুন")
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
                placeholder = { Text("যাত্রা শুরুর স্থান বা গন্তব্য জেলা খুঁজুন...") },
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
                    icon = Icons.Outlined.AltRoute,
                    title = "কোন রুট পাওয়া যায়নি",
                    subtitle = "নতুন আন্তঃজেলা রুট যোগ করতে '+' বাটনে চাপুন।"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { route ->
                        val fromName = districtMap[route.fromDistrictId]?.name ?: "অজানা"
                        val toName = districtMap[route.toDistrictId]?.name ?: "অজানা"

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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsBus,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$fromName  ➔  $toName",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    StatusBadge(status = if (route.active) "active" else "inactive")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (route.distanceKm > 0) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("দূরত্ব: ${route.distanceKm.toInt()} কিমি") }
                                        )
                                    }
                                    if (route.estimatedDuration.isNotBlank()) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("সময়: ${route.estimatedDuration}") }
                                        )
                                    }
                                }

                                if (route.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = route.description,
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
                                            checked = route.active,
                                            onCheckedChange = {
                                                coroutineScope.launch {
                                                    val result = routeRepository.toggleActive(route)
                                                    result.fold(
                                                        onSuccess = { loadData() },
                                                        onFailure = { err -> snackbarHostState.showSnackbar(err.localizedMessage ?: "আপডেট ব্যর্থ হয়েছে") }
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { editingRoute = route }) {
                                            Icon(Icons.Default.Edit, contentDescription = "এডিট")
                                        }
                                        IconButton(onClick = { deletingRoute = route }) {
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

    if (showAddDialog || editingRoute != null) {
        val current = editingRoute ?: Route()
        var fromDistId by remember { mutableStateOf(if (current.fromDistrictId.isNotBlank()) current.fromDistrictId else districts.firstOrNull()?.id.orEmpty()) }
        var toDistId by remember { mutableStateOf(if (current.toDistrictId.isNotBlank()) current.toDistrictId else districts.getOrNull(1)?.id.orEmpty()) }
        var distanceKm by remember { mutableStateOf(if (current.distanceKm > 0) current.distanceKm.toString() else "") }
        var estimatedDuration by remember { mutableStateOf(current.estimatedDuration) }
        var description by remember { mutableStateOf(current.description) }
        var active by remember { mutableStateOf(current.active) }

        var fromDropdownExpanded by remember { mutableStateOf(false) }
        var toDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingRoute = null
            },
            title = { Text(if (editingRoute == null) "নতুন রুট তৈরি করুন" else "রুট এডিট করুন") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // From District
                    Column {
                        Text("যাত্রা শুরুর জেলা *", style = MaterialTheme.typography.labelSmall)
                        Box {
                            OutlinedButton(
                                onClick = { fromDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(districtMap[fromDistId]?.name ?: "জেলা নির্বাচন করুন")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = fromDropdownExpanded,
                                onDismissRequest = { fromDropdownExpanded = false }
                            ) {
                                districts.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d.name) },
                                        onClick = {
                                            fromDistId = d.id
                                            fromDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // To District
                    Column {
                        Text("গন্তব্য জেলা *", style = MaterialTheme.typography.labelSmall)
                        Box {
                            OutlinedButton(
                                onClick = { toDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(districtMap[toDistId]?.name ?: "জেলা নির্বাচন করুন")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = toDropdownExpanded,
                                onDismissRequest = { toDropdownExpanded = false }
                            ) {
                                districts.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d.name) },
                                        onClick = {
                                            toDistId = d.id
                                            toDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = distanceKm,
                        onValueChange = { distanceKm = it },
                        label = { Text("মোট দূরত্ব (কিলোমিটার)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = estimatedDuration,
                        onValueChange = { estimatedDuration = it },
                        label = { Text("আনুমানিক ভ্রমণ সময় (যেমন: ৫-৬ ঘণ্টা)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("মহাসড়কের বিবরণ / ভায়া স্টপেজ") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("সক্রিয় রুট:")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fromDistId.isNotBlank() && toDistId.isNotBlank()) {
                            coroutineScope.launch {
                                val toSave = current.copy(
                                    fromDistrictId = fromDistId,
                                    toDistrictId = toDistId,
                                    distanceKm = distanceKm.toDoubleOrNull() ?: 0.0,
                                    estimatedDuration = estimatedDuration.trim(),
                                    description = description.trim(),
                                    active = active
                                )
                                val result = routeRepository.saveRoute(toSave)
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
                            editingRoute = null
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddDialog = false
                    editingRoute = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (deletingRoute != null) {
        ConfirmDeleteDialog(
            title = "রুটটি মুছে ফেলতে চান?",
            message = "এই রুটটি স্থায়ীভাবে মুছে ফেলা হবে।",
            onConfirm = {
                deletingRoute?.let { r ->
                    coroutineScope.launch {
                        val result = routeRepository.deleteRoute(r.id)
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
                deletingRoute = null
            },
            onDismiss = { deletingRoute = null }
        )
    }
}
