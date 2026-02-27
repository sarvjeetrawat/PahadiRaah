package com.kunpitech.pahadiraah.ui.screens.driver

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.pahadiraah.data.model.BookingDto
import com.kunpitech.pahadiraah.data.model.RouteDto
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.data.model.UserDto
import com.kunpitech.pahadiraah.ui.theme.*
import com.kunpitech.pahadiraah.viewmodel.BookingViewModel
import com.kunpitech.pahadiraah.viewmodel.RouteViewModel
import com.kunpitech.pahadiraah.viewmodel.UserViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun DriverDashboardScreen(
    onPostRoute:       () -> Unit,
    onActiveRoutes:    () -> Unit,
    onBookingRequests: () -> Unit,
    onProfile:         () -> Unit,
    onBack:            () -> Unit,
    userViewModel:    UserViewModel    = hiltViewModel(),
    routeViewModel:   RouteViewModel   = hiltViewModel(),
    bookingViewModel: BookingViewModel = hiltViewModel()
) {
    val profileState      by userViewModel.myProfile.collectAsStateWithLifecycle()
    val myRoutesState     by routeViewModel.myRoutes.collectAsStateWithLifecycle()
    val pendingCount      by bookingViewModel.pendingCount.collectAsStateWithLifecycle()
    val isOnline          by userViewModel.isOnline.collectAsStateWithLifecycle()
    // FIX: routeBookings is the correct StateFlow — loadDriverBookings() populates it
    val allBookingsState  by bookingViewModel.routeBookings.collectAsStateWithLifecycle()
    val freshTotalTrips   by routeViewModel.totalTrips.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        userViewModel.loadMyProfile()
        routeViewModel.loadMyRoutes()
        bookingViewModel.loadDriverBookings()
    }

    LaunchedEffect(profileState) {
        if (profileState is UiState.Success) {
            userViewModel.initOnlineStatus(
                (profileState as UiState.Success<UserDto>).data.isOnline
            )
        }
    }

    val profile  = (profileState     as? UiState.Success<UserDto>)?.data
    val routes   = (myRoutesState    as? UiState.Success<List<RouteDto>>)?.data ?: emptyList()
    val bookings = (allBookingsState as? UiState.Success<List<BookingDto>>)?.data ?: emptyList()

    val totalTrips      = freshTotalTrips ?: profile?.totalTrips ?: 0
    val avgRating       = profile?.avgRating  ?: 0.0
    val activeCount     = routes.count { it.status == "upcoming" || it.status == "ongoing" }
    val experienceLabel = remember(profile) { computeExperience(profile) }
    val earningsData    = remember(bookings) { computeWeeklyEarnings(bookings) }

    var started by remember { mutableStateOf(false) }
    val headerAlpha   by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "ha")
    val headerOffset  by animateFloatAsState(if (started) 0f else -30f, tween(600, easing = EaseOutCubic), label = "hY")
    val contentAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 250), label = "ca")
    val contentOffset by animateFloatAsState(if (started) 0f else 40f, tween(700, delayMillis = 250, easing = EaseOutCubic), label = "cY")
    LaunchedEffect(Unit) { started = true }

    val stats = listOf(
        StatItem(totalTrips.toString(),                                        "Total Trips", "🛣️"),
        StatItem(if (avgRating > 0) String.format("%.1f", avgRating) else "—", "Rating",    "⭐"),
        StatItem(activeCount.toString(),                                       "Active",      "🟢"),
        StatItem(experienceLabel,                                              "Experience",  "🏔️"),
    )

    Box(modifier = Modifier.fillMaxSize().background(Slate)) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize().systemBarsPadding(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            item {
                DriverDashHeader(
                    profile        = profile,
                    isOnline       = isOnline,
                    onToggleOnline = { userViewModel.setOnline(!isOnline) },
                    onProfile      = onProfile,
                    onRequests     = onBookingRequests,
                    modifier       = Modifier.alpha(headerAlpha).graphicsLayer { translationY = headerOffset }
                )
            }
            item {
                DriverHeroBanner(
                    profile  = profile,
                    isOnline = isOnline,
                    modifier = Modifier
                        .padding(horizontal = 20.dp).padding(top = 4.dp)
                        .alpha(headerAlpha).graphicsLayer { translationY = headerOffset }
                )
            }
            item {
                Spacer(Modifier.height(20.dp))
                StatsRow(
                    stats    = stats,
                    modifier = Modifier.alpha(contentAlpha).graphicsLayer { translationY = contentOffset }
                )
            }
            item {
                Spacer(Modifier.height(24.dp))
                DashSectionHeading(title = "Quick Actions", modifier = Modifier.padding(horizontal = 20.dp).alpha(contentAlpha))
                Spacer(Modifier.height(12.dp))
                ActionGrid(
                    onPostRoute       = onPostRoute,
                    onActiveRoutes    = onActiveRoutes,
                    onBookingRequests = onBookingRequests,
                    pendingCount      = pendingCount,
                    modifier          = Modifier.alpha(contentAlpha).graphicsLayer { translationY = contentOffset }
                )
            }
            item {
                Spacer(Modifier.height(24.dp))
                DashSectionHeading(title = "This Week", modifier = Modifier.padding(horizontal = 20.dp).alpha(contentAlpha))
                Spacer(Modifier.height(12.dp))
                EarningsCard(
                    data     = earningsData,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(contentAlpha).graphicsLayer { translationY = contentOffset }
                )
            }
            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp).alpha(contentAlpha),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    DashSectionHeading(title = "My Routes")
                    Text(
                        text     = "See All →",
                        style    = PahadiRaahTypography.labelMedium.copy(color = Sage),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onActiveRoutes
                        )
                    )
                }
            }
            when {
                myRoutesState is UiState.Loading -> item {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        CircularProgressIndicator(color = GlacierTeal, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                }
                routes.isEmpty() -> item {
                    EmptyRoutesHint(
                        onPostRoute = onPostRoute,
                        modifier    = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).alpha(contentAlpha)
                    )
                }
                else -> items(routes.take(5)) { route ->
                    Spacer(Modifier.height(10.dp))
                    RouteCard(
                        route    = route,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .alpha(contentAlpha).graphicsLayer { translationY = contentOffset }
                    )
                }
            }
        }
        PostRouteFab(
            onClick  = onPostRoute,
            modifier = Modifier
                .align(Alignment.BottomEnd).navigationBarsPadding()
                .padding(end = 20.dp, bottom = 24.dp).alpha(contentAlpha)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EARNINGS DATA MODEL + COMPUTATION
// ─────────────────────────────────────────────────────────────────────────────

data class WeeklyEarningsData(
    val dailyEarnings: List<Int>,
    val weekTotal:     Int,
    val lastWeekTotal: Int,
    val todayIndex:    Int
) {
    val barValues: List<Float> get() {
        val max = dailyEarnings.maxOrNull()?.takeIf { it > 0 } ?: 1
        return dailyEarnings.map { it.toFloat() / max }
    }
    val weekChangePercent: Int get() {
        if (lastWeekTotal == 0) return if (weekTotal > 0) 100 else 0
        return ((weekTotal - lastWeekTotal) * 100.0 / lastWeekTotal).toInt()
    }
}

private fun computeWeeklyEarnings(bookings: List<BookingDto>): WeeklyEarningsData {
    val today      = LocalDate.now()
    val monday     = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastMonday = monday.minusWeeks(1)
    val fmt        = DateTimeFormatter.ISO_LOCAL_DATE

    val accepted = bookings.filter { it.status == "accepted" }
    val thisWeek = IntArray(7)
    val lastWeek = IntArray(7)

    for (b in accepted) {
        val dateStr = b.createdAt?.take(10) ?: continue
        val date    = runCatching { LocalDate.parse(dateStr, fmt) }.getOrNull() ?: continue
        val dayIdx  = date.dayOfWeek.value - 1
        when {
            !date.isBefore(monday) && date.isBefore(monday.plusWeeks(1)) -> thisWeek[dayIdx] += b.grandTotal
            !date.isBefore(lastMonday) && date.isBefore(monday)          -> lastWeek[dayIdx] += b.grandTotal
        }
    }

    return WeeklyEarningsData(
        dailyEarnings = thisWeek.toList(),
        weekTotal     = thisWeek.sum(),
        lastWeekTotal = lastWeek.sum(),
        todayIndex    = today.dayOfWeek.value - 1
    )
}

private fun computeExperience(profile: UserDto?): String {
    if (profile == null) return "—"
    val createdAt = profile.createdAt
    if (!createdAt.isNullOrBlank()) {
        val joined = runCatching { LocalDate.parse(createdAt.take(10)) }.getOrNull()
        if (joined != null) {
            val months = java.time.temporal.ChronoUnit.MONTHS.between(joined, LocalDate.now()).toInt()
            return when {
                months < 1  -> "New"
                months < 12 -> "${months}mo"
                else        -> "${months / 12}yr"
            }
        }
    }
    return when {
        profile.yearsActive <= 0 -> "New"
        else                     -> "${profile.yearsActive}yr"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EARNINGS CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EarningsCard(data: WeeklyEarningsData, modifier: Modifier = Modifier) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var barsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(data) { barsVisible = false; barsVisible = true }

    val barScales = data.barValues.mapIndexed { i, _ ->
        animateFloatAsState(
            targetValue   = if (barsVisible) 1f else 0f,
            animationSpec = tween(420, delayMillis = i * 65, easing = EaseOutCubic),
            label         = "bar$i"
        )
    }

    val changePercent = data.weekChangePercent
    val changeUp      = changePercent >= 0
    val changeColor   = if (changeUp) Sage else StatusError

    Column(
        modifier = modifier
            .fillMaxWidth().clip(PahadiRaahShapes.large)
            .background(SurfaceLow).border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Weekly Earnings", style = PahadiRaahTypography.labelMedium.copy(color = Sage))
                Spacer(Modifier.height(4.dp))
                if (data.weekTotal > 0) {
                    Text("₹${formatAmount(data.weekTotal)}", style = PahadiRaahTypography.headlineSmall.copy(color = Amber))
                } else {
                    Text("₹0", style = PahadiRaahTypography.headlineSmall.copy(color = MistVeil.copy(alpha = 0.4f)))
                }
            }
            if (data.lastWeekTotal > 0 || data.weekTotal > 0) {
                Box(
                    modifier = Modifier
                        .clip(PillShape).background(changeColor.copy(alpha = 0.12f))
                        .border(1.dp, changeColor.copy(alpha = 0.3f), PillShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text  = "${if (changeUp) "↑" else "↓"} ${kotlin.math.abs(changePercent)}% vs last week",
                        style = PahadiRaahTypography.labelSmall.copy(color = changeColor, fontSize = 10.sp, letterSpacing = 0.sp)
                    )
                }
            } else {
                Box(modifier = Modifier.clip(PillShape).background(SurfaceGhost).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text("No data yet", style = PahadiRaahTypography.labelSmall.copy(color = MistVeil.copy(alpha = 0.4f), fontSize = 10.sp, letterSpacing = 0.sp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            data.barValues.forEachIndexed { i, value ->
                val isToday     = i == data.todayIndex
                val hasEarnings = data.dailyEarnings[i] > 0
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier            = Modifier.weight(1f).fillMaxHeight()
                ) {
                    if (isToday && hasEarnings) {
                        Text("₹${formatAmount(data.dailyEarnings[i])}", style = PahadiRaahTypography.labelSmall.copy(color = Amber, fontSize = 8.sp, letterSpacing = 0.sp), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(2.dp))
                    }
                    val barHeight = if (hasEarnings) (value * barScales[i].value).coerceAtLeast(0.05f) else 0.05f
                    Box(
                        modifier = Modifier
                            .width(if (isToday) 14.dp else 9.dp).fillMaxHeight(barHeight)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                when {
                                    isToday && hasEarnings -> Brush.verticalGradient(listOf(Marigold, TurmericGlow))
                                    hasEarnings            -> Brush.verticalGradient(listOf(PineMid.copy(alpha = 0.85f), PineMid.copy(alpha = 0.3f)))
                                    else                   -> Brush.verticalGradient(listOf(SurfaceGhost, SurfaceGhost))
                                }
                            )
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEachIndexed { i, day ->
                Text(
                    text      = day,
                    style     = PahadiRaahTypography.labelSmall.copy(color = if (i == data.todayIndex) Amber else Sage.copy(alpha = 0.5f), fontSize = 9.sp, letterSpacing = 0.sp),
                    modifier  = Modifier.weight(1f), textAlign = TextAlign.Center
                )
            }
        }

        if (data.dailyEarnings.any { it > 0 }) {
            Spacer(Modifier.height(10.dp))
            Divider(color = BorderGhost, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Today", style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp))
                Text(
                    text  = if (data.dailyEarnings[data.todayIndex] > 0) "₹${formatAmount(data.dailyEarnings[data.todayIndex])}" else "₹0",
                    style = PahadiRaahTypography.labelMedium.copy(color = if (data.dailyEarnings[data.todayIndex] > 0) SnowPeak else MistVeil.copy(alpha = 0.35f))
                )
            }
        }
    }
}

private fun formatAmount(amount: Int): String = "%,d".format(amount)

// ─────────────────────────────────────────────────────────────────────────────
//  HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverDashHeader(profile: UserDto?, isOnline: Boolean, onToggleOnline: () -> Unit, onProfile: () -> Unit, onRequests: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text("PahadiRaah", style = PahadiRaahTypography.titleLarge.copy(brush = Brush.horizontalGradient(listOf(Sage, Amber))))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val toggleColor by animateColorAsState(if (isOnline) GlacierTeal else SurfaceMid, tween(300), label = "toggle")
            Box(
                modifier = Modifier
                    .clip(PillShape).background(toggleColor.copy(alpha = if (isOnline) 0.2f else 0.1f))
                    .border(1.dp, toggleColor.copy(alpha = 0.4f), PillShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggleOnline)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text  = if (isOnline) "🟢  Online" else "⚫  Offline",
                    style = PahadiRaahTypography.labelSmall.copy(color = if (isOnline) GlacierTeal else MistVeil.copy(alpha = 0.5f), fontSize = 11.sp, letterSpacing = 0.sp)
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp).clip(CircleShape).background(SurfaceMid)
                    .border(1.5.dp, GlacierTeal.copy(alpha = 0.4f), CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onProfile)
            ) {
                Text(text = profile?.emoji ?: "🧑", fontSize = 18.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HERO BANNER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverHeroBanner(profile: UserDto?, isOnline: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(
        modifier = modifier.fillMaxWidth().clip(PahadiRaahShapes.large)
            .background(Brush.linearGradient(listOf(PineDeep, PineMid.copy(alpha = 0.7f))))
            .padding(24.dp)
    ) {
        Box(modifier = Modifier.size(150.dp).align(Alignment.CenterEnd).offset(x = 40.dp).scale(pulseScale).background(SnowPeak.copy(alpha = 0.04f), CircleShape))
        Text("🏔️", fontSize = 52.sp, modifier = Modifier.align(Alignment.CenterEnd).alpha(0.15f).offset(x = (-4).dp))
        Column {
            Text("Welcome back,", style = PahadiRaahTypography.bodySmall.copy(color = SnowPeak.copy(alpha = 0.6f), letterSpacing = 0.5.sp))
            Spacer(Modifier.height(4.dp))
            Text(profile?.name ?: "Driver", style = PahadiRaahTypography.headlineSmall.copy(color = SnowPeak), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val rating = profile?.avgRating
                if (rating != null && rating > 0) HeroBannerChip("⭐", String.format("%.1f", rating))
                val trips = profile?.totalTrips
                if (trips != null && trips > 0) HeroBannerChip("🛣️", "$trips Trips")
                HeroBannerChip(icon = if (isOnline) "🟢" else "⚫", label = if (isOnline) "Online" else "Offline")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ROUTE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RouteCard(route: RouteDto, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "rc")
    val statusColor = when (route.status) { "upcoming" -> GlacierTeal; "ongoing" -> Marigold; "completed" -> ParchmentMist; else -> StatusError }
    val statusLabel = when (route.status) { "upcoming" -> "Upcoming"; "ongoing" -> "Ongoing"; "completed" -> "Completed"; else -> "Cancelled" }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth().scale(scale).clip(PahadiRaahShapes.medium).background(SurfaceLow)
            .border(1.dp, if (isPressed) BorderFocus else BorderSubtle, PahadiRaahShapes.medium)
            .clickable(interactionSource = interactionSource, indication = null, onClick = {})
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(PineDeep, PineMid.copy(alpha = 0.6f))))) { Text("🏔️", fontSize = 22.sp) }
        Column(modifier = Modifier.weight(1f)) {
            Text("${route.origin} → ${route.destination}", style = PahadiRaahTypography.titleSmall.copy(color = SnowPeak), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text("${route.date} • ${route.time.take(5)} • ${route.seatsLeft} seats left", style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp), maxLines = 1)
            Spacer(Modifier.height(7.dp))
            Box(modifier = Modifier.clip(PillShape).background(statusColor.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                Text(text = statusLabel, style = BadgeStyle.copy(color = statusColor))
            }
        }
        Text("₹${route.farePerSeat}", style = FareStyle.copy(fontSize = 17.sp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EMPTY ROUTES HINT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmptyRoutesHint(onPostRoute: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().clip(PahadiRaahShapes.large).background(SurfaceGhost)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large).padding(32.dp)
    ) {
        Text("🗺️", fontSize = 36.sp)
        Spacer(Modifier.height(12.dp))
        Text("No routes yet", style = PahadiRaahTypography.titleSmall.copy(color = SnowPeak), textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text("Post your first route and start\nearning from the mountains", style = PahadiRaahTypography.bodySmall.copy(color = MistVeil, fontSize = 12.sp), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clip(PillShape).background(Brush.linearGradient(GradientPrimary))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onPostRoute)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("+ Post a Route", style = PahadiRaahTypography.labelMedium.copy(color = SnowPeak))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SHARED COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

data class StatItem(val value: String, val label: String, val emoji: String)

@Composable
fun HeroBannerChip(icon: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.clip(PillShape).background(SnowPeak.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(icon, fontSize = 11.sp)
        Text(label, style = PahadiRaahTypography.labelSmall.copy(color = SnowPeak, fontSize = 11.sp, letterSpacing = 0.sp))
    }
}

@Composable
fun StatsRow(stats: List<StatItem>, modifier: Modifier = Modifier) {
    LazyRow(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(stats) { stat -> StatCard(stat) }
    }
}

@Composable
fun StatCard(stat: StatItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp).clip(PahadiRaahShapes.medium).background(SurfaceLow)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.medium).padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Text(stat.emoji, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(stat.value, style = PahadiRaahTypography.headlineSmall.copy(color = SnowPeak, fontSize = 20.sp), textAlign = TextAlign.Center, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(stat.label, style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 10.sp), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ActionGrid(onPostRoute: () -> Unit, onActiveRoutes: () -> Unit, onBookingRequests: () -> Unit, pendingCount: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionCard("🗺️", "Post Route",  Modifier.weight(1f), onClick = onPostRoute)
        ActionCard("📍", "My Routes",   Modifier.weight(1f), onClick = onActiveRoutes)
        ActionCard("🔔", "Requests",    Modifier.weight(1f), badge = if (pendingCount > 0) pendingCount.toString() else null, onClick = onBookingRequests)
    }
}

@Composable
fun ActionCard(emoji: String, label: String, modifier: Modifier = Modifier, badge: String? = null, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.93f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "ac")
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().scale(scale).clip(PahadiRaahShapes.medium).background(SurfaceLow)
                .border(1.dp, if (isPressed) BorderFocus else BorderSubtle, PahadiRaahShapes.medium)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(vertical = 20.dp, horizontal = 8.dp)
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.height(10.dp))
            Text(label, style = PahadiRaahTypography.labelMedium.copy(color = MistVeil), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (badge != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp).align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).clip(CircleShape).background(Marigold)) {
                Text(badge, style = PahadiRaahTypography.labelSmall.copy(color = PineDeep, fontSize = 10.sp, letterSpacing = 0.sp))
            }
        }
    }
}

@Composable
fun DashSectionHeading(title: String, modifier: Modifier = Modifier) {
    Text(text = title, style = PahadiRaahTypography.titleMedium.copy(color = SnowPeak), modifier = modifier)
}

@Composable
fun PostRouteFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "fab")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.scale(scale).clip(PillShape).background(Brush.horizontalGradient(GradientPrimary))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Post Route", tint = SnowPeak, modifier = Modifier.size(20.dp))
        Text("Post Route", style = PahadiRaahTypography.labelLarge.copy(color = SnowPeak))
    }
}

@Composable
fun TripStatusBadge(status: TripStatus) {
    val (label, bg, fg) = when (status) {
        TripStatus.ACTIVE    -> Triple("Active",    PineMid.copy(alpha = 0.2f), Sage)
        TripStatus.PENDING   -> Triple("Pending",   Marigold.copy(alpha = 0.15f), Amber)
        TripStatus.COMPLETED -> Triple("Completed", SurfaceMid, MistVeil)
    }
    Box(modifier = Modifier.clip(PillShape).background(bg).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(label, style = BadgeStyle.copy(color = fg))
    }
}

data class TripItem(val id: String, val emoji: String, val route: String, val meta: String, val fare: String, val status: TripStatus)
enum class TripStatus { ACTIVE, PENDING, COMPLETED }