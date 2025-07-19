package com.example.beautydate.viewmodels.state

import com.example.beautydate.data.models.Service
import com.example.beautydate.data.models.ServiceCategory

/**
 * UI state for service management screens
 * Holds all state needed for service listing and management
 * Memory efficient: immutable data class with computed properties
 */
data class ServiceUiState(
    val services: List<Service> = emptyList(),
    val filteredServices: List<Service> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: ServiceCategory? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,
    val isOnline: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val totalServices: Int = 0,
    val selectedService: Service? = null,
    val showAddServiceSheet: Boolean = false,
    val isSyncing: Boolean = false,
    // Bulk operations state
    val showBulkUpdateSheet: Boolean = false,
    val selectedServices: Set<String> = emptySet(),
    val isInSelectionMode: Boolean = false
) {
    /**
     * Returns services to display based on search query and category filter
     * Memory efficient: computed property with lazy evaluation
     */
    val displayServices: List<Service>
        get() {
            val baseList = if (searchQuery.isBlank()) services else filteredServices
            return if (selectedCategory != null) {
                baseList.filter { it.category == selectedCategory }
            } else {
                baseList
            }
        }
        
    /**
     * Returns true if no services are available
     */
    val isEmpty: Boolean
        get() = services.isEmpty() && !isLoading
        
    /**
     * Returns true if search results are empty but there are services
     */
    val isSearchEmpty: Boolean
        get() = searchQuery.isNotBlank() && filteredServices.isEmpty() && services.isNotEmpty()
        
    /**
     * Returns true if category filter results are empty but there are services
     */
    val isCategoryEmpty: Boolean
        get() = selectedCategory != null && displayServices.isEmpty() && services.isNotEmpty()
        
    /**
     * Returns services grouped by category for display
     * Memory efficient: groupBy with minimal object creation
     */
    val servicesByCategory: Map<ServiceCategory, List<Service>>
        get() = displayServices.groupBy { it.category }
        
    /**
     * Returns total selected services count for bulk operations
     */
    val selectedCount: Int
        get() = selectedServices.size
        
    /**
     * Returns true if all visible services are selected
     */
    val isAllSelected: Boolean
        get() = displayServices.isNotEmpty() && 
                displayServices.all { it.id in selectedServices }
                
    /**
     * Returns price statistics for current displayed services
     * Memory efficient: computed on-demand
     */
    val priceStats: PriceStats
        get() {
            val prices = displayServices.map { it.price }
            return PriceStats(
                minPrice = prices.minOrNull() ?: 0.0,
                maxPrice = prices.maxOrNull() ?: 0.0,
                averagePrice = if (prices.isNotEmpty()) prices.average() else 0.0,
                totalServices = prices.size
            )
        }
}

/**
 * Price statistics data class
 * Memory efficient: simple data holder
 */
data class PriceStats(
    val minPrice: Double,
    val maxPrice: Double,
    val averagePrice: Double,
    val totalServices: Int
) {
    /**
     * Returns formatted minimum price
     */
    val formattedMinPrice: String
        get() = "${minPrice.toInt()} ₺"
        
    /**
     * Returns formatted maximum price
     */
    val formattedMaxPrice: String
        get() = "${maxPrice.toInt()} ₺"
        
    /**
     * Returns formatted average price
     */
    val formattedAveragePrice: String
        get() = "${averagePrice.toInt()} ₺"
} 