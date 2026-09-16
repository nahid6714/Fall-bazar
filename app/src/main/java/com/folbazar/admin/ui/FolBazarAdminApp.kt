@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.folbazar.admin.ui

import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.folbazar.admin.BuildConfig
import com.folbazar.admin.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private data class NavItem(val route: String, val label: String, val icon: ImageVector)
private val ORDER_STATUSES = listOf("pending","confirmed","processing","packed","shipped","out_for_delivery","delivered","cancelled","returned")
private val PAYMENT_STATUSES = listOf("pending","paid","failed","refunded")
private val CUSTOMER_ROLES = listOf("customer","reseller","seller","admin")
private val COMPLAINT_STATUSES = listOf("open","in_review","resolved","closed","rejected")

@Composable
fun FolBazarAdminApp() {
    var loggedIn by remember { mutableStateOf(Session.isLoggedIn) }
    if (!loggedIn) { LoginScreen(onLoggedIn = { loggedIn = true }); return }
    val context = LocalContext.current
    val nav = rememberNavController()
    val items = listOf(
        NavItem("dashboard","ড্যাশবোর্ড",Icons.Default.Dashboard),
        NavItem("products","পণ্য",Icons.Default.Inventory2),
        NavItem("orders","অর্ডার",Icons.Default.ShoppingCart),
        NavItem("more","আরও",Icons.Default.MoreHoriz)
    )
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ফল বাজার Admin", fontWeight = FontWeight.Bold) }, actions = {
                IconButton(onClick = { Session.clear(context); loggedIn = false }) { Icon(Icons.Default.Logout, "লগআউট") }
            })
        },
        bottomBar = {
            NavigationBar {
                val current = nav.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { item -> NavigationBarItem(selected = current == item.route || (item.route == "more" && current in listOf("more","categories","customers","complaints","coupons","wishlist","settings")), onClick = { nav.navigate(item.route) { launchSingleTop = true } }, icon = { Icon(item.icon,null) }, label = { Text(item.label) }) }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { Dashboard(nav) }
            composable("products") { Products() }
            composable("orders") { Orders() }
            composable("more") { More(nav) }
            composable("categories") { Categories() }
            composable("customers") { Customers() }
            composable("complaints") { Complaints() }
            composable("coupons") { Coupons() }
            composable("wishlist") { Wishlist() }
            composable("settings") { SettingsScreen() }
        }
    }
}

@Composable private fun Dashboard(nav: NavHostController) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    LaunchedEffect(refresh) {
        val r = Repository(); val p=r.products(); val o=r.orders(); val c=r.customers(); val x=r.complaints()
        products=p.getOrNull().orEmpty(); orders=o.getOrNull().orEmpty(); customers=c.getOrNull().orEmpty(); complaints=x.getOrNull().orEmpty()
        error = p.exceptionOrNull()?.message ?: o.exceptionOrNull()?.message ?: c.exceptionOrNull()?.message ?: x.exceptionOrNull()?.message
    }
    val pending = orders.count { it.status in setOf("pending","confirmed","processing","packed") }
    val revenue = orders.filter { it.status == "delivered" }.sumOf { it.total }
    RefreshableList(refresh, { refresh++ }) {
        item { Text("স্বাগতম 👋", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold); Text("ফল বাজারের সম্পূর্ণ নিয়ন্ত্রণ কেন্দ্র") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) { Stat("পণ্য",products.size.toString(),Icons.Default.Inventory2,Modifier.weight(1f)); Stat("অর্ডার",orders.size.toString(),Icons.Default.ShoppingBag,Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) { Stat("কাস্টমার",customers.size.toString(),Icons.Default.People,Modifier.weight(1f)); Stat("Pending",pending.toString(),Icons.Default.Pending,Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) { Stat("Delivered Sales","৳ ${money(revenue)}",Icons.Default.Payments,Modifier.weight(1f)); Stat("অভিযোগ",complaints.count{it.status=="open"}.toString(),Icons.Default.ReportProblem,Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) { FilledTonalButton({ nav.navigate("products") },Modifier.weight(1f)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(4.dp));Text("পণ্য")}; FilledTonalButton({ nav.navigate("orders") },Modifier.weight(1f)){Icon(Icons.Default.ShoppingCart,null);Spacer(Modifier.width(4.dp));Text("অর্ডার")} } }
        item { OutlinedButton({ refresh++ }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh,null); Spacer(Modifier.width(6.dp)); Text("সব ডাটা রিফ্রেশ") } }
        error?.let { item { Text("Supabase: $it", color=MaterialTheme.colorScheme.error) } }
    }
}

