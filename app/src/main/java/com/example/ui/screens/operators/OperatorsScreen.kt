package com.example.ui.screens.operators

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BusOperator
import com.example.data.network.CloudinaryUploader
import com.example.data.repository.BusOperatorRepository
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorsScreen(
    operatorRepository: BusOperatorRepository,
    cloudinaryUploader: CloudinaryUploader,
    onNavigateBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var operators by remember { mutableStateOf<List<BusOperator>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var editingOperator by remember { mutableStateOf<BusOperator?>(null) }
    var deletingOperator by remember { mutableStateOf<BusOperator?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = operatorRepository.getOperators()
            isLoading = false
            res.fold(
                onSuccess = { operators = it },
                onFailure = { 
                    errorMessage = it.localizedMessage ?: "ডাটা লোড ব্যর্থ"
                    snackbarHostState.showSnackbar(it.localizedMessage ?: "ডাটা লোড ব্যর্থ") 
                }
            )
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val filtered = remember(operators, searchQuery) {
        operators.filter {
            searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery) ||
                    it.slug.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BtbdTopAppBar(
                title = "বাস অপারেটর (Bus Operators)",
                subtitle = "মোট ${filtered.size} টি পরিবহন কোম্পানি",
                onRefresh = { loadData() },
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "অপারেটর যোগ করুন")
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
                placeholder = { Text("অপারেটরের নাম, ফোন বা স্লাগ খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                BackendErrorView(
                    tableName = "bus_operators",
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
                    icon = Icons.Outlined.Business,
                    title = "কোন অপারেটর পাওয়া যায়নি",
                    subtitle = "নতুন বাস অপারেটর যুক্ত করতে '+' চাপুন।"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { op ->
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
                                    // Logo
                                    if (op.logo.isNotBlank()) {
                                        AsyncImage(
                                            model = op.logo,
                                            contentDescription = op.name,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Business,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = op.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            StatusBadge(status = if (op.active) "active" else "inactive")
                                        }
                                        if (op.phone.isNotBlank()) {
                                            Text(
                                                text = "হটলাইন: ${op.phone}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }
                                }

                                if (op.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = op.description,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                        maxLines = 2
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "সক্রিয় স্ট্যাটাস:",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = op.active,
                                            onCheckedChange = {
                                                coroutineScope.launch {
                                                    val result = operatorRepository.toggleActive(op)
                                                    result.fold(
                                                        onSuccess = { loadData() },
                                                        onFailure = { err -> snackbarHostState.showSnackbar(err.localizedMessage ?: "আপডেট ব্যর্থ হয়েছে") }
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { editingOperator = op }) {
                                            Icon(Icons.Default.Edit, contentDescription = "এডিট")
                                        }
                                        IconButton(onClick = { deletingOperator = op }) {
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

    if (showAddDialog || editingOperator != null) {
        val current = editingOperator ?: BusOperator()
        var name by remember { mutableStateOf(current.name) }
        var slug by remember {
            mutableStateOf(
                if (editingOperator != null) current.slug
                else name.lowercase().trim().replace(Regex("\\s+"), "-")
            )
        }
        var logoUrl by remember { mutableStateOf(current.logo) }
        var description by remember { mutableStateOf(current.description) }
        var phone by remember { mutableStateOf(current.phone) }
        var website by remember { mutableStateOf(current.website) }
        var facebookUrl by remember { mutableStateOf(current.facebookUrl) }
        var active by remember { mutableStateOf(current.active) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingOperator = null
            },
            title = { Text(if (editingOperator == null) "নতুন অপারেটর যুক্ত করুন" else "অপারেটর এডিট করুন") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImageUploadBox(
                        imageUrl = logoUrl,
                        cloudinaryUploader = cloudinaryUploader,
                        folder = com.example.data.network.CloudinaryFolders.OPERATOR_LOGOS,
                        label = "অপারেটরের লোগো (Cloudinary)",
                        onImageUploaded = { logoUrl = it }
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (editingOperator == null) {
                                slug = it.lowercase().trim().replace(Regex("\\s+"), "-")
                            }
                        },
                        label = { Text("অপারেটরের নাম *") },
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
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("হেল্পলাইন / ফোন নম্বর") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text("অফিসিয়াল ওয়েবসাইট") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = facebookUrl,
                        onValueChange = { facebookUrl = it },
                        label = { Text("ফেসবুক পেজ লিংক") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("বিবরণ") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
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
                                    slug = if (editingOperator != null && current.slug.isNotBlank()) current.slug else slug.trim(),
                                    logo = logoUrl,
                                    description = description.trim(),
                                    phone = phone.trim(),
                                    website = website.trim(),
                                    facebookUrl = facebookUrl.trim(),
                                    active = active
                                )
                                val result = operatorRepository.saveOperator(toSave)
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
                            editingOperator = null
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddDialog = false
                    editingOperator = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (deletingOperator != null) {
        ConfirmDeleteDialog(
            title = "অপারেটর মুছে ফেলতে চান?",
            message = "${deletingOperator?.name} স্থায়ীভাবে মুছে ফেলা হবে। এর সাথে সংযুক্ত বাসগুলো প্রভাবিত হতে পারে।",
            onConfirm = {
                deletingOperator?.let { op ->
                    coroutineScope.launch {
                        val result = operatorRepository.deleteOperator(op.id)
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
                deletingOperator = null
            },
            onDismiss = { deletingOperator = null }
        )
    }
}
