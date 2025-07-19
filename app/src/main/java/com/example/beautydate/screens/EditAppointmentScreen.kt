/**
 * Edit Appointment Screen - Enhanced appointment editing interface
 * Pre-fills form with existing appointment data
 * Material Design 3 with clean architecture and memory efficiency
 */
package com.example.beautydate.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.beautydate.R
import com.example.beautydate.data.models.Customer
import com.example.beautydate.data.models.Service
import com.example.beautydate.data.models.Appointment
import com.example.beautydate.data.models.AppointmentStatus
import com.example.beautydate.utils.ToastUtils
import com.example.beautydate.viewmodels.CustomerViewModel
import com.example.beautydate.viewmodels.ServiceViewModel
import com.example.beautydate.viewmodels.AppointmentViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppointmentScreen(
    appointment: Appointment,
    onNavigateBack: () -> Unit,
    onNavigateToAddCustomer: () -> Unit = {},
    onNavigateToAddService: () -> Unit = {},
    onAppointmentUpdated: () -> Unit = {},
    customerViewModel: CustomerViewModel = hiltViewModel(),
    serviceViewModel: ServiceViewModel = hiltViewModel(),
    appointmentViewModel: AppointmentViewModel = hiltViewModel()
) {
    val customerUiState by customerViewModel.uiState.collectAsStateWithLifecycle()
    val serviceUiState by serviceViewModel.uiState.collectAsStateWithLifecycle()
    val appointmentUiState by appointmentViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // State for appointment editing - pre-filled with current data
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedService by remember { mutableStateOf<Service?>(null) }
    var selectedDate by remember { mutableStateOf(appointment.appointmentDate) }
    var selectedTime by remember { mutableStateOf(appointment.appointmentTime) }
    var appointmentNotes by remember { mutableStateOf(appointment.notes) }
    var showCustomerDialog by remember { mutableStateOf(false) }
    var showServiceDialog by remember { mutableStateOf(false) }
    var isButtonDisabled by remember { mutableStateOf(false) }

    // Initialize data
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { businessId ->
            customerViewModel.initializeCustomers()
            serviceViewModel.initializeServices()
            
            // Pre-fill form data from the provided appointment
            selectedCustomer = customerUiState.customers.find { it.id == appointment.customerId }
            selectedService = serviceUiState.services.find { it.id == appointment.serviceId }
            selectedDate = appointment.appointmentDate
            selectedTime = appointment.appointmentTime
            appointmentNotes = appointment.notes
        }
    }

    // Pre-select current customer and service when data loads
    LaunchedEffect(customerUiState.customers, appointment.customerId) {
        if (customerUiState.customers.isNotEmpty() && selectedCustomer == null) {
            selectedCustomer = customerUiState.customers.find { it.id == appointment.customerId }
        }
    }

    LaunchedEffect(serviceUiState.services, appointment.serviceId) {
        if (serviceUiState.services.isNotEmpty() && selectedService == null) {
            selectedService = serviceUiState.services.find { it.id == appointment.serviceId }
        }
    }

    // Handle appointment update success
    LaunchedEffect(appointmentUiState.successMessage) {
        appointmentUiState.successMessage?.let { message ->
            ToastUtils.showSuccess(context, message)
            appointmentViewModel.clearMessages()
            isButtonDisabled = false
            onAppointmentUpdated()
            onNavigateBack()
        }
    }

    // Handle appointment update error
    LaunchedEffect(appointmentUiState.errorMessage) {
        appointmentUiState.errorMessage?.let { message ->
            ToastUtils.showError(context, message)
            appointmentViewModel.clearMessages()
            isButtonDisabled = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Randevu Düzenle",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer Selection
            item {
                AppointmentCustomerCard(
                    selectedCustomer = selectedCustomer,
                    onClick = { showCustomerDialog = true },
                    onAddNewCustomer = onNavigateToAddCustomer
                )
            }

            // Service Selection
            item {
                AppointmentServiceCard(
                    selectedService = selectedService,
                    onClick = { showServiceDialog = true },
                    onAddNewService = onNavigateToAddService
                )
            }

            // Date Selection
            item {
                AppointmentDateCard(
                    selectedDate = selectedDate,
                    onDateSelected = { date -> selectedDate = date }
                )
            }

            // Time Selection
            item {
                AppointmentTimeCard(
                    selectedTime = selectedTime,
                    onTimeSelected = { time -> selectedTime = time }
                )
            }

            // Notes Section
            item {
                AppointmentNotesCard(
                    notes = appointmentNotes,
                    onNotesChanged = { notes -> appointmentNotes = notes }
                )
            }

            // Update Appointment Button
            item {
                UpdateAppointmentButton(
                    isEnabled = selectedCustomer != null && selectedService != null && 
                               selectedDate.isNotBlank() && selectedTime.isNotBlank() && !isButtonDisabled,
                    isLoading = appointmentUiState.isLoading,
                    onClick = {
                        if (selectedCustomer != null && selectedService != null && !isButtonDisabled) {
                            isButtonDisabled = true
                            
                            val updatedAppointment = appointment.copy(
                                customerId = selectedCustomer!!.id,
                                customerName = selectedCustomer!!.fullName,
                                customerPhone = selectedCustomer!!.phoneNumber,
                                serviceId = selectedService!!.id,
                                serviceName = selectedService!!.name,
                                servicePrice = selectedService!!.price,
                                appointmentDate = selectedDate,
                                appointmentTime = selectedTime,
                                notes = appointmentNotes,
                                updatedAt = Timestamp.now()
                            )
                            
                            appointmentViewModel.updateAppointment(updatedAppointment)
                        }
                    }
                )
            }
        }
    }

    // Customer Selection Dialog
    if (showCustomerDialog && customerUiState.customers.isNotEmpty()) {
        CustomerSelectionDialog(
            customers = customerUiState.customers,
            onCustomerSelected = { customer ->
                selectedCustomer = customer
                showCustomerDialog = false
            },
            onDismiss = { showCustomerDialog = false }
        )
    }

    // Service Selection Dialog
    if (showServiceDialog && serviceUiState.services.isNotEmpty()) {
        ServiceSelectionDialog(
            services = serviceUiState.services,
            onServiceSelected = { service ->
                selectedService = service
                showServiceDialog = false
            },
            onDismiss = { showServiceDialog = false }
        )
    }
}