@Composable private fun Stat(title:String,value:String,icon:ImageVector,modifier:Modifier){ Card(modifier){Column(Modifier.padding(14.dp)){Icon(icon,null,tint=MaterialTheme.colorScheme.primary);Text(title);Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}} }

@Composable private fun More(nav:NavHostController){
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("অ্যাডমিন ম্যানেজমেন্ট",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("ওয়েবসাইটের বাকি সব নিয়ন্ত্রণ এখান থেকে")}
        item{AdminAction("ক্যাটাগরি","ক্যাটাগরি যোগ, এডিট, active/off, delete",Icons.Default.Category){nav.navigate("categories")}}
        item{AdminAction("কাস্টমার","প্রোফাইল ও customer/reseller/seller/admin role",Icons.Default.People){nav.navigate("customers")}}
        item{AdminAction("অভিযোগ","অভিযোগ দেখা, নোট ও status পরিবর্তন",Icons.Default.ReportProblem){nav.navigate("complaints")}}
        item{AdminAction("কুপন / ডিসকাউন্ট","coupon code, percent/fixed discount, limit",Icons.Default.LocalOffer){nav.navigate("coupons")}}
        item{AdminAction("Wishlist","কোন পণ্য কতবার wishlist হয়েছে",Icons.Default.Favorite){nav.navigate("wishlist")}}
        item{AdminAction("সেটিংস / App Update","অ্যাপ আপডেট চেক, ডাউনলোড ও ইনস্টল",Icons.Default.Settings){nav.navigate("settings")}}
        item{Text("নিরাপত্তা: database RLS policy-ই চূড়ান্ত permission; app শুধু admin JWT দিয়ে কাজ করে.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}
@Composable private fun AdminAction(title:String,desc:String,icon:ImageVector,onClick:()->Unit){Card(Modifier.fillMaxWidth().clickable(onClick=onClick)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold);Text(desc,style=MaterialTheme.typography.bodySmall)};Icon(Icons.Default.ChevronRight,null)}}}

@Composable private fun Products(){
    val scope=rememberCoroutineScope(); var products by remember{mutableStateOf<List<Product>>(emptyList())}; var cats by remember{mutableStateOf<List<Category>>(emptyList())}; var loading by remember{mutableStateOf(true)}; var error by remember{mutableStateOf<String?>(null)}; var refresh by remember{mutableStateOf(0)}; var edit by remember{mutableStateOf<Product?>(null)}; var add by remember{mutableStateOf(false)}; var del by remember{mutableStateOf<Product?>(null)}
    fun reload(){refresh++}
    LaunchedEffect(refresh){loading=true;val r=Repository();val p=r.products();val c=r.categories();products=p.getOrNull().orEmpty();cats=c.getOrNull().orEmpty();error=p.exceptionOrNull()?.message?:c.exceptionOrNull()?.message;loading=false}
    Column(Modifier.fillMaxSize().padding(16.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("পণ্য",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("CRUD + stock + category + flags + Cloudinary")};FilledTonalButton({add=true}){Icon(Icons.Default.Add,null);Spacer(Modifier.width(4.dp));Text("নতুন")}};Spacer(Modifier.height(12.dp));when{loading->LinearProgressIndicator(Modifier.fillMaxWidth());error!=null->Text("ডাটা লোড হয়নি: $error",color=MaterialTheme.colorScheme.error);products.isEmpty()->Text("কোনো পণ্য নেই");else->RefreshableList(refresh,{refresh++}){items(products,key={it.id}){p->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){p.imageUrl?.let{AsyncImage(it,null,Modifier.size(56.dp))};Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.SemiBold);Text("৳ ${money(p.price)} • স্টক ${p.stock}");Text(p.categoryName?:"ক্যাটাগরি নেই",style=MaterialTheme.typography.bodySmall);Text(if(p.active)"Active" else "Off",color=if(p.active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)};IconButton({edit=p}){Icon(Icons.Default.Edit,"এডিট")};IconButton({del=p}){Icon(Icons.Default.Delete,"ডিলিট")}}}}}}
    }
    if(add)ProductDialog(null,cats,{add=false},{n,d,pr,op,st,ci,img,f,fl,h->scope.launch{Repository().addProduct(n,d,pr,op,st,ci,img,f,fl,h).fold({add=false;reload()},{error=it.message})}})
    edit?.let{p->ProductDialog(p,cats,{edit=null},{n,d,pr,op,st,ci,img,f,fl,h->scope.launch{Repository().updateProduct(p.copy(name=n,description=d,price=pr,oldPrice=op,stock=st,categoryId=ci,imageUrl=img,featured=f,flashSale=fl,hotDeal=h)).fold({edit=null;reload()},{error=it.message})}})}
    del?.let{p->Confirm("পণ্য ডিলিট করবেন?","${p.name} স্থায়ীভাবে মুছে যাবে.",{scope.launch{Repository().deleteProduct(p.id).fold({del=null;reload()},{error=it.message;del=null})}},{del=null})}
}


