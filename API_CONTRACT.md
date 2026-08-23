# Voice Yanga API Contract

This document defines the REST API contract expected by the Voice Yanga mobile application. The application is built with an offline-first architecture, utilizing a local Room database and background synchronization via WorkManager.

## 1. Project Overview

**Voice Yanga** is a citizen engagement platform designed to bridge the gap between residents and local authorities. 
- **Purpose**: Enable citizens to report community issues (Water, Sanitation, Roads, Electricity, Security), attach photographic evidence, and track resolution progress in real-time.
- **REST API Role**: Serves as the central data hub for user authentication, persistent storage of reports, administrative status updates, and push notification triggers.
- **Relationship**:
    - **Mobile App**: The primary interface for citizens to submit and track complaints.
    - **Backend API**: Manages business logic, data validation, and communication between components.
    - **Database**: Stores complaints, user profiles, and audit trails.
    - **Admin Dashboard**: (Expected) Used by government officials to review, assign, and resolve reported issues.

## 2. Mobile Application Architecture

The application follows the **MVVM (Model-View-ViewModel)** pattern combined with **Clean Architecture** principles.

### Data Flow Pattern
```
      [ UI Layer ] (Activities/Fragments)
           |
      [ ViewModel ] (UI State Management)
           |
      [ Repository ] (Data Strategy: Local vs Remote)
           | ----------------------- |
    [ Room Database ]        [ Retrofit / API Service ]
    (Local Entities)         (Remote DTOs)
```

**Sync Strategy**: 
1. User creates a report locally (Status: `PENDING`).
2. `SyncWorker` triggers in the background when connectivity is available.
3. Photos are uploaded to a storage service/endpoint.
4. Complaint data is submitted to the REST API.
5. Server reconciles the `clientUuid` and returns a `serverId` and `referenceCode`.

## 3. API Communication Standards

- **Base URL**: `https://api.voiceyanga.com/api/v1/`
- **Authentication**: JWT (JSON Web Token)
    - **Header**: `Authorization: Bearer <token>`
    - **Session**: Managed securely via `EncryptedSharedPreferences`.
- **Content Type**: `application/json` (Request/Response bodies).
- **Media Upload**: `multipart/form-data` (For photo attachments).
- **Date Format**: Unix Timestamp in milliseconds (`long`). Example: `1724354267000`.
- **Pagination**: 
    - **Request**: Query parameters `page` (int) and `limit` (int).
    - **Response**: Envelope containing `data` array and `meta` (total items, total pages).
- **Standard Error Response**:
    ```json
    {
      "error": true,
      "message": "Detailed error message for the user",
      "code": "ERROR_CODE_STRING"
    }
    ```

---

## 4. Authentication API Contract

### User Registration
- **Endpoint**: `/auth/register`
- **HTTP Method**: `POST`
- **Purpose**: Create a new citizen account.
- **Authentication Required**: No
- **Request Body**:
    ```json
    {
      "firstName": "John",
      "lastName": "Doe",
      "phone": "+260971123456",
      "email": "john.doe@example.com",
      "password": "password123"
    }
    ```
- **Expected Response (201 Created)**:
    ```json
    {
      "token": "jwt_access_token_string",
      "user": {
        "name": "Harrison Mwewa",
        "email": "john.doe@example.com",
        "phone": "+260971123456"
      }
    }
    ```

### User Login
- **Endpoint**: `/auth/login`
- **HTTP Method**: `POST`
- **Purpose**: Authenticate an existing user.
- **Authentication Required**: No
- **Request Body**:
    ```json
    {
      "email": "john.doe@example.com",
      "password": "password123"
    }
    ```
- **Expected Response (200 OK)**:
    ```json
    {
      "token": "jwt_access_token_string",
      "user": {
        "name": "Harrison Mwewa",
        "email": "john.doe@example.com",
        "phone": "+260971123456"
      }
    }
    ```

### Password Reset
- **Endpoint**: `/auth/forgot-password`
- **HTTP Method**: `POST`
- **Purpose**: Initiate password recovery.
- **Authentication Required**: No
- **Request Body**:
    ```json
    {
      "email": "john.doe@example.com"
    }
    ```
- **Expected Response (200 OK)**:
    ```json
    {
      "message": "Reset instructions sent to your email"
    }
    ```

---

## 5. User API Contract

### User Profile Retrieval
- **Endpoint**: `/user/profile`
- **HTTP Method**: `GET`
- **Authentication Required**: Yes
- **Expected Response**:
    ```json
    {
      "id": "user_id_uuid",
      "name": "Harrison Mwewa",
      "email": "john.doe@example.com",
      "phone": "+260971123456",
      "role": "CITIZEN",
      "createdAt": 1724354267000
    }
    ```

