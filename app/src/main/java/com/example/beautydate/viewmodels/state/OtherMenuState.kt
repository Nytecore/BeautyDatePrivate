package com.example.beautydate.viewmodels.state

import com.example.beautydate.domain.models.OtherMenuItem

/**
 * UI State for Other menu screen
 * Contains user information and menu items
 */
data class OtherMenuState(
    val businessName: String = "",
    val username: String = "",
    val menuItems: List<OtherMenuItem> = emptyList(),
    val showLogoutDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) 