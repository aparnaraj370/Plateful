package com.example.plateful.database

import com.example.plateful.domain.repository.UserDataRepository
import com.example.plateful.domain.repository.RestaurantDataRepository
import com.example.plateful.domain.services.UserRoleService
import com.example.plateful.domain.services.UserType
import com.example.plateful.domain.services.UserEntryType
import com.example.plateful.model.UserEntity
import com.example.plateful.model.RestaurantEntity
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for database operations
 * Note: These tests require Firebase emulator or test environment
 */
class DatabaseIntegrationTest {
    
    private lateinit var userRepository: UserDataRepository
    private lateinit var restaurantRepository: RestaurantDataRepository
    private lateinit var userRoleService: UserRoleService
    
    @Before
    fun setup() {
        userRepository = UserDataRepository()
        restaurantRepository = RestaurantDataRepository()
        userRoleService = UserRoleService()
    }
    
    @Test
    fun `test customer user creation and retrieval`() = runBlocking {
        // Given
        val testUid = "test_customer_uid_${System.currentTimeMillis()}"
        val customerUser = UserEntity(
            uid = testUid,
            email = "customer@test.com",
            name = "Test Customer",
            phoneNumber = "+1234567890",
            userType = "CUSTOMER",
            entryType = "CUSTOMER_ENTRY",
            isProfileComplete = true
        )
        
        // When
        val saveResult = userRepository.setUserData(customerUser)
        
        // Then
        assertTrue("User should be saved successfully", saveResult)
        
        // Verify retrieval
        val retrievedUser = userRepository.getUserData(testUid)
        assertNotNull("User should be retrievable", retrievedUser)
        assertEquals("Email should match", customerUser.email, retrievedUser?.email)
        assertEquals("Name should match", customerUser.name, retrievedUser?.name)
        assertEquals("User type should match", customerUser.userType, retrievedUser?.userType)
        assertEquals("Entry type should match", customerUser.entryType, retrievedUser?.entryType)
    }
    
    @Test
    fun `test restaurant owner creation and role update`() = runBlocking {
        // Given
        val testUid = "test_owner_uid_${System.currentTimeMillis()}"
        val initialUser = UserEntity(
            uid = testUid,
            email = "owner@test.com",
            name = "Test Owner",
            phoneNumber = "+1234567891",
            userType = "CUSTOMER", // Initially customer
            entryType = "RESTAURANT_ENTRY",
            isProfileComplete = false
        )
        
        val restaurant = RestaurantEntity(
            restaurantName = "Test Restaurant",
            restaurantDescription = "Test Description",
            restaurantLocation = "Test Location",
            ownerId = testUid,
            // Add other required fields
        )
        
        // When
        val userSaveResult = userRepository.setUserData(initialUser)
        assertTrue("Initial user should be saved", userSaveResult)
        
        val restaurantId = restaurantRepository.setRestaurantData(restaurant)
        assertNotNull("Restaurant should be saved", restaurantId)
        
        val roleUpdateResult = userRepository.setUserAsRestaurantOwner(testUid, restaurantId!!)
        assertTrue("User role should be updated", roleUpdateResult)
        
        // Then
        val updatedUser = userRepository.getUserData(testUid)
        assertNotNull("Updated user should be retrievable", updatedUser)
        assertEquals("User type should be RESTAURANT_OWNER", "RESTAURANT_OWNER", updatedUser?.userType)
        assertEquals("Restaurant ID should match", restaurantId, updatedUser?.restaurantId)
        assertTrue("Profile should be complete", updatedUser?.isProfileComplete ?: false)
    }
    
    @Test
    fun `test user role service detection`() = runBlocking {
        // Given
        val testUid = "test_role_uid_${System.currentTimeMillis()}"
        val restaurantId = "test_restaurant_id"
        
        // Create restaurant owner user
        val ownerUser = UserEntity(
            uid = testUid,
            email = "roletest@test.com",
            name = "Role Test User",
            userType = "RESTAURANT_OWNER",
            entryType = "RESTAURANT_ENTRY",
            restaurantId = restaurantId,
            isProfileComplete = true
        )
        
        val restaurant = RestaurantEntity(
            restaurantId = restaurantId,
            restaurantName = "Role Test Restaurant",
            restaurantDescription = "Test Description",
            restaurantLocation = "Test Location",
            ownerId = testUid
        )
        
        // When
        userRepository.setUserData(ownerUser)
        restaurantRepository.setRestaurantData(restaurant)
        
        val userRole = userRoleService.getUserRole(testUid, UserEntryType.RESTAURANT_ENTRY)
        
        // Then
        assertNotNull("User role should be detected", userRole)
        assertEquals("User type should be RESTAURANT_OWNER", UserType.RESTAURANT_OWNER, userRole?.userType)
        assertEquals("Restaurant ID should match", restaurantId, userRole?.restaurantId)
        assertEquals("Entry type should match", UserEntryType.RESTAURANT_ENTRY, userRole?.entryType)
        assertTrue("Profile should be complete", userRole?.isProfileComplete ?: false)
    }
    
