package com.kunpitech.pahadiraah.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.kunpitech.pahadiraah.R

// ─────────────────────────────────────────────────────────────────────────────
//  GOOGLE FONTS PROVIDER — No .ttf files needed!
// ─────────────────────────────────────────────────────────────────────────────

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

private val CormorantGaramondFont = GoogleFont("Cormorant Garamond")
private val DmSansFont            = GoogleFont("DM Sans")

val CormorantGaramond = FontFamily(
    Font(googleFont = CormorantGaramondFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = CormorantGaramondFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = CormorantGaramondFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = CormorantGaramondFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = CormorantGaramondFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = CormorantGaramondFont, fontProvider = provider, weight = FontWeight.Normal, style = FontStyle.Italic),
)

val DmSans = FontFamily(
    Font(googleFont = DmSansFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = DmSansFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = DmSansFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = DmSansFont, fontProvider = provider, weight = FontWeight.SemiBold),
)



// ─────────────────────────────────────────────────────────────────────────────
//  TYPOGRAPHY SCALE
//  Maps to Material 3 slots used throughout the app
// ─────────────────────────────────────────────────────────────────────────────

val PahadiRaahTypography = Typography(

    // ── Display ─────────────────────────────────────────────────────────────
    // Used for: App name on Splash, hero numbers (fare, stats)
    displayLarge = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.SemiBold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.5).sp,
        color = Snow
    ),
    displayMedium = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp,
        color = Snow
    ),
    displaySmall = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp,
        color = Snow
    ),

    // ── Headline ─────────────────────────────────────────────────────────────
    // Used for: Screen titles (Post a Route, Booking Confirmed, etc.)
    headlineLarge = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1).sp,
        color = Snow
    ),
    headlineMedium = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        color = Snow
    ),
    headlineSmall = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        color = Snow
    ),

    // ── Title ────────────────────────────────────────────────────────────────
    // Used for: Card titles, driver names, route names
    titleLarge = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = Snow
    ),
    titleMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
        color = Snow
    ),
    titleSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = Snow
    ),

    // ── Body ─────────────────────────────────────────────────────────────────
    // Used for: Descriptions, review text, meta info
    bodyLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        color = Snow
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = Mist
    ),
    bodySmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = Sage
    ),

    // ── Label ────────────────────────────────────────────────────────────────
    // Used for: Eyebrow text, button labels, tags, nav labels
    labelLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = Snow
    ),
    labelMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = Mist
    ),
    labelSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 4.sp,          // Wide tracking for UPPERCASE eyebrows
        color = Gold
    ),
)

// ─────────────────────────────────────────────────────────────────────────────
//  CONVENIENCE EXTENSIONS
//  Use these directly in Composables for non-Material slots
// ─────────────────────────────────────────────────────────────────────────────

// App name on Splash screen
val AppNameStyle = TextStyle(
    fontFamily = CormorantGaramond,
    fontWeight = FontWeight.SemiBold,
    fontSize = 52.sp,
    lineHeight = 56.sp,
    letterSpacing = (-1).sp,
    color = Snow
)

// Section eyebrow (e.g. "WHO ARE YOU TODAY?")
val EyebrowStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    letterSpacing = 5.sp,
    color = Gold
)

// Tagline below app name on Splash
val TaglineStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Light,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 4.sp,
    color = Sage
)

// Fare / price display
val FareStyle = TextStyle(
    fontFamily = CormorantGaramond,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp,
    color = Amber
)

// Bottom nav label
val NavLabelStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.5.sp
)

// Form field label (e.g. "ORIGIN")
val FormLabelStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    letterSpacing = 2.sp,
    color = Sage
)

// Status badge text
val BadgeStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.5.sp
)