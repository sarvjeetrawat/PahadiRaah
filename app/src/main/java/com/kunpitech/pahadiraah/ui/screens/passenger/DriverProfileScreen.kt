package com.kunpitech.pahadiraah.ui.screens.passenger

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
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
import com.kunpitech.pahadiraah.data.model.ReviewDto
import com.kunpitech.pahadiraah.data.model.RouteDto
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.data.model.UserDto
import com.kunpitech.pahadiraah.viewmodel.ReviewViewModel
import com.kunpitech.pahadiraah.viewmodel.RouteViewModel
import com.kunpitech.pahadiraah.viewmodel.UserViewModel
import com.kunpitech.pahadiraah.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class DriverReview(
    val passengerName: String,
    val passengerEmoji: String,
    val rating: Int,
    val comment: String,
    val date: String,
    val route: String
)

data class UpcomingTrip(
    val id: String,
    val emoji: String,
    val origin: String,
    val destination: String,
    val date: String,
    val time: String,
    val fare: String,
    val seatsLeft: Int,
    val totalSeats: Int,
    val vehicle: String
)

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
//  MAPPERS  — DTO → UI model
// ─────────────────────────────────────────────────────────────────────────────

private fun ReviewDto.toDriverReview() = DriverReview(
    passengerName  = users?.name  ?: "Passenger",
    passengerEmoji = users?.emoji ?: "🧑",
    rating         = overallRating,
    comment        = comment.ifBlank { "Great experience on this route!" },
    date           = createdAt?.take(10) ?: "",
    route          = tags.firstOrNull() ?: "Mountain Route"
)

