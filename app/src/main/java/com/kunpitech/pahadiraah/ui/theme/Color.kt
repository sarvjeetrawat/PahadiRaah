package com.kunpitech.pahadiraah.ui.theme

import androidx.compose.ui.graphics.Color

// ═════════════════════════════════════════════════════════════════════════════
//  PahadiRaah Color System — "Mountain Dawn" Palette
//  Inspired by: Himalayan slate, saffron prayer flags, glacier teal,
//               pine resin warmth, and the golden hour on snow peaks.
// ═════════════════════════════════════════════════════════════════════════════

// ─── Backgrounds — Deep Himalayan Slate ──────────────────────────────────────
val Slate          = Color(0xFF0F1419)   // Deepest background — night rock
val SlateDeep      = Color(0xFF0B1015)   // Absolute darkest — hero overlays
val SlateMid       = Color(0xFF1A2430)   // Card / sheet surfaces
val SlateWarm      = Color(0xFF1E2A35)   // Alternate surface — slight warmth
val SlateLight     = Color(0xFF253545)   // Elevated surface — visible layering

// ─── Primary — Pine & Glacier ────────────────────────────────────────────────
val PineDeep       = Color(0xFF1B3A2F)   // Deep pine — primary action dark
val PineMid        = Color(0xFF2E6B50)   // Pine mid — buttons, icons
val PineLight      = Color(0xFF3D8F69)   // Pine light — active states
val GlacierTeal    = Color(0xFF4DBFA8)   // Glacier melt — pop accent, success
val GlacierLight   = Color(0xFF80D4C0)   // Light glacier — highlights

// ─── Accent — Saffron & Marigold (Pahadi warmth) ────────────────────────────
val Saffron        = Color(0xFFD4702A)   // Deep saffron — primary warm accent
val Marigold       = Color(0xFFE8972E)   // Marigold — fares, ratings, CTAs
val TurmericGlow   = Color(0xFFF2B84B)   // Bright turmeric — highlights, badges
val EmberWarm      = Color(0xFFB85C28)   // Ember dark — pressed states

// ─── Neutral Warm — Stone & Dust ─────────────────────────────────────────────
val StoneWarm      = Color(0xFF4A3D32)   // Warm stone dark
val DustTaupe      = Color(0xFF6B5A48)   // Dusty mid-tone
val ParchmentMist  = Color(0xFFA89880)   // Parchment — secondary text
val SnowPeak       = Color(0xFFF0EDE8)   // Snow — primary text (warm white)
val MistVeil       = Color(0xFFCDC8C0)   // Mist — subtitle text

// ─── Semantic Colors ──────────────────────────────────────────────────────────
val StatusActive    = GlacierTeal         // Active / online
val StatusPending   = Marigold            // Pending / upcoming
val StatusDone      = ParchmentMist       // Completed
val StatusError     = Color(0xFFCF4E4E)   // Error / cancelled
val StatusWarning   = TurmericGlow        // Warning

// ─── Surface Overlays — Layered depth ────────────────────────────────────────
val SurfaceGhost    = Color(0x0DFFFFFF)   // White 5%  — subtle card bg
val SurfaceLow      = Color(0x14FFFFFF)   // White 8%  — card bg
val SurfaceMid      = Color(0x1FFFFFFF)   // White 12% — elevated card
val SurfaceHigh     = Color(0x2BFFFFFF)   // White 17% — modal / sheet

// ─── Borders ─────────────────────────────────────────────────────────────────
val BorderGhost     = Color(0x1AFFFFFF)   // White 10% — barely-there dividers
val BorderSubtle    = Color(0x26FFFFFF)   // White 15% — default border
val BorderMid       = Color(0x40FFFFFF)   // White 25% — visible border
val BorderFocus     = GlacierTeal         // Focused / selected border
val BorderWarm      = Color(0x33D4702A)   // Saffron 20% — warm border

// ─── Gradient Collections ─────────────────────────────────────────────────────

// Hero / banner — night sky to deep pine
val GradientHero         = listOf(SlateDeep, PineDeep)

// Primary action — pine to glacier
val GradientPrimary      = listOf(PineMid, GlacierTeal)

// Warm CTA — saffron to marigold (fares, Book Now buttons)
val GradientSaffron      = listOf(Saffron, Marigold)

// Gold highlight — marigold to turmeric (ratings, earnings)
val GradientGold         = listOf(Marigold, TurmericGlow)

// Card ambient — slate with warmth
val GradientCard         = listOf(SlateMid, SlateWarm)

// Mountain sky — deep slate to teal horizon
val GradientSky          = listOf(Slate, Color(0xFF0D2B35))

// Passenger role — warm stone gradient
val GradientPassenger    = listOf(Color(0xFF1A120A), Color(0xFF2E1F10))

// Driver role — pine depth
val GradientDriver       = listOf(PineDeep, Color(0xFF0D2018))

// Overlay scrim — for bottom sheets and modals
val GradientScrim        = listOf(Color(0xCC0F1419), Color(0xFF0F1419))

// Divider shimmer — transparent → subtle → transparent
val GradientDivider      = listOf(Color.Transparent, BorderSubtle, Color.Transparent)

// ─── Legacy aliases (keep for backward compat while migrating) ────────────────
// Map old names → new palette so existing code compiles without changes
val Pine       = PineDeep
val Forest     = SlateMid
val Moss       = PineMid
val Sage       = GlacierLight
val Mist       = MistVeil
val Snow       = SnowPeak
val Gold       = Marigold
val Amber      = TurmericGlow
val Dusk       = StoneWarm
val Rock       = Color(0xFF2A1F18)

val SurfaceLight   = SurfaceLow
val SurfaceMedium  = SurfaceMid
val BorderFocusOld = BorderFocus

val GradientMoss      = GradientPrimary
val GradientPine      = listOf(Slate, SlateMid)