@Composable
private fun RefreshableList(
    refreshKey: Int,
    onRefresh: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) {
            delay(1200)
            refreshing = false
        }
    }

    PullToRefreshBox(
        state = rememberPullToRefreshState(),
        isRefreshing = refreshing,
        onRefresh = {
            if (!refreshing) {
                refreshing = true
                onRefresh()
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ProductDialog(
    initial: Product?,
    cats: List<Category>,
    onDismiss: () -> Unit,
    onSave: (String, String?, Double, Double?, Int, String?, String?, Boolean, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var price by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var old by remember { mutableStateOf(initial?.oldPrice?.toString() ?: "") }
    var stock by remember { mutableStateOf(initial?.stock?.toString() ?: "0") }
    var cat by remember { mutableStateOf(initial?.categoryId) }
    var image by remember { mutableStateOf(initial?.imageUrl) }
    var featured by remember { mutableStateOf(initial?.featured ?: false) }
    var flash by remember { mutableStateOf(initial?.flashSale ?: false) }
    var hot by remember { mutableStateOf(initial?.hotDeal ?: false) }
    var uploading by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            uploading = true
            scope.launch {
                CloudinaryClient(context).uploadImage(uri).fold(
                    { uploadedUrl -> image = uploadedUrl; uploading = false },
                    { e -> formError = e.message; uploading = false }
                )
            }
        }
    }
    val dialogTitle = if (initial == null) "নতুন পণ্য" else "পণ্য এডিট"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Field(name, { name = it }, "পণ্যের নাম")
                Field(price, { price = it }, "দাম (৳)", KeyboardType.Decimal)
                Field(old, { old = it }, "পুরনো দাম (৳)", KeyboardType.Decimal)
                Field(stock, { stock = it }, "স্টক", KeyboardType.Number)
                Field(desc, { desc = it }, "বিবরণ", single = false)
                Box {
                    OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(cats.firstOrNull { it.id == cat }?.name ?: "ক্যাটাগরি নির্বাচন")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        cats.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = { cat = category.id; menu = false }
                            )
                        }
                    }
                }
                image?.let { url ->
                    AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxWidth().height(130.dp))
                }
                OutlinedButton(
                    onClick = { picker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uploading
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (uploading) "আপলোড হচ্ছে…" else "Cloudinary থেকে ছবি")
                }
                SwitchRow("Featured", featured) { featured = it }
                SwitchRow("Flash sale", flash) { flash = it }
                SwitchRow("Hot deal", hot) { hot = it }
                formError?.let { message -> Text(message, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            val parsedPrice = price.toDoubleOrNull()
            TextButton(
                enabled = !uploading && name.isNotBlank() && parsedPrice != null,
                onClick = {
                    onSave(
                        name.trim(), desc.trim().ifBlank { null }, parsedPrice ?: 0.0,
                        old.toDoubleOrNull(), stock.toIntOrNull() ?: 0, cat, image,
                        featured, flash, hot
                    )
                }
            ) { Text("সেভ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
private fun Categories() {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Category>>(emptyList()) }
    var refresh by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var add by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Category?>(null) }
    var del by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(refresh) {
        loading = true
        Repository().categories().fold(
            { list = it; error = null },
            { error = it.message }
        )
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ক্যাটাগরি", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = { add = true }) {
                Icon(Icons.Default.Add, null)
                Text("নতুন")
            }
        }
        Spacer(Modifier.height(10.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        RefreshableList(refresh, { refresh++ }) {
            if (loading && list.isEmpty()) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            } else if (!loading && list.isEmpty()) {
                item { EmptyState(if (error != null) "ডাটা লোড হয়নি" else "এখানে কোনো ক্যাটাগরি নেই") }
            }
            items(list, key = { it.id }) { c ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        c.imageUrl?.let { AsyncImage(it, null, Modifier.size(48.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.SemiBold)
                            Text(if (c.active) "Active" else "Off")
                        }
                        IconButton(onClick = { edit = c }) { Icon(Icons.Default.Edit, null) }
                        IconButton(onClick = { del = c }) { Icon(Icons.Default.Delete, null) }
                    }
                }
            }
        }
    }

    if (add) {
        CategoryDialog(null, { add = false }) { n, d, img, o ->
            scope.launch {
                Repository().addCategory(n, d, img, o).fold(
                    { add = false; refresh++ },
                    { error = it.message }
                )
            }
        }
    }
    edit?.let { c ->
        CategoryDialog(c, { edit = null }) { n, d, img, o ->
            scope.launch {
                Repository().updateCategory(c.copy(name = n, description = d, imageUrl = img, sortOrder = o)).fold(
                    { edit = null; refresh++ },
                    { error = it.message }
                )
            }
        }
    }
    del?.let { c ->
        Confirm(
            "ক্যাটাগরি ডিলিট?",
            "${c.name} মুছে যাবে.",
            {
                scope.launch {
                    Repository().deleteCategory(c.id).fold(
                        { del = null; refresh++ },
                        { error = it.message; del = null }
                    )
                }
            },
            { del = null }
        )
    }
}

