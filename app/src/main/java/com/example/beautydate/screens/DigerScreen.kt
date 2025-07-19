package com.example.beautydate.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.beautydate.components.OtherMenuItem
import com.example.beautydate.screens.components.DigerHeaderSection
import com.example.beautydate.viewmodels.OtherMenuViewModel

/**
 * "Diğer" screen showing profile header and menu options
 * Part of bottom navigation - rightmost tab with three dots icon
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigerScreen(
    onNavigateToProfileSettings: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    onNavigateToLogin: () -> Unit,
    otherMenuViewModel: OtherMenuViewModel = hiltViewModel()
) {
    val uiState by otherMenuViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Diğer",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        
        // Profile Header Section (non-clickable now)
        DigerHeaderSection(
            businessName = uiState.businessName.ifEmpty { "İşletme Adı" },
            username = uiState.username,
            onProfileClick = { /* No action - handled by menu item */ }
        )
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        
        // Menu Items
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // User Profile Section Header
            item {
                DigerHeaderSection(
                    businessName = uiState.businessName,
                    username = uiState.username,
                    onProfileClick = onNavigateToProfileSettings,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Menu items
            items(uiState.menuItems) { item ->
                OtherMenuItem(
                    item = item,
                    onClick = {
                        when (item.id) {
                            "profile_settings" -> onNavigateToProfileSettings()
                            "theme" -> onNavigateToTheme()
                            "feedback" -> onNavigateToFeedback()
                            "statistics" -> onNavigateToStatistics()
                            "finance" -> onNavigateToFinance()
                            "how_to_use" -> onNavigateToTutorial()
                            "logout" -> otherMenuViewModel.showLogoutDialog()
                            "notifications" -> {
                                Toast.makeText(
                                    context,
                                    "${item.title} çok yakında eklenecek",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
        }
    }
    
    // Logout confirmation dialog
    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { otherMenuViewModel.hideLogoutDialog() },
            title = {
                Text(text = "Çıkış Yap")
            },
            text = {
                Text(text = "Çıkış yapmayı onaylıyor musunuz?")
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        otherMenuViewModel.logout()
                        // Navigate immediately after logout call
                        onNavigateToLogin()
                    }
                ) {
                    Text("Çıkış")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { otherMenuViewModel.hideLogoutDialog() }
                ) {
                    Text("İptal")
                }
            }
        )
    }
    
    // Error message
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            otherMenuViewModel.clearError()
        }
    }
} 