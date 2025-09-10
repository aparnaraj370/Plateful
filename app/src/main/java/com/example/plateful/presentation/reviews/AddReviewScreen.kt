package com.example.plateful.presentation.reviews

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.plateful.presentation.reviews.components.RatingInput
import com.example.plateful.presentation.reviews.components.ReviewTextInput
import kotlinx.serialization.Serializable

@Serializable
data class NavAddReviewScreen(
    val restaurantId: String,
    val restaurantName: String,
    val menuItemId: String? = null,
    val menuItemName: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewScreen(
    restaurantId: String,
    restaurantName: String,
    menuItemId: String? = null,
    menuItemName: String? = null,
    navController: NavController,
    viewModel: ReviewViewModel = viewModel()
) {
    val context = LocalContext.current
    val addReviewState by viewModel.addReviewState.collectAsStateWithLifecycle()
    
    // Handle submission result
    LaunchedEffect(addReviewState.isSubmitted) {
        if (addReviewState.isSubmitted) {
            Toast.makeText(context, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }
    
    // Handle errors
    LaunchedEffect(addReviewState.error) {
        addReviewState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Write Review",
                        fontWeight = FontWeight.Bold
                    )
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
                    TextButton(
                        onClick = {
                            if (addReviewState.rating > 0) {
                                viewModel.addReview(
                                    restaurantId = restaurantId,
                                    restaurantName = restaurantName,
                                    menuItemId = menuItemId,
                                    menuItemName = menuItemName
                                )
                            }
                        },
                        enabled = addReviewState.rating > 0 && !addReviewState.isSubmitting
                    ) {
                        if (addReviewState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Submit"
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Submit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Restaurant Info Card
            RestaurantInfoCard(
                restaurantName = restaurantName,
                menuItemName = menuItemName
            )
            
            // Rating Section
            RatingInput(
                rating = addReviewState.rating,
                onRatingChanged = viewModel::updateReviewRating,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Review Text Section
            Column {
                Text(
                    text = "Share your experience",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ReviewTextInput(
                    reviewText = addReviewState.reviewText,
                    onReviewTextChanged = viewModel::updateReviewText,
                    placeholder = "Tell others about your experience. What did you like or dislike?"
                )
                
                Text(
                    text = "${addReviewState.reviewText.length}/500 characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Review Guidelines
            ReviewGuidelines()
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RestaurantInfoCard(
    restaurantName: String,
    menuItemName: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Reviewing",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = restaurantName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            menuItemName?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• $it",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ReviewGuidelines() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Review Guidelines",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val guidelines = listOf(
                "Be honest and helpful to other users",
                "Focus on your personal experience",
                "Mention specific details about the food and service",
                "Be respectful in your language",
                "Avoid posting personal information"
            )
            
            guidelines.forEach { guideline ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = guideline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