@Composable
private fun Orders() {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Order>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(refresh) {
        loading = true
        Repository().orders().fold(
            { list = it; error = null },
            { error = it.message }
        )
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("অর্ডার", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Status + payment status + customer/address")
        Spacer(Modifier.height(10.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        RefreshableList(refresh, { refresh++ }) {
            if (loading && list.isEmpty()) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            } else if (!loading && list.isEmpty()) {
                item { EmptyState(if (error != null) "ডাটা লোড হয়নি" else "এখনও কোনো অর্ডার আসেনি") }
            }
            items(list, key = { it.id }) { o ->
                Card(Modifier.fillMaxWidth().clickable { selected = o }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("#${o.orderNumber}", fontWeight = FontWeight.Bold)
                            Text("${o.customer} • ${o.phone}")
                            Text("৳ ${money(o.total)} • ${o.paymentStatus}")
                        }
                        AssistChip(onClick = { selected = o }, label = { Text(o.status) })
                    }
                }
            }
        }
    }

    selected?.let { o ->
        OrderDialog(
            o,
            { selected = null }
        ) { status, pay ->
            scope.launch {
                Repository().updateOrderStatus(o.id, status)
                Repository().updatePaymentStatus(o.id, pay)
                Repository().orders().fold(
                    { list = it; selected = null },
                    { error = it.message }
                )
            }
        }
    }
}

