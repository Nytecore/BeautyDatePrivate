package com.example.beautydate.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beautydate.data.models.Appointment
import com.example.beautydate.data.models.AppointmentStatus
import com.example.beautydate.data.models.Customer
import com.example.beautydate.data.repository.AppointmentRepository
import com.example.beautydate.data.repository.CustomerRepository
import com.example.beautydate.utils.NetworkMonitor
import com.example.beautydate.utils.AuthUtil
import com.example.beautydate.viewmodels.state.AppointmentUiState
import com.google.firebase.auth.FirebaseAuth
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
 * ViewModel for appointment management functionality
 * Handles all appointment-related operations following MVVM pattern
 * Multi-tenant architecture: BusinessId handled automatically by AuthUtil
 * Memory efficient: Flow-based reactive data and minimal object creation
 */
@HiltViewModel
class AppointmentViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val customerRepository: CustomerRepository,
    private val networkMonitor: NetworkMonitor,
    private val firebaseAuth: FirebaseAuth,
    private val authUtil: AuthUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentUiState())
    val uiState: StateFlow<AppointmentUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var networkMonitorJob: Job? = null
    private var isInitialized: Boolean = false

    init {
        // Start monitoring network connectivity
        startNetworkMonitoring()
    }

    /**
     * Initializes appointment data with automatic authentication check
     * Memory efficient: reuses existing data if already loaded
     * BusinessId handled automatically by AuthUtil
     */
    fun initializeAppointments() {
        println("Debug: AppointmentViewModel - initializeAppointments called")
        
        // Set loading state immediately for better UX
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )
        
        // Check if user is authenticated before proceeding
        if (!authUtil.isUserAuthenticated()) {
            println("Debug: AppointmentViewModel - User not authenticated")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = authUtil.getAuthErrorMessage()
            )
            return
        }
        
        val businessId = authUtil.getCurrentBusinessIdSafe()
        println("Debug: AppointmentViewModel - User authenticated: $businessId")
        
        if (isInitialized && _uiState.value.appointments.isNotEmpty()) {
            println("Debug: AppointmentViewModel - Already initialized, skipping")
            _uiState.value = _uiState.value.copy(isLoading = false)
            return // Already initialized
        }
        
        isInitialized = true
        println("Debug: AppointmentViewModel - Setting initialized to true")
        
        // Perform initial sync to ensure offline functionality
        performInitialSyncIfNeeded()
        
        // Load appointments from local database
        loadAppointments()
        
        // Load customers for selection
        loadCustomers()
    }

    /**
     * Enhanced loadAppointments with better sync coordination
     * Memory efficient: Flow-based data loading with sync conflict resolution
     * BusinessId handled automatically by repository layer
     */
    private fun loadAppointments() {
        // Check authentication before loading appointments
        if (!authUtil.isUserAuthenticated()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = authUtil.getAuthErrorMessage()
            )
            return
        }
        
        println("Debug: AppointmentViewModel - Loading appointments")
        
        viewModelScope.launch {
            try {
                appointmentRepository.getAllAppointments("")
                    .catch { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Randevular yüklenirken hata oluştu: ${exception.message}"
                        )
                    }
                    .collect { appointments ->
                        // Update UI state with new appointments
                        _uiState.value = _uiState.value.copy(
                            appointments = appointments,
                            isLoading = false,
                            errorMessage = null
                        )
                        
                        // Load statistics after appointments are loaded
                        loadAppointmentStatistics()
                        
                        println("Debug: AppointmentViewModel - Loaded ${appointments.size} appointments")
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Randevular yüklenirken hata oluştu: ${e.message}"
                )
                println("Debug: AppointmentViewModel - Error loading appointments: ${e.message}")
            }
        }
    }

    /**
     * Loads appointment statistics for dashboard
     * BusinessId handled automatically by repository layer
     */
    private fun loadAppointmentStatistics() {
        viewModelScope.launch {
            try {
                val totalCount = appointmentRepository.getAppointmentCount("")
                
                _uiState.value = _uiState.value.copy(
                    appointmentStatistics = mapOf(
                        AppointmentStatus.SCHEDULED to totalCount,
                        AppointmentStatus.COMPLETED to 0,
                        AppointmentStatus.CANCELLED to 0,
                        AppointmentStatus.NO_SHOW to 0
                    )
                )
                
                println("Debug: AppointmentViewModel - Statistics: Total=$totalCount")
            } catch (e: Exception) {
                println("Debug: AppointmentViewModel - Error loading statistics: ${e.message}")
            }
        }
    }

    /**
     * Loads customers for appointment creation
     * BusinessId handled automatically by repository layer
     */
    private fun loadCustomers() {
        viewModelScope.launch {
            try {
                customerRepository.getAllCustomers()
                    .catch { exception ->
                        println("Debug: AppointmentViewModel - Error loading customers: ${exception.message}")
                    }
                    .collect { customers ->
                        _uiState.value = _uiState.value.copy(customers = customers)
                        println("Debug: AppointmentViewModel - Loaded ${customers.size} customers")
                    }
            } catch (e: Exception) {
                println("Debug: AppointmentViewModel - Error loading customers: ${e.message}")
            }
        }
    }

    /**
     * Adds a new appointment
     * Business logic: validates appointment data and handles creation
     */
    fun addAppointment(
        customer: Customer,
        serviceName: String,
        servicePrice: Double,
        appointmentDate: String,
        appointmentTime: String,
        notes: String = "",
        serviceId: String? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val appointment = Appointment.createForCustomer(
                    customer = customer,
                    serviceName = serviceName,
                    servicePrice = servicePrice,
                    appointmentDate = appointmentDate,
                    appointmentTime = appointmentTime,
                    businessId = authUtil.getCurrentBusinessIdSafe(),
                    notes = notes,
                    serviceId = serviceId
                )
                
                if (!appointment.isValid()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Lütfen tüm gerekli alanları doldurun."
                    )
                    return@launch
                }
                
                val result = appointmentRepository.addAppointment(appointment)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Randevu başarıyla oluşturuldu.",
                            showAddAppointmentSheet = false
                        )
                        clearFormData()
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Randevu oluşturulurken hata oluştu: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Beklenmeyen hata: ${e.message}"
                )
            }
        }
    }

    /**
     * Enhanced status update with optimistic updates
     * Memory efficient: local update + remote sync pattern
     */
    fun updateAppointmentStatus(appointmentId: String, status: AppointmentStatus) {
        viewModelScope.launch {
            try {
                // Show immediate feedback
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val result = appointmentRepository.updateAppointmentStatus(appointmentId, status)
                result.fold(
                    onSuccess = {
                        val statusMessage = when (status) {
                            AppointmentStatus.COMPLETED -> "Randevu tamamlandı olarak işaretlendi"
                            AppointmentStatus.CANCELLED -> "Randevu iptal edildi"
                            AppointmentStatus.NO_SHOW -> "Randevu 'Gelmedi' olarak işaretlendi"
                            AppointmentStatus.SCHEDULED -> "Randevu zamanlandı"
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = statusMessage
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Durum güncellenirken hata oluştu: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Beklenmeyen hata: ${e.message}"
                )
            }
        }
    }

    /**
     * Updates an existing appointment
     * Business logic: validates appointment data and handles update
     */
    fun updateAppointment(appointment: Appointment) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                if (!appointment.isValid()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Lütfen tüm gerekli alanları doldurun."
                    )
                    return@launch
                }
                
                val result = appointmentRepository.updateAppointment(appointment)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Randevu başarıyla güncellendi"
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Randevu güncellenirken hata oluştu: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Beklenmeyen hata: ${e.message}"
                )
            }
        }
    }

    /**
     * Deletes an appointment
     */
    fun deleteAppointment(appointmentId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val result = appointmentRepository.deleteAppointment(appointmentId)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Randevu silindi."
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Randevu silinirken hata oluştu: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Beklenmeyen hata: ${e.message}"
                )
            }
        }
    }

    /**
     * Filters appointments by status and search query
     * Fixed: Proper status filtering without showing only SCHEDULED when null
     */
    fun filterAppointments(status: AppointmentStatus?) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
        // Remove the old manual filtering - let the UI state computed property handle it
    }

    /**
     * Searches appointments by customer name or phone
     * Enhanced: Real-time reactive search with debouncing
     */
    fun searchAppointments(query: String) {
        // Cancel previous search job to implement debouncing
        searchJob?.cancel()
        
        searchJob = viewModelScope.launch {
            // Add small delay for debouncing
            kotlinx.coroutines.delay(300)
            
            _uiState.value = _uiState.value.copy(searchQuery = query.trim())
        }
    }

    /**
     * Shows/hides add appointment sheet
     * UI state management: sheet visibility
     */
    fun setShowAddAppointmentSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddAppointmentSheet = show)
    }

    /**
     * Selects an appointment for detail view
     * UI state management: appointment selection
     */
    fun selectAppointment(appointment: Appointment?) {
        _uiState.value = _uiState.value.copy(selectedAppointment = appointment)
    }

    /**
     * Clears success and error messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    /**
     * Performs initial sync if needed
     * BusinessId handled automatically by repository layer
     */
    private fun performInitialSyncIfNeeded() {
        viewModelScope.launch {
            try {
                // Only sync if network is available
                if (networkMonitor.isCurrentlyConnected()) {
                    appointmentRepository.syncWithFirestore("")
                }
            } catch (e: Exception) {
                // Silent failure for background sync
                println("Debug: AppointmentViewModel - Background sync failed: ${e.message}")
            }
        }
    }

    /**
     * Starts network monitoring for sync management
     */
    private fun startNetworkMonitoring() {
        networkMonitorJob = viewModelScope.launch {
            networkMonitor.isConnected.collect { isOnline ->
                _uiState.value = _uiState.value.copy(isOnline = isOnline)
                
                // Auto-sync when coming back online
                if (isOnline && isInitialized) {
                    performInitialSyncIfNeeded()
                }
            }
        }
    }

    /**
     * Enhanced sync with conflict resolution
     * Memory efficient: optimized sync process with duplicate prevention
     * BusinessId handled automatically by repository layer
     */
    fun syncAppointments() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSyncing = true, errorMessage = null)
                
                // Perform sync with conflict resolution - no businessId parameter needed
                val result = appointmentRepository.syncWithFirestore("")
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            successMessage = "Randevular başarıyla senkronize edildi"
                        )
                        
                        // Reload statistics after sync
                        loadAppointmentStatistics()
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            errorMessage = "Senkronizasyon hatası: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = "Senkronizasyon hatası: ${e.message}"
                )
            }
        }
    }

    /**
     * Helper method to filter appointments
     */
    private fun filterAppointments(
        appointments: List<Appointment>,
        status: AppointmentStatus?,
        query: String
    ): List<Appointment> {
        return appointments.filter { appointment ->
            // When status is null (Tümü selected), show only SCHEDULED appointments
            val matchesStatus = if (status == null) {
                appointment.status == AppointmentStatus.SCHEDULED
            } else {
                appointment.status == status
            }
            
            val matchesQuery = query.isBlank() || 
                appointment.customerName.contains(query, ignoreCase = true) ||
                appointment.customerPhone.contains(query, ignoreCase = true) ||
                appointment.serviceName.contains(query, ignoreCase = true)
            
            matchesStatus && matchesQuery
        }
    }

    /**
     * Clears form data
     */
    private fun clearFormData() {
        // Form data clearing can be implemented if needed for add appointment form
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        networkMonitorJob?.cancel()
    }
} 