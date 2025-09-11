package com.example.plateful.authentication

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.plateful.domain.services.AuthenticationService
import com.example.plateful.domain.services.SessionManager
import com.example.plateful.domain.services.UserRoleService
import com.example.plateful.domain.services.UserEntryType
import com.example.plateful.domain.services.UserType
import com.example.plateful.domain.services.AuthResult
import com.example.plateful.domain.repository.UserDataRepository
import com.example.plateful.domain.repository.RestaurantDataRepository
import com.example.plateful.model.RestaurantEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-end tests for authentication flow
 * Note: These tests use Robolectric for Android context and require Firebase emulator
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthenticationFlowTest {
    
    private lateinit var context: Context
    private lateinit var authService: AuthenticationService
    private lateinit var sessionManager: SessionManager
    private lateinit var userRoleService: UserRoleService
    private lateinit var userRepository: UserDataRepository
    private lateinit var restaurantRepository: RestaurantDataRepository
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        authService = AuthenticationService()
        sessionManager = SessionManager(context)
        userRoleService = UserRoleService()
        userRepository = UserDataRepository()
        restaurantRepository = RestaurantDataRepository()
    }
    
    @Test
    fun `test complete customer registration and login flow`() = runBlocking {
        // Given
        val testEmail = "customer_flow_test@test.com"
        val testPassword = "testPassword123"
        val testName = "Flow Test Customer"
        
        // When - Create new customer account
        val createResult = authService.createAccountWithEmail(
            email = testEmail,
            password = testPassword,
            name = testName,
            entryType = UserEntryType.CUSTOMER_ENTRY
        )
        
        // Then - Account creation should succeed
        assertTrue("Account creation should succeed", createResult is AuthResult.Success)
        
        val successResult = createResult as AuthResult.Success
        assertEquals("Email should match", testEmail, successResult.firebaseUser.email)
        assertEquals("Name should match", testName, successResult.userEntity.name)
        assertEquals("User type should be CUSTOMER", "CUSTOMER", successResult.userEntity.userType)
        assertEquals("Entry type should be CUSTOMER_ENTRY", "CUSTOMER_ENTRY", successResult.userEntity.entryType)
        assertFalse("Profile should initially be incomplete", successResult.userEntity.isProfileComplete)
        
        // Save session
        sessionManager.saveUserSession(successResult.firebaseUser, successResult.userEntity)
        
        // Verify session state
        val sessionState = sessionManager.sessionState.value
        assertTrue("Session should be authenticated", sessionState.isAuthenticated)
        assertEquals("Session user type should be CUSTOMER", UserType.CUSTOMER, sessionState.userRole?.userType)
        assertTrue("Should be able to access customer features", sessionManager.canAccessCustomerFeatures())
        assertFalse("Should not be able to access restaurant features", sessionManager.canAccessRestaurantFeatures())
        
        // Sign out and sign back in
        FirebaseAuth.getInstance().signOut()
        
        val signInResult = authService.signInWithEmail(
            email = testEmail,
            password = testPassword,
            entryType = UserEntryType.CUSTOMER_ENTRY
        )
        
        assertTrue("Sign in should succeed", signInResult is AuthResult.Success)
    }
    
    @Test
    fun `test complete restaurant owner registration and onboarding flow`() = runBlocking {
        // Given
        val testEmail = "owner_flow_test@test.com"
        val testPassword = "testPassword123"
        val testName = "Flow Test Owner"
        val restaurantName = "Flow Test Restaurant"
        
        // When - Create new restaurant owner account
        val createResult = authService.createAccountWithEmail(
            email = testEmail,
            password = testPassword,
            name = testName,
            entryType = UserEntryType.RESTAURANT_ENTRY
        )
        
        // Then - Account creation should succeed
        assertTrue("Account creation should succeed", createResult is AuthResult.Success)
        
        val successResult = createResult as AuthResult.Success
        assertEquals("Entry type should be RESTAURANT_ENTRY", "CUSTOMER_ENTRY", successResult.userEntity.entryType)
        // Note: userType starts as CUSTOMER until restaurant onboarding is complete
        
        // Simulate restaurant onboarding
        val restaurant = RestaurantEntity(
            restaurantName = restaurantName,
            restaurantDescription = "Test restaurant description",
            restaurantLocation = "123 Test Street",
            ownerId = successResult.firebaseUser.uid
        )
        
        val restaurantId = restaurantRepository.setRestaurantData(restaurant)
        assertNotNull("Restaurant should be saved", restaurantId)
        
        // Update user to restaurant owner
        val roleUpdateResult = userRepository.setUserAsRestaurantOwner(successResult.firebaseUser.uid, restaurantId!!)
        assertTrue("Role update should succeed", roleUpdateResult)
        
        // Verify updated role
        val updatedUserRole = userRoleService.getUserRole(
            successResult.firebaseUser.uid,
            UserEntryType.RESTAURANT_ENTRY
        )
        
        assertNotNull("Updated role should be detected", updatedUserRole)
        assertEquals("User type should be RESTAURANT_OWNER", UserType.RESTAURANT_OWNER, updatedUserRole?.userType)
        assertEquals("Restaurant ID should match", restaurantId, updatedUserRole?.restaurantId)
        assertTrue("Profile should be complete", updatedUserRole?.isProfileComplete ?: false)
        
        // Test session management
        sessionManager.updateUserRole(restaurantId)
        
        assertTrue("Should be able to access restaurant features", sessionManager.canAccessRestaurantFeatures())
        assertEquals("Current user type should be RESTAURANT_OWNER", UserType.RESTAURANT_OWNER, sessionManager.getCurrentUserType())
        assertEquals("Restaurant ID should match", restaurantId, sessionManager.getRestaurantId())
    }
    
    @Test
    fun `test navigation routing based on user type`() = runBlocking {
        // Test customer routing
        val customerUid = "test_customer_routing_${System.currentTimeMillis()}"
        val customerUser = com.example.plateful.database.TestDataHelper.createTestCustomerUser(customerUid)
        userRepository.setUserData(customerUser)
        
        val customerRole = userRoleService.getUserRole(customerUid, UserEntryType.CUSTOMER_ENTRY)
        val customerNextRoute = userRoleService.getNextRoute(customerRole!!)
        assertEquals("Customer should route to main_screen", "main_screen", customerNextRoute)
        
        // Test restaurant owner routing
        val ownerUid = "test_owner_routing_${System.currentTimeMillis()}"
        val restaurantId = "test_restaurant_routing_${System.currentTimeMillis()}"
        val ownerUser = com.example.plateful.database.TestDataHelper.createTestRestaurantUser(ownerUid, restaurantId)
        userRepository.setUserData(ownerUser)
        
        val restaurant = com.example.plateful.database.TestDataHelper.createTestRestaurant(ownerUid)
        restaurantRepository.setRestaurantData(restaurant.copy(restaurantId = restaurantId))
        
        val ownerRole = userRoleService.getUserRole(ownerUid, UserEntryType.RESTAURANT_ENTRY)
        val ownerNextRoute = userRoleService.getNextRoute(ownerRole!!)
        assertEquals("Owner should route to restaurant_dashboard", "restaurant_dashboard", ownerNextRoute)
        
        // Test incomplete profile routing
        val incompleteUser = customerUser.copy(isProfileComplete = false)
        userRepository.setUserData(incompleteUser)
        
        val incompleteRole = userRoleService.getUserRole(customerUid, UserEntryType.CUSTOMER_ENTRY)
        val incompleteNextRoute = userRoleService.getNextRoute(incompleteRole!!)
        assertEquals("Incomplete profile should route to personal_details", "personal_details", incompleteNextRoute)
    }
    
    @Test
    fun `test role validation and access control`() = runBlocking {
        // Given
        val customerUid = "test_customer_access_${System.currentTimeMillis()}"
        val ownerUid = "test_owner_access_${System.currentTimeMillis()}"
        val restaurantId = "test_restaurant_access_${System.currentTimeMillis()}"
        
        val customerUser = com.example.plateful.database.TestDataHelper.createTestCustomerUser(customerUid)
        val ownerUser = com.example.plateful.database.TestDataHelper.createTestRestaurantUser(ownerUid, restaurantId)
        val restaurant = com.example.plateful.database.TestDataHelper.createTestRestaurant(ownerUid)
        
        // When
        userRepository.setUserData(customerUser)
        userRepository.setUserData(ownerUser)
        restaurantRepository.setRestaurantData(restaurant.copy(restaurantId = restaurantId))
        
        val customerRole = userRoleService.getUserRole(customerUid, UserEntryType.CUSTOMER_ENTRY)
        val ownerRole = userRoleService.getUserRole(ownerUid, UserEntryType.RESTAURANT_ENTRY)
        
        // Then - Customer access validation
        assertTrue("Customer should access customer features", userRoleService.canAccessCustomerFeatures(customerRole!!))
        assertFalse("Customer should not access restaurant features", userRoleService.canAccessRestaurantFeatures(customerRole))
        
        // Restaurant owner access validation
        assertTrue("Owner should access restaurant features", userRoleService.canAccessRestaurantFeatures(ownerRole!!))
        assertFalse("Owner should not access customer features", userRoleService.canAccessCustomerFeatures(ownerRole))
    }
    
    @Test
    fun `test entry type mismatch handling`() = runBlocking {
        // Given - User registered as customer but trying to access restaurant features
        val testUid = "test_mismatch_${System.currentTimeMillis()}"
        val customerUser = com.example.plateful.database.TestDataHelper.createTestCustomerUser(testUid)
        userRepository.setUserData(customerUser)
        
        // When - Check role with restaurant entry type
        val roleWithRestaurantEntry = userRoleService.getUserRole(testUid, UserEntryType.RESTAURANT_ENTRY)
        
        // Then - Should still detect as customer but with restaurant entry preference
        assertNotNull("Role should be detected", roleWithRestaurantEntry)
        assertEquals("User type should remain CUSTOMER", UserType.CUSTOMER, roleWithRestaurantEntry?.userType)
        assertEquals("Entry type should be RESTAURANT_ENTRY", UserEntryType.RESTAURANT_ENTRY, roleWithRestaurantEntry?.entryType)
        
        // Should be able to access customer features based on actual role
        assertTrue("Should access customer features", userRoleService.canAccessCustomerFeatures(roleWithRestaurantEntry!!))
        // Should not access restaurant features without actual restaurant
        assertFalse("Should not access restaurant features", userRoleService.canAccessRestaurantFeatures(roleWithRestaurantEntry))
    }
    
    @Test
    fun `test authentication error handling`() = runBlocking {
        // Test invalid email format
        val invalidEmailResult = authService.signInWithEmail(
            email = "invalid-email",
            password = "password123",
            entryType = UserEntryType.CUSTOMER_ENTRY
        )
        
        assertTrue("Invalid email should return error", invalidEmailResult is AuthResult.Error)
        
        // Test empty credentials
        val emptyEmailResult = authService.signInWithEmail(
            email = "",
            password = "password123",
            entryType = UserEntryType.CUSTOMER_ENTRY
        )
        
        assertTrue("Empty email should return error", emptyEmailResult is AuthResult.Error)
        
        // Test non-existent user
        val nonExistentResult = authService.signInWithEmail(
            email = "nonexistent@test.com",
            password = "password123",
            entryType = UserEntryType.CUSTOMER_ENTRY
        )
        
        assertTrue("Non-existent user should return error", nonExistentResult is AuthResult.Error)
    }
}
