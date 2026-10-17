# Backend Implementation Instructions: Dynamic Location Handling & Eliminating Location Defaults

This document serves as the **authoritative specification for backend engineers / Codex** to align the Voice Yanga Backend REST API (`/api/v1/`) with the Android mobile application regarding complaint locations.

---

## 1. Core Specification: Dynamic User-Submitted Locations

### Background
The mobile app collects dynamic location inputs from citizens via:
1. **Manual Text Entry**: Custom neighborhood, street, or ward names entered by the user (e.g., `"Chibolya"`, `"Kalingalinga"`, `"Mandevu"`, `"Kabwe"`).
2. **GPS Geocoding**: Reverse-geocoded address strings detected by device GPS.

The backend **does not rely on a static/pre-defined locations database table**. Therefore, any location string provided by the user must be stored verbatim and returned as-is.

---

## 2. Critical Rules & Guardrails

> [!IMPORTANT]
> 1. **NO HARDCODED FALLBACK TO MATERO**: Under no circumstances should unmatched or free-text locations default to `"Matero"`, `"Matero Ward 1"`, or any other fixed location.
> 2. **NO STRICT FK CONSTRAINTS ON LOCATION ID**: `location_id` in the `complaints` database table must be **NULLABLE** (`VARCHAR NULLABLE`). If `location_id` is null or omitted in a request, DO NOT reject the request and DO NOT overwrite `location` text.
> 3. **PRESERVE USER-SUBMITTED STRINGS**: Store the raw `location` string submitted in `POST /complaints`, multipart form submissions, or `PATCH /complaints/{id}` exactly as received.

---

## 3. Database Schema Specification

Ensure the `complaints` SQL table columns for location are defined as follows:

```sql
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS location TEXT NULL;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS location_id VARCHAR(255) NULL;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS ward VARCHAR(255) NULL;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS district VARCHAR(255) NULL;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS province VARCHAR(255) NULL;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION NULL;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION NULL;
```

---

## 4. REST API Endpoint Specifications

### 4.1 Complaint Creation (`POST /complaints` & Multipart `POST /complaints`)

#### Request Extraction Logic
When receiving a complaint creation request:
```typescript
const locationText = req.body.location || req.body.locationName || "Location Not Specified";
const locationId = req.body.locationId || null;
const ward = req.body.ward || null;
const district = req.body.district || null;
const province = req.body.province || null;
const latitude = req.body.latitude ? parseFloat(req.body.latitude) : null;
const longitude = req.body.longitude ? parseFloat(req.body.longitude) : null;
```

#### SQL Insertion
```sql
INSERT INTO complaints (
  id, title, description, category, location, location_id, 
  ward, district, province, latitude, longitude, client_uuid, user_id, status, created_at
) VALUES (
  $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, 'SUBMITTED', NOW()
);
```

---

### 4.2 Response Formatting (`GET /complaints`, `GET /complaints/my`, `GET /complaints/{id}`)

Return the saved location as both a top-level string (`location` / `locationName`) and in the `location` object for backward compatibility:

```json
{
  "id": "complaint_123",
  "clientUuid": "70000000-0000-4000-8000-000000000001",
  "title": "Broken Water Pipe",
  "description": "Clean water leaking onto street",
  "category": "Water & Sanitation",
  "location": "Chibolya Ward 14",
  "locationName": "Chibolya Ward 14",
  "locationData": {
    "id": null,
    "ward": "Chibolya",
    "district": "Lusaka",
    "province": "Lusaka Province",
    "displayName": "Chibolya Ward 14"
  },
  "status": "SUBMITTED",
  "createdAt": 1743436800000
}
```

---

### 4.3 Complaint Updates (`PATCH /complaints/{id}`)

When editing a complaint's location:
```typescript
if (req.body.location || req.body.locationName) {
  const newLocation = req.body.location || req.body.locationName;
  await db.query(
    "UPDATE complaints SET location = $1, updated_at = NOW() WHERE id = $2",
    [newLocation, req.params.id]
  );
}
```

---

## 5. Summary Checklist for Backend Engineers

- [ ] Ensure `location_id` in `complaints` table is `NULLABLE`.
- [ ] Remove any hardcoded `"Matero"` or `"Matero Ward 1"` default assignments in location resolution middleware.
- [ ] Store raw user-provided `location` string directly into `location` database column.
- [ ] Include `"location": "User Location String"` and `"locationName": "User Location String"` in all complaint JSON DTO responses.
