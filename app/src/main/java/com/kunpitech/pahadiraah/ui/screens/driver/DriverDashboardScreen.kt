package com.kunpitech.pahadiraah.ui.screens.driver

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
import androidx.compose.material.icons.filled.Notifications
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
//  DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class TripItem(
    val id: String,
    val emoji: String,
    val route: String,
    val meta: String,
    val fare: String,
    val status: TripStatus
)

data class StatItem(
    val value: String,
    val label: String,
    val emoji: String
)

enum class TripStatus { ACTIVE, PENDING, COMPLETED }

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverDashboardScreen(
    onPostRoute: () -> Unit,
    onActiveRoutes: () -> Unit,
    onBookingRequests: () -> Unit,
    onBack: () -> Unit
) {
    val trips = remember {
        listOf(
            TripItem("1", "🏔️", "Shimla → Manali",     "Tomorrow • 6:00 AM • 3 seats left",  "₹850",   TripStatus.ACTIVE),
            TripItem("2", "🌄", "Dehradun → Mussoorie", "Yesterday • 8:30 AM • Completed",    "₹320",   TripStatus.COMPLETED),
            TripItem("3", "⛰️", "Nainital → Bhimtal",   "2 days ago • 2 passengers",          "₹420",   TripStatus.COMPLETED),
            TripItem("4", "🗻", "Dharamshala → Spiti",  "Jun 15 • 5:00 AM • 1 seat left",     "₹1,200", TripStatus.ACTIVE),
        )
    }

    val stats = remember {
        listOf(
            StatItem("134",  "Total Trips",  "🛣️"),
            StatItem("4.9",  "Rating",       "⭐"),
            StatItem("₹24K", "This Month",   "💰"),
            StatItem("98%",  "Acceptance",   "✅"),
        )
    }

    val pendingCount = 2

    // ── Entrance animations ───────────────────────────────────────────────────
    var started by remember { mutableStateOf(false) }
    val headerAlpha   by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "ha")
    val headerOffset  by animateFloatAsState(if (started) 0f else -30f, tween(600, easing = EaseOutCubic), label = "hY")
    val contentAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 250), label = "ca")
    val contentOffset by animateFloatAsState(if (started) 0f else 40f, tween(700, delayMillis = 250, easing = EaseOutCubic), label = "cY")

    LaunchedEffect(Unit) { started = true }

    // ═════════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Pine)
    ) {

        LazyColumn(
            modifier       = Modifier.fillMaxSize().systemBarsPadding(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {

            // Header
            item {
                DashHeader(
                    pendingCount = pendingCount,
                    onNotifClick = onBookingRequests,
                    modifier     = Modifier
                        .alpha(headerAlpha)
                        .graphicsLayer { translationY = headerOffset }
                )
            }

            // Hero banner
            item {
                DriverHeroBanner(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 4.dp)
                        .alpha(headerAlpha)
                        .graphicsLayer { translationY = headerOffset }
                )
            }

            // Stats
            item {
                Spacer(modifier = Modifier.height(20.dp))
                StatsRow(
                    stats    = stats,
                    modifier = Modifier
                        .alpha(contentAlpha)
                        .graphicsLayer { translationY = contentOffset }
                )
            }

            // Quick actions
            item {
                Spacer(modifier = Modifier.height(24.dp))
                DashSectionHeading(
                    title    = "Quick Actions",
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(contentAlpha)
                )
                Spacer(modifier = Modifier.height(12.dp))
                ActionGrid(
                    onPostRoute       = onPostRoute,
                    onActiveRoutes    = onActiveRoutes,
                    onBookingRequests = onBookingRequests,
                    pendingCount      = pendingCount,
                    modifier          = Modifier
                        .alpha(contentAlpha)
                        .graphicsLayer { translationY = contentOffset }
                )
            }

            // Earnings chart
            item {
                Spacer(modifier = Modifier.height(24.dp))
                DashSectionHeading(
                    title    = "This Week",
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(contentAlpha)
                )
                Spacer(modifier = Modifier.height(12.dp))
                EarningsCard(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(contentAlpha)
                        .graphicsLayer { translationY = contentOffset }
                )
            }

            // Recent trips heading
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .alpha(contentAlpha),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    DashSectionHeading(title = "Recent Trips")
                    Text(
                        text  = "See All →",
                        style = PahadiRaahTypography.labelMedium.copy(color = Sage),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onActiveRoutes
                        )
                    )
                }
            }

            // Trip list
            items(trips) { trip ->
                Spacer(modifier = Modifier.height(10.dp))
                TripCard(
                    trip     = trip,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(contentAlpha)
                        .graphicsLayer { translationY = contentOffset }
                )
            }
        }

        // FAB
        PostRouteFab(
            onClick  = onPostRoute,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 24.dp)
                .alpha(contentAlpha)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DashHeader(
    pendingCount: Int,
    onNotifClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = "PahadiRaah",
            style = PahadiRaahTypography.titleLarge.copy(
                brush = Brush.horizontalGradient(listOf(Sage, Amber))
            )
        )

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
                        indication = null,
                        onClick = onNotifClick
                    )
            ) {
                Icon(
                    imageVector        = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint               = Mist,
                    modifier           = Modifier.size(22.dp)
                )
            }

            if (pendingCount > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Gold)
                ) {
                    Text(
                        text  = pendingCount.toString(),
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
}

// ─────────────────────────────────────────────────────────────────────────────
//  HERO BANNER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverHeroBanner(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = EaseInOutSine), RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(Brush.linearGradient(listOf(Forest, Moss.copy(alpha = 0.8f))))
            .padding(24.dp)
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 40.dp)
                .scale(pulseScale)
                .background(Snow.copy(alpha = 0.04f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 20.dp)
                .background(Snow.copy(alpha = 0.05f), CircleShape)
        )
        // Decorative mountain emoji
        Text(
            text = "🏔️",
            fontSize = 52.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .alpha(0.15f)
                .offset(x = (-4).dp)
        )

        Column {
            Text(
                text  = "Good morning, Driver",
                style = PahadiRaahTypography.bodySmall.copy(
                    color         = Snow.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "Ramesh Kumar",
                style = PahadiRaahTypography.headlineSmall.copy(color = Snow)
            )
            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroBannerChip(icon = "⭐", label = "4.9")
                HeroBannerChip(icon = "🛣️", label = "134 Trips")
                HeroBannerChip(icon = "🟢", label = "Online")
            }
        }
    }
}

