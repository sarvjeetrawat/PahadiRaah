package com.kunpitech.pahadiraah.ui.screens.passenger

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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
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
import com.kunpitech.pahadiraah.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  LOCAL DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class PopularRoute(
    val emoji: String,
    val origin: String,
    val destination: String,
    val duration: String,
    val startingFare: String
)

data class FeaturedDriver(
    val name: String,
    val emoji: String,
    val rating: Float,
    val trips: Int,
    val vehicle: String,
    val nextRoute: String,
    val fare: String,
    val seatsLeft: Int
)

data class NearbyTrip(
    val id: String,
    val emoji: String,
    val origin: String,
    val destination: String,
    val date: String,
    val time: String,
    val driverName: String,
    val driverEmoji: String,
    val fare: String,
    val seatsLeft: Int,
    val rating: Float
)

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PassengerDashboardScreen(
    onSearchRoutes: () -> Unit,
    onBrowseDrivers: () -> Unit,
    onMyBookings: () -> Unit,
    onBack: () -> Unit
) {
    // ── Sample data ───────────────────────────────────────────────────────────
    val popularRoutes = remember {
        listOf(
            PopularRoute("🏔️", "Shimla",      "Manali",       "6–7 hrs",  "₹750"),
            PopularRoute("🌄", "Dehradun",    "Mussoorie",    "1.5 hrs",  "₹280"),
            PopularRoute("⛰️", "Nainital",    "Bhimtal",      "45 min",   "₹180"),
            PopularRoute("🗻", "Dharamshala", "Spiti Valley", "8–9 hrs",  "₹1,100"),
            PopularRoute("🌲", "Rishikesh",   "Chopta",       "4 hrs",    "₹500"),
        )
    }

    val featuredDrivers = remember {
        listOf(
            FeaturedDriver("Ramesh Kumar", "🧔", 4.9f, 134, "SUV / Jeep",  "Shimla → Manali",    "₹850", 2),
            FeaturedDriver("Sita Devi",    "👩", 4.7f, 89,  "Tempo",       "Dehradun → Musso.",  "₹300", 4),
            FeaturedDriver("Dev Singh",    "👨", 4.8f, 212, "SUV / Jeep",  "Nainital → Bhimtal", "₹420", 1),
        )
    }

    val nearbyTrips = remember {
        listOf(
            NearbyTrip("1","🏔️","Shimla","Manali",      "Jun 22","6:00 AM","Ramesh Kumar","🧔","₹850",  2,4.9f),
            NearbyTrip("2","🌄","Dehradun","Mussoorie",  "Jun 22","8:30 AM","Sita Devi",   "👩","₹300",  4,4.7f),
            NearbyTrip("3","🌲","Rishikesh","Chopta",    "Jun 23","7:00 AM","Dev Singh",   "👨","₹550",  3,4.8f),
            NearbyTrip("4","🗻","Dharamshala","Spiti",   "Jun 25","5:00 AM","Arjun Mehta", "👨","₹1,200",1,4.6f),
        )
    }

    // ── Entrance animations ───────────────────────────────────────────────────
    var started by remember { mutableStateOf(false) }
    val headerAlpha   by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "ha")
    val headerOffset  by animateFloatAsState(if (started) 0f else -28f, tween(600, easing = EaseOutCubic), label = "hY")
    val heroAlpha     by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 150), label = "heA")
    val heroOffset    by animateFloatAsState(if (started) 0f else 24f, tween(700, delayMillis = 150, easing = EaseOutCubic), label = "heY")
    val contentAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 300), label = "ca")
    val contentOffset by animateFloatAsState(if (started) 0f else 36f, tween(700, delayMillis = 300, easing = EaseOutCubic), label = "cY")
    LaunchedEffect(Unit) { started = true }

    // ═════════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Pine)
    ) {
        // Ambient glow — warm gold tint for passenger (vs green for driver)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Gold.copy(alpha = 0.06f), Moss.copy(alpha = 0.04f), Color.Transparent)
                    )
                )
        )

        LazyColumn(
            modifier       = Modifier.fillMaxSize().systemBarsPadding(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // ── HEADER ────────────────────────────────────────────────────────
            item {
                PassengerHeader(
                    onNotifClick = onMyBookings,
                    modifier     = Modifier
                        .alpha(headerAlpha)
                        .graphicsLayer { translationY = headerOffset }
                )
            }

            // ── SEARCH HERO ───────────────────────────────────────────────────
            item {
                SearchHero(
                    onSearchClick = onSearchRoutes,
                    modifier      = Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(heroAlpha)
                        .graphicsLayer { translationY = heroOffset }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── QUICK ACTIONS ─────────────────────────────────────────────────
            item {
                PassengerQuickActions(
                    onSearch       = onSearchRoutes,
                    onBrowse       = onBrowseDrivers,
                    onMyBookings   = onMyBookings,
                    modifier       = Modifier
                        .alpha(contentAlpha)
                        .graphicsLayer { translationY = contentOffset }
                )
                Spacer(modifier = Modifier.height(28.dp))
            }

            // ── POPULAR ROUTES ────────────────────────────────────────────────
            item {
                PassengerSectionHeader(
                    title    = "Popular Routes",
                    action   = "Browse All →",
                    onAction = onSearchRoutes,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(contentAlpha)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.alpha(contentAlpha)
                ) {
                    items(popularRoutes) { route ->
                        PopularRouteCard(
                            route   = route,
                            onClick = onSearchRoutes
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // ── TOP DRIVERS ───────────────────────────────────────────────────
            item {
                PassengerSectionHeader(
                    title    = "Top Drivers",
                    action   = "See All →",
                    onAction = onBrowseDrivers,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(contentAlpha)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.alpha(contentAlpha)
                ) {
                    items(featuredDrivers) { driver ->
                        FeaturedDriverCard(
                            driver  = driver,
                            onClick = onBrowseDrivers
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // ── NEARBY TRIPS ──────────────────────────────────────────────────
            item {
                PassengerSectionHeader(
                    title    = "Available Now",
                    action   = "View All →",
                    onAction = onSearchRoutes,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(contentAlpha)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(nearbyTrips) { trip ->
                NearbyTripCard(
                    trip     = trip,
                    onClick  = onSearchRoutes,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(contentAlpha)
                        .graphicsLayer { translationY = contentOffset }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PassengerHeader(
    onNotifClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text  = "Good morning,",
                style = PahadiRaahTypography.bodySmall.copy(
                    color         = Sage.copy(alpha = 0.7f),
                    letterSpacing = 0.3.sp
                )
            )
            Text(
                text  = "Priya Sharma 🎒",
                style = PahadiRaahTypography.titleLarge.copy(color = Snow)
            )
        }

        // Notification bell
        Box {
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
                        onClick           = onNotifClick
                    )
            ) {
                Icon(
                    imageVector        = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint               = Mist,
                    modifier           = Modifier.size(22.dp)
                )
            }
            // Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Gold)
            ) {
                Text(
                    text  = "2",
                    style = PahadiRaahTypography.labelSmall.copy(
                        color         = Pine,
                        fontSize      = 9.sp,
                        letterSpacing = 0.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEARCH HERO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchHero(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.16f,
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "gA"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(
                Brush.linearGradient(listOf(Forest, Moss.copy(alpha = 0.7f)))
            )
            .padding(20.dp)
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .background(Snow.copy(alpha = glowAlpha), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp)
                .background(Gold.copy(alpha = 0.08f), CircleShape)
        )
        // Mountain decoration
        Text(
            text     = "🏔️",
            fontSize = 60.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .alpha(0.18f)
                .offset(x = (-8).dp)
        )

        Column {
            Text(
                text  = "WHERE ARE YOU HEADED?",
                style = EyebrowStyle.copy(fontSize = 10.sp, color = Amber)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text  = "Find Your\nMountain Ride",
                style = PahadiRaahTypography.headlineSmall.copy(color = Snow)
            )
            Spacer(modifier = Modifier.height(18.dp))

            // Search bar tap target
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                if (isPressed) 0.97f else 1f,
                spring(stiffness = Spring.StiffnessMedium),
                label = "sb"
            )

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .clip(PillShape)
                    .background(Snow.copy(alpha = 0.12f))
                    .border(1.dp, Snow.copy(alpha = 0.2f), PillShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication        = null,
                        onClick           = onSearchClick
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Search,
                    contentDescription = "Search",
                    tint               = Snow.copy(alpha = 0.6f),
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text  = "Search routes, destinations...",
                    style = PahadiRaahTypography.bodyMedium.copy(
                        color    = Snow.copy(alpha = 0.45f),
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  QUICK ACTIONS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PassengerQuickActions(
    onSearch: () -> Unit,
    onBrowse: () -> Unit,
    onMyBookings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PassengerActionCard("🔍", "Search\nRoutes",  Sage,  Modifier.weight(1f), onSearch)
        PassengerActionCard("👤", "Browse\nDrivers", Amber, Modifier.weight(1f), onBrowse)
        PassengerActionCard("🎫", "My\nBookings",    Mist,  Modifier.weight(1f), onMyBookings)
    }
}

@Composable
fun PassengerActionCard(
    emoji: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.93f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "pac"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clip(PahadiRaahShapes.medium)
            .background(accentColor.copy(alpha = 0.08f))
            .border(
                1.dp,
                if (isPressed) accentColor.copy(alpha = 0.4f) else accentColor.copy(alpha = 0.15f),
                PahadiRaahShapes.medium
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(vertical = 18.dp, horizontal = 8.dp)
    ) {
        Text(text = emoji, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text      = label,
            style     = PahadiRaahTypography.labelMedium.copy(
                color         = accentColor,
                letterSpacing = 0.sp,
                fontSize      = 11.sp
            ),
            textAlign = TextAlign.Center,
            maxLines  = 2
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  POPULAR ROUTE CARD  — horizontal scroll card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PopularRouteCard(
    route: PopularRoute,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.95f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "prc"
    )

    Column(
        modifier = Modifier
            .width(148.dp)
            .scale(scale)
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(
                1.dp,
                if (isPressed) BorderFocus else BorderSubtle,
                PahadiRaahShapes.large
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(16.dp)
    ) {
        // Emoji circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.6f)))
                )
        ) {
            Text(text = route.emoji, fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Origin
        Text(
            text     = route.origin,
            style    = PahadiRaahTypography.labelMedium.copy(
                color    = Sage,
                fontSize = 11.sp,
                letterSpacing = 0.sp
            ),
            maxLines = 1
        )

        // Arrow + dest
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text  = "→",
                style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.5f))
            )
            Text(
                text     = route.destination,
                style    = PahadiRaahTypography.titleSmall.copy(color = Snow, fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Duration
        Text(
            text  = "⏱ ${route.duration}",
            style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 10.sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Fare
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text  = "from ",
                style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.5f), fontSize = 10.sp)
            )
            Text(
                text  = route.startingFare,
                style = FareStyle.copy(fontSize = 15.sp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FEATURED DRIVER CARD  — horizontal scroll card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FeaturedDriverCard(
    driver: FeaturedDriver,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.95f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "fdc"
    )

    Column(
        modifier = Modifier
            .width(172.dp)
            .scale(scale)
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(
                1.dp,
                if (isPressed) BorderFocus else BorderSubtle,
                PahadiRaahShapes.large
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(16.dp)
    ) {
        // Top row: avatar + rating
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(PahadiRaahShapes.medium)
                    .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.7f))))
            ) {
                Text(text = driver.emoji, fontSize = 22.sp)
            }

            // Rating badge
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(Gold.copy(alpha = 0.12f))
                    .border(1.dp, Gold.copy(alpha = 0.25f), PillShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text  = "⭐ ${driver.rating}",
                    style = PahadiRaahTypography.labelSmall.copy(
                        color         = Amber,
                        fontSize      = 11.sp,
                        letterSpacing = 0.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text     = driver.name,
            style    = PahadiRaahTypography.titleSmall.copy(color = Snow),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text  = "${driver.trips} trips • ${driver.vehicle}",
            style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Route
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PahadiRaahShapes.small)
                .background(SurfaceMedium)
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(
                text     = driver.nextRoute,
                style    = PahadiRaahTypography.bodySmall.copy(color = Mist, fontSize = 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fare + seats
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = driver.fare,
                style = FareStyle.copy(fontSize = 16.sp)
            )
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(Moss.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text  = "${driver.seatsLeft} left",
                    style = BadgeStyle.copy(color = Sage, fontSize = 10.sp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  NEARBY TRIP CARD  — vertical list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NearbyTripCard(
    trip: NearbyTrip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "ntc"
    )

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(PahadiRaahShapes.medium)
            .background(SurfaceLight)
            .border(
                1.dp,
                if (isPressed) BorderFocus else BorderSubtle,
                PahadiRaahShapes.medium
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(14.dp)
    ) {
        // Route icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .clip(PahadiRaahShapes.medium)
                .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.65f))))
        ) {
            Text(text = trip.emoji, fontSize = 22.sp)
        }

        // Middle info
        Column(modifier = Modifier.weight(1f)) {
            // Route
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

            // Date + time
            Text(
                text  = "${trip.date} • ${trip.time}",
                style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Driver + rating
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = trip.driverEmoji, fontSize = 12.sp)
                Text(
                    text  = trip.driverName,
                    style = PahadiRaahTypography.bodySmall.copy(color = Mist, fontSize = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = "•",
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.3f))
                )
                Text(
                    text  = "⭐ ${trip.rating}",
                    style = PahadiRaahTypography.bodySmall.copy(color = Amber, fontSize = 11.sp)
                )
            }
        }

        // Right: fare + seats
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = trip.fare,
                style = FareStyle.copy(fontSize = 17.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(
                        if (trip.seatsLeft == 1) Gold.copy(alpha = 0.15f)
                        else Moss.copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text  = "${trip.seatsLeft} left",
                    style = BadgeStyle.copy(
                        color    = if (trip.seatsLeft == 1) Amber else Sage,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SECTION HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PassengerSectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = title,
            style = PahadiRaahTypography.titleMedium.copy(color = Snow)
        )
        Text(
            text  = action,
            style = PahadiRaahTypography.labelMedium.copy(color = Sage),
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onAction
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PREVIEW
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PassengerDashboardPreview() {
    PahadiRaahTheme {
        PassengerDashboardScreen(
            onSearchRoutes  = {},
            onBrowseDrivers = {},
            onMyBookings    = {},
            onBack          = {}
        )
    }
}