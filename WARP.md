# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

**Plateful** is an Android food waste reduction app built with Kotlin and Jetpack Compose. The app connects restaurants with surplus food to individuals and charitable organizations, helping reduce food waste while providing affordable meals. The project follows MVVM architecture with Firebase backend integration.

## Essential Development Commands

### Building and Running
```bash
# Build the project (debug)
./gradlew assembleDebug

# Build release version
./gradlew assembleRelease

# Clean and rebuild
./gradlew clean assembleDebug

# Install debug APK to connected device
./gradlew installDebug

# Run all checks (lint, compile, etc.)
./gradlew check
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run tests with coverage
./gradlew testDebugUnitTestCoverage

# Run specific test class
./gradlew test --tests "com.example.plateful.ExampleUnitTest"
```

### Code Quality and Analysis
```bash
# Run lint analysis
./gradlew lint

# Generate lint report
./gradlew lintDebug

# Check for dependency updates
./gradlew dependencyUpdates
```

## Architecture Overview

### High-Level Structure
The app follows **Clean Architecture** with MVVM pattern:

- **Domain Layer** (`domain/`): Contains business logic, repositories, and services
  - `services/`: Authentication and core business services (AccountService)
  - `repository/`: Data access abstractions for restaurants and user data
- **Presentation Layer** (`presentation/`): UI components and ViewModels
  - Feature-based organization (login, restaurant onboarding, community, etc.)
  - Uses Jetpack Compose for all UI
- **Data/DI Layer** (`di/`): Dependency injection modules (Hilt + Firebase)

### Key Architectural Patterns

**Navigation**: Uses type-safe Navigation Compose with serializable route objects
- Routes defined as `@Serializable` data classes (e.g., `NavMainScreen`, `NavSignInUI`)
- Centralized navigation in `MainActivity.kt` with NavHost

**State Management**:
- ViewModels use `StateFlow`/`LiveData` for reactive state
- UI state classes follow naming pattern: `*UIState` (e.g., `EmailUIState`, `MainSignInUIState`)
- Global state managed through companion objects in `MainActivity`

**Firebase Integration**:
- Authentication: Email, phone, Google Sign-In, anonymous
- Firestore for data persistence
- Firebase Storage for images
- Configured through `FirebaseModule` with Hilt

### Feature Modules Structure

**Authentication Flow**:
- `mainlogin/`: Phone-based authentication with OTP
- `emaillogin/`: Email/password authentication 
- Multiple authentication methods with unified `AccountService`

**User Journeys**:
- **Restaurants**: Onboarding → Dashboard → Add leftover items
- **Users**: Browse → Search/Filter → Order → Pickup
- **Community**: Social features and food sharing posts

**Core Features**:
- `restaurantonboarding/`: Multi-step restaurant registration
- `restaurantDashboard/`: Restaurant management interface
- `itemdetailscreen/`: Food item management and ordering
- `searchFilter/`: Advanced filtering and search functionality
- `communityScreen/`: Social sharing and community features

## Development Guidelines

### Working with Authentication
- All auth operations go through `AccountService` interface
- Firebase Auth user state is managed globally in `MainActivity.userEntity`
- Support for multiple auth methods: phone, email, Google, anonymous

### Firebase Configuration
- Project uses Firebase project: `plateful-abin`
- Required files: `google-services.json` (already included)
- Debug keystore included for development

### Jetpack Compose Patterns
- Theme configuration in `ui/theme/` with dark mode support
- Lottie animations for loading and success states (`res/raw/`)
- Material 3 design system throughout
- Custom composables follow feature-based organization

### State and Navigation
- Use type-safe navigation routes
- ViewModels should extend appropriate base classes
- State updates should be reactive (StateFlow/Compose State)

### Testing Strategy
- Unit tests: `src/test/` directory
- Instrumented tests: `src/androidTest/` directory
- Test files follow naming: `*Test.kt`

### Dependencies Management
- Version catalog in `gradle/libs.versions.toml`
- Major dependencies: Compose BOM, Firebase BOM, Hilt, Coil, Lottie
- Use alias references for consistent versioning

## Firebase Project Details
- **Project ID**: `plateful-abin` 
- **Authentication Methods**: Phone, Email, Google, Anonymous
- **Firestore**: User profiles, restaurant data, menu items
- **Storage**: Restaurant images, food item photos
- **Required permissions**: Internet, camera, location

## Key Files to Understand
- `MainActivity.kt`: Navigation hub and global state management
- `FirebaseModule.kt`: DI configuration for Firebase services
- `AccountServiceImpl.kt`: Authentication business logic
- `gradle/libs.versions.toml`: Dependency version management
- `app/build.gradle.kts`: Module configuration and dependencies

## Common Development Patterns
- Feature folders contain related screens, ViewModels, and data classes
- Navigation objects are serializable data classes with `Nav` prefix
- ViewModels use factory pattern when requiring dependencies
- Resource files organized by type: animations in `raw/`, images in `drawable/`
