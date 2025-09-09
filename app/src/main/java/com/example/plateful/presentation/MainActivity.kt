package com.example.plateful.presentation


import NavOrderingPickup
import OrderingPickup
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.toRoute

import com.google.firebase.auth.FirebaseAuth
import com.example.plateful.R
import com.example.plateful.domain.services.AccountService
import com.example.plateful.domain.services.AccountServiceImpl
import com.example.plateful.model.UserEntity
import com.example.plateful.presentation.address.AddAddressScreen
import com.example.plateful.presentation.address.AddressScreen
import com.example.plateful.presentation.communityScreen.CommunityScreen
import com.example.plateful.presentation.communityScreen.NavCommunityScreen
import com.example.plateful.presentation.itemdetailscreen.AddingLeftovers
import com.example.plateful.presentation.itemdetailscreen.ItemDetailScreen
import com.example.plateful.presentation.itemdetailscreen.NavAddingLeftovers
import com.example.plateful.presentation.itemdetailscreen.NavItemDetailScreen
import com.example.plateful.presentation.itemdetailscreen.NavOrderConfirmScreen
import com.example.plateful.presentation.itemdetailscreen.OrderConfirmScreen
import com.example.plateful.presentation.itemdetailscreen.SuccessScreen
import com.example.plateful.presentation.login.NavPersonalDetails
import com.example.plateful.presentation.login.NavWaitScreen
import com.example.plateful.presentation.login.PersonalDetails
import com.example.plateful.presentation.login.Screens.MainScreen
import com.example.plateful.presentation.login.Screens.NavMainScreen
import com.example.plateful.presentation.login.Screens.NavRoleSelectScreen
import com.example.plateful.presentation.login.Screens.PlatefulUserSelection
import com.example.plateful.presentation.login.SignInDataViewModel
import com.example.plateful.presentation.login.WaitScreen
import com.example.plateful.presentation.login.emaillogin.EmailSignInViewModel
import com.example.plateful.presentation.login.emaillogin.EmailSignInViewModelFactory
import com.example.plateful.presentation.login.emaillogin.ForgotPassword
import com.example.plateful.presentation.login.emaillogin.NavEmailSignIn
import com.example.plateful.presentation.login.emaillogin.NavForgotPassword
import com.example.plateful.presentation.login.emaillogin.SignInUsingEmail
import com.example.plateful.presentation.login.mainlogin.MainSignInViewModel
import com.example.plateful.presentation.login.mainlogin.MainSignInViewModelFactory
import com.example.plateful.presentation.login.mainlogin.NavOTPVerificationUI
import com.example.plateful.presentation.login.mainlogin.NavSignInUI
import com.example.plateful.presentation.login.mainlogin.OTPVerificationUI
import com.example.plateful.presentation.login.mainlogin.SignInUI
import com.example.plateful.presentation.onboardingProcess.NavOnboarding
import com.example.plateful.presentation.onboardingProcess.Onboarding
import com.example.plateful.presentation.welcome.NavWelcomeScreen
import com.example.plateful.presentation.welcome.WelcomeScreen
import com.example.plateful.presentation.orders.NavOrdersScreen
import com.example.plateful.presentation.orders.OrdersScreen
import com.example.plateful.presentation.postScreen.NavPostScreen
import com.example.plateful.presentation.postScreen.PostScreen
import com.example.plateful.presentation.restaurantonboarding.ListingRestaurantScreen
import com.example.plateful.presentation.restaurantonboarding.ListingViewModel
import com.example.plateful.presentation.restaurantonboarding.ListingViewModelFactory
import com.example.plateful.presentation.restaurantonboarding.NavListingRestaurant
import com.example.plateful.ui.theme.AppTheme
//import com.example.plateful.presentation.login.Screens.ShoppingCartScreen
import com.example.plateful.presentation.restaurantDashboard.NavRestaurantDashboardScreen
import com.example.plateful.presentation.restaurantDashboard.RestaurantDashboardScreen
import com.example.plateful.presentation.restaurantDashboard.RestaurantViewModel
import com.example.plateful.presentation.searchFilter.FilterSortBottomSheetUI
import com.example.plateful.presentation.searchFilter.FilterSearchViewModel
import com.example.plateful.presentation.searchFilter.NavSearchScreen
import com.example.plateful.presentation.searchFilter.SearchScreen
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import android.util.Log