/**
 * Update appointment button
 */
@Composable
private fun UpdateAppointmentButton(
    isEnabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = isEnabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Randevuyu Güncelle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Reuse existing card components from AddAppointmentScreen
@Composable
private fun AppointmentCustomerCard(
    selectedCustomer: Customer?,
    onClick: () -> Unit,
    onAddNewCustomer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selectedCustomer != null) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Müşteri Seçin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row {
                    IconButton(onClick = onAddNewCustomer) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Yeni Müşteri Ekle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (selectedCustomer != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedCustomer.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = selectedCustomer.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppointmentServiceCard(
    selectedService: Service?,
    onClick: () -> Unit,
    onAddNewService: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selectedService != null) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hizmet Seçin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row {
                    IconButton(onClick = onAddNewService) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Yeni Hizmet Ekle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (selectedService != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedService.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = selectedService.formattedPrice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AppointmentDateCard(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    // Date picker implementation - for now showing current date
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Randevu Tarihi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = selectedDate.ifEmpty { "Tarih seçiniz" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AppointmentTimeCard(
    selectedTime: String,
    onTimeSelected: (String) -> Unit
) {
    // Time picker implementation - for now showing current time
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Randevu Saati",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = selectedTime.ifEmpty { "Saat seçiniz" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AppointmentNotesCard(
    notes: String,
    onNotesChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Notlar (Opsiyonel)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Randevu ile ilgili notlarınızı yazabilirsiniz...")
                },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
private fun CustomerSelectionDialog(
    customers: List<Customer>,
    onCustomerSelected: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Müşteri Seçin",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(customers) { customer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = { onCustomerSelected(customer) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = customer.fullName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = customer.phoneNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun ServiceSelectionDialog(
    services: List<Service>,
    onServiceSelected: (Service) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Hizmet Seçin",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(services) { service ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = { onServiceSelected(service) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = service.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = service.formattedPrice,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
} 