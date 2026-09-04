# CONVENTIONS.md — নাম-অভিধান ও কোড-চুক্তি

**এই ফাইলের নাম/কলাম/স্বাক্ষর ভিন্ন করে লিখলে কোড ভাঙবে ও মিলবে না।** নতুন কিছু দরকার হলে:
আগে এখানে যোগ করো (DECISIONS.md-এন্ট্রিসহ), তারপর কোড। AI-সেশনের প্রথম কাজ এই ফাইল পড়া।

## ১. পরিচয়-স্বাক্ষর
- অ্যাপ-নাম: **Boi Khata (বই খাতা)** · প্যাকেজ/namespace/R: **com.boikhata** (com.example নিষিদ্ধ)
- থিম-কম্পোজেবল: **BoiKhataTheme** (BoikhataTheme নয়!)
- ডিভাইস-সত্য: Room; এন্টিটি-টেবিল-নাম নিচে; বিল্ডারে `.addMigrations(...)` বাধ্যতামূলক

## ২. এনাম (মান-সেট হুবহু — বাড়তি মান হ্যালুসিনেশন)
- Role: OWNER, MANAGER, SALES, ACCOUNTANT
- LicenseState: FULL, PAID_UNVERIFIED, GRACE, SOFT_LOCKED, SUSPENDED
- KhataEntryType: CREDIT, PAYMENT, ADJUSTMENT, OPENING
- CashbookAccount: CASH, BKASH, BANK
- CashbookEntryType: INCOME, EXPENSE, TRANSFER (⚠ ADJUSTMENT নেই!)
- PaymentMethod: CASH, BKASH, NAGAD, CREDIT
- BookCategory: TEXTBOOK, GENERAL, STATIONERY, OTHER
- StockChangeReason: SALE, PURCHASE, RETURN, ADJUSTMENT, MELA_IN, MELA_OUT
- BookCondition: NEW, USED, DAMAGED
- SupplierEntryType: OPENING, CONSIGNMENT, PURCHASE, PAYMENT, ADJUSTMENT
  (D51: OPENING=initial payable; CONSIGNMENT=goods on consignment→increases payable;
   PURCHASE=credit purchase→increases payable; PAYMENT=cash/MFS→decreases payable;
   ADJUSTMENT=±correction. Mirrors KhataEntryType with supplier-specific credits.)

## ৩. Room-স্কিমা v1 (টেবিল → কলাম; টাকার-টেবিল = ট্যাগড 🔒 append-only)

- tenants(id PK, name, phone, createdAt)
- users(id PK, tenantId, name, role, pinHash, salt, isActive, createdAt, updatedAt)
- devices(id PK, tenantId, label, isPrimary, isActive, boundAt)
- cloud_sync_state(id PK="primary", tenantId, cloudPhone, cloudRole, isPendingActivation,
  lastBackupAt, lastRestoreAt, lastCatalogSyncAt, licenseExpiresAt, licenseState=GRACE,
  updatedAt) — এক-সারি-টেবিল; আপসার্ট-বাধ্য
- books(id PK, tenantId, isbn?, titleBn, titleEn?, author, publisher, classLevel, subject,
  editionYear, category, condition, purchasePrice, sellingPrice, initialStock,
  lowStockThreshold, isActive, createdAt, updatedAt)
- stock_ledger 🔒(id PK, tenantId, bookId, changeQuantity, reason, referenceId?, userId,
  timestamp, idempotencyKey)
- bills(id PK, tenantId, billNumber, customerId?, customerNameBn, customerPhone?,
  userId, subtotal, discountAmount, discountType, vatAmount, totalAmount, paymentMethod,
  paidAmount, dueAmount, khataEntryId?, billDate, status, idempotencyKey)
- bill_lines(id PK, tenantId, billId, bookId, bookTitleBn, quantity, unitPrice, lineTotal, vatAmount)
- khata_customers(id PK, tenantId, nameBn, phone?, address?, creditLimit, isActive,
  createdAt, updatedAt)
- khata_entries 🔒(id PK, tenantId, customerId, amount, type, description, referenceBillId?,
  collectedByUserId, date, idempotencyKey)
- khata_installments(id PK, tenantId, customerId, khataEntryId, dueDate, amount, isPaid)
- expense_categories(id PK, tenantId, nameBn, icon, isActive)
- expenses(id PK, tenantId, categoryId, amount, description, expenseDate, receiptPhotoPath?,
  userId, idempotencyKey)
- cashbook_entries 🔒(id PK, tenantId, account, type, amount, description, referenceId?,
  date, userId, idempotencyKey)
