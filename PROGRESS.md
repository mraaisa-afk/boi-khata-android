# PROGRESS.md — বই খাতা বিল্ড-চেকলিস্ট

**প্রোটোকল:** প্রতি সেশনের শুরুতে ARCHITECTURE.md, CONVENTIONS.md, DECISIONS.md, GIT_WORKFLOW.md ও এই ফাইল পুরো পড়ো।
**PROGRESS-এর প্রথম অ-চেকড আইটেম** থেকে শুরু। সেশনের একদম শেষ কাজ: সম্পন্ন আইটেম চেক + কমিট।
মাঝপথে থামলে: আইটেম অ-চেকড রেখে নিচে এক-লাইন নোট।

**Exact workflow:** এক active workstream = এক branch = এক PR। একই workstream-এর সব batch/fix/CI-fix একই branch/PR-এ push হবে। প্রতিটি push-এ CI নতুন করে চলবে। Merge হবে সব batch complete ও CI green হলে, একবার। আলাদা branch/PR হবে শুধু independent workstream বা owner-approved split হলে।

---

## P0 — স্কেলেটন ও গার্ডরেল
- [x] Gradle-KTS প্রজেক্ট + ভার্সন-ক্যাটালগ
- [x] মডিউল-শেল
- [x] Hilt-ওয়্যারিং + MainActivity
- [x] Noto Sans Bengali + বাংলা strings + NumberFormatter foundation
- [x] CI: প্রতি PR-এ ক্লিন-ক্লোন build
- [x] .env.example
- [x] **Exit-gate:** ক্লিন-ক্লোন বিল্ড সবুজ

## P1 — লোকাল-ফাউন্ডেশন
- [x] Room v1 স্কিমা
- [x] Tenant/User/Device seed
- [x] PIN-login + role-switch + auto-lock
- [x] খাতা-first home
- [x] Data-meter + Wi-Fi-only toggle
- [x] LicensePolicy + LicenseWriteGuard + tests
- [x] AgingCalculator + tests
- [ ] **Exit-gate:** airplane-mode device demo pending
  - নোট: tests/build green; real-device demo pending.

## P2 — ক্যাটালগ + POS + খাতা
- [x] Catalog + Bengali fuzzy search
- [x] POS cart/discount/VAT/partial→khata/bill-number
- [x] WhatsApp receipt
- [x] Khata customer/installment/credit warning/statement
- [x] **Exit-gate:** first-bill flow verified by domain tests/build

## P3 — হিসাব-কোর
- [x] ExpenseCategory + PurchaseRouter + recurring
- [x] Cashbook auto-populate
- [x] OwnerDrawing
- [x] P&L + Bengali fiscal rollup + balance sheet + COGS split
- [x] Hisab pack PDF + period lock
- [x] Cash-close
- [x] **Exit-gate:** P&L and period-lock tests green

## P4 — Firebase backbone
- [x] google-services.json setup
- [x] Phone OTP + claims + pending activation + tenant rebind
- [x] License sync + banner
- [x] Incremental backup + restore
- [x] Subscription screen
- [x] Master catalog refresh
- [x] DailyBackupWorker
- [x] **Exit-gate:** live chain marked green; device-only verification caveat

## P5 — সাপ্লায়ার + মেলা
- [x] Supplier ledger + publisher statement + reorder insight
- [x] Mela mode
- [ ] **Exit-gate:** consignment-settlement E2E final verification
  - নোট: PR #50 open on `agent/p5-exit-gate`; separate phase gate, not same workstream as P10/D81.

## P6 — রিপোর্ট + ট্রাস্ট + ভয়েস
- [x] Report depth + monthly data copy + voice setup + Lite UI mode
- [x] Dashboard fix
- [ ] **Exit-gate:** data-copy-flow E2E real-device verification pending

## P7 — পাইলট-হার্ডেনিং
- [x] Trial mode + anti-farm + number migration + device group + demo mode
- [x] Offline chaos suite
- [ ] **Exit-gate:** 20-shop pilot APK ready pending device/pilot verification

## P8 — GA
- [x] Release hardening + APK channel
- [x] Referral + co-sell kit + founders club onboarding
- [x] Demo mode local reset
- [x] Lite device group
- [ ] **Exit-gate:** first paying tenant live pending

## P10 — Design rebuild + D79/D81
- [x] D71 Design System Specification
- [x] D72 M3 Color-Scheme Token Mapping
- [x] BoiKhataTheme full D71 token set
- [x] BengaliFontFamily wired
- [x] Catalog + FAB crash fixes
- [x] D81 formula fix: `paidAmount` + khata PAYMENT collection included in HomeScreen net-profit formula [PR #51]
- [ ] Per-screen device audit
- [ ] **Exit-gate:** screenshot test at max font-scale
  - নোট: PR #51 open on `agent/p10-d81-formula-fix`; independent P10 formula-fix workstream, not same as P5 exit-gate.

---

## Active Branches / PRs

| Branch | PR | Workstream | Status |
| --- | --- | --- | --- |
| `agent/p5-exit-gate` | #50 | P5 exit-gate verification | Open |
| `agent/p10-d81-formula-fix` | #51 | P10 D81 formula fix | Open |
| `agent/workflow-exact-stacked-pr` | pending | Workflow doc alignment | This PR |

**Rule:** if future work belongs to an existing workstream above, push to that same branch/PR instead of creating a new one.

---

*অ-চেকড+নোটহীন বাক্স = “শুরু হয়নি” — অস্পষ্টতা রেখো না।*
