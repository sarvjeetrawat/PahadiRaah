package com.kunpitech.pahadiraah.ui.screens.passenger

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.pahadiraah.data.model.ActionResult
import com.kunpitech.pahadiraah.viewmodel.ReviewViewModel
import com.kunpitech.pahadiraah.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  DATA
// ─────────────────────────────────────────────────────────────────────────────

data class RatingAspect(
    val emoji: String,
    val label: String
)

val ratingAspects = listOf(
    RatingAspect("🚗", "Driving"),
    RatingAspect("🗣️", "Communication"),
    RatingAspect("⏰", "Punctuality"),
    RatingAspect("🧹", "Cleanliness"),
    RatingAspect("🗺️", "Route Knowledge"),
)

val quickTags = listOf(
    "Safe driver", "Very punctual", "Clean vehicle", "Friendly",
    "Knew the route well", "Comfortable ride", "Great music",
    "Would recommend", "Excellent views", "Helped with luggage"
)

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RateReviewScreen(
    bookingId:   String,
    routeId:     String,
    driverId:    String,
    driverName:  String,
    driverEmoji: String,
    onBack:      () -> Unit,
    onDone:      () -> Unit,
    reviewVm:    ReviewViewModel = hiltViewModel()
) {
    val submitResult by reviewVm.submitResult.collectAsStateWithLifecycle()

    // Reset state when leaving so re-entry starts fresh
    DisposableEffect(Unit) { onDispose { reviewVm.resetSubmitResult() } }

    // ── State ─────────────────────────────────────────────────────────────────
    var overallRating by remember { mutableIntStateOf(0) }
    var aspectRatings by remember { mutableStateOf(ratingAspects.associate { it.label to 0 }) }
    var selectedTags  by remember { mutableStateOf(setOf<String>()) }
    var reviewText    by remember { mutableStateOf("") }

    val showSuccess  = submitResult is ActionResult.Success
    val isSubmitting = submitResult is ActionResult.Loading
    val submitError  = (submitResult as? ActionResult.Error)?.message

    // All 5 aspects must be rated
    val allAspectsRated = aspectRatings.values.all { it > 0 }
    // At least 1 tag selected
    val hasTag          = selectedTags.isNotEmpty()
    // Button enables only when: overall rated + all aspects rated + at least 1 tag + not submitting
    val canSubmit       = overallRating > 0 && allAspectsRated && hasTag && !isSubmitting

    // Hint shown under button explaining what's still missing
    val missingHint = when {
        overallRating == 0        -> "⭐ Tap the stars to give an overall rating"
        !allAspectsRated          -> "Rate all 5 aspects (Driving, Punctuality…)"
        !hasTag                   -> "Select at least one quick tag"
        else                      -> null
    }

    // ── Entrance ──────────────────────────────────────────────────────────────
    var started by remember { mutableStateOf(false) }
    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(400), label = "ha")
    val headerOffset by animateFloatAsState(if (started) 0f else -20f, tween(500, easing = EaseOutCubic), label = "hY")
    val contentAlpha by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 150), label = "ca")
    val contentOffset by animateFloatAsState(if (started) 0f else 24f, tween(600, delayMillis = 150, easing = EaseOutCubic), label = "cY")
    LaunchedEffect(Unit) { started = true }

    // ── Success overlay ───────────────────────────────────────────────────────
    if (showSuccess) {
        ReviewSuccessOverlay(
            rating  = overallRating,
            onDone  = onDone
        )
        return
    }

    // ═════════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Pine)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Brush.verticalGradient(listOf(Gold.copy(alpha = 0.07f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // ── TOP BAR ───────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .alpha(headerAlpha)
                    .graphicsLayer { translationY = headerOffset }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceLight)
                        .border(1.dp, BorderSubtle, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onBack
                        )
                ) {
                    androidx.compose.material3.Icon(
                        imageVector        = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint               = Mist,
                        modifier           = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("RATE YOUR TRIP", style = EyebrowStyle.copy(fontSize = 10.sp))
                    Spacer(Modifier.height(2.dp))
                    Text("Share Your Experience", style = PahadiRaahTypography.titleLarge.copy(color = Snow))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .alpha(contentAlpha)
                    .graphicsLayer { translationY = contentOffset }
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── DRIVER SUMMARY ────────────────────────────────────────────
                DriverSummaryCard(
                    driverName   = driverName,
                    driverEmoji  = driverEmoji,
                    driverRating = 0.0,
                    origin       = "",
                    destination  = "",
                    date         = "",
                    time         = ""
                )

                // ── OVERALL STAR RATING ───────────────────────────────────────
                OverallRatingSection(
                    rating   = overallRating,
                    onSelect = { overallRating = it }
                )

                // ── ASPECT RATINGS ────────────────────────────────────────────
                if (overallRating > 0) {
                    AspectRatingsSection(
                        ratings  = aspectRatings,
                        onUpdate = { label, value ->
                            aspectRatings = aspectRatings.toMutableMap().also { it[label] = value }
                        }
                    )
                }

                // ── QUICK TAGS ────────────────────────────────────────────────
                if (overallRating > 0) {
                    QuickTagsSection(
                        selected = selectedTags,
                        onToggle = { tag ->
                            selectedTags = if (tag in selectedTags)
                                selectedTags - tag else selectedTags + tag
                        }
                    )
                }

                // ── WRITTEN REVIEW ────────────────────────────────────────────
                if (overallRating > 0) {
                    WrittenReviewSection(
                        text     = reviewText,
                        onChange = { reviewText = it }
                    )
                }
            }

            // ── BOTTOM CTA ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Pine.copy(alpha = 0.96f), Pine)))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                SubmitReviewButton(
                    enabled     = canSubmit,
                    isLoading   = isSubmitting,
                    rating      = overallRating,
                    onClick     = {
                        reviewVm.submitReview(
                            bookingId     = bookingId,
                            routeId       = routeId,
                            driverIdHint  = driverId,
                            overallRating = overallRating,
                            aspectRatings = aspectRatings,
                            tags          = selectedTags.toList(),
                            comment       = reviewText
                        )
                    }
                )
                val feedbackText = submitError?.let { "⚠ $it" } ?: missingHint
                if (feedbackText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        feedbackText,
                        style     = PahadiRaahTypography.bodySmall.copy(
                            color    = if (submitError != null) StatusError else Sage.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DRIVER SUMMARY CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverSummaryCard(
    driverName:   String,
    driverEmoji:  String,
    driverRating: Double,
    origin:       String,
    destination:  String,
    date:         String,
    time:         String
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.65f))))
            .padding(18.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(58.dp)
                .clip(PahadiRaahShapes.medium)
                .background(Snow.copy(alpha = 0.12f))
                .border(2.dp, Snow.copy(alpha = 0.2f), PahadiRaahShapes.medium)
        ) {
            Text(driverEmoji.ifBlank { "🧑" }, fontSize = 28.sp)
        }

        Column(Modifier.weight(1f)) {
            Text(
                driverName.ifBlank { "Driver" },
                style = PahadiRaahTypography.titleMedium.copy(color = Snow)
            )
            Spacer(Modifier.height(3.dp))
            if (origin.isNotBlank() && destination.isNotBlank()) {
                Text(
                    "$origin → $destination",
                    style    = PahadiRaahTypography.bodySmall.copy(color = Snow.copy(alpha = 0.65f), fontSize = 12.sp),
                    maxLines = 1
                )
                Spacer(Modifier.height(3.dp))
            }
            val dateLabel = buildString {
                if (date.isNotBlank()) append("📅 $date")
                if (time.isNotBlank()) {
                    if (isNotEmpty()) append("  •  ")
                    append("🕐 $time")
                }
            }
            if (dateLabel.isNotBlank()) {
                Text(
                    dateLabel,
                    style = PahadiRaahTypography.bodySmall.copy(color = Snow.copy(alpha = 0.5f), fontSize = 10.sp)
                )
            }
            if (driverRating > 0) {
                Spacer(Modifier.height(3.dp))
                Text(
                    "⭐ ${String.format("%.1f", driverRating)}",
                    style = PahadiRaahTypography.bodySmall.copy(color = Gold.copy(alpha = 0.85f), fontSize = 11.sp)
                )
            }
        }

        Text("🏔️", fontSize = 36.sp, modifier = Modifier.alpha(0.45f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  OVERALL STAR RATING
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OverallRatingSection(rating: Int, onSelect: (Int) -> Unit) {
    val ratingLabels = mapOf(
        1 to "😔 Poor",
        2 to "😐 Fair",
        3 to "🙂 Good",
        4 to "😊 Great",
        5 to "🤩 Excellent!"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, if (rating > 0) Gold.copy(alpha = 0.3f) else BorderSubtle, PahadiRaahShapes.large)
            .padding(24.dp)
    ) {
        Text("HOW WAS YOUR TRIP?", style = EyebrowStyle.copy(fontSize = 10.sp))
        Spacer(Modifier.height(20.dp))

        // Stars
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            (1..5).forEach { star ->
                val isFilled  = star <= rating
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val starScale by animateFloatAsState(
                    targetValue   = when {
                        isPressed -> 1.3f
                        isFilled  -> 1.1f
                        else      -> 1f
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label         = "star$star"
                )

                Text(
                    text     = if (isFilled) "★" else "☆",
                    fontSize = (38 * starScale).sp,
                    color    = if (isFilled) Gold else Sage.copy(alpha = 0.25f),
                    modifier = Modifier
                        .scale(starScale)
                        .clickable(
                            interactionSource = interactionSource,
                            indication        = null,
                            onClick           = { onSelect(star) }
                        )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Rating label
        val label = ratingLabels[rating] ?: "Tap a star to rate"
        Text(
            text  = label,
            style = PahadiRaahTypography.titleSmall.copy(
                color    = if (rating > 0) Snow else Sage.copy(alpha = 0.35f),
                fontSize = 15.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ASPECT RATINGS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AspectRatingsSection(
    ratings:  Map<String, Int>,
    onUpdate: (String, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("RATE EACH ASPECT", style = EyebrowStyle.copy(fontSize = 10.sp))

        ratingAspects.forEach { aspect ->
            val current = ratings[aspect.label] ?: 0
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(aspect.emoji, fontSize = 18.sp)
                Text(
                    aspect.label,
                    style    = PahadiRaahTypography.bodyMedium.copy(color = Mist, fontSize = 13.sp),
                    modifier = Modifier.weight(1f)
                )
                // Mini stars (1–5)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        val isFilled = star <= current
                        Text(
                            text     = if (isFilled) "★" else "☆",
                            fontSize = 18.sp,
                            color    = if (isFilled) Gold else Sage.copy(alpha = 0.2f),
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                                onClick           = { onUpdate(aspect.label, star) }
                            )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  QUICK TAGS
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickTagsSection(
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("QUICK TAGS", style = EyebrowStyle.copy(fontSize = 10.sp))
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp)
        ) {
            quickTags.forEach { tag ->
                val isSelected = tag in selected
                val bg by animateColorAsState(
                    if (isSelected) Moss.copy(alpha = 0.2f) else SurfaceLight, tween(150), label = "qtBg"
                )
                val border by animateColorAsState(
                    if (isSelected) Sage.copy(alpha = 0.4f) else BorderSubtle, tween(150), label = "qtB"
                )
                val textColor by animateColorAsState(
                    if (isSelected) Snow else Mist.copy(alpha = 0.6f), tween(150), label = "qtT"
                )

                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(bg)
                        .border(1.dp, border, PillShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = { onToggle(tag) }
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text  = if (isSelected) "✓ $tag" else tag,
                        style = PahadiRaahTypography.labelSmall.copy(
                            color         = textColor,
                            fontSize      = 12.sp,
                            letterSpacing = 0.sp
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  WRITTEN REVIEW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WrittenReviewSection(text: String, onChange: (String) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (isFocused) BorderFocus else BorderSubtle, tween(200), label = "wrB"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("WRITE A REVIEW", style = EyebrowStyle.copy(fontSize = 10.sp))
            Text(
                "${text.length}/400",
                style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.4f), fontSize = 10.sp)
            )
        }
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PahadiRaahShapes.large)
                .background(SurfaceLight)
                .border(1.dp, borderColor, PahadiRaahShapes.large)
                .padding(16.dp)
        ) {
            BasicTextField(
                value           = text,
                onValueChange   = { if (it.length <= 400) onChange(it) },
                minLines        = 4,
                maxLines        = 6,
                textStyle       = PahadiRaahTypography.bodyMedium.copy(color = Snow, fontSize = 14.sp, lineHeight = 22.sp),
                cursorBrush     = SolidColor(Sage),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier        = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox   = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                "Share what made this journey memorable — the mountain roads, the driver's skill, the views…",
                                style = PahadiRaahTypography.bodyMedium.copy(
                                    color      = Sage.copy(alpha = 0.3f),
                                    fontSize   = 14.sp,
                                    lineHeight = 22.sp
                                )
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUBMIT BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SubmitReviewButton(enabled: Boolean, isLoading: Boolean, rating: Int, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed && enabled) 0.97f else 1f,
        spring(stiffness = Spring.StiffnessMedium), label = "srb"
    )

    val stars = "★".repeat(rating) + "☆".repeat(5 - rating)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .clip(PillShape)
            .background(
                if (enabled)
                    Brush.horizontalGradient(GradientGold)
                else
                    Brush.horizontalGradient(listOf(SurfaceMedium, SurfaceMedium))
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = enabled,
                onClick           = onClick
            )
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier  = Modifier.size(22.dp),
                color     = Pine,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text  = if (enabled) "$stars  Submit Review" else "Rate to continue",
                style = PahadiRaahTypography.labelLarge.copy(
                    color    = if (enabled) Pine else Sage.copy(alpha = 0.3f),
                    fontSize = 15.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUCCESS OVERLAY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ReviewSuccessOverlay(rating: Int, onDone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(400), label = "rsoA")
    val scale by animateFloatAsState(
        if (visible) 1f else 0.65f,
        tween(500, easing = EaseOutBack), label = "rsoS"
    )

    LaunchedEffect(Unit) {
        visible = true
        kotlinx.coroutines.delay(2800)
        onDone()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Pine)
            .alpha(alpha)
            .systemBarsPadding()
    ) {
        // Glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(Brush.radialGradient(listOf(Gold.copy(alpha = 0.14f), Color.Transparent)), CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier
                .scale(scale)
                .padding(horizontal = 36.dp)
        ) {
            // Gold star circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(GradientGold))
            ) {
                Text("⭐", fontSize = 44.sp)
            }

            Spacer(Modifier.height(24.dp))

            Text("Thank You!", style = PahadiRaahTypography.headlineMedium.copy(color = Snow), textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "Your review helps other travellers\nchoose the best mountain rides",
                style     = PahadiRaahTypography.bodyMedium.copy(color = Sage, textAlign = TextAlign.Center, lineHeight = 22.sp)
            )

            Spacer(Modifier.height(20.dp))

            // Stars display
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(5) { i ->
                    Text(
                        text     = if (i < rating) "★" else "☆",
                        fontSize = 30.sp,
                        color    = if (i < rating) Gold else Sage.copy(alpha = 0.15f)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Progress + returning label
            Text(
                "Returning to home screen...",
                style = PahadiRaahTypography.labelSmall.copy(
                    color         = Sage.copy(alpha = 0.45f),
                    letterSpacing = 1.sp,
                    fontSize      = 10.sp
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RateReviewPreview() {
    PahadiRaahTheme { RateReviewScreen(bookingId = "1", routeId = "", driverId = "", driverName = "Driver", driverEmoji = "🧑", onBack = {}, onDone = {}) }
}