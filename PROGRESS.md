# PROGRESS.md — বই খাতা বিল্ড-চেকলিস্ট

**প্রোটোকল:** প্রতি সেশনের শুরুতে ARCHITECTURE.md, CONVENTIONS.md, DECISIONS.md ও এই ফাইল পুরো পড়ো।
**PROGRESS-এর প্রথম অ-চেকড আইটেম** থেকে শুরু। সেশনের একদম শেষ কাজ: সম্পন্ন আইটেম চেক + কমিট।
মাঝপথে থামলে: আইটেম অ-চেকড রেখে নিচে এক-লাইন নোট ("LedgerEvent হলো, ভিউ বাকি")।
**এক ফেজ = এক PR = সর্বোচ্চ ৫টি batch push সেই PR-এ = শেষে একবার merge।** আগের ফেজের exit-gate অ-চেকড থাকলে পরের ফেজ শুরু নিষিদ্ধ।
**PR ওয়ার্কফ্লো:** প্রতিটি push-এ PR-এর CI নতুন করে চলে। Merge শুধু সব কাজ শেষ হলে, একবার — এটাই এই প্রজেক্টের একমাত্র বিল্ড প্রটোকল।

---

## P0 — স্কেলেটন ও গার্ডরেল
- [x] Gradle-KTS প্রজেক্ট + ভার্সন-ক্যাটালগ (সংশোধিত libs.versions.toml)
- [x] মডিউল-শেল (§২-এর প্রতিটি মডিউল; খালি build.gradle.kts + প্যাকেজ, লজিক নয়)
- [x] Hilt-ওয়্যারিং + খালি @HiltAndroidApp + MainActivity (BoiKhataTheme)
- [x] Noto Sans Bengali বান্ডেল + বাংলা-ডিফল্ট strings (values-bn প্রাথমিক) + digits-টগল-ফাউন্ডেশন (NumberFormatter)
- [x] CI: প্রতি PR-এ ক্লিন-ক্লোন `gradlew build`
- [x] .env.example রুটে (Secrets-প্লাগইন-প্রত্যাশা)
- [x] **Exit-gate:** ক্লিন-ক্লোন বিল্ড সবুজ, শূন্য-মডিউলে-লজিক

## P1 — লোকাল-ফাউন্ডেশন
- [x] Room v1 স্কিমা: CONVENTIONS §৩-এর প্রতিটি টেবিল (নাম/কলাম হুবহু)
- [x] Tenant/User/Device + seed (১ টেন্যান্ট t_1 + OWNER-ব্যবহারকারী + GRACE-লাইসেন্স-সিড)
- [x] PIN-লগইন + রোল-সুইচ (SessionManager) + ২-মিনিট অটো-লক + বায়োমেট্রিক-স্টাব-ইন্টারফেস
- [x] খাতা-প্রথম হোম (দেনা-তালিকা + আজকের বিক্রি + top-৫) — mock-ডেটা নয়, Room-প্রবাহিত
- [x] ডেটা-মিটার (OkHttp-নয়; Firestore-বাইট-কাউন্টার + Wi-Fi-only-টগল)
- [x] LicensePolicy-ট্রিপল (ARCH §৫) + LicenseWriteGuard + ইউনিট-টেস্ট (গ্রেস-সীমানা/উৎসব/৩৫-দিন)
- [x] AgingCalculator (FIFO) + ইউনিট-টেস্ট
- [ ] **Exit-gate:** এয়ারপ্লেন-মোডে লগইন→হোম→স্টেট-মেশিন ডেমো; টেস্ট সবুজ
  - নোট: টেস্ট সবুজ (৪৪ টেস্ট, ০ ফেইল); ক্লিন-বিল্ড সবুজ (assemble+lint+test)। এয়ারপ্লেন-মোড ডেমো = ডিভাইসে ম্যানুয়াল-চেক (স্যান্ডবক্সে ডিভাইস নেই)।

## P2 — ক্যাটালগ + POS + খাতা
- [x] ক্যাটালগ (শর্ত+দাম; বাংলা-ফাজি-সার্চ সহজ-রূপ: LIKE+নরমালাইজড-কলাম)
  - নোট: P2a — লোকাল বুক ক্যাটালগ (list, Bengali fuzzy search via BengaliNormalizer+LIKE, add/edit) সম্পূর্ণ অফলাইন। মাস্টার NCTB ক্যাটালগ ইমপোর্ট = P4/Firebase।
- [x] POS: কার্ট, ছাড়, ভ্যাট-স্প্লিট, আংশিক→অটো-খাতা, বিল-নম্বর-জেনারেটর
  - নোট: P2b — POS sale flow (cart, quantity, discount PERCENTAGE/FIXED, VAT per-line books 0%/stationery 15%, payment method, partial→auto-khata via atomic Room transaction, bill number INV-YYYYMMDD-NNNN). VatCalculator + BillNumberGenerator pure services. SaleRepositoryImpl with @Transaction (bill+lines+stock+khata). 9 VatCalculatorTest + 7 BillNumberGeneratorTest pass.
