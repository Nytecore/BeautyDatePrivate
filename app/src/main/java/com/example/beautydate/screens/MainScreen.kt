package com.example.beautydate.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.beautydate.components.BottomNavigationBar
import com.example.beautydate.navigation.BottomNavigationItem
import com.example.beautydate.navigation.NavigationRoutes
import com.example.beautydate.viewmodels.AuthViewModel
import com.example.beautydate.viewmodels.CustomerViewModel
import com.example.beautydate.screens.CalendarScreen

/**
 * Main screen with bottom navigation
 * Contains bottom navigation bar and navigation host for tab content
 * Memory efficient: reuses CustomerViewModel for navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToProfileSettings: () -> Unit,
    mainNavController: NavHostController? = null,
    customerViewModel: CustomerViewModel? = null,
    employeeViewModel: com.example.beautydate.viewmodels.EmployeeViewModel? = null,
    navController: NavHostController = rememberNavController(),
    initialTab: String = "ziyaretler"
) {
    // Memory efficient: use passed CustomerViewModel or create new one
    val custViewModel = customerViewModel ?: hiltViewModel<CustomerViewModel>()
    val empViewModel = employeeViewModel ?: hiltViewModel<com.example.beautydate.viewmodels.EmployeeViewModel>()
    
    // Determine start destination based on initialTab parameter
    val startDestination = when (initialTab) {
        "musteriler" -> NavigationRoutes.MUSTERILER
        "ziyaretler" -> NavigationRoutes.ZIYARETLER
        "operations" -> NavigationRoutes.OPERATIONS  // Updated: "islemler" → "operations"
        "diger" -> NavigationRoutes.DIGER
        else -> NavigationRoutes.RANDEVULAR // default
    }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Randevular tab (Takvim ekranı)
            composable(NavigationRoutes.RANDEVULAR) {
                CalendarScreen(
                    onNavigateToAddAppointment = { date, time ->
                        mainNavController?.navigate("${NavigationRoutes.ADD_APPOINTMENT}?date=$date&time=$time")
                    },
                    onNavigateToAppointmentDetail = { appointmentId ->
                        mainNavController?.navigate("${NavigationRoutes.APPOINTMENT_DETAIL}/$appointmentId")
                    },
                    onNavigateToWorkingHours = {
                        mainNavController?.navigate(NavigationRoutes.WORKING_HOURS)
                    }
                )
            }
            
            // Ziyaretler tab (randevu sistemi burada)
            composable(NavigationRoutes.ZIYARETLER) {
                AppointmentsScreen(
                    onNavigateToAddAppointment = {
                        mainNavController?.navigate(NavigationRoutes.ADD_APPOINTMENT)
                    },
                    onNavigateToEditAppointment = {
                        mainNavController?.navigate(NavigationRoutes.EDIT_APPOINTMENT)
                    }
                )
            }
            
            // İşlemler tab - Updated from YeniScreen to OperationsScreen
            composable(NavigationRoutes.OPERATIONS) {
                OperationsScreen(
                    onNavigateToAddCustomer = {
                        mainNavController?.navigate("${NavigationRoutes.ADD_CUSTOMER}?source=operations")
                    },
                    onNavigateToAddAppointment = {
                        mainNavController?.navigate(NavigationRoutes.ADD_APPOINTMENT)
                    },
                    onNavigateToEmployeeList = {
                        mainNavController?.navigate(NavigationRoutes.EMPLOYEE_LIST)
                    },
                    onNavigateToAddEmployee = {
                        mainNavController?.navigate(NavigationRoutes.ADD_EMPLOYEE)
                    },
                    onNavigateToServiceList = {
                        mainNavController?.navigate(NavigationRoutes.SERVICE_LIST)
                    },
                    onNavigateToAddService = {
                        mainNavController?.navigate(NavigationRoutes.ADD_SERVICE)
                    },
                    onNavigateToWorkingHours = {
                        mainNavController?.navigate(NavigationRoutes.WORKING_HOURS)
                    },
                    onNavigateToBusinessExpenses = {
                        mainNavController?.navigate(NavigationRoutes.BUSINESS_EXPENSES)
                    },
                    onNavigateToPriceUpdate = {
                        mainNavController?.navigate(NavigationRoutes.PRICE_UPDATE)
                    },
                    onNavigateToCustomerNotes = {
                        mainNavController?.navigate(NavigationRoutes.CUSTOMER_NOTES)
                    },
                    onNavigateToAppointments = {
                        mainNavController?.navigate(NavigationRoutes.APPOINTMENTS)
                    },
                    onNavigateToCalendar = {
                        // Navigate to Calendar (Randevular) tab from Operations
                        navController.navigate(NavigationRoutes.RANDEVULAR)
                    },
                    employeeViewModel = empViewModel
                )
            }
            
            // Müşteriler tab
            composable(NavigationRoutes.MUSTERILER) {
                MusterilerScreen(
                    onNavigateToCustomerDetail = { customer ->
                        // Memory efficient: select customer in ViewModel, then navigate
                        custViewModel.selectCustomer(customer)
                        mainNavController?.let { navController ->
                            // Pass customerViewModel to AppNavigation for proper state management
                            navController.navigate(NavigationRoutes.CUSTOMER_DETAIL)
                        }
                    },
                    onNavigateToAddCustomer = {
                        mainNavController?.navigate("${NavigationRoutes.ADD_CUSTOMER}?source=musteriler")
                    },
                    onNavigateToEditCustomer = { customer ->
                        // Memory efficient: select customer in ViewModel, then navigate
                        custViewModel.selectCustomer(customer)
                        mainNavController?.let { navController ->
                            navController.navigate(NavigationRoutes.EDIT_CUSTOMER)
                        }
                    },
                    customerViewModel = custViewModel
                )
            }
            
            // Diğer tab
            composable(NavigationRoutes.DIGER) {
                DigerScreen(
                    onNavigateToProfileSettings = onNavigateToProfileSettings,
                    onNavigateToTheme = { 
                        mainNavController?.navigate(NavigationRoutes.THEME) 
                    },
                    onNavigateToFeedback = { 
                        mainNavController?.navigate(NavigationRoutes.FEEDBACK) 
                    },
                    onNavigateToFinance = { 
                        mainNavController?.navigate(NavigationRoutes.FINANCE) 
                    },
                    onNavigateToStatistics = { 
                        mainNavController?.navigate(NavigationRoutes.STATISTICS) 
                    },
                    onNavigateToTutorial = { 
                        mainNavController?.navigate(NavigationRoutes.TUTORIAL) 
                    },
                    onNavigateToLogin = onNavigateToLogin
                )
            }
        }
    }
} 