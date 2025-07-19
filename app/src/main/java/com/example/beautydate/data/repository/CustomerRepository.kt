package com.example.beautydate.data.repository

import com.example.beautydate.data.local.CustomerDao
import com.example.beautydate.data.local.CustomerEntity
import com.example.beautydate.data.models.Customer
import com.example.beautydate.data.models.CustomerFirestore
import com.example.beautydate.utils.NetworkMonitor
import com.example.beautydate.utils.AuthUtil
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for customer management
 * Follows Repository pattern and Interface Segregation Principle
 * Multi-tenant architecture: All operations use authenticated businessId
 */
interface CustomerRepository {
    
    /**
     * Gets all customers for current authenticated business as Flow (offline-first)
     * BusinessId is automatically retrieved from current authenticated user
     */
    fun getAllCustomers(): Flow<List<Customer>>
    
    /**
     * Searches customers by query (name or phone) for current business
     * BusinessId filtering is handled automatically
     */
    fun searchCustomers(query: String): Flow<List<Customer>>
    
    /**
     * Gets a specific customer by ID (with businessId validation)
     * Ensures customer belongs to current authenticated business
     */
    suspend fun getCustomerById(customerId: String): Customer?
    
    /**
     * Adds a new customer (businessId added automatically)
     * BusinessId is set from current authenticated user
     */
    suspend fun addCustomer(customer: Customer): Result<Customer>
    
    /**
     * Updates an existing customer (with businessId validation)
     * Ensures customer belongs to current authenticated business
     */
    suspend fun updateCustomer(customer: Customer): Result<Customer>
    
    /**
     * Deletes a customer (with businessId validation)
     * Ensures customer belongs to current authenticated business
     */
    suspend fun deleteCustomer(customerId: String): Result<Unit>
    
    /**
     * Manually syncs customers with Firestore for current business
     * BusinessId filtering applied automatically
     */
    suspend fun syncWithFirestore(): Result<Unit>
    
    /**
     * Performs initial sync when app starts (downloads Firestore data to local DB)
     * Only syncs data for current authenticated business
     */
    suspend fun performInitialSync(): Result<Unit>
    
    /**
     * Performs comprehensive bidirectional sync (upload + download)
     * BusinessId filtering applied automatically
     */
    suspend fun performComprehensiveSync(): Result<Unit>
    
    /**
     * Checks if phone number already exists for current business
     * BusinessId filtering applied automatically
     */
    suspend fun phoneNumberExists(phoneNumber: String, excludeCustomerId: String = ""): Boolean
    
    /**
     * Gets total customer count for current business
     * BusinessId filtering applied automatically
     */
    suspend fun getCustomerCount(): Int
}

/**
 * Implementation of CustomerRepository with offline-first approach
 * Uses Room for local storage and Firestore for remote sync
 * Multi-tenant architecture: All data operations filtered by businessId automatically
 */