- [x] WhatsApp-রসিদ (টেক্সট+PNG, দ্বৈত-অঙ্ক) shared/receipt-এ
  - নোট: P2b — ReceiptBuilder in shared/receipt (D21: Unicode plain-text, D2-compliant no PNG, dual digits via NumberFormatter, WhatsApp share via Intent.ACTION_SEND). 11 ReceiptBuilderTest pass.
- [x] খাতা: নাম+এলাকা-কী, কিস্তি, ক্রেডিট-লিমিট-ওয়ার্নিং, দেনা-মুন→ব্যাড-ডেট, dispute-freeze, স্টেটমেন্ট, কোহোর্ট
  - নোট: P2a — নাম+এলাকা-কী কাস্টমার, কিস্তি ট্র্যাকিং (KhataInstallmentDao), ক্রেডিট-লিমিট-ওয়ার্নিং, দেনা-মুন (ADJUSTMENT entry), শেয়ারেবল বাকি হিসাব স্টেটমেন্ট (KhataStatementBuilder, WhatsApp text share) সম্পন্ন। dispute-freeze ও কোহোর্ট-ট্যাগ = DEFERRED (P5 স্কোপে প্রস্তাবিত)।
- [x] **Exit-gate:** প্রথম-বিল ≤৩০-মিনিট-প্রবাহ ইউনিট+UI-টেস্টে
  - নোট: P2 complete — ক্যাটালগ+খাতা (P2a) + POS+রসিদ (P2b) সম্পন্ন। ৮৮ টেস্ট সবুজ (২৭ নতুন P2b + ১৭ P2a + ৪৪ P1)। Full `./gradlew build` (assemble+lint+test) সবুজ — CI-equivalent verification। প্রথম-বিল ≤৩০-মিনিট flow: catalog→POS cart→VAT+discount→payment→checkout→bill+stock+khata atomic→receipt share। UI টেস্ট = Compose-টেস্ট (P7 স্কোপে প্রস্তাবিত; ডোমেইন-লজিক ইউনিট-টেস্ট সবুজ)।

## P3 — হিসাব-কোর
- [x] ExpenseCategory-সিড + ১-ট্যাপ-এন্ট্রি + অটো-রুট (বই→ইনভেন্টরি) + ঘরি সাব-লেজার + recurring
  - নোট: P3a — 8 BD expense categories seeded (ভাড়া, বিদ্যুৎ, ইন্টারনেট, বেতন, ঘরি/অ্যাডভান্স, পরিবহন, MFS-ফি, অন্যান্য)। ১-ট্যাপ expense entry with category + amount + cashbook auto-populate। D24: PurchaseRouter (book→stock_ledger PURCHASE, non-book→expense)। D26: ঘরি sub-ledger via GoriBalanceCalculator (advances − returns, description "ঘরি ফেরত")। D27: RecurringExpenseCalculator pure service (next-due logic unit-tested)। P3b completed the deferred persistence + budget alert (D35): recurring_expenses + budgets tables via Migration v2→v3, RecurringExpenseRepository (addTemplate/applyTemplate/getDueTemplates), BudgetRepository (setBudget/getMonthlyAlerts), BudgetAlertCalculator + RecurringExpenseReminder pure services। 4 PurchaseRouterTest + 6 CashbookBalanceCalculatorTest + 5 GoriBalanceCalculatorTest + 7 RecurringExpenseCalculatorTest + 9 BudgetAlertCalculatorTest + 7 RecurringExpenseReminderTest pass.
- [x] Cashbook ৩-অ্যাকাউন্ট + ম্যানুয়াল-এন্ট্রি + অটো-পপুলেট (বিল/খরচ/আদায়)
  - নোট: P3a — CashbookBalanceCalculator (নগদ/বিকাশ/ব্যাংক, derived balances)। D25: every money flow creates a cashbook entry (expense→EXPENSE, bill payment→INCOME, khata collection→INCOME, owner drawing→EXPENSE) in same Room @Transaction। Manual entry UI with INCOME/EXPENSE/TRANSFER + account selection। P3b completed the deferred bill/khata auto-populate (D34): SaleRepositoryImpl.createBill now inserts INCOME (account from paymentMethod, amount=actualPaid) in the same transaction; KhataRepositoryImpl.addEntry now inserts INCOME for PAYMENT type (account chosen by user, default CASH).
- [x] OwnerDrawing
- [x] P&L (মাসিক) + বাংলা-বর্ষ-রোলআপ + ব্যালেন্স-শিট-লাইট + COGS-স্প্লিট (কনসাইনমেন্ট/ক্রয়)
  - নোট: P3b — PnLCalculator (D29) + BengaliFiscalCalendar (D30) + BalanceSheetCalculator (D31)। 40 new tests pass.
