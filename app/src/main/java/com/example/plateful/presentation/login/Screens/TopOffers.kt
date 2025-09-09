package com.example.plateful.presentation.login.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plateful.R
import com.example.plateful.ui.theme.BlueEnd
import com.example.plateful.ui.theme.BlueStart
import com.example.plateful.ui.theme.GreenEnd
import com.example.plateful.ui.theme.GreenStart
import com.example.plateful.ui.theme.OrangeEnd
import com.example.plateful.ui.theme.OrangeStart
import com.example.plateful.ui.theme.PurpleEnd
import com.example.plateful.ui.theme.PurpleStart

data class Card(
    val Menu: String,
    val RestroName: String,
    val Offer: String,
    val Icon: ImageVector,
    val color: Brush,
    val categories: List<String>,
    val price: Float = 0f, // Original price in rupees
    val discountedPrice: Float = 0f, // Discounted price
    val distance: Float = 0f, // Distance in km
    val rating: Float = 4.0f,
    val isVegan: Boolean = false,
    val location: String = "Central Delhi",
    val pickupTime: String = "30-45 min"
)

val allCategories = listOf(
    "Appetizer", "Main Course", "Dessert", "Beverage", "Snack", "Salad"
)

val cards = listOf(
    // BUDGET FRIENDLY (Under ₹100)
    Card(
        Menu = "Veg Momos",
        RestroName = "Tibet Corner",
        Offer = "₹40 Only",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(GreenStart, GreenEnd),
        categories = listOf("Snack", "Appetizer"),
        price = 80f,
        discountedPrice = 40f,
        distance = 0.5f,
        rating = 4.2f,
        isVegan = true,
        location = "Near You",
        pickupTime = "15-20 min"
    ),
    Card(
        Menu = "Samosa Platter",
        RestroName = "Tripathi Brothers",
        Offer = "₹50 for 4pcs",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(BlueStart, BlueEnd),
        categories = listOf("Snack", "Appetizer"),
        price = 100f,
        discountedPrice = 50f,
        distance = 1.2f,
        rating = 4.0f,
        isVegan = true,
        location = "Connaught Place",
        pickupTime = "20-30 min"
    ),
    Card(
        Menu = "Tea & Biscuits",
        RestroName = "Chai Sutta Bar",
        Offer = "₹25 Only",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(OrangeStart, OrangeEnd),
        categories = listOf("Beverage", "Snack"),
        price = 50f,
        discountedPrice = 25f,
        distance = 0.3f,
        rating = 3.8f,
        isVegan = false,
        location = "Near You",
        pickupTime = "10-15 min"
    ),
    Card(
        Menu = "Fruit Salad",
        RestroName = "Healthy Bowl",
        Offer = "₹60 Only",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(GreenStart, BlueEnd),
        categories = listOf("Salad", "Dessert"),
        price = 120f,
        discountedPrice = 60f,
        distance = 0.8f,
        rating = 4.5f,
        isVegan = true,
        location = "Lajpat Nagar",
        pickupTime = "15-25 min"
    ),
    
    // MID RANGE (₹100-300)
    Card(
        Menu = "Paneer Butter Masala",
        RestroName = "Punjab Kitchen",
        Offer = "₹150 (50% Off)",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(OrangeStart, PurpleEnd),
        categories = listOf("Main Course"),
        price = 300f,
        discountedPrice = 150f,
        distance = 2.1f,
        rating = 4.3f,
        isVegan = false,
        location = "Karol Bagh",
        pickupTime = "25-35 min"
    ),
    Card(
        Menu = "Chicken Biryani",
        RestroName = "Hyderabadi Biryani House",
        Offer = "₹180 (40% Off)",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(PurpleStart, GreenEnd),
        categories = listOf("Main Course"),
        price = 300f,
        discountedPrice = 180f,
        distance = 3.5f,
        rating = 4.7f,
        isVegan = false,
        location = "Nizamuddin",
        pickupTime = "35-45 min"
    ),
    Card(
        Menu = "Margherita Pizza",
        RestroName = "Pizza Corner",
        Offer = "₹200 (33% Off)",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(GreenStart, OrangeEnd),
        categories = listOf("Main Course"),
        price = 300f,
        discountedPrice = 200f,
        distance = 1.5f,
        rating = 4.1f,
        isVegan = false,
        location = "Greater Kailash",
        pickupTime = "20-30 min"
    ),
    Card(
        Menu = "Vegan Buddha Bowl",
        RestroName = "Green Valley",
        Offer = "₹220 (25% Off)",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(GreenStart, GreenEnd),
        categories = listOf("Salad", "Main Course"),
        price = 280f,
        discountedPrice = 220f,
        distance = 2.8f,
        rating = 4.6f,
        isVegan = true,
        location = "Hauz Khas",
        pickupTime = "30-40 min"
    ),
    
    // PREMIUM (Above ₹300)
    Card(
        Menu = "Tandoori Platter",
        RestroName = "Royal Mughal",
        Offer = "₹350 (30% Off)",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(PurpleStart, PurpleEnd),
        categories = listOf("Main Course", "Appetizer"),
        price = 500f,
        discountedPrice = 350f,
        distance = 4.2f,
        rating = 4.8f,
        isVegan = false,
        location = "Khan Market",
        pickupTime = "40-50 min"
    ),
    Card(
        Menu = "Lobster Thermidor",
        RestroName = "Fine Dine Bistro",
        Offer = "₹800 (20% Off)",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(BlueStart, PurpleEnd),
        categories = listOf("Main Course"),
        price = 1000f,
        discountedPrice = 800f,
        distance = 5.5f,
        rating = 4.9f,
        isVegan = false,
        location = "Cyber City",
        pickupTime = "45-60 min"
    ),
    
    // DESSERTS & BEVERAGES
    Card(
        Menu = "Chocolate Cake Slice",
        RestroName = "Sweet Dreams Bakery",
        Offer = "₹80 (50% Off)",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(PurpleStart, BlueEnd),
        categories = listOf("Dessert"),
        price = 160f,
        discountedPrice = 80f,
        distance = 1.1f,
        rating = 4.4f,
        isVegan = false,
        location = "Rajouri Garden",
        pickupTime = "15-25 min"
    ),
    Card(
        Menu = "Fresh Smoothie Bowl",
        RestroName = "Juice Junction",
        Offer = "₹120 (40% Off)",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(GreenStart, OrangeEnd),
        categories = listOf("Beverage", "Dessert"),
        price = 200f,
        discountedPrice = 120f,
        distance = 0.9f,
        rating = 4.3f,
        isVegan = true,
        location = "Laxmi Nagar",
        pickupTime = "10-20 min"
    ),
    Card(
        Menu = "Kulfi Falooda",
        RestroName = "Desi Ice Cream",
        Offer = "₹90 (40% Off)",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(OrangeStart, PurpleEnd),
        categories = listOf("Dessert"),
        price = 150f,
        discountedPrice = 90f,
        distance = 1.7f,
        rating = 4.2f,
        isVegan = false,
        location = "Chandni Chowk",
        pickupTime = "20-30 min"
    ),
    
    // FAST PICKUP OPTIONS
    Card(
        Menu = "Instant Noodles",
        RestroName = "Quick Bites",
        Offer = "₹35 Ready!",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(BlueStart, GreenEnd),
        categories = listOf("Snack"),
        price = 60f,
        discountedPrice = 35f,
        distance = 0.2f,
        rating = 3.9f,
        isVegan = true,
        location = "Near You",
        pickupTime = "5-10 min"
    ),
    Card(
        Menu = "Sandwich Combo",
        RestroName = "Express Eatery",
        Offer = "₹70 Ready!",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(GreenStart, BlueEnd),
        categories = listOf("Snack", "Main Course"),
        price = 120f,
        discountedPrice = 70f,
        distance = 0.4f,
        rating = 4.0f,
        isVegan = false,
        location = "Near You",
        pickupTime = "8-12 min"
    ),
    
    // INTERNATIONAL CUISINE
    Card(
        Menu = "Pad Thai",
        RestroName = "Thai Garden",
        Offer = "₹280 (30% Off)",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(OrangeStart, GreenEnd),
        categories = listOf("Main Course"),
        price = 400f,
        discountedPrice = 280f,
        distance = 3.2f,
        rating = 4.5f,
        isVegan = false,
        location = "Defence Colony",
        pickupTime = "35-45 min"
    ),
    Card(
        Menu = "Vegan Sushi Roll",
        RestroName = "Tokyo Express",
        Offer = "₹320 (20% Off)",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(BlueStart, PurpleEnd),
        categories = listOf("Main Course", "Appetizer"),
        price = 400f,
        discountedPrice = 320f,
        distance = 4.8f,
        rating = 4.7f,
        isVegan = true,
        location = "Saket",
        pickupTime = "40-50 min"
    ),
    Card(
        Menu = "Mediterranean Bowl",
        RestroName = "Olive Branch",
        Offer = "₹250 (35% Off)",
        Icon = Icons.Rounded.ArrowForwardIos,
        color = getGradiet(GreenStart, PurpleEnd),
        categories = listOf("Salad", "Main Course"),
        price = 380f,
        discountedPrice = 250f,
        distance = 2.5f,
        rating = 4.4f,
        isVegan = true,
        location = "Vasant Vihar",
        pickupTime = "25-35 min"
    )
)

