rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      allow create: if request.auth != null && request.auth.uid == userId;
      // Geçici: Username lookup için okuma izni
      allow read: if true;
    }
    
    match /usernames/{username} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // BASİT VE GÜVENLİ: Customer kuralları
    match /customers/{customerId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // BASİT VE GÜVENLİ: Employee kuralları
    match /employees/{employeeId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }

    // BASİT VE GÜVENLİ: Services (Hizmetler) kuralları
    match /services/{serviceId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // BASİT VE GÜVENLİ: Customer Notes (Müşteri Notları) kuralları
    match /customer_notes/{noteId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // YENİ: Appointments (Randevular) kuralları
    match /appointments/{appointmentId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // YENİ: Working Hours (Çalışma Saatleri) kuralları
    match /working_hours/{workingHourId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
  }
} 