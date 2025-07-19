package com.example.beautydate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.beautydate.data.local.ThemeMode
import com.example.beautydate.navigation.AppNavigation
import com.example.beautydate.navigation.NavigationRoutes
import com.example.beautydate.ui.theme.BeautyDateTheme
import com.example.beautydate.viewmodels.AuthViewModel
import com.example.beautydate.viewmodels.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for BeautyDate app
 * Handles navigation between Welcome, Login, Register, and Home screens
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BeautyDateApp()
        }
    }
}

/**
 * Main app composable with theme support
 */
@Composable
fun BeautyDateApp() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val customerViewModel: com.example.beautydate.viewmodels.CustomerViewModel = hiltViewModel()
    val employeeViewModel: com.example.beautydate.viewmodels.EmployeeViewModel = hiltViewModel()
    val appointmentViewModel: com.example.beautydate.viewmodels.AppointmentViewModel = hiltViewModel()
    val navController = rememberNavController()
    
    // Observe current theme
    val themeState by themeViewModel.uiState.collectAsState()
    val isDarkTheme = themeState.currentTheme == ThemeMode.DARK
    
    // Determine start destination based on login status
    val startDestination = if (authViewModel.isLoggedIn()) {
        NavigationRoutes.HOME
    } else {
        NavigationRoutes.WELCOME
    }
    
    BeautyDateTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(
                navController = navController,
                authViewModel = authViewModel,
                customerViewModel = customerViewModel,
                employeeViewModel = employeeViewModel,
                appointmentViewModel = appointmentViewModel,
                startDestination = startDestination
            )
        }
    }
}

