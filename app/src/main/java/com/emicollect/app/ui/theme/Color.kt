package com.emicollect.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════
// EMIX v4.0 — Premium Finance Design Tokens
// ═══════════════════════════════════════════════════════════════════

// ─── Primary: Emerald Family ─────────────────────────────────────
val EmeraldPrimary   = Color(0xFF064E3B)
val EmeraldDeep      = Color(0xFF022C22)
val EmeraldLight     = Color(0xFF34D399)
val EmeraldMuted     = Color(0xFF10B981)
val EmeraldSoft      = Color(0xFF6EE7B7)
val EmeraldTint      = Color(0xFFD1FAE5) // Very light tint for backgrounds

// ─── Accent: Gold Family ─────────────────────────────────────────
val GoldAccent       = Color(0xFFFFD700)
val GoldLight        = Color(0xFFFDE68A)
val GoldDeep         = Color(0xFFD97706)
val GoldSoft         = Color(0xFFFBBF24)
val GoldTint         = Color(0xFFFFF7ED) // Warm tint for light mode

// ─── Neutral: Gunmetal / Slate ───────────────────────────────────
val GunmetalDark     = Color(0xFF0B0F19)  // Deepest background
val GunmetalMid      = Color(0xFF111827)  // Primary background
val GunmetalLight    = Color(0xFF1F2937)  // Card / surface
val GunmetalElevated = Color(0xFF1E293B)  // Elevated surface
val SlateSubtle      = Color(0xFF334155)  // Borders, dividers
val SlateMuted       = Color(0xFF475569)  // Muted text, icons
val SlateLight       = Color(0xFF94A3B8)  // Secondary text
val SlateLighter     = Color(0xFFCBD5E1)  // Tertiary text
val SlateWhite       = Color(0xFFE2E8F0)  // Light mode borders

// ─── Text ────────────────────────────────────────────────────────
val TextWhite        = Color(0xFFF9FAFB)
val TextGold         = Color(0xFFFBBF24)
val TextDark         = Color(0xFF0F172A)  // For light mode

// ─── Semantic: Status Colors ─────────────────────────────────────
val ErrorRed         = Color(0xFFEF4444)
val ErrorRedSoft     = Color(0xFFFCA5A5)
val ErrorRedTint     = Color(0x1AEF4444)  // 10% opacity background

val WarningAmber     = Color(0xFFF59E0B)
val WarningAmberSoft = Color(0xFFFCD34D)
val WarningAmberTint = Color(0x1AF59E0B)

val SuccessGreen     = Color(0xFF10B981)
val SuccessGreenSoft = Color(0xFF6EE7B7)
val SuccessGreenTint = Color(0x1A10B981)

val InfoBlue         = Color(0xFF3B82F6)
val InfoBlueSoft     = Color(0xFF93C5FD)
val InfoBlueTint     = Color(0x1A3B82F6)

// ─── Gradient Anchors ────────────────────────────────────────────
val GradientEmeraldStart = Color(0xFF064E3B)
val GradientEmeraldEnd   = Color(0xFF0D9488) // Teal endpoint
val GradientGoldStart    = Color(0xFFD97706)
val GradientGoldEnd      = Color(0xFFFBBF24)
val GradientDarkStart    = Color(0xFF0F172A)
val GradientDarkEnd      = Color(0xFF1E293B)

// ─── Glass / Overlay ─────────────────────────────────────────────
val GlassSurface     = Color(0xFF1F2937).copy(alpha = 0.65f)
val GlassBorder      = Color(0xFF374151).copy(alpha = 0.4f)
val GlassHighlight   = Color(0xFFFFFFFF).copy(alpha = 0.05f)
val OverlayDark      = Color(0xFF000000).copy(alpha = 0.4f)

// ─── Light Mode Surfaces ─────────────────────────────────────────
val LightBackground  = Color(0xFFF8FAFC) // Slate 50
val LightSurface     = Color(0xFFFFFFFF)
val LightSurfaceVar  = Color(0xFFF1F5F9) // Slate 100
val LightCard        = Color(0xFFFFFFFF)
val LightBorder      = Color(0xFFE2E8F0) // Slate 200
