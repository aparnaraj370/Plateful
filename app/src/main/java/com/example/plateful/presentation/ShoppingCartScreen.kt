package com.example.plateful.presentation.cart

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.plateful.R
import com.example.plateful.model.CartItem
import com.example.plateful.model.RestaurantCart
import com.example.plateful.presentation.itemdetailscreen.NavOrderConfirmScreen
import com.example.plateful.presentation.cart.CartViewModel
import com.example.plateful.model.CartState
import kotlinx.serialization.Serializable

@Serializable
object ShoppingCartScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingCartScreen(
    navController: NavController,
    onBackClick: () -> Unit = {},
    viewModel: CartViewModel = viewModel()
) {
    val cartState by viewModel.cartState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Cart", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            if (cartState.restaurantCarts.isEmpty()) {
                EmptyCartContent()
            } else {
                CartContent(
                    cartState = cartState,
                    onQuantityChange = { restaurantId, menuItemId, quantity ->
                        viewModel.updateItemQuantity(restaurantId, menuItemId, quantity)
                    },
                    onItemRemove = { restaurantId, menuItemId ->
                        viewModel.removeFromCart(restaurantId, menuItemId)
                    },
                    onCheckout = {
                        // TODO: Fix checkout navigation - NavOrderConfirmScreen requires item parameters
                        // For now, just navigate back to main screen
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyCartContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = "Empty Cart",
            modifier = Modifier.size(120.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your cart is empty",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add some delicious items to get started!",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}

@Composable
fun CartContent(
    cartState: CartState,
    onQuantityChange: (String, String, Int) -> Unit,
    onItemRemove: (String, String) -> Unit,
    onCheckout: () -> Unit
) {
    Column {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        items(cartState.restaurantCarts) { restaurantCart ->
            RestaurantCartSection(
                restaurantCart = restaurantCart,
                onQuantityChange = onQuantityChange,
                onItemRemove = onItemRemove
            )
        }
        
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Total and Checkout Section
        Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            OrderSummary(cartState = cartState)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Proceed to Checkout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
}

@Composable
fun RestaurantCartSection(
    restaurantCart: RestaurantCart,
    onQuantityChange: (String, String, Int) -> Unit,
    onItemRemove: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Restaurant Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = "Restaurant",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = restaurantCart.restaurantName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Cart Items
            restaurantCart.items.forEach { cartItem ->
                CartItemRow(
                    cartItem = cartItem,
                    onQuantityChange = { quantity ->
                        onQuantityChange(restaurantCart.restaurantId, cartItem.foodCard.Menu, quantity)
                    },
                    onItemRemove = {
                        onItemRemove(restaurantCart.restaurantId, cartItem.foodCard.Menu)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Restaurant Subtotal
            androidx.compose.material3.HorizontalDivider(
                thickness = 1.dp,
                color = Color.Gray.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtotal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹${restaurantCart.subtotal}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onQuantityChange: (Int) -> Unit,
    onItemRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF8F9FA),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Food Image - Using a default image since Card doesn't have imageRes
        Image(
            painter = painterResource(id = R.drawable.trust6),
            contentDescription = cartItem.foodCard.Menu,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Item Details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = cartItem.foodCard.Menu,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "₹${cartItem.foodCard.discountedPrice.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            if (cartItem.foodCard.price > cartItem.foodCard.discountedPrice) {
                Text(
                    text = "₹${cartItem.foodCard.price.toInt()}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = Color.Gray
                )
            }
        }
        
        // Quantity Controls
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (cartItem.quantity > 1) {
                        onQuantityChange(cartItem.quantity - 1)
                    } else {
                        onItemRemove()
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (cartItem.quantity > 1) Icons.Default.Remove else Icons.Default.Delete,
                    contentDescription = if (cartItem.quantity > 1) "Decrease" else "Remove",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Text(
                text = "${cartItem.quantity}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            IconButton(
                onClick = { onQuantityChange(cartItem.quantity + 1) },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun OrderSummary(cartState: CartState) {
    Column {
        Text(
            text = "Order Summary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SummaryRow("Subtotal", "₹${cartState.subtotal}")
        SummaryRow("Delivery Fee", "₹${cartState.deliveryFee}")
        SummaryRow("Taxes", "₹${cartState.taxes}")
        
        if (cartState.discount > 0) {
            SummaryRow(
                "Discount", 
                "-₹${cartState.discount}", 
                valueColor = MaterialTheme.colorScheme.error
            )
        }
        
        androidx.compose.material3.HorizontalDivider(
            thickness = 1.dp,
            color = Color.Gray.copy(alpha = 0.3f),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        SummaryRow(
            "Total", 
            "₹${cartState.total}",
            titleStyle = MaterialTheme.typography.titleLarge,
            titleWeight = FontWeight.Bold,
            valueStyle = MaterialTheme.typography.titleLarge,
            valueWeight = FontWeight.Bold,
            valueColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SummaryRow(
    title: String,
    value: String,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    titleWeight: FontWeight = FontWeight.Normal,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    valueWeight: FontWeight = FontWeight.Normal,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = titleStyle,
            fontWeight = titleWeight
        )
        Text(
            text = value,
            style = valueStyle,
            fontWeight = valueWeight,
            color = if (valueColor == Color.Unspecified) Color.Unspecified else valueColor
        )
    }
}
