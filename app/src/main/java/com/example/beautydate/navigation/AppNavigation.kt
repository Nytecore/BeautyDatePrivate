package com.example.beautydate.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseIn
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.beautydate.screens.*
import com.example.beautydate.viewmodels.AuthViewModel
import com.example.beautydate.viewmodels.CustomerViewModel
import com.example.beautydate.viewmodels.EmployeeViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.beautydate.screens.EditEmployeeScreen
import com.example.beautydate.screens.EditAppointmentScreen

/**
 * Navigation configuration for the BeautyDate app
 * Handles all screen transitions with smooth animations
 * Memory efficient: reuses ViewModels and parameter passing
 * Enhanced: Smoother transitions with improved easing curves
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    customerViewModel: CustomerViewModel? = null,
    employeeViewModel: com.example.beautydate.viewmodels.EmployeeViewModel? = null,
    appointmentViewModel: com.example.beautydate.viewmodels.AppointmentViewModel? = null,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> (fullWidth * 0.3f).toInt() },
                animationSpec = tween(400, easing = EaseOut)
            ) + fadeIn(
                animationSpec = tween(400, easing = EaseOut)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * 0.3f).toInt() },
                animationSpec = tween(400, easing = EaseIn)
            ) + fadeOut(
                animationSpec = tween(300, easing = EaseIn)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * 0.3f).toInt() },
                animationSpec = tween(400, easing = EaseOut)
            ) + fadeIn(
                animationSpec = tween(400, easing = EaseOut)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> (fullWidth * 0.3f).toInt() },
                animationSpec = tween(400, easing = EaseIn)
            ) + fadeOut(
                animationSpec = tween(300, easing = EaseIn)
            )
        }
    ) {
        composable(NavigationRoutes.WELCOME) {
            WelcomeScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(NavigationRoutes.LOGIN) {
                        popUpTo(NavigationRoutes.WELCOME) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("${NavigationRoutes.HOME}?tab=randevular") {
                        popUpTo(NavigationRoutes.WELCOME) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavigationRoutes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(NavigationRoutes.REGISTER)
                },
                onNavigateToHome = {
                    navController.navigate("${NavigationRoutes.HOME}?tab=randevular") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavigationRoutes.REGISTER) {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = "${NavigationRoutes.HOME}?tab={tab}",
            arguments = listOf(navArgument("tab") { 
                type = NavType.StringType
                defaultValue = "randevular"
            })
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getString("tab") ?: "randevular"
            
            MainScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(NavigationRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToProfileSettings = {
                    navController.navigate(NavigationRoutes.PROFILE_SETTINGS)
                },
                mainNavController = navController,
                customerViewModel = customerViewModel,
                employeeViewModel = employeeViewModel,
                initialTab = initialTab
            )
        }
        
        composable(NavigationRoutes.PROFILE_SETTINGS) {
            ProfileSettingsScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavigationRoutes.THEME) {
            ThemeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavigationRoutes.FEEDBACK) {
            FeedbackScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavigationRoutes.FINANCE) {
            FinanceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(NavigationRoutes.STATISTICS) {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(NavigationRoutes.TUTORIAL) {
            TutorialScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "${NavigationRoutes.ADD_CUSTOMER}?source={source}",
            arguments = listOf(navArgument("source") { 
                type = NavType.StringType
                defaultValue = "musteriler"
            })
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "musteriler"
            
            AddCustomerScreen(
                onNavigateBack = {
                    // Navigate back based on source
                    when (source) {
                        "appointment" -> {
                            // Navigate back to Add Appointment screen
                            navController.popBackStack()
                        }
                        "operations" -> {
                            // Navigate back to Operations tab (İşlemler)
                            navController.navigate("${NavigationRoutes.HOME}?tab=operations") {
                                popUpTo(NavigationRoutes.ADD_CUSTOMER) { inclusive = true }
                            }
                        }
                        else -> {
                            // Default: Navigate back to Musteriler tab
                            navController.navigate("${NavigationRoutes.HOME}?tab=musteriler") {
                                popUpTo(NavigationRoutes.ADD_CUSTOMER) { inclusive = true }
                            }
                        }
                    }
                },
                onCustomerAdded = {
                    // After successful customer addition
                    when (source) {
                        "appointment" -> {
                            // Navigate back to Add Appointment screen
                            navController.popBackStack()
                        }
                        else -> {
                            // Default behavior - already handled in onNavigateBack
                        }
                    }
                }
            )
        }
        
        // Customer detail screen - Use passed customerViewModel or create new one
        composable(NavigationRoutes.CUSTOMER_DETAIL) {
            val viewModel = customerViewModel ?: hiltViewModel<CustomerViewModel>()
            val uiState by viewModel.uiState.collectAsState()
            val selectedCustomer = uiState.selectedCustomer
            
            if (selectedCustomer != null) {
                CustomerDetailScreen(
                    customer = selectedCustomer,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEdit = { customerToEdit ->
                        viewModel.selectCustomer(customerToEdit)
                        navController.navigate(NavigationRoutes.EDIT_CUSTOMER)
                    },
                    customerViewModel = viewModel
                )
            } else {
                // Fallback: Navigate back if no customer is selected
                navController.popBackStack()
            }
        }
        
        // Customer edit screen - Use passed customerViewModel or create new one
        composable(NavigationRoutes.EDIT_CUSTOMER) {
            val viewModel = customerViewModel ?: hiltViewModel<CustomerViewModel>()
            val uiState by viewModel.uiState.collectAsState()
            val selectedCustomer = uiState.selectedCustomer
            
            if (selectedCustomer != null) {
                EditCustomerScreen(
                    customer = selectedCustomer,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    customerViewModel = viewModel
                )
            } else {
                // Fallback: Navigate back if no customer is selected
                navController.popBackStack()
            }
        }
        
        // Employee Management Routes
        composable(NavigationRoutes.EMPLOYEE_LIST) {
            EmployeeScreen(
                onNavigateToAddEmployee = {
                    navController.navigate(NavigationRoutes.ADD_EMPLOYEE)
                },
                onNavigateToEditEmployee = { employeeId ->
                    navController.navigate("${NavigationRoutes.EDIT_EMPLOYEE}/$employeeId")
                },
                onNavigateBack = {
                    // Navigate back to Operations screen (İşlemler)
                    navController.navigate("${NavigationRoutes.HOME}?tab=operations") {
                        popUpTo(NavigationRoutes.EMPLOYEE_LIST) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavigationRoutes.ADD_EMPLOYEE) {
            AddEmployeeScreen(
                onNavigateBack = {
                    // Navigate back to Employee List
                    navController.popBackStack()
                }
            )
        }
        
        // Employee detail screen - Use selectedEmployee from state like Customer detail
        composable(NavigationRoutes.EMPLOYEE_DETAIL) {
            val viewModel = employeeViewModel ?: hiltViewModel<com.example.beautydate.viewmodels.EmployeeViewModel>()
            val uiState by viewModel.uiState.collectAsState()
            val selectedEmployee = uiState.selectedEmployee
            
            if (selectedEmployee != null) {
                EmployeeDetailScreen(
                    employee = selectedEmployee,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToEdit = {
                        // Navigate to Employee Edit Screen
                        navController.navigate(NavigationRoutes.EDIT_EMPLOYEE)
                    },
                    employeeViewModel = viewModel
                )
            } else {
                // Fallback: Navigate back if no employee is selected
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
        
        // Employee edit screen - Use selectedEmployee from state like Customer edit
        composable(NavigationRoutes.EDIT_EMPLOYEE) {
            val viewModel = employeeViewModel ?: hiltViewModel<com.example.beautydate.viewmodels.EmployeeViewModel>()
            val uiState by viewModel.uiState.collectAsState()
            val selectedEmployee = uiState.selectedEmployee
            
            if (selectedEmployee != null) {
                EditEmployeeScreen(
                    employee = selectedEmployee,
                    employeeViewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            } else {
                // Fallback: Navigate back if no employee is selected
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
        
        // Service Management Routes
        composable(NavigationRoutes.SERVICE_LIST) {
            ServiceScreen(
                onNavigateToAddService = {
                    navController.navigate(NavigationRoutes.ADD_SERVICE)
                },
                onNavigateToEditService = { serviceId ->
                    navController.navigate("${NavigationRoutes.EDIT_SERVICE}/$serviceId")
                },
                onNavigateBack = {
                    navController.navigate("${NavigationRoutes.HOME}?tab=operations") {
                        popUpTo(NavigationRoutes.SERVICE_LIST) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = "${NavigationRoutes.ADD_SERVICE}?source={source}",
            arguments = listOf(navArgument("source") { 
                type = NavType.StringType
                defaultValue = "hizmetler"
            })
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "hizmetler"
            
            AddServiceScreen(
                onNavigateBack = {
                    // Navigate back based on source
                    when (source) {
                        "appointment" -> {
                            // Navigate back to Add Appointment screen
                            navController.popBackStack()
                        }
                        else -> {
                            // Default: Navigate back to service list
                            navController.popBackStack()
                        }
                    }
                }
            )
        }
        
        composable(
            route = "${NavigationRoutes.EDIT_SERVICE}/{serviceId}",
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            EditServiceScreen(
                serviceId = serviceId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavigationRoutes.PRICE_UPDATE) {
            PriceUpdateScreen(
                onNavigateBack = {
                    navController.navigate("${NavigationRoutes.HOME}?tab=operations") {
                        popUpTo(NavigationRoutes.PRICE_UPDATE) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavigationRoutes.WORKING_HOURS) {
            WorkingHoursScreen(
                onNavigateBack = {
                    navController.navigate("${NavigationRoutes.HOME}?tab=operations") {
                        popUpTo(NavigationRoutes.WORKING_HOURS) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavigationRoutes.BUSINESS_EXPENSES) {
            BusinessExpensesScreen(
                onNavigateBack = {
                    navController.navigate("${NavigationRoutes.HOME}?tab=operations") {
                        popUpTo(NavigationRoutes.BUSINESS_EXPENSES) { inclusive = true }
                    }
                }
            )
        }
        
        // Customer Notes Routes
        composable(NavigationRoutes.CUSTOMER_NOTES) {
            CustomerNotesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Appointment Management Routes
        composable(NavigationRoutes.APPOINTMENTS) {
            AppointmentsScreen(
                onNavigateToAddAppointment = {
                    navController.navigate(NavigationRoutes.ADD_APPOINTMENT)
                },
                onNavigateToEditAppointment = {
                    navController.navigate(NavigationRoutes.EDIT_APPOINTMENT)
                }
            )
        }
        
        composable(
            route = "${NavigationRoutes.ADD_APPOINTMENT}?date={date}&time={time}&source={source}",
            arguments = listOf(
                navArgument("date") { 
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("time") { 
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("source") { 
                    type = NavType.StringType
                    defaultValue = "default"
                }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val time = backStackEntry.arguments?.getString("time") ?: ""
            val source = backStackEntry.arguments?.getString("source") ?: "default"
            
            AddAppointmentScreen(
                preSelectedDate = date,
                preSelectedTime = time,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddCustomer = {
                    navController.navigate("${NavigationRoutes.ADD_CUSTOMER}?source=appointment")
                },
                onNavigateToAddService = {
                    navController.navigate("${NavigationRoutes.ADD_SERVICE}?source=appointment")
                },
                onAppointmentCreated = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavigationRoutes.APPOINTMENT_DETAIL) {
            val viewModel = // Assuming appointmentViewModel is available in the scope or passed as an argument
                // For now, we'll just navigate to detail, assuming a ViewModel handles selection
                hiltViewModel<com.example.beautydate.viewmodels.AppointmentViewModel>()
            val uiState by viewModel.uiState.collectAsState()
            val selectedAppointment = uiState.selectedAppointment
            
            if (selectedAppointment != null) {
                AppointmentDetailScreen(
                    appointment = selectedAppointment,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    appointmentViewModel = viewModel
                )
            } else {
                // Fallback: Navigate back if no appointment is selected
                navController.popBackStack()
            }
        }
        
        composable(NavigationRoutes.EDIT_APPOINTMENT) {
            val viewModel = hiltViewModel<com.example.beautydate.viewmodels.AppointmentViewModel>()
            val uiState by viewModel.uiState.collectAsState()
            val selectedAppointment = uiState.selectedAppointment
            
            if (selectedAppointment != null) {
                EditAppointmentScreen(
                    appointment = selectedAppointment,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToAddCustomer = {
                        navController.navigate("${NavigationRoutes.ADD_CUSTOMER}?source=appointment")
                    },
                    onNavigateToAddService = {
                        navController.navigate("${NavigationRoutes.ADD_SERVICE}?source=appointment")
                    },
                    onAppointmentUpdated = {
                        // Refresh appointment list or navigate back
                    },
                    appointmentViewModel = viewModel
                )
            } else {
                // Fallback: Navigate back if no appointment is selected
                navController.popBackStack()
            }
        }
    }
}

/**
 * Centralized navigation routes
 */
