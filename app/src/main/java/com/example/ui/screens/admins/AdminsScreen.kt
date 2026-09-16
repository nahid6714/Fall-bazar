package com.example.ui.screens.admins

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
import com.example.data.model.Admin
import com.example.data.repository.AdminRepository
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminsScreen(
    adminRepository: AdminRepository,
    onNavigateBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentAdmin by adminRepository.currentAdmin.collectAsState()

    // Defense in depth: SettingsScreen already hides the entry point for non-super_admin
    // roles, but this screen also refuses to render its content if reached any other way
    // (e.g. process restore to a saved back-stack entry).
    if (currentAdmin?.role != "super_admin") {
        Scaffold(
            topBar = {
                BtbdTopAppBar(
                    title = "অ্যাডমিন ব্যবস্থাপনা (Admins)",
                    subtitle = "সুরক্ষিত মডিউল",
                    onNavigateBack = onNavigateBack
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "এই মডিউলে প্রবেশের অনুমতি শুধুমাত্র super_admin রোলের জন্য।",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
        return
    }

    var admins by remember { mutableStateOf<List<Admin>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var editingAdmin by remember { mutableStateOf<Admin?>(null) }
    var deletingAdmin by remember { mutableStateOf<Admin?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadData() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = adminRepository.getAdmins()
            isLoading = false
            res.fold(
                onSuccess = { admins = it },
                onFailure = { err ->
                    errorMessage = err.localizedMessage ?: "ডাটাবেজ লোড ব্যর্থ"
                    snackbarHostState.showSnackbar(err.localizedMessage ?: "অ্যাডমিন তালিকা লোড ব্যর্থ")
                }
            )
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val filtered = remember(admins, searchQuery) {
        admins.filter {
            searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true) ||
                    it.role.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BtbdTopAppBar(
                title = "অ্যাডমিন ব্যবস্থাপনা (Admins)",
                subtitle = "মোট ${filtered.size} জন সিস্টেম অ্যাডমিন",
                onRefresh = { loadData() },
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "অ্যাডমিন যোগ করুন")
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
                placeholder = { Text("নাম, ইমেইল বা রোল দিয়ে খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                BackendErrorView(
                    tableName = "admins",
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
                    icon = Icons.Outlined.AdminPanelSettings,
                    title = "কোন অ্যাডমিন পাওয়া যায়নি",
                    subtitle = "নতুন অ্যাডমিন অ্যাকাউন্ট যুক্ত করতে '+' বাটনে চাপুন।"
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
                                        text = item.email,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = "ভূমিকা: ${item.role}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = item.active,
                                        onCheckedChange = {
                                            coroutineScope.launch {
                                                val result = adminRepository.toggleActive(item)
                                                result.fold(
                                                    onSuccess = { loadData() },
                                                    onFailure = { err -> snackbarHostState.showSnackbar(err.localizedMessage ?: "আপডেট ব্যর্থ হয়েছে") }
                                                )
                                            }
                                        }
                                    )
                                    IconButton(onClick = { editingAdmin = item }) {
                                        Icon(Icons.Default.Edit, contentDescription = "এডিট")
                                    }
                                    IconButton(onClick = { deletingAdmin = item }) {
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

    if (showAddDialog || editingAdmin != null) {
        val current = editingAdmin ?: Admin()
        var name by remember { mutableStateOf(current.name) }
        var email by remember { mutableStateOf(current.email) }
        var authUserId by remember { mutableStateOf(current.authUserId) }
        var role by remember { mutableStateOf(current.role) }
        var active by remember { mutableStateOf(current.active) }
        var validationError by remember { mutableStateOf<String?>(null) }

        val roles = listOf("super_admin", "admin", "operator_admin", "support")
        var roleDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingAdmin = null
            },
            title = { Text(if (editingAdmin == null) "নতুন অ্যাডমিন যুক্ত করুন" else "অ্যাডমিন এডিট করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("পূর্ণ নাম *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("ইমেইল ঠিকানা *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (editingAdmin == null) {
                        Text(
                            text = "লগইন করতে হলে এই ইউজারকে আগে Supabase Auth-এ (Dashboard → Authentication) সাইন আপ/ইনভাইট করাতে হবে। এরপর সেই ইউজারের Auth UUID এখানে দিন — এই UUID ছাড়া নতুন অ্যাডমিন কখনো লগইন করতে পারবে না।",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                        )
                        OutlinedTextField(
                            value = authUserId,
                            onValueChange = { authUserId = it; validationError = null },
                            label = { Text("Supabase Auth User UUID *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = validationError != null
                        )
                    }

                    if (validationError != null) {
                        Text(
                            text = validationError.orEmpty(),
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }

                    // Role selector
                    Column {
                        Text("অ্যাডমিন রোল / ভূমিকা", style = MaterialTheme.typography.labelSmall)
                        Box {
                            OutlinedButton(
                                onClick = { roleDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(role)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = roleDropdownExpanded,
                                onDismissRequest = { roleDropdownExpanded = false }
                            ) {
                                roles.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r) },
                                        onClick = {
                                            role = r
                                            roleDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

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
                        when {
                            name.isBlank() || email.isBlank() ->
                                validationError = "নাম ও ইমেইল আবশ্যক"
                            editingAdmin == null && authUserId.isBlank() ->
                                validationError = "নতুন অ্যাডমিনের জন্য Supabase Auth UUID আবশ্যক"
                            else -> {
                                coroutineScope.launch {
                                    // Login looks admins up by `id = eq.<authenticated user UUID>`,
                                    // so a brand-new row's id must be set to the Supabase Auth
                                    // UUID directly — otherwise the row would get an unrelated
                                    // auto-generated id and the new admin could never log in.
                                    val toSave = current.copy(
                                        id = if (editingAdmin == null) authUserId.trim() else current.id,
                                        name = name.trim(),
                                        email = email.trim(),
                                        authUserId = authUserId.trim(),
                                        role = role,
                                        active = active
                                    )
                                    val result = adminRepository.saveAdmin(toSave, isNew = editingAdmin == null)
                                    result.fold(
                                        onSuccess = { loadData() },
                                        onFailure = { err ->
                                            snackbarHostState.showSnackbar(err.localizedMessage ?: "সংরক্ষণ ব্যর্থ হয়েছে")
                                        }
                                    )
                                }
                                showAddDialog = false
                                editingAdmin = null
                            }
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddDialog = false
                    editingAdmin = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (deletingAdmin != null) {
        ConfirmDeleteDialog(
            title = "অ্যাডমিন মুছে ফেলতে চান?",
            message = "আপনি কি নিশ্চিত যে '${deletingAdmin?.name}' কে মুছে ফেলতে চান?",
            onConfirm = {
                deletingAdmin?.let {
                    coroutineScope.launch {
                        val result = adminRepository.deleteAdmin(it.id)
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
                deletingAdmin = null
            },
            onDismiss = { deletingAdmin = null }
        )
    }
}
