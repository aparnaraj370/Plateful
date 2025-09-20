// Test script to manually insert food pack data to Firestore
// Run this with Node.js after setting up Firebase Admin SDK

const admin = require('firebase-admin');

// Initialize Firebase Admin SDK (you'll need to replace with your service account key)
const serviceAccount = require('./path/to/your-service-account-key.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://your-project-id.firebaseio.com'
});

const db = admin.firestore();

async function insertTestFoodPack() {
  try {
    // Create a test food pack
    const testFoodPack = {
      id: 'test-food-pack-1',
      vendorId: 'test-vendor-123',
      restaurantId: 'test-restaurant-123',
      restaurantName: 'Test Restaurant',
      name: 'Delicious Test Pack',
      description: 'A test food pack to verify data retrieval',
      originalPrice: 15.99,
      discountedPrice: 9.99,
      quantity: 5,
      totalQuantity: 10,
      imageUrl: 'https://example.com/test-image.jpg',
      // Set expiry time to 24 hours from now
      expiryTime: admin.firestore.Timestamp.fromDate(new Date(Date.now() + 24 * 60 * 60 * 1000)),
      status: 'AVAILABLE',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now(),
      cuisineType: 'VEGETARIAN',
      isVegetarian: true,
      isVegan: false,
      images: ['https://example.com/test-image.jpg'],
      nutritionInfo: 'Test nutrition info',
      allergyInfo: 'Test allergy info',
      packagingType: 'Eco-friendly',
      pickupInstructions: 'Come to the counter',
      rating: 4.5,
      reviewCount: 10,
      location: {
        address: '123 Test Street',
        city: 'Test City',
        state: 'Test State',
        postalCode: '12345',
        latitude: 37.7749,
        longitude: -122.4194
      }
    };

    // Add to Firestore
    await db.collection('foodPacks').doc('test-food-pack-1').set(testFoodPack);
    
    console.log('✅ Test food pack inserted successfully!');
    console.log('Food Pack ID:', testFoodPack.id);
    console.log('Restaurant Name:', testFoodPack.restaurantName);
    console.log('Name:', testFoodPack.name);
    console.log('Price:', testFoodPack.discountedPrice);
    
    // Verify the insertion by reading it back
    const doc = await db.collection('foodPacks').doc('test-food-pack-1').get();
    if (doc.exists) {
      console.log('✅ Verification successful - document exists in Firestore');
      console.log('Document data:', doc.data());
    } else {
      console.log('❌ Verification failed - document not found');
    }
    
  } catch (error) {
    console.error('❌ Error inserting test food pack:', error);
  }
}

// Run the test
insertTestFoodPack().then(() => {
  console.log('Test completed');
  process.exit(0);
}).catch((error) => {
  console.error('Test failed:', error);
  process.exit(1);
});