@Composable
fun HeroBannerChip(icon: String, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(PillShape)
            .background(Snow.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = icon, fontSize = 11.sp)
        Text(
            text  = label,
            style = PahadiRaahTypography.labelSmall.copy(
                color         = Snow,
                fontSize      = 11.sp,
                letterSpacing = 0.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  STATS ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatsRow(stats: List<StatItem>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier              = modifier.fillMaxWidth(),
        contentPadding        = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stats) { stat ->
            StatCard(stat = stat)
        }
    }
}

@Composable
fun StatCard(stat: StatItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(90.dp)
            .clip(PahadiRaahShapes.medium)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.medium)
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Text(text = stat.emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = stat.value,
            style     = PahadiRaahTypography.headlineSmall.copy(
                color    = Snow,
                fontSize = 20.sp
            ),
            textAlign = TextAlign.Center,
            maxLines  = 1
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text      = stat.label,
            style     = PahadiRaahTypography.bodySmall.copy(
                color    = Sage,
                fontSize = 10.sp
            ),
            textAlign = TextAlign.Center,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ACTION GRID
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ActionGrid(
    onPostRoute: () -> Unit,
    onActiveRoutes: () -> Unit,
    onBookingRequests: () -> Unit,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier              = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionCard("🗺️", "Post Route", Modifier.weight(1f), onClick = onPostRoute)
        ActionCard("📍", "My Routes",  Modifier.weight(1f), onClick = onActiveRoutes)
        ActionCard(
            emoji    = "🔔",
            label    = "Requests",
            modifier = Modifier.weight(1f),
            badge    = if (pendingCount > 0) pendingCount.toString() else null,
            onClick  = onBookingRequests
        )
    }
}

@Composable
fun ActionCard(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.93f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "ac"
    )

    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
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
                .padding(vertical = 20.dp, horizontal = 8.dp)
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text      = label,
                style     = PahadiRaahTypography.labelMedium.copy(color = Mist),
                textAlign = TextAlign.Center,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )
        }

        if (badge != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .clip(CircleShape)
                    .background(Gold)
            ) {
                Text(
                    text  = badge,
                    style = PahadiRaahTypography.labelSmall.copy(
                        color         = Pine,
                        fontSize      = 10.sp,
                        letterSpacing = 0.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EARNINGS CARD — animated bar chart
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EarningsCard(modifier: Modifier = Modifier) {
    val days       = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val values     = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 1.0f, 0.3f)
    val todayIndex = 5

    var barsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { barsVisible = true }

    val barScales = values.mapIndexed { i, _ ->
        animateFloatAsState(
            targetValue   = if (barsVisible) 1f else 0f,
            animationSpec = tween(400, delayMillis = i * 70, easing = EaseOutCubic),
            label         = "bar$i"
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(20.dp)
    ) {
        // Header row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text  = "Weekly Earnings",
                    style = PahadiRaahTypography.labelMedium.copy(color = Sage)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = "₹14,280",
                    style = PahadiRaahTypography.headlineSmall.copy(color = Amber)
                )
            }
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(Moss.copy(alpha = 0.15f))
                    .border(1.dp, Moss.copy(alpha = 0.3f), PillShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text  = "↑ 18% vs last week",
                    style = PahadiRaahTypography.labelSmall.copy(
                        color         = Sage,
                        fontSize      = 10.sp,
                        letterSpacing = 0.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bar chart
        Row(
            modifier              = Modifier.fillMaxWidth().height(80.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Bottom
        ) {
            values.forEachIndexed { i, value ->
                val isToday = i == todayIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier            = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (isToday) 14.dp else 9.dp)
                            .fillMaxHeight(value * barScales[i].value)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (isToday)
                                    Brush.verticalGradient(listOf(Gold, Amber))
                                else
                                    Brush.verticalGradient(
                                        listOf(Moss.copy(alpha = 0.8f), Moss.copy(alpha = 0.3f))
                                    )
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day labels
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEachIndexed { i, day ->
                Text(
                    text      = day,
                    style     = PahadiRaahTypography.labelSmall.copy(
                        color         = if (i == todayIndex) Amber else Sage.copy(alpha = 0.5f),
                        fontSize      = 9.sp,
                        letterSpacing = 0.sp
                    ),
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TRIP CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TripCard(trip: TripItem, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "tc"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
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
                onClick           = {}
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.6f))))
        ) {
            Text(text = trip.emoji, fontSize = 22.sp)
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = trip.route,
                style    = PahadiRaahTypography.titleSmall.copy(color = Snow),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text     = trip.meta,
                style    = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(7.dp))
            TripStatusBadge(status = trip.status)
        }

        // Fare
        Text(
            text  = trip.fare,
            style = FareStyle.copy(fontSize = 17.sp)
        )
    }
}

@Composable
fun TripStatusBadge(status: TripStatus) {
    val (label, bg, fg) = when (status) {
        TripStatus.ACTIVE    -> Triple("Active",    Moss.copy(alpha = 0.2f),  Sage)
        TripStatus.PENDING   -> Triple("Pending",   Gold.copy(alpha = 0.15f), Amber)
        TripStatus.COMPLETED -> Triple("Completed", SurfaceMedium,            Mist)
    }
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text = label, style = BadgeStyle.copy(color = fg))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SECTION HEADING
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DashSectionHeading(title: String, modifier: Modifier = Modifier) {
    Text(
        text     = title,
        style    = PahadiRaahTypography.titleMedium.copy(color = Snow),
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  FAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PostRouteFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.92f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "fab"
    )

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .scale(scale)
            .clip(PillShape)
            .background(Brush.horizontalGradient(GradientMoss))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        Icon(
            imageVector        = Icons.Default.Add,
            contentDescription = "Post Route",
            tint               = Snow,
            modifier           = Modifier.size(20.dp)
        )
        Text(
            text  = "Post Route",
            style = PahadiRaahTypography.labelLarge.copy(color = Snow)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PREVIEW
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DriverDashboardPreview() {
    PahadiRaahTheme {
        DriverDashboardScreen(
            onPostRoute       = {},
            onActiveRoutes    = {},
            onBookingRequests = {},
            onBack            = {}
        )
    }
}