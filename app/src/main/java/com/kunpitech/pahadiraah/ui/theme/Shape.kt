package com.kunpitech.pahadiraah.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
//  PahadiRaah Shape System
//  Soft, rounded corners throughout — feels natural like mountain curves
// ─────────────────────────────────────────────────────────────────────────────

val PahadiRaahShapes = Shapes(
    // Chips, badges, small tags → pill / subtle
    extraSmall = RoundedCornerShape(8.dp),

    // Buttons, text fields, small cards
    small = RoundedCornerShape(12.dp),

    // Standard cards (TripCard, ActionCard)
    medium = RoundedCornerShape(18.dp),

    // Hero cards, role cards, driver cards
    large = RoundedCornerShape(24.dp),

    // Full bottom sheets, large modals
    extraLarge = RoundedCornerShape(32.dp),
)

// ─────────────────────────────────────────────────────────────────────────────
//  CUSTOM SHAPE TOKENS (for shapes outside Material slots)
// ─────────────────────────────────────────────────────────────────────────────

val PillShape         = RoundedCornerShape(100.dp)   // Buttons, chips, status badges
val AvatarShape       = RoundedCornerShape(16.dp)    // Driver avatars, user avatar
val LogoShape         = RoundedCornerShape(24.dp)    // App logo icon on splash
val MapShape          = RoundedCornerShape(24.dp)    // Map placeholder card
val HeroBannerShape   = RoundedCornerShape(24.dp)    // Dashboard hero banner
val TagShape          = RoundedCornerShape(100.dp)   // Driver tag pills
val StepDotShape      = RoundedCornerShape(50)       // Progress step circles
val ProgressBarShape  = RoundedCornerShape(6.dp)     // Seat fill / journey progress bar
val BottomNavShape    = RoundedCornerShape(            // Bottom nav rounded top
    topStart = 20.dp,
    topEnd   = 20.dp,
    bottomStart = 0.dp,
    bottomEnd   = 0.dp
)