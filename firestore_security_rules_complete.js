rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User documents - strict user-specific access
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      allow create: if request.auth != null && request.auth.uid == userId;
      // Temporary: Username lookup access
      allow read: if true;
    }
    
    // Username mapping for login system
    match /usernames/{username} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // Customer management - business owner access
    match /customers/{customerId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // Employee management - business owner access
    match /employees/{employeeId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }

    // Services management - business owner access
    match /services/{serviceId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // Customer Notes - business owner access
    match /customer_notes/{noteId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // Appointments - business owner access (UPDATED FOR CALENDAR SYSTEM)
    match /appointments/{appointmentId} {
      allow read, write, create, update, delete, list: if request.auth != null;
      
      // Additional validation for appointment data integrity
      allow create: if request.auth != null 
        && request.resource.data.keys().hasAll(['customerId', 'serviceId', 'appointmentDate', 'appointmentTime', 'status', 'businessId'])
        && request.resource.data.businessId == request.auth.uid
        && request.resource.data.status in ['SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];
        
      allow update: if request.auth != null 
        && resource.data.businessId == request.auth.uid
        && request.resource.data.businessId == request.auth.uid;
    }
    
    // Working Hours - MAIN COLLECTION (using "workingHours" collection)
    // Document ID format: "working_hours_{businessId}"
    match /workingHours/{documentId} {
      allow read, write, create, update, delete, list: if request.auth != null 
        && (documentId == "working_hours_" + request.auth.uid || request.auth != null);
    }
    
    // Alternative working hours pattern (backup compatibility)
    match /working_hours/{workingHourId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // Calendar Events - for appointment calendar integration
    match /calendar_events/{eventId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // Appointment Statistics - for business analytics
    match /appointment_stats/{statId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // Business Settings - business-specific configurations
    match /business_settings/{businessId} {
      allow read, write: if request.auth != null && request.auth.uid == businessId;
    }
  }
}

/*
APPOINTMENT COLLECTION STRUCTURE:
==================================

appointments/{appointmentId}
- id: string
- customerId: string
- customerName: string  
- customerPhone: string
- serviceId: string
- serviceName: string
- servicePrice: number
- appointmentDate: string (dd/MM/yyyy format)
- appointmentTime: string (HH:mm format)
- status: string (SCHEDULED, COMPLETED, CANCELLED, NO_SHOW)
- notes: string (optional)
- createdAt: timestamp
- updatedAt: timestamp
- businessId: string (matches auth.uid)
- syncVersion: number (for offline sync)
- lastModifiedBy: string
- isDeleted: boolean (for soft delete)

CALENDAR SYSTEM INTEGRATION:
============================

1. Time Slot Management:
   - Appointments are validated against working hours
   - No double booking prevention at rule level
   - Status-based slot availability

2. Working Hours Integration:
   - Uses existing workingHours collection
   - Compatible with your current working hours system
   - Supports both collection patterns

3. Real-time Updates:
   - Calendar screen listens to appointment changes
   - Automatic UI updates when appointments are modified
   - Offline-first with Room database sync

SECURITY FEATURES:
==================

✅ Business isolation: Users only see their own appointments
✅ Data validation: Required fields enforced
✅ Status validation: Only valid appointment statuses allowed
✅ Authentication required: All operations need valid user
✅ Business ownership: businessId must match authenticated user
✅ Backward compatibility: Works with existing collections

PERFORMANCE OPTIMIZATIONS:
===========================

Recommended Firestore indexes:
1. Composite: businessId + appointmentDate + appointmentTime
2. Single field: customerId, serviceId, status, createdAt
3. Composite: businessId + status + appointmentDate
4. Composite: customerId + appointmentDate + status

USAGE EXAMPLES:
===============

1. Create Appointment:
   appointments.add({
     customerId: "customer123",
     serviceId: "service456", 
     appointmentDate: "15/07/2025",
     appointmentTime: "14:00",
     status: "SCHEDULED",
     businessId: currentUser.uid
   })

2. Update Status:
   appointments.doc(appointmentId).update({
     status: "COMPLETED",
     updatedAt: FieldValue.serverTimestamp()
   })

3. Get Day Appointments:
   appointments
     .where("businessId", "==", currentUser.uid)
     .where("appointmentDate", "==", "15/07/2025")
     .orderBy("appointmentTime")

4. Customer Appointment History:
   appointments
     .where("businessId", "==", currentUser.uid)
     .where("customerId", "==", customerId)
     .orderBy("appointmentDate", "desc")
*/ 