package com.example.plateful.presentation.restaurant

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.serialization.Serializable
import com.example.plateful.R
import com.example.plateful.presentation.reviews.NavReviewsListScreen
import com.example.plateful.presentation.reviews.components.RatingStars
import com.example.plateful.presentation.reviews.components.ReviewSummaryCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.plateful.presentation.reviews.ReviewViewModel
import com.example.plateful.presentation.login.Screens.Card
import com.example.plateful.presentation.login.Screens.TopOffers
import com.example.plateful.presentation.itemdetailscreen.NavItemDetailScreen

@Serializable
data class NavRestaurantProfileScreen(
    val restaurantName: String,
    val restaurantId: String
)

data class RestaurantInfo(
    val name: String,
    val cuisine: String,
    val rating: Float,
    val totalReviews: Int,
    val deliveryTime: String,
    val distance: String,
    val address: String,
    val phoneNumber: String,
    val description: String,
    val operatingHours: String,
    val isVeg: Boolean,
    val averagePrice: String,
    val imageResId: Int = R.drawable.trust6
)

data class Review(
    val userName: String,
    val rating: Float,
    val comment: String,
    val date: String,
    val userImageResId: Int = R.drawable.trust6
)

@Composable
fun RestaurantProfileScreen(
    navController: NavController,
    restaurantName: String,
    restaurantId: String,
    reviewViewModel: ReviewViewModel = viewModel()
) {
    // Mock data - in real app this would come from API
    val restaurantInfo = RestaurantInfo(
        name = restaurantName,
        cuisine = when {
            restaurantName.contains("Biryani", ignoreCase = true) -> "Indian, Biryani"
            restaurantName.contains("Pizza", ignoreCase = true) -> "Italian, Fast Food"
            restaurantName.contains("Thai", ignoreCase = true) -> "Thai, Asian"
            restaurantName.contains("Sushi", ignoreCase = true) -> "Japanese, Sushi"
            restaurantName.contains("Mediterranean", ignoreCase = true) -> "Mediterranean"
            restaurantName.contains("Tibet", ignoreCase = true) -> "Tibetan, Chinese"
            restaurantName.contains("Chai", ignoreCase = true) -> "Cafe, Beverages"
            else -> "Multi-cuisine"
        },
        rating = 4.5f,
        totalReviews = 1247,
        deliveryTime = "30-45 mins",
        distance = "2.1 km",
        address = "123 Food Street, Near Central Mall, Delhi",
        phoneNumber = "+91 98765 43210",
        description = "Experience authentic flavors at $restaurantName. We serve fresh, delicious food made with the finest ingredients. Perfect for food lovers seeking quality and taste.",
        operatingHours = "9:00 AM - 11:00 PM",
        isVeg = restaurantName.contains("Veg", ignoreCase = true) || restaurantName.contains("Healthy", ignoreCase = true),
        averagePrice = "₹200 for two"
    )

    // Mock menu items - filter from existing cards
    val mockMenuItems = listOf(
        Card(
            Menu = if (restaurantName.contains("Tibet")) "Veg Momos" else "Special ${restaurantName} Dish",
            RestroName = restaurantName,
            Offer = "20% OFF",
            Icon = Icons.Rounded.ArrowForwardIos,
            color = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(Color(0xFF4CAF50), Color(0xFF45a049))
            ),
            categories = listOf("Main Course"),
            price = 200f,
            discountedPrice = 160f,
            distance = 2.1f,
            rating = 4.5f,
            isVegan = restaurantInfo.isVeg,
            location = "Delhi",
            pickupTime = "30-40 min"
        ),
        Card(
            Menu = "Popular ${restaurantName} Special",
            RestroName = restaurantName,
            Offer = "15% OFF",
            Icon = Icons.Rounded.ArrowForwardIos,
            color = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(Color(0xFFFF9800), Color(0xFFF57C00))
            ),
            categories = listOf("Main Course"),
            price = 300f,
            discountedPrice = 255f,
            distance = 2.1f,
            rating = 4.7f,
            isVegan = false,
            location = "Delhi",
            pickupTime = "25-35 min"
        )
    )

    // Load reviews when screen opens
    val reviewState by reviewViewModel.reviewState.collectAsStateWithLifecycle()
    
    LaunchedEffect(restaurantId) {
        reviewViewModel.loadRestaurantReviews(restaurantId)
    }

    var selectedTab by remember { mutableStateOf("Menu") }
    val tabs = listOf("Menu", "Reviews", "Info")

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Restaurant Header
        item {
            RestaurantHeader(
                restaurantInfo = restaurantInfo,
                reviewState = reviewState,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Tab Row
        item {
            TabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTab == title,
                        onClick = { selectedTab = title }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tab Content
        when (selectedTab) {
            "Menu" -> {
                item {
                    MenuSection(
                        menuItems = mockMenuItems,
                        onItemClick = { card ->
                            navController.navigate(
                                NavItemDetailScreen(
                                    itemName = card.Menu,
                                    restaurantName = card.RestroName,
                                    originalPrice = card.price,
                                    discountedPrice = card.discountedPrice,
                                    rating = card.rating,
                                    distance = card.distance,
                                    location = card.location,
                                    pickupTime = card.pickupTime,
                                    isVegan = card.isVegan
                                )
                            )
                        }
                    )
                }
            }
            "Reviews" -> {
                item {
                    ReviewsSection(
                        navController = navController,
                        restaurantId = restaurantId,
                        restaurantName = restaurantName,
                        reviewState = reviewState
                    )
                }
            }
            "Info" -> {
                item {
                    RestaurantInfoSection(restaurantInfo = restaurantInfo)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantHeader(
    restaurantInfo: RestaurantInfo,
    reviewState: com.example.plateful.model.ReviewState,
    onBackClick: () -> Unit
) {
    Box {
        // Background Image
        Image(
            painter = painterResource(id = restaurantInfo.imageResId),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentScale = ContentScale.Crop
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Restaurant Info Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = restaurantInfo.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = restaurantInfo.cuisine,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${reviewState.reviewSummary.averageRating} (${reviewState.reviewSummary.totalReviews} reviews)",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = restaurantInfo.deliveryTime,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun MenuSection(
    menuItems: List<Card>,
    onItemClick: (Card) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Popular Items",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        menuItems.forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onItemClick(item) },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.trust6),
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.Menu,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row {
                            Text(
                                text = "₹${item.discountedPrice.toInt()}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "₹${item.price.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = item.rating.toString(),
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            if (item.isVegan) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFF4CAF50),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "VEG",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to cart",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            )
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewItem(review: Review) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = review.userImageResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = review.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun RestaurantInfoSection(restaurantInfo: RestaurantInfo) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        InfoItem(
            icon = Icons.Default.LocationOn,
            title = "Address",
            content = restaurantInfo.address
        )
        
        InfoItem(
            icon = Icons.Default.Phone,
            title = "Phone",
            content = restaurantInfo.phoneNumber
        )
        
        InfoItem(
            icon = Icons.Default.Schedule,
            title = "Operating Hours",
            content = restaurantInfo.operatingHours
        )
        
        InfoItem(
            icon = Icons.Default.AttachMoney,
            title = "Average Price",
            content = restaurantInfo.averagePrice
        )
        
        InfoItem(
            icon = Icons.Default.Info,
            title = "About",
            content = restaurantInfo.description
        )
    }
}

@Composable
fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ReviewsSection(
    navController: NavController,
    restaurantId: String,
    restaurantName: String,
    reviewState: com.example.plateful.model.ReviewState
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Review Summary
        if (reviewState.reviewSummary.totalReviews > 0) {
            ReviewSummaryCard(
                reviewSummary = reviewState.reviewSummary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Reviews Header with "See All" button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Reviews",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            if (reviewState.reviewSummary.totalReviews > 3) {
                TextButton(
                    onClick = {
                        navController.navigate(
                            NavReviewsListScreen(
                                restaurantId = restaurantId,
                                restaurantName = restaurantName
                            )
                        )
                    }
                ) {
                    Text("See All")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Show recent reviews or empty state
        when {
            reviewState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            reviewState.reviewSummary.recentReviews.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "No reviews yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "Be the first to review this restaurant!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            else -> {
                // Show first 3 recent reviews
                reviewState.reviewSummary.recentReviews.take(3).forEach { review ->
                    com.example.plateful.presentation.reviews.components.ReviewCard(
                        review = review,
                        onHelpfulClick = { /* Handle in full reviews screen */ },
                        onReportClick = { /* Handle in full reviews screen */ },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // "View All Reviews" button if there are more
                if (reviewState.reviewSummary.totalReviews > 3) {
                    Button(
                        onClick = {
                            navController.navigate(
                                NavReviewsListScreen(
                                    restaurantId = restaurantId,
                                    restaurantName = restaurantName
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View All ${reviewState.reviewSummary.totalReviews} Reviews")
                    }
                }
            }
        }
    }
}
