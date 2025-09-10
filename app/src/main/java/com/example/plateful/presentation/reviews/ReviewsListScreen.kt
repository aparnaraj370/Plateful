package com.example.plateful.presentation.reviews

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.plateful.model.ReviewFilterOption
import com.example.plateful.model.ReviewSortOption
import com.example.plateful.presentation.reviews.components.ReviewCard
import com.example.plateful.presentation.reviews.components.ReviewFilterChip
import com.example.plateful.presentation.reviews.components.ReviewSummaryCard
import kotlinx.serialization.Serializable

@Serializable
data class NavReviewsListScreen(
    val restaurantId: String,
    val restaurantName: String,
    val menuItemId: String? = null,
    val menuItemName: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsListScreen(
    restaurantId: String,
    restaurantName: String,
    menuItemId: String? = null,
    menuItemName: String? = null,
    navController: NavController,
    viewModel: ReviewViewModel = viewModel()
) {
    val reviewState by viewModel.reviewState.collectAsStateWithLifecycle()
    val canUserReview by viewModel.canUserReview.collectAsStateWithLifecycle()
    val currentSortOption by viewModel.currentSortOption.collectAsStateWithLifecycle()
    val currentFilterOption by viewModel.currentFilterOption.collectAsStateWithLifecycle()
    
    val listState = rememberLazyListState()
    var showSortDialog by remember { mutableStateOf(false) }
    
    // Load reviews when screen opens
    LaunchedEffect(restaurantId, menuItemId) {
        if (menuItemId != null) {
            viewModel.loadMenuItemReviews(restaurantId, menuItemId)
        } else {
            viewModel.loadRestaurantReviews(restaurantId)
        }
    }
    
    // Handle pagination
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()
                val totalItems = listState.layoutInfo.totalItemsCount
                
                if (lastVisibleItem != null && 
                    lastVisibleItem.index >= totalItems - 3 && 
                    !reviewState.isLoading &&
                    reviewState.hasMoreReviews) {
                    viewModel.loadMoreReviews()
                }
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (menuItemId != null) "Item Reviews" else "Reviews",
                            fontWeight = FontWeight.Bold
                        )
                        if (menuItemId != null && menuItemName != null) {
                            Text(
                                text = menuItemName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSortDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = "Sort"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (canUserReview) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(
                            NavAddReviewScreen(
                                restaurantId = restaurantId,
                                restaurantName = restaurantName,
                                menuItemId = menuItemId,
                                menuItemName = menuItemName
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Review",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Review Summary (only for restaurant reviews, not item-specific)
            if (menuItemId == null && reviewState.reviewSummary.totalReviews > 0) {
                ReviewSummaryCard(
                    reviewSummary = reviewState.reviewSummary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(ReviewFilterOption.values()) { filterOption ->
                    ReviewFilterChip(
                        text = filterOption.displayName,
                        isSelected = currentFilterOption == filterOption,
                        onSelectionChanged = {
                            viewModel.setFilterOption(filterOption)
                        }
                    )
                }
            }
            
            // Reviews List
            when {
                reviewState.isLoading && reviewState.reviews.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                reviewState.error != null && reviewState.reviews.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error loading reviews",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = reviewState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (menuItemId != null) {
                                    viewModel.loadMenuItemReviews(restaurantId, menuItemId)
                                } else {
                                    viewModel.loadRestaurantReviews(restaurantId)
                                }
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
                
                reviewState.reviews.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No reviews yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (canUserReview) {
                                "Be the first to leave a review!"
                            } else {
                                "Reviews will appear here once customers start reviewing."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        if (canUserReview) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    navController.navigate(
                                        NavAddReviewScreen(
                                            restaurantId = restaurantId,
                                            restaurantName = restaurantName,
                                            menuItemId = menuItemId,
                                            menuItemName = menuItemName
                                        )
                                    )
                                }
                            ) {
                                Text("Write Review")
                            }
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reviewState.reviews) { review ->
                            ReviewCard(
                                review = review,
                                onHelpfulClick = {
                                    viewModel.markReviewHelpful(review.reviewId)
                                },
                                onReportClick = {
                                    viewModel.reportReview(review.reviewId)
                                }
                            )
                        }
                        
                        // Loading indicator for pagination
                        if (reviewState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Sort Dialog
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = {
                Text("Sort Reviews By")
            },
            text = {
                Column {
                    ReviewSortOption.values().forEach { sortOption ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSortOption == sortOption,
                                onClick = {
                                    viewModel.setSortOption(sortOption)
                                    showSortDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sortOption.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSortDialog = false }
                ) {
                    Text("Done")
                }
            }
        )
    }
}
