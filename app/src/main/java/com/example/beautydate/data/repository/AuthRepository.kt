package com.example.beautydate.data.repository

import com.example.beautydate.data.local.UserPreferences
import com.example.beautydate.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication operations
 * Handles Firebase Auth and Firestore operations for user management
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences
) {
    
    /**
     * Gets current Firebase user
     * @return Current Firebase user or null
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    /**
     * Gets current user as Flow
     * @return Flow of current user
     */
    fun getCurrentUserFlow(): Flow<FirebaseUser?> {
        return kotlinx.coroutines.flow.flow {
            emit(auth.currentUser)
        }
    }
    
    /**
     * Signs in user with email and password
     * @param email User email
     * @param password User password
     * @return Result with success status and error message
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                // Save user ID for auto-login
                userPreferences.saveLastUserId(user.uid)
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Signs in user with username and password
     * First finds the user by username, then authenticates with email
     * @param username Username
     * @param password User password
     * @return Result with success status and error message
     */
    suspend fun signInWithUsernameAndPassword(username: String, password: String): Result<FirebaseUser> {
        return try {
            // Find user by username first with improved error handling
            val userEmail = getUserEmailByUsername(username)
            if (userEmail == null) {
                return Result.failure(Exception("Kullanıcı adı bulunamadı"))
            }
            
            // Sign in with email and password
            val result = auth.signInWithEmailAndPassword(userEmail, password).await()
            result.user?.let { user ->
                // Save user ID for auto-login
                userPreferences.saveLastUserId(user.uid)
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Finds user email by username
     * Enhanced method to avoid cache issues and improve reliability
     * @param username Username to search for
     * @return User email or null if not found
     */
    private suspend fun getUserEmailByUsername(username: String): String? {
        return try {
            println("Debug: Searching for username: '$username'")
            
            // First try the secure username mapping collection
            val email = getUserEmailByUsernameSecure(username)
            if (email != null) {
                println("Debug: Found email via secure method: $email")
                return email
            }
            
            // Fallback to users collection if secure method fails
            println("Debug: Secure method failed, trying users collection...")
            
            // Try both exact case and lowercase
            val searchUsernames = listOf(username, username.lowercase())
            
            for (searchUsername in searchUsernames) {
                println("Debug: Trying username variant: '$searchUsername'")
                
                // Try default source first (cache then server)
                println("Debug: Trying DEFAULT source...")
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("username", searchUsername)
                    .limit(1)
                    .get()
                    .await()
                
                println("Debug: DEFAULT query returned ${querySnapshot.documents.size} documents")
                
                val foundEmail = querySnapshot.documents.firstOrNull()?.getString("email")
                
                if (foundEmail != null) {
                    println("Debug: Found email from DEFAULT: $foundEmail")
                    // Create mapping for future secure lookups
                    createUsernameMapping(username, foundEmail)
                    return foundEmail
                }
            }
            
            println("Debug: Username '$username' not found in any collection")
            null
            
        } catch (e: Exception) {
            // Log the exception for debugging
            println("Debug: Exception in getUserEmailByUsername: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Alternative secure method: Finds user email by username using separate collection
     * This method uses a separate 'usernames' collection for security
     * @param username Username to search for
     * @return User email or null if not found
     */
    private suspend fun getUserEmailByUsernameSecure(username: String): String? {
        return try {
            println("Debug: Searching for username (secure): '$username'")
            
            // Try both exact case and lowercase
            val searchUsernames = listOf(username, username.lowercase())
            
            for (searchUsername in searchUsernames) {
                println("Debug: Trying username variant (secure): '$searchUsername'")
                
                // Query the separate usernames collection
                val document = firestore.collection("usernames")
                    .document(searchUsername)
                    .get()
                    .await()
                
                if (document.exists()) {
                    val email = document.getString("email")
                    if (email != null) {
                        println("Debug: Found email from usernames collection: $email")
                        return email
                    }
                }
            }
            
            println("Debug: Username '$username' not found in usernames collection")
            null
            
        } catch (e: Exception) {
            println("Debug: Exception in getUserEmailByUsernameSecure: ${e.message}")
            null
        }
    }

    /**
     * Creates username mapping document for secure lookup
     * @param username Username
     * @param email Email
     */
    private suspend fun createUsernameMapping(username: String, email: String) {
        try {
            firestore.collection("usernames")
                .document(username.lowercase())
                .set(mapOf("email" to email))
                .await()
            println("Debug: Username mapping created for: $username")
        } catch (e: Exception) {
            println("Debug: Failed to create username mapping: ${e.message}")
        }
    }
    
    /**
     * Creates new user account with business details
     * @param username Username
     * @param email User email
     * @param password User password
     * @param businessName Business name
     * @param phoneNumber Phone number
     * @param address Business address
     * @param taxNumber Tax number (optional)
     * @return Result with success status and error message
     */
    suspend fun createUserWithEmailAndPassword(
        username: String,
        email: String,
        password: String,
        businessName: String,
        phoneNumber: String,
        address: String,
        taxNumber: String = ""
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                // Create user document in Firestore
                val userData = User(
                    id = user.uid,
                    username = username.lowercase(), // Store username in lowercase for consistency
                    email = email,
                    businessName = businessName,
                    phoneNumber = phoneNumber,
                    address = address,
                    taxNumber = taxNumber,
                    createdAt = Timestamp.now(),
                    emailVerified = false
                )
                
                println("Debug: Creating user with username: '$username', email: '$email'")
                
                firestore.collection("users")
                    .document(user.uid)
                    .set(userData)
                    .await()
                
                println("Debug: User document created successfully with ID: ${user.uid}")
                
                // Create username mapping for secure lookup
                createUsernameMapping(username, email)
                
                // Send email verification
                user.sendEmailVerification().await()
                
                // Save user ID for auto-login
                userPreferences.saveLastUserId(user.uid)
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Signs out current user
     */
    suspend fun signOut() {
        auth.signOut()
        userPreferences.clearUserPreferences()
    }
    
    /**
     * Checks if user email is verified
     * @param user Firebase user
     * @return True if email is verified
     */
    suspend fun isEmailVerified(user: FirebaseUser): Boolean {
        // Reload user to get latest email verification status
        user.reload().await()
        
        // Update Firestore document with latest verification status
        if (user.isEmailVerified) {
            firestore.collection("users")
                .document(user.uid)
                .update("emailVerified", true)
                .await()
        }
        
        return user.isEmailVerified
    }
    
    /**
     * Gets user data from Firestore
     * @param userId User ID
     * @return User data or null
     */
    suspend fun getUserData(userId: String): User? {
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Updates user data in Firestore
     * @param user User data to update
     * @return Result with success status
     */
    suspend fun updateUserData(user: User): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(user.id)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Saves remembered username
     * @param username Username to remember
     */
    suspend fun saveRememberedUsername(username: String) {
        userPreferences.saveRememberedUsername(username)
    }
    
    /**
     * Gets remembered username as Flow
     * @return Flow of remembered username
     */
    fun getRememberedUsername(): Flow<String> {
        return userPreferences.rememberedUsername
    }
    
    /**
     * Gets remember username enabled status as Flow
     * @return Flow of remember username enabled status
     */
    fun getRememberUsernameEnabled(): Flow<Boolean> {
        return userPreferences.rememberUsernameEnabled
    }
    
    /**
     * Saves remember username enabled status
     * @param enabled Whether remember username is enabled
     */
    suspend fun saveRememberUsernameEnabled(enabled: Boolean) {
        userPreferences.saveRememberUsernameEnabled(enabled)
    }
    
    /**
     * Saves auto-login enabled status
     * @param enabled Whether auto-login is enabled
     */
    suspend fun saveAutoLoginEnabled(enabled: Boolean) {
        userPreferences.saveAutoLoginEnabled(enabled)
    }
    
    /**
     * Gets auto-login enabled status as Flow
     * @return Flow of auto-login status
     */
    fun getAutoLoginEnabled(): Flow<Boolean> {
        return userPreferences.autoLoginEnabled
    }
    
    /**
     * Gets last user ID as Flow
     * @return Flow of last user ID
     */
    fun getLastUserId(): Flow<String> {
        return userPreferences.lastUserId
    }

    /**
     * Debug function to list all users in Firestore
     * This helps diagnose login issues
     */
    suspend fun debugListAllUsers(): String {
        return try {
            val snapshot = firestore.collection("users")
                .get()
                .await()
            
            val userList = StringBuilder()
            userList.append("=== ALL USERS IN FIRESTORE ===\n")
            userList.append("Total users: ${snapshot.documents.size}\n\n")
            
            snapshot.documents.forEachIndexed { index, doc ->
                val username = doc.getString("username") ?: "null"
                val email = doc.getString("email") ?: "null"
                val emailVerified = doc.getBoolean("emailVerified") ?: false
                userList.append("User ${index + 1}:\n")
                userList.append("  ID: ${doc.id}\n")
                userList.append("  Username: '$username'\n")
                userList.append("  Email: '$email'\n")
                userList.append("  Email Verified: $emailVerified\n")
                userList.append("  Document exists: ${doc.exists()}\n\n")
            }
            
            userList.toString()
        } catch (e: Exception) {
            "Error listing users: ${e.message}"
        }
    }
} 