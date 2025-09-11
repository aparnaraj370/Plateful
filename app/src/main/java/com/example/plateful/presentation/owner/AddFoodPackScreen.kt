package com.example.plateful.presentation.owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.plateful.model.CuisineType
import com.example.plateful.presentation.vendor.FoodPackManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodPackScreen(
    navController: NavController,
    viewModel: FoodPackManagementViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var originalPrice by remember { mutableStateOf("") }
    var discountedPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var expiryHours by remember { mutableStateOf("4") }
    var selectedCuisine by remember { mutableStateOf(CuisineType.VEGETARIAN) }
    var isVegetarian by remember { mutableStateOf(true) }
    var isVegan by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Add Food Pack",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Food Pack Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food Pack Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                minLines = 2
            )
            
            // Prices Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = originalPrice,
                    onValueChange = { originalPrice = it },
                    label = { Text("Original Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = discountedPrice,
                    onValueChange = { discountedPrice = it },
                    label = { Text("Discounted Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Quantity and Expiry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = expiryHours,
                    onValueChange = { expiryHours = it },
                    label = { Text("Expires in (hours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Cuisine Type Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCuisine.name.replace("_", " "),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Cuisine Type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    CuisineType.entries.forEach { cuisine ->
                        DropdownMenuItem(
                            text = { Text(cuisine.name.replace("_", " ")) },
                            onClick = {
                                selectedCuisine = cuisine
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            // Checkboxes
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isVegetarian,
                        onCheckedChange = { isVegetarian = it }
                    )
                    Text("Vegetarian", modifier = Modifier.padding(start = 8.dp))
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isVegan,
                        onCheckedChange = { isVegan = it }
                    )
                    Text("Vegan", modifier = Modifier.padding(start = 8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add Button
            Button(
                onClick = {
                    if (validateForm(name, description, originalPrice, discountedPrice, quantity)) {
                        isLoading = true
                        viewModel.createFoodPack(
                            name = name,
                            description = description,
                            originalPrice = originalPrice.toDoubleOrNull() ?: 0.0,
                            discountedPrice = discountedPrice.toDoubleOrNull() ?: 0.0,
                            quantity = quantity.toIntOrNull() ?: 0,
                            expiryHours = expiryHours.toIntOrNull() ?: 4,
                            cuisineType = selectedCuisine,
                            isVegetarian = isVegetarian,
                            isVegan = isVegan
                        ) { success, message ->
                            isLoading = false
                            if (success) {
                                navController.popBackStack()
                            }
                            // Handle error message
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        "Add Food Pack",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun validateForm(
    name: String,
    description: String,
    originalPrice: String,
    discountedPrice: String,
    quantity: String
): Boolean {
    return name.isNotBlank() &&
            description.isNotBlank() &&
            originalPrice.isNotBlank() && originalPrice.toDoubleOrNull() != null &&
            discountedPrice.isNotBlank() && discountedPrice.toDoubleOrNull() != null &&
            quantity.isNotBlank() && quantity.toIntOrNull() != null
}
