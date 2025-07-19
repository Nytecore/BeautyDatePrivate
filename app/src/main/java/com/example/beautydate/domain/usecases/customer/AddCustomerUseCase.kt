package com.example.beautydate.domain.usecases.customer

import com.example.beautydate.data.models.Customer
import com.example.beautydate.data.repository.CustomerRepository
import javax.inject.Inject

/**
 * Use case for adding new customers with validation
 * Follows Single Responsibility Principle and validates business rules
 */
class AddCustomerUseCase @Inject constructor(
    private val customerRepository: CustomerRepository
) {
    /**
     * Adds a new customer after validation
     * @param customer Customer to add
     * @return Result with customer or error
     */
    suspend operator fun invoke(customer: Customer): Result<Customer> {
        println("Debug: AddCustomerUseCase - Starting validation for customer: ${customer.firstName} ${customer.lastName}")
        println("Debug: Customer data - Phone: ${customer.phoneNumber}, Gender: ${customer.gender}, BirthDate: ${customer.birthDate}, BusinessId: ${customer.businessId}")
        
        // Validate customer data
        if (!customer.isValid()) {
            println("Debug: Customer validation failed")
            return Result.failure(IllegalArgumentException("Müşteri bilgileri eksik veya hatalı"))
        }
        
        println("Debug: Customer validation passed")
        
        // Check if phone number already exists
        val phoneExists = customerRepository.phoneNumberExists(
            phoneNumber = customer.phoneNumber,
            excludeCustomerId = "" // Empty since this is a new customer
        )
        
        if (phoneExists) {
            println("Debug: Phone number already exists: ${customer.phoneNumber}")
            return Result.failure(IllegalArgumentException("Bu telefon numarası zaten kayıtlı"))
        }
        
        println("Debug: Phone number check passed, adding customer to repository")
        
        // Add customer
        return customerRepository.addCustomer(customer)
    }
} 