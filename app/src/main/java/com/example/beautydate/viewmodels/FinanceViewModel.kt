package com.example.beautydate.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beautydate.data.models.Payment
import com.example.beautydate.data.models.PaymentMethod
import com.example.beautydate.data.models.PaymentStatus
import com.example.beautydate.data.models.Transaction
import com.example.beautydate.data.models.TransactionType
import com.example.beautydate.data.models.TransactionCategory
import com.example.beautydate.data.models.AppointmentStatus
import com.example.beautydate.data.models.Expense
import com.example.beautydate.data.repository.AppointmentRepository
import com.example.beautydate.data.repository.ExpenseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

/**
 * Enhanced Finance ViewModel with Income/Expense separation
 * Integrates with AppointmentRepository for income and ExpenseRepository for expenses
 * Memory efficient: Flow-based reactive data with calculated properties and monthly filtering
 */
@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val appointmentRepository: AppointmentRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private val businessId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    init {
        loadFinanceData()
    }

    /**
     * Loads comprehensive finance data from appointments and expenses
     * Memory efficient: Combine flows for reactive updates
     */
    private fun loadFinanceData() {
        if (businessId.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Combine appointments and expenses data
                combine(
                    appointmentRepository.getAllAppointments(businessId),
                    expenseRepository.getAllExpenses(businessId)
                ) { appointments, expenses ->
                    Pair(appointments, expenses)
                }.catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Finans verileri yüklenirken hata oluştu: ${exception.message}"
                    )
                }.collect { (appointments, expenses) ->
                    processFinanceData(appointments, expenses)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Finans verileri yüklenirken hata oluştu: ${e.message}"
                )
            }
        }
    }

    /**
     * Processes combined finance data and updates UI state
     * Memory efficient: single pass calculation with monthly totals
     */
    private fun processFinanceData(appointments: List<com.example.beautydate.data.models.Appointment>, expenses: List<Expense>) {
        // Filter completed appointments for financial calculations
        val completedAppointments = appointments.filter { 
            it.status == AppointmentStatus.COMPLETED 
        }
        
        // Convert appointments to payments (assuming all completed appointments are paid)
        val payments = completedAppointments.map { appointment ->
            Payment(
                id = Payment.generatePaymentId(),
                appointmentId = appointment.id,
                customerId = appointment.customerId,
                customerName = appointment.customerName,
                serviceName = appointment.serviceName,
                amount = appointment.servicePrice,
                paymentMethod = PaymentMethod.CASH, // Default - in real app would be stored
                status = PaymentStatus.COMPLETED,
                createdAt = appointment.updatedAt ?: appointment.createdAt ?: Timestamp.now(),
                businessId = appointment.businessId
            )
        }
        
        // Convert payments to income transactions
        val incomeTransactions = payments.map { payment ->
            Transaction(
                id = Transaction.generateTransactionId(),
                type = TransactionType.INCOME,
                category = TransactionCategory.SERVICE,
                amount = payment.amount,
                description = "${payment.customerName} - ${payment.serviceName}",
                paymentId = payment.id,
                createdAt = payment.createdAt ?: Timestamp.now(),
                updatedAt = payment.createdAt ?: Timestamp.now(),
                businessId = payment.businessId
            )
        }
        
        // Convert expenses to expense transactions
        val expenseTransactions = expenses.map { expense ->
            Transaction(
                id = Transaction.generateTransactionId(),
                type = TransactionType.EXPENSE,
                category = TransactionCategory.OTHER,
                amount = expense.amount,
                description = expense.description,
                paymentId = "", // No payment ID for expenses
                createdAt = expense.createdAt ?: Timestamp.now(),
                updatedAt = expense.updatedAt ?: Timestamp.now(),
                businessId = expense.businessId
            )
        }
        
        // Combine all transactions
        val allTransactions = incomeTransactions + expenseTransactions
        
        // Calculate financial totals
        val totals = calculateFinancialTotals(payments, expenses)
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            payments = payments,
            transactions = allTransactions,
            expenses = expenses,
            totalRevenue = totals.totalRevenue,
            totalCash = totals.totalCash,
            totalCredit = totals.totalCredit,
            totalExpenses = totals.totalExpenses,
            netProfit = totals.netProfit,
            errorMessage = null
        )
    }

    /**
     * Refreshes finance data with sync
     * Memory efficient: reloads data with sync
     */
    fun refreshFinanceData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            
            try {
                // Sync appointments and expenses with Firestore
                appointmentRepository.syncWithFirestore(businessId)
                expenseRepository.syncWithFirestore(businessId)
                
                // Reload data after sync
                loadFinanceData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = "Senkronizasyon hatası: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isSyncing = false)
            }
        }
    }

    /**
     * Filters data by period (monthly filtering)
     * Memory efficient: computed filtering without data duplication
     */
    fun filterByPeriod(period: String) {
        // Period filtering is handled in UI layer for better performance
        // This method can be extended for server-side filtering if needed
    }

    /**
     * Calculates comprehensive financial totals including expenses
     * Memory efficient: single pass calculation with all metrics
     */
    private fun calculateFinancialTotals(payments: List<Payment>, expenses: List<Expense>): FinancialTotals {
        val completedPayments = payments.filter { it.status == PaymentStatus.COMPLETED }
        
        val totalRevenue = completedPayments.sumOf { it.amount }
        val totalCash = completedPayments
            .filter { it.paymentMethod == PaymentMethod.CASH }
            .sumOf { it.amount }
        val totalCredit = completedPayments
            .filter { it.paymentMethod == PaymentMethod.CREDIT_CARD }
            .sumOf { it.amount }
        
        val totalExpenses = expenses.sumOf { it.amount }
        val netProfit = totalRevenue - totalExpenses
        
        return FinancialTotals(
            totalRevenue = totalRevenue,
            totalCash = totalCash,
            totalCredit = totalCredit,
            totalExpenses = totalExpenses,
            netProfit = netProfit
        )
    }

    /**
     * Gets expenses for specific month
     * Business logic: monthly expense filtering
     */
    fun getExpensesForMonth(monthKey: String): List<Expense> {
        return if (monthKey == "Tümü") {
            _uiState.value.expenses
        } else {
            _uiState.value.expenses.filter { expense ->
                getMonthKeyFromExpense(expense) == monthKey
            }
        }
    }

    /**
     * Gets monthly expense total
     * Business logic: monthly expense calculation
     */
    fun getMonthlyExpenseTotal(monthKey: String): Double {
        return getExpensesForMonth(monthKey).sumOf { it.amount }
    }

    /**
     * Helper function to get month key from expense
     */
    private fun getMonthKeyFromExpense(expense: Expense): String {
        return try {
            val date = expense.createdAt?.toDate()?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
            date?.let {
                val monthName = it.month.getDisplayName(TextStyle.FULL, Locale("tr"))
                "$monthName ${it.year}"
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Clears error message
     */
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

/**
 * Data class for financial totals calculation
 */
private data class FinancialTotals(
    val totalRevenue: Double,
    val totalCash: Double,
    val totalCredit: Double,
    val totalExpenses: Double,
    val netProfit: Double
)

/**
 * Enhanced Finance UI State with expense integration
 * Memory efficient: immutable data class with computed properties
 */
data class FinanceUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val payments: List<Payment> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val totalRevenue: Double = 0.0,
    val totalCash: Double = 0.0,
    val totalCredit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val errorMessage: String? = null
) {
    /**
     * Computed property for formatted total revenue
     */
    val formattedTotalRevenue: String
        get() = "${totalRevenue.toInt()} ₺"
    
    /**
     * Computed property for formatted net profit
     */
    val formattedNetProfit: String
        get() = "${netProfit.toInt()} ₺"
    
    /**
     * Computed property for formatted total expenses
     */
    val formattedTotalExpenses: String
        get() = "${totalExpenses.toInt()} ₺"
} 