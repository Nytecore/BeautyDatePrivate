package com.example.beautydate.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beautydate.data.repository.AuthRepository
import com.example.beautydate.domain.models.OtherMenuItemFactory
import com.example.beautydate.viewmodels.state.OtherMenuState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Other menu screen
 * Handles menu items, user info, and logout functionality
 * Follows Single Responsibility Principle
 */
@HiltViewModel
class OtherMenuViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(OtherMenuState())
    val uiState: StateFlow<OtherMenuState> = _uiState.asStateFlow()
    
    init {
        loadMenuItems()
        loadUserInfo()
    }
    
    /**
     * Loads menu items from factory
     */
    private fun loadMenuItems() {
        val menuItems = OtherMenuItemFactory.createMenuItems()
        _uiState.update { 
            it.copy(menuItems = menuItems) 
        }
    }
    
    /**
     * Loads current user information
     */
    private fun loadUserInfo() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            currentUser?.let { user ->
                val userData = authRepository.getUserData(user.uid)
                userData?.let { data ->
                    _uiState.update { 
                        it.copy(
                            businessName = data.businessName,
                            username = data.username
                        ) 
                    }
                }
            }
        }
    }
    
    /**
     * Shows logout confirmation dialog
     */
    fun showLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }
    
    /**
     * Hides logout confirmation dialog
     */
    fun hideLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }
    
    /**
     * Performs logout operation
     */
    fun logout() {
        _uiState.update { 
            it.copy(
                isLoading = true, 
                showLogoutDialog = false,
                errorMessage = null
            ) 
        }
        
        viewModelScope.launch {
            try {
                authRepository.signOut()
                // ViewModel doesn't handle navigation directly
                // UI layer will handle navigation through callbacks
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        businessName = "",
                        username = ""
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Çıkış yapılırken hata oluştu"
                    ) 
                }
            }
        }
    }
    
    /**
     * Clears any error messages
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Refreshes user information
     */
    fun refreshUserInfo() {
        loadUserInfo()
    }
} 