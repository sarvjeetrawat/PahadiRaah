package com.kunpitech.pahadiraah.ui.screens.passenger

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.pahadiraah.data.model.ActionResult
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.viewmodel.BookingViewModel
import com.kunpitech.pahadiraah.viewmodel.RouteViewModel
import com.kunpitech.pahadiraah.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  DATA
// ─────────────────────────────────────────────────────────────────────────────

data class BookingSummary(
    val bookingId:     String,
    val driverName:    String,
    val driverEmoji:   String,
    val driverRating:  Float,
    val vehicle:       String,
    val vehicleEmoji:  String,
    val origin:        String,
    val destination:   String,
    val date:          String,
    val time:          String,
    val duration:      String,
    val seats:         Int,
    val farePerSeat:   String,
    val totalFare:     String,
    val serviceFee:    String,
    val grandTotal:    String,
    val routeEmoji:    String
)

enum class PaymentMethod { UPI, CARD, CASH }

// ─────────────────────────────────────────────────────────────────────────────
//  SAMPLE DATA
// ─────────────────────────────────────────────────────────────────────────────

fun bookingSummaryFor(bookingId: String) = BookingSummary(
    bookingId    = bookingId,
    driverName   = "Ramesh Kumar",
    driverEmoji  = "🧔",
    driverRating = 4.9f,
    vehicle      = "SUV / Jeep",
    vehicleEmoji = "🚐",
    origin       = "Shimla",
    destination  = "Manali",
    date         = "Jun 22, 2025",
    time         = "6:00 AM",
    duration     = "6–7 hrs",
    seats        = 1,
    farePerSeat  = "₹850",
    totalFare    = "₹850",
    serviceFee   = "₹43",
    grandTotal   = "₹893",
    routeEmoji   = "🏔️"
)

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BookingConfirmScreen(
    routeId:     String,
    onBack:      () -> Unit,
    onTrackTrip: (String) -> Unit,
    onHome:      () -> Unit,
    routeVm:     RouteViewModel   = hiltViewModel(),
    bookingVm:   BookingViewModel = hiltViewModel()
) {
    val routeState    by routeVm.selectedRoute.collectAsStateWithLifecycle()
    val confirmResult by bookingVm.confirmResult.collectAsStateWithLifecycle()
    val confirmedRef  by bookingVm.confirmedRef.collectAsStateWithLifecycle()
    val myBookingsState by bookingVm.myBookings.collectAsStateWithLifecycle()

    LaunchedEffect(routeId) {
        routeVm.loadRoute(routeId)
        bookingVm.loadMyBookings()
    }

    // Reset booking result when leaving screen
    DisposableEffect(Unit) { onDispose { bookingVm.resetConfirmResult() } }

    val route        = (routeState as? UiState.Success)?.data
    val isRouteLoading = routeState is UiState.Loading
    val routeError   = (routeState as? UiState.Error)?.message

    val maxSeats = (route?.seatsLeft ?: 4).coerceAtLeast(1)

    // ── Departure guard — true when the trip has already departed ─────────────
    // Checked against now() each recomposition so it stays accurate even if the
    // screen is left open past midnight.
    val isDeparted = remember(route) {
        val r = route ?: return@remember false
        try {
            val depDate = java.time.LocalDate.parse(r.date)
            val parts   = r.time.split(":").map { it.toIntOrNull() ?: 0 }
            val depTime = java.time.LocalTime.of(
                parts.getOrElse(0) { 0 },
                parts.getOrElse(1) { 0 },
                parts.getOrElse(2) { 0 }
            )
            java.time.LocalDateTime.of(depDate, depTime).isBefore(java.time.LocalDateTime.now())
        } catch (e: Exception) { false }
    }

    val booking = remember(route) {
        route?.let { r ->
            BookingSummary(
                bookingId    = routeId,
                driverName   = r.users?.name ?: "Driver",
                driverEmoji  = r.users?.emoji ?: "🧑",
                driverRating = r.users?.avgRating?.toFloat() ?: 0f,
                vehicle      = r.vehicleId ?: "Vehicle",
                vehicleEmoji = when {
                    r.vehicleId?.contains("suv",   ignoreCase = true) == true -> "🚐"
                    r.vehicleId?.contains("tempo", ignoreCase = true) == true -> "🚌"
                    else -> "🚙"
                },
                origin       = r.origin,
                destination  = r.destination,
                date         = r.date,
                time         = r.time.take(5),
                duration     = r.durationHrs,
                seats        = 1,
                farePerSeat  = "₹${r.farePerSeat}",
                totalFare    = "₹${r.farePerSeat}",
                serviceFee   = "₹${(r.farePerSeat * 0.05).toInt()}",
                grandTotal   = "₹${(r.farePerSeat * 1.05).toInt()}",
                routeEmoji   = when {
                    r.origin.contains("Shimla",      ignoreCase = true) -> "🏔️"
                    r.origin.contains("Dharamshala", ignoreCase = true) -> "🌲"
                    r.origin.contains("Rishikesh",   ignoreCase = true) -> "🌊"
                    r.origin.contains("Nainital",    ignoreCase = true) -> "⛰️"
                    else -> "🏕️"
                }
            )
        } ?: bookingSummaryFor(routeId)
    }

    // ── Existing booking check — must come BEFORE LaunchedEffect that uses it ──
    val existingBookings = (myBookingsState as? com.kunpitech.pahadiraah.data.model.UiState.Success)?.data ?: emptyList()
    val existingBooking  = existingBookings.firstOrNull { it.routeId == routeId && it.status != "cancelled" }
    val alreadyBooked    = existingBooking != null
    val isEditMode       = alreadyBooked

    var seats         by remember { mutableIntStateOf(1) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var agreedToTerms by remember { mutableStateOf(false) }

    // In edit mode: pre-fill seats from existing booking + 1 (minimum add 1)
    LaunchedEffect(existingBooking) {
        if (existingBooking != null) seats = existingBooking.seats + 1
    }
    // Clamp seats if maxSeats changes after load
    LaunchedEffect(maxSeats) { if (seats > maxSeats) seats = maxSeats }

    val baseFareNum = route?.farePerSeat ?: 850
    val totalFare   = baseFareNum * seats
    val serviceFee  = (totalFare * 0.05).toInt()
    val grandTotal  = totalFare + serviceFee

    fun formatAmount(v: Int) = "₹${"%,d".format(v)}"

    val isBookingLoading = confirmResult is ActionResult.Loading
    // Map duplicate key constraint to friendly message
    val bookingError     = (confirmResult as? ActionResult.Error)?.message?.let { raw ->
        if (raw.contains("unique_passenger_route", ignoreCase = true) ||
            raw.contains("duplicate key", ignoreCase = true))
            "You already have a booking on this route."
        else raw
    }
    // In edit mode, seats must be > existing seats (adding more only)
    val minSeats         = if (isEditMode) (existingBooking?.seats ?: 1) + 1 else 1
    val isReady          = agreedToTerms && !isBookingLoading && route != null &&
            !isDeparted &&   // block booking if trip already departed
            (!isEditMode || seats >= minSeats)
    val showSuccess      = confirmResult is ActionResult.Success

    // ── Entrance animations ───────────────────────────────────────────────────
    var started by remember { mutableStateOf(false) }
    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(400), label = "ha")
    val headerOffset by animateFloatAsState(if (started) 0f else -20f, tween(500, easing = EaseOutCubic), label = "hY")
    val cardAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 150), label = "ca")
    val cardOffset   by animateFloatAsState(if (started) 0f else 28f, tween(600, delayMillis = 150, easing = EaseOutCubic), label = "cY")
    LaunchedEffect(Unit) { started = true }

    // ── Success overlay ───────────────────────────────────────────────────────
    if (showSuccess) {
        val displayBooking = booking.copy(
            bookingId  = confirmedRef ?: routeId,
            grandTotal = formatAmount(grandTotal),
            totalFare  = formatAmount(totalFare),
            serviceFee = formatAmount(serviceFee)
        )
        BookingSuccessOverlay(
            booking = displayBooking,
            onTrack = { onTrackTrip(confirmedRef ?: routeId) },
            onHome  = onHome
        )
        return
    }

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
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Gold.copy(alpha = 0.06f), Moss.copy(alpha = 0.04f), Color.Transparent)
                    )
                )
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
                    Icon(
                        imageVector        = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint               = Mist,
                        modifier           = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "CONFIRM BOOKING", style = EyebrowStyle.copy(fontSize = 10.sp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = "Review & Pay",
                        style = PahadiRaahTypography.titleLarge.copy(color = Snow)
                    )
                }
            }

            // ── SCROLL CONTENT ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .alpha(cardAlpha)
                    .graphicsLayer { translationY = cardOffset }
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── LOADING STATE ─────────────────────────────────────────────
                if (isRouteLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(PahadiRaahShapes.large)
                            .background(SurfaceLight)
                            .border(1.dp, BorderSubtle, PahadiRaahShapes.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Sage, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Loading route details…", style = PahadiRaahTypography.bodySmall.copy(color = Sage))
                        }
                    }
                }

                // ── ROUTE ERROR ───────────────────────────────────────────────
                if (routeError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PahadiRaahShapes.medium)
                            .background(StatusError.copy(alpha = 0.1f))
                            .border(1.dp, StatusError.copy(alpha = 0.3f), PahadiRaahShapes.medium)
                            .padding(16.dp)
                    ) {
                        Text(
                            text  = "⚠️  $routeError",
                            style = PahadiRaahTypography.bodySmall.copy(color = StatusError)
                        )
                    }
                }

                // ── ROUTE HERO CARD ───────────────────────────────────────────
                if (!isRouteLoading) {
                    RouteHeroCard(booking = booking)
                    DriverMiniCard(booking = booking)
                }

                // ── SEAT SELECTOR ─────────────────────────────────────────────
                ConfirmSectionLabel("SEATS")
                SeatRow(
                    seats      = seats,
                    maxSeats   = maxSeats,
                    onIncrease = { if (seats < maxSeats) seats++ },
                    onDecrease = { if (seats > 1) seats-- }
                )

                // ── TRIP DETAILS ──────────────────────────────────────────────
                if (!isRouteLoading) {
                    ConfirmSectionLabel("TRIP DETAILS")
                    TripDetailsCard(booking = booking)
                }

                // ── FARE BREAKDOWN ────────────────────────────────────────────
                ConfirmSectionLabel("FARE BREAKDOWN")
                FareBreakdownCard(
                    farePerSeat = "₹$baseFareNum",
                    seats       = seats,
                    totalFare   = formatAmount(totalFare),
                    serviceFee  = formatAmount(serviceFee),
                    grandTotal  = formatAmount(grandTotal)
                )

                // ── PAYMENT METHOD ────────────────────────────────────────────
                ConfirmSectionLabel("PAYMENT METHOD")
                PaymentMethodSelector(
                    selected = paymentMethod,
                    onSelect = { paymentMethod = it }
                )

                // ── BOOKING ERROR ─────────────────────────────────────────────
                if (bookingError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PahadiRaahShapes.medium)
                            .background(StatusError.copy(alpha = 0.1f))
                            .border(1.dp, StatusError.copy(alpha = 0.3f), PahadiRaahShapes.medium)
                            .padding(16.dp)
                    ) {
                        Text(
                            text  = "⚠️  $bookingError",
                            style = PahadiRaahTypography.bodySmall.copy(color = StatusError)
                        )
                    }
                }

                // ── TERMS ─────────────────────────────────────────────────────
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PahadiRaahShapes.medium)
                        .background(SurfaceLight)
                        .border(1.dp, BorderSubtle, PahadiRaahShapes.medium)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = { agreedToTerms = !agreedToTerms }
                        )
                        .padding(14.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(PahadiRaahShapes.small)
                            .background(
                                if (agreedToTerms)
                                    Brush.verticalGradient(GradientMoss)
                                else
                                    Brush.verticalGradient(listOf(SurfaceMedium, SurfaceMedium))
                            )
                            .border(1.5.dp, if (agreedToTerms) Sage else BorderSubtle, PahadiRaahShapes.small)
                    ) {
                        if (agreedToTerms) {
                            Icon(
                                imageVector        = Icons.Default.Check,
                                contentDescription = null,
                                tint               = Snow,
                                modifier           = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text  = "I agree to the cancellation policy and terms of service for this mountain route",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color      = Mist.copy(alpha = 0.8f),
                            fontSize   = 12.sp,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── BOTTOM CTA ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Pine.copy(alpha = 0.95f), Pine)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                // Grand total display
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text  = "TOTAL PAYABLE",
                            style = EyebrowStyle.copy(fontSize = 9.sp)
                        )
                        Text(
                            text  = formatAmount(grandTotal),
                            style = PahadiRaahTypography.headlineSmall.copy(color = Snow)
                        )
                    }
                    // Payment method icon
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(SurfaceLight)
                            .border(1.dp, BorderSubtle, PillShape)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text  = when (paymentMethod) {
                                PaymentMethod.UPI  -> "📱 UPI"
                                PaymentMethod.CARD -> "💳 Card"
                                PaymentMethod.CASH -> "💵 Cash"
                            },
                            style = PahadiRaahTypography.labelMedium.copy(
                                color         = Mist,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }

                // Edit mode banner — shown when passenger already has a booking
                if (isEditMode && existingBooking != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PahadiRaahShapes.medium)
                            .background(Amber.copy(alpha = 0.08f))
                            .border(1.dp, Amber.copy(alpha = 0.3f), PahadiRaahShapes.medium)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✏️", fontSize = 16.sp)
                            Text(
                                "Edit Booking",
                                style = PahadiRaahTypography.titleSmall.copy(color = Amber, fontSize = 13.sp)
                            )
                        }
                        Text(
                            "You already have ${existingBooking.seats} seat${if (existingBooking.seats > 1) "s" else ""} booked. " +
                                    "Select new total seat count below (minimum ${minSeats}).",
                            style = PahadiRaahTypography.bodySmall.copy(color = Mist, fontSize = 11.sp, lineHeight = 16.sp)
                        )
                        if (seats < minSeats) {
                            Text(
                                "⚠ Must book at least $minSeats seats total to add more.",
                                style = PahadiRaahTypography.bodySmall.copy(color = Amber.copy(alpha = 0.8f), fontSize = 11.sp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Confirm / Update button
                // ── Departed warning — shown instead of confirm when trip is gone ─────
                if (isDeparted) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .clip(com.kunpitech.pahadiraah.ui.theme.PahadiRaahShapes.medium)
                            .background(com.kunpitech.pahadiraah.ui.theme.StatusError.copy(alpha = 0.10f))
                            .border(
                                1.dp,
                                com.kunpitech.pahadiraah.ui.theme.StatusError.copy(alpha = 0.35f),
                                com.kunpitech.pahadiraah.ui.theme.PahadiRaahShapes.medium
                            )
                            .padding(16.dp)
                    ) {
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                        ) {
                            Text("🚫", fontSize = 28.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "This trip has already departed",
                                style     = com.kunpitech.pahadiraah.ui.theme.PahadiRaahTypography
                                    .titleSmall.copy(color = com.kunpitech.pahadiraah.ui.theme.StatusError),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Bookings are closed. Please search for another route.",
                                style     = com.kunpitech.pahadiraah.ui.theme.PahadiRaahTypography
                                    .bodySmall.copy(
                                        color     = com.kunpitech.pahadiraah.ui.theme.MistVeil,
                                        fontSize  = 12.sp
                                    ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                ConfirmBookingButton(
                    enabled   = isReady,
                    isLoading = isBookingLoading,
                    label     = if (isEditMode) "Update Booking" else "Confirm Booking",
                    onClick   = {
                        if (isEditMode && existingBooking != null) {
                            bookingVm.updateBooking(
                                bookingId   = existingBooking.id,
                                seats       = seats,
                                farePerSeat = baseFareNum
                            )
                        } else {
                            bookingVm.confirmBooking(
                                routeId       = routeId,
                                seats         = seats,
                                farePerSeat   = baseFareNum,
                                paymentMethod = paymentMethod.name.lowercase()
                            )
                        }
                    }
                )

                if (!agreedToTerms && !isBookingLoading && !alreadyBooked) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text      = "Please agree to terms to continue",
                        style     = PahadiRaahTypography.bodySmall.copy(
                            color    = Sage.copy(alpha = 0.4f),
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
//  ROUTE HERO CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RouteHeroCard(booking: BookingSummary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.75f))))
            .padding(20.dp)
    ) {
        // Decorative mountain
        Text(
            text     = booking.routeEmoji,
            fontSize = 80.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .alpha(0.12f)
        )

        Column {
            Text(text = "YOUR TRIP", style = EyebrowStyle.copy(fontSize = 9.sp, color = Amber))
            Spacer(modifier = Modifier.height(10.dp))

            // Origin → Destination
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column {
                    Text(
                        text  = "FROM",
                        style = FormLabelStyle.copy(fontSize = 8.sp, color = Snow.copy(alpha = 0.5f))
                    )
                    Text(
                        text  = booking.origin,
                        style = PahadiRaahTypography.headlineSmall.copy(color = Snow, fontSize = 22.sp)
                    )
                }

                // Animated connector
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(PillShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Sage.copy(alpha = 0.4f), Amber.copy(alpha = 0.6f))
                                )
                            )
                    )
                    Text(
                        text  = booking.duration,
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = Snow.copy(alpha = 0.5f),
                            fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text  = "TO",
                        style = FormLabelStyle.copy(fontSize = 8.sp, color = Snow.copy(alpha = 0.5f))
                    )
                    Text(
                        text  = booking.destination,
                        style = PahadiRaahTypography.headlineSmall.copy(color = Snow, fontSize = 22.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date + time chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroChip(label = "📅 ${booking.date}")
                HeroChip(label = "🕐 ${booking.time}")
            }
        }
    }
}

@Composable
fun HeroChip(label: String) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(Snow.copy(alpha = 0.1f))
            .border(1.dp, Snow.copy(alpha = 0.15f), PillShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text  = label,
            style = PahadiRaahTypography.bodySmall.copy(color = Snow.copy(alpha = 0.85f), fontSize = 11.sp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DRIVER MINI CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverMiniCard(booking: BookingSummary) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(16.dp)
    ) {
        // Avatar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(PahadiRaahShapes.medium)
                .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.7f))))
        ) {
            Text(text = booking.driverEmoji, fontSize = 24.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = booking.driverName,
                style    = PahadiRaahTypography.titleSmall.copy(color = Snow),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text  = "⭐ ${booking.driverRating}",
                    style = PahadiRaahTypography.bodySmall.copy(color = Amber, fontSize = 11.sp)
                )
                Text("•", style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.3f)))
                Text(
                    text  = "${booking.vehicleEmoji} ${booking.vehicle}",
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
                )
            }
        }

        // Verified badge
        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(Moss.copy(alpha = 0.15f))
                .border(1.dp, Sage.copy(alpha = 0.25f), PillShape)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text  = "🔒 Verified",
                style = BadgeStyle.copy(color = Sage, fontSize = 10.sp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEAT ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SeatRow(
    seats: Int,
    maxSeats: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(text = "🪑", fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = "$seats seat${if (seats > 1) "s" else ""} selected",
                style = PahadiRaahTypography.titleSmall.copy(color = Snow)
            )
            Text(
                text  = "Max $maxSeats seats per booking",
                style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 10.sp)
            )
        }
        // Stepper
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ConfirmStepBtn(label = "−", enabled = seats > 1, onClick = onDecrease)
            Text(
                text  = "$seats",
                style = PahadiRaahTypography.headlineSmall.copy(color = Snow, fontSize = 24.sp),
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center
            )
            ConfirmStepBtn(label = "+", enabled = seats < maxSeats, onClick = onIncrease)
        }
    }
}

