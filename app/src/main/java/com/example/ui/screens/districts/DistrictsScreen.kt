package com.example.ui.screens.districts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.District
import com.example.data.repository.DistrictRepository
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistrictsScreen(
    districtRepository: DistrictRepository,
    onNavigateBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var districts by remember { mutableStateOf<List<District>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var editingDistrict by remember { mutableStateOf<District?>(null) }
    var deletingDistrict by remember { mutableStateOf<District?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = districtRepository.getDistricts()
            isLoading = false
            res.fold(
                onSuccess = { districts = it },
                onFailure = { 
                    errorMessage = it.localizedMessage ?: "ডাটা লোড ব্যর্থ"
                    snackbarHostState.showSnackbar(it.localizedMessage ?: "ডাটা লোড ব্যর্থ") 
                }
            )
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val filtered = remember(districts, searchQuery) {
        districts.filter {
            searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.division.contains(searchQuery, ignoreCase = true) ||
                    it.slug.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BtbdTopAppBar(
                title = "জেলা ব্যবস্থাপনা (Districts)",
                subtitle = "মোট ${filtered.size} টি জেলা তালিকাভুক্ত",
                onRefresh = { loadData() },
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "জেলা যোগ করুন")
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
                placeholder = { Text("জেলার নাম বা বিভাগ খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                BackendErrorView(
                    tableName = "districts",
                    errorMessage = errorMessage!!,
                    onRetry = { loadData() }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filtered.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.LocationCity,
                    title = "কোন জেলা পাওয়া যায়নি",
                    subtitle = "নতুন জেলা যুক্ত করতে '+' বাটনে চাপুন।"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        StatusBadge(status = if (item.active) "active" else "inactive")
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "বিভাগ: ${item.division} | স্লাগ: ${item.slug}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }

                                Row {
                                    Switch(
                                        checked = item.active,
                                        onCheckedChange = {
                                            coroutineScope.launch {
                                                val result = districtRepository.toggleActive(item)
                                                result.fold(
                                                    onSuccess = { loadData() },
                                                    onFailure = { err -> snackbarHostState.showSnackbar(err.localizedMessage ?: "আপডেট ব্যর্থ হয়েছে") }
                                                )
                                            }
                                        }
                                    )
                                    IconButton(onClick = { editingDistrict = item }) {
                                        Icon(Icons.Default.Edit, contentDescription = "এডিট")
                                    }
                                    IconButton(onClick = { deletingDistrict = item }) {
                                        Icon(Icons.Default.Delete, contentDescription = "ডিলিট", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    
        }}

    if (showAddDialog || editingDistrict != null) {
        val current = editingDistrict ?: District()
        var name by remember { mutableStateOf(current.name) }
        var division by remember { mutableStateOf(current.division) }
        var slug by remember {
            mutableStateOf(
                if (editingDistrict != null) current.slug
                else name.lowercase().trim().replace(Regex("\\s+"), "-")
            )
        }
        var active by remember { mutableStateOf(current.active) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingDistrict = null
            },
            title = { Text(if (editingDistrict == null) "নতুন জেলা যুক্ত করুন" else "জেলা এডিট করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (editingDistrict == null) {
                                slug = it.lowercase().trim().replace(Regex("\\s+"), "-")
                            }
                        },
                        label = { Text("জেলার নাম *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = division,
                        onValueChange = { division = it },
                        label = { Text("বিভাগ *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = slug,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("স্লাগ (URL Slug)" ) },
                        supportingText = { Text("প্রথমবার সংরক্ষণ করার সময় স্বয়ংক্রিয়ভাবে তৈরি হবে এবং পরে পরিবর্তন হবে না") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("সক্রিয় রাখুন:")
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
                                    division = division.trim(),
                                    slug = if (editingDistrict != null && current.slug.isNotBlank()) current.slug else slug.trim(),
                                    active = active
                                )
                                val result = districtRepository.saveDistrict(toSave)
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
                            editingDistrict = null
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddDialog = false
                    editingDistrict = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (deletingDistrict != null) {
        ConfirmDeleteDialog(
            title = "জেলা মুছে ফেলতে চান?",
            message = "${deletingDistrict?.name} জেলাটি তালিকা থেকে স্থায়ীভাবে অপসারণ করা হবে।",
            onConfirm = {
                deletingDistrict?.let { d ->
                    coroutineScope.launch {
                        val result = districtRepository.deleteDistrict(d.id)
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
                deletingDistrict = null
            },
            onDismiss = { deletingDistrict = null }
        )
    }
}
