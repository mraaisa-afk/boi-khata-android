```
# BUILD.md — বই খাতা Master Android Build Architecture & Workflow
## বিল্ড-যন্ত্রের আইন: কীভাবে তৈরি, যাচাই, টেস্ট ও রিলিজ হয়

**Companions:** Blueprint (ব্যবসা) · ARCHITECTURE.md (কোড-আইন) · CONVENTIONS.md (নাম-অভিধান) ·
PROGRESS.md (ফেজ-চেকলিস্ট) · DECISIONS.md (সিদ্ধান্ত-লগ) · Firebase-Project-Context.md (ব্যাকএন্ড-সত্য)

**মালিকানা-নিয়ম:** প্রতিটি বিষয়ের একটাই মালিক-ফাইল (উপরের টেবিল)। এই ফাইল শুধু
বিল্ড-যন্ত্রের মালিক। অন্য ফাইলের বিষয়ে সংঘর্ষ দেখা দিলে ARCHITECTURE/CONVENTIONS জেতে —
এই ফাইল সংশোধিত হয়, কোড নয়।

---

## ১. বিল্ড-সিস্টেম কাঠামো

- **Gradle Kotlin DSL** সর্বত্র; ভার্সনের একমাত্র উৎস `gradle/libs.versions.toml` —
  ইনলাইন ভার্সন-স্ট্রিং নিষিদ্ধ (ARCHITECTURE.md §১)।
- রুট `build.gradle.kts`: প্লাগইন-ডিক্লারেশন (`apply false`) + `settings.gradle.kts`-এ
  ARCHITECTURE.md §২-এর মডিউলগুলোই include — বাড়তি মডিউল নিষিদ্ধ।
- প্রতি মডিউলের `build.gradle.kts` সাধারণ-ছাঁচ:
  - `compileSdk`/`targetSdk` = ৩৭, `minSdk` = ২৬, JDK ১৭ টুলচেইন
  - Compose চালু (`buildFeatures.compose = true`); Compose-কম্পাইলার Kotlin-প্লাগইনের
    অংশ (AGP 9 + Kotlin 2.4 যুগ) — পৃথক compilerExtension নয়
  - `testOptions.unitTests.isIncludeAndroidResources = true` (Robolectric)
- **`.env.example` রুটে বাধ্যতামূলক** (Secrets Gradle Plugin ফাইলটির অস্তিত্ব আশা করে —
  অনুপস্থিত হলে স্যান্ডবক্স-বিল্ডই ভাঙে)।

## ২. Firebase-ওয়্যারিং

- `app/google-services.json` → applicationId **com.boikhata** (CONVENTIONS §১)।
  Firebase প্রজেক্টে (boi-khata-app) ঠিক এই applicationId-ই রেজিস্টার্ড —
  **বিদ্যমান ফাইলই বৈধ; রিপো-বদলে কিছু করা লাগবে না।**
- ফাইলটি রিপোতে কমিট হয় না (.gitignore); প্রতি AI-সেশনে অ্যাটাচ করে `app/`-এ বসানো হয়।
- `google-services` প্লাগইন app-মডিউলে; Firebase ভার্সন BOM-দ্বারা পরিচালিত (catalog দেখো)।

## ৩. বিল্ড-ভ্যারিয়েন্ট ও সাইনিং

| ভ্যারিয়েন্ট | কাজ | সাইনিং |
|---|---|---|
| `debug` | ডেভ/এমুলেটর/স্যান্ডবক্স | অটো debug-keystore |
| `release` | প্রোডাকশন APK | ভেন্ডর-হোল্ড কীস্টোর — কীস্টোর/পাসওয়ার্ড কখনো রিপো/চ্যাটে নয় |

- release-এ `minifyEnabled` + R8 চালু; `proguard-rules.pro` খালি-সিড হিসেবে শুরু, ফেজভিত্তিক বাড়ে।
- `versionCode`: প্রতি রিলিজে +১ · `versionName`: `0.<phase>.<build>` (যেমন `0.4.1`)।

## ৪. AI-এজেন্ট বিল্ড-যাচাই লুপ (এই ফাইলের হৃদয়)

প্রতি কোড-লেখা সেশনের বাধ্যতামূলক সমাপ্তি-ক্রম:

1. **কম্পাইল চালাও** (স্যান্ডবক্স কম্পাইলার/বিল্ড-টুল দিয়ে)।
2. ফল হুবহু রিপোর্ট করো: `Build succeeded` **অথবা** সম্পূর্ণ error-আউটপুট।
3. **কম্পাইল না-চালিয়ে সফলতা-দাবি নিষিদ্ধ** — এই প্রজেক্ট-পরিবারের সবচেয়ে দামি শিক্ষা।
4. কম্পাইল ব্যর্থ? সেশন সেখানেই থামাও — ভাঙা অবস্থায় কমিট কখনো নয়; ফিক্স পরের ধাপে।
5. ফেজ-শেষে (PROGRESS-এর exit-gate): ইউনিট-টেস্টও চালাও + PASS/FAIL-টেবিল।
6. রিপোর্টে যা যাচাই করা যায়নি (যেমন আসল-ফোনে OTP), স্পষ্টভাবে ফ্ল্যাগ করো।

## ৫. টেস্ট-কৌশল ও স্যুট-ম্যাপ

পিরামিড: ইউনিট (ডোমেইন-ইঞ্জিন) → DAO/রিপো (Room-in-memory/Robolectric) → Compose UI
(মূল-ফ্লো) → ম্যানুয়াল-চেকলিস্ট। Firestore = ইন্টিগ্রেশন-ম্যানুয়াল (P4-লাইভ-চেইন), অটো-টেস্ট নয়।

| স্যুট | যা যাচাই করে | কোন exit-gate |
|---|---|---|
| `LicensePolicyTest` | গ্রেস-সীমানা, উৎসব-উইন্ডো, ৩৫-দিন-ডিগ্রেড, paid-কখনো-readonly-নয় | P1 |
| `AgingCalculatorTest` | FIFO-অ্যালোকেশন, অ্যাডজাস্টমেন্ট-সহ | P1 |
| `PnLCalculatorTest` | কনসাইনমেন্ট/ক্রয়-COGS-স্প্লিট, বাংলা-বর্ষ-রোলআপ | P3 |
| `BackupMapperTest` | এন্টিটি↔ম্যাপ, "Negative Adj:"-উভয়-দিক (আপলোড-প্রিফিক্স/রিস্টোর-আনফ্লিপ) | P4 |
| `OfflineSoakTest` | ৩০দি/~৪,৫০০-ইভেন্ট DB-সাইজ বাজেট (ARCHITECTURE §৭) | P7 |

নামকরণ: `<ClassUnderTest>Test`; মেথড: `should <expected> when <condition>`।
fake/stub টেস্ট-মডিউলেই; প্রোডাকশন-কোডে টেস্ট-শর্টকাট নিষিদ্ধ।

## ৬. CI (GitHub Actions — ন্যূনতম-সোলো)

প্রতি PR: checkout → JDK ১৭ → `gradlew build` (assemble + unit tests)। main-এ মার্জ = সবুজ বাধ্যতামূলক।
(detekt/ktlint/ডিভাইস-ম্যাট্রিক্স = ভবিষ্যৎ, DECISIONS-এন্ট্রির মাধ্যমেই যোগ হবে।)

## ৭. ডিপেন্ডেন্সি-নীতি

১. ভার্সন শুধু ক্যাটালগে। ২. নতুন ডিপেন্ডেন্সি = আগে DECISIONS.md (কেন + বিকল্প কী হারল)।
৩. `⚠ VERIFY` এন্ট্রি Phase-0-এর আগে লাইভ-উৎসে যাচাই — অনুমানে বসানো নিষিদ্ধ।
৪. যে লাইব্রেরি ব্যবহার হয় না, সেটা ক্যাটালগে থাকবে না।

## ৮. রিলিজ-ওয়ার্কফ্লো

ট্যাগ `v<versionName>` → release-বিল্ড → R8 → zipalign → apksigner → সরাসরি-সাইনড-APK
(এজেন্ট-চ্যানেল; Play = ভবিষ্যৎ)।
সানসেট: ৬-মাস-পুরনো ভার্সনে sync-বন্ধ, এক্সপোর্ট সর্বদা-খোলা (P8-এ বাস্তবায়ন)।
চেঞ্জ-ফ্রিজ: বইমেলা (২১দি), ঈদ-সপ্তাহ, ১–১৫ জানু (Blueprint §৬-ক্যালেন্ডার)।

## ৯. পরিবেশ

- ডেভ = লোকাল/AI-স্যান্ডবক্স + এমুলেটর-প্রিভিউ।
- লাইভ = একটাই Firebase প্রজেক্ট (boi-khata-app) — পৃথক staging নেই (Spark-সরলতা);
  **ধ্বংসাত্মক-টেস্ট নিষিদ্ধ**; পরীক্ষা হয় আলাদা tenantId-র টেস্ট-টেন্যান্টে।
```
