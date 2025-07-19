package com.example.beautydate.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beautydate.data.models.Appointment
import com.example.beautydate.data.models.AppointmentStatus
import com.example.beautydate.data.models.Customer
import com.example.beautydate.data.models.Service
import com.example.beautydate.data.models.DayOfWeek
import com.example.beautydate.data.repository.AppointmentRepository
import com.example.beautydate.data.repository.WorkingHoursRepository
import com.example.beautydate.data.repository.CustomerRepository
import com.example.beautydate.data.repository.ServiceRepository
import com.example.beautydate.domain.usecases.appointment.AddAppointmentUseCase
import com.example.beautydate.domain.usecases.appointment.UpdateAppointmentStatusUseCase
import com.example.beautydate.utils.NetworkMonitor
import com.example.beautydate.utils.AuthUtil
import com.example.beautydate.viewmodels.state.CalendarUiState
import com.example.beautydate.viewmodels.state.TimeSlot
import com.example.beautydate.viewmodels.state.TimeSlotStatus
import com.example.beautydate.viewmodels.state.CalendarDay
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for Calendar screen
 * Handles calendar functionality, appointment management, and working hours integration
 * Multi-tenant architecture: BusinessId handled automatically by AuthUtil
 * Memory efficient: Flow-based reactive data and computed properties
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val addAppointmentUseCase: AddAppointmentUseCase,
    private val updateAppointmentStatusUseCase: UpdateAppointmentStatusUseCase,
    private val workingHoursRepository: WorkingHoursRepository,
    private val customerRepository: CustomerRepository,
    private val serviceRepository: ServiceRepository,
    private val appointmentRepository: AppointmentRepository,
    private val networkMonitor: NetworkMonitor,
    private val firebaseAuth: FirebaseAuth,
    private val authUtil: AuthUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var networkMonitorJob: Job? = null
    private var isInitialized: Boolean = false

    init {
        startNetworkMonitoring()
        initializeCurrentWeek()
    }

    /**
     * Initializes calendar with business data
     * Memory efficient: single initialization call
     * BusinessId handled automatically by AuthUtil
     */
    fun initializeCalendar() {
        println("Debug: CalendarViewModel - initializeCalendar called")
        
        // Check if user is authenticated before proceeding
        if (!authUtil.isUserAuthenticated()) {
            println("Debug: CalendarViewModel - User not authenticated")
            _uiState.value = _uiState.value.copy(
                errorMessage = authUtil.getAuthErrorMessage()
            )
            return
        }
        
        val businessId = authUtil.getCurrentBusinessIdSafe()
        println("Debug: CalendarViewModel - User authenticated: $businessId")
        
        if (isInitialized) {
            println("Debug: CalendarViewModel - Already initialized, skipping")
            return
        }
        
        isInitialized = true
        
        try {
            // Perform initial sync to ensure data availability
            performInitialSyncIfNeeded()
            
            loadWorkingHours()
            
            loadAppointments()
            
            loadCustomersAndServices()
            
            generateTimeSlotsForSelectedDate()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Takvim başlatılırken hata oluştu: ${e.message}"
            )
        }
    }

    /**
     * Selects a specific date and updates calendar
     * Memory efficient: targeted date update with time slot regeneration
     */
    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        
        viewModelScope.launch {
            updateSelectedDateData()
            generateTimeSlotsForSelectedDate()
        }
    }

    /**
     * Navigates to next week
     * Memory efficient: computed week navigation
     */
    fun navigateToNextWeek() {
        val currentWeek = _uiState.value.currentWeekDays
        if (currentWeek.isNotEmpty()) {
            val nextWeekStart = currentWeek.first().plusWeeks(1)
            updateCurrentWeek(nextWeekStart)
        }
    }

    /**
     * Navigates to previous week
     * Memory efficient: computed week navigation
     */
    fun navigateToPreviousWeek() {
        val currentWeek = _uiState.value.currentWeekDays
        if (currentWeek.isNotEmpty()) {
            val previousWeekStart = currentWeek.first().minusWeeks(1)
            updateCurrentWeek(previousWeekStart)
        }
    }

    /**
     * Navigates to next month
     * Memory efficient: monthly navigation with calendar grid update
     */
    fun navigateToNextMonth() {
        val currentDate = _uiState.value.selectedDate
        val nextMonth = currentDate.plusMonths(1)
        _uiState.value = _uiState.value.copy(
            selectedMonth = nextMonth.monthValue,
            selectedYear = nextMonth.year
        )
        updateCurrentMonth(nextMonth)
    }

    /**
     * Navigates to previous month
     * Memory efficient: monthly navigation with calendar grid update
     */
    fun navigateToPreviousMonth() {
        val currentDate = _uiState.value.selectedDate
        val previousMonth = currentDate.minusMonths(1)
        _uiState.value = _uiState.value.copy(
            selectedMonth = previousMonth.monthValue,
            selectedYear = previousMonth.year
        )
        updateCurrentMonth(previousMonth)
    }

    /**
     * Navigates to next year (±1 year range: 2023-2025)
     * Professional calendar: limited year range for business use
     */
    fun navigateToNextYear() {
        val currentDate = _uiState.value.selectedDate
        val currentYear = currentDate.year
        
        // Limit to +1 year range (max 2025)
        if (currentYear < 2025) {
            val nextYear = currentDate.plusYears(1)
            _uiState.value = _uiState.value.copy(
                selectedMonth = nextYear.monthValue,
                selectedYear = nextYear.year
            )
            updateCurrentMonth(nextYear)
        }
    }

    /**
     * Navigates to previous year (±1 year range: 2023-2025)
     * Professional calendar: limited year range for business use
     */
    fun navigateToPreviousYear() {
        val currentDate = _uiState.value.selectedDate
        val currentYear = currentDate.year
        
        // Limit to -1 year range (min 2023)
        if (currentYear > 2023) {
            val previousYear = currentDate.minusYears(1)
            _uiState.value = _uiState.value.copy(
                selectedMonth = previousYear.monthValue,
                selectedYear = previousYear.year
            )
            updateCurrentMonth(previousYear)
        }
    }

    /**
     * Selects a time slot for appointment creation
     * Memory efficient: single slot selection with card display
     */
    fun selectTimeSlot(timeSlot: TimeSlot) {
        if (timeSlot.status == TimeSlotStatus.AVAILABLE) {
            _uiState.value = _uiState.value.copy(
                selectedTimeSlot = timeSlot,
                showSlotSelectionCard = true
            )
        } else if (timeSlot.status == TimeSlotStatus.BOOKED && timeSlot.appointment != null) {
            // Navigate to appointment detail (handled in UI)
            _uiState.value = _uiState.value.copy(selectedTimeSlot = timeSlot)
        }
    }

    /**
     * Hides slot selection card
     * Memory efficient: single boolean update
     */
    fun hideSlotSelectionCard() {
        _uiState.value = _uiState.value.copy(
            showSlotSelectionCard = false,
            selectedTimeSlot = null
        )
    }

    /**
     * Shows month selector
     * Memory efficient: single boolean update
     */
    fun showMonthSelector() {
        _uiState.value = _uiState.value.copy(showMonthSelector = true)
    }

    /**
     * Hides month selector
     * Memory efficient: single boolean update
     */
    fun hideMonthSelector() {
        _uiState.value = _uiState.value.copy(showMonthSelector = false)
    }

    /**
     * Selects month and year
     * Memory efficient: date navigation with calendar update
     */
    fun selectMonth(month: Int, year: Int) {
        val newDate = LocalDate.of(year, month, 1)
        _uiState.value = _uiState.value.copy(
            selectedMonth = month,
            selectedYear = year,
            selectedDate = newDate,
            showMonthSelector = false
        )
        
        updateCurrentWeek(newDate)
        updateCurrentMonth(newDate)
        viewModelScope.launch {
            updateSelectedDateData()
            generateTimeSlotsForSelectedDate()
        }
    }

    /**
     * Manually syncs appointments with Firestore
     * Memory efficient: network-aware sync with state management
     */
    fun syncAppointments() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSyncing = true, errorMessage = null)

                val result = appointmentRepository.syncWithFirestore(authUtil.getCurrentBusinessIdSafe())
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            successMessage = "Randevular senkronize edildi"
                        )
                        clearMessageAfterDelay()
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
     * Clears success/error messages
     * Memory efficient: selective state clearing
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    /**
     * Loads working hours from repository
     * Memory efficient: single repository call with reactive updates
     */
    private fun loadWorkingHours() {
        viewModelScope.launch {
            try {
                workingHoursRepository.getWorkingHoursFlow()
                    .catch { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingWorkingHours = false,
                            errorMessage = "Çalışma saatleri yüklenemedi: ${exception.message}"
                        )
                    }
                    .collect { workingHours ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingWorkingHours = false,
                            workingHours = workingHours
                        )
                        updateSelectedDateData()
                        generateTimeSlotsForSelectedDate()
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingWorkingHours = false,
                    errorMessage = "Çalışma saatleri yüklenemedi: ${e.message}"
                )
            }
        }
    }

    /**
     * Loads appointments from repository
     * Memory efficient: reactive appointment loading with date filtering
     */
    private fun loadAppointments() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingAppointments = true)

                // Use direct repository call instead of UseCase to match AppointmentViewModel behavior
                appointmentRepository.getAllAppointments(authUtil.getCurrentBusinessIdSafe())
                    .catch { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingAppointments = false,
                            errorMessage = "Randevular yüklenemedi: ${exception.message}"
                        )
                    }
                    .collect { appointments ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingAppointments = false,
                            appointments = appointments
                        )
                        updateAppointmentsForSelectedDate()
                        generateTimeSlotsForSelectedDate()
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingAppointments = false,
                    errorMessage = "Randevular yüklenemedi: ${e.message}"
                )
            }
        }
    }

    /**
     * Loads customers and services for appointment creation
     * BusinessId handled automatically by repositories
     */
    private fun loadCustomersAndServices() {
        viewModelScope.launch {
            try {
                // Load customers
                customerRepository.getAllCustomers()
                    .catch { exception ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Müşteriler yüklenirken hata oluştu: ${exception.message}"
                        )
                    }
                    .collect { customers ->
                        _uiState.value = _uiState.value.copy(availableCustomers = customers)
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Müşteriler yüklenirken hata oluştu: ${e.message}"
                )
            }
        }

        viewModelScope.launch {
            // Load services
            serviceRepository.getAllServices(authUtil.getCurrentBusinessIdSafe())
                .catch { /* Ignore errors for non-critical data */ }
                .collect { services ->
                    _uiState.value = _uiState.value.copy(availableServices = services)
                }
        }
    }

    /**
     * Generates time slots for the selected date
     * Memory efficient: computed time slots based on working hours and appointments
     */
    private fun generateTimeSlotsForSelectedDate() {
        val workingHours = _uiState.value.workingHours ?: return
        val selectedDate = _uiState.value.selectedDate
        val appointments = _uiState.value.appointmentsForSelectedDate

        // Get day of week
        val dayOfWeek = when (selectedDate.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
            java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            java.time.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }

        val dayHours = workingHours.getDayHours(dayOfWeek)
        
        if (!dayHours.isWorking) {
            _uiState.value = _uiState.value.copy(
                isWorkingDay = false,
                dayStartTime = null,
                dayEndTime = null,
                timeSlots = emptyList()
            )
            return
        }

        val startTime = LocalTime.parse(dayHours.startTime, DateTimeFormatter.ofPattern("HH:mm"))
        val endTime = LocalTime.parse(dayHours.endTime, DateTimeFormatter.ofPattern("HH:mm"))
        
        _uiState.value = _uiState.value.copy(
            isWorkingDay = true,
            dayStartTime = startTime,
            dayEndTime = endTime
        )

        // Generate hourly time slots
        val timeSlots = mutableListOf<TimeSlot>()
        var currentTime = startTime

        while (currentTime.isBefore(endTime)) {
            val timeSlotStatus = determineTimeSlotStatus(selectedDate, currentTime, appointments)
            val appointment = appointments.find { 
                it.appointmentTime == currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            }

            timeSlots.add(
                TimeSlot(
                    time = currentTime,
                    status = timeSlotStatus,
                    appointment = appointment
                )
            )

            currentTime = currentTime.plusHours(1)
        }

        _uiState.value = _uiState.value.copy(timeSlots = timeSlots)
    }

    /**
     * Determines time slot status based on date, time and existing appointments
     * Memory efficient: business logic computation
     */
    private fun determineTimeSlotStatus(
        date: LocalDate,
        time: LocalTime,
        appointments: List<Appointment>
    ): TimeSlotStatus {
        val now = LocalDate.now()
        val currentTime = LocalTime.now()

        // Check if it's in the past
        if (date.isBefore(now) || (date == now && time.isBefore(currentTime))) {
            return TimeSlotStatus.PAST
        }

        // Check if there's an appointment at this time (any active status)
        val timeString = time.format(DateTimeFormatter.ofPattern("HH:mm"))
        val hasAppointment = appointments.any { appointment ->
            val appointmentTimeMatches = appointment.appointmentTime == timeString
            val statusIsActive = appointment.status in setOf(
                AppointmentStatus.SCHEDULED,
                AppointmentStatus.COMPLETED,
                AppointmentStatus.NO_SHOW
            ) // Exclude only CANCELLED appointments
            
            appointmentTimeMatches && statusIsActive
        }

        return if (hasAppointment) TimeSlotStatus.BOOKED else TimeSlotStatus.AVAILABLE
    }

    /**
     * Initializes current week based on today's date
     * Memory efficient: computed week generation
     * Always centers the week around today to ensure visibility
     */
    private fun initializeCurrentWeek() {
        val today = LocalDate.now()
        
        // Always center the week around today - 3 days before, today, 3 days after
        val startDay = today.minusDays(3)
        val weekDays = (0..6).map { startDay.plusDays(it.toLong()) }
        
        // Also initialize monthly calendar
        val monthDays = generateMonthCalendar(today)
        
        _uiState.value = _uiState.value.copy(
            selectedDate = today,
            selectedMonth = today.monthValue,
            selectedYear = today.year,
            currentWeekDays = weekDays,
            currentMonthDays = monthDays
        )
    }

    /**
     * Updates current week days based on given date
     * Memory efficient: week computation around given date
     * Ensures the reference date is always visible in the week
     */
    private fun updateCurrentWeek(referenceDate: LocalDate) {
        // Calculate Monday-based week
        val startOfWeek = referenceDate.minusDays(referenceDate.dayOfWeek.value.toLong() - 1)
        val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

        // Verify reference date is in the week - if not, recalculate
        val finalWeekDays = if (weekDays.contains(referenceDate)) {
            weekDays
        } else {
            // If reference date is not in calculated week, center the week around it
            val startDay = referenceDate.minusDays(3) // Show 3 days before reference
            (0..6).map { startDay.plusDays(it.toLong()) }
        }

        _uiState.value = _uiState.value.copy(currentWeekDays = finalWeekDays)
    }

    /**
     * Updates current month days based on given date
     * Memory efficient: monthly calendar computation with 6x7 grid
     */
    private fun updateCurrentMonth(referenceDate: LocalDate) {
        val monthDays = generateMonthCalendar(referenceDate)
        _uiState.value = _uiState.value.copy(currentMonthDays = monthDays)
    }

    /**
     * Generates 42-day calendar grid for given date's month
     * Memory efficient: computed monthly calendar (6 weeks * 7 days)
     */
    private fun generateMonthCalendar(referenceDate: LocalDate): List<LocalDate> {
        val firstDayOfMonth = referenceDate.withDayOfMonth(1)
        // Start from Monday of the week containing the first day of month
        val startOfCalendar = firstDayOfMonth.minusDays(firstDayOfMonth.dayOfWeek.value.toLong() - 1)
        return (0..41).map { startOfCalendar.plusDays(it.toLong()) }
    }

    /**
     * Updates selected date data (working status, appointments)
     * Memory efficient: targeted data update for selected date
     */
    private fun updateSelectedDateData() {
        updateAppointmentsForSelectedDate()
    }

    /**
     * Updates appointments for the selected date
     * Memory efficient: filtered appointment list
     */
    private fun updateAppointmentsForSelectedDate() {
        val selectedDate = _uiState.value.selectedDate
        val allAppointments = _uiState.value.appointments
        val dateString = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        val appointmentsForDate = allAppointments.filter { appointment ->
            appointment.appointmentDate == dateString
        }

        _uiState.value = _uiState.value.copy(appointmentsForSelectedDate = appointmentsForDate)
    }

    /**
     * Starts network monitoring for sync management
     * Memory efficient: single Flow subscription
     */
    private fun startNetworkMonitoring() {
        networkMonitorJob = viewModelScope.launch {
            networkMonitor.isConnected.collect { isOnline ->
                _uiState.value = _uiState.value.copy(isOnline = isOnline)
            }
        }
    }

    /**
     * Performs initial sync if needed (matching AppointmentViewModel behavior)
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
                println("Debug: CalendarViewModel - Background sync failed: ${e.message}")
            }
        }
    }

    /**
     * Clears messages after delay
     * Memory efficient: single coroutine with delay
     */
    private fun clearMessageAfterDelay() {
        viewModelScope.launch {
            delay(3000) // 3 seconds
            _uiState.value = _uiState.value.copy(
                successMessage = null,
                errorMessage = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitorJob?.cancel()
    }
}