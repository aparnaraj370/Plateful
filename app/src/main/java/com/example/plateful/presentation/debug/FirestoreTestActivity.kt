package com.example.plateful.presentation.debug

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.plateful.model.*
import com.example.plateful.domain.repository.FoodPackRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FirestoreTestActivity : ComponentActivity() {
    
    @Inject
    lateinit var foodPackRepository: FoodPackRepository
    
    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FirestoreTestScreen()
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FirestoreTestScreen() {
        var testResults by remember { mutableStateOf<List<String>>(emptyList()) }
        var foodPacks by remember { mutableStateOf<List<FoodPack>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Firestore Food Pack Test",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Test buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        isLoading = true
                        lifecycleScope.launch {
                            testCreateFoodPack { result ->
                                testResults = testResults + result
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Create Test Food Pack")
                }
                
                Button(
                    onClick = {
                        isLoading = true
                        lifecycleScope.launch {
                            testGetFoodPacks { packs ->
                                foodPacks = packs
                                testResults = testResults + "Retrieved ${packs.size} food packs"
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Get Food Packs")
                }
            }
            
            Button(
                onClick = {
                    testResults = emptyList()
                    foodPacks = emptyList()
                },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text("Clear Results")
            }
            
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
            
            LazyColumn {
                // Test results
                if (testResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "Test Results:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(testResults) { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = result,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                
                // Food packs
                if (foodPacks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Retrieved Food Packs:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(foodPacks) { foodPack ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = foodPack.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Restaurant: ${foodPack.restaurantName}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Price: ₹${foodPack.discountedPrice}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Quantity: ${foodPack.quantity}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Status: ${foodPack.status}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    private suspend fun testCreateFoodPack(onResult: (String) -> Unit) {
        try {
            Log.d("FirestoreTest", "🚀 Starting food pack creation test...")
            
            val currentUser = auth.currentUser
            if (currentUser == null) {
                onResult("❌ User not authenticated")
                return
            }
            
            val testFoodPack = FoodPack(
                id = "", // Will be generated
                vendorId = currentUser.uid,
                restaurantId = "test-restaurant-${System.currentTimeMillis()}",
                restaurantName = "Test Debug Restaurant",
                name = "Debug Test Food Pack",
                description = "A test food pack created from debug screen",
                originalPrice = 20.0,
                discountedPrice = 12.99,
                quantity = 3,
                totalQuantity = 5,
                expiryTime = Timestamp(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)), // 24 hours from now
                status = FoodPackStatus.AVAILABLE,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
                cuisineType = CuisineType.VEGETARIAN,
                isVegetarian = true,
                isVegan = false,
                images = emptyList(),
                nutritionInfo = "Test nutrition",
                allergyInfo = "No allergies",
                packagingType = "Eco-friendly",
                pickupInstructions = "Come to counter",
                rating = 0.0,
                reviewCount = 0,
                location = RestaurantLocation(
                    address = "Debug Test Address",
                    city = "Debug City",
                    state = "Debug State",
                    postalCode = "12345",
                    latitude = 0.0,
                    longitude = 0.0
                )
            )
            
            Log.d("FirestoreTest", "📦 Creating food pack: ${testFoodPack.name}")
            val result = foodPackRepository.createFoodPack(testFoodPack)
            
            if (result != null) {
                Log.d("FirestoreTest", "✅ Food pack created successfully with ID: $result")
                onResult("✅ Food pack created with ID: $result")
            } else {
                Log.e("FirestoreTest", "❌ Failed to create food pack")
                onResult("❌ Failed to create food pack")
            }
            
        } catch (e: Exception) {
            Log.e("FirestoreTest", "❌ Exception creating food pack", e)
            onResult("❌ Exception: ${e.message}")
        }
    }
    
    private suspend fun testGetFoodPacks(onResult: (List<FoodPack>) -> Unit) {
        try {
            Log.d("FirestoreTest", "🔍 Starting food pack retrieval test...")
            
            val availableFoodPacks = foodPackRepository.getAvailableFoodPacks()
            Log.d("FirestoreTest", "📋 Retrieved ${availableFoodPacks.size} available food packs")
            
            availableFoodPacks.forEach { pack ->
                Log.d("FirestoreTest", "  - ${pack.name} by ${pack.restaurantName} (₹${pack.discountedPrice})")
            }
            
            onResult(availableFoodPacks)
            
        } catch (e: Exception) {
            Log.e("FirestoreTest", "❌ Exception retrieving food packs", e)
            onResult(emptyList())
        }
    }
}
