package com.kunpitech.pahadiraah.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═════════════════════════════════════════════════════════════════════════════
//  PahadiRaah — "Mountain Dawn" Theme
//  Deep Himalayan slate foundations. Saffron & marigold warmth.
//  Glacier teal as a living, breathing accent.
// ═════════════════════════════════════════════════════════════════════════════

private val PahadiDarkColorScheme = darkColorScheme(

    // ── Backgrounds ───────────────────────────────────────────────────────────
    background          = Slate,          // Himalayan night rock
    surface             = SlateMid,       // Card surfaces
    surfaceVariant      = SlateWarm,      // Alternate surface (sheets, drawers)
    surfaceTint         = GlacierTeal,    // Material3 surface tint

    // ── Primary — Pine & Glacier Teal ────────────────────────────────────────
    primary             = GlacierTeal,    // Main brand action color
    onPrimary           = Slate,          // Text/icons on primary
    primaryContainer    = PineDeep,       // Filled containers
    onPrimaryContainer  = GlacierLight,   // Content inside primary containers

    // ── Secondary — Saffron & Marigold ───────────────────────────────────────
    secondary           = Marigold,       // Fares, ratings, warm accents
    onSecondary         = Slate,          // Text on marigold
    secondaryContainer  = StoneWarm,      // Warm-tinted containers
    onSecondaryContainer= TurmericGlow,   // Content in secondary containers

    // ── Tertiary — Muted Parchment ────────────────────────────────────────────
    tertiary            = ParchmentMist,  // Subdued third accent
    onTertiary          = Slate,
    tertiaryContainer   = SlateLight,
    onTertiaryContainer = MistVeil,

    // ── Text / Icons ──────────────────────────────────────────────────────────
    onBackground        = SnowPeak,       // Primary text — warm white
    onSurface           = SnowPeak,
    onSurfaceVariant    = MistVeil,       // Secondary / hint text

    // ── Borders ───────────────────────────────────────────────────────────────
    outline             = BorderSubtle,
    outlineVariant      = BorderGhost,

    // ── Error ─────────────────────────────────────────────────────────────────
    error               = StatusError,
    onError             = SnowPeak,
    errorContainer      = Color(0xFF3D1515),
    onErrorContainer    = Color(0xFFFFB4A8),

    // ── Inverse ───────────────────────────────────────────────────────────────
    inverseSurface      = SnowPeak,
    inverseOnSurface    = Slate,
    inversePrimary      = PineMid,
)

// Light scheme — kept minimal (app is dark-first)
private val PahadiLightColorScheme = lightColorScheme(
    background          = Color(0xFFF5F2EE),  // Warm parchment
    surface             = Color(0xFFEDEAE4),
    primary             = PineMid,
    onPrimary           = SnowPeak,
    secondary           = Saffron,
    onSecondary         = SnowPeak,
    tertiary            = GlacierTeal,
    onTertiary          = Slate,
    onBackground        = Slate,
    onSurface           = SlateMid,
    outline             = Color(0xFFB0A898),
    error               = StatusError,
    onError             = SnowPeak,
)

// ─────────────────────────────────────────────────────────────────────────────
//  MAIN THEME COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PahadiRaahTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PahadiDarkColorScheme else PahadiLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Deep Himalayan slate — matches new background
            window.statusBarColor     = Slate.toArgb()
            window.navigationBarColor = Slate.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = PahadiRaahTypography,
        content     = content
    )
}