- owner_drawings(id PK, tenantId, amount, description, drawingDate, userId, idempotencyKey)
- suppliers(id PK, tenantId, nameBn, phone?, settlementCycle, notes?)
- supplier_entries 🔒(id PK, tenantId, supplierId, amount, type, description, referenceId?,
  date, idempotencyKey) — type = SupplierEntryType name (D51)
- mela_sessions(id PK, tenantId, nameBn, location, startDate, endDate, isActive, isPaused,
  pauseReason?, createdAt, updatedAt) — D57; isPaused blocks new MELA_IN/MELA_OUT writes
- master_catalog(id PK, isbn?, titleBn, titleEn?, author, publisher, classLevel, subject,
  editionYear, mrp, isActive, lastUpdated)
- audit_logs 🔒(id PK, tenantId, userId, action, detail, timestamp) — LOCAL-ONLY, কখনো আপলোড নয়
- trial_redemptions(id PK, tenantId, deviceFingerprint, phoneHash, redeemedAt) — LOCAL-ONLY anti-farm fact

নিয়ম: টাকা=Double, তারিখ=epoch-millis Long, এনাম=String(name); প্রতিটি 🔒-টেবিলে idempotencyKey;
মাইগ্রেশন = Room-Migration ক্লাস, টেবিল-ড্রপ নিষিদ্ধ (নতুন টেবিল-যোগ বা ALTER-ADD কলাম)।

## ৪. রোল-ম্যাট্রিক্স (ডেটা-লেয়ার-গেট)
OWNER: সব · MANAGER: বিক্রি/স্টক/লাভ-দেখা; খাতা-দেখা+আদায়; খরচ/কাস্টমার-তৈরি নয় ·
SALES: নিজ-শিফট-বিল (টাকাসহ); লাভ/খরচ/খাতা/অন্য-শিফট নয় ·
ACCOUNTANT: হিসাব-সব; খাতা-দেখা+আদায়-রেকর্ড; বিল-এন্ট্রি/কাস্টমার-তৈরি নয়।
**khata_customers-তৈরি = OWNER-ONLY (রুলসও deny করে)।**

## ৫. Firestore-কালেকশন (ব্যাকআপ-স্কোপ; রুলস: Firebase-Project-Context.md)
books, stock_ledger, bills, bill_lines, khata_customers, khata_entries, expenses,
cashbook_entries, expense_categories, owner_drawings (🔒=create-only) +
license_records(ভেন্ডর-রাইট) + subscription_payments(PENDING-only) + masterCatalog(read)।
কোনো সময়ে audit_logs ক্লাউডে নয়।

## ৬. কোডিং-আইন
- Compose-only UI; প্রতি স্ক্রিন: ViewModel + StateFlow<UiState>; hiltViewModel()
- রিপোজিটরি-প্যাটার্ন: domain-ইন্টারফেস + data-ইম্প্ল + Hilt-বাইন্ডিং
- সব UI-টেক্সট strings.xml (bn-ডিফল্ট+en); হার্ডকোড-স্ট্রিং নিষিদ্ধ
- অঙ্ক-প্রদর্শন NumberFormatter দিয়েই; টাকা-ফরম্যাট: ৳ + বাংলা-অঙ্ক (টগল-সচেতন)
- ফাংশন ≤ ৪০ লাইন লক্ষ্য; ক্লাস-নাম PascalCase (BoiKhata…), রিসোর্স snake_case
- টাকার-মিউটেশন সবসময় রিপোজিটরি-দিয়ে + LicenseWriteGuard-অতিক্রম
- ত্রুটি-বার্তা বাংলা + অ্যাকশনযোগ্য; নীরব-ব্যর্থতা নিষিদ্ধ

## ৭. AI-সেশন-শৃঙ্খলা (গভর্নেন্স)
- স্কোপ-নিয়ম: প্রদত্ত-ফেজের আইটেমগুলোই বানাও; বাড়তি ফিচার = স্থগিত-তালিকায় প্রস্তাব
- মিথ্যা-সফলতা নিষিদ্ধ: "সেভ হয়েছে!" শুধু তখনই যখন সত্যিই সেভ
- কম্পাইল-দাবি নিষিদ্ধ যদি না স্যান্ডবক্সে চালানো হয়; অ-যাচাইকৃত = রিপোর্টে ফ্ল্যাগ
- ফাইল-পরিবর্তন = প্রতিটির পূর্ণ-পথ + এক-লাইন সারাংশ; শেষে Honest-Report টেবিল
- "সংবিধান পড়ো"-প্রম্পট ছাড়া সেশন শুরু নয়
```
