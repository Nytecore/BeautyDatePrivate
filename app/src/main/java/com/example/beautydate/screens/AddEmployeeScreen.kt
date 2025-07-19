package com.example.beautydate.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.beautydate.data.models.Employee
import com.example.beautydate.data.models.EmployeeGender
import com.example.beautydate.data.models.EmployeePermission
import com.example.beautydate.screens.components.EmployeeFormSection
import com.example.beautydate.screens.components.EmployeeSkillsSection
import com.example.beautydate.screens.components.EmployeePermissionsSection
import com.example.beautydate.screens.components.EmployeeGenderSelector
import com.example.beautydate.utils.PhoneNumberTransformation
import com.example.beautydate.viewmodels.EmployeeViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * Screen for adding new employees
 * Follows RegisterScreen pattern with form validation
 * Memory efficient: state management with computed validation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeScreen(
    employeeViewModel: EmployeeViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    // Form state
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(EmployeeGender.MALE) } // Changed from OTHER to MALE
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var hireDate by remember { mutableStateOf("") }
    var selectedSkills by remember { mutableStateOf(emptySet<String>()) }
    var selectedPermissions by remember { mutableStateOf(emptySet<EmployeePermission>()) }
    var notes by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    
    // Date picker state for hire date
    var showHireDatePicker by remember { mutableStateOf(false) }
    val hireDatePickerState = rememberDatePickerState()
    
    // Validation states
    var firstNameError by remember { mutableStateOf("") }
    var lastNameError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var hireDateError by remember { mutableStateOf("") }
    var salaryError by remember { mutableStateOf("") }
    
    // Form submission state
    var isSubmitting by remember { mutableStateOf(false) }
    
    val uiState by employeeViewModel.uiState.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    
    // Handle success/error messages
    LaunchedEffect(uiState.successMessage) {
        if (!uiState.successMessage.isNullOrBlank()) {
            // Navigate back after successful addition
            kotlinx.coroutines.delay(1000)
            onNavigateBack()
        }
    }
    
    // Validation function
    fun validateForm(): Boolean {
        var isValid = true
        
        // First name validation
        if (firstName.isBlank()) {
            firstNameError = "Ad alanı zorunludur"
            isValid = false
        } else {
            firstNameError = ""
        }
        
        // Last name validation
        if (lastName.isBlank()) {
            lastNameError = "Soyad alanı zorunludur"
            isValid = false
        } else {
            lastNameError = ""
        }
        
        // Phone validation
        if (phoneNumber.isBlank()) {
            phoneError = "Telefon numarası zorunludur"
            isValid = false
        } else if (phoneNumber.length < 10) {
            phoneError = "Geçerli bir telefon numarası girin"
            isValid = false
        } else {
            phoneError = ""
        }
        
        // Email validation (optional but if provided, must be valid)
        if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Geçerli bir e-mail adresi girin"
            isValid = false
        } else {
            emailError = ""
        }
        
        // Hire date validation
        if (hireDate.isBlank()) {
            hireDateError = "İşe başlama tarihi zorunludur"
            isValid = false
        } else {
            hireDateError = ""
        }
        
        // Salary validation (optional but must be valid if provided)
        if (salary.isNotBlank()) {
            val salaryValue = salary.toDoubleOrNull()
            if (salaryValue == null || salaryValue < 0) {
                salaryError = "Geçerli bir maaş tutarı girin"
                isValid = false
            } else {
                salaryError = ""
            }
        } else {
            salaryError = ""
        }
        
        return isValid
    }
    
    // Submit function
    fun submitEmployee() {
        if (!validateForm()) return
        
        isSubmitting = true
        
        val employee = Employee(
            id = Employee.generateEmployeeId(),
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            gender = selectedGender,
            phoneNumber = phoneNumber.trim(),
            email = email.trim(),
            address = address.trim(),
            hireDate = hireDate.trim(),
            skills = selectedSkills.toList(),
            permissions = selectedPermissions.toList(),
            notes = notes.trim(),
            salary = salary.toDoubleOrNull() ?: 0.0,
            isActive = true,
            businessId = currentUser?.uid ?: ""
        )
        
        employeeViewModel.addEmployee(employee)
        isSubmitting = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Yeni Çalışan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Geri"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // Error message
        uiState.errorMessage?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Success message
        uiState.successMessage?.let { successMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = successMessage,
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Form Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Personal Information Section
            EmployeeFormSection(
                title = "Kişisel Bilgiler",
                icon = Icons.Default.Person
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // First Name
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { 
                            firstName = it
                            if (firstNameError.isNotEmpty()) {
                                firstNameError = ""
                            }
                        },
                        label = { Text("Ad *") },
                        placeholder = { Text("Çalışanın adı") },
                        isError = firstNameError.isNotEmpty(),
                        supportingText = if (firstNameError.isNotEmpty()) {
                            { Text(firstNameError) }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Last Name
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { 
                            lastName = it
                            if (lastNameError.isNotEmpty()) {
                                lastNameError = ""
                            }
                        },
                        label = { Text("Soyad *") },
                        placeholder = { Text("Çalışanın soyadı") },
                        isError = lastNameError.isNotEmpty(),
                        supportingText = if (lastNameError.isNotEmpty()) {
                            { Text(lastNameError) }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Gender Selection
                    EmployeeGenderSelector(
                        selectedGender = selectedGender,
                        onGenderSelected = { selectedGender = it }
                    )
                }
            }
            
            // Contact Information Section
            EmployeeFormSection(
                title = "İletişim Bilgileri",
                icon = Icons.Default.Phone
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Phone Number
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { 
                            phoneNumber = it
                            if (phoneError.isNotEmpty()) {
                                phoneError = ""
                            }
                        },
                        label = { Text("Telefon Numarası *") },
                        placeholder = { Text("0 (5--) --- ----") },
                        isError = phoneError.isNotEmpty(),
                        supportingText = if (phoneError.isNotEmpty()) {
                            { Text(phoneError) }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null
                            )
                        },
                        visualTransformation = PhoneNumberTransformation.phoneTransformation,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            if (emailError.isNotEmpty()) {
                                emailError = ""
                            }
                        },
                        label = { Text("E-mail") },
                        placeholder = { Text("ornek@email.com") },
                        isError = emailError.isNotEmpty(),
                        supportingText = if (emailError.isNotEmpty()) {
                            { Text(emailError) }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null
                            )
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Address
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Adres") },
                        placeholder = { Text("Tam adres") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(top = 8.dp), // Icon'u en üste hizalar
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp), // 3 satır için yeterli height
                        minLines = 3,
                        maxLines = 3
                    )
                }
            }
            
            // Work Information Section
            EmployeeFormSection(
                title = "İş Bilgileri",
                icon = Icons.Default.Work
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hire Date with Date Picker
                    OutlinedTextField(
                        value = hireDate,
                        onValueChange = { /* Read only, handled by date picker */ },
                        label = { Text("İşe Başlama Tarihi *") },
                        placeholder = { Text("__ / __ / ____") },
                        isError = hireDateError.isNotEmpty(),
                        supportingText = if (hireDateError.isNotEmpty()) {
                            { Text(hireDateError) }
                        } else null,
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showHireDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Tarih Seç"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHireDatePicker = true },
                        singleLine = true
                    )
                    
                    // Skills Selection
                    EmployeeSkillsSection(
                        selectedSkills = selectedSkills,
                        onSkillsChanged = { selectedSkills = it }
                    )
                    
                    // Permissions Selection
                    EmployeePermissionsSection(
                        selectedPermissions = selectedPermissions,
                        onPermissionsChanged = { selectedPermissions = it }
                    )
                }
            }
            
            // Notes Section
            EmployeeFormSection(
                title = "Notlar",
                icon = Icons.Default.Notes
            ) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ek Notlar") },
                    placeholder = { Text("Çalışan hakkında ek bilgiler...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        }
        
        // Submit Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Button(
                onClick = { submitEmployee() },
                enabled = !isSubmitting && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                if (isSubmitting || uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Çalışan Ekle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    
    // Hire Date Picker Dialog
    if (showHireDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showHireDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        hireDatePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = millis
                            hireDate = String.format(
                                "%02d/%02d/%04d",
                                calendar.get(Calendar.DAY_OF_MONTH),
                                calendar.get(Calendar.MONTH) + 1,
                                calendar.get(Calendar.YEAR)
                            )
                            // Clear error if any
                            if (hireDateError.isNotEmpty()) {
                                hireDateError = ""
                            }
                        }
                        showHireDatePicker = false
                    }
                ) {
                    Text("Tamam")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHireDatePicker = false }) {
                    Text("İptal")
                }
            }
        ) {
            DatePicker(
                state = hireDatePickerState,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
} 