@Composable
private fun Customers() {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Customer?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(refresh) {
        loading = true
        Repository().customers().fold(
            { list = it; error = null },
            { error = it.message }
        )
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("কাস্টমার", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Profile + role management")
        Spacer(Modifier.height(10.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        RefreshableList(refresh, { refresh++ }) {
            if (loading && list.isEmpty()) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            } else if (!loading && list.isEmpty()) {
                item { EmptyState(if (error != null) "ডাটা লোড হয়নি" else "এখানে কোনো কাস্টমার ডাটা নেই") }
            }
            items(list, key = { it.id }) { c ->
                Card(Modifier.fillMaxWidth().clickable { selected = c }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(c.name, fontWeight = FontWeight.SemiBold)
                        Text(c.email ?: "—")
                        Text(c.phone ?: "—")
                        AssistChip(onClick = { selected = c }, label = { Text(c.role) })
                    }
                }
            }
        }
    }

    selected?.let { c ->
        RoleDialog(c, { selected = null }) { role ->
            scope.launch {
                Repository().updateCustomerRole(c.id, role).fold(
                    { selected = null; refresh++ },
                    { error = it.message; selected = null }
                )
            }
        }
    }
}

@Composable
private fun Complaints() {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Complaint?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(refresh) {
        loading = true
        Repository().complaints().fold(
            { list = it; error = null },
            { error = it.message }
        )
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("অভিযোগ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        RefreshableList(refresh, { refresh++ }) {
            if (loading && list.isEmpty()) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            } else if (!loading && list.isEmpty()) {
                item { EmptyState(if (error != null) "ডাটা লোড হয়নি" else "এখনও কোনো অভিযোগ আসেনি") }
            }
            items(list, key = { it.id }) { c ->
                Card(Modifier.fillMaxWidth().clickable { selected = c }) {
                    Column(Modifier.padding(12.dp)) {
                        Text("#${c.number}", fontWeight = FontWeight.Bold)
                        Text(c.subject ?: "অভিযোগ")
                        Text("${c.customerName} • ${c.phone}")
                        Text(c.description, maxLines = 2)
                        AssistChip(onClick = { selected = c }, label = { Text(c.status) })
                    }
                }
            }
        }
    }

    selected?.let { c ->
        ComplaintDialog(c, { selected = null }) { status, note ->
            scope.launch {
                Repository().updateComplaint(c.id, status, note).fold(
                    { selected = null; refresh++ },
                    { error = it.message; selected = null }
                )
            }
        }
    }
}

@Composable private fun Wishlist(){
    var list by remember{mutableStateOf<List<WishlistSummary>>(emptyList())}; var error by remember{mutableStateOf<String?>(null)}; var refresh by remember{mutableStateOf(0)}; var loading by remember{mutableStateOf(true)}
    LaunchedEffect(refresh){loading=true;Repository().wishlistSummary().fold({list=it;error=null},{error=it.message});loading=false}
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Wishlist",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("সবচেয়ে বেশি wishlist হওয়া পণ্য")};IconButton({refresh++}){Icon(Icons.Default.Refresh,null)}}
        Spacer(Modifier.height(10.dp));error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        RefreshableList(refresh,{refresh++}) {
            if (loading && list.isEmpty()) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            } else if (!loading && list.isEmpty()) {
                item { EmptyState(if (error != null) "Wishlist ডাটা লোড হয়নি" else "এখনও কোনো Wishlist ডাটা নেই") }
            }
            items(list,key={it.productId}){w->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text("#${w.productId.take(8)}",Modifier.weight(1f),fontWeight=FontWeight.SemiBold);AssistChip({},{Text("${w.count} wishlist")})}}}}
    }
}

private data class CouponForm(val code:String,val title:String?,val type:String,val value:Double,val min:Double,val max:Double?,val limit:Int?,val start:String?,val end:String?)

@Composable private fun CouponDialog(initial:Coupon?,onDismiss:()->Unit,onSave:(CouponForm)->Unit){var code by remember{mutableStateOf(initial?.code?:"")};var title by remember{mutableStateOf(initial?.title?:"")};var type by remember{mutableStateOf(initial?.discountType ?: "percentage")};var value by remember{mutableStateOf(initial?.discountValue?.toString()?:"")};var min by remember{mutableStateOf(initial?.minOrder?.toString()?: "0")};var max by remember{mutableStateOf(initial?.maxDiscount?.toString()?: "")};var limit by remember{mutableStateOf(initial?.usageLimit?.toString()?: "")};AlertDialog(onDismissRequest=onDismiss,title={Text(if(initial==null)"নতুন কুপন" else "কুপন এডিট")},text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){Field(code,{code=it},"কুপন কোড");Field(title,{title=it},"শিরোনাম");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(type=="percentage",{type="percentage"},label={Text("Percent")});FilterChip(type=="fixed",{type="fixed"},label={Text("Fixed")})};Field(value,{value=it},"Discount value",KeyboardType.Decimal);Field(min,{min=it},"Minimum order",KeyboardType.Decimal);Field(max,{max=it},"Maximum discount",KeyboardType.Decimal);Field(limit,{limit=it},"Usage limit",KeyboardType.Number)}},confirmButton={TextButton(enabled=code.isNotBlank()&&value.toDoubleOrNull()!=null,onClick={onSave(CouponForm(code.trim(),title.trim().ifBlank{null},type,value.toDoubleOrNull()?:0.0,min.toDoubleOrNull()?:0.0,max.toDoubleOrNull(),limit.toIntOrNull(),initial?.startsAt,initial?.expiresAt))}){Text("সেভ")}},dismissButton={TextButton(onClick=onDismiss){Text("বাতিল")}})}

