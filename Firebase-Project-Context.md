# Boi Khata — Firebase Project Context v1.0
> Attach alongside: System Instructions + Boi-Khata-Technical-Spec-v6.md.
> This file documents the LIVE Firebase backend. The app must COMPLY with it —
> never propose changing rules to fit app code. Rules are correct by definition.

## 1. Project Identity
- Firebase Console project: **boi-khata-app** (owner: mraaisa@gmail.com)
- Vendor: Mohsin Ul Hasan / Raisa Trading House / +8801711468027
- Android applicationId: **com.boikhata** (MUST match google-services.json)
- google-services.json: attach in chat → place at app/google-services.json
  (NOT a secret file; repo-safe. Real secrets live only in vendor-side keys.)
- Firestore: Standard edition, region **asia-south1 (Mumbai)** — IMMUTABLE, chosen deliberately
- Firestore mode: locked (deny-by-default)
- Auth: **Phone provider ENABLED** (OTP, +880 numbers)
- Real-device OTP requires the app's SHA-1 fingerprint in Console →
  Project Settings → Your apps → Add fingerprint (add AFTER first APK build)

## 2. Cloud Architecture (LOCKED)
- OFFLINE-FIRST: Room is the single source of truth. Firebase adds identity +
  license + backup ONLY. App must be 100% functional in airplane mode.
- Identity: Phone OTP → ID token carries custom claims {tenantId, role}
  (claims are set VENDOR-SIDE via Admin SDK — the app NEVER writes claims)
- Roles: OWNER / MANAGER / SALES / ACCOUNTANT
- Tenant isolation: ALL business data lives in TOP-LEVEL collections; every
  document carries tenantId matching claims; document IDs = local UUIDs
- License lifecycle (§8.3): ACTIVE → GRACE (+14d) → SOFT_LOCKED (+30d:
  writes blocked, reads/exports open — NEVER-LOCK rule) → SUSPENDED
- /license_records/{tenantId}: doc ID = tenantId; fields: tenantId, state,
  expiresAt (Firestore **Timestamp** — vendor scripts write it), updatedAt

## 3. Collection Inventory & Rules
| Collection | Write policy |
|---|---|
| books, bills, bill_lines, khata_customers, expenses, expense_categories | tenant-scoped set() allowed per role matrix |
| khata_entries, stock_ledger, cashbook_entries | **APPEND-ONLY** (client update/delete DENIED — create only) |
| license_records, wallet_transactions, activation_redemptions, local_audit_logs | **SERVER-ONLY** (all client writes denied) |
| masterCatalog | read: any authenticated; write: false (Admin SDK bypasses rules) |
| subscription_payments | client create ONLY with status == 'PENDING' |

## 4. LIVE SECURITY RULES (authoritative — published in Firebase Console)
> Repo may keep a reference copy (app/applet/firestore.rules) — console is truth.

rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {

    // ================== 1. TENANTS ==================
    match /tenants/{tenantId} {
      allow read: if isTenantUser(tenantId);
      allow write: if isTenantUser(tenantId) && isOwner();
    }

    // ================== 2. USERS ==================
    match /users/{userId} {
      allow read: if isSameTenantResource() && (isOwner() || isManager() || request.auth.uid == userId);
      allow write: if isSameTenantIncoming() && isOwner();
    }

    // ================== 3. CATALOG & STOCK ==================
    match /books/{bookId} {
      allow read: if isSameTenantResource();
      allow write: if isSameTenantIncoming() && (isOwner() || isManager());
    }

    match /stock_ledger/{entryId} {
      allow read: if isSameTenantResource() && (isOwner() || isManager() || isSales());
      allow create: if isSameTenantIncoming() && (isOwner() || isManager() || isSales());
      allow update, delete: if false; // append-only ledger
    }

    // ================== 4. BILLING ==================
    match /bills/{billId} {
      allow read: if isSameTenantResource() && (
        isOwner() || isManager() || isAccountant() ||
        (isSales() && resource.data.userId == request.auth.uid)
      );
      allow create: if isSameTenantIncoming() && (isOwner() || isManager() || isSales());
      allow update, delete: if isSameTenantResource() && isOwner();
    }

    match /bill_lines/{lineId} {
      allow read: if isSameTenantResource() && (isOwner() || isManager() || isAccountant() || isSales());
      allow create: if isSameTenantIncoming() && (isOwner() || isManager() || isSales());
      allow update, delete: if isSameTenantResource() && isOwner();
    }

    // ================== 5. KHATA ==================
    match /khata_customers/{customerId} {
      allow read: if isSameTenantResource() && (isOwner() || isManager() || isAccountant());
      allow create: if isSameTenantIncoming() && isOwner(); // OWNER-ONLY (locked design)
      allow update, delete: if false;
    }

    match /khata_entries/{entryId} {
      allow read: if isSameTenantResource() && (isOwner() || isManager() || isAccountant());
      // OWNER: any type; MANAGER & ACCOUNTANT: PAYMENT-only;
      // SALES: CREDIT-only when tied to a bill; amount must be > 0
      // (backup stores negatives as magnitude + "Negative Adj: " prefix)
      allow create: if isSameTenantIncoming() && isValidAmount() && (
        isOwner() ||
        ((isManager() || isAccountant()) && request.resource.data.type == 'PAYMENT') ||
        (isSales() && request.resource.data.type == 'CREDIT' && request.resource.data.referenceBillId != null)
      );
      allow update, delete: if false;
    }

    // ================== 6. ACCOUNTING ==================
    match /expenses/{expenseId} {
      allow read: if isSameTenantResource() && (isOwner() || isAccountant());
      allow write: if isSameTenantIncoming() && (isOwner() || isAccountant());
    }

    match /cashbook_entries/{entryId} {
      allow read: if isSameTenantResource() && (isOwner() || isAccountant());
      allow create: if isSameTenantIncoming() && (isOwner() || isAccountant()) && isValidAmount();
      allow update, delete: if false;
    }

    match /expense_categories/{categoryId} {
      allow read: if isSameTenantResource() && (isOwner() || isAccountant());
      allow create: if isSameTenantIncoming() && (isOwner() || isAccountant());
      allow update, delete: if isSameTenantResource() && (isOwner() || isAccountant());
    }

    match /owner_drawings/{drawingId} {
      allow read: if isSameTenantResource() && isOwner();
      allow create: if isSameTenantIncoming() && isOwner();
      allow update, delete: if false;
    }

    // ================== 7. MASTER CATALOG ==================
    match /masterCatalog/{bookId} {
      allow read: if isAuthenticated();
      allow write: if false;
    }

    // ================== 8. SUBSCRIPTION / LICENSE / WALLET ==================
    match /license_records/{licenseId} {
      allow read: if isSameTenantResource() && isOwner();
      allow write: if false; // vendor scripts only
    }

    match /subscription_payments/{paymentId} {
      allow read: if isSameTenantResource() && isOwner();
      allow create: if isSameTenantIncoming() && isOwner() && request.resource.data.status == 'PENDING';
      allow update, delete: if false;
    }

    match /wallet_transactions/{transactionId} {
      allow read: if isSameTenantResource() && isOwner();
      allow write: if false;
    }

    match /activation_redemptions/{redemptionId} {
      allow read, write: if false;
    }

    // ================== 9. AUDIT LOGS ==================
    match /local_audit_logs/{logId} {
      allow read: if isSameTenantResource() && isOwner();
      allow write: if false;
    }

    // ================== DEFAULT DENY ==================
    match /{document=**} {
      allow read, write: if false;
    }
  }

  // ================== HELPERS ==================
  function isAuthenticated() {
    return request.auth != null && request.auth.uid != null;
  }
  function getTenantId() { return request.auth.token.tenantId; }
  function getRole() { return request.auth.token.role; }
  function isTenantUser(tenantId) {
    return isAuthenticated() && getTenantId() == tenantId;
  }
  function isSameTenantResource() {
    return isAuthenticated() && resource.data.tenantId == getTenantId();
  }
  function isSameTenantIncoming() {
    return isAuthenticated() && request.resource.data.tenantId == getTenantId();
  }
  function isOwner() { return isAuthenticated() && getRole() == 'OWNER'; }
  function isManager() { return isAuthenticated() && getRole() == 'MANAGER'; }
  function isSales() { return isAuthenticated() && getRole() == 'SALES'; }
  function isAccountant() { return isAuthenticated() && getRole() == 'ACCOUNTANT'; }
  function isValidAmount() {
    return request.resource.data.amount is number && request.resource.data.amount > 0;
  }
}

