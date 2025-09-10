package com.example.plateful.presentation.reviews.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.plateful.R
import com.example.plateful.model.Review
import com.example.plateful.model.ReviewSummary
import java.text.SimpleDateFormat
import java.util.*

// Rating Star Component
@Composable
fun RatingStars(
    rating: Float,
    maxRating: Int = 5,
    starSize: Int = 20,
    onRatingChanged: ((Float) -> Unit)? = null,
    isInteractive: Boolean = onRatingChanged != null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(maxRating) { index ->
            val starRating = index + 1f
            val isFilled = rating >= starRating
            val isHalfFilled = rating >= starRating - 0.5f && rating < starRating
            
            val starColor by animateColorAsState(
                targetValue = when {
                    isFilled -> Color(0xFFFFD700) // Gold
                    isHalfFilled -> Color(0xFFFFD700).copy(alpha = 0.5f)
                    else -> Color.Gray.copy(alpha = 0.3f)
                },
                animationSpec = tween(150)
            )
            
            Icon(
                imageVector = when {
                    isFilled -> Icons.Filled.Star
                    isHalfFilled -> Icons.Filled.StarHalf
                    else -> Icons.Filled.StarBorder
                },
                contentDescription = "Star $starRating",
                tint = starColor,
                modifier = Modifier
                    .size(starSize.dp)
                    .then(
                        if (isInteractive) {
                            Modifier.clickable { onRatingChanged?.invoke(starRating) }
                        } else {
                            Modifier
                        }
                    )
            )
        }
        
        if (!isInteractive && rating > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = String.format("%.1f", rating),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// Review Summary Component
@Composable
fun ReviewSummaryCard(
    reviewSummary: ReviewSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = String.format("%.1f", reviewSummary.averageRating),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    RatingStars(
                        rating = reviewSummary.averageRating,
                        starSize = 18
                    )
                    Text(
                        text = "${reviewSummary.totalReviews} reviews",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                // Rating Distribution
                Column(
                    modifier = Modifier.weight(1f).padding(start = 16.dp)
                ) {
                    reviewSummary.ratingDistribution.entries.reversed().forEach { (star, count) ->
                        RatingDistributionBar(
                            starLevel = star,
                            count = count,
                            totalReviews = reviewSummary.totalReviews
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingDistributionBar(
    starLevel: Int,
    count: Int,
    totalReviews: Int
) {
    val percentage = if (totalReviews > 0) (count.toFloat() / totalReviews) else 0f
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$starLevel",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(12.dp)
        )
        
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(12.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .background(
                    Color.Gray.copy(alpha = 0.2f),
                    RoundedCornerShape(2.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .background(
                        Color(0xFFFFD700),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(20.dp),
            textAlign = TextAlign.End
        )
    }
}

// Individual Review Card
@Composable
fun ReviewCard(
    review: Review,
    onHelpfulClick: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Reviewer Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile Image
                AsyncImage(
                    model = review.userProfileImage ?: R.drawable.trust6,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    fallback = painterResource(R.drawable.trust6)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = review.userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        if (review.isVerifiedPurchase) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                            ) {
                                Text(
                                    text = "Verified",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = formatDate(review.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                // More options
                IconButton(
                    onClick = onReportClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Rating and Menu Item
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RatingStars(
                    rating = review.rating,
                    starSize = 16
                )
                
                if (!review.menuItemName.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "• ${review.menuItemName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Review Text
            if (review.reviewText.isNotEmpty()) {
                Text(
                    text = review.reviewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Review Images
            if (review.reviewImages.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(review.reviewImages) { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Review Image",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Restaurant Response
            review.response?.let { response ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Response from ${response.restaurantManagerName}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = response.responseText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = formatDate(response.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onHelpfulClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ThumbUp,
                            contentDescription = "Helpful",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Helpful (${review.helpfulCount})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Text(
                    text = "Was this review helpful?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// Interactive Rating Input Component
@Composable
fun RatingInput(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Rate your experience",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        RatingStars(
            rating = rating,
            starSize = 32,
            onRatingChanged = onRatingChanged,
            isInteractive = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = when {
                rating == 0f -> "Tap to rate"
                rating <= 1f -> "Terrible"
                rating <= 2f -> "Poor"
                rating <= 3f -> "Average"
                rating <= 4f -> "Good"
                else -> "Excellent"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// Review Text Input Component
@Composable
fun ReviewTextInput(
    reviewText: String,
    onReviewTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Share your experience..."
) {
    OutlinedTextField(
        value = reviewText,
        onValueChange = onReviewTextChanged,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp),
        maxLines = 5
    )
}

// Review Filter Chip
@Composable
fun ReviewFilterChip(
    text: String,
    isSelected: Boolean,
    onSelectionChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onSelectionChanged,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

// Helper function to format date
private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diffInMillis = now.time - date.time
    val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)
    
    return when {
        diffInDays == 0L -> "Today"
        diffInDays == 1L -> "Yesterday"
        diffInDays < 7 -> "${diffInDays} days ago"
        diffInDays < 30 -> "${diffInDays / 7} weeks ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }
}