@Composable private fun CategoryDialog(initial:Category?,onDismiss:()->Unit,onSave:(String,String?,String?,Int)->Unit){var n by remember{mutableStateOf(initial?.name?:"")};var d by remember{mutableStateOf(initial?.description?:"")};var img by remember{mutableStateOf(initial?.imageUrl?:"")};var o by remember{mutableStateOf(initial?.sortOrder?.toString()?: "0")};AlertDialog(onDismissRequest=onDismiss,title={Text(if(initial==null)"নতুন ক্যাটাগরি" else "ক্যাটাগরি এডিট")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Field(n,{n=it},"নাম");Field(d,{d=it},"বিবরণ");Field(img,{img=it},"Image URL");Field(o,{o=it},"Sort order",KeyboardType.Number)}},confirmButton={TextButton(enabled=n.isNotBlank(),onClick={onSave(n.trim(),d.trim().ifBlank{null},img.trim().ifBlank{null},o.toIntOrNull()?:0)}){Text("সেভ")}},dismissButton={TextButton(onClick=onDismiss){Text("বাতিল")}})}

@Composable private fun OrderDialog(o:Order,onDismiss:()->Unit,onSave:(String,String)->Unit){var status by remember{mutableStateOf(o.status)};var pay by remember{mutableStateOf(o.paymentStatus)};AlertDialog(onDismissRequest=onDismiss,title={Text("অর্ডার #${o.orderNumber}")},text={Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)){Text("কাস্টমার: ${o.customer}");Text("ফোন: ${o.phone}");Text("ইমেইল: ${o.email?:"—"}");Text("ঠিকানা: ${o.address}");Text("Subtotal: ৳ ${money(o.subtotal)}");Text("Delivery: ৳ ${money(o.deliveryCharge)}");Text("Discount: ৳ ${money(o.discount)}");Text("Total: ৳ ${money(o.total)}",fontWeight=FontWeight.Bold);Text("Payment: ${o.paymentMethod}");Text("Order status",fontWeight=FontWeight.SemiBold);SimpleChoice(status,ORDER_STATUSES){status=it};Text("Payment status",fontWeight=FontWeight.SemiBold);SimpleChoice(pay,PAYMENT_STATUSES){pay=it}}},confirmButton={TextButton(onClick={onSave(status,pay)}){Text("আপডেট")}},dismissButton={TextButton(onClick=onDismiss){Text("বন্ধ")}})}

@Composable private fun RoleDialog(c:Customer,onDismiss:()->Unit,onSave:(String)->Unit){var role by remember{mutableStateOf(c.role)};AlertDialog(onDismissRequest=onDismiss,title={Text("${c.name} — Role")},text={SimpleChoice(role,CUSTOMER_ROLES){role=it}},confirmButton={TextButton(onClick={onSave(role)}){Text("সেভ")}},dismissButton={TextButton(onClick=onDismiss){Text("বাতিল")}})}
@Composable private fun ComplaintDialog(c:Complaint,onDismiss:()->Unit,onSave:(String,String?)->Unit){var status by remember{mutableStateOf(c.status)};var note by remember{mutableStateOf(c.adminNote?:"")};AlertDialog(onDismissRequest=onDismiss,title={Text("অভিযোগ #${c.number}")},text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(c.description);SimpleChoice(status,COMPLAINT_STATUSES){status=it};Field(note,{note=it},"Admin note",single=false)}},confirmButton={TextButton(onClick={onSave(status,note.trim().ifBlank{null})}){Text("আপডেট")}},dismissButton={TextButton(onClick=onDismiss){Text("বন্ধ")}})}