@Composable
fun ConfirmStepBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed && enabled) 0.88f else 1f,
        spring(stiffness = Spring.StiffnessHigh), label = "csb"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (enabled) Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.7f)))
                else Brush.verticalGradient(listOf(SurfaceMedium, SurfaceMedium))
            )
            .border(1.dp, if (enabled) Sage.copy(alpha = 0.3f) else BorderSubtle, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = enabled,
                onClick           = onClick
            )
    ) {
        Text(
            text  = label,
            style = PahadiRaahTypography.titleMedium.copy(
                color    = if (enabled) Snow else Sage.copy(alpha = 0.2f),
                fontSize = 18.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TRIP DETAILS CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TripDetailsCard(booking: BookingSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TripDetailRow("📅", "Date",       booking.date)
        TripDetailDivider()
        TripDetailRow("🕐", "Departure",  booking.time)
        TripDetailDivider()
        TripDetailRow("⏱️", "Duration",   booking.duration)
        TripDetailDivider()
        TripDetailRow("🚙", "Vehicle",    "${booking.vehicleEmoji} ${booking.vehicle}")
        TripDetailDivider()
        TripDetailRow("📍", "Pickup",     booking.origin + " (exact point TBD)")
        TripDetailDivider()
        TripDetailRow("🏁", "Drop-off",   booking.destination + " Town Center")
    }
}

@Composable
fun TripDetailRow(emoji: String, label: String, value: String) {
    Row(
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = emoji, fontSize = 15.sp, modifier = Modifier.width(20.dp))
        Text(
            text  = label,
            style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 12.sp),
            modifier = Modifier.width(72.dp)
        )
        Text(
            text  = value,
            style = PahadiRaahTypography.bodyMedium.copy(color = Mist, fontSize = 13.sp),
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
    }
}

@Composable
fun TripDetailDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, BorderSubtle, Color.Transparent)
                )
            )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  FARE BREAKDOWN CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FareBreakdownCard(
    farePerSeat: String,
    seats: Int,
    totalFare: String,
    serviceFee: String,
    grandTotal: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FareRow(label = "$farePerSeat × $seats seat${if (seats > 1) "s" else ""}", value = totalFare, highlight = false)
        FareRow(label = "Platform fee (5%)",   value = serviceFee, highlight = false)
        TripDetailDivider()
        FareRow(label = "Grand Total", value = grandTotal, highlight = true)

        // Savings note
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PahadiRaahShapes.small)
                .background(Moss.copy(alpha = 0.1f))
                .border(1.dp, Sage.copy(alpha = 0.2f), PahadiRaahShapes.small)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(
                text  = "💚 You save ~60% compared to private taxi rates on this route",
                style = PahadiRaahTypography.bodySmall.copy(
                    color      = Sage,
                    fontSize   = 11.sp,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

@Composable
fun FareRow(label: String, value: String, highlight: Boolean) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = PahadiRaahTypography.bodyMedium.copy(
                color    = if (highlight) Snow else Mist.copy(alpha = 0.7f),
                fontSize = if (highlight) 15.sp else 13.sp
            )
        )
        Text(
            text  = value,
            style = if (highlight) FareStyle.copy(fontSize = 18.sp)
            else PahadiRaahTypography.bodyMedium.copy(color = Mist, fontSize = 13.sp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PAYMENT METHOD SELECTOR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PaymentMethodSelector(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit
) {
    val methods = listOf(
        Triple(PaymentMethod.UPI,  "📱", "UPI / GPay"),
        Triple(PaymentMethod.CARD, "💳", "Credit / Debit Card"),
        Triple(PaymentMethod.CASH, "💵", "Pay Cash to Driver"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        methods.forEach { (method, emoji, label) ->
            val isSelected        = selected == method
            val interactionSource = remember { MutableInteractionSource() }
            val bgColor by animateColorAsState(
                if (isSelected) Moss.copy(alpha = 0.12f) else SurfaceLight, tween(150), label = "pm"
            )
            val borderColor by animateColorAsState(
                if (isSelected) Sage.copy(alpha = 0.4f) else BorderSubtle, tween(150), label = "pmB"
            )

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PahadiRaahShapes.medium)
                    .background(bgColor)
                    .border(1.dp, borderColor, PahadiRaahShapes.medium)
                    .clickable(
                        interactionSource = interactionSource,
                        indication        = null,
                        onClick           = { onSelect(method) }
                    )
                    .padding(16.dp)
            ) {
                Text(text = emoji, fontSize = 22.sp)
                Text(
                    text     = label,
                    style    = PahadiRaahTypography.bodyMedium.copy(
                        color = if (isSelected) Snow else Mist
                    ),
                    modifier = Modifier.weight(1f)
                )
                // Radio dot
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected)
                                Brush.verticalGradient(GradientMoss)
                            else
                                Brush.verticalGradient(listOf(SurfaceMedium, SurfaceMedium))
                        )
                        .border(1.5.dp, if (isSelected) Sage else BorderSubtle, CircleShape)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Snow)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CONFIRM BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ConfirmBookingButton(enabled: Boolean, isLoading: Boolean = false, label: String = "Confirm Booking", onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed && enabled) 0.97f else 1f,
        spring(stiffness = Spring.StiffnessMedium), label = "cbb"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .clip(PillShape)
            .background(
                if (enabled)
                    Brush.horizontalGradient(GradientMoss)
                else
                    Brush.horizontalGradient(listOf(Forest.copy(alpha = 0.5f), Moss.copy(alpha = 0.3f)))
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = enabled,
                onClick           = onClick
            )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color       = Snow,
                strokeWidth = 2.dp,
                modifier    = Modifier.size(24.dp)
            )
        } else {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = if (label.startsWith("Update")) "✏️" else "🎫", fontSize = 18.sp)
                Text(
                    text  = label,
                    style = PahadiRaahTypography.labelLarge.copy(
                        color    = if (enabled) Snow else Sage.copy(alpha = 0.4f),
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SECTION LABEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ConfirmSectionLabel(label: String) {
    Text(text = label, style = EyebrowStyle.copy(fontSize = 10.sp))
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUCCESS OVERLAY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BookingSuccessOverlay(
    booking: BookingSummary,
    onTrack: () -> Unit,
    onHome:  () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(400), label = "bsA")
    val scale by animateFloatAsState(
        if (visible) 1f else 0.7f,
        tween(500, easing = EaseOutBack), label = "bsS"
    )

    LaunchedEffect(Unit) { visible = true }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Pine)
            .alpha(alpha)
            .systemBarsPadding()
    ) {
        // Background glow
        Box(
            modifier = Modifier
                .size(340.dp)
                .background(
                    Brush.radialGradient(listOf(Moss.copy(alpha = 0.18f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier
                .padding(horizontal = 32.dp)
                .scale(scale)
        ) {
            // Check circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(GradientMoss))
            ) {
                Text(text = "✓", fontSize = 44.sp, color = Snow)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text      = "Booking Confirmed!",
                style     = PahadiRaahTypography.headlineMedium.copy(color = Snow),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text      = "${booking.origin} → ${booking.destination}",
                style     = PahadiRaahTypography.titleSmall.copy(color = Sage),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Booking reference card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PahadiRaahShapes.large)
                    .background(SurfaceLight)
                    .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
                    .padding(18.dp),
                verticalArrangement   = Arrangement.spacedBy(10.dp),
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                Text(
                    text  = "BOOKING REFERENCE",
                    style = EyebrowStyle.copy(fontSize = 9.sp)
                )
                Text(
                    text  = "#PH${booking.bookingId.padStart(6, '0')}",
                    style = PahadiRaahTypography.headlineMedium.copy(
                        color        = Snow,
                        letterSpacing = 3.sp
                    )
                )
                TripDetailDivider()
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = "📅 ${booking.date}",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
                    )
                    Text(
                        text  = "🕐 ${booking.time}",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
                    )
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = "Driver: ${booking.driverName}",
                        style = PahadiRaahTypography.bodySmall.copy(color = Mist, fontSize = 11.sp)
                    )
                    Text(
                        text  = booking.grandTotal,
                        style = FareStyle.copy(fontSize = 14.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Track Trip button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(PillShape)
                    .background(Brush.horizontalGradient(GradientMoss))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onTrack
                    )
            ) {
                Text(
                    text  = "🗺️  Track My Trip",
                    style = PahadiRaahTypography.labelLarge.copy(color = Snow, fontSize = 15.sp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Go home
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(PillShape)
                    .background(SurfaceLight)
                    .border(1.dp, BorderSubtle, PillShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onHome
                    )
            ) {
                Text(
                    text  = "Back to Home",
                    style = PahadiRaahTypography.labelMedium.copy(color = Mist, letterSpacing = 0.sp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PREVIEW
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BookingConfirmPreview() {
    PahadiRaahTheme {
        BookingConfirmScreen(
            routeId     = "1",
            onBack      = {},
            onTrackTrip = {},
            onHome      = {}
        )
    }
}