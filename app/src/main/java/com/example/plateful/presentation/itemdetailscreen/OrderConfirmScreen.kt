package com.example.plateful.presentation.itemdetailscreen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.google.firebase.auth.FirebaseAuth
import com.example.plateful.R
import com.example.plateful.presentation.login.Screens.NavMainScreen
import com.example.plateful.presentation.orders.OrderCreationState
import com.example.plateful.presentation.orders.OrderViewModel
import com.example.plateful.ui.theme.backgroundDark
import kotlinx.serialization.Serializable


@Serializable
data class NavOrderConfirmScreen(
    val itemName: String,
    val restaurantName: String,
    val originalPrice: Float,
    val discountedPrice: Float,
    val rating: Float,
    val distance: Float,
    val location: String,
    val pickupTime: String,
    val isVegan: Boolean,
    val quantity: Int
)

@Composable
fun OrderConfirmScreen( 
    navController: NavController,
    itemName: String,
    restaurantName: String,
    originalPrice: Float,
    discountedPrice: Float,
    rating: Float,
    distance: Float,
    location: String,
    pickupTime: String,
    isVegan: Boolean,
    quantity: Int,
    orderViewModel: OrderViewModel = viewModel()
) {
    val localContext = LocalContext.current
    val orderCreationState by orderViewModel.orderCreationState.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    // Handle order creation state changes
    LaunchedEffect(orderCreationState) {
        val currentState = orderCreationState
        when (currentState) {
            is OrderCreationState.Success -> {
                Toast.makeText(localContext, "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
                orderViewModel.resetOrderCreationState()
                navController.navigate(NavMainScreen) {
                    navOptions {
                        popUpTo(NavMainScreen) {
                            inclusive = false
                        }
                    }
                }
            }
            is OrderCreationState.Error -> {
                Toast.makeText(localContext, "Error: ${currentState.message}", Toast.LENGTH_LONG).show()
                orderViewModel.resetOrderCreationState()
            }
            else -> {}
        }
    }
    Column {
        Row {
            Card(
                modifier = Modifier
                    .height(340.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column {
                    val imageId = R.drawable.trust6
                    Image(
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp)),
                        contentScale = ContentScale.Crop,
                        painter = painterResource(id = imageId),
                        contentDescription = null
                    )
                    Text(text = "Image Section")
                }
            }
        }
        Row{
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = -50.dp),
                contentAlignment = Alignment.Center,

                ){
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                    shape = RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                ) {
                    Spacer(modifier = Modifier.height(30.dp))
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Row (
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            IconButton(
                                onClick = {
                                    navController.popBackStack()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Return to previous page"
                                )
                            }
                            Text(
                                text = "Order Confirmation",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }


                        Spacer(modifier = Modifier.height(30.dp))
                        // Item and Restaurant Info
                        Column {
                            Text(
                                text = itemName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = restaurantName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Text(
                                    text = "₹${discountedPrice.toInt()} × $quantity = ",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "₹${(discountedPrice * quantity).toInt()}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "Pickup: $pickupTime",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Location: $location",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(30.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { /*TODO*/ },
                                colors = ButtonDefaults.buttonColors( backgroundDark)
                            ) {
                                Text(text = "Payment method", color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = { /*TODO*/ },
                                colors = ButtonDefaults.buttonColors( backgroundDark)
                                ) {
                                Text(text = "Direction to store", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                        Button(
                            onClick = {
                                if (currentUserId.isNotEmpty()) {
                                    val totalPrice = discountedPrice * quantity
                                    val description = "${if (isVegan) "Vegan " else ""}$itemName from $restaurantName"
                                    orderViewModel.createOrder(
                                        userId = currentUserId,
                                        restaurantId = restaurantName.replace(" ", "_").lowercase(),
                                        restaurantName = restaurantName,
                                        itemName = itemName,
                                        itemDescription = description,
                                        price = "₹${totalPrice.toInt()}",
                                        pickupTime = pickupTime,
                                        restaurantAddress = location
                                    )
                                } else {
                                    Toast.makeText(localContext, "Please log in to place order", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = orderCreationState !is OrderCreationState.Loading
                        ) {
                            val buttonState = orderCreationState
                            if (buttonState is OrderCreationState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                            } else {
                                Text(text = "Place Order")
                            }
                        }
                    }


                }

            }
        }


    }


}