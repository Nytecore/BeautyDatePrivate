package com.example.beautydate.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beautydate.data.models.Service
import com.example.beautydate.data.models.ServiceCategory
import com.example.beautydate.data.repository.ServiceRepository
import com.example.beautydate.data.repository.PriceUpdateType
import com.example.beautydate.utils.NetworkMonitor
import com.example.beautydate.utils.AuthUtil
import com.example.beautydate.viewmodels.state.ServiceUiState
import com.example.beautydate.viewmodels.actions.ServiceAction
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.example.beautydate.data.models.User
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for service management functionality
 * Handles all service-related operations following MVVM pattern
 * Multi-tenant architecture: BusinessId handled automatically by repositories
 * Memory efficient: Flow-based reactive data and minimal object creation
 */
@HiltViewModel
class ServiceViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val networkMonitor: NetworkMonitor,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val authUtil: AuthUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceUiState())
    val uiState: StateFlow<ServiceUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var networkMonitorJob: Job? = null
    private var isInitialized: Boolean = false

    init {
        // Start monitoring network connectivity
        startNetworkMonitoring()
    }

    /**
     * Initializes services with automatic authentication check
     * BusinessId is handled automatically by repository layer
     */
    fun initializeServices() {
        println("Debug: ServiceViewModel - initializeServices called")
        
        // Set loading state immediately for better UX
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )
        
        // Check if user is authenticated before proceeding
        if (!authUtil.isUserAuthenticated()) {
            println("Debug: ServiceViewModel - User not authenticated")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = authUtil.getAuthErrorMessage()
            )
            return
        }
        
        val businessId = authUtil.getCurrentBusinessIdSafe()
        println("Debug: ServiceViewModel - User authenticated: $businessId")
        
        if (isInitialized && _uiState.value.services.isNotEmpty()) {
            println("Debug: ServiceViewModel - Already initialized, skipping")
            _uiState.value = _uiState.value.copy(isLoading = false)
            return // Already initialized
        }
        
        isInitialized = true
        println("Debug: ServiceViewModel - Setting initialized to true")
        
        // Perform initial sync to ensure offline functionality
        performInitialSyncIfNeeded()
        
        // Load services from local database
        loadServices()
    }

    /**
     * Syncs services with Firestore (manual refresh)
     * BusinessId handled automatically by repository
     */
    fun syncServices() {
        if (authUtil.isUserAuthenticated()) {
            manualSync()
        }
    }

    /**
     * Manually triggers sync with loading state management
     * Used for refresh button functionality
     * BusinessId handled automatically by repository
     */
    private fun manualSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val result = serviceRepository.syncWithFirestore("")
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Servisler başarıyla senkronize edildi"
                    )
                    
                    // Clear success message after delay
                    delay(2000)
                    clearSuccessMessage()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Senkronizasyon hatası: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Senkronizasyon hatası: ${e.message}"
                )
            }
        }
    }

    /**
     * Performs initial sync if needed for offline functionality
     * BusinessId handled automatically by repository
     */
    private fun performInitialSyncIfNeeded() {
        viewModelScope.launch {
            try {
                serviceRepository.syncWithFirestore("")
            } catch (e: Exception) {
                println("Debug: ServiceViewModel - Initial sync failed: ${e.message}")
                // Don't show error for initial sync failure - will work offline
            }
        }
    }

    /**
     * Loads services from local database using repository
     * BusinessId handled automatically by repository layer
     */
    private fun loadServices() {
        // Check authentication before loading services
        if (!authUtil.isUserAuthenticated()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = authUtil.getAuthErrorMessage()
            )
            return
        }
        
        println("Debug: ServiceViewModel - Loading services")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            serviceRepository.getAllServices("") // No businessId parameter needed
                .catch { error ->
                    println("Debug: ServiceViewModel - Error loading services: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Servisler yüklenirken hata oluştu: ${error.message}"
                    )
                }
                .collect { services ->
                    println("Debug: ServiceViewModel - Loaded ${services.size} services from database")
                    services.forEach { service ->
                        println("Debug: Service found - ${service.name} (${service.id}) - ${service.formattedPrice}")
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        services = services,
                        isLoading = false,
                        totalServices = services.size,
                        errorMessage = null
                    )
                    
                    // Update filtered services if there's a search query
                    if (_uiState.value.searchQuery.isNotBlank()) {
                        searchServices(_uiState.value.searchQuery)
                    }
                }
        }
    }

    /**
     * Searches services by query
     * BusinessId handled automatically by repository
     */
    fun searchServices(query: String) {
        // Cancel previous search job for memory efficiency
        searchJob?.cancel()
        
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            isSearching = true
        )
        
        searchJob = viewModelScope.launch {
            // Small delay for better UX (debounce effect)
            delay(300)
            
            try {
                serviceRepository.searchServices("", query) // No businessId parameter needed
                    .catch { error ->
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            errorMessage = "Arama sırasında hata oluştu: ${error.message}"
                        )
                    }
                    .collect { searchResults ->
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            services = searchResults
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    errorMessage = "Arama sırasında hata oluştu: ${e.message}"
                )
            }
        }
    }

    /**
     * Filters services by category
     * BusinessId handled automatically by repository
     */
    fun filterByCategory(category: ServiceCategory?) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            isLoading = true
        )
        
        viewModelScope.launch {
            try {
                val flow = if (category != null) {
                    serviceRepository.getServicesByCategory("", category) // No businessId parameter needed
                } else {
                    serviceRepository.getAllServices("") // No businessId parameter needed
                }
                
                flow.catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Filtreleme sırasında hata oluştu: ${error.message}"
                    )
                }.collect { filteredServices ->
                    _uiState.value = _uiState.value.copy(
                        services = filteredServices,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Filtreleme sırasında hata oluştu: ${e.message}"
                )
            }
        }
    }

    /**
     * Adds a new service
     * BusinessId automatically set by repository layer
     */
    fun addService(service: Service) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val result = serviceRepository.addService(service)
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Servis başarıyla eklendi",
                        showAddServiceSheet = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Servis eklenirken hata oluştu: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Servis eklenirken hata oluştu: ${e.message}"
                )
            }
        }
    }

    /**
     * Gets a service by ID
     * BusinessId validation handled by repository layer
     */
    suspend fun getServiceById(serviceId: String): Service? {
        return try {
            serviceRepository.getServiceById(serviceId)
        } catch (e: Exception) {
            println("Debug: ServiceViewModel - Error getting service by ID: ${e.message}")
            null
        }
    }

    /**
     * Updates an existing service
     * BusinessId validation handled by repository layer
     */
    fun updateService(service: Service) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val result = serviceRepository.updateService(service)
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Servis başarıyla güncellendi"
                    )
                    
                    // Clear success message after delay
                    delay(2000)
                    clearSuccessMessage()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Servis güncellenirken hata oluştu: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Servis güncellenirken hata oluştu: ${e.message}"
                )
            }
        }
    }

    /**
     * Deletes a service
     * BusinessId validation handled by repository layer
     */
    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val result = serviceRepository.deleteService(serviceId)
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Servis başarıyla silindi"
                    )
                    
                    // Clear success message after delay
                    delay(2000)
                    clearSuccessMessage()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Servis silinirken hata oluştu: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Servis silinirken hata oluştu: ${e.message}"
                )
            }
        }
    }

    /**
     * Bulk updates service prices
     * BusinessId handled automatically by repository layer
     */
    fun bulkUpdatePrices(
        updateType: PriceUpdateType,
        value: Double,
        category: ServiceCategory?,
        serviceIds: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val result = serviceRepository.bulkUpdatePrices(
                    businessId = "", // Will be handled automatically by repository
                    updateType = updateType,
                    value = value,
                    category = category,
                    serviceIds = serviceIds
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Fiyatlar başarıyla güncellendi",
                        showBulkUpdateSheet = false
                    )
                    
                    // Clear success message after delay
                    delay(2000)
                    clearSuccessMessage()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Fiyat güncellemesi sırasında hata oluştu: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Fiyat güncellemesi sırasında hata oluştu: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggles service active status
     * BusinessId validation handled by repository layer
     */
    fun toggleServiceStatus(serviceId: String) {
        viewModelScope.launch {
            try {
                val service = serviceRepository.getServiceById(serviceId)
                if (service != null) {
                    val updatedService = service.copy(isActive = !service.isActive)
                    updateService(updatedService)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Servis durumu değiştirilirken hata oluştu: ${e.message}"
                )
            }
        }
    }

    /**
     * Starts monitoring network connectivity for sync operations
     */
    private fun startNetworkMonitoring() {
        networkMonitorJob = viewModelScope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                _uiState.value = _uiState.value.copy(isOnline = isConnected)
                
                // Auto-sync when network becomes available
                if (isConnected && authUtil.isUserAuthenticated()) {
                    performBackgroundSync()
                }
            }
        }
    }

    /**
     * Performs background sync when network becomes available
     * BusinessId handled automatically by repository
     */
    private fun performBackgroundSync() {
        viewModelScope.launch {
            try {
                serviceRepository.syncWithFirestore("")
                println("Debug: ServiceViewModel - Background sync completed")
            } catch (e: Exception) {
                println("Debug: ServiceViewModel - Background sync failed: ${e.message}")
                // Silent failure for background sync
            }
        }
    }

    /**
     * Clears error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Clears success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /**
     * Handles service actions with improved action pattern
     * BusinessId handled automatically by repositories
     */
    fun handleAction(action: ServiceAction) {
        when (action) {
            is ServiceAction.AddService -> addService(action.service)
            is ServiceAction.UpdateService -> updateService(action.service)
            is ServiceAction.DeleteService -> deleteService(action.serviceId)
            is ServiceAction.SearchServices -> searchServices(action.query)
            is ServiceAction.FilterByCategory -> filterByCategory(action.category)
            is ServiceAction.BulkUpdatePrices -> bulkUpdatePrices(action.updateType, action.value, action.category, action.serviceIds)
            is ServiceAction.ToggleServiceStatus -> toggleServiceStatus(action.serviceId)
            is ServiceAction.SyncServices -> manualSync() // No businessId parameter needed
            is ServiceAction.InitializeServices -> initializeServices() // No businessId parameter needed
            is ServiceAction.ClearError -> clearError()
            is ServiceAction.ClearSuccess -> clearSuccessMessage()
            is ServiceAction.ShowAddServiceSheet -> _uiState.value = _uiState.value.copy(showAddServiceSheet = action.show)
            is ServiceAction.ShowBulkUpdateSheet -> _uiState.value = _uiState.value.copy(showBulkUpdateSheet = action.show)
            is ServiceAction.SetSelectedService -> _uiState.value = _uiState.value.copy(selectedService = action.service)
            is ServiceAction.SetSelectedServices -> _uiState.value = _uiState.value.copy(selectedServices = action.serviceIds)
            is ServiceAction.SetSelectionMode -> _uiState.value = _uiState.value.copy(isInSelectionMode = action.isSelectionMode)
        }
    }

    /**
     * Cleanup when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        networkMonitorJob?.cancel()
    }
} 