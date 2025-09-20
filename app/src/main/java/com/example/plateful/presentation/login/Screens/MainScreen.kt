package com.example.plateful.presentation.login.Screens


import android.content.Intent
import android.util.Log

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Down
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Up
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.plateful.model.TopOffersViewModel
import com.example.plateful.presentation.itemdetailscreen.NavItemDetailScreen
import com.example.plateful.presentation.restaurant.NavRestaurantProfileScreen
import com.example.plateful.presentation.login.BottomNavigation.BottomNavigationItem
import com.example.plateful.presentation.profilescreens.ProfileScreen
import com.example.plateful.presentation.profilescreens.TopAppBarProfileScreen
import com.example.plateful.presentation.searchFilter.NavSearchScreen
import com.example.plateful.presentation.owner.NavOwnerDashboard
import com.example.plateful.domain.services.SessionManager
import com.example.plateful.domain.services.UserRoleServiceImpl
import com.example.plateful.domain.model.UserType
import androidx.compose.runtime.LaunchedEffect
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.Color
import com.example.plateful.presentation.MainActivity
import timber.log.Timber

@Serializable
object NavMainScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userRoleService = remember { UserRoleServiceImpl() }
    val auth = remember { FirebaseAuth.getInstance() }
    var isRestaurantOwner by rememberSaveable { mutableStateOf(false) }
    
    // Check if user is restaurant owner using both SessionManager and UserRoleService
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // First try SessionManager (SharedPreferences)
            val sessionUserType = sessionManager.getCurrentUserType()
            Timber.d("UserType from SessionManager: $sessionUserType")
            
            // Then try UserRoleService (direct Firestore check)
            val userRole = userRoleService.getUserRole(currentUser.uid)
            Timber.d("UserRole from UserRoleService: $userRole")
            
            // Use UserRoleService as the source of truth
            isRestaurantOwner = userRole.userType == UserType.RESTAURANT_OWNER
            Timber.d("Final isRestaurantOwner flag: $isRestaurantOwner")
            
            // Update SessionManager if there's a mismatch
            if (sessionUserType != userRole.userType && userRole.userType == UserType.RESTAURANT_OWNER) {
                sessionManager.updateUserRole(userRole.restaurantId)
            }
        }
    }
    
    val isRequestDrawer = rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val items = listOf(
        BottomNavigationItem(
            title = "Order",
            selectedIcon = Icons.Filled.AutoStories,
            unselectedIcon = Icons.Outlined.AutoStories
        ),
        BottomNavigationItem(
            title = "You",
            selectedIcon = Icons.Filled.AccountCircle,
            unselectedIcon = Icons.Outlined.AccountCircle
        ),
    )
    var selectedItemIndex by rememberSaveable {
        mutableStateOf(0)
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Top Row: Icon + App Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "App Icon",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Plateful",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Main Navigation Items
                    NavigationDrawerItem(
                        label = { Text("Home") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedItemIndex = 0
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    NavigationDrawerItem(
                        label = { Text("Profile") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedItemIndex = 1
                        },
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Owner Dashboard - only show for restaurant owners
                    Timber.d("outside isRestaurantOwner flag: $isRestaurantOwner")
                    if (isRestaurantOwner) {
                        NavigationDrawerItem(
                            label = { Text("Owner Dashboard") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(NavOwnerDashboard)
                            },
                            icon = { Icon(Icons.Default.Business, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Spacer to visually separate bottom section
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // View Map
                    NavigationDrawerItem(
                        label = { Text("View Map") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedItemIndex = 2
                        },
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dark Mode in SAME LINE with Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = "Dark Mode Icon",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Dark Mode", style = MaterialTheme.typography.bodyLarge)
                        }

                        Switch(
                            checked = MainActivity.isDarkModeEnabled.value,
                            onCheckedChange = { isChecked ->
                                MainActivity.isDarkModeEnabled.value = isChecked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFD600),
                                checkedTrackColor = Color(0xFFFFD600).copy(alpha = 0.4f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }
            // Add more items as needed
                }
            }
        },
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
                // --- ADD VIEWMODEL INJECTION ---
                val topOffersViewModel: TopOffersViewModel = viewModel()
                Log.d("MainScreen", "🎆 TopOffersViewModel instantiated in MainScreen")
                Scaffold(
                    modifier = if (selectedItemIndex == 0) Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                    else Modifier.fillMaxSize(),
                    topBar = {
                        if (selectedItemIndex == 1)
                            TopAppBarProfileScreen()
                        else
                            LargeTopAppBar(
                                title = { Text(text = "Plateful") },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch { drawerState.open() }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Menu"
                                        )
                                    }
                                },
                                actions = {
                                    // Add refresh button for food packs
                                    if (selectedItemIndex == 0) {
                                        IconButton(onClick = { 
                                            Log.d("MainScreen", "🔄 MANUAL REFRESH BUTTON CLICKED")
                                            Log.d("MainScreen", "Current offers count before refresh: ${topOffersViewModel.filteredOffers.size}")
                                            topOffersViewModel.forceRefresh() // Use force refresh to bypass cooldown
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Search, // Using Search as refresh for now
                                                contentDescription = "Force Refresh Food Packs"
                                            )
                                        }
                                        
                                        // Debug button to check all documents
                                        IconButton(onClick = { 
                                            Log.d("MainScreen", "🔍 DEBUG BUTTON CLICKED")
                                            topOffersViewModel.debugCheckAllFoodPacks()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.FilterList,
                                                contentDescription = "Debug Check All Food Packs"
                                            )
                                        }
                                    }
                                    IconButton(onClick = { /*TODO*/ }) {
                                        Icon(
                                            imageVector = Icons.Rounded.ShoppingCart,
                                            contentDescription = "Cart"
                                        )
                                    }
                                },
                                scrollBehavior = scrollBehavior,
                            )
                    },
                    bottomBar = {
                        NavigationBar {
                            items.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = selectedItemIndex == index,
                                    onClick = {
                                        selectedItemIndex = index
                                    },
                                    label = { Text(text = item.title) },
                                    alwaysShowLabel = false,
                                    icon = {
                                        Icon(
                                            imageVector = if (index == selectedItemIndex) {
                                                item.selectedIcon
                                            } else {
                                                item.unselectedIcon
                                            },
                                            contentDescription = item.title
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    AnimatedContent(
                        targetState = selectedItemIndex,
                        label = "ScreenAnimation",
                        transitionSpec = {
                            val direction = if (targetState > initialState) {
                                AnimatedContentTransitionScope.SlideDirection.Left
                            } else {
                                AnimatedContentTransitionScope.SlideDirection.Right
                            }
                            slideIntoContainer(
                                towards = direction,
                                animationSpec = tween(300, easing = EaseIn)
                            ).togetherWith(
                                slideOutOfContainer(
                                    towards = direction,
                                    animationSpec = tween(300, easing = EaseOut)
                                )
                            )
                        }

                        ) { targetState ->
                        when (targetState) {
                            0 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                                ) {
                                    // Enhanced Search and Filter Section
                                    SearchAndFilterSection(
                                        onSearchClick = { navController.navigate(NavSearchScreen) },
                                        onFilterClick = { navController.navigate(NavSearchScreen) }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Quick Filters
                                    QuickFiltersRow()
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // --- CATEGORY CHIP ROW UI ---
                                    CategoryChipRow(
                                        categories = topOffersViewModel.categories,
                                        selected = topOffersViewModel.selectedCategories,
                                        onChipClick = { topOffersViewModel.onCategoryChipClick(it) }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    val uiState by topOffersViewModel.uiState.collectAsState()
                                    
                                    // Enhanced debug logging for UI state
                                    LaunchedEffect(uiState) {
                                        Log.d("MainScreen", "=== MAIN SCREEN UI STATE UPDATE ===")
                                        Log.d("MainScreen", "📊 UI State updated:")
                                        Log.d("MainScreen", "  - Loading: ${uiState.isLoading}")
                                        Log.d("MainScreen", "  - Error: ${uiState.error}")
                                        Log.d("MainScreen", "  - Direct offers count: ${uiState.offers.size}")
                                        Log.d("MainScreen", "  - Filtered offers count: ${topOffersViewModel.filteredOffers.size}")
                                        Log.d("MainScreen", "  - All offers count: ${topOffersViewModel.allOffers.size}")
                                        
                                        // Log actual offers
                                        if (uiState.offers.isNotEmpty()) {
                                            Log.d("MainScreen", "🎆 Direct offers details:")
                                            uiState.offers.forEachIndexed { index, offer ->
                                                Log.d("MainScreen", "  ${index + 1}: ${offer.Menu} at ${offer.RestroName} - ${offer.Offer}")
                                            }
                                        }
                                        
                                        if (topOffersViewModel.filteredOffers.isNotEmpty()) {
                                            Log.d("MainScreen", "🎆 Filtered offers details:")
                                            topOffersViewModel.filteredOffers.take(3).forEachIndexed { index, offer ->
                                                Log.d("MainScreen", "  ${index + 1}: ${offer.Menu} at ${offer.RestroName} - ${offer.Offer}")
                                            }
                                            if (topOffersViewModel.filteredOffers.size > 3) {
                                                Log.d("MainScreen", "  ... and ${topOffersViewModel.filteredOffers.size - 3} more")
                                            }
                                        } else {
                                            Log.w("MainScreen", "⚠️ NO FILTERED OFFERS TO DISPLAY!")
                                        }
                                        
                                        Log.d("MainScreen", "=== END MAIN SCREEN UI STATE UPDATE ===")
                                    }
                                    
                                    // Show loading indicator if loading and no offers available
                                    if (uiState.isLoading && topOffersViewModel.filteredOffers.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CircularProgressIndicator()
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Loading fresh food packs...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else if (uiState.error != null && topOffersViewModel.filteredOffers.isEmpty()) {
                                        // Show error state with retry option
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "Failed to load food packs",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = uiState.error ?: "Unknown error",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(
                                                    onClick = { topOffersViewModel.refreshOffers() }
                                                ) {
                                                    Text("Retry")
                                                }
                                            }
                                        }
                                    } else {
                                        // Show food pack cards
                                        CardsSection(
                                            offers = topOffersViewModel.filteredOffers,
                                            onClick = { card ->
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
                                            },
                                            onRestaurantClick = { restaurantName ->
                                                navController.navigate(
                                                    NavRestaurantProfileScreen(
                                                        restaurantName = restaurantName,
                                                        restaurantId = restaurantName.replace(" ", "_").lowercase()
                                                    )
                                                )
                                            }
                                        )
                                        
                                        // Show a small indicator if there was an error but we have fallback data
                                        if (uiState.error != null && topOffersViewModel.filteredOffers.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = "⚠️ Showing cached results. Pull to refresh for latest offers.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                ProfileScreen(paddingValues, navController)
                            }
                            2 -> {
                                @Suppress("WrongTimberUsage")

                                Timber.d("View Map")
//                                val context = LocalContext.current
//                                selectedItemIndex = 0
//                                val intent = Intent(context, MapScreen::class.java)
//                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }
        })
    }

/**
 * CategoryChipRow renders a horizontal list of selectable category chips.
 * @param categories List of all category names (including 'All').
 * @param selected Set of currently selected categories (show highlighted).
 * @param onChipClick Lambda invoked with category when chip is clicked.
 */
@Composable
fun CategoryChipRow(
    categories: List<String>,
    selected: Set<String>,
    onChipClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .padding(top = 14.dp, bottom = 0.dp, start = 12.dp, end = 12.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories) { cat ->
            val isSelected = selected.contains(cat)
            FilterChip(
                selected = isSelected,
                onClick = { onChipClick(cat) },
                label = {
                    Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                },
                modifier = Modifier,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun SearchAndFilterSection(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Bar
        OutlinedTextField(
            value = "",
            onValueChange = { },
            placeholder = { 
                Text(
                    text = "Search for food, restaurants...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier
                .weight(1f)
                .clickable { onSearchClick() },
            enabled = false, // Make it clickable but not editable here
            shape = RoundedCornerShape(24.dp),
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledIndicatorColor = Color.Transparent
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Filter Button
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clickable { onFilterClick() },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 2.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun QuickFiltersRow() {
    Column {
        Text(
            text = "Quick Filters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickFilters) { filter ->
                QuickFilterChip(
                    text = filter.text,
                    icon = filter.icon,
                    onClick = { /* Handle quick filter click */ }
                )
            }
        }
    }
}

@Composable
fun QuickFilterChip(
    text: String,
    icon: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

data class QuickFilter(
    val text: String,
    val icon: String
)

val quickFilters = listOf(
    QuickFilter("Near Me", "📍"),
    QuickFilter("Under ₹100", "💰"),
    QuickFilter("Fast Pickup", "⚡"),
    QuickFilter("Vegan", "🌱"),
    QuickFilter("Desserts", "🍰"),
    QuickFilter("Indian", "🍛")
)

@Preview
@Composable
fun MainScreenPreview() {
    val context = LocalContext.current
    MainScreen(navController = NavController(context = context))
}
