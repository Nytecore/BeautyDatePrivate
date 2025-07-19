# BeautyDate Android App

A modern Android dating application built with Kotlin, Jetpack Compose, and Firebase.

## Features

- **Authentication**: Email/password authentication using Firebase Auth
- **Real-time Database**: Firebase Firestore for data storage
- **Modern UI**: Material3 design with Jetpack Compose
- **MVVM Architecture**: Clean architecture with ViewModels and Repositories
- **Multi-language Support**: Turkish and English localization
- **Dependency Injection**: Hilt for dependency management

## Project Structure

```
app/src/main/java/com/example/beautydate/
├── BeautyDateApplication.kt          # Application class with Hilt
├── MainActivity.kt                   # Main activity with navigation
├── components/                       # Reusable UI components
│   └── CommonComponents.kt
├── data/                            # Data layer
│   ├── models/                      # Data models
│   │   ├── User.kt
│   │   ├── Match.kt
│   │   └── Message.kt
│   └── remote/                      # Firebase implementations
│       ├── FirebaseAuthRepository.kt
│       └── FirebaseUserRepository.kt
├── di/                              # Dependency injection
│   └── AppModule.kt
├── repositories/                     # Repository interfaces
│   ├── AuthRepository.kt
│   └── UserRepository.kt
├── screens/                         # UI screens
│   ├── HomeScreen.kt
│   ├── LoginScreen.kt
│   └── RegisterScreen.kt
├── utils/                           # Utility classes
│   └── ValidationUtils.kt
└── viewmodels/                      # ViewModels
    └── AuthViewModel.kt
```

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Backend**: Firebase (Auth, Firestore)
- **Navigation**: Navigation Compose
- **State Management**: StateFlow
- **Coroutines**: For asynchronous operations

## Setup Instructions

### Prerequisites

- Android Studio Hedgehog or later
- Minimum SDK: 29 (Android 10)
- Target SDK: 36 (Android 15)
- Kotlin 2.0.21

### Firebase Setup

1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add your Android app to the project
3. Download `google-services.json` and place it in the `app/` directory
4. Enable Email/Password authentication in Firebase Console
5. Set up Firestore database with the following security rules:

```javascript
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /users/{userId} {
      allow read: if request.auth != null;
    }
  }
}
```

### Build and Run

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run the app

## Architecture

### MVVM Pattern

- **Model**: Data models and repositories
- **View**: Compose UI components
- **ViewModel**: Business logic and state management

### Repository Pattern

- Abstract repository interfaces define data operations
- Firebase implementations provide concrete implementations
- Dependency injection manages repository instances

### SOLID Principles

- **Single Responsibility**: Each class has one responsibility
- **Open/Closed**: Open for extension, closed for modification
- **Liskov Substitution**: Repository implementations are interchangeable
- **Interface Segregation**: Repository interfaces are focused
- **Dependency Inversion**: High-level modules depend on abstractions

## Multi-language Support

The app supports Turkish and English languages:

- `res/values/strings.xml` - English strings
- `res/values-tr/strings.xml` - Turkish strings

## Code Quality

- Maximum 300 lines per file (excluding comments)
- English variable names and comments
- Comprehensive error handling
- Input validation
- Modern Android development practices

## Future Enhancements

- Profile management
- Match functionality
- Real-time messaging
- Push notifications
- Image upload and storage
- Advanced search and filtering
- Settings and preferences

## Contributing

1. Follow the established code style
2. Keep files under 300 lines
3. Write comprehensive tests
4. Use meaningful commit messages
5. Follow SOLID principles

## License

This project is licensed under the MIT License. 