```
# ARCHITECTURE .md — বই খাতা (Agent Reference)

**প্রতি সেশনের শুরুতে পুরোটা পড়ো।** এই ফাইল = কোড-স্তরের সিদ্ধান্তের আইন। ব্যবসা-স্তরের আইন
`Boi-Khata-Master-Blueprint.md`; লাইভ ব্যাকএন্ডের সত্য `Firebase-Project-Context.md`। নাম/চুক্তি
`CONVENTIONS.md`। কোনো টেবিলের সাথে সংঘর্ষ হলে: **থেমে যাও, DECISIONS.md-তে এন্ট্রি করো, তারপর কোড।**

**মেটা-আইন:** এই ফাইলের কোনো টেবিলের বিরোধী কিছু লিখতে যাচ্ছিলে স্টপ — আগে ডকুমেন্ট আপডেট।

---

## ১. স্ট্যাক (লকড)

| স্তর | পছন্দ | বিকল্প নিষিদ্ধ |
|---|---|---|
| ভাষা/UI | Kotlin + Jetpack Compose (Material 3), minSdk 26 | Java নয়, XML-view নয় |
| লোকাল DB | Room (সত্যের একমাত্র উৎস) | Realm/raw-SQLite নয় |
| ক্লাউড | **Firebase: Auth (Phone) + Firestore** — শুধু পরিচয়+লাইসেন্স+ব্যাকআপ+মাস্টার-ক্যাটালগ | কাস্টম REST-সার্ভার নয়; Crashlytics/FCM/Functions/Analytics নয় (Spark-ফ্রি-শর্ত) |
| DI | Hilt | Koin/ম্যানুয়াল নয় |
| ব্যাকগ্রাউন্ড | WorkManager | — |
| নেটওয়ার্ক | Firestore SDK সরাসরি | OkHttp/Retrofit দরকার নেই (PE-স্তরে) |
| ফন্ট | Noto Sans Bengali APK-তে বান্ডেল | Play-Fonts নয় (অফলাইন-শর্ত) |
| বিল্ড | Gradle KTS + ভার্সন-ক্যাটালগ; ইনলাইন-ভার্সন-স্ট্রিং নিষিদ্ধ | |

## ২. মডিউল-ম্যাপ (এই তালিকার বাইরে মডিউল নিষিদ্ধ)

```
app/                    shell, Hilt, Navigation-Compose, app-lock হোস্ট
core/database/          Room স্কিমা, DAO, মাইগ্রেশন, ব্যাকআপ-সিরিয়ালাইজার
core/domain/            এন্টিটি-মডেল, রিপোজিটরি-ইন্টারফেস, ডোমেইন-সার্ভিস (LicensePolicy, AgingCalculator)
core/cloud/             FirebaseAuth/Firestore ইম্প্ল, টেন্যান্ট-সেশন
core/designsystem/      থিম, বাংলা-ফন্ট, NumberFormatter (০১২/012), Lite-UI
core/common/            স্ট্রিং/ডেট-টুল, Result-র‍্যাপার
feature/home|sale|catalog|khata|expense|supplier|reports|subscription|melamode|support/
shared/receipt/         WhatsApp টেক্সট/PNG বিল্ডার (দ্বৈত-অঙ্ক)
vendorapp/              ভবিষ্যৎ (এখন নয়)
```
নিয়ম: `feature/*` শুধু `core/*`+`shared/*`-কে দেখে; feature↔feature সরাসরি নিষিদ্ধ।

## ৩. তীর্ণ-সংঘর্ষ-নিরসন (আইন, পরামর্শ নয়)

| # | ভুল-পথ | সঠিক-পথ |
|---|---|---|
| C1 | টাকার-লেজারে ওভাররাইট/ডিলিট | Append-only: ভুল হলে নতুন রিভার্সাল-এন্ট্রি; UPDATE/DELETE নিষিদ্ধ (রুলসও deny করে) |
| C2 | ব্যাকআপে প্রতিবার পুরো-আপলোড | ইনক্রিমেন্টাল (lastBackupAt ফিল্টার); append-only কালেকশনে create-only; **প্রতি-কালেকশন আলাদা ব্যাচ ≤৪৫০** — এক denied-রাইট পুরো ব্যাচ ভাঙে |
| C3 | Firestore-ডক → data-class-এ toObjects() | হাতে-ফিল্ড-বাই-ফিল্ড ম্যাপিং (no-arg কনস্ট্রাক্টর নেই) |
| C4 | whereEqualTo+orderBy একসাথে | orderBy বাদ; ক্লায়েন্ট-সাইড সর্ট (কম্পোজিট-ইনডেক্স এড়াও) |
| C5 | Query.select() ব্যবহার | Android SDK-তে নেই — প্লেইন get() দিয়ে doc-ID-সেট আনো |
| C6 | expiresAt-পড়া getLong() দিয়ে | getTimestamp()?.toDate()?.time, ফলব্যাক getLong(); **snapshot.exists() বাধ্যতামূলক** (ডক নেই = exception নয়) |
| C7 | মাইনাস-অ্যাডজাস্টমেন্ট ক্লাউডে যাওয়া | magnitude + "Negative Adj: " প্রিফিক্স আপলোডে; রিস্টোরে সাইন-আনফ্লিপ + প্রিফিক্স-স্ট্রিপ |
| C8 | রোল-চেক শুধু UI-তে | ডেটা-লেয়ারে (রিপোজিটরিতে) — রোল-ম্যাট্রিক্স CONVENTIONS §৪ |
| C9 | লাইসেন্স-স্টেট UI-তে হার্ডকোড | Room cloud_sync_state থেকে প্রবাহিত; ACTIVE-ডিফল্ট নিষিদ্ধ (GRACE-ডিফল্ট) |
| C10 | com.example কোথাও | namespace+R-import = com.boikhata |

## ৪. ডেটা-লেয়ার চুক্তি

- **লোকাল = সত্য:** প্রতিটি রাইট: Room-ট্রানজেকশনে (এন্টিটি-ইনসার্ট + ভিউ-রিফ্রেশ-প্রয়োজনে + আউটবক্স/ফ্ল্যাগ)।
- **ক্লাউড-লেয়ার:** ব্যাকআপ-রিপোজিটরি (ইনক্রিমেন্টাল, টপ-লেভেল কালেকশন, tenantId-প্রতি-ডক) · লাইসেন্স-রিপো (OWNER-gated) · অথ-রিপো (OTP+claims) · মাস্টার-ক্যাটালগ-রিপো (read-only)। ক্লাউড-রাইট শুধু ব্যাকআপ+subscription_payments(PENDING)।
- **Balances = ডেরাইভড:** SUM-কোয়েরি/ইন-মেমরি গণনায় (CREDIT − PAYMENT ± ADJUSTMENT/OPENING); stored-balance নয়।
- **Aging = FIFO:** পেমেন্ট প্রাচীনতম CREDIT-এ কাটে; প্রথম অপরিশোধিত CREDIT-এর তারিখ থেকে দিন।

## ৫. লাইসেন্সিং (লোকাল-ইঞ্জিন, ৩টি স্বাধীন-পরীক্ষাযোগ্য ফাংশন)

```
fun isInsideFestivalWindow(windows, now): Boolean
fun evaluateGrace(lastVerified, lastPayment, now): GraceState   // max(v,p)+14d
fun evaluateContactDegrade(lastServerContact, now): Boolean    // >35d → WARN
স্টেট: FULL / PAID_UNVERIFIED / GRACE / SOFT_LOCKED(write-only-blocked) / SUSPENDED
প্রদত্ত-টেন্যান্ট কখনো READONLY নয়; পড়া/এক্সপোর্ট সর্বদা খোলা (never-lock)।
```
ক্লাউড-স্টেট = license_records/{tenantId}; অফলাইন-ফলব্যাক = শেষ-জানা লোকাল-স্টেট (কখনো ACTIVE-বানানো নয়)।
`LicenseWriteGuard` — সব রাইট-রিপোজিটরিতে ইনজেক্টেড একক-গেট; SOFT_LOCKED/SUSPENDED → LicenseBlockedException।

## ৬. RBAC

লোকাল-ক্যাশড রোল→পারমিশন; ক্লাউড-claims {tenantId, role} ক্লাউড-অপসের জন্য, লোকাল-PIN-role লোকাল-RBAC-এর জন্য। রোল-গেট রিপোজিটরিতে (`requireRole(...)`)। ম্যান্ডেটরি app-lock: PIN/বায়োমেট্রিক নন-OWNER-রোলে; ২-মিনিট অটো-লক।

## ৭. CI-বাজেট (সত্যিকার টেস্ট-অ্যাসার্শন, মন্তব্য নয়)

| বাজেট | সীমা |
|---|---|
| APK সাইজ | < ৩০MB |
| কোল্ড-স্টার্ট (2GB) | < ৪ সে |
| রানটাইম মেমরি | < ১৫০MB |
| অফলাইন-সোক (৩০দি, ~৪,৫০০ ইভেন্ট) DB সাইজ | ≈ ৩–৫MB |
| ব্যাকআপ-ব্যাচ | ≤ ৪৫০ অপ/কালেকশন |

## ৮. নিষিদ্ধ-প্যাটার্ন (রিভিউয়ার-গ্রেপ, প্রতি PR-এ)

TODO()/NotImplementedError · ফেক-ডেটা-সফলতা-স্টাব · @Suppress-দিয়ে সত্যিকার ওয়ার্নিং চাপা · ইনলাইন ভার্সন-স্ট্রিং · feature↔feature import · main-এ সরাসরি কমিট (PR-অনলি) · হার্ডকোডেড UI-টেক্সট (strings.xml-বাধ্যতামূলক) · লোকাল-রিপোতে google-services.json কমিট করা ছাড়া বিল্ড দাবি · টাকার-টেবিলে UPDATE/DELETE · com.example.* · "Negative Adj:"-প্রিফিক্স-না-জানা রিস্টোর-ম্যাপার।

## ৯. সেশন-প্রোটোকল

শুরুতে: ARCHITECTURE + CONVENTIONS + PROGRESS + DECISIONS পড়ো → PROGRESS-এর প্রথম অ-চেকড আইটেম ধরো। শেষে: সম্পন্ন-আইটেম চেক + এই ফাইলগুলো কমিট (সেশনের শেষ অ্যাকশন)। এক ফেজ = সেশনের-মতো, এক PR।

*জীবন্ত-নথি: নতুন সিদ্ধান্ত → এই ফাইল + DECISIONS.md একই PR-এ।*
```
