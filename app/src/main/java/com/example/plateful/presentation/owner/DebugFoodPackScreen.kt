package com.example.plateful.presentation.owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.plateful.model.FoodPack
import com.example.plateful.presentation.vendor.FoodPackManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugFoodPackScreen(
    viewModel: FoodPackManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Food Pack Debug Screen",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Refresh button
        Button(
            onClick = { viewModel.refreshData() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Food Packs")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isError) 
                    MaterialTheme.colorScheme.errorContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Status: ${if (uiState.isLoading) "Loading..." else "Loaded"}")
                Text("Food Packs Count: ${uiState.foodPacks.size}")
                Text("Reservations Count: ${uiState.reservations.size}")
                
                if (uiState.isError) {
                    Text("Error: ${uiState.errorMessage}")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Food packs list
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                items(uiState.foodPacks) { foodPack ->
                    FoodPackDebugCard(foodPack = foodPack)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (uiState.foodPacks.isEmpty()) {
                    item {
                        Text(
                            "No food packs found. Try adding some from the Add Food Pack screen.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodPackDebugCard(foodPack: FoodPack) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = foodPack.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text("Restaurant: ${foodPack.restaurantName}")
            Text("Price: ₹${foodPack.originalPrice} → ₹${foodPack.discountedPrice}")
            Text("Quantity: ${foodPack.quantity}")
            Text("Status: ${foodPack.status}")
            Text("Created: ${foodPack.createdAt}")
            Text("ID: ${foodPack.id}")
            Text("Restaurant ID: ${foodPack.restaurantId}")
            Text("Vendor ID: ${foodPack.vendorId}")
        }
    }
}
