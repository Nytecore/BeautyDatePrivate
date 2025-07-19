package com.example.beautydate.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beautydate.data.repository.AuthRepository
import com.example.beautydate.domain.usecases.LoginResult
import com.example.beautydate.domain.usecases.LoginUseCase
import com.example.beautydate.domain.usecases.RegisterResult
import com.example.beautydate.domain.usecases.RegisterUseCase
import com.example.beautydate.viewmodels.actions.AuthActions
import com.example.beautydate.viewmodels.state.AuthUiState
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication operations
 * Manages login, register, and user session state
 * Implements AuthActions following Single Responsibility Principle
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel(), AuthActions {
    
    // UI State
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    // Current user
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    // Remembered username
    private val _rememberedUsername = MutableStateFlow("")
    val rememberedUsername: StateFlow<String> = _rememberedUsername.asStateFlow()
    
    private val _businessName = MutableStateFlow("")
    val businessName: StateFlow<String> = _businessName.asStateFlow()
    
    init {
        // Load remembered username and checkbox status
        viewModelScope.launch {
            // Load remember username enabled status
            authRepository.getRememberUsernameEnabled()
                .collect { rememberEnabled ->
                    _uiState.update { it.copy(rememberUsername = rememberEnabled) }
                }
        }
        
        viewModelScope.launch {
            authRepository.getRememberedUsername()
                .collect { username ->
                    _rememberedUsername.value = username
                    // Only auto-fill if current state username is empty AND "Beni Hatırla" is enabled
                    if (_uiState.value.username.isEmpty() && _uiState.value.rememberUsername && username.isNotEmpty()) {
                        _uiState.update { it.copy(username = username) }
                    }
                }
        }
        
        // Check current user and load their data
        _currentUser.value = authRepository.getCurrentUser()
        
        // Load user data if user is logged in
        _currentUser.value?.let { user ->
            loadUserData(user.uid)
        }
    }
    
    /**
     * Updates username field
     */
    override fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username) }
    }
    
    /**
     * Updates email field
     */
    override fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }
    
    /**
     * Updates password field
     */
    override fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }
    
    /**
     * Updates confirm password field
     */
    override fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword) }
    }
    
    /**
     * Updates business name field
     */
    override fun updateBusinessName(businessName: String) {
        _uiState.update { it.copy(businessName = businessName) }
    }
    
    /**
     * Updates phone number field
     */
    override fun updatePhoneNumber(phoneNumber: String) {
        _uiState.update { it.copy(phoneNumber = phoneNumber) }
    }
    
    /**
     * Updates address field
     */
    override fun updateAddress(address: String) {
        _uiState.update { it.copy(address = address) }
    }
    
    /**
     * Updates tax number field
     */
    override fun updateTaxNumber(taxNumber: String) {
        _uiState.update { it.copy(taxNumber = taxNumber) }
    }
    
    /**
     * Updates remember username checkbox
     */
    override fun updateRememberUsername(remember: Boolean) {
        _uiState.update { it.copy(rememberUsername = remember) }
        // Save the checkbox status immediately when changed
        viewModelScope.launch {
            authRepository.saveRememberUsernameEnabled(remember)
        }
    }
    

    
    /**
     * Signs in user using LoginUseCase
     */
    override fun signIn() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            when (val result = loginUseCase.execute(_uiState.value)) {
                is LoginResult.Success -> {
                    _currentUser.value = result.user
                    loadUserData(result.user.uid)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true
                        ) 
                    }
                }
                is LoginResult.EmailNotVerified -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Lütfen e-mail adresinizi doğrulayın."
                        ) 
                    }
                }
                is LoginResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        ) 
                    }
                }
                is LoginResult.ValidationError -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        ) 
                    }
                }
            }
        }
    }
    
    /**
     * Registers new user using RegisterUseCase
     */
    override fun register() {
        register("", "")
    }
    
    /**
     * Registers new user with city and district information
     */
    fun register(selectedCity: String, selectedDistrict: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            when (val result = registerUseCase.execute(_uiState.value, selectedCity, selectedDistrict)) {
                is RegisterResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isRegistered = true,
                            shouldNavigateToLogin = true,
                            registrationSuccessMessage = "Kayıt başarılı! Lütfen e-mail adresinize gönderilen linke tıklayarak doğrulama yapınız.",
                            successMessage = "Kayıt başarılı! Lütfen e-mail adresinize gönderilen linke tıklayarak doğrulama yapınız.",
                            // Clear form fields except username for login screen
                            username = result.registeredUsername,
                            password = "", // Clear password
                            confirmPassword = "",
                            email = "",
                            businessName = "",
                            phoneNumber = "",
                            address = "",
                            taxNumber = ""
                        ) 
                    }
                }
                is RegisterResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        ) 
                    }
                }
                is RegisterResult.ValidationError -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        ) 
                    }
                }
            }
        }
    }
    
    /**
     * Signs out user
     */
    override fun signOut() {
        viewModelScope.launch {
            // Get current remember username setting before logout
            val shouldRememberUsername = _uiState.value.rememberUsername
            
            authRepository.signOut()
            _currentUser.value = null
            
            // Clear UI state based on remember username setting
            _uiState.update { 
                it.copy(
                    // Always clear password
                    password = "",
                    // Clear username only if "Beni Hatırla" is not checked
                    username = if (shouldRememberUsername) it.username else "",
                    isLoggedIn = false,
                    isRegistered = false,
                    errorMessage = null,
                    successMessage = null
                ) 
            }
        }
    }
    
    /**
     * Clears error message
     */
    override fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Clears success message
     */
    override fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    /**
     * Clears navigation flag after navigation is complete
     */
    override fun clearNavigationFlag() {
        _uiState.update { it.copy(shouldNavigateToLogin = false) }
    }
    
    /**
     * Clears registration success message
     */
    override fun clearRegistrationSuccess() {
        _uiState.update { it.copy(registrationSuccessMessage = null) }
    }
    
    /**
     * Clears register form fields for fresh registration
     * Keeps username from remembered preferences if available
     */
    override fun clearRegisterForm() {
        _uiState.update { 
            it.copy(
                email = "",
                password = "",
                confirmPassword = "",
                businessName = "",
                phoneNumber = "",
                address = "",
                taxNumber = "",
                errorMessage = null,
                successMessage = null,
                isRegistered = false
            ) 
        }
        // Reset username to remembered username or empty based on checkbox status
        if (_uiState.value.rememberUsername) {
            _uiState.update { it.copy(username = _rememberedUsername.value) }
        } else {
            _uiState.update { it.copy(username = "") }
        }
    }
    
    /**
     * Updates user profile information
     */
    override fun updateProfile() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        
        viewModelScope.launch {
            _currentUser.value?.let { firebaseUser ->
                // Get current user data and update with new values
                val currentUserData = authRepository.getUserData(firebaseUser.uid)
                if (currentUserData != null) {
                    val updatedUser = currentUserData.copy(
                        businessName = _uiState.value.businessName,
                        phoneNumber = _uiState.value.phoneNumber,
                        address = _uiState.value.address
                    )
                    
                    authRepository.updateUserData(updatedUser).fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = "Profil bilgileriniz başarıyla güncellendi."
                                )
                            }
                        },
                        onFailure = { exception ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = exception.message ?: "Profil güncellenirken hata oluştu."
                                )
                            }
                        }
                    )
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Kullanıcı bilgileri bulunamadı."
                        )
                    }
                }
            } ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Oturum açmamış kullanıcı."
                    )
                }
            }
        }
    }
    
    /**
     * Checks if user is logged in
     */
    override fun isLoggedIn(): Boolean {
        return _currentUser.value != null
    }
    
    /**
     * Loads user data from Firestore and updates UI state
     */
    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            authRepository.getUserData(userId)?.let { userData ->
                _uiState.update {
                    it.copy(
                        username = userData.username,
                        email = userData.email,
                        businessName = userData.businessName,
                        phoneNumber = userData.phoneNumber,
                        address = userData.address,
                        taxNumber = userData.taxNumber
                    )
                }
                _businessName.value = userData.businessName
            }
        }
    }


} 