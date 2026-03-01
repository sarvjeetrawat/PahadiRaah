package com.kunpitech.pahadiraah.ui.screens.driver

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kunpitech.pahadiraah.data.model.BookingDto
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.viewmodel.BookingViewModel
import com.kunpitech.pahadiraah.ui.theme.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
//  DATA MODEL
// ─────────────────────────────────────────────────────────────────────────────

data class BookingRequest(
    val id: String,
    val passengerName: String,
    val passengerEmoji: String,
    val passengerPhotoUrl: String? = null,
    val route: String,
    val seats: Int,
    val fare: String,
    val time: String,
    val rating: Float,
    val trips: Int,
    val status: RequestStatus,
    val phone: String = "+91 98765 43210"
)

enum class RequestStatus { PENDING, ACCEPTED, DECLINED }

enum class RequestFilter { ALL, PENDING, ACCEPTED, DECLINED }

// ─────────────────────────────────────────────────────────────────────────────
//  BOOKING REQUESTS SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BookingRequestsScreen(
    routeId:    String,
    onBack:     () -> Unit,
    bookingVm:  BookingViewModel = hiltViewModel()
) {
    val routeBookingsState by bookingVm.routeBookings.collectAsStateWithLifecycle()
    val newBookingAlert    by bookingVm.newBookingAlert.collectAsStateWithLifecycle(null)

    // Show toast when a new booking arrives via realtime
    var toastBooking by remember { mutableStateOf<BookingDto?>(null) }
    LaunchedEffect(newBookingAlert) {
        newBookingAlert?.let {
            toastBooking = it
            kotlinx.coroutines.delay(4000)
            toastBooking = null
        }
    }

    LaunchedEffect(routeId) {
        if (routeId == "all") {
            bookingVm.loadDriverBookings()
        } else {
            bookingVm.loadBookingsForRoute(routeId)
            bookingVm.subscribeToBookings(routeId)
        }
    }

    val isLoading  = routeBookingsState is UiState.Loading
    val errorMsg   = (routeBookingsState as? UiState.Error)?.message

    // Map BookingDto → UI BookingRequest model
    val allRequests = when (val s = routeBookingsState) {
        is UiState.Success -> s.data.map { b ->
            BookingRequest(
                id            = b.id,
                passengerName = b.users?.name ?: "Passenger",
                passengerEmoji    = b.users?.emoji ?: "🧑",
                passengerPhotoUrl = b.users?.avatarUrl,
                route         = "${b.routes?.origin ?: ""} → ${b.routes?.destination ?: ""}",
                seats         = b.seats,
                fare          = "₹${b.grandTotal}",
                time          = b.createdAt?.take(10) ?: "Recent",
                rating        = b.users?.avgRating?.toFloat() ?: 0f,
                trips         = b.users?.totalTrips ?: 0,
                status        = when (b.status) {
                    "accepted"  -> RequestStatus.ACCEPTED
                    "cancelled" -> RequestStatus.DECLINED
                    else        -> RequestStatus.PENDING
                }
            )
        }
        else -> emptyList()
    }

    var activeFilter by remember { mutableStateOf(RequestFilter.PENDING) }

    val filtered = when (activeFilter) {
        RequestFilter.ALL      -> allRequests
        RequestFilter.PENDING  -> allRequests.filter { it.status == RequestStatus.PENDING }
        RequestFilter.ACCEPTED -> allRequests.filter { it.status == RequestStatus.ACCEPTED }
        RequestFilter.DECLINED -> allRequests.filter { it.status == RequestStatus.DECLINED }
    }

    val pendingCount  = allRequests.count { it.status == RequestStatus.PENDING }
    val acceptedCount = allRequests.count { it.status == RequestStatus.ACCEPTED }
    val declinedCount = allRequests.count { it.status == RequestStatus.DECLINED }

    // ── Entrance animations ───────────────────────────────────────────────────
    var started by remember { mutableStateOf(false) }
    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(400), label = "ha")
    val headerOffset by animateFloatAsState(if (started) 0f else -24f, tween(500, easing = EaseOutCubic), label = "hY")
    val listAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 200), label = "la")
    val listOffset   by animateFloatAsState(if (started) 0f else 30f, tween(600, delayMillis = 200, easing = EaseOutCubic), label = "lY")
    LaunchedEffect(Unit) { started = true }

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
                    Brush.verticalGradient(listOf(Gold.copy(alpha = 0.07f), Color.Transparent))
                )
        )

        // ── REALTIME NEW BOOKING TOAST ────────────────────────────────────────
        AnimatedVisibility(
            visible  = toastBooking != null,
            enter    = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(250)),
            exit     = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f)
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            toastBooking?.let { b ->
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .clip(PahadiRaahShapes.large)
                        .background(Brush.horizontalGradient(GradientMoss))
                        .border(1.dp, Sage.copy(alpha = 0.4f), PahadiRaahShapes.large)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(text = "🔔", fontSize = 18.sp)
                    Column {
                        Text(
                            text  = "New Booking Request!",
                            style = PahadiRaahTypography.labelMedium.copy(
                                color         = Snow,
                                letterSpacing = 0.sp
                            )
                        )
                        Text(
                            text  = "${b.users?.name ?: "A passenger"} wants ${b.seats} seat${if (b.seats > 1) "s" else ""}",
                            style = PahadiRaahTypography.bodySmall.copy(color = Snow.copy(alpha = 0.75f))
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "BOOKING REQUESTS", style = EyebrowStyle.copy(fontSize = 10.sp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = "Manage Passengers",
                        style = PahadiRaahTypography.titleLarge.copy(color = Snow)
                    )
                }

                // Pending badge
                if (pendingCount > 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(PillShape)
                            .background(Gold.copy(alpha = 0.15f))
                            .border(1.dp, Gold.copy(alpha = 0.3f), PillShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text  = "$pendingCount pending",
                            style = PahadiRaahTypography.labelSmall.copy(
                                color         = Amber,
                                fontSize      = 11.sp,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }
            }

            // ── SUMMARY STATS ROW ─────────────────────────────────────────────
            if (!isLoading && errorMsg == null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp)
                        .alpha(headerAlpha)
                        .graphicsLayer { translationY = headerOffset }
                ) {
                    RequestStatChip("🕐", "$pendingCount Pending",  Amber, Modifier.weight(1f))
                    RequestStatChip("✅", "$acceptedCount Accepted", Sage,  Modifier.weight(1f))
                    RequestStatChip("✗",  "$declinedCount Declined", Mist.copy(alpha = 0.6f), Modifier.weight(1f))
                }

                // ── FILTER TABS ───────────────────────────────────────────────────
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .alpha(listAlpha)
                ) {
                    val filters = listOf(
                        RequestFilter.PENDING  to "Pending ($pendingCount)",
                        RequestFilter.ACCEPTED to "Accepted ($acceptedCount)",
                        RequestFilter.DECLINED to "Declined ($declinedCount)",
                        RequestFilter.ALL      to "All (${allRequests.size})",
                    )
                    items(filters) { (filter, label) ->
                        FilterChip(
                            label      = label,
                            isSelected = activeFilter == filter,
                            onClick    = { activeFilter = filter }
                        )
                    }
                }
            } // end !isLoading && errorMsg == null

            // ── REQUEST LIST ──────────────────────────────────────────────────
            if (isLoading) {
                Box(
                    modifier          = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment  = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color       = Sage,
                            strokeWidth = 2.dp,
                            modifier    = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text  = "Loading requests…",
                            style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.6f))
                        )
                    }
                }
            } else if (errorMsg != null) {
                Box(
                    modifier         = Modifier.weight(1f).fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .clip(PahadiRaahShapes.large)
                            .background(StatusError.copy(alpha = 0.08f))
                            .border(1.dp, StatusError.copy(alpha = 0.25f), PahadiRaahShapes.large)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "⚠️", fontSize = 32.sp)
                        Text(
                            text      = "Failed to load bookings",
                            style     = PahadiRaahTypography.titleSmall.copy(color = Snow),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text      = errorMsg,
                            style     = PahadiRaahTypography.bodySmall.copy(
                                color     = StatusError,
                                textAlign = TextAlign.Center
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(SurfaceLight)
                                .border(1.dp, BorderSubtle, PillShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                    onClick           = {
                                        if (routeId == "all") bookingVm.loadDriverBookings()
                                        else bookingVm.loadBookingsForRoute(routeId)
                                    }
                                )
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text  = "Retry",
                                style = PahadiRaahTypography.labelMedium.copy(color = Mist, letterSpacing = 0.sp)
                            )
                        }
                    }
                }
            } else if (filtered.isEmpty()) {
                EmptyState(
                    filter   = activeFilter,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(listAlpha)
                )
            } else {
                LazyColumn(
                    modifier       = Modifier
                        .weight(1f)
                        .alpha(listAlpha)
                        .graphicsLayer { translationY = listOffset },
                    contentPadding = PaddingValues(
                        start  = 20.dp,
                        end    = 20.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filtered,
                        key   = { it.id }
                    ) { request ->
                        AnimatedVisibility(
                            visible = true,
                            enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        ) {
                            RequestCard(
                                request   = request,
                                onAccept  = { bookingVm.acceptBooking(request.id, routeId) },
                                onDecline = { bookingVm.declineBooking(request.id, routeId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FILTER CHIP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) Moss else SurfaceLight.copy(alpha = 0.5f),
        tween(200), label = "fcBg"
    )
    val borderColor by animateColorAsState(
        if (isSelected) Sage.copy(alpha = 0.5f) else BorderSubtle,
        tween(200), label = "fcBorder"
    )
    val textColor by animateColorAsState(
        if (isSelected) Snow else Sage.copy(alpha = 0.6f),
        tween(200), label = "fcText"
    )

    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(bgColor)
            .border(1.dp, borderColor, PillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text  = label,
            style = PahadiRaahTypography.labelMedium.copy(
                color         = textColor,
                letterSpacing = 0.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  STAT CHIP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RequestStatChip(
    emoji: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(PahadiRaahShapes.small)
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), PahadiRaahShapes.small)
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Text(text = emoji, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text  = label,
            style = PahadiRaahTypography.labelMedium.copy(
                color         = color,
                fontSize      = 11.sp,
                letterSpacing = 0.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  REQUEST CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RequestCard(
    request: BookingRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    // Border color based on status
    val borderColor = when (request.status) {
        RequestStatus.PENDING  -> Gold.copy(alpha = 0.3f)
        RequestStatus.ACCEPTED -> Sage.copy(alpha = 0.3f)
        RequestStatus.DECLINED -> Mist.copy(alpha = 0.1f)
    }
    val bgColor = when (request.status) {
        RequestStatus.PENDING  -> Gold.copy(alpha = 0.05f)
        RequestStatus.ACCEPTED -> Moss.copy(alpha = 0.06f)
        RequestStatus.DECLINED -> SurfaceLight
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(bgColor)
            .border(1.dp, borderColor, PahadiRaahShapes.large)
            .padding(18.dp)
    ) {
        // ── Passenger Info Row ────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(PahadiRaahShapes.medium)
                    .background(
                        Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.7f)))
                    )
                    .border(2.dp, BorderSubtle, PahadiRaahShapes.medium)
            ) {
                if (!request.passengerPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model              = request.passengerPhotoUrl,
                        contentDescription = request.passengerName,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.size(52.dp).clip(PahadiRaahShapes.medium)
                    )
                } else {
                    Text(text = request.passengerEmoji, fontSize = 24.sp)
                }
            }

            // Name + meta
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = request.passengerName,
                    style    = PahadiRaahTypography.titleSmall.copy(color = Snow),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Star rating
                    Text(
                        text  = "⭐ ${request.rating}",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = Amber,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text  = "•",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.4f))
                    )
                    Text(
                        text  = "${request.trips} trips",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = Sage,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text  = "•",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.4f))
                    )
                    Text(
                        text  = request.time,
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = Sage.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Fare
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text  = request.fare,
                    style = FareStyle.copy(fontSize = 18.sp)
                )
                Text(
                    text  = "${request.seats} seat${if (request.seats > 1) "s" else ""}",
                    style = PahadiRaahTypography.bodySmall.copy(
                        color    = Sage,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Route pill ────────────────────────────────────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(PahadiRaahShapes.small)
                .background(SurfaceLight)
                .border(1.dp, BorderSubtle, PahadiRaahShapes.small)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Sage)
            )
            Text(
                text     = request.route,
                style    = PahadiRaahTypography.bodySmall.copy(color = Mist, fontSize = 13.sp),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Gold)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Status badge OR action buttons ────────────────────────────────────
        when (request.status) {
            RequestStatus.PENDING -> {
                // Accept + Decline buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Decline
                    ActionButton(
                        label    = "Decline",
                        icon     = Icons.Default.Close,
                        style    = ActionButtonStyle.GHOST,
                        modifier = Modifier.weight(1f),
                        onClick  = onDecline
                    )
                    // Accept
                    ActionButton(
                        label    = "Accept",
                        icon     = Icons.Default.Check,
                        style    = ActionButtonStyle.PRIMARY,
                        modifier = Modifier.weight(2f),
                        onClick  = onAccept
                    )
                }
            }

            RequestStatus.ACCEPTED -> {
                // Show status + call button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Accepted badge
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(PahadiRaahShapes.small)
                            .background(Moss.copy(alpha = 0.15f))
                            .border(1.dp, Sage.copy(alpha = 0.25f), PahadiRaahShapes.small)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Check,
                            contentDescription = null,
                            tint               = Sage,
                            modifier           = Modifier.size(14.dp)
                        )
                        Text(
                            text  = "Accepted",
                            style = PahadiRaahTypography.labelMedium.copy(
                                color         = Sage,
                                letterSpacing = 0.sp
                            )
                        )
                    }

                    // Call button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(GradientMoss))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { /* open dialer */ }
                            )
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Phone,
                            contentDescription = "Call passenger",
                            tint               = Snow,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            }

            RequestStatus.DECLINED -> {
                // Declined badge
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PahadiRaahShapes.small)
                        .background(SurfaceMedium)
                        .border(1.dp, BorderSubtle, PahadiRaahShapes.small)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Close,
                        contentDescription = null,
                        tint               = Mist.copy(alpha = 0.4f),
                        modifier           = Modifier.size(14.dp)
                    )
                    Text(
                        text  = "Request Declined",
                        style = PahadiRaahTypography.labelMedium.copy(
                            color         = Mist.copy(alpha = 0.5f),
                            letterSpacing = 0.sp
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ACTION BUTTON
// ─────────────────────────────────────────────────────────────────────────────

enum class ActionButtonStyle { PRIMARY, GHOST }

@Composable
fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    style: ActionButtonStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.95f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "abScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .scale(scale)
            .clip(PahadiRaahShapes.small)
            .background(
                when (style) {
                    ActionButtonStyle.PRIMARY -> Brush.horizontalGradient(GradientMoss)
                    ActionButtonStyle.GHOST   -> Brush.horizontalGradient(
                        listOf(SurfaceLight, SurfaceLight)
                    )
                }
            )
            .border(
                1.dp,
                when (style) {
                    ActionButtonStyle.PRIMARY -> Sage.copy(alpha = 0.3f)
                    ActionButtonStyle.GHOST   -> BorderSubtle
                },
                PahadiRaahShapes.small
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint = when (style) {
                    ActionButtonStyle.PRIMARY -> Snow
                    ActionButtonStyle.GHOST   -> Mist.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = PahadiRaahTypography.labelMedium.copy(
                    color = when (style) {
                        ActionButtonStyle.PRIMARY -> Snow
                        ActionButtonStyle.GHOST   -> Mist.copy(alpha = 0.6f)
                    },
                    letterSpacing = 0.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EMPTY STATE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    filter: RequestFilter,
    modifier: Modifier = Modifier
) {
    val (emoji, title, sub) = when (filter) {
        RequestFilter.PENDING  -> Triple("🔔", "No Pending Requests", "All caught up! New requests will appear here.")
        RequestFilter.ACCEPTED -> Triple("✅", "No Accepted Requests", "Accept a request to see it here.")
        RequestFilter.DECLINED -> Triple("✗",  "No Declined Requests", "Declined requests will appear here.")
        RequestFilter.ALL      -> Triple("🗂️", "No Requests Yet",      "Booking requests will appear once you post a route.")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(40.dp)
    ) {
        Text(text = emoji, fontSize = 52.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text      = title,
            style     = PahadiRaahTypography.titleMedium.copy(color = Snow),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = sub,
            style     = PahadiRaahTypography.bodySmall.copy(
                color     = Sage.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PREVIEW
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BookingRequestsPreview() {
    PahadiRaahTheme {
        BookingRequestsScreen(routeId = "all", onBack = {})
    }
}