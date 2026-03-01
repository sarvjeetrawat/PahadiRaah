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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kunpitech.pahadiraah.data.model.RouteDto
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.data.model.UserDto
import com.kunpitech.pahadiraah.ui.theme.*
import com.kunpitech.pahadiraah.viewmodel.RouteViewModel
import com.kunpitech.pahadiraah.viewmodel.UserViewModel

// ─────────────────────────────────────────────────────────────────────────────
//  PassengerDashboardScreen  — fully dynamic
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PassengerDashboardScreen(
    onSearchRoutes:  () -> Unit,
    onBrowseDrivers: () -> Unit,
    onMyBookings:    () -> Unit,
    onProfile:       () -> Unit,
    onBack:          () -> Unit,
    userViewModel:  UserViewModel  = hiltViewModel(),
    routeViewModel: RouteViewModel = hiltViewModel()
) {
    val profileState    by userViewModel.myProfile.collectAsStateWithLifecycle()
    val allDriversState by userViewModel.allDrivers.collectAsStateWithLifecycle()
    val searchState     by routeViewModel.searchState.collectAsStateWithLifecycle()

    // Load data on entry — empty search = all upcoming routes
    LaunchedEffect(Unit) {
        userViewModel.loadMyProfile()
        userViewModel.loadAllDrivers()
        routeViewModel.searchRoutes("", "", "", 1)
    }

    val profile  = (profileState as? UiState.Success<UserDto>)?.data
    val drivers  = (allDriversState as? UiState.Success<List<UserDto>>)?.data ?: emptyList()
    val routes   = (searchState as? UiState.Success<List<RouteDto>>)?.data ?: emptyList()

    // Entrance animation
    var started by remember { mutableStateOf(false) }
    val headerAlpha   by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "ha")
    val headerOffset  by animateFloatAsState(if (started) 0f else -28f, tween(600, easing = EaseOutCubic), label = "hY")
    val heroAlpha     by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 150), label = "heA")
    val heroOffset    by animateFloatAsState(if (started) 0f else 24f, tween(700, delayMillis = 150, easing = EaseOutCubic), label = "heY")
    val contentAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 300), label = "ca")
    val contentOffset by animateFloatAsState(if (started) 0f else 36f, tween(700, delayMillis = 300, easing = EaseOutCubic), label = "cY")
    LaunchedEffect(Unit) { started = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate)
    ) {
        // Warm gradient header tint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Saffron.copy(alpha = 0.05f), PineMid.copy(alpha = 0.04f), Color.Transparent)
                    )
                )
        )

        LazyColumn(
            modifier       = Modifier.fillMaxSize().systemBarsPadding(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // ── Header ────────────────────────────────────────────────────────
            item {
                PassengerDashHeader(
                    profile   = profile,
                    onProfile = onProfile,
                    onBookings = onMyBookings,
                    modifier  = Modifier
                        .alpha(headerAlpha)
                        .graphicsLayer { translationY = headerOffset }
                )
            }

            // ── Search hero ───────────────────────────────────────────────────
            item {
                SearchHero(
                    profile       = profile,
                    onSearchClick = onSearchRoutes,
                    modifier      = Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(heroAlpha)
                        .graphicsLayer { translationY = heroOffset }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Quick actions ─────────────────────────────────────────────────
            item {
                PassengerQuickActions(
                    onSearch     = onSearchRoutes,
                    onBrowse     = onBrowseDrivers,
                    onMyBookings = onMyBookings,
                    modifier     = Modifier
                        .alpha(contentAlpha)
                        .graphicsLayer { translationY = contentOffset }
                )
                Spacer(modifier = Modifier.height(28.dp))
            }

            // ── Top Drivers ───────────────────────────────────────────────────
            item {
                PassengerSectionHeader(
                    title    = "Top Drivers",
                    action   = "See All →",
                    onAction = onBrowseDrivers,
                    modifier = Modifier.padding(horizontal = 20.dp).alpha(contentAlpha)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                when {
                    allDriversState is UiState.Loading -> Box(
                        Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = GlacierTeal, strokeWidth = 2.dp, modifier = Modifier.size(24.dp)
                        )
                    }
                    drivers.isEmpty() -> Text(
                        text     = "No drivers found",
                        style    = PahadiRaahTypography.bodySmall.copy(color = MistVeil),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    else -> LazyRow(
                        contentPadding        = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier              = Modifier.alpha(contentAlpha)
                    ) {
                        items(drivers.take(6)) { driver ->
                            RealDriverCard(driver = driver, onClick = onBrowseDrivers)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // ── Available Routes ──────────────────────────────────────────────
            item {
                PassengerSectionHeader(
                    title    = "Available Now",
                    action   = "View All →",
                    onAction = onSearchRoutes,
                    modifier = Modifier.padding(horizontal = 20.dp).alpha(contentAlpha)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            when {
                searchState is UiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                        CircularProgressIndicator(
                            color = GlacierTeal, strokeWidth = 2.dp, modifier = Modifier.size(24.dp)
                        )
                    }
                }
                routes.isEmpty() -> item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(PahadiRaahShapes.large)
                            .background(SurfaceGhost)
                            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
                            .padding(32.dp)
                    ) {
                        Text("🏔️", fontSize = 32.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text      = "No routes available yet",
                            style     = PahadiRaahTypography.titleSmall.copy(color = SnowPeak),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text      = "Check back soon — drivers are\nposting new routes daily",
                            style     = PahadiRaahTypography.bodySmall.copy(color = MistVeil, fontSize = 12.sp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> items(routes.take(6)) { route ->
                    RealRouteCard(
                        route    = route,
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
}

// ─────────────────────────────────────────────────────────────────────────────
//  HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PassengerDashHeader(
    profile:   UserDto?,
    onProfile: () -> Unit,
    onBookings: () -> Unit,
    modifier:  Modifier = Modifier
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
                text  = "Hello,",
                style = PahadiRaahTypography.bodySmall.copy(
                    color = GlacierLight.copy(alpha = 0.7f), letterSpacing = 0.3.sp
                )
            )
            Text(
                text     = "${profile?.name ?: "Traveller"} ${profile?.emoji ?: "🎒"}",
                style    = PahadiRaahTypography.titleLarge.copy(color = SnowPeak),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Bookings icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceLow)
                    .border(1.dp, BorderSubtle, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onBookings
                    )
            ) {
                Text("🎫", fontSize = 18.sp)
            }

            // Avatar → profile
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceLow)
                    .border(1.5.dp, Saffron.copy(alpha = 0.5f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onProfile
                    )
            ) {
                if (!profile?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model              = profile!!.avatarUrl,
                        contentDescription = "Avatar",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.size(40.dp).clip(CircleShape)
                    )
                } else {
                    Text(text = profile?.emoji ?: "🎒", fontSize = 18.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEARCH HERO  — shows user name
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchHero(
    profile:       UserDto?,
    onSearchClick: () -> Unit,
    modifier:      Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.08f, targetValue = 0.16f,
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "gA"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(Brush.linearGradient(listOf(PineDeep, PineMid.copy(alpha = 0.7f))))
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .background(SnowPeak.copy(alpha = glowAlpha), CircleShape)
        )
        Text(
            text     = "🏔️",
            fontSize = 60.sp,
            modifier = Modifier.align(Alignment.CenterEnd).alpha(0.18f).offset(x = (-8).dp)
        )

        Column {
            Text(
                text  = "WHERE ARE YOU HEADED?",
                style = EyebrowStyle.copy(fontSize = 10.sp, color = Marigold)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text  = "Find Your\nMountain Ride",
                style = PahadiRaahTypography.headlineSmall.copy(color = SnowPeak)
            )
            Spacer(Modifier.height(18.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                if (isPressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "sb"
            )

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .clip(PillShape)
                    .background(SnowPeak.copy(alpha = 0.12f))
                    .border(1.dp, SnowPeak.copy(alpha = 0.2f), PillShape)
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onSearchClick)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Search,
                    contentDescription = "Search",
                    tint               = SnowPeak.copy(alpha = 0.6f),
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text  = "Search routes, destinations...",
                    style = PahadiRaahTypography.bodyMedium.copy(
                        color = SnowPeak.copy(alpha = 0.45f), fontSize = 14.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  REAL DRIVER CARD  — from UserDto
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RealDriverCard(driver: UserDto, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "rdc"
    )

    Column(
        modifier = Modifier
            .width(172.dp)
            .scale(scale)
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLow)
            .border(1.dp, if (isPressed) BorderFocus else BorderSubtle, PahadiRaahShapes.large)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
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
                    .background(Brush.verticalGradient(listOf(PineDeep, PineMid.copy(alpha = 0.7f))))
            ) {
                if (!driver.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model              = driver.avatarUrl,
                        contentDescription = driver.name,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.size(48.dp).clip(PahadiRaahShapes.medium)
                    )
                } else {
                    Text(text = driver.emoji, fontSize = 22.sp)
                }
            }
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(Marigold.copy(alpha = 0.12f))
                    .border(1.dp, Marigold.copy(alpha = 0.25f), PillShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text  = "⭐ ${String.format("%.1f", driver.avgRating)}",
                    style = PahadiRaahTypography.labelSmall.copy(
                        color = Amber, fontSize = 11.sp, letterSpacing = 0.sp
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text     = driver.name,
            style    = PahadiRaahTypography.titleSmall.copy(color = SnowPeak),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text  = "${driver.totalTrips} trips",
            style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
        )

        if (!driver.bio.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PahadiRaahShapes.small)
                    .background(SurfaceMid)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    text     = driver.bio,
                    style    = PahadiRaahTypography.bodySmall.copy(color = MistVeil, fontSize = 11.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Online status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (driver.isOnline) GlacierTeal else MistVeil.copy(alpha = 0.3f))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text  = if (driver.isOnline) "Online" else "Offline",
                style = PahadiRaahTypography.labelSmall.copy(
                    color    = if (driver.isOnline) GlacierTeal else MistVeil.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    letterSpacing = 0.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  REAL ROUTE CARD  — from RouteDto
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RealRouteCard(route: RouteDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "rrc"
    )

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(PahadiRaahShapes.medium)
            .background(SurfaceLow)
            .border(1.dp, if (isPressed) BorderFocus else BorderSubtle, PahadiRaahShapes.medium)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(14.dp)
    ) {
        // Route icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .clip(PahadiRaahShapes.medium)
                .background(Brush.verticalGradient(listOf(PineDeep, PineMid.copy(alpha = 0.65f))))
        ) {
            Text("🏔️", fontSize = 22.sp)
        }

        // Middle
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text     = route.origin,
                    style    = PahadiRaahTypography.titleSmall.copy(color = SnowPeak, fontSize = 13.sp),
                    maxLines = 1
                )
                Text("→", style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.5f)))
                Text(
                    text     = route.destination,
                    style    = PahadiRaahTypography.titleSmall.copy(color = SnowPeak, fontSize = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(3.dp))
            Text(
                text  = "${route.date} • ${route.time.take(5)}",
                style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
            )

            Spacer(Modifier.height(5.dp))

            // Driver info from join
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val driver = route.users
                if (driver != null) {
                    Text(driver.emoji, fontSize = 12.sp)
                    Text(
                        text     = driver.name,
                        style    = PahadiRaahTypography.bodySmall.copy(color = MistVeil, fontSize = 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (driver.avgRating > 0) {
                        Text("•", style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.3f)))
                        Text(
                            text  = "⭐ ${String.format("%.1f", driver.avgRating)}",
                            style = PahadiRaahTypography.bodySmall.copy(color = Amber, fontSize = 11.sp)
                        )
                    }
                }
            }
        }

        // Right side
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = "₹${route.farePerSeat}",
                style = FareStyle.copy(fontSize = 17.sp)
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(
                        if (route.seatsLeft == 1) Marigold.copy(alpha = 0.15f)
                        else PineMid.copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text  = "${route.seatsLeft} left",
                    style = BadgeStyle.copy(
                        color    = if (route.seatsLeft == 1) Amber else Sage,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  QUICK ACTIONS / SECTION HEADER (unchanged layout, updated colors)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PassengerQuickActions(
    onSearch: () -> Unit,
    onBrowse: () -> Unit,
    onMyBookings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier              = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PassengerActionCard("🔍", "Search\nRoutes",  GlacierTeal, Modifier.weight(1f), onSearch)
        PassengerActionCard("👤", "Browse\nDrivers", Marigold,    Modifier.weight(1f), onBrowse)
        PassengerActionCard("🎫", "My\nBookings",    MistVeil,    Modifier.weight(1f), onMyBookings)
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
        if (isPressed) 0.93f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "pac"
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
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 8.dp)
    ) {
        Text(text = emoji, fontSize = 28.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            text      = label,
            style     = PahadiRaahTypography.labelMedium.copy(
                color = accentColor, letterSpacing = 0.sp, fontSize = 11.sp
            ),
            textAlign = TextAlign.Center,
            maxLines  = 2
        )
    }
}

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
        Text(text = title, style = PahadiRaahTypography.titleMedium.copy(color = SnowPeak))
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

// Local data models (still used by older screens that may reference them)
data class PopularRoute(val emoji: String, val origin: String, val destination: String, val duration: String, val startingFare: String)
data class FeaturedDriver(val name: String, val emoji: String, val rating: Float, val trips: Int, val vehicle: String, val nextRoute: String, val fare: String, val seatsLeft: Int)
data class NearbyTrip(val id: String, val emoji: String, val origin: String, val destination: String, val date: String, val time: String, val driverName: String, val driverEmoji: String, val fare: String, val seatsLeft: Int, val rating: Float)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PassengerDashboardPreview() {
    PahadiRaahTheme {
        PassengerDashboardScreen(
            onSearchRoutes  = {},
            onBrowseDrivers = {},
            onMyBookings    = {},
            onProfile       = {},
            onBack          = {}
        )
    }
}