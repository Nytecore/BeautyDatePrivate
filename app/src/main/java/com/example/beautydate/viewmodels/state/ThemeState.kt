package com.example.beautydate.viewmodels.state

import com.example.beautydate.data.local.ThemeMode

/**
 * UI State for theme management
 * Contains current theme and loading state
 */
data class ThemeState(
    val currentTheme: ThemeMode = ThemeMode.LIGHT,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) 