fun getGradiet(
    startColor: Color,
    endColor: Color,
): Brush {
    return Brush.horizontalGradient(
        colors = listOf(startColor, endColor)
    )
}

@Preview
@Composable
fun CardsSection() {
    CardsSection(cards)
}

@Composable
fun CardsSection(
    offers: List<Card>,
    onClick: ((Card) -> Unit)? = null
) {
    if (offers.isEmpty()) {
        Box(
            Modifier
                .height(120.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text("No offers available in this category")
        }
    } else {
        LazyRow {
            items(offers.size) { index ->
                TopOffers(
                    card = offers[index],
                    onClick = { card -> onClick?.invoke(card) }
                )
            }
        }
    }
}

@Composable
fun TopOffers(
    card: Card,
    onClick: ((Card) -> Unit)? = null
) {
    val lastItemPaddingEnd = 0.dp
    // This padding decision will be handled in the row for the last card
    Box(
        modifier = Modifier.padding(start = 16.dp, end = lastItemPaddingEnd)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(25.dp))
                .background(card.color)
                .width(250.dp)
                .height(150.dp)
                .clickable { onClick?.invoke(card) }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = card.RestroName,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = card.Offer,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Row() {
                Text(
                    text = card.Menu,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    modifier = Modifier.clickable { },
                    tint = Color.White,
                    imageVector = card.Icon,
                    contentDescription = " Menu"
                )
            }
        }
    }
}

