package com.example.beautydate.data.repository

import com.example.beautydate.data.local.dao.ExpenseDao
import com.example.beautydate.data.local.entities.ExpenseEntity
import com.example.beautydate.data.models.Expense
import com.example.beautydate.data.models.ExpenseCategory
import com.example.beautydate.data.models.ExpenseFirestore
import com.example.beautydate.utils.NetworkMonitor
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ExpenseRepository with offline-first approach
 * Uses Room for local storage and Firestore for remote sync
 * Memory efficient: Flow-based reactive data and minimal object creation
 */
@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val networkMonitor: NetworkMonitor
) : ExpenseRepository {

    companion object {
        private const val EXPENSES_COLLECTION = "expenses"
    }

    override fun getAllExpenses(businessId: String): Flow<List<Expense>> {
        return expenseDao.getAllExpenses(businessId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getExpensesByCategory(category: ExpenseCategory, businessId: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByCategory(category.name, businessId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getExpenseById(id: String, businessId: String): Expense? {
        return expenseDao.getExpenseById(id, businessId)?.toDomainModel()
    }

    override suspend fun getTotalExpenses(businessId: String): Double {
        return expenseDao.getTotalExpenses(businessId)
    }

    override suspend fun getExpenseCount(businessId: String): Int {
        return expenseDao.getExpenseCount(businessId)
    }

    override suspend fun getAmountByCategory(category: ExpenseCategory, businessId: String): Double {
        return expenseDao.getAmountByCategory(category.name, businessId)
    }

    override suspend fun addExpense(expense: Expense): Result<String> {
        return try {
            val newExpense = expense.copy(
                id = if (expense.id.isEmpty()) Expense.generateExpenseId() else expense.id,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            val entity = ExpenseEntity.fromDomainModel(newExpense, needsSync = true)
            expenseDao.insertExpense(entity)

            // Trigger automatic sync to Firebase (only if online)
            if (networkMonitor.isCurrentlyConnected()) {
                try {
                    val currentUser = firebaseAuth.currentUser
                    val firestoreExpense = ExpenseFirestore.fromDomainModel(
                        expense = newExpense,
                        lastModifiedBy = currentUser?.uid ?: "unknown"
                    )

                    firestore.collection(EXPENSES_COLLECTION)
                        .document(newExpense.id)
                        .set(firestoreExpense)
                        .await()

                    expenseDao.markAsSynced(newExpense.id)
                } catch (syncError: Exception) {
                    // Don't fail the operation, just log for later sync
                }
            }

            Result.success(newExpense.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Expense> {
        return try {
            val updatedExpense = expense.copy(updatedAt = Timestamp.now())
            val entity = ExpenseEntity.fromDomainModel(updatedExpense, needsSync = true)
            expenseDao.updateExpense(entity)

            // Try immediate sync for updates
            if (networkMonitor.isCurrentlyConnected()) {
                try {
                    val currentUser = firebaseAuth.currentUser
                    val firestoreExpense = ExpenseFirestore.fromDomainModel(
                        expense = updatedExpense,
                        lastModifiedBy = currentUser?.uid ?: "unknown"
                    )

                    firestore.collection(EXPENSES_COLLECTION)
                        .document(updatedExpense.id)
                        .set(firestoreExpense)
                        .await()

                    expenseDao.markAsSynced(updatedExpense.id)
                } catch (syncError: Exception) {
                    // Log but don't fail
                }
            }

            Result.success(updatedExpense)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(id: String, businessId: String): Result<Unit> {
        return try {
            // Mark as deleted (soft delete) and sync
            expenseDao.markAsDeleted(id, businessId)

            if (networkMonitor.isCurrentlyConnected()) {
                try {
                    firestore.collection(EXPENSES_COLLECTION)
                        .document(id)
                        .delete()
                        .await()

                    // Hard delete locally after successful Firestore deletion
                    expenseDao.hardDeleteExpense(id, businessId)
                } catch (syncError: Exception) {
                    // Will be synced later
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun searchExpenses(query: String, businessId: String): Flow<List<Expense>> {
        return expenseDao.searchExpenses(query, businessId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun syncWithFirestore(businessId: String): Result<Unit> {
        return try {
            // TODO: Implement comprehensive sync when needed
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun performComprehensiveSync(businessId: String): Result<Unit> {
        return try {
            // TODO: Implement comprehensive sync when needed
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 