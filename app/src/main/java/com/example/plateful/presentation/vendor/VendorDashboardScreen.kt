package com.example.plateful.presentation.vendor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.plateful.model.*
import kotlinx.serialization.Serializable

@Serializable
object NavVendorDashboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDashboardScreen(
    navController: NavController,
    viewModel: FoodPackManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(VendorDashboardTab.OVERVIEW) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vendor Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    IconButton(onClick = { /* Navigate to settings */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Navigation
            VendorTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
            
            // Content based on selected tab
            when (selectedTab) {
                VendorDashboardTab.OVERVIEW -> OverviewContent(uiState, viewModel)
                VendorDashboardTab.FOOD_PACKS -> FoodPacksContent(uiState, viewModel)
                VendorDashboardTab.RESERVATIONS -> ReservationsContent(uiState, viewModel)
                VendorDashboardTab.ANALYTICS -> AnalyticsContent(uiState, viewModel)
                VendorDashboardTab.PROFILE -> ProfileContent(navController)
            }
        }
    }
}

enum class VendorDashboardTab(val displayName: String) {
    OVERVIEW("Overview"),
    FOOD_PACKS("Food Packs"),
    RESERVATIONS("Orders"),
    ANALYTICS("Analytics"),
    PROFILE("Profile")
}

@Composable
fun VendorTabRow(
    selectedTab: VendorDashboardTab,
    onTabSelected: (VendorDashboardTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        VendorDashboardTab.values().forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.displayName,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
fun OverviewContent(
    uiState: FoodPackManagementUiState,
    viewModel: FoodPackManagementViewModel
) {
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Stats Cards
        item {
            Text(
                text = "Today's Overview",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    listOf(
                        StatCard("Active Packs", "${viewModel.getActiveFoodPacks().size}", Icons.Default.Restaurant, Color(0xFF4CAF50)),
                        StatCard("Today Orders", "${viewModel.getTodayReservations().size}", Icons.Default.ShoppingCart, Color(0xFF2196F3)),
                        StatCard("Revenue", "₹${String.format("%.0f", uiState.analytics.totalSales)}", Icons.Default.AttachMoney, Color(0xFF9C27B0)),
                        StatCard("Expiring Soon", "${viewModel.getExpiringSoonFoodPacks().size}", Icons.Default.Warning, Color(0xFFFF9800))
                    )
                ) { stat ->
                    QuickStatCard(stat)
                }
            }
        }
        
        // Recent Reservations
        item {
            Text(
                text = "Recent Orders",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(viewModel.getTodayReservations().take(5)) { reservation ->
            ReservationCard(
                reservation = reservation,
                onStatusUpdate = { status, reason ->
                    viewModel.updateReservationStatus(reservation.id, status, reason ?: "") { success, message ->
                        // Handle result
                    }
                }
            )
        }
        
        // Expiring Food Packs Alert
        val expiringSoon = viewModel.getExpiringSoonFoodPacks()
        if (expiringSoon.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Food Packs Expiring Soon!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        expiringSoon.forEach { pack ->
                            Text(
                                text = "• ${pack.name} expires in ${getTimeUntilExpiry(pack.expiryTime)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoodPacksContent(
    uiState: FoodPackManagementUiState,
    viewModel: FoodPackManagementViewModel
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with Add Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Food Packs",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Food Pack"
                )
            }
        }
        
        // Food Packs List
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.foodPacks) { foodPack ->
                    FoodPackCard(
                        foodPack = foodPack,
                        onEdit = { viewModel.selectFoodPack(it) },
                        onDelete = { 
                            viewModel.deleteFoodPack(it.id) { success, message ->
                                // Handle result
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Add Food Pack Dialog
    if (showAddDialog) {
        AddFoodPackDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, description, originalPrice, discountedPrice, quantity, expiryHours, cuisineType, isVeg, isVegan ->
                viewModel.createFoodPack(
                    name = name,
                    description = description,
                    originalPrice = originalPrice,
                    discountedPrice = discountedPrice,
                    quantity = quantity,
                    expiryHours = expiryHours,
                    cuisineType = cuisineType,
                    isVegetarian = isVeg,
                    isVegan = isVegan
                ) { success, message ->
                    if (success) showAddDialog = false
                    // Handle result
                }
            }
        )
    }
}

@Composable
fun ReservationsContent(
    uiState: FoodPackManagementUiState,
    viewModel: FoodPackManagementViewModel
) {
    var selectedFilter by remember { mutableStateOf<ReservationStatus?>(null) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Filter Chips
        LazyRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All") }
                )
            }
            
            items(ReservationStatus.values()) { status ->
                FilterChip(
                    selected = selectedFilter == status,
                    onClick = { selectedFilter = status },
                    label = { Text(status.name.replace("_", " ")) }
                )
            }
        }
        
        // Reservations List
        val filteredReservations = viewModel.getFilteredReservations(selectedFilter)
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredReservations) { reservation ->
                    ReservationCard(
                        reservation = reservation,
                        onStatusUpdate = { status, reason ->
                            viewModel.updateReservationStatus(reservation.id, status, reason ?: "") { success, message ->
                                // Handle result
                            }
                        }
                    )
                }
            }
        }
    }
}

data class StatCard(
    val title: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
fun QuickStatCard(stat: StatCard) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = stat.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = stat.icon,
                contentDescription = null,
                tint = stat.color,
                modifier = Modifier.size(24.dp)
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stat.value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = stat.color
                )
                Text(
                    text = stat.title,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AnalyticsContent(
    uiState: FoodPackManagementUiState,
    viewModel: FoodPackManagementViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analytics Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Revenue Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Sales: ₹${String.format("%.2f", uiState.analytics.totalSales)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Total Orders: ${uiState.analytics.totalOrders}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Average Order Value: ₹${String.format("%.2f", if (uiState.analytics.totalOrders > 0) uiState.analytics.totalSales / uiState.analytics.totalOrders else 0.0)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Food Wastage Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Food Saved: ${String.format("%.1f", uiState.analytics.foodSavedKg)} kg",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Packs Sold: ${uiState.analytics.packsSold}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileContent(navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Restaurant Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Profile Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Restaurant Logo & Banner",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Business Hours",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Cuisine Type",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Performance Stats",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { /* TODO: Navigate to profile edit */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit Profile")
                    }
                }
            }
        }
    }
}

@Composable
fun ReservationCard(
    reservation: FoodPackReservation,
    onStatusUpdate: (ReservationStatus, String?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order #${reservation.id.take(8)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "User: ${reservation.customerId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Quantity: ${reservation.quantity}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Total: ₹${String.format("%.2f", reservation.totalPrice)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Status Badge
                Surface(
                    color = getStatusColor(reservation.status),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = reservation.status.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status Update Buttons
            if (reservation.status == ReservationStatus.CONFIRMED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onStatusUpdate(ReservationStatus.READY_FOR_PICKUP, null) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Ready")
                    }
                    OutlinedButton(
                        onClick = { onStatusUpdate(ReservationStatus.CANCELED, "Restaurant cancelled") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }
            } else if (reservation.status == ReservationStatus.READY_FOR_PICKUP) {
                Button(
                    onClick = { onStatusUpdate(ReservationStatus.COMPLETED, null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Text("Mark as Completed")
                }
            }
        }
    }
}

@Composable
fun FoodPackCard(
    foodPack: FoodPack,
    onEdit: (FoodPack) -> Unit,
    onDelete: (FoodPack) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = foodPack.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = foodPack.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "₹${String.format("%.0f", foodPack.originalPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                        Text(
                            text = "₹${String.format("%.0f", foodPack.discountedPrice)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    
                    Text(
                        text = "Quantity: ${foodPack.quantity} | Expires: ${getTimeUntilExpiry(foodPack.expiryTime)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Status Badge
                Surface(
                    color = getFoodPackStatusColor(foodPack.status),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = foodPack.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onEdit(foodPack) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = { onDelete(foodPack) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun AddFoodPackDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, Int, Int, CuisineType, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var originalPrice by remember { mutableStateOf("") }
    var discountedPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var expiryHours by remember { mutableStateOf("24") }
    var selectedCuisine by remember { mutableStateOf(CuisineType.NORTH_INDIAN) }
    var isVeg by remember { mutableStateOf(false) }
    var isVegan by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add New Food Pack")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Food Pack Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = originalPrice,
                            onValueChange = { originalPrice = it },
                            label = { Text("Original Price") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discountedPrice,
                            onValueChange = { discountedPrice = it },
                            label = { Text("Discounted Price") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = expiryHours,
                            onValueChange = { expiryHours = it },
                            label = { Text("Expiry Hours") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isVeg,
                            onCheckedChange = { isVeg = it }
                        )
                        Text("Vegetarian")
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Checkbox(
                            checked = isVegan,
                            onCheckedChange = { isVegan = it }
                        )
                        Text("Vegan")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val origPrice = originalPrice.toDoubleOrNull() ?: 0.0
                    val discPrice = discountedPrice.toDoubleOrNull() ?: 0.0
                    val qty = quantity.toIntOrNull() ?: 0
                    val expiry = expiryHours.toIntOrNull() ?: 24
                    
                    if (name.isNotBlank() && origPrice > 0 && discPrice > 0 && qty > 0) {
                        onConfirm(name, description, origPrice, discPrice, qty, expiry, selectedCuisine, isVeg, isVegan)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
fun getStatusColor(status: ReservationStatus): Color {
    return when (status) {
        ReservationStatus.CONFIRMED -> Color(0xFFFF9800)
        ReservationStatus.READY_FOR_PICKUP -> Color(0xFF2196F3)
        ReservationStatus.COMPLETED -> Color(0xFF4CAF50)
        ReservationStatus.CANCELED -> Color(0xFFD32F2F)
        ReservationStatus.NO_SHOW -> Color(0xFF757575)
    }
}

fun getFoodPackStatusColor(status: FoodPackStatus): Color {
    return when (status) {
        FoodPackStatus.AVAILABLE -> Color(0xFF4CAF50)
        FoodPackStatus.RESERVED -> Color(0xFFFF9800)
        FoodPackStatus.COMPLETED -> Color(0xFFD32F2F)
        FoodPackStatus.EXPIRED -> Color(0xFF757575)
        FoodPackStatus.CANCELED -> Color(0xFF9E9E9E)
    }
}

// Helper function to calculate time until expiry
fun getTimeUntilExpiry(expiryTime: com.google.firebase.Timestamp): String {
    val now = System.currentTimeMillis()
    val expiry = expiryTime.toDate().time
    val diffMs = expiry - now
    
    return when {
        diffMs <= 0 -> "Expired"
        diffMs < 60 * 60 * 1000 -> "${diffMs / (60 * 1000)}min"
        diffMs < 24 * 60 * 60 * 1000 -> "${diffMs / (60 * 60 * 1000)}h"
        else -> "${diffMs / (24 * 60 * 60 * 1000)}d"
    }
}
