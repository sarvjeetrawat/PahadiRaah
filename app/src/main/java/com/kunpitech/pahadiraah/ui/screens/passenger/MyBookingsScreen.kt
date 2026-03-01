package com.kunpitech.pahadiraah.ui.screens.passenger

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
import com.kunpitech.pahadiraah.data.model.BookingDto
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.viewmodel.BookingViewModel
import com.kunpitech.pahadiraah.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  DATA
// ─────────────────────────────────────────────────────────────────────────────

data class MyBooking(
    val id:           String,
    val routeEmoji:   String,
    val origin:       String,
    val destination:  String,
    val date:         String,
    val time:         String,
    val driverName:   String,
    val driverEmoji:  String,
    val driverPhoto:  String? = null,
    val driverRating: Float,
    val routeId:      String  = "",
    val driverId:     String  = "",
    val seats:        Int,
    val fare:         String,
    val status:       MyBookingStatus,
    val vehicle:      String,
    val bookingRef:   String,
    val hasReview:    Boolean = false
)

enum class MyBookingStatus { UPCOMING, ONGOING, COMPLETED, CANCELLED }
enum class BookingTab       { ALL, UPCOMING, ONGOING, COMPLETED, CANCELLED }


// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
// Map BookingDto from Supabase → existing MyBooking UI model
private fun BookingDto.toMyBooking() = MyBooking(
    id           = id,
    routeEmoji   = when {
        routes?.origin?.contains("Shimla",      ignoreCase = true) == true -> "🏔️"
        routes?.origin?.contains("Dehradun",    ignoreCase = true) == true -> "🌄"
        routes?.origin?.contains("Nainital",    ignoreCase = true) == true -> "⛰️"
        routes?.origin?.contains("Dharamshala", ignoreCase = true) == true -> "🌲"
        routes?.origin?.contains("Rishikesh",   ignoreCase = true) == true -> "🌊"
        else -> "🏕️"
    },
    origin       = routes?.origin ?: "",
    destination  = routes?.destination ?: "",
    date         = routes?.date ?: "",
    time         = routes?.time?.take(5) ?: "",
    driverName   = routes?.users?.name ?: "Driver",
    driverEmoji  = routes?.users?.emoji ?: "🧑",
    driverPhoto  = routes?.users?.avatarUrl,
    driverRating = routes?.users?.avgRating?.toFloat() ?: 0f,
    seats        = seats,
    fare         = "₹$grandTotal",
    status       = when (routes?.status ?: status) {
        "upcoming"  -> MyBookingStatus.UPCOMING
        "ongoing"   -> MyBookingStatus.ONGOING
        "completed" -> MyBookingStatus.COMPLETED
        "cancelled" -> MyBookingStatus.CANCELLED
        "pending"   -> MyBookingStatus.UPCOMING
        "accepted"  -> MyBookingStatus.UPCOMING
        else        -> MyBookingStatus.COMPLETED
    },
    vehicle      = listOfNotNull(
        routes?.vehicles?.model?.ifBlank { null },
        routes?.vehicles?.regNumber?.ifBlank { null }
    ).joinToString(" • ").ifBlank { "Vehicle" },
    bookingRef   = "#$bookingRef",
    hasReview    = hasReview,
    driverId     = routes?.driverId ?: "",
    routeId      = routeId,
)