private fun RouteDto.toUpcomingTrip() = UpcomingTrip(
    id         = id,
    emoji      = when {
        origin.contains("Shimla",       ignoreCase = true) -> "🏔️"
        origin.contains("Dharamshala",  ignoreCase = true) -> "🌲"
        origin.contains("Rishikesh",    ignoreCase = true) -> "🌊"
        origin.contains("Nainital",     ignoreCase = true) -> "⛰️"
        origin.contains("Haridwar",     ignoreCase = true) -> "🌊"
        else -> "🏕️"
    },
    origin      = origin,
    destination = destination,
    date        = date,
    time        = time.take(5),
    fare        = "₹$farePerSeat",
    seatsLeft   = seatsLeft,
    totalSeats  = seatsTotal,
    vehicle     = vehicleId ?: "Vehicle"
)

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverProfileScreen(
    driverId:    String,
    onBack:      () -> Unit,
    onBookSeat:  (String) -> Unit,
    userVm:      UserViewModel   = hiltViewModel(),
    routeVm:     RouteViewModel  = hiltViewModel(),
    reviewVm:    ReviewViewModel = hiltViewModel()
) {
    // ── Load all data on enter ────────────────────────────────────────────────
    LaunchedEffect(driverId) {
        userVm.loadDriverProfile(driverId)
        routeVm.loadDriverUpcomingRoutes(driverId)
        reviewVm.loadDriverReviews(driverId)
    }

    val profileState  by userVm.driverProfile.collectAsStateWithLifecycle()
    val routesState   by routeVm.driverUpcoming.collectAsStateWithLifecycle()
    val reviewsState  by reviewVm.reviews.collectAsStateWithLifecycle()

    // ── Map to UI models ──────────────────────────────────────────────────────
    val driver: UserDto? = (profileState as? UiState.Success)?.data
    // Filter to strictly future-departing routes before mapping to UI models.
    // loadDriverUpcomingRoutes already guards at ViewModel level, but this is
    // a second client-side check in case of stale cached data.
    val upcomingTrips = (routesState as? UiState.Success)?.data
        ?.filter { route ->
            try {
                val depDate = java.time.LocalDate.parse(route.date)
                val parts   = route.time.split(":").map { it.toIntOrNull() ?: 0 }
                val depTime = java.time.LocalTime.of(
                    parts.getOrElse(0) { 0 },
                    parts.getOrElse(1) { 0 },
                    parts.getOrElse(2) { 0 }
                )
                java.time.LocalDateTime.of(depDate, depTime)
                    .isAfter(java.time.LocalDateTime.now())
            } catch (e: Exception) { false }
        }
        ?.map { it.toUpcomingTrip() } ?: emptyList()
    val reviews = (reviewsState as? UiState.Success)?.data
        ?.map { it.toDriverReview() } ?: emptyList()

    // Derive badges from real profile data
    val badges = buildList {
        if ((driver?.totalTrips ?: 0) >= 100) add(DriverBadge("🏆", "100+ Trips"))
        if ((driver?.avgRating ?: 0.0) >= 4.8) add(DriverBadge("⭐", "Top Rated"))
        if ((driver?.languages?.size ?: 0) >= 3) add(DriverBadge("🌐", "Multilingual"))
        if ((driver?.yearsActive ?: 0) >= 3) add(DriverBadge("🗺️", "Expert Guide"))
        if (driver?.isOnline == true) add(DriverBadge("🟢", "Available Now"))
    }

    val isLoading = profileState is UiState.Loading

    var selectedTripId by remember { mutableStateOf<String?>(null) }

    // ── Entrance animations ───────────────────────────────────────────────────
    var started by remember { mutableStateOf(false) }
    val heroAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "hA")
    val heroOffset   by animateFloatAsState(if (started) 0f else -30f, tween(600, easing = EaseOutCubic), label = "hY")
    val contentAlpha by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 200), label = "cA")
    val contentOffset by animateFloatAsState(if (started) 0f else 30f, tween(700, delayMillis = 200, easing = EaseOutCubic), label = "cY")
    LaunchedEffect(Unit) { started = true }

    val isOnline = driver?.isOnline ?: false

    // Online pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = if (isOnline) 1f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation  = tween(if (isOnline) 1200 else Int.MAX_VALUE, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pA"
    )


    // ═════════════════════════════════════════════════════════════════════════
    Box(modifier = Modifier.fillMaxSize().background(Pine)) {

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {

            // ── HERO SECTION ──────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(heroAlpha)
                        .graphicsLayer { translationY = heroOffset }
                ) {
                    // Hero background gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Forest, Moss.copy(alpha = 0.8f), Pine)
                                )
                            )
                    )

                    // Decorative mountain emoji
                    Text(
                        text     = driver?.emoji ?: "🏔️",
                        fontSize = 120.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 40.dp, end = 16.dp)
                            .alpha(0.1f)
                    )

                    // Top bar (back + share)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProfileIconBtn(icon = Icons.Default.ArrowBack, onClick = onBack)
                        ProfileIconBtn(icon = Icons.Default.Share,     onClick = {})
                    }

                    // Avatar + name block
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color       = Sage,
                                strokeWidth = 2.dp,
                                modifier    = Modifier.size(48.dp)
                            )
                        } else {
                            // Avatar with online ring
                            Box(contentAlignment = Alignment.Center) {
                                if (isOnline) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .background(Sage.copy(alpha = pulseAlpha * 0.25f))
                                    )
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.7f))))
                                        .border(3.dp, if (isOnline) Sage else BorderSubtle, CircleShape)
                                ) {
                                    Text(text = driver?.emoji ?: "🧑", fontSize = 38.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(if (isOnline) Sage else SurfaceMedium)
                                        .border(3.dp, Pine, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text      = driver?.name ?: "Loading…",
                                style     = PahadiRaahTypography.headlineSmall.copy(color = Snow),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text  = driver?.speciality ?: "Mountain Driver",
                                style = PahadiRaahTypography.bodyMedium.copy(color = Amber, fontSize = 13.sp),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment     = Alignment.CenterVertically,
                                modifier              = Modifier.padding(horizontal = 20.dp)
                            ) {
                                HeroStat("⭐", "${"%.1f".format(driver?.avgRating ?: 0.0)}", "Rating")
                                ProfileStatDivider()
                                HeroStat("🛣️", "${driver?.totalTrips ?: 0}", "Trips")
                                ProfileStatDivider()
                                HeroStat("📅", "${driver?.yearsActive ?: 0} yrs", "Active")
                                ProfileStatDivider()
                                HeroStat("🌐", "${driver?.languages?.size ?: 0}", "Languages")
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            // ── BADGES ────────────────────────────────────────────────────────
            if (badges.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier              = Modifier
                            .padding(bottom = 20.dp)
                            .alpha(contentAlpha)
                            .graphicsLayer { translationY = contentOffset }
                    ) {
                        items(badges) { badge ->
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(Gold.copy(alpha = 0.1f))
                                    .border(1.dp, Gold.copy(alpha = 0.22f), PillShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = badge.emoji, fontSize = 12.sp)
                                Text(
                                    text  = badge.label,
                                    style = BadgeStyle.copy(color = Amber.copy(alpha = 0.85f), fontSize = 10.sp)
                                )
                            }
                        }
                    }
                }
            }

            // ── ABOUT SECTION ─────────────────────────────────────────────────
            if (driver != null) {
                item {
                    ProfileSection(
                        title    = "About",
                        modifier = Modifier
                            .alpha(contentAlpha)
                            .graphicsLayer { translationY = contentOffset }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(PahadiRaahShapes.large)
                                .background(SurfaceLight)
                                .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (driver.bio?.isNotBlank() == true) {
                                AboutRow("📝", "Bio", driver.bio)
                            }
                            if (driver.languages.isNotEmpty()) {
                                AboutRow("🌐", "Languages", driver.languages.joinToString(" • "))
                            }
                            if (driver.speciality?.isNotBlank() == true) {
                                AboutRow("🏔️", "Speciality", driver.speciality)
                            }
                            AboutRow("📅", "Experience", "${driver.yearsActive} years active")
                            AboutRow(
                                "🟢", "Status",
                                if (driver.isOnline) "Available for bookings" else "Currently offline"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // ── UPCOMING TRIPS ────────────────────────────────────────────────
            item {
                ProfileSection(
                    title    = "Upcoming Trips",
                    modifier = Modifier
                        .alpha(contentAlpha)
                        .graphicsLayer { translationY = contentOffset }
                ) {
                    if (routesState is UiState.Loading) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Sage, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        }
                    } else if (upcomingTrips.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(PahadiRaahShapes.large)
                                .background(SurfaceLight)
                                .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text  = "No upcoming trips posted yet",
                                style = PahadiRaahTypography.bodyMedium.copy(color = Sage.copy(alpha = 0.5f)),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            upcomingTrips.forEach { trip ->
                                UpcomingTripCard(
                                    trip       = trip,
                                    isSelected = selectedTripId == trip.id,
                                    onSelect   = {
                                        selectedTripId = if (selectedTripId == trip.id) null else trip.id
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── RATING BREAKDOWN ──────────────────────────────────────────────
            item {
                ProfileSection(
                    title    = "Reviews",
                    modifier = Modifier
                        .alpha(contentAlpha)
                        .graphicsLayer { translationY = contentOffset }
                ) {
                    RatingBreakdown(
                        rating  = driver?.avgRating?.toFloat() ?: 0f,
                        reviews = reviews
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ── REVIEW CARDS ──────────────────────────────────────────────────
            if (reviewsState is UiState.Loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Sage, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                }
            } else if (reviews.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .clip(PahadiRaahShapes.large)
                            .background(SurfaceLight)
                            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text      = "No reviews yet — be the first to ride!",
                            style     = PahadiRaahTypography.bodyMedium.copy(color = Sage.copy(alpha = 0.5f)),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            } else {
                items(reviews) { review ->
                    ReviewCard(
                        review   = review,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .alpha(contentAlpha)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // ── BOTTOM CTA ────────────────────────────────────────────────────────
        BookingCtaDynamic(
            driverName     = driver?.name,
            selectedTripId = selectedTripId,
            onBook         = { tripId -> onBookSeat(tripId) },
            modifier       = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .alpha(contentAlpha)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HERO STAT ITEM
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HeroStat(emoji: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.padding(horizontal = 14.dp)
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text  = value,
            style = PahadiRaahTypography.titleMedium.copy(color = Snow, fontSize = 16.sp)
        )
        Text(
            text  = label,
            style = PahadiRaahTypography.bodySmall.copy(
                color    = Sage.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun ProfileStatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(BorderSubtle)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  PROFILE SECTION WRAPPER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp)
    ) {
        Text(
            text  = title,
            style = PahadiRaahTypography.titleMedium.copy(color = Snow)
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ABOUT ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AboutRow(emoji: String, label: String, value: String) {
    Row(
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(PahadiRaahShapes.small)
                .background(SurfaceMedium)
        ) {
            Text(text = emoji, fontSize = 15.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = label,
                style = FormLabelStyle.copy(fontSize = 9.sp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = value,
                style = PahadiRaahTypography.bodyMedium.copy(color = Mist, fontSize = 13.sp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  UPCOMING TRIP CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UpcomingTripCard(
    trip: UpcomingTrip,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) Sage.copy(alpha = 0.5f) else BorderSubtle,
        tween(200), label = "utcB"
    )
    val bgColor by animateColorAsState(
        if (isSelected) Moss.copy(alpha = 0.1f) else SurfaceLight,
        tween(200), label = "utcBg"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "utcS"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(PahadiRaahShapes.large)
            .background(bgColor)
            .border(1.dp, borderColor, PahadiRaahShapes.large)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onSelect
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Route icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(PahadiRaahShapes.medium)
                    .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.65f))))
            ) {
                Text(text = trip.emoji, fontSize = 20.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                // Origin → Destination
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text     = trip.origin,
                        style    = PahadiRaahTypography.titleSmall.copy(color = Snow, fontSize = 13.sp),
                        maxLines = 1
                    )
                    Text(
                        text  = "→",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.5f))
                    )
                    Text(
                        text     = trip.destination,
                        style    = PahadiRaahTypography.titleSmall.copy(color = Snow, fontSize = 13.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text  = "📅 ${trip.date}  •  🕐 ${trip.time}  •  🚙 ${trip.vehicle}",
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 10.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text  = trip.fare,
                    style = FareStyle.copy(fontSize = 17.sp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(
                            if (trip.seatsLeft == 1) Gold.copy(alpha = 0.15f)
                            else Moss.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text  = "${trip.seatsLeft}/${trip.totalSeats} seats",
                        style = BadgeStyle.copy(
                            color    = if (trip.seatsLeft == 1) Amber else Sage,
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }

        // Selected indicator
        if (isSelected) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PahadiRaahShapes.small)
                    .background(Moss.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = "✓", style = PahadiRaahTypography.labelMedium.copy(color = Sage))
                Text(
                    text  = "Selected — tap Book Seat below to confirm",
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  RATING BREAKDOWN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RatingBreakdown(rating: Float, reviews: List<DriverReview>) {
    val starCounts = (5 downTo 1).map { star ->
        star to reviews.count { it.rating == star }
    }
    val maxCount = reviews.size.coerceAtLeast(1)

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Big rating number
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(64.dp)
        ) {
            Text(
                text  = "$rating",
                style = PahadiRaahTypography.headlineLarge.copy(color = Snow, fontSize = 40.sp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                repeat(5) { i ->
                    Text(
                        text  = if (i < rating.toInt()) "★" else "☆",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = if (i < rating.toInt()) Gold else Sage.copy(alpha = 0.2f),
                            fontSize = 12.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "${reviews.size} reviews",
                style = PahadiRaahTypography.bodySmall.copy(
                    color    = Sage.copy(alpha = 0.5f),
                    fontSize = 10.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        // Bars
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            starCounts.forEach { (star, count) ->
                var barVisible by remember { mutableStateOf(false) }
                val barFraction by animateFloatAsState(
                    targetValue   = if (barVisible) count.toFloat() / maxCount else 0f,
                    animationSpec = tween(500, delayMillis = (5 - star) * 80, easing = EaseOutCubic),
                    label         = "rBar$star"
                )
                LaunchedEffect(Unit) { barVisible = true }

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text  = "$star ★",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = if (count > 0) Gold else Sage.copy(alpha = 0.3f),
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.width(28.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(PillShape)
                            .background(SurfaceMedium)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .fillMaxHeight()
                                .clip(PillShape)
                                .background(
                                    Brush.horizontalGradient(listOf(Gold, Amber))
                                )
                        )
                    }
                    Text(
                        text  = "$count",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = Sage.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.width(16.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  REVIEW CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ReviewCard(review: DriverReview, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.6f))))
            ) {
                Text(text = review.passengerEmoji, fontSize = 17.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = review.passengerName,
                    style    = PahadiRaahTypography.labelMedium.copy(
                        color         = Snow,
                        letterSpacing = 0.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text  = review.date,
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.5f), fontSize = 10.sp)
                )
            }
            // Star rating
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                repeat(5) { i ->
                    Text(
                        text  = if (i < review.rating) "★" else "☆",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = if (i < review.rating) Gold else Sage.copy(alpha = 0.2f),
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Route chip
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .clip(PillShape)
                .background(SurfaceMedium)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Sage.copy(alpha = 0.4f))
            )
            Text(
                text  = review.route,
                style = PahadiRaahTypography.bodySmall.copy(color = Mist, fontSize = 10.sp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Comment
        Text(
            text  = review.comment,
            style = PahadiRaahTypography.bodyMedium.copy(
                color      = Mist.copy(alpha = 0.85f),
                fontSize   = 13.sp,
                lineHeight = 20.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BOOKING CTA — pinned bottom bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BookingCtaDynamic(
    driverName:     String?,
    selectedTripId: String?,
    onBook:         (String) -> Unit,
    modifier:       Modifier = Modifier
) {
    val isEnabled = selectedTripId != null
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed && isEnabled) 0.96f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "ctaS"
    )
    val bgAlpha by animateFloatAsState(
        if (isEnabled) 1f else 0.5f, tween(300), label = "ctaBg"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Pine.copy(alpha = 0.96f), Pine))
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Call button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SurfaceLight)
                    .border(1.dp, BorderSubtle, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = {}
                    )
            ) {
                Icon(
                    imageVector        = Icons.Default.Phone,
                    contentDescription = "Call",
                    tint               = Mist,
                    modifier           = Modifier.size(20.dp)
                )
            }

            // Book seat button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .scale(scale)
                    .alpha(bgAlpha)
                    .clip(PillShape)
                    .background(
                        Brush.horizontalGradient(
                            if (isEnabled) GradientMoss
                            else listOf(Forest.copy(alpha = 0.5f), Moss.copy(alpha = 0.3f))
                        )
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication        = null,
                        enabled           = isEnabled,
                        onClick           = { selectedTripId?.let { onBook(it) } }
                    )
            ) {
                Text(
                    text  = if (isEnabled) "Book Seat →" else "Select a trip above",
                    style = PahadiRaahTypography.labelLarge.copy(
                        color    = if (isEnabled) Snow else Sage.copy(alpha = 0.45f),
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ICON BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Snow.copy(alpha = 0.12f))
            .border(1.dp, Snow.copy(alpha = 0.15f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = Snow,
            modifier           = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PREVIEW
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DriverProfilePreview() {
    PahadiRaahTheme {
        DriverProfileScreen(
            driverId  = "1",
            onBack    = {},
            onBookSeat = {}
        )
    }
}