object NavigationRoutes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    
    // Bottom Navigation Routes
    const val RANDEVULAR = "randevular"
    const val ZIYARETLER = "ziyaretler"
    const val OPERATIONS = "operations"  // Updated: "islemler" → "operations" for English standards
    const val MUSTERILER = "musteriler"
    const val DIGER = "diger"
    
    // Profile/Settings Route
    const val PROFILE_SETTINGS = "profile_settings"
    
    // Other Menu Routes
    const val FINANCE = "finance"
    const val STATISTICS = "statistics"
    const val HOW_TO_USE = "how_to_use"
    const val NOTIFICATIONS = "notifications"
    const val THEME = "theme"
    const val FEEDBACK = "feedback"
    const val TUTORIAL = "tutorial"
    
    // Customer Management Routes
    const val ADD_CUSTOMER = "add_customer"
    const val CUSTOMER_DETAIL = "customer_detail"
    const val EDIT_CUSTOMER = "edit_customer"
    
    // Employee Management Routes
    const val EMPLOYEE_LIST = "employee_list"
    const val ADD_EMPLOYEE = "add_employee"
    const val EMPLOYEE_DETAIL = "employee_detail"
    const val EDIT_EMPLOYEE = "edit_employee"
    
    // Service Management Routes
    const val SERVICE_LIST = "service_list"
    const val ADD_SERVICE = "add_service"
    const val EDIT_SERVICE = "edit_service"
    const val PRICE_UPDATE = "price_update"
    const val WORKING_HOURS = "working_hours"
    const val BUSINESS_EXPENSES = "business_expenses"
    
    // Customer Notes Routes
    const val CUSTOMER_NOTES = "customer_notes"
    
    // Appointment Management Routes
    const val APPOINTMENTS = "appointments"
    const val ADD_APPOINTMENT = "add_appointment"
    const val APPOINTMENT_DETAIL = "appointment_detail"
    const val EDIT_APPOINTMENT = "edit_appointment"
} 