@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val networkMonitor: NetworkMonitor,
    private val authUtil: AuthUtil
) : CustomerRepository {
    
    companion object {
        private const val CUSTOMERS_COLLECTION = "customers"
    }
    
    /**
     * Gets all customers as Flow from local database (offline-first)
     * BusinessId filtering applied automatically from authenticated user
     */
    override fun getAllCustomers(): Flow<List<Customer>> {
        val businessId = authUtil.getCurrentBusinessIdSafe()
        return customerDao.getAllCustomers(businessId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    /**
     * Searches customers by query from local database
     * BusinessId filtering applied automatically from authenticated user
     */
    override fun searchCustomers(query: String): Flow<List<Customer>> {
        val businessId = authUtil.getCurrentBusinessIdSafe()
        return if (query.isBlank()) {
            getAllCustomers()
        } else {
            customerDao.searchCustomers(businessId, query)
                .map { entities -> entities.map { it.toDomainModel() } }
        }
    }
    
    /**
     * Gets customer by ID from local database with businessId validation
     * Ensures customer belongs to current authenticated business
     */
    override suspend fun getCustomerById(customerId: String): Customer? {
        val businessId = authUtil.getCurrentBusinessId() ?: return null
        val customer = customerDao.getCustomerById(customerId)?.toDomainModel()
        
        // Validate that customer belongs to current business
        return if (customer?.businessId == businessId) {
            customer
        } else {
            null // Customer doesn't belong to this business
        }
    }
    
    /**
     * Adds new customer to local database and triggers automatic sync
     * BusinessId is automatically set from current authenticated user
     */
    override suspend fun addCustomer(customer: Customer): Result<Customer> {
        return try {
            // Get current business ID from authenticated user
            val businessId = authUtil.getCurrentBusinessId()
                ?: return Result.failure(Exception(authUtil.getAuthErrorMessage()))
            
            println("Debug: Starting addCustomer process for: ${customer.firstName} ${customer.lastName}")
            
            // Automatically set businessId for new customer
            val newCustomer = customer.copy(
                id = if (customer.id.isEmpty()) Customer.generateCustomerId() else customer.id,
                fileNumber = if (customer.fileNumber.isEmpty()) Customer.generateFileNumber() else customer.fileNumber,
                businessId = businessId, // Automatically set from authenticated user
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            println("Debug: Generated customer ID: ${newCustomer.id}, FileNumber: ${newCustomer.fileNumber}, BusinessId: $businessId")
            
            val entity = CustomerEntity.fromDomainModel(newCustomer, needsSync = true)
            customerDao.insertCustomer(entity)
            
            println("Debug: Customer inserted to local database successfully")
            
            // Automatic background sync if network available
            if (networkMonitor.isCurrentlyConnected()) {
                try {
                    syncCustomerToFirestore(newCustomer)
                    customerDao.markAsSynced(newCustomer.id)
                    println("Debug: Customer synced to Firestore successfully")
                } catch (syncError: Exception) {
                    println("Debug: Firestore sync failed, will retry later: ${syncError.message}")
                    // Don't fail the operation - will sync when network is available
                }
            }
            
            Result.success(newCustomer)
        } catch (e: Exception) {
            println("Debug: Error adding customer: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Updates an existing customer with businessId validation
     * Ensures customer belongs to current authenticated business
     */
    override suspend fun updateCustomer(customer: Customer): Result<Customer> {
        return try {
            val businessId = authUtil.getCurrentBusinessId()
                ?: return Result.failure(Exception(authUtil.getAuthErrorMessage()))
            
            // Validate that customer belongs to current business
            if (customer.businessId != businessId) {
                return Result.failure(Exception(authUtil.getTenantErrorMessage()))
            }
            
            val updatedCustomer = customer.copy(updatedAt = Timestamp.now())
            val entity = CustomerEntity.fromDomainModel(updatedCustomer, needsSync = true)
            
            customerDao.updateCustomer(entity)
            
            // Automatic background sync if network available
            if (networkMonitor.isCurrentlyConnected()) {
                try {
                    syncCustomerToFirestore(updatedCustomer)
                    customerDao.markAsSynced(updatedCustomer.id)
                } catch (syncError: Exception) {
                    println("Debug: Firestore sync failed, will retry later: ${syncError.message}")
                }
            }
            
            Result.success(updatedCustomer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deletes a customer with businessId validation
     * Ensures customer belongs to current authenticated business
     */
    override suspend fun deleteCustomer(customerId: String): Result<Unit> {
        return try {
            val businessId = authUtil.getCurrentBusinessId()
                ?: return Result.failure(Exception(authUtil.getAuthErrorMessage()))
            
            // Get customer to validate businessId
            val customer = customerDao.getCustomerById(customerId)?.toDomainModel()
            if (customer?.businessId != businessId) {
                return Result.failure(Exception(authUtil.getTenantErrorMessage()))
            }
            
            // Mark as deleted (soft delete) and sync
            customerDao.markAsDeleted(customerId)
            
            if (networkMonitor.isCurrentlyConnected()) {
                try {
                    firestore.collection(CUSTOMERS_COLLECTION)
                        .document(customerId)
                        .delete()
                        .await()
                    
                    // Hard delete locally after successful Firestore deletion
                    customerDao.hardDeleteCustomer(customerId)
                } catch (syncError: Exception) {
                    // Will be synced later when network is available
                    println("Debug: Firestore delete failed, will retry later: ${syncError.message}")
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Manually syncs customers with Firestore for current business
     * BusinessId filtering applied automatically
     */
    override suspend fun syncWithFirestore(): Result<Unit> {
        return try {
            val businessId = authUtil.getCurrentBusinessId()
                ?: return Result.failure(Exception(authUtil.getAuthErrorMessage()))
            
            if (!networkMonitor.isCurrentlyConnected()) {
                return Result.failure(Exception("No network connection"))
            }
            
            // Get local customers needing sync for current business
            val localCustomers = customerDao.getCustomersNeedingSync(businessId)
            
            // Upload local changes to Firestore
            localCustomers.forEach { entity ->
                val customer = entity.toDomainModel()
                syncCustomerToFirestore(customer)
                customerDao.markAsSynced(customer.id)
            }
            
            // Download remote customers for current business only
            val remoteCustomers = firestore.collection(CUSTOMERS_COLLECTION)
                .whereEqualTo("businessId", businessId) // BusinessId filtering
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val remoteEntities = remoteCustomers.documents.mapNotNull { doc ->
                doc.toObject(CustomerFirestore::class.java)?.let { firestore ->
                    CustomerEntity.fromDomainModel(firestore.toDomainModel(), needsSync = false)
                }
            }
            
            // Save remote customers to local database
            if (remoteEntities.isNotEmpty()) {
                customerDao.insertCustomers(remoteEntities)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Performs initial sync when app starts for current business
     * Only downloads data belonging to current authenticated business
     */
    override suspend fun performInitialSync(): Result<Unit> {
        return try {
            val businessId = authUtil.getCurrentBusinessId()
                ?: return Result.failure(Exception(authUtil.getAuthErrorMessage()))
            
            // Check if we have any customers locally for this business
            val localCount = customerDao.getCustomerCount(businessId)
            
            if (localCount == 0) {
                // No local data, perform full sync for current business
                println("Debug: No local customers found for business $businessId, performing initial sync...")
                syncWithFirestore()
            } else {
                println("Debug: Local customers found ($localCount), skipping initial sync")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            println("Debug: Initial sync failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Performs comprehensive bidirectional sync for current business
     * BusinessId filtering applied automatically
     */
    override suspend fun performComprehensiveSync(): Result<Unit> {
        return syncWithFirestore() // Same implementation with businessId filtering
    }
    
    /**
     * Checks if phone number already exists for current business
     * BusinessId filtering applied automatically
     */
    override suspend fun phoneNumberExists(phoneNumber: String, excludeCustomerId: String): Boolean {
        val businessId = authUtil.getCurrentBusinessIdSafe()
        return customerDao.phoneNumberExists(businessId, phoneNumber, excludeCustomerId)
    }
    
    /**
     * Gets total customer count for current business
     * BusinessId filtering applied automatically
     */
    override suspend fun getCustomerCount(): Int {
        val businessId = authUtil.getCurrentBusinessIdSafe()
        return customerDao.getCustomerCount(businessId)
    }
    
    /**
     * Private helper: Syncs single customer to Firestore with businessId
     * Used internally for upload operations
     */
    private suspend fun syncCustomerToFirestore(customer: Customer) {
        val firestoreCustomer = CustomerFirestore.fromDomainModel(
            customer, 
            lastModifiedBy = authUtil.getCurrentBusinessIdSafe()
        )
        
        firestore.collection(CUSTOMERS_COLLECTION)
            .document(customer.id)
            .set(firestoreCustomer)
            .await()
    }
} 