# Backend Implementation Specification: Sensitive Categories

This document provides exact backend REST API specifications for handling sensitive categories.

---

## 1. Sensitive Categories List

The following 6 categories are designated as **Sensitive Categories**:
1. `"Gender-Based Violence (GBV)"`
2. `"Sexual harassment"`
3. `"Human rights abuses"`
4. `"Child labour"`
5. `"Child abuse and neglect"`
6. `"Violence against children"`

---

## 2. Core Specification & Auto-Escalation

When a complaint is submitted under **ANY** of the above sensitive categories:
- **Automatic Priority**: Must be set to `"CRITICAL"` immediately upon receipt.
- **Privacy & Confidentiality**: Must be set to `"isConfidential": true` / `"isPrivate": true`.

---

## 3. Public Feed Exclusion Rule

> [!IMPORTANT]
> - Complaints under any sensitive category (or where `is_confidential = true`) **MUST BE EXCLUDED** from the public community feed endpoint (`GET /complaints`).
> - They must **ONLY** be accessible to:
>   1. Admins & admin approved organisations 
>   2. Authorized staff/official response dashboards.

---

## 4. Database Schema Updates

Ensure the `complaints` SQL table includes the `is_confidential` and `is_anonymous` boolean flags:

```sql
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS is_confidential BOOLEAN DEFAULT FALSE;
ALTER TABLE complaints ADD COLUMN IF NOT EXISTS is_anonymous BOOLEAN DEFAULT FALSE;
```

---

## 5. Backend Processing Logic (`POST /complaints`)

When receiving `POST /complaints` or multipart `POST /complaints`:

```typescript
const SENSITIVE_CATEGORIES = [
  "Gender-Based Violence (GBV)",
  "Sexual harassment",
  "Human rights abuses",
  "Child labour",
  "Child abuse and neglect",
  "Violence against children"
];

const category = req.body.category;
const isSensitive = SENSITIVE_CATEGORIES.includes(category);

// 1. Force CRITICAL priority for sensitive reports
const priority = isSensitive ? "CRITICAL" : (req.body.priority || "MEDIUM");

// 2. Force confidentiality
const isConfidential = isSensitive || req.body.isConfidential === true;

// 3. Handle Anonymous submissions
const isAnonymous = req.body.isAnonymous === true;
const authorName = isAnonymous ? "Anonymous Citizen" : (req.user?.name || "Citizen");
const authorEmail = isAnonymous ? null : (req.user?.email || null);
```

---

## 6. Summary Checklist for Backend Engineers

- [ ] Add all 6 sensitive categories to valid categories database list.
- [ ] Automatically set `priority = "CRITICAL"` for sensitive categories.
- [ ] Automatically set `is_confidential = true` for sensitive categories.
- [ ] Filter out `is_confidential = true` reports from public `GET /complaints` response.
- [ ] Support `isAnonymous: true` by masking user identity.
