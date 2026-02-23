package com.kunpitech.pahadiraah.ui.screens.driver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.kunpitech.pahadiraah.data.model.RouteDto
import com.kunpitech.pahadiraah.data.model.RouteBookingDto
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.viewmodel.RouteViewModel
import com.kunpitech.pahadiraah.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class ActiveRoute(
    val id: String,
    val emoji: String,
    val origin: String,
    val destination: String,
    val date: String,
    val time: String,
    val totalSeats: Int,
    val bookedSeats: Int,
    val fare: String,
    val vehicle: String,
    val status: RouteStatus,
    val passengers: List<PassengerMini> = emptyList(),
    val earnings: String = "₹0"
)

data class PassengerMini(
    val name: String,
    val emoji: String,
    val seats: Int,
    val fare: String
)

enum class RouteStatus { UPCOMING, ONGOING, COMPLETED, CANCELLED }
enum class RouteFilter  { ALL, UPCOMING, ONGOING, COMPLETED }

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ActiveRoutesScreen(
    onBack:         () -> Unit,
    onViewRequests: (String) -> Unit = {},
    routeVm:        RouteViewModel = hiltViewModel()
) {
    val activeRoutesState by routeVm.activeRoutes.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { routeVm.loadActiveRoutes() }

    // Map RouteDto → ActiveRoute UI model
    val routes = when (val s = activeRoutesState) {
        is UiState.Success -> s.data.map { r ->
            val bookedSeats = r.seatsTotal - r.seatsLeft
            // Map joined bookings → PassengerMini (only accepted bookings)
            val passengers = r.bookings
                .filter { it.status == "accepted" }
                .map { b ->
                    PassengerMini(
                        name  = b.users?.name  ?: "Passenger",
                        emoji = b.users?.emoji ?: "🧑",
                        seats = b.seats,
                        fare  = "₹${b.grandTotal}"
                    )
                }
            ActiveRoute(
                id          = r.id,
                emoji       = when {
                    r.origin.contains("Shimla",      ignoreCase = true) -> "🏔️"
                    r.origin.contains("Dehradun",    ignoreCase = true) -> "🌄"
                    r.origin.contains("Nainital",    ignoreCase = true) -> "⛰️"
                    r.origin.contains("Dharamshala", ignoreCase = true) -> "🌲"
                    r.origin.contains("Rishikesh",   ignoreCase = true) -> "🌊"
                    else -> "🏕️"
                },
                origin      = r.origin,
                destination = r.destination,
                date        = r.date,
                time        = r.time.take(5),
                totalSeats  = r.seatsTotal,
                bookedSeats = bookedSeats,
                fare        = "₹${r.farePerSeat}",
                vehicle     = r.vehicleId ?: "Vehicle",
                status      = when (r.status) {
                    "ongoing"   -> RouteStatus.ONGOING
                    "completed" -> RouteStatus.COMPLETED
                    "cancelled" -> RouteStatus.CANCELLED
                    else        -> RouteStatus.UPCOMING
                },
                passengers  = passengers,
                earnings    = "₹${r.farePerSeat * bookedSeats}"
            )
        }
        else -> emptyList()
    }

    val isLoading = activeRoutesState is UiState.Loading
    val errorMsg  = (activeRoutesState as? UiState.Error)?.message

    var activeFilter by remember { mutableStateOf(RouteFilter.ALL) }
    var expandedId   by remember { mutableStateOf<String?>(null) }

    val filtered = when (activeFilter) {
        RouteFilter.ALL       -> routes
        RouteFilter.UPCOMING  -> routes.filter { it.status == RouteStatus.UPCOMING }
        RouteFilter.ONGOING   -> routes.filter { it.status == RouteStatus.ONGOING }
        RouteFilter.COMPLETED -> routes.filter { it.status == RouteStatus.COMPLETED }
    }

    val upcomingCount  = routes.count { it.status == RouteStatus.UPCOMING }
    val ongoingCount   = routes.count { it.status == RouteStatus.ONGOING }
    val completedCount = routes.count { it.status == RouteStatus.COMPLETED }

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
                .height(300.dp)
                .background(
                    Brush.verticalGradient(listOf(Moss.copy(alpha = 0.1f), Color.Transparent))
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
                    Text(text = "MY ROUTES", style = EyebrowStyle.copy(fontSize = 10.sp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = "Active Routes",
                        style = PahadiRaahTypography.titleLarge.copy(color = Snow)
                    )
                }
            }

            // ── STATS BANNER ──────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
                    .alpha(headerAlpha)
                    .graphicsLayer { translationY = headerOffset }
            ) {
                RouteStatCard("📍", "$upcomingCount",  "Upcoming",  Sage,  Modifier.weight(1f))
                RouteStatCard("🔴", "$ongoingCount",   "Ongoing",   Amber, Modifier.weight(1f))
                RouteStatCard("✅", "$completedCount", "Done",      Mist,  Modifier.weight(1f))
                //  RouteStatCard("💰", totalEarnings,     "Earned",    Gold,  Modifier.weight(1f))
            }

            // ── FILTER TABS ───────────────────────────────────────────────────
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier
                    .padding(bottom = 16.dp)
                    .alpha(listAlpha)
            ) {
                val filters = listOf(
                    RouteFilter.ALL       to "All (${routes.size})",
                    RouteFilter.UPCOMING  to "Upcoming ($upcomingCount)",
                    RouteFilter.ONGOING   to "Ongoing ($ongoingCount)",
                    RouteFilter.COMPLETED to "Completed ($completedCount)",
                )
                items(filters) { (filter, label) ->
                    RouteFilterChip(
                        label      = label,
                        isSelected = activeFilter == filter,
                        filter     = filter,
                        onClick    = {
                            activeFilter = filter
                            expandedId   = null
                        }
                    )
                }
            }

            // ── ROUTE LIST ────────────────────────────────────────────────────
            when {
                isLoading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Sage, strokeWidth = 2.dp, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading routes…", style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.6f)))
                        }
                    }
                }
                errorMsg != null -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(PahadiRaahShapes.medium)
                                .background(StatusError.copy(alpha = 0.08f))
                                .border(1.dp, StatusError.copy(alpha = 0.25f), PahadiRaahShapes.medium)
                                .padding(24.dp)
                        ) {
                            Text("⚠️", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Failed to load routes", style = PahadiRaahTypography.titleSmall.copy(color = Snow))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(errorMsg, style = PahadiRaahTypography.bodySmall.copy(color = Mist), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(PahadiRaahShapes.small)
                                    .background(Sage.copy(alpha = 0.12f))
                                    .border(1.dp, Sage.copy(alpha = 0.3f), PahadiRaahShapes.small)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { routeVm.loadActiveRoutes() }
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text("Retry", style = PahadiRaahTypography.labelMedium.copy(color = Sage, letterSpacing = 0.sp))
                            }
                        }
                    }
                }
                filtered.isEmpty() -> {
                    RouteEmptyState(
                        filter   = activeFilter,
                        modifier = Modifier.weight(1f).alpha(listAlpha)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier       = Modifier
                            .weight(1f)
                            .alpha(listAlpha)
                            .graphicsLayer { translationY = listOffset },
                        contentPadding = PaddingValues(
                            start  = 20.dp,
                            end    = 20.dp,
                            bottom = 40.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = filtered,
                            key   = { _, r -> r.id }
                        ) { index, route ->
                            val isExpanded = expandedId == route.id

                            AnimatedVisibility(
                                visible = true,
                                enter   = fadeIn(tween(300, index * 60)) +
                                        slideInVertically(tween(350, index * 60)) { it / 3 }
                            ) {
                                RouteCard(
                                    route      = route,
                                    isExpanded = isExpanded,
                                    onToggle   = {
                                        expandedId = if (isExpanded) null else route.id
                                    },
                                    onDelete   = {
                                        routeVm.cancelRoute(route.id)
                                        if (expandedId == route.id) expandedId = null
                                    }
                                )
                            }
                        }
                    }
                }
            } // end when
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ROUTE CARD  — collapsible with passenger list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RouteCard(
    route: ActiveRoute,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = when (route.status) {
        RouteStatus.UPCOMING  -> Sage.copy(alpha = 0.3f)
        RouteStatus.ONGOING   -> Amber.copy(alpha = 0.4f)
        RouteStatus.COMPLETED -> BorderSubtle
        RouteStatus.CANCELLED -> BorderSubtle
    }
    val topGlow = when (route.status) {
        RouteStatus.UPCOMING  -> Moss.copy(alpha = 0.08f)
        RouteStatus.ONGOING   -> Gold.copy(alpha = 0.07f)
        else                  -> Color.Transparent
    }

    // Ongoing pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = if (route.status == RouteStatus.ONGOING)
            infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse)
        else
            infiniteRepeatable(tween(1), RepeatMode.Restart),
        label = "pb"
    )
    val activeBorderColor = if (route.status == RouteStatus.ONGOING)
        Amber.copy(alpha = pulseBorder) else borderColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(topGlow)
            .background(SurfaceLight)
            .border(1.dp, activeBorderColor, PahadiRaahShapes.large)
    ) {
        // ── Main row ──────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onToggle
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Emoji icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(PahadiRaahShapes.medium)
                    .background(
                        Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.7f)))
                    )
            ) {
                Text(text = route.emoji, fontSize = 24.sp)
            }

            // Route info
            Column(modifier = Modifier.weight(1f)) {
                // Origin → Destination
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text     = route.origin,
                        style    = PahadiRaahTypography.titleSmall.copy(color = Snow, fontSize = 14.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text  = "→",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage)
                    )
                    Text(
                        text     = route.destination,
                        style    = PahadiRaahTypography.titleSmall.copy(color = Snow, fontSize = 14.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date + time
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "📅 ${route.date}",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
                    )
                    Text(
                        text  = "•",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.4f))
                    )
                    Text(
                        text  = "🕐 ${route.time}",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Seat fill bar + status badge
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SeatFillBar(
                        booked = route.bookedSeats,
                        total  = route.totalSeats,
                        status = route.status,
                        modifier = Modifier.weight(1f)
                    )
                    RouteStatusBadge(status = route.status)
                }
            }

            // Right: fare + chevron
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text  = route.fare,
                    style = FareStyle.copy(fontSize = 16.sp)
                )
                Text(
                    text  = "/ seat",
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.5f), fontSize = 10.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector        = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint               = Sage.copy(alpha = 0.5f),
                    modifier           = Modifier.size(18.dp)
                )
            }
        }

        // ── Expanded section ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter   = fadeIn(tween(200)) + expandVertically(tween(300, easing = EaseOutCubic)),
            exit    = fadeOut(tween(150)) + shrinkVertically(tween(250))
        ) {
            Column {
                // Divider
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

                // Earnings summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RouteDetailChip("💰", "Earnings", route.earnings, Modifier.weight(1f))
                    RouteDetailChip("🪑", "Booked", "${route.bookedSeats}/${route.totalSeats}", Modifier.weight(1f))
                    RouteDetailChip("🚙", "Vehicle", route.vehicle.split("/").first().trim(), Modifier.weight(1f))
                }

                // Passenger list
                if (route.passengers.isNotEmpty()) {
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text  = "PASSENGERS",
                            style = EyebrowStyle.copy(fontSize = 9.sp)
                        )
                        route.passengers.forEach { passenger ->
                            PassengerRow(passenger = passenger)
                        }
                    }
                } else {
                    // No passengers yet
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .clip(PahadiRaahShapes.small)
                            .background(SurfaceMedium)
                            .padding(vertical = 14.dp)
                    ) {
                        Text(
                            text  = "No passengers booked yet",
                            style = PahadiRaahTypography.bodySmall.copy(
                                color    = Sage.copy(alpha = 0.45f),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Action buttons
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Delete / Cancel
                    if (route.status == RouteStatus.UPCOMING) {
                        RouteActionBtn(
                            icon    = Icons.Default.Delete,
                            label   = "Cancel Route",
                            style   = RouteActionStyle.DANGER,
                            modifier = Modifier.weight(1f),
                            onClick = onDelete
                        )
                        RouteActionBtn(
                            icon    = Icons.Default.Edit,
                            label   = "Edit Route",
                            style   = RouteActionStyle.GHOST,
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )
                    } else {
                        RouteActionBtn(
                            icon    = Icons.Default.Delete,
                            label   = "Remove",
                            style   = RouteActionStyle.DANGER,
                            modifier = Modifier.weight(1f),
                            onClick = onDelete
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEAT FILL BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SeatFillBar(
    booked: Int,
    total: Int,
    status: RouteStatus,
    modifier: Modifier = Modifier
) {
    val fraction   = if (total > 0) booked.toFloat() / total else 0f
    val barColor   = when {
        status == RouteStatus.ONGOING   -> Brush.horizontalGradient(listOf(Gold, Amber))
        status == RouteStatus.COMPLETED -> Brush.horizontalGradient(listOf(Moss.copy(alpha = 0.6f), Moss.copy(alpha = 0.4f)))
        fraction >= 1f                  -> Brush.horizontalGradient(listOf(Gold, Amber))
        fraction >= 0.5f                -> Brush.horizontalGradient(listOf(Moss, Sage))
        else                            -> Brush.horizontalGradient(listOf(Moss.copy(alpha = 0.5f), Moss.copy(alpha = 0.3f)))
    }

    val animFraction by animateFloatAsState(fraction, tween(600, easing = EaseOutCubic), label = "bar")

    Column(modifier = modifier) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(PillShape)
                .background(SurfaceMedium)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animFraction)
                    .fillMaxHeight()
                    .clip(PillShape)
                    .background(barColor)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text  = "$booked/$total seats",
            style = PahadiRaahTypography.bodySmall.copy(
                color    = Sage.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ROUTE STATUS BADGE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RouteStatusBadge(status: RouteStatus) {
    val (label, bg, fg) = when (status) {
        RouteStatus.UPCOMING  -> Triple("Upcoming",  Moss.copy(alpha = 0.18f), Sage)
        RouteStatus.ONGOING   -> Triple("Ongoing",   Gold.copy(alpha = 0.18f), Amber)
        RouteStatus.COMPLETED -> Triple("Done",       SurfaceMedium,            Mist)
        RouteStatus.CANCELLED -> Triple("Cancelled", Color.Red.copy(alpha = 0.1f), Color.Red.copy(alpha = 0.6f))
    }
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, style = BadgeStyle.copy(color = fg, fontSize = 9.sp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PASSENGER ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PassengerRow(passenger: PassengerMini) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.small)
            .background(SurfaceMedium)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Mini avatar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.6f))))
        ) {
            Text(text = passenger.emoji, fontSize = 16.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = passenger.name,
                style    = PahadiRaahTypography.labelMedium.copy(color = Snow, letterSpacing = 0.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text  = "${passenger.seats} seat${if (passenger.seats > 1) "s" else ""}",
                style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
            )
        }

        Text(
            text  = passenger.fare,
            style = FareStyle.copy(fontSize = 15.sp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ROUTE DETAIL CHIP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RouteDetailChip(
    emoji: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(PahadiRaahShapes.small)
            .background(SurfaceMedium)
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text      = value,
            style     = PahadiRaahTypography.titleSmall.copy(color = Snow, fontSize = 14.sp),
            textAlign = TextAlign.Center,
            maxLines  = 1
        )
        Text(
            text      = label,
            style     = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 10.sp),
            textAlign = TextAlign.Center,
            maxLines  = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ROUTE STAT CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RouteStatCard(
    emoji: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(PahadiRaahShapes.small)
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.2f), PahadiRaahShapes.small)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text      = value,
            style     = PahadiRaahTypography.titleSmall.copy(color = color, fontSize = 16.sp),
            textAlign = TextAlign.Center,
            maxLines  = 1
        )
        Text(
            text      = label,
            style     = PahadiRaahTypography.bodySmall.copy(color = color.copy(alpha = 0.7f), fontSize = 9.sp),
            textAlign = TextAlign.Center,
            maxLines  = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ROUTE ACTION BUTTON
// ─────────────────────────────────────────────────────────────────────────────

enum class RouteActionStyle { DANGER, GHOST }

@Composable
fun RouteActionBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    style: RouteActionStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.95f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "rab"
    )

    val (bg, borderClr, textClr) = when (style) {
        RouteActionStyle.DANGER -> Triple(
            Color.Red.copy(alpha = 0.08f),
            Color.Red.copy(alpha = 0.2f),
            Color.Red.copy(alpha = 0.7f)
        )
        RouteActionStyle.GHOST -> Triple(
            SurfaceMedium,
            BorderSubtle,
            Mist.copy(alpha = 0.7f)
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(40.dp)
            .scale(scale)
            .clip(PahadiRaahShapes.small)
            .background(bg)
            .border(1.dp, borderClr, PahadiRaahShapes.small)
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
                tint               = textClr,
                modifier           = Modifier.size(15.dp)
            )
            Text(
                text  = label,
                style = PahadiRaahTypography.labelMedium.copy(
                    color         = textClr,
                    fontSize      = 12.sp,
                    letterSpacing = 0.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FILTER CHIP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RouteFilterChip(
    label: String,
    isSelected: Boolean,
    filter: RouteFilter,
    onClick: () -> Unit
) {
    val accentColor = when (filter) {
        RouteFilter.UPCOMING  -> Sage
        RouteFilter.ONGOING   -> Amber
        RouteFilter.COMPLETED -> Mist
        RouteFilter.ALL       -> Sage
    }

    val bg by animateColorAsState(
        if (isSelected) accentColor.copy(alpha = 0.15f) else SurfaceLight.copy(alpha = 0.5f),
        tween(200), label = "rfBg"
    )
    val border by animateColorAsState(
        if (isSelected) accentColor.copy(alpha = 0.4f) else BorderSubtle,
        tween(200), label = "rfBorder"
    )
    val textClr by animateColorAsState(
        if (isSelected) accentColor else Sage.copy(alpha = 0.5f),
        tween(200), label = "rfText"
    )

    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(bg)
            .border(1.dp, border, PillShape)
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
                color         = textClr,
                letterSpacing = 0.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EMPTY STATE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RouteEmptyState(filter: RouteFilter, modifier: Modifier = Modifier) {
    val (emoji, title, sub) = when (filter) {
        RouteFilter.UPCOMING  -> Triple("📍", "No Upcoming Routes", "Post a new route to get started.")
        RouteFilter.ONGOING   -> Triple("🔴", "No Ongoing Trips", "Your active trips will appear here.")
        RouteFilter.COMPLETED -> Triple("✅", "No Completed Routes", "Completed trips will show up here.")
        RouteFilter.ALL       -> Triple("🗺️", "No Routes Yet", "Post your first mountain route!")
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
                color     = Sage.copy(alpha = 0.55f),
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
fun ActiveRoutesPreview() {
    PahadiRaahTheme {
        ActiveRoutesScreen(onBack = {}, onViewRequests = {})
    }
}