@Composable private fun SimpleChoice(selected:String,options:List<String>,onChange:(String)->Unit){var open by remember{mutableStateOf(false)};Box{OutlinedButton({open=true}){Text(selected)};DropdownMenu(open,{open=false}){options.forEach{DropdownMenuItem(text={Text(it)},onClick={onChange(it);open=false})}}}}
@Composable private fun SwitchRow(label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f));Switch(value,onChange)}}
@Composable private fun Field(value:String,onValueChange:(String)->Unit,label:String,type:KeyboardType=KeyboardType.Text,single:Boolean=true){OutlinedTextField(value,onValueChange,label={Text(label)},singleLine=single,keyboardOptions=KeyboardOptions(keyboardType=type),modifier=Modifier.fillMaxWidth())}
@Composable private fun Confirm(title:String,text:String,onConfirm:()->Unit,onDismiss:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Text(text)},confirmButton={TextButton(onClick=onConfirm){Text("ডিলিট",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick=onDismiss){Text("বাতিল")}})}
private fun money(v:Double)=String.format("%.2f",v)

@Composable
fun Coupons() {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Coupon>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var add by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Coupon?>(null) }
    var del by remember { mutableStateOf<Coupon?>(null) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        loading = true
        Repository().coupons().fold(
            { list = it; error = null },
            { error = it.message }
        )
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "কুপন / ডিসকাউন্ট",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("Percent বা fixed discount")
            }

            IconButton(onClick = { refresh++ }) {
                Icon(Icons.Default.Refresh, "রিফ্রেশ")
            }

            FilledTonalButton({ add = true }) {
                Icon(Icons.Default.Add, null)
                Text("নতুন")
            }
        }

        Spacer(Modifier.height(10.dp))
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        RefreshableList(refresh, { refresh++ }) {
            if (loading && list.isEmpty()) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            } else if (error != null && list.isEmpty()) {
                item { EmptyState("কুপনের ডাটা লোড হয়নি") }
            } else if (list.isEmpty()) {
                item { EmptyState("এখানে কোনো কুপন নেই") }
            }

            items(list, key = { it.id }) { c ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(c.code, fontWeight = FontWeight.Bold)
                            Text(
                                "${c.discountType}: ${c.discountValue} • ব্যবহার ${c.usedCount}/${c.usageLimit ?: "∞"}"
                            )
                        }

                        Switch(
                            checked = c.active,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    Repository().updateCoupon(c.copy(active = enabled)).fold(
                                        { refresh++ },
                                        { error = it.message }
                                    )
                                }
                            }
                        )

                        IconButton({ edit = c }) {
                            Icon(Icons.Default.Edit, null)
                        }

                        IconButton({ del = c }) {
                            Icon(Icons.Default.Delete, null)
                        }
                    }
                }
            }
        }
    }

    if (add) {
        CouponDialog(null, { add = false }) { v ->
            scope.launch {
                Repository().addCoupon(
                    v.code, v.title, v.type, v.value,
                    v.min, v.max, v.limit, v.start, v.end
                ).fold(
                    { add = false; refresh++ },
                    { error = it.message }
                )
            }
        }
    }

    edit?.let { c ->
        CouponDialog(c, { edit = null }) { v ->
            scope.launch {
                Repository().updateCoupon(
                    c.copy(
                        code = v.code,
                        title = v.title,
                        discountType = v.type,
                        discountValue = v.value,
                        minOrder = v.min,
                        maxDiscount = v.max,
                        usageLimit = v.limit,
                        startsAt = v.start,
                        expiresAt = v.end
                    )
                ).fold(
                    { edit = null; refresh++ },
                    { error = it.message }
                )
            }
        }
    }

    del?.let { c ->
        Confirm(
            "কুপন ডিলিট?",
            c.code,
            {
                scope.launch {
                    Repository().deleteCoupon(c.id).fold(
                        { del = null; refresh++ },
                        { error = it.message; del = null }
                    )
                }
            },
            { del = null }
        )
    }
}