class MainActivity : ComponentActivity() {


    companion object {
        var userEntity: UserEntity? = null
        var isUserAnonymous = mutableStateOf(false)
        var isDarkModeEnabled = mutableStateOf(false) //darkthemeoffhere
        const val NavAddressScreen = "address"
        const val NavAddAddressScreen = "add_address"

    }


    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val auth = FirebaseAuth.getInstance()
        // Test Firebase connection
        Firebase.firestore.clearPersistence().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Firebase", "✅ Connected successfully to your Firebase project: plateful-abin")
            } else {
                Log.e("Firebase", "❌ Connection failed: ${task.exception?.message}")
            }
        }

        val accountService: AccountService = AccountServiceImpl()
        val alreadyLoggedIn = FirebaseAuth.getInstance().currentUser != null
        val restaurantListingViewModel: ListingViewModel = ListingViewModelFactory().create(ListingViewModel::class.java)

        enableEdgeToEdge()
        setContent {
            AppTheme (useDarkTheme = isDarkModeEnabled.value){
                val navController = rememberNavController()
                val viewModel: SignInDataViewModel = viewModel()
                val mainSignInViewModel: MainSignInViewModel =
                    viewModel(factory = MainSignInViewModelFactory(auth, accountService))
                if (alreadyLoggedIn)
                    viewModel.getUserData(FirebaseAuth.getInstance().currentUser!!.uid)
                NavHost(
                    navController = navController,
                    startDestination =
                    if (alreadyLoggedIn)
                        NavMainScreen  else NavWelcomeScreen
                ) {
                    composable<NavWelcomeScreen>(
                        enterTransition = {
                            slideHorizontallyAnimation()
                        },
                    ) {
                        WelcomeScreen(navController = navController)
                    }
                    composable<NavSignInUI>(
                        enterTransition = {
                            slideHorizontallyAnimation()
                        },
                    ) {
                        SignInUI(navController = navController, mainSignInViewModel)
                    }
                    composable<NavSearchScreen>(
                        enterTransition = {
                            slideHorizontallyAnimation()
                        },
                    ){
                        val filterViewModel : FilterSearchViewModel = viewModel()
                        SearchScreen(filterViewModel, navController)
                    }
                    composable<NavOTPVerificationUI>(
                        enterTransition = {
                            slideHorizontallyAnimation()
                        },
                    ) {
                        OTPVerificationUI(
                            navController = navController,
                            mainSignInViewModel = mainSignInViewModel
                        )
                    }
                    composable<NavRestaurantDashboardScreen> {
                        val restaurantViewModel: RestaurantViewModel = viewModel()
                        RestaurantDashboardScreen(
                            navController = navController,
                            restaurantViewModel = restaurantViewModel
                        )
                    }
                    composable<NavEmailSignIn>(
                        enterTransition = {
                            slideHorizontallyAnimation()
                        },
                    ) {
                        val emailViewModel: EmailSignInViewModel =
                            viewModel(factory = EmailSignInViewModelFactory(auth, accountService))
                        SignInUsingEmail(emailViewModel, navController = navController)
                    }
                    composable<NavForgotPassword>(
                        enterTransition = {
                            slideHorizontallyAnimation()
                        },
                    ) {
                        ForgotPassword(navController = navController)
                    }
                    composable<NavMainScreen>(
                        enterTransition = {
                            slideHorizontallyAnimation()
                        },
                    ) {
                        MainScreen(navController = navController)
                    }
                    composable<NavPersonalDetails>(
                        enterTransition = {
                            slideHorizontallyAnimation()
                        },
                    ) {
                        PersonalDetails(navigationAfterCompletion = {
                            navController.popBackStack(navController.graph.startDestinationId, true)
                            navController.navigate(NavRoleSelectScreen)
                        })
                    }
                    composable<NavWaitScreen> {
                        WaitScreen(navController)
                    }
                    composable<NavItemDetailScreen> { backStackEntry ->
                        val args = backStackEntry.toRoute<NavItemDetailScreen>()
                        ItemDetailScreen(
                            navController = navController,
                            itemName = args.itemName,
                            restaurantName = args.restaurantName,
                            originalPrice = args.originalPrice,
                            discountedPrice = args.discountedPrice,
                            rating = args.rating,
                            distance = args.distance,
                            location = args.location,
                            pickupTime = args.pickupTime,
                            isVegan = args.isVegan
                        )
                    }
                    composable<NavOrderConfirmScreen> { backStackEntry ->
                        val args = backStackEntry.toRoute<NavOrderConfirmScreen>()
                        OrderConfirmScreen(
                            navController = navController,
                            itemName = args.itemName,
                            restaurantName = args.restaurantName,
                            originalPrice = args.originalPrice,
                            discountedPrice = args.discountedPrice,
                            rating = args.rating,
                            distance = args.distance,
                            location = args.location,
                            pickupTime = args.pickupTime,
                            isVegan = args.isVegan,
                            quantity = args.quantity
                        )
                    }
                    composable<NavAddingLeftovers> {
                        AddingLeftovers(navController)
                    }
                    composable<NavOrderingPickup> {
                        OrderingPickup(navController)
                    }
                    composable<SuccessScreen> {
                        SuccessScreen(
                            buttonText = "Continue",
                            onClick = {
                                navController.navigate(NavMainScreen) {
                                    popUpTo(NavMainScreen) {
                                        inclusive = false
                                    }
                                }
                            },
                            animationJsonResId = R.raw.successanimation
                        )
                    }
                    composable<NavCommunityScreen> {
                        CommunityScreen(navigateBackToHome = {
                            navController.navigate(NavMainScreen) {
                                popUpTo(NavMainScreen) {
                                    inclusive = false
                                }
                            }
                        }) {
                            navController.navigate(NavPostScreen)
                        }
                    }
                    composable<NavPostScreen> {
                        PostScreen {
                            navController.popBackStack()
                        }


                        composable<NavOnboarding> {
                            Onboarding(navController = navController)
                        }
                    }
                    composable<NavOnboarding>{
                        Onboarding(navController = navController)
                    }
                    composable<NavRoleSelectScreen>{
                        PlatefulUserSelection(navController = navController)
                    }
            /*        composable<NavListingRestaurant> {
                        ListingRestaurantScreen(restaurantListingViewModel, navController)
                    }*/

                    composable<NavListingRestaurant> { backStackEntry ->
                        val args = backStackEntry.toRoute<NavListingRestaurant>()
                        ListingRestaurantScreen(
                            restaurantListingViewModel,
                            navController,
                            args.entryPoint // pass it down
                        )
                    }



                    //added the shopping cart screen
                    composable<ShoppingCartScreen>(
                        enterTransition = { slideHorizontallyAnimation() }
                    ) {
                        ShoppingCartScreen(
                            onBackClick = { navController.popBackStack() },
                            padding = PaddingValues()
                        )
                    }
                    composable("address_screen") {
                        AddressScreen(
                            userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                            onAddClick = { navController.navigate("add_address_screen")
                            }
                        )
                    }


                    composable("add_address_screen") {
                        AddAddressScreen(
                            userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                            onSaveSuccess = {
                                navController.popBackStack() // go back to AddressScreen
                            }
                        )
                    }
                    //forfedback
                    composable("feedback/{userId}") { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: ""
                        FeedbackScreen(userId = userId)
                    }
                    
                    composable<NavOrdersScreen> {
                        OrdersScreen(navController = navController)
                    }

                }
            }
        }
    }


    private fun slideHorizontallyAnimation(): EnterTransition {
        return slideInHorizontally(animationSpec = tween(300),
            initialOffsetX = { fullWidth -> fullWidth }
        )
    }
}

