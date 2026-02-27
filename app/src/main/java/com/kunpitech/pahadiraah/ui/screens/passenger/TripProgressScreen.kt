package com.kunpitech.pahadiraah.ui.screens.passenger

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.pahadiraah.data.model.BookingDto
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.viewmodel.BookingViewModel
import com.kunpitech.pahadiraah.viewmodel.LocationViewModel
import com.kunpitech.pahadiraah.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  DATA
// ─────────────────────────────────────────────────────────────────────────────

data class TripMilestone(
    val label:     String,
    val emoji:     String,
    val time:      String,
    val isDone:    Boolean,
    val isActive:  Boolean
)

data class LiveTrip(
    val tripId:       String,
    val origin:       String,
    val destination:  String,
    val routeEmoji:   String,
    val driverName:   String,
    val driverEmoji:  String,
    val vehicle:      String,
    val date:         String,
    val departedAt:   String,
    val eta:          String,
    val progressFrac: Float,       // 0.0 – 1.0
    val currentLeg:   String,      // "En route to Mandi"
    val speed:        String,      // "62 km/h"
    val distance:     String,      // "145 km remaining"
    val milestones:   List<TripMilestone>
)


// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TripProgressScreen(
    bookingId:   String,
    onBack:      () -> Unit,
    onRateTrip:  (bookingId: String, routeId: String, driverId: String, driverName: String, driverEmoji: String) -> Unit,
    bookingVm:   BookingViewModel  = hiltViewModel(),
    locationVm:  LocationViewModel = hiltViewModel()
) {
    val bookingsState by bookingVm.myBookings.collectAsStateWithLifecycle()
    val liveLocation  by locationVm.liveLocation.collectAsStateWithLifecycle()

    // Subscribe to live updates
    LaunchedEffect(bookingId) {
        bookingVm.loadMyBookings()
        bookingVm.subscribeToBookingUpdates(bookingId)
        locationVm.startListeningLocation(bookingId)
    }

    // Find the specific booking from loaded list
    val booking = (bookingsState as? UiState.Success)?.data?.firstOrNull { it.id == bookingId }

    // Build LiveTrip from BookingDto, falling back to sample for initial render
    val trip = remember(booking, liveLocation) {
        booking?.let { b ->
            LiveTrip(
                tripId      = b.id,
                origin      = b.routes?.origin ?: "Origin",
                destination = b.routes?.destination ?: "Destination",
                routeEmoji  = "🏔️",
                driverName  = b.routes?.users?.name ?: "Driver",
                driverEmoji = b.routes?.users?.emoji ?: "🧑",
                vehicle     = b.routes?.vehicleId ?: "Vehicle",
                date        = b.routes?.date ?: "",
                departedAt  = b.routes?.time?.take(5) ?: "--",
                eta         = "~${b.routes?.durationHrs ?: "--"}",
                progressFrac= when (b.status) {
                    "ongoing"   -> 0.5f
                    "completed" -> 1.0f
                    else        -> 0.0f
                },
                currentLeg  = "En route to ${b.routes?.destination ?: "destination"}",
                speed       = "${liveLocation?.speedKmh?.toInt() ?: 0} km/h",
                distance    = "-- km remaining",
                milestones  = emptyList()
            )
        } ?: LiveTrip(
            tripId       = bookingId,
            origin       = "Loading…",
            destination  = "",
            routeEmoji   = "🏔️",
            driverName   = "Loading…",
            driverEmoji  = "🧑",
            vehicle      = "",
            date         = "",
            departedAt   = "--",
            eta          = "--",
            progressFrac = 0f,
            currentLeg   = "Fetching trip details…",
            speed        = "-- km/h",
            distance     = "-- km remaining",
            milestones   = emptyList()
        )
    }

    // ── Entrance animations ───────────────────────────────────────────────────
    var started by remember { mutableStateOf(false) }
    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(400), label = "ha")
    val headerOffset by animateFloatAsState(if (started) 0f else -20f, tween(500, easing = EaseOutCubic), label = "hY")
    val cardAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 150), label = "ca")
    val cardOffset   by animateFloatAsState(if (started) 0f else 28f, tween(700, delayMillis = 150, easing = EaseOutCubic), label = "cY")
    LaunchedEffect(Unit) { started = true }

    // ── Animated progress bar ─────────────────────────────────────────────────
    var barVisible by remember { mutableStateOf(false) }
    val animProgress by animateFloatAsState(
        if (barVisible) trip.progressFrac else 0f,
        tween(1200, easing = EaseOutCubic),
        label = "prog"
    )
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400)
        barVisible = true
    }

    // ── Infinite pulse for active vehicle dot ─────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "vPulse")
    val vehiclePulse by infiniteTransition.animateFloat(
        initialValue  = 0.6f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "vp"
    )
    val liveBlink by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 0.3f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "lb"
    )

    // ═════════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Pine)
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Brush.verticalGradient(listOf(Moss.copy(alpha = 0.1f), Color.Transparent)))
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Mist, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("TRIP IN PROGRESS", style = EyebrowStyle.copy(fontSize = 10.sp))
                    Spacer(Modifier.height(2.dp))
                    Text("Live Tracking", style = PahadiRaahTypography.titleLarge.copy(color = Snow))
                }
                // Live badge
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(PillShape)
                        .background(Color.Red.copy(alpha = 0.12f))
                        .border(1.dp, Color.Red.copy(alpha = 0.25f), PillShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(liveBlink)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = 0.85f))
                    )
                    Text("LIVE", style = EyebrowStyle.copy(fontSize = 9.sp, color = Color.Red.copy(alpha = 0.85f)))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── ROUTE PROGRESS CARD ───────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(cardAlpha)
                        .graphicsLayer { translationY = cardOffset }
                        .clip(PahadiRaahShapes.large)
                        .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.7f))))
                        .padding(20.dp)
                ) {
                    // Origin → Destination
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("FROM", style = FormLabelStyle.copy(fontSize = 8.sp, color = Snow.copy(alpha = 0.5f)))
                            Text(trip.origin, style = PahadiRaahTypography.titleLarge.copy(color = Snow))
                        }
                        Text(trip.routeEmoji, fontSize = 32.sp, modifier = Modifier.alpha(0.6f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TO", style = FormLabelStyle.copy(fontSize = 8.sp, color = Snow.copy(alpha = 0.5f)))
                            Text(trip.destination, style = PahadiRaahTypography.titleLarge.copy(color = Snow))
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(PillShape)
                            .background(Snow.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animProgress)
                                .fillMaxHeight()
                                .clip(PillShape)
                                .background(Brush.horizontalGradient(GradientGold))
                        )
                        // Vehicle dot
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animProgress)
                                .wrapContentWidth(Alignment.End)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .scale(vehiclePulse)
                                    .offset(x = (-9).dp, y = (-5).dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(Snow, Amber)))
                                    .border(2.dp, Gold, CircleShape)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${(animProgress * 100).toInt()}% complete",
                            style = PahadiRaahTypography.bodySmall.copy(color = Amber, fontSize = 11.sp)
                        )
                        Text(
                            trip.distance,
                            style = PahadiRaahTypography.bodySmall.copy(color = Snow.copy(alpha = 0.6f), fontSize = 11.sp)
                        )
                    }
                }

                // ── LIVE STATS ROW ────────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .alpha(cardAlpha)
                        .graphicsLayer { translationY = cardOffset }
                ) {
                    LiveStatCard("📍", trip.currentLeg, "Current Position", Sage,  Modifier.weight(1.6f))
                    LiveStatCard("🚀", trip.speed,      "Speed",            Amber, Modifier.weight(1f))
                    LiveStatCard("🕐", trip.eta,        "ETA",              Mist,  Modifier.weight(1f))
                }

                // ── DRIVER CARD ───────────────────────────────────────────────
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(cardAlpha)
                        .graphicsLayer { translationY = cardOffset }
                        .clip(PahadiRaahShapes.large)
                        .background(SurfaceLight)
                        .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
                        .padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(PahadiRaahShapes.medium)
                            .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.7f))))
                    ) {
                        Text(trip.driverEmoji, fontSize = 24.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(trip.driverName, style = PahadiRaahTypography.titleSmall.copy(color = Snow))
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "🚐 ${trip.vehicle}  •  📅 ${trip.date}",
                            style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
                        )
                    }
                    // Call button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(GradientMoss))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                                onClick           = {}
                            )
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = Snow, modifier = Modifier.size(18.dp))
                    }
                }

                // ── JOURNEY TIMELINE ──────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(cardAlpha)
                        .graphicsLayer { translationY = cardOffset }
                ) {
                    Text("JOURNEY TIMELINE", style = EyebrowStyle.copy(fontSize = 10.sp))
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PahadiRaahShapes.large)
                            .background(SurfaceLight)
                            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        trip.milestones.forEachIndexed { index, milestone ->
                            MilestoneRow(
                                milestone = milestone,
                                isLast    = index == trip.milestones.lastIndex
                            )
                        }
                    }
                }
            }
        }

        // ── BOTTOM BAR ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Pine.copy(alpha = 0.96f), Pine)))
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            // Departed / ETA row
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("DEPARTED", style = EyebrowStyle.copy(fontSize = 8.sp))
                    Text(trip.departedAt, style = PahadiRaahTypography.titleSmall.copy(color = Snow))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Mini progress indicator
                    LinearProgressIndicator(
                        progress          = { animProgress },
                        modifier          = Modifier.width(100.dp).height(4.dp).clip(PillShape),
                        color             = Amber,
                        trackColor        = SurfaceMedium,
                        strokeCap         = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${(animProgress * 100).toInt()}%",
                        style = PahadiRaahTypography.bodySmall.copy(color = Amber, fontSize = 10.sp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ETA", style = EyebrowStyle.copy(fontSize = 8.sp))
                    Text(trip.eta, style = PahadiRaahTypography.titleSmall.copy(color = Snow))
                }
            }

            // Rate trip button (only if progress >= 100%)
            if (trip.progressFrac >= 1f) {
                RateTripButton(onClick = {
                    onRateTrip(
                        bookingId,
                        booking?.routeId ?: "",
                        booking?.routes?.driverId ?: "",
                        trip.driverName,
                        trip.driverEmoji
                    )
                })
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(PillShape)
                        .background(SurfaceLight)
                        .border(1.dp, BorderSubtle, PillShape)
                ) {
                    Text(
                        "⏳  Trip in progress — rate after arrival",
                        style = PahadiRaahTypography.labelMedium.copy(color = Sage.copy(alpha = 0.55f), letterSpacing = 0.sp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  LIVE STAT CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiveStatCard(emoji: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(PahadiRaahShapes.medium)
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.18f), PahadiRaahShapes.medium)
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.height(5.dp))
        Text(
            value,
            style    = PahadiRaahTypography.labelMedium.copy(color = color, fontSize = 12.sp, letterSpacing = 0.sp),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = PahadiRaahTypography.bodySmall.copy(color = color.copy(alpha = 0.55f), fontSize = 9.sp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MILESTONE ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MilestoneRow(milestone: TripMilestone, isLast: Boolean) {
    val dotColor = when {
        milestone.isDone   -> Sage
        milestone.isActive -> Gold
        else               -> SurfaceMedium
    }
    val textColor = when {
        milestone.isDone   -> Sage
        milestone.isActive -> Snow
        else               -> Mist.copy(alpha = 0.45f)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier              = Modifier.fillMaxWidth()
    ) {
        // Timeline spine
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (milestone.isDone || milestone.isActive)
                            Brush.verticalGradient(
                                if (milestone.isActive) GradientGold else GradientMoss
                            )
                        else
                            Brush.verticalGradient(listOf(SurfaceMedium, SurfaceMedium))
                    )
                    .border(1.5.dp, dotColor.copy(alpha = 0.5f), CircleShape)
            ) {
                Text(
                    text     = if (milestone.isDone) "✓" else milestone.emoji,
                    fontSize = if (milestone.isDone) 12.sp else 11.sp,
                    color    = if (milestone.isDone || milestone.isActive) Snow else Sage.copy(alpha = 0.3f)
                )
            }
            // Connector line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(
                            Brush.verticalGradient(
                                if (milestone.isDone)
                                    listOf(Sage.copy(alpha = 0.5f), Sage.copy(alpha = 0.2f))
                                else
                                    listOf(BorderSubtle, BorderSubtle)
                            )
                        )
                )
            }
        }

        // Text content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp)
                .padding(bottom = if (!isLast) 36.dp else 0.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(
                    text  = milestone.label,
                    style = PahadiRaahTypography.bodyMedium.copy(
                        color    = textColor,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text  = milestone.time,
                    style = PahadiRaahTypography.bodySmall.copy(
                        color    = if (milestone.isActive) Amber else Sage.copy(alpha = 0.45f),
                        fontSize = 11.sp
                    )
                )
            }
            if (milestone.isActive) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Current stop",
                    style = BadgeStyle.copy(color = Amber, fontSize = 9.sp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  RATE TRIP BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RateTripButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "rtb")

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .clip(PillShape)
            .background(Brush.horizontalGradient(GradientGold))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Text(
            "⭐  Rate Your Trip",
            style = PahadiRaahTypography.labelLarge.copy(color = Pine, fontSize = 15.sp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TripProgressPreview() {
    PahadiRaahTheme { TripProgressScreen(bookingId = "1", onBack = {}, onRateTrip = { _, _, _, _, _ -> }) }
}