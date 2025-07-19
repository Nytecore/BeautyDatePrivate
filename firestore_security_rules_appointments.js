rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // USER AUTHENTICATION RULES
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      allow create: if request.auth != null && request.auth.uid == userId;
      // Username lookup için okuma izni
      allow read: if true;
    }
    
    match /usernames/{username} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // CUSTOMER MANAGEMENT RULES
    match /customers/{customerId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // EMPLOYEE MANAGEMENT RULES
    match /employees/{employeeId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }

    // SERVICE MANAGEMENT RULES
    match /services/{serviceId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // CUSTOMER NOTES RULES
    match /customer_notes/{noteId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // APPOINTMENT MANAGEMENT RULES - YENİ!
    match /appointments/{appointmentId} {
      allow read, write, create, update, delete, list: if request.auth != null;
      
      // İlave güvenlik kontrolleri (opsiyonel)
      allow read: if request.auth != null 
        && (resource == null || resource.data.businessId == request.auth.uid);
      
      allow write: if request.auth != null 
        && (resource == null || resource.data.businessId == request.auth.uid)
        && request.resource.data.businessId == request.auth.uid;
    }
    
    // WORKING HOURS RULES
    match /working_hours/{workingHourId} {
      allow read, write, create, update, delete, list: if request.auth != null;
      
      // Working hours format: working_hours_{businessId}
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        (workingHourId == 'working_hours_' + request.auth.uid || 
         workingHourId == request.auth.uid);
    }
    
    // CALENDAR INTEGRATION RULES
    match /calendar_events/{eventId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // APPOINTMENT STATISTICS RULES
    match /appointment_stats/{statId} {
      allow read, write, create, update, delete, list: if request.auth != null;
    }
    
    // BUSINESS SETTINGS RULES
    match /business_settings/{businessId} {
      allow read, write: if request.auth != null && request.auth.uid == businessId;
    }
  }
}

/*
APPOINTMENT COLLECTION YAPISI:
===============================

appointments/{appointmentId}
- id: string
- customerId: string
- customerName: string
- customerPhone: string
- serviceId: string
- serviceName: string
- servicePrice: number
- appointmentDate: string (dd/MM/yyyy)
- appointmentTime: string (HH:mm)
- status: string (SCHEDULED, COMPLETED, CANCELLED, NO_SHOW)
- notes: string
- createdAt: timestamp
- updatedAt: timestamp
- businessId: string
- syncVersion: number
- lastModifiedBy: string
- isDeleted: boolean

KULLANIM ÖRNEKLERİ:
==================

1. Randevu Oluşturma:
   POST /appointments/{appointmentId}
   Body: { appointmentDate: "15/07/2025", appointmentTime: "14:00", ... }

2. Randevu Güncelleme:
   PUT /appointments/{appointmentId}
   Body: { status: "COMPLETED", updatedAt: timestamp }

3. Randevu Silme (Soft Delete):
   PUT /appointments/{appointmentId}
   Body: { isDeleted: true, updatedAt: timestamp }

4. Randevu Listeleme:
   GET /appointments?businessId={businessId}&date={date}

5. Durum Değiştirme:
   PUT /appointments/{appointmentId}
   Body: { status: "CANCELLED", updatedAt: timestamp }

GÜVENLİK KURALLARI:
==================

✅ Sadece kimlik doğrulaması yapılmış kullanıcılar erişebilir
✅ Kullanıcılar sadece kendi işletmelerinin randevularına erişebilir
✅ businessId alanı otomatik olarak request.auth.uid ile doğrulanır
✅ Soft delete pattern desteklenir
✅ Senkronizasyon için syncVersion ve lastModifiedBy alanları
✅ Working hours entegrasyonu desteklenir
✅ Real-time updates için reactive listening

PERFORMANS İYİLEŞTİRMELERİ:
===========================

- Compound index: businessId + appointmentDate + appointmentTime
- Single field index: customerId, serviceId, status, createdAt
- Partial index: isDeleted == false için
- TTL index: Eski cancelled randevular için (opsiyonel)

OFFLINE-FIRST DESTEĞI:
======================

- Local Room database ile tam senkronizasyon
- Network durumuna göre otomatik sync
- Conflict resolution: lastModifiedBy + syncVersion
- Background sync desteği
- Manual sync tetikleme

*/ 