## 5. Vendor-Side Tooling (PC, not in repo)
- Node.js + firebase-admin scripts at F:\boi_khata\vendor_tools\:
  - `node activate.js +8801XXXXXXXXX "দোকানের নাম" [days=30]` — provisions
    tenant + user + license + custom claims (first-time customer activation)
  - `node renew.js +8801XXXXXXXXX [days=30]` — extends expiresAt after
    verifying bKash payment manually (Phase 0a manual model — NO gateway)
- `serviceAccountKey.json` = MASTER KEY. NEVER commit to any repo or chat.
- Subscription model: customer pays ৳250/month via bKash to vendor's personal
  number (+8801711468027); vendor verifies and runs renew.js. TrxID is
  OPTIONAL in-app (never require it).

## 6. Hard-Won Integration Constraints (from Phase 5 audits — VIOLATING THESE = KNOWN BUGS)
1. `Query.select()` does NOT exist in the Android Firestore SDK — never use it
   for existence checks (fetch existing doc IDs with a plain get() instead)
2. `toObjects()` requires no-arg constructors — our data classes don't have
   them → ALWAYS map documents field-by-field manually
3. `whereEqualTo + orderBy` combos require composite indexes → avoid orderBy,
   sort client-side
4. Firestore rules deny amount <= 0 → negative ADJUSTMENT entries must be
   uploaded as magnitude with "Negative Adj: " description prefix, and
   restore must reverse the sign + strip the prefix
5. A Firestore WriteBatch is ATOMIC — one denied write fails the whole batch
   → commit per-collection, ≤450 ops per batch
6. Backup must be INCREMENTAL (lastBackupAt filter) because re-uploading an
   existing doc = update = DENIED on append-only collections
7. expiresAt arrives as Firestore Timestamp → parse getTimestamp() with
   getLong() fallback; ALWAYS check snapshot.exists() (missing doc throws
   NO exception — the catch block alone is insufficient)
8. License read is OWNER-only in rules → gate license sync on role == OWNER;
   non-owners use the locally cached state (offline-first)
9. RBAC enforced at the DATA layer (repositories), never UI-only:
   khata customer create/manual credit = OWNER-only; record payment =
   OWNER/MANAGER/ACCOUNTANT; SALES = own bills only
10. Khata due aging = FIFO from the customer's OLDEST UNPAID entry
    (payments net against oldest credits first), NOT account creation date;
    colors: 🟢 <15d · 🟡 15–30d · 🔴 >30d
11. AI Studio builds expect an `.env.example` file at repo root (Secrets
    Gradle plugin) — keep it (may be empty)
12. One-time tenant rebind: local rows start as tenantId "t_1"; on FIRST
    successful cloud login, migrate all local rows to the claims tenantId
    in one Room transaction BEFORE the first backup (else backup is empty)

## 7. Console Quick-Map
- Rules: Console → Firestore Database → Rules
- Phone Auth: Console → Authentication → Sign-in method → Phone
- App + SHA-1: Console → Project Settings → Your apps (com.boikhata)
- Service account key (vendor): Console → Project Settings → Service accounts
```
