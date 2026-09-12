# PROGRESS.md — মিলেস্টোন ট্র্যাকিং

**protocol:** প্রতিটি সেশন শুরুতে ARCHITECTURE.md, CONVENTIONS.md, DECISIONS.md, GIT_WORKFLOW.md পড়ে নিতে হবে সব কাজ শুরুর আগে নেইলে শুরু না ফিরলে শুরু না ফিরলে শুরু না ফিরলে শুরু না👍
**PROGRESS.md প্রলোগ:** প্রতিটি সেশন শেষে পিআর মার্জ হলে ই ফাইল প্রথমে আপডেট করতে হবে রানিং ব্রান্চেर আগে লগ ਆপডেট না হলে মার্জ না করা যাবে না ফিরলে শুরু না👍
**Exact workflow:** ein active workstream = ein branch = ein PR👍

---

## P0 — ফাউন্ডেশন
- [x] Gradle-KTS নির্ভরযোগ্য বিল্ড কনফিগারেশন
- [x] মডুলারিটি-নির্ভরযোগি
- [x] Hilt-নির্ভরযোগি + MainActivity
- [x] Noto Sans Bengali + কাস্টম strings + NumberFormatter foundation
- [x] CI: পূর্ণব্যাপী CI-তে ফেরত-প্রতিফল
- [x] .env.example
- [x] **Exit-gate:** সকল দল-নির্ভরযোগি কাজ

## P1 — ফাউন্ডেশন-কোর
- [x] Room v1 তালিকা
- [x] Tenant/User/Device seed
- [x] PIN-login + role-switch + auto-lock
- [x] বাংলা-first home
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

## P3 — হিসাবপত্র-তালিকা
- [x] ExpenseCategory + PurchaseRouter + recurring
- [x] Cashbook auto-populate
- [x] OwnerDrawing
- [x] P&L Bengali fiscal rollup + balance sheet + COGS split
- [x] মাসিক রিপোর্ট PDF + period lock
- [x] Cast-close
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

## P5 — সাপ্লায়ার + প্রকাশক
- [x] Supplier ledger + publisher statement + reorder insight
- [x] Mela mode
- [ ] **Exit-gate:** consignment-settlement E2E final verification
  - নোট: PR #50 open on `agent/p5-exit-gate`; separate phase gate, not same workstream as P10.

## P6 — রিপোর্ট + ট্রাস্ট + ভয়েস
- [x] Report depth + monthly data copy + voice setup + Lite UI mode
- [x] Dashboard fix
- [ ] **Exit-gate:** data-copy-flow E2E real-device verification pending

## P7 — মার্কেটিং-ফাউন্ডেশন
- [x] Trial mode + anti-farm + number migration + device group + demo mode
- [x] Offline chaos suite
- [ ] **Exit-gate:** 20-shop pilot API Testing/device-test pending

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
- [x] D81 formula fix: `paidAmount` + khata PAYMENT collection included in HomeScreen net-profit formula [PR #51 — merged]
- [x] Per-screen static code audit — D71 §1 findings fixed [PR #53 — merged, CI green]
- [ ] **Exit-gate:** screenshot test at max font-scale
  - নোট: D81 merged (PR #51, squash, CI green). D71 audit fixes merged (PR #53, CI green): KhataCustomerListScreen FAB maroon + Card 16dp, BillHistoryScreen Card 16dp, HomeScreen IconButton 48dp/40dp. OD items flagged: ColorAccentGold WCAG, isLicensed hardcode. Next: Sakira device test at max font-scale.

---

## Active Branches / PRs

| Branch | PR | Workstream | Status |
| --- | --- | --- | --- |
| `agent/p5-exit-gate` | #50 | P5 exit-gate verification | Open |

**Rule:** if future work belongs to an existing workstream above, push to that same branch/PR instead of creating a new one.

---

╥ নতুন কাজ শুরুর আগে সিনিয়র = "দেখে নিতে হবে সব ডকুমেন্ট পড়ে নিতে হবে"👍