@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var download by remember { mutableStateOf(DownloadState()) }
    var installedFile by remember { mutableStateOf<java.io.File?>(null) }

    fun check() {
        if (checking) return
        checking = true
        message = null
        scope.launch {
            when (val result = UpdateManager.checkForUpdate()) {
                UpdateResult.UpToDate -> {
                    update = null
                    message = "আপনার অ্যাপ বর্তমানে সর্বশেষ ভার্সনে আছে।"
                }
                is UpdateResult.Available -> {
                    update = result.info
                    message = null
                }
                is UpdateResult.Error -> {
                    update = null
                    message = result.message
                }
            }
            checking = false
        }
    }

    LaunchedEffect(Unit) { check() }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("সেটিংস", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Fol Bazar Admin • বর্তমান ভার্সন ${BuildConfig.VERSION_NAME}")
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("অ্যাপ আপডেট", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "GitHub Release থেকে নতুন Admin APK খুঁজে দেখুন। নতুন ভার্সন থাকলে এখান থেকেই ডাউনলোড করে ইনস্টল করা যাবে।",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    when {
                        checking -> {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text("নতুন আপডেট খোঁজা হচ্ছে…")
                        }
                        update != null -> {
                            val info = update!!
                            Text("নতুন ভার্সন পাওয়া গেছে: ${info.versionName}", fontWeight = FontWeight.Bold)
                            Text(info.releaseName)
                            if (download.running) {
                                LinearProgressIndicator(
                                    progress = { download.progress / 100f },
                                    Modifier.fillMaxWidth()
                                )
                                Text(
                                    if (download.totalBytes > 0)
                                        "ডাউনলোড হচ্ছে… ${download.progress}% (${formatBytes(download.downloadedBytes)} / ${formatBytes(download.totalBytes)})"
                                    else
                                        "ডাউনলোড হচ্ছে… ${formatBytes(download.downloadedBytes)}"
                                )
                            } else if (installedFile != null) {
                                Text("ডাউনলোড সম্পন্ন হয়েছে। এখন ইনস্টল করুন।", fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = {
                                        if (!UpdateManager.canInstallPackages(context)) {
                                            UpdateManager.openUnknownSourcesSettings(context)
                                        } else {
                                            UpdateManager.installApk(context, installedFile!!)
                                                .onFailure { message = it.message ?: "ইনস্টল শুরু করা যায়নি" }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    content = {
                                        Icon(Icons.Default.InstallMobile, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (UpdateManager.canInstallPackages(context))
                                                "আপডেট ইনস্টল করুন"
                                            else
                                                "Install permission চালু করুন"
                                        )
                                    }
                                )
                            } else {
                                Button(
                                    onClick = {
                                        download = DownloadState(running = true)
                                        scope.launch {
                                            UpdateManager.downloadUpdate(context, info) { state ->
                                                download = state
                                            }.fold(
                                                { installedFile = it; download = DownloadState(progress = 100, downloadedBytes = it.length(), totalBytes = it.length(), file = it) },
                                                { error -> message = error.message ?: "ডাউনলোড ব্যর্থ হয়েছে"; download = DownloadState(error = error.message) }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    content = {
                                        Icon(Icons.Default.Download, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("নতুন আপডেট ডাউনলোড করুন")
                                    }
                                )
                            }
                        }
                        else -> {
                            message?.let {
                                Text(
                                    it,
                                    color = if (it.contains("সর্বশেষ")) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { check() },
                        enabled = !checking && !download.running,
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(6.dp))
                            Text("আপডেট চেক করুন")
                        }
                    )
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("আপডেট কীভাবে কাজ করবে", fontWeight = FontWeight.Bold)
                    Text("1. নতুন GitHub Release হলে অ্যাপ সেটি শনাক্ত করবে।")
                    Text("2. নতুন ভার্সন থাকলে এই পেজে দেখাবে।")
                    Text("3. ডাউনলোডে চাপলে অগ্রগতি (%) দেখা যাবে।")
                    Text("4. ডাউনলোড শেষ হলে এখানেই Install বাটন আসবে।")
                    Text("5. Android-এর নিরাপত্তার কারণে প্রথমবার এই অ্যাপের জন্য 'Install unknown apps' অনুমতি লাগতে পারে।")
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
    return String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
