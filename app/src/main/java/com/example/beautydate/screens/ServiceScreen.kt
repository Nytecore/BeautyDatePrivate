package com.example.beautydate.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.beautydate.data.models.Service
import com.example.beautydate.data.models.ServiceCategory
import com.example.beautydate.viewmodels.ServiceViewModel
import com.example.beautydate.screens.components.ServiceCard
import com.example.beautydate.screens.components.ServiceEmptyState
import com.example.beautydate.screens.components.ServiceCategoryFilter
import com.example.beautydate.screens.components.BulkPriceUpdateSheet
import com.example.beautydate.components.ServiceFab
import com.example.beautydate.data.repository.PriceUpdateType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.ui.platform.LocalContext
import com.example.beautydate.utils.ToastUtils

/**
 * Service management screen
 * Displays list of services with search, filtering, and management options
 * Memory efficient: LazyColumn with minimal object creation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceScreen(
    onNavigateToAddService: () -> Unit,
    onNavigateToEditService: (String) -> Unit,
    onNavigateBack: () -> Unit, // Yeni parametre eklendi
    serviceViewModel: ServiceViewModel = hiltViewModel()
) {
    val uiState by serviceViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showBulkPriceUpdateSheet by remember { mutableStateOf(false) }
    var showIndividualPriceUpdateDialog by remember { mutableStateOf(false) }
    var selectedServiceForPriceUpdate by remember { mutableStateOf<Service?>(null) }
    
    LaunchedEffect(Unit) {
        // Initialize services when screen loads
        serviceViewModel.initializeServices()
    }
    
    // Handle success messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            ToastUtils.showSuccess(context, message)
            serviceViewModel.clearSuccessMessage()
        }
    }
    
    // Handle error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            ToastUtils.showError(context, message)
            serviceViewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hizmetlerimiz",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ServiceFab(
                onAddServiceClick = onNavigateToAddService,
                onPriceUpdateClick = { showBulkPriceUpdateSheet = true }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            ServiceSearchBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { serviceViewModel.searchServices(it) },
                modifier = Modifier.padding(16.dp)
            )
            
            // Category filter chips
            ServiceCategoryFilter(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { serviceViewModel.filterByCategory(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Service count
            if (uiState.services.isNotEmpty()) {
                Text(
                    text = "${uiState.totalServices} hizmet bulundu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Service list or empty state
            val filtered = uiState.filteredServices
            val hasSearchQuery = uiState.searchQuery.isNotBlank()
            val hasCategory = uiState.selectedCategory != null
            val showFiltered = hasSearchQuery || hasCategory
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if ((showFiltered && filtered.isEmpty()) || (!showFiltered && uiState.services.isEmpty())) {
                val categoryName = uiState.selectedCategory?.getDisplayName() ?: ""
                val message = when {
                    hasSearchQuery && hasCategory -> "İşletmenizin $categoryName kategorisinde '${uiState.searchQuery}' araması için sonuç bulunamadı."
                    hasSearchQuery -> "'${uiState.searchQuery}' araması için sonuç bulunamadı."
                    hasCategory -> "İşletmenizin $categoryName kategorisinde herhangi bir hizmeti şu anda yoktur."
                    else -> "Henüz hiç hizmet eklenmemiş."
                }
                ServiceEmptyState(
                    onAddService = onNavigateToAddService,
                    modifier = Modifier.fillMaxSize(),
                    message = message
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = if (showFiltered) filtered else uiState.services,
                        key = { it.id }
                    ) { service ->
                        ServiceCard(
                            service = service,
                            onEdit = { onNavigateToEditService(service.id) },
                            onToggleStatus = { serviceViewModel.toggleServiceStatus(service.id) },
                            onDelete = { serviceViewModel.deleteService(service.id) },
                            onPriceClick = {
                                selectedServiceForPriceUpdate = service
                                showIndividualPriceUpdateDialog = true
                            }
                        )
                    }
                }
            }
        }
        
        // Bulk price update sheet
        if (showBulkPriceUpdateSheet) {
            BulkPriceUpdateSheet(
                onUpdatePrices = { updateType, value ->
                    serviceViewModel.bulkUpdatePrices(updateType, value, category = null, serviceIds = emptyList())
                    showBulkPriceUpdateSheet = false
                },
                onDismiss = { showBulkPriceUpdateSheet = false }
            )
        }
        
        // Individual price update dialog
        if (showIndividualPriceUpdateDialog && selectedServiceForPriceUpdate != null) {
            IndividualPriceUpdateDialog(
                service = selectedServiceForPriceUpdate!!,
                onUpdatePrice = { newPrice ->
                    val updatedService = selectedServiceForPriceUpdate!!.copy(
                        price = newPrice,
                        updatedAt = com.google.firebase.Timestamp.now()
                    )
                    serviceViewModel.updateService(updatedService)
                    showIndividualPriceUpdateDialog = false
                    selectedServiceForPriceUpdate = null
                },
                onDismiss = {
                    showIndividualPriceUpdateDialog = false
                    selectedServiceForPriceUpdate = null
                }
            )
        }
        
        // Error message
        if (uiState.errorMessage != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(
                        onClick = {
                            serviceViewModel.clearError()
                        }
                    ) {
                        Text("Tamam")
                    }
                }
            ) {
                Text(uiState.errorMessage!!)
            }
        }
        
        // Success message
        if (uiState.successMessage != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(
                        onClick = {
                            serviceViewModel.clearSuccessMessage()
                        }
                    ) {
                        Text("Tamam")
                    }
                }
            ) {
                Text(uiState.successMessage!!)
            }
        }
    }
}

/**
 * Search bar for services
 * Memory efficient: single composable with state hoisting
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text("Hizmet ara...")
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Ara"
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Temizle"
                    )
                }
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * Individual price update dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IndividualPriceUpdateDialog(
    service: Service,
    onUpdatePrice: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var newPrice by remember { mutableStateOf(service.price.toInt().toString()) }
    var isValid by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Fiyat Güncelle",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "\"${service.name}\" hizmetinin fiyatını güncelleyin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = newPrice,
                    onValueChange = { value ->
                        newPrice = value
                        isValid = value.toDoubleOrNull()?.let { it > 0 } ?: false
                    },
                    label = { Text("Yeni Fiyat") },
                    suffix = { Text("₺") },
                    singleLine = true,
                    isError = !isValid,
                    supportingText = {
                        if (!isValid) {
                            Text(
                                text = "Geçerli bir fiyat girin",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Current price info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Mevcut fiyat: ${service.formattedPrice}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    newPrice.toDoubleOrNull()?.let { price ->
                        if (price > 0) {
                            onUpdatePrice(price)
                        }
                    }
                },
                enabled = isValid && newPrice.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text("Güncelle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}