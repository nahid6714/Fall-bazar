package com.example.ui.screens.bookings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Booking
import com.example.data.repository.BookingRepository
import com.example.ui.components.BtbdTopAppBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    bookingRepository: BookingRepository,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("all") }
    var selectedTypeFilter by remember { mutableStateOf("all") }
    var activeBookingDetails by remember { mutableStateOf<Booking?>(null) }
    var showEditPriceDialog by remember { mutableStateOf<Booking?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            val result = bookingRepository.getBookings()
            isLoading = false
            result.fold(
                onSuccess = { bookings = it },
                onFailure = { err ->
                    snackbarHostState.showSnackbar(err.localizedMessage ?: "বুকিং তথ্য লোড করা যায়নি")
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val filteredBookings = remember(bookings, searchQuery, selectedStatusFilter, selectedTypeFilter) {
        bookings.filter { b ->
            val matchesQuery = searchQuery.isBlank() ||
                    b.customerName.contains(searchQuery, ignoreCase = true) ||
                    b.phone.contains(searchQuery) ||
                    b.bookingId.contains(searchQuery, ignoreCase = true) ||
                    b.destination.contains(searchQuery, ignoreCase = true) ||
                    b.pickup.contains(searchQuery, ignoreCase = true)

            val matchesStatus = selectedStatusFilter == "all" || b.status.equals(selectedStatusFilter, ignoreCase = true)
            val matchesType = selectedTypeFilter == "all" || b.bookingType.equals(selectedTypeFilter, ignoreCase = true)

            matchesQuery && matchesStatus && matchesType
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BtbdTopAppBar(
                title = "বুকিং ব্যবস্থাপনা",
                subtitle = "মোট ${filteredBookings.size} টি বুকিং পাওয়া গেছে",
                onRefresh = { loadData() },
                onNavigateBack = onNavigateBack
            )
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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("গ্রাহকের নাম, মোবাইল, আইডি বা গন্তব্য খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "মুছুন")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Status Filter Chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "all",
                        onClick = { selectedStatusFilter = "all" },
                        label = { Text("সকল স্ট্যাটাস") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "pending",
                        onClick = { selectedStatusFilter = "pending" },
                        label = { Text("অপেক্ষমাণ (Pending)") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "confirmed",
                        onClick = { selectedStatusFilter = "confirmed" },
                        label = { Text("নিশ্চিত (Confirmed)") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "completed",
                        onClick = { selectedStatusFilter = "completed" },
                        label = { Text("সম্পন্ন (Completed)") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == "cancelled",
                        onClick = { selectedStatusFilter = "cancelled" },
                        label = { Text("বাতিল (Cancelled)") }
                    )
                }
            }

            // Booking Type Filter Chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SuggestionChip(
                        onClick = { selectedTypeFilter = "all" },
                        label = { Text("সকল ধরন") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (selectedTypeFilter == "all") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { selectedTypeFilter = "bus" },
                        label = { Text("🚌 বাস") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (selectedTypeFilter == "bus") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { selectedTypeFilter = "mini_coach" },
                        label = { Text("🚐 মিনি কোচ") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (selectedTypeFilter == "mini_coach") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { selectedTypeFilter = "tour_package" },
                        label = { Text("🏖️ ট্যুর প্যাকেজ") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (selectedTypeFilter == "tour_package") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredBookings.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.SearchOff,
                    title = "কোন বুকিং পাওয়া যায়নি",
                    subtitle = "ফিল্টার পরিবর্তন করুন বা নতুন বুকিংয়ের জন্য অপেক্ষা করুন।"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBookings, key = { it.id }) { booking ->
                        BookingItemCard(
                            booking = booking,
                            onStatusChange = { newStatus ->
                                coroutineScope.launch {
                                    val res = bookingRepository.updateStatus(booking.id, newStatus)
                                    res.fold(
                                        onSuccess = {
                                            snackbarHostState.showSnackbar("বুকিং স্ট্যাটাস পরিবর্তন হয়েছে")
                                            loadData()
                                        },
                                        onFailure = { err ->
                                            snackbarHostState.showSnackbar(err.localizedMessage ?: "স্ট্যাটাস পরিবর্তন ব্যর্থ")
                                        }
                                    )
                                }
                            },
                            onEditPrice = {
                                showEditPriceDialog = booking
                            },
                            onCall = { phone ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            },
                            onClick = { activeBookingDetails = booking }
                        )
                    }
                }
            }
        }
    
        }}

    // Detail Bottom Sheet
    if (activeBookingDetails != null) {
        val b = activeBookingDetails!!
        ModalBottomSheet(
            onDismissRequest = { activeBookingDetails = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = b.bookingId,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "তারিখ: ${b.createdAt}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                    }
                    StatusBadge(status = b.status)
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    text = "গ্রাহকের তথ্য",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(label = "নাম", value = b.customerName)
                DetailRow(
                    label = "মোবাইল নম্বর",
                    value = b.phone,
                    isPhone = true,
                    onPhoneClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${b.phone}"))
                        context.startActivity(intent)
                    }
                )
                if (b.email.isNotBlank()) {
                    DetailRow(label = "ইমেইল", value = b.email)
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    text = "ভ্রমণ বিবরণ",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(label = "সার্ভিসের ধরন", value = b.bookingType.uppercase())
                DetailRow(label = "পিকআপ লোকেশন", value = b.pickup)
                DetailRow(label = "গন্তব্য", value = b.destination)
                DetailRow(label = "ভ্রমণের তারিখ", value = b.travelDate)
                if (b.returnDate.isNotBlank()) {
                    DetailRow(label = "ফিরতি তারিখ", value = b.returnDate)
                }
                DetailRow(label = "যাত্রী সংখ্যা", value = "${b.passengers} জন")
                if (b.vehicleType.isNotBlank()) {
                    DetailRow(label = "গাড়ির ধরন / কোচ", value = b.vehicleType)
                }
                DetailRow(label = "ট্রিপ ডুরেশন", value = "${b.tripDays} দিন")
                DetailRow(label = "আনুমানিক মূল্য", value = "৳${b.estimatedPrice.toInt()}")

                if (b.specialRequest.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "বিশেষ রিকোয়েস্ট:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = b.specialRequest,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${b.phone}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("কল করুন")
                    }

                    OutlinedButton(
                        onClick = {
                            activeBookingDetails = null
                            showEditPriceDialog = b
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("মূল্য নির্ধারণ")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Edit Price Dialog
    if (showEditPriceDialog != null) {
        val b = showEditPriceDialog!!
        var priceInput by remember { mutableStateOf(b.estimatedPrice.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showEditPriceDialog = null },
            title = { Text("বুকিং মূল্য নির্ধারণ করুন") },
            text = {
                Column {
                    Text("বুকিং আইডি: ${b.bookingId} (${b.customerName})")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("মোট নির্ধারিত মূল্য (টাকা)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPrice = priceInput.toDoubleOrNull() ?: 0.0
                        coroutineScope.launch {
                            val res = bookingRepository.updateEstimatedPrice(b.id, newPrice)
                            res.fold(
                                onSuccess = {
                                    snackbarHostState.showSnackbar("মূল্য সফলভাবে আপডেট হয়েছে")
                                    loadData()
                                },
                                onFailure = {
                                    snackbarHostState.showSnackbar("মূল্য পরিবর্তন ব্যর্থ")
                                }
                            )
                        }
                        showEditPriceDialog = null
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditPriceDialog = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun BookingItemCard(
    booking: Booking,
    onStatusChange: (String) -> Unit,
    onEditPrice: () -> Unit,
    onCall: (String) -> Unit,
    onClick: () -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = booking.bookingId,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        StatusBadge(
                            status = booking.status,
                            modifier = Modifier.clickable { statusMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("অপেক্ষমাণ (Pending)") },
                                onClick = { onStatusChange("pending"); statusMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("নিশ্চিত (Confirmed)") },
                                onClick = { onStatusChange("confirmed"); statusMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("সম্পন্ন (Completed)") },
                                onClick = { onStatusChange("completed"); statusMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("বাতিল (Cancelled)") },
                                onClick = { onStatusChange("cancelled"); statusMenuExpanded = false }
                            )
                        }
                    }
                }

                Text(
                    text = "৳${booking.estimatedPrice.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = booking.customerName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = EmeraldPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = booking.phone,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable { onCall(booking.phone) }
                )
            }

            Text(
                text = "${booking.pickup} → ${booking.destination}",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "তারিখ: ${booking.travelDate} | ${booking.passengers} সিট",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onEditPrice,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = "মূল্য পরিবর্তন",
                            tint = AmberTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onCall(booking.phone) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "কল করুন",
                            tint = EmeraldOnPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    isPhone: Boolean = false,
    onPhoneClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        if (isPhone && onPhoneClick != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                ),
                modifier = Modifier.clickable { onPhoneClick() }
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
