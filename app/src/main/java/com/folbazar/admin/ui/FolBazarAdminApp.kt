package com.folbazar.admin.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.folbazar.admin.data.*
import kotlinx.coroutines.launch

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val ORDER_STATUSES = listOf("pending", "processing", "shipped", "delivered", "cancelled")

@Composable
fun FolBazarAdminApp() {
    var loggedIn by remember { mutableStateOf(Session.isLoggedIn) }
    if (!loggedIn) {
        LoginScreen(onLoggedIn = { loggedIn = true })
        return
    }

    val context = LocalContext.current
    val nav = rememberNavController()
    val items = listOf(
        NavItem("dashboard", "ড্যাশবোর্ড", Icons.Default.Dashboard),
        NavItem("products", "পণ্য", Icons.Default.Inventory2),
        NavItem("orders", "অর্ডার", Icons.Default.ShoppingCart),
        NavItem("customers", "কাস্টমার", Icons.Default.People)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ফল বাজার Admin", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        Session.clear(context)
                        loggedIn = false
                    }) { Icon(Icons.Default.Logout, contentDescription = "লগআউট") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val current = nav.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = current == item.route,
                        onClick = { nav.navigate(item.route) { launchSingleTop = true } },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { Dashboard() }
            composable("products") { Products() }
            composable("orders") { Orders() }
            composable("customers") { Customers() }
        }
    }
}

// ---------------- Dashboard ----------------

@Composable
private fun Dashboard() {
    var productCount by remember { mutableStateOf("—") }
    var orderCount by remember { mutableStateOf("—") }
    var customerCount by remember { mutableStateOf("—") }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        val repo = Repository()
        val p = repo.products()
        val o = repo.orders()
        val c = repo.customers()
        productCount = p.getOrNull()?.size?.toString() ?: "—"
        orderCount = o.getOrNull()?.size?.toString() ?: "—"
        customerCount = c.getOrNull()?.size?.toString() ?: "—"
        error = p.exceptionOrNull()?.message ?: o.exceptionOrNull()?.message ?: c.exceptionOrNull()?.message
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("স্বাগতম 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("ফল বাজারের নিয়ন্ত্রণ কেন্দ্র")
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("মোট পণ্য", productCount, Icons.Default.Inventory2, Modifier.weight(1f))
                Stat("মোট অর্ডার", orderCount, Icons.Default.ShoppingBag, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("কাস্টমার", customerCount, Icons.Default.People, Modifier.weight(1f))
                Stat("সিঙ্ক", "লাইভ", Icons.Default.CloudDone, Modifier.weight(1f))
            }
        }
        item {
            OutlinedButton(onClick = { refreshKey++ }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("রিফ্রেশ করুন")
            }
        }
        if (error != null) {
            item { Text("Supabase সংযোগ সমস্যা: $error", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun Stat(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------- Products ----------------

@Composable
private fun Products() {
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Product?>(null) }

    fun reload() { refreshKey++ }

    LaunchedEffect(refreshKey) {
        loading = true
        Repository().products().fold(
            onSuccess = { products = it; error = null },
            onFailure = { error = it.message }
        )
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("পণ্য", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("দাম, স্টক ও ক্যাটাগরি")
            }
            FilledTonalButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("নতুন")
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            error != null -> Text("ডাটা লোড হয়নি: $error", color = MaterialTheme.colorScheme.error)
            products.isEmpty() -> Text("কোনো পণ্য পাওয়া যায়নি। 'নতুন' চেপে যোগ করুন।")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(products, key = { it.id }) { p ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (p.imageUrl != null) {
                                AsyncImage(
                                    model = p.imageUrl, contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(p.title, fontWeight = FontWeight.SemiBold)
                                Text("৳ ${p.price} • স্টক ${p.stock}" + (p.category?.let { " • $it" } ?: ""))
                                Text(if (p.active) "Active" else "Off", color = if (p.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { editing = p }) { Icon(Icons.Default.Edit, contentDescription = "এডিট") }
                            IconButton(onClick = { deleteTarget = p }) { Icon(Icons.Default.Delete, contentDescription = "ডিলিট") }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ProductEditDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { title, price, stock, category, imageUrl ->
                scope.launch {
                    Repository().addProduct(title, price, stock, category, imageUrl).fold(
                        onSuccess = { showAddDialog = false; reload() },
                        onFailure = { error = it.message; showAddDialog = false }
                    )
                }
            }
        )
    }

    editing?.let { p ->
        ProductEditDialog(
            initial = p,
            onDismiss = { editing = null },
            onSave = { title, price, stock, category, imageUrl ->
                scope.launch {
                    Repository().updateProduct(
                        p.copy(title = title, price = price, stock = stock, category = category, imageUrl = imageUrl)
                    ).fold(
                        onSuccess = { editing = null; reload() },
                        onFailure = { error = it.message; editing = null }
                    )
                }
            }
        )
    }

    deleteTarget?.let { p ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("পণ্য ডিলিট করবেন?") },
            text = { Text("\"${p.title}\" স্থায়ীভাবে মুছে যাবে।") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        Repository().deleteProduct(p.id).fold(
                            onSuccess = { deleteTarget = null; reload() },
                            onFailure = { error = it.message; deleteTarget = null }
                        )
                    }
                }) { Text("ডিলিট", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("বাতিল") } }
        )
    }
}

@Composable
private fun ProductEditDialog(
    initial: Product?,
    onDismiss: () -> Unit,
    onSave: (title: String, price: Double, stock: Int, category: String?, imageUrl: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var price by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var stock by remember { mutableStateOf(initial?.stock?.toString() ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl) }
    var uploading by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            CloudinaryClient(context).uploadImage(uri).fold(
                onSuccess = { url -> imageUrl = url; uploading = false },
                onFailure = { e -> formError = e.message; uploading = false }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "নতুন পণ্য" else "পণ্য এডিট করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("নাম") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(price, { price = it }, label = { Text("দাম (৳)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(stock, { stock = it }, label = { Text("স্টক") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("ক্যাটাগরি") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                if (imageUrl != null) {
                    AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(120.dp))
                }
                OutlinedButton(onClick = { pickImage.launch("image/*") }, enabled = !uploading, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (uploading) "আপলোড হচ্ছে…" else "ছবি বেছে নিন (Cloudinary)")
                }
                if (formError != null) Text(formError!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !uploading && title.isNotBlank() && price.toDoubleOrNull() != null,
                onClick = {
                    onSave(
                        title.trim(),
                        price.toDoubleOrNull() ?: 0.0,
                        stock.toIntOrNull() ?: 0,
                        category.trim().ifBlank { null },
                        imageUrl
                    )
                }
            ) { Text("সেভ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

// ---------------- Orders ----------------

@Composable
private fun Orders() {
    val scope = rememberCoroutineScope()
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    var statusTarget by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(refreshKey) {
        loading = true
        Repository().orders().fold(
            onSuccess = { orders = it; error = null },
            onFailure = { error = it.message }
        )
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("অর্ডার", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("অর্ডার স্ট্যাটাস ও কাস্টমার তথ্য")
        Spacer(Modifier.height(12.dp))
        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            error != null -> Text("লোড হয়নি: $error", color = MaterialTheme.colorScheme.error)
            orders.isEmpty() -> Text("এখনো কোনো অর্ডার নেই।")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(orders, key = { it.id }) { o ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("#${o.id.take(8)}", fontWeight = FontWeight.Bold)
                                Text(o.customer)
                                Text("৳ ${o.amount}")
                            }
                            AssistChip(onClick = { statusTarget = o }, label = { Text(o.status) })
                        }
                    }
                }
            }
        }
    }

    statusTarget?.let { o ->
        AlertDialog(
            onDismissRequest = { statusTarget = null },
            title = { Text("অর্ডার স্ট্যাটাস পরিবর্তন") },
            text = {
                Column {
                    ORDER_STATUSES.forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = o.status == s, onClick = {
                                scope.launch {
                                    Repository().updateOrderStatus(o.id, s).fold(
                                        onSuccess = {
                                            statusTarget = null
                                            orders = orders.map { if (it.id == o.id) it.copy(status = s) else it }
                                        },
                                        onFailure = { error = it.message; statusTarget = null }
                                    )
                                }
                            })
                            Text(s)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { statusTarget = null }) { Text("বন্ধ করুন") } }
        )
    }
}

// ---------------- Customers ----------------

@Composable
private fun Customers() {
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        Repository().customers().fold(
            onSuccess = { customers = it },
            onFailure = { error = it.message }
        )
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("কাস্টমার", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("কাস্টমার প্রোফাইল")
        Spacer(Modifier.height(12.dp))
        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            error != null -> Text("লোড হয়নি: $error", color = MaterialTheme.colorScheme.error)
            customers.isEmpty() -> Text("কোনো কাস্টমার পাওয়া যায়নি।")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(customers, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(c.name, fontWeight = FontWeight.SemiBold)
                            c.phone?.let { Text(it) }
                            c.address?.let { Text(it) }
                        }
                    }
                }
            }
        }
    }
}