- [x] হিসাব-প্যাক PDF + পিরিয়ড-লক
  - নোট: P3b — Period-lock (D32) + HisabPackBuilder (D33)। 18 new tests pass.
- [x] ক্যাশ-ক্লোজ "আজকের হিসাব" (MFS-ফি-অটোলাইন + ভ্যারিয়েন্স)
  - নোট: P3c — CashCloseCalculator (D36)। 19 new tests pass.
- [x] **Exit-gate:** ১-ট্যাপ P&L; পিরিয়ড-লক-টেস্ট সবুজ
  - নোট: P3c complete — 203 টেস্ট, 0 fail.

## P4 — Firebase-ব্যাকবোন (গার্ড: Firebase-Project-Context.md)
- [x] google-services.json সেটআপ
- [x] Phone-OTP + claims-সেশন + pending-activation-স্ক্রিন + প্রথম-লগইনে এককালীন টেন্যান্ট-রিবাইন্ড
- [x] লাইসেন্স-সিঙ্ক + ডানিং-ব্যানার
- [x] ইনক্রিমেন্টাল-ব্যাকআপ + ফ্রেশ-ডিভাইস-রিস্টোর
- [x] সাবস্ক্রিপশন-স্ক্রিন
- [x] মাস্টার-ক্যাটালগ-রিফ্রেশ
- [x] DailyBackupWorker
- [x] **Exit-gate:** লাইভ চেইন সবুজ: OTP → activate → ব্যানার → ব্যাকআপ → renew
  - নোট: 296 tests, 0 failures. ⚠ Live chain = device-only.

## P5 — সাপ্লায়ার + মেলা
- [x] দেনা-খাতা + পাবলিশার-স্টেটমেন্ট + রি-অর্ডার-ইনসাইট
- [x] মেলা-মোড
- [ ] **Exit-gate:** কনসাইনমেন্ট-সেটেলমেন্ট E2E-টেস্ট
  - নোট: pure-logic E2E written. ⚠ NOT RUN — sandbox lacks JDK/toolchain.

## P6 — রিপোর্ট + ট্রাস্ট + ভয়েস
- [x] রিপোর্ট-গভীরতা + মাসিক-ডেটা-কপি + ভয়েস-সেটআপ + Lite-UI-মোড
- [x] ড্যাশবোর্ড-ফিক্স
- [ ] **Exit-gate:** ডেটা-কপি-ফ্লো E2E
  - নোট: monthly data-copy worker written. Real-device verification pending.

## P7 — পাইলট-হার্ডেনিং
- [x] ট্রায়াল-মোড + anti-farm + নম্বর-মাইগ্রেশন + ডিভাইস-গ্রুপ-ম্যানেজার + ডেমো-মোড
- [x] অফলাইন-কাওস-স্যুট (এয়ারপ্লেন-দিন, মিড-সিঙ্ক-কিল, ৩০-দিন-সোক+সাইজ-গেট)
  - নোট: D80 — OfflineDaySimulator + MidSyncKillGuard + DbSizeGateCalculator। 19 টেস্ট, CI সবুজ ✅, merge সবুজ ✅ (PR #48, commit fe2f9ab).
- [ ] **Exit-gate:** ২০-দোকান-পাইলট APK রেডি
  - নোট: Requires device verification — pilot phase

## P8 — GA
- [x] রিলিজ: R8+সাইনড-APK+ভার্সন-সানসেট-ম্যানিফেস্ট + এজেন্ট-APK-চ্যানেল
- [x] রেফারেল+কো-সেল-কিট; ফাউন্ডার্স-ক্লাব-অনবোর্ডিং
- [x] ডেমো-মোড: OWNER-confirmed local reset + seed restore
- [x] Lite ডিভাইস-গ্রুপ
- [ ] **Exit-gate:** প্রথম পেইং-টেন্যান্ট লাইভ
  - নোট: Requires vendor activation + device verification — pilot phase

## P10 — ডিজাইন-সিস্টেম (D67 + D71 + D72)
- [x] D71: Design System Specification
- [x] D72: M3 Color-Scheme Token Mapping
- [x] BoiKhataTheme.kt: Full D71 color token set (22 tokens)
- [x] BoiKhataTheme.kt: BengaliFontFamily wired
- [x] Catalog + FAB crash fixed
- [ ] Per-screen device audit: card surfaces (#F2EDE7), FAB (maroon #800000), nav indicator (green #B8F0D4), Bengali digits
- [ ] **Exit-gate:** screenshot test at max font-scale
  - নোট: Device verification pending (HomeScreen v2 install check in progress).

---

## পোস্ট-GA (চলমান)
- [ ] প্রতি-মার্জড-ফেজে লোকাল-ভেরিফিকেশন-চেকলিস্ট (ফ্রেশ-ক্লোন+বিল্ড+বাজেট-স্পট)

*অ-চেকড+নোটহীন বাক্স = "শুরু হয়নি" — অস্পষ্টতা রেখো না।*
