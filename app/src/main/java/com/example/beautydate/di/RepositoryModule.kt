package com.example.beautydate.di

import com.example.beautydate.data.repository.FeedbackRepository
import com.example.beautydate.data.repository.FeedbackRepositoryImpl
import com.example.beautydate.data.repository.ThemeRepository
import com.example.beautydate.data.repository.ThemeRepositoryImpl
import com.example.beautydate.data.repository.CustomerRepository
import com.example.beautydate.data.repository.CustomerRepositoryImpl
import com.example.beautydate.data.repository.ServiceRepository
import com.example.beautydate.data.repository.ServiceRepositoryImpl
import com.example.beautydate.data.repository.EmployeeRepository
import com.example.beautydate.data.repository.EmployeeRepositoryImpl
import com.example.beautydate.data.repository.CustomerNoteRepository
import com.example.beautydate.data.repository.CustomerNoteRepositoryImpl
import com.example.beautydate.data.repository.AppointmentRepository
import com.example.beautydate.data.repository.AppointmentRepositoryImpl
import com.example.beautydate.data.repository.WorkingHoursRepository
import com.example.beautydate.data.repository.WorkingHoursRepositoryImpl
import com.example.beautydate.data.repository.PaymentRepository
import com.example.beautydate.data.repository.PaymentRepositoryImpl
import com.example.beautydate.data.repository.TransactionRepository
import com.example.beautydate.data.repository.TransactionRepositoryImpl
import com.example.beautydate.data.repository.ExpenseRepository
import com.example.beautydate.data.repository.ExpenseRepositoryImpl
import com.example.beautydate.data.repository.StatisticsRepository
import com.example.beautydate.data.repository.StatisticsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository bindings
 * Separates repository interfaces from their implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds FeedbackRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindFeedbackRepository(
        feedbackRepositoryImpl: FeedbackRepositoryImpl
    ): FeedbackRepository

    /**
     * Binds ThemeRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        themeRepositoryImpl: ThemeRepositoryImpl
    ): ThemeRepository

    /**
     * Binds CustomerRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindCustomerRepository(
        customerRepositoryImpl: CustomerRepositoryImpl
    ): CustomerRepository

    /**
     * Binds ServiceRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindServiceRepository(
        serviceRepositoryImpl: ServiceRepositoryImpl
    ): ServiceRepository

    /**
     * Binds EmployeeRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindEmployeeRepository(
        employeeRepositoryImpl: EmployeeRepositoryImpl
    ): EmployeeRepository

    /**
     * Binds CustomerNoteRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindCustomerNoteRepository(
        customerNoteRepositoryImpl: CustomerNoteRepositoryImpl
    ): CustomerNoteRepository

    /**
     * Binds AppointmentRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindAppointmentRepository(
        appointmentRepositoryImpl: AppointmentRepositoryImpl
    ): AppointmentRepository

    /**
     * Binds WorkingHoursRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindWorkingHoursRepository(
        workingHoursRepositoryImpl: WorkingHoursRepositoryImpl
    ): WorkingHoursRepository

    /**
     * Binds PaymentRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        paymentRepositoryImpl: PaymentRepositoryImpl
    ): PaymentRepository

    /**
     * Binds TransactionRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    /**
     * Binds StatisticsRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindStatisticsRepository(
        statisticsRepositoryImpl: StatisticsRepositoryImpl
    ): StatisticsRepository

    /**
     * Binds TutorialRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindTutorialRepository(
        tutorialRepositoryImpl: com.example.beautydate.data.repository.TutorialRepositoryImpl
    ): com.example.beautydate.data.repository.TutorialRepository

    /**
     * Binds ExpenseRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        expenseRepositoryImpl: ExpenseRepositoryImpl
    ): ExpenseRepository
} 