    @Test
    fun `test customer role detection`() = runBlocking {
        // Given
        val testUid = "test_customer_role_uid_${System.currentTimeMillis()}"
        
        val customerUser = UserEntity(
            uid = testUid,
            email = "customer_role@test.com",
            name = "Customer Role Test",
            userType = "CUSTOMER",
            entryType = "CUSTOMER_ENTRY",
            isProfileComplete = true
        )
        
        // When
        userRepository.setUserData(customerUser)
        val userRole = userRoleService.getUserRole(testUid, UserEntryType.CUSTOMER_ENTRY)
        
        // Then
        assertNotNull("User role should be detected", userRole)
        assertEquals("User type should be CUSTOMER", UserType.CUSTOMER, userRole?.userType)
        assertNull("Restaurant ID should be null", userRole?.restaurantId)
        assertEquals("Entry type should match", UserEntryType.CUSTOMER_ENTRY, userRole?.entryType)
        assertTrue("Profile should be complete", userRole?.isProfileComplete ?: false)
    }
    
    @Test
    fun `test hasRestaurantProfile detection`() = runBlocking {
        // Given
        val testUid = "test_has_restaurant_uid_${System.currentTimeMillis()}"
        val restaurant = RestaurantEntity(
            restaurantName = "Has Restaurant Test",
            restaurantDescription = "Test Description",
            restaurantLocation = "Test Location",
            ownerId = testUid
        )
        
        // When
        val restaurantId = restaurantRepository.setRestaurantData(restaurant)
        assertNotNull("Restaurant should be saved", restaurantId)
        
        val hasRestaurant = userRoleService.hasRestaurantProfile(testUid)
        
        // Then
        assertTrue("Should detect restaurant profile", hasRestaurant)
        
        // Test negative case
        val nonExistentUid = "non_existent_uid_${System.currentTimeMillis()}"
        val hasNoRestaurant = userRoleService.hasRestaurantProfile(nonExistentUid)
        assertFalse("Should not detect restaurant profile for non-existent user", hasNoRestaurant)
    }
    
    @Test
    fun `test getRestaurantIdForOwner`() = runBlocking {
        // Given
        val testUid = "test_get_restaurant_id_uid_${System.currentTimeMillis()}"
        val restaurant = RestaurantEntity(
            restaurantName = "Get Restaurant ID Test",
            restaurantDescription = "Test Description",
            restaurantLocation = "Test Location",
            ownerId = testUid
        )
        
        // When
        val savedRestaurantId = restaurantRepository.setRestaurantData(restaurant)
        assertNotNull("Restaurant should be saved", savedRestaurantId)
        
        val retrievedRestaurantId = userRoleService.getRestaurantIdForOwner(testUid)
        
        // Then
        assertNotNull("Should retrieve restaurant ID", retrievedRestaurantId)
        assertEquals("Restaurant IDs should match", savedRestaurantId, retrievedRestaurantId)
        
        // Test negative case
        val nonExistentUid = "non_existent_owner_uid_${System.currentTimeMillis()}"
        val nonExistentRestaurantId = userRoleService.getRestaurantIdForOwner(nonExistentUid)
        assertNull("Should return null for non-existent owner", nonExistentRestaurantId)
    }
    
    @Test
    fun `test user data field updates`() = runBlocking {
        // Given
        val testUid = "test_update_uid_${System.currentTimeMillis()}"
        val initialUser = UserEntity(
            uid = testUid,
            email = "update@test.com",
            name = "Initial Name",
            isProfileComplete = false
        )
        
        // When
        userRepository.setUserData(initialUser)
        
        val updatedUser = initialUser.copy(
            name = "Updated Name",
            isProfileComplete = true,
            phoneNumber = "+9876543210"
        )
        
        val updateResult = userRepository.setUserData(updatedUser)
        
        // Then
        assertTrue("Update should succeed", updateResult)
        
        val retrievedUser = userRepository.getUserData(testUid)
        assertNotNull("Updated user should be retrievable", retrievedUser)
        assertEquals("Name should be updated", "Updated Name", retrievedUser?.name)
        assertTrue("Profile should be complete", retrievedUser?.isProfileComplete ?: false)
        assertEquals("Phone number should be updated", "+9876543210", retrievedUser?.phoneNumber)
    }
}

/**
 * Mock data helpers for testing
 */
object TestDataHelper {
    
    fun createTestCustomerUser(uid: String): UserEntity {
        return UserEntity(
            uid = uid,
            email = "customer_$uid@test.com",
            name = "Test Customer $uid",
            phoneNumber = "+123456789${uid.take(3)}",
            userType = "CUSTOMER",
            entryType = "CUSTOMER_ENTRY",
            isProfileComplete = true
        )
    }
    
    fun createTestRestaurantUser(uid: String, restaurantId: String = ""): UserEntity {
        return UserEntity(
            uid = uid,
            email = "owner_$uid@test.com",
            name = "Test Owner $uid",
            phoneNumber = "+987654321${uid.take(3)}",
            userType = "RESTAURANT_OWNER",
            entryType = "RESTAURANT_ENTRY",
            restaurantId = restaurantId,
            isProfileComplete = true
        )
    }
    
    fun createTestRestaurant(ownerId: String): RestaurantEntity {
        return RestaurantEntity(
            restaurantName = "Test Restaurant $ownerId",
            restaurantDescription = "Test Description for $ownerId",
            restaurantLocation = "Test Location $ownerId",
            ownerId = ownerId,
            // Add other required fields with default values
        )
    }
}
