package com.boikhata.core.designsystem

import androidx.compose.ui.graphics.Color

// ─── D71 §1: Semantic color palette ──────────────────────────────────────────
// RULE: No new color may be added without a Locked Design (D-) entry.
// All usages must stay within their declared role.

/** Brand/identity — key actions + highest-urgency indicators. */
val ColorBrandMaroon = Color(0xFF800000)

/** Positive semantic — credit amounts, positive balances, hero card background. */
val ColorSemanticPositive = Color(0xFF1B6E3F)

/** Caution semantic — overdue indicators, debt warnings. */
val ColorSemanticCaution = Color(0xFF9E5C00)

/** Surface ivory — primary background. */
val ColorSurfaceIvory = Color(0xFFFDFAF6)

/**
 * Gold accent — D79 §4.5 (spec: third location forbidden).
 * Usage restricted to:
 *   1. Hero card net-profit amount text
 *   2. FAB ৳+ icon
 *
 * ⚠️ WCAG AA: #C9A227 on #1B6E3F = 2.59:1 — FAILS ≥4.5:1 (normal) and ≥3.0:1 (large).
 * Owner ruling required before GA (flagged in PR description per spec §4.5).
 */
val ColorAccentGold = Color(0xFFC9A227)

/** Navigation unselected label/icon tint. */
val ColorNavUnselected = Color(0xFF6B6B6B)