---

## 6. Voice Yanga Core Features API Contract

### Create Complaint
- **Endpoint**: `/complaints`
- **HTTP Method**: `POST`
- **Purpose**: Submit a new community issue report.
- **Authentication Required**: Yes
- **Request Body**:
    ```json
    {
      "clientUuid": "local-unique-id-uuid",
      "title": "Broken Water Pipe",
      "description": "Large leak at the corner of Cairo Rd and Independence Ave.",
      "category": "Water",
      "location": "Matero, Lusaka",
      "priority": "MEDIUM",
      "authorEmail": "john.doe@example.com",
      "createdAt": 1724354267000,
      "photoUrls": ["https://storage.url/photo1.jpg"]
    }
    ```
- **Expected Response (201 Created)**:
    ```json
    {
      "serverId": "SRV-1724354267000",
      "referenceCode": "VY-123456",
      "status": "SUBMITTED",
      "syncStatus": "SYNCED"
    }
    ```

### List Complaints
- **Endpoint**: `/complaints`
- **HTTP Method**: `GET`
- **Purpose**: Retrieve a list of all community complaints.
- **Parameters**: `page`, `limit`, `category` (optional), `status` (optional).
- **Expected Response**: Array of Complaint objects.

### List My Complaints
- **Endpoint**: `/complaints/my`
- **HTTP Method**: `GET`
- **Purpose**: Retrieve complaints submitted by the authenticated user.
- **Expected Response**: Array of Complaint objects.

### Support a Complaint (Upvote)
- **Endpoint**: `/complaints/{serverId}/support`
- **HTTP Method**: `POST`
- **Purpose**: Increment the support count for a specific issue.
- **Expected Response (200 OK)**:
    ```json
    {
      "newSupportCount": 15
    }
    ```

### Get Complaint Comments
- **Endpoint**: `/complaints/{serverId}/comments`
- **HTTP Method**: `GET`
- **Expected Response**:
    ```json
    [
      {
        "id": "comment_uuid",
        "authorName": "Official Admin",
        "content": "Your report has been reviewed by our triage team.",
        "isOfficial": true,
        "createdAt": 1724354267000
      }
    ]
    ```

---

## 7. Media Upload Flow

- **Endpoint**: `/photos/upload`
- **HTTP Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Request Parts**:
    - `file`: Binary photo data.
    - `complaintUuid`: The local client UUID to link the photo.
- **Expected Response**:
    ```json
    {
      "photoUrl": "https://voiceyanga-storage.s3.amazonaws.com/uploads/photo.jpg"
    }
    ```

---

## 8. Push Notifications

The app expects push notifications via Firebase Cloud Messaging (FCM).

### Expected Payload Shape:
```json
{
  "notification": {
    "title": "Status Update",
    "body": "Your report VY-123456 has been RESOLVED."
  },
  "data": {
    "complaintUuid": "original-client-uuid-or-server-id",
    "type": "STATUS_CHANGE"
  }
}
```
- **Types**: `STATUS_CHANGE`, `NEW_COMMENT`, `SYSTEM`.

---

## 9. Enums & Fixed Values

### Complaint Category
- `Water`
- `Sanitation`
- `Roads`
- `Electricity`
- `Security`

### Complaint Status
- `SUBMITTED`: Initial state after sync.
- `REVIEWED`: Triage team has seen the report.
- `ASSIGNED`: Allocated to a specific department/technician.
- `IN_PROGRESS`: On-site work started.
- `RESOLVED`: Issue fixed.
- `REJECTED`: Duplicate or invalid report.

### Sync Status (Local Only)
- `PENDING`, `SYNCING`, `SYNCED`, `FAILED`.

---

## 10. Assumptions & Mock-Data Gaps

> [!IMPORTANT]
> **NEEDS BACKEND CONFIRMATION**
> 1. **Photo Upload**: The mock code simulates photo upload but doesn't define the exact multipart field name. "photo" or "file" is assumed.
> 2. **Reference Code Generation**: The app expects a user-friendly code like `VY-XXXXXX`. The backend must implement logic to generate this uniquely.
> 3. **Priority Selection**: The mobile UI currently defaults all reports to `MEDIUM`. The backend should decide if it will override this based on category or if a "HIGH" priority endpoint exists for urgent safety issues.
> 4. **Date Precision**: The app uses `long` (milliseconds). Confirm if the backend prefers ISO 8601 strings.
> 5. **Pagination Envelope**: The app's repository doesn't yet handle paginated responses in the mock. The `meta` structure in section 3 is a best-practice recommendation.