@Composable
fun MyBookingsScreen(
    onBack:       () -> Unit,
    onTrackTrip:  (String) -> Unit,
    onRateTrip:   (bookingId: String, routeId: String, driverId: String, driverName: String, driverEmoji: String) -> Unit,
    bookingVm:    BookingViewModel = hiltViewModel()
) {
    val bookingsState by bookingVm.myBookings.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { bookingVm.loadMyBookings() }

    val myBookings = when (val s = bookingsState) {
        is UiState.Success -> s.data.map { it.toMyBooking() }
        else -> emptyList()
    }

    var activeTab    by remember { mutableStateOf(BookingTab.ALL) }
    var expandedId   by remember { mutableStateOf<String?>(null) }

    val filtered = when (activeTab) {
        BookingTab.ALL       -> myBookings
        BookingTab.UPCOMING  -> myBookings.filter { it.status == MyBookingStatus.UPCOMING }
        BookingTab.ONGOING   -> myBookings.filter { it.status == MyBookingStatus.ONGOING }
        BookingTab.COMPLETED -> myBookings.filter { it.status == MyBookingStatus.COMPLETED }
        BookingTab.CANCELLED -> myBookings.filter { it.status == MyBookingStatus.CANCELLED }
    }

    val upcomingCount  = myBookings.count { it.status == MyBookingStatus.UPCOMING }
    val ongoingCount   = myBookings.count { it.status == MyBookingStatus.ONGOING }
    val completedCount = myBookings.count { it.status == MyBookingStatus.COMPLETED }
    val cancelledCount = myBookings.count { it.status == MyBookingStatus.CANCELLED }

    var started by remember { mutableStateOf(false) }
    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(400), label = "ha")
    val headerOffset by animateFloatAsState(if (started) 0f else -20f, tween(500, easing = EaseOutCubic), label = "hY")
    val listAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 200), label = "la")
    val listOffset   by animateFloatAsState(if (started) 0f else 28f, tween(600, delayMillis = 200, easing = EaseOutCubic), label = "lY")
    LaunchedEffect(Unit) { started = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Pine)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Brush.verticalGradient(listOf(Gold.copy(alpha = 0.06f), Color.Transparent)))
        )

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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Mist, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("MY BOOKINGS", style = EyebrowStyle.copy(fontSize = 10.sp))
                    Spacer(Modifier.height(2.dp))
                    Text("Trip History", style = PahadiRaahTypography.titleLarge.copy(color = Snow))
                }
                if (ongoingCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(Gold.copy(alpha = 0.15f))
                            .border(1.dp, Gold.copy(alpha = 0.3f), PillShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text  = "$ongoingCount active",
                            style = PahadiRaahTypography.labelSmall.copy(color = Amber, fontSize = 11.sp, letterSpacing = 0.sp)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
                    .alpha(headerAlpha)
                    .graphicsLayer { translationY = headerOffset }
            ) {
                BookingStatChip("📍", "$upcomingCount",  "Upcoming",  Sage,  Modifier.weight(1f))
                BookingStatChip("🔴", "$ongoingCount",   "Active",    Amber, Modifier.weight(1f))
                BookingStatChip("✅", "$completedCount", "Done",      Mist,  Modifier.weight(1f))
                BookingStatChip("✗",  "$cancelledCount", "Cancelled", Sage.copy(alpha = 0.4f), Modifier.weight(1f))
            }

            LazyRow(
                contentPadding        = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier
                    .padding(bottom = 16.dp)
                    .alpha(listAlpha)
            ) {
                val tabs = listOf(
                    BookingTab.ALL       to "All (${myBookings.size})",
                    BookingTab.UPCOMING  to "Upcoming ($upcomingCount)",
                    BookingTab.ONGOING   to "Active ($ongoingCount)",
                    BookingTab.COMPLETED to "Completed ($completedCount)",
                    BookingTab.CANCELLED to "Cancelled ($cancelledCount)",
                )
                items(tabs) { (tab, label) ->
                    val accent = when (tab) {
                        BookingTab.UPCOMING  -> Sage
                        BookingTab.ONGOING   -> Amber
                        BookingTab.COMPLETED -> Mist
                        BookingTab.CANCELLED -> Sage.copy(alpha = 0.4f)
                        BookingTab.ALL       -> Sage
                    }
                    BookingTabChip(label = label, isSelected = activeTab == tab, accent = accent) {
                        activeTab  = tab
                        expandedId = null
                    }
                }
            }

            if (filtered.isEmpty()) {
                BookingEmptyState(
                    tab      = activeTab,
                    modifier = Modifier.weight(1f).alpha(listAlpha)
                )
            } else {
                LazyColumn(
                    modifier       = Modifier
                        .weight(1f)
                        .alpha(listAlpha)
                        .graphicsLayer { translationY = listOffset },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(filtered, key = { _, b -> b.id }) { index, booking ->
                        AnimatedVisibility(
                            visible = true,
                            enter   = fadeIn(tween(250, index * 55)) +
                                    slideInVertically(tween(300, index * 55)) { it / 3 }
                        ) {
                            MyBookingCard(
                                booking    = booking,
                                isExpanded = expandedId == booking.id,
                                onToggle   = { expandedId = if (expandedId == booking.id) null else booking.id },
                                onTrack    = { onTrackTrip(booking.id) },
                                onReview   = {
                                    onRateTrip(booking.id, booking.routeId, booking.driverId, booking.driverName, booking.driverEmoji)
                                }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(28.dp)) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BOOKING CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MyBookingCard(
    booking:    MyBooking,
    isExpanded: Boolean,
    onToggle:   () -> Unit,
    onTrack:    () -> Unit,
    onReview:   () -> Unit
) {
    val borderColor = when (booking.status) {
        MyBookingStatus.UPCOMING  -> Sage.copy(alpha = 0.28f)
        MyBookingStatus.ONGOING   -> Amber.copy(alpha = 0.45f)
        MyBookingStatus.COMPLETED -> BorderSubtle
        MyBookingStatus.CANCELLED -> BorderSubtle
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mbPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.3f,
        targetValue   = if (booking.status == MyBookingStatus.ONGOING) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(if (booking.status == MyBookingStatus.ONGOING) 1200 else Int.MAX_VALUE, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pa"
    )
    val activeBorder = if (booking.status == MyBookingStatus.ONGOING)
        Amber.copy(alpha = pulseAlpha) else borderColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, activeBorder, PahadiRaahShapes.large)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onToggle
                )
                .padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(PahadiRaahShapes.medium)
                    .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.65f))))
            ) {
                Text(booking.routeEmoji, fontSize = 22.sp)
            }

            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text     = booking.origin,
                        style    = PahadiRaahTypography.titleSmall.copy(color = Snow, fontSize = 13.sp),
                        maxLines = 1
                    )
                    Text("→", style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.5f)))
                    Text(
                        text     = booking.destination,
                        style    = PahadiRaahTypography.titleSmall.copy(color = Snow, fontSize = 13.sp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text  = "📅 ${booking.date}  •  🕐 ${booking.time}",
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 10.sp)
                )
                Spacer(Modifier.height(5.dp))
                MyBookingStatusBadge(status = booking.status)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(booking.fare, style = FareStyle.copy(fontSize = 16.sp))
                Spacer(Modifier.height(4.dp))
                Text(
                    "${booking.seats} seat${if (booking.seats > 1) "s" else ""}",
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 10.sp)
                )
                Spacer(Modifier.height(8.dp))
                Icon(
                    imageVector        = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint               = Sage.copy(alpha = 0.5f),
                    modifier           = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter   = fadeIn(tween(200)) + expandVertically(tween(300, easing = EaseOutCubic)),
            exit    = fadeOut(tween(150)) + shrinkVertically(tween(250))
        ) {
            Column {
                Box(Modifier.fillMaxWidth().height(1.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, BorderSubtle, Color.Transparent))
                ))

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.65f))))
                    ) {
                        if (!booking.driverPhoto.isNullOrBlank()) {
                            AsyncImage(
                                model              = booking.driverPhoto,
                                contentDescription = booking.driverName,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.size(40.dp).clip(CircleShape)
                            )
                        } else {
                            Text(booking.driverEmoji, fontSize = 18.sp)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(booking.driverName, style = PahadiRaahTypography.labelMedium.copy(color = Snow, letterSpacing = 0.sp))
                        Text(
                            "⭐ ${booking.driverRating}  •  ${booking.vehicle}",
                            style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 10.sp)
                        )
                    }
                    Text(
                        booking.bookingRef,
                        style = PahadiRaahTypography.labelSmall.copy(color = Sage.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 1.sp)
                    )
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, BorderSubtle, Color.Transparent))
                ))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when (booking.status) {
                        MyBookingStatus.UPCOMING -> {
                            BookingActionBtn("Cancel", style = BookingBtnStyle.GHOST, modifier = Modifier.weight(1f), onClick = {})
                            BookingActionBtn("🗺️ Track", style = BookingBtnStyle.PRIMARY, modifier = Modifier.weight(1.5f), onClick = onTrack)
                        }
                        MyBookingStatus.ONGOING -> {
                            BookingActionBtn("🗺️ Track Live", style = BookingBtnStyle.PRIMARY, modifier = Modifier.weight(1f), onClick = onTrack)
                        }
                        MyBookingStatus.COMPLETED -> {
                            if (!booking.hasReview) {
                                BookingActionBtn("⭐ Rate Trip", style = BookingBtnStyle.GOLD, modifier = Modifier.weight(1f), onClick = onReview)
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(PahadiRaahShapes.small)
                                        .background(Moss.copy(alpha = 0.1f))
                                        .border(1.dp, Sage.copy(alpha = 0.2f), PahadiRaahShapes.small)
                                        .padding(vertical = 10.dp)
                                ) {
                                    Text("✓ Review submitted", style = PahadiRaahTypography.labelSmall.copy(color = Sage, fontSize = 11.sp, letterSpacing = 0.sp))
                                }
                            }
                            BookingActionBtn("Book Again", style = BookingBtnStyle.GHOST, modifier = Modifier.weight(1f), onClick = {})
                        }
                        MyBookingStatus.CANCELLED -> {
                            BookingActionBtn("Book Again", style = BookingBtnStyle.GHOST, modifier = Modifier.fillMaxWidth(), onClick = {})
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  STATUS BADGE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MyBookingStatusBadge(status: MyBookingStatus) {
    val (label, bg, fg) = when (status) {
        MyBookingStatus.UPCOMING  -> Triple("📍 Upcoming",  Moss.copy(alpha = 0.15f), Sage)
        MyBookingStatus.ONGOING   -> Triple("🔴 Active",    Gold.copy(alpha = 0.15f), Amber)
        MyBookingStatus.COMPLETED -> Triple("✅ Completed", SurfaceMedium,             Mist)
        MyBookingStatus.CANCELLED -> Triple("✗ Cancelled",  SurfaceMedium,             Sage.copy(alpha = 0.35f))
    }
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, style = BadgeStyle.copy(color = fg, fontSize = 9.sp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ACTION BUTTON
// ─────────────────────────────────────────────────────────────────────────────

enum class BookingBtnStyle { PRIMARY, GHOST, GOLD }

@Composable
fun BookingActionBtn(
    label:    String,
    style:    BookingBtnStyle,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "bab")

    val bg = when (style) {
        BookingBtnStyle.PRIMARY -> Brush.horizontalGradient(GradientMoss)
        BookingBtnStyle.GOLD    -> Brush.horizontalGradient(GradientGold)
        BookingBtnStyle.GHOST   -> Brush.horizontalGradient(listOf(SurfaceMedium, SurfaceMedium))
    }
    val border = when (style) {
        BookingBtnStyle.PRIMARY -> Sage.copy(alpha = 0.3f)
        BookingBtnStyle.GOLD    -> Gold.copy(alpha = 0.4f)
        BookingBtnStyle.GHOST   -> BorderSubtle
    }
    val textColor = when (style) {
        BookingBtnStyle.PRIMARY -> Snow
        BookingBtnStyle.GOLD    -> Pine
        BookingBtnStyle.GHOST   -> Mist.copy(alpha = 0.7f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(40.dp)
            .scale(scale)
            .clip(PahadiRaahShapes.small)
            .background(bg)
            .border(1.dp, border, PahadiRaahShapes.small)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Text(label, style = PahadiRaahTypography.labelMedium.copy(color = textColor, letterSpacing = 0.sp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BookingStatChip(emoji: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(PahadiRaahShapes.small)
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.18f), PahadiRaahShapes.small)
            .padding(vertical = 10.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, style = PahadiRaahTypography.titleSmall.copy(color = color, fontSize = 15.sp))
        Text(label, style = PahadiRaahTypography.bodySmall.copy(color = color.copy(alpha = 0.6f), fontSize = 9.sp))
    }
}

@Composable
fun BookingTabChip(label: String, isSelected: Boolean, accent: Color, onClick: () -> Unit) {
    val bg     by animateColorAsState(if (isSelected) accent.copy(alpha = 0.15f) else SurfaceLight.copy(alpha = 0.5f), tween(200), label = "btcBg")
    val border by animateColorAsState(if (isSelected) accent.copy(alpha = 0.4f) else BorderSubtle, tween(200), label = "btcB")
    val text   by animateColorAsState(if (isSelected) accent else Sage.copy(alpha = 0.55f), tween(200), label = "btcT")
    Box(
        modifier = Modifier
            .clip(PillShape).background(bg).border(1.dp, border, PillShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, style = PahadiRaahTypography.labelMedium.copy(color = text, letterSpacing = 0.sp))
    }
}

@Composable
fun BookingEmptyState(tab: BookingTab, modifier: Modifier = Modifier) {
    val (emoji, title, sub) = when (tab) {
        BookingTab.UPCOMING  -> Triple("📍", "No Upcoming Trips", "Book a route to get started.")
        BookingTab.ONGOING   -> Triple("🗺️", "No Active Trip", "Your live trip will appear here.")
        BookingTab.COMPLETED -> Triple("✅", "No Completed Trips", "Completed journeys show here.")
        BookingTab.CANCELLED -> Triple("✗",  "No Cancelled Trips", "Cancelled bookings show here.")
        BookingTab.ALL       -> Triple("🎫", "No Bookings Yet", "Your mountain journeys will appear here.")
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier            = modifier.fillMaxWidth().padding(40.dp)
    ) {
        Text(emoji, fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = PahadiRaahTypography.titleMedium.copy(color = Snow), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(sub, style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.55f), textAlign = TextAlign.Center, lineHeight = 18.sp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyBookingsPreview() {
    PahadiRaahTheme { MyBookingsScreen(onBack = {}, onTrackTrip = {}, onRateTrip = { _, _, _, _, _ -> }) }
}