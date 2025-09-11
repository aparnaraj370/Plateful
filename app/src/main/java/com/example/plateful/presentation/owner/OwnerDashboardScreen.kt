package com.example.plateful.presentation.owner

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.plateful.model.*
import com.example.plateful.presentation.vendor.FoodPackManagementViewModel
import com.example.plateful.presentation.vendor.FoodPackManagementUiState
import kotlinx.serialization.Serializable

@Serializable
object NavOwnerDashboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    navController: NavController,
    viewModel: FoodPackManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Owner Dashboard",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { 
                        navController.navigate(NavAddFoodPack)
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Food Pack")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Food Packs") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reservations") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Analytics") }
                )
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> FoodPacksTab(uiState, viewModel, navController)
                1 -> ReservationsTab(uiState, viewModel)
                2 -> AnalyticsTab(uiState, viewModel)
            }
        }
    }
}

@Composable
private fun FoodPacksTab(
    uiState: FoodPackManagementUiState,
    viewModel: FoodPackManagementViewModel,
    navController: NavController
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
    
    if (uiState.foodPacks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No food packs yet",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Add your first food pack to get started",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.foodPacks) { foodPack ->
            FoodPackCard(
                foodPack = foodPack,
                onEdit = { 
                    navController.navigate("edit_food_pack/${foodPack.id}")
                },
                onDelete = { 
                    viewModel.deleteFoodPack(foodPack.id) { success, message ->
                        // Handle result
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodPackCard(
    foodPack: FoodPack,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val cardHeight by animateDpAsState(
        targetValue = if (expanded) 200.dp else 120.dp,
        label = "cardHeight"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (foodPack.status) {
                FoodPackStatus.AVAILABLE -> MaterialTheme.colorScheme.surface
                FoodPackStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer
                FoodPackStatus.CANCELED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = foodPack.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${foodPack.discountedPrice} (was ₹${foodPack.originalPrice})",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                StatusBadge(status = foodPack.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Available: ${foodPack.quantity} / ${foodPack.totalQuantity}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = foodPack.description,
                    fontSize = 12.sp,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onEdit) {
                        Text("Edit")
                    }
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: FoodPackStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        FoodPackStatus.AVAILABLE -> Triple(
            Color.Green.copy(alpha = 0.2f),
            Color.Green,
            "Available"
        )
        FoodPackStatus.EXPIRED -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.error,
            "Expired"
        )
        FoodPackStatus.CANCELED -> Triple(
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.outline,
            "Cancelled"
        )
        else -> Triple(
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.outline,
            status.name
        )
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ReservationsTab(
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
    
    if (uiState.reservations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No reservations yet",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.reservations) { reservation ->
            ReservationCard(
                reservation = reservation,
                onStatusUpdate = { newStatus ->
                    viewModel.updateReservationStatus(
                        reservation.id,
                        newStatus
                    ) { success, message ->
                        // Handle result
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReservationCard(
    reservation: FoodPackReservation,
    onStatusUpdate: (ReservationStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reservation.customerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reservation.customerPhone,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                Text(
                    text = "₹${reservation.totalPrice}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Quantity: ${reservation.quantity}",
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(reservation.status)
                
                Row {
                    if (reservation.status == ReservationStatus.CONFIRMED) {
                        TextButton(
                            onClick = { onStatusUpdate(ReservationStatus.READY_FOR_PICKUP) }
                        ) {
                            Text("Ready")
                        }
                    }
                    if (reservation.status == ReservationStatus.READY_FOR_PICKUP) {
                        TextButton(
                            onClick = { onStatusUpdate(ReservationStatus.COMPLETED) }
                        ) {
                            Text("Complete")
                        }
                    }
                    TextButton(
                        onClick = { onStatusUpdate(ReservationStatus.CANCELED) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ReservationStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        ReservationStatus.CONFIRMED -> Triple(
            Color.Blue.copy(alpha = 0.2f),
            Color.Blue,
            "Confirmed"
        )
        ReservationStatus.READY_FOR_PICKUP -> Triple(
            Color.Green.copy(alpha = 0.2f),
            Color.Green,
            "Ready"
        )
        ReservationStatus.COMPLETED -> Triple(
            Color.Green.copy(alpha = 0.3f),
            Color.Green,
            "Completed"
        )
        ReservationStatus.CANCELED -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.error,
            "Cancelled"
        )
        ReservationStatus.NO_SHOW -> Triple(
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.outline,
            "No Show"
        )
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun AnalyticsTab(
    uiState: FoodPackManagementUiState,
    viewModel: FoodPackManagementViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnalyticsCard(
                title = "Daily Sales",
                value = "₹${uiState.analytics.totalSales}",
                icon = Icons.Default.AttachMoney,
                backgroundColor = Color.Green.copy(alpha = 0.1f)
            )
        }
        
        item {
            AnalyticsCard(
                title = "Total Orders",
                value = "${uiState.analytics.totalOrders}",
                icon = Icons.Default.ShoppingCart,
                backgroundColor = Color.Blue.copy(alpha = 0.1f)
            )
        }
        
        item {
            AnalyticsCard(
                title = "Food Saved",
                value = "${uiState.analytics.foodSavedKg} kg",
                icon = Icons.Default.Eco,
                backgroundColor = Color.Green.copy(alpha = 0.1f)
            )
        }
        
        item {
            AnalyticsCard(
                title = "Packs Sold",
                value = "${uiState.analytics.packsSold}",
                icon = Icons.Default.Fastfood,
                backgroundColor = Color(0xFFFF9800).copy(alpha = 0.1f)
            )
        }
        
        item {
            AnalyticsCard(
                title = "Average Rating",
                value = "${uiState.analytics.averageRating}⭐",
                icon = Icons.Default.Star,
                backgroundColor = Color.Yellow.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Navigation routes
@Serializable
object NavAddFoodPack
