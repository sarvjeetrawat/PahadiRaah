package com.kunpitech.pahadiraah.ui.screens.passenger

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.data.model.UserDto
import com.kunpitech.pahadiraah.viewmodel.UserViewModel
import com.kunpitech.pahadiraah.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class DriverProfile(
    val id: String,
    val name: String,
    val emoji: String,
    val rating: Float,
    val totalTrips: Int,
    val vehicle: String,
    val vehicleEmoji: String,
    val speciality: String,          // e.g. "High altitude expert"
    val routes: List<String>,        // key routes they drive
    val fare: String,                // starting fare
    val seatsAvailable: Int,
    val yearsActive: Int,
    val languages: List<String>,
    val badges: List<DriverBadge>,
    val isOnline: Boolean,
    val nextTrip: String?
)

data class DriverBadge(val emoji: String, val label: String)

enum class DriverSortOption { RATING, TRIPS, FARE_LOW, FARE_HIGH, AVAILABILITY }
enum class DriverFilter      { ALL, ONLINE, TOP_RATED, EXPERIENCED }

// ─────────────────────────────────────────────────────────────────────────────
//  SAMPLE DATA
// ─────────────────────────────────────────────────────────────────────────────

val sampleDrivers = listOf(
    DriverProfile(
        id             = "1",
        name           = "Ramesh Kumar",
        emoji          = "🧔",
        rating         = 4.9f,
        totalTrips     = 134,
        vehicle        = "SUV / Jeep",
        vehicleEmoji   = "🚐",
        speciality     = "High altitude expert",
        routes         = listOf("Shimla → Manali", "Dharamshala → Spiti"),
        fare           = "₹850",
        seatsAvailable = 2,
        yearsActive    = 5,
        languages      = listOf("Hindi", "English", "Pahari"),
        badges         = listOf(
            DriverBadge("🏔️", "Mountain Pro"),
            DriverBadge("⭐", "Top Rated"),
            DriverBadge("🔒", "Verified"),
        ),
        isOnline  = true,
        nextTrip  = "Jun 22 • Shimla → Manali"
    ),
    DriverProfile(
        id             = "2",
        name           = "Sita Devi",
        emoji          = "👩",
        rating         = 4.7f,
        totalTrips     = 89,
        vehicle        = "Tempo Traveller",
        vehicleEmoji   = "🚌",
        speciality     = "Group travel specialist",
        routes         = listOf("Dehradun → Mussoorie", "Rishikesh → Chopta"),
        fare           = "₹300",
        seatsAvailable = 8,
        yearsActive    = 3,
        languages      = listOf("Hindi", "Garhwali"),
        badges         = listOf(
            DriverBadge("👥", "Group Expert"),
            DriverBadge("🔒", "Verified"),
        ),
        isOnline  = true,
        nextTrip  = "Jun 22 • Dehradun → Mussoorie"
    ),
    DriverProfile(
        id             = "3",
        name           = "Dev Singh",
        emoji          = "👨",
        rating         = 4.8f,
        totalTrips     = 212,
        vehicle        = "SUV / Jeep",
        vehicleEmoji   = "🚐",
        speciality     = "Senior mountain driver",
        routes         = listOf("Nainital → Bhimtal", "Almora → Munsiyari"),
        fare           = "₹420",
        seatsAvailable = 3,
        yearsActive    = 8,
        languages      = listOf("Hindi", "Kumaoni", "English"),
        badges         = listOf(
            DriverBadge("🥇", "Veteran"),
            DriverBadge("⭐", "Top Rated"),
            DriverBadge("🔒", "Verified"),
            DriverBadge("❤️", "200+ Trips"),
        ),
        isOnline  = false,
        nextTrip  = "Jun 23 • Nainital → Bhimtal"
    ),
    DriverProfile(
        id             = "4",
        name           = "Meena Rawat",
        emoji          = "👩",
        rating         = 4.6f,
        totalTrips     = 77,
        vehicle        = "Sedan",
        vehicleEmoji   = "🚙",
        speciality     = "Uttarakhand specialist",
        routes         = listOf("Rishikesh → Chopta", "Haridwar → Badrinath"),
        fare           = "₹550",
        seatsAvailable = 4,
        yearsActive    = 2,
        languages      = listOf("Hindi", "English"),
        badges         = listOf(
            DriverBadge("🌿", "Eco Driver"),
            DriverBadge("🔒", "Verified"),
        ),
        isOnline  = true,
        nextTrip  = "Jun 23 • Rishikesh → Chopta"
    ),
    DriverProfile(
        id             = "5",
        name           = "Arjun Thakur",
        emoji          = "👨",
        rating         = 4.4f,
        totalTrips     = 38,
        vehicle        = "Sedan",
        vehicleEmoji   = "🚙",
        speciality     = "Himachal routes",
        routes         = listOf("Shimla → Manali", "Shimla → Kalpa"),
        fare           = "₹750",
        seatsAvailable = 2,
        yearsActive    = 1,
        languages      = listOf("Hindi", "Pahari"),
        badges         = listOf(
            DriverBadge("🔒", "Verified"),
        ),
        isOnline  = false,
        nextTrip  = null
    ),
    DriverProfile(
        id             = "6",
        name           = "Priyanka Dev",
        emoji          = "👩",
        rating         = 4.9f,
        totalTrips     = 155,
        vehicle        = "SUV / Jeep",
        vehicleEmoji   = "🚐",
        speciality     = "Char Dham specialist",
        routes         = listOf("Haridwar → Badrinath", "Haridwar → Kedarnath"),
        fare           = "₹950",
        seatsAvailable = 5,
        yearsActive    = 6,
        languages      = listOf("Hindi", "English", "Sanskrit"),
        badges         = listOf(
            DriverBadge("🕉️", "Char Dham"),
            DriverBadge("⭐", "Top Rated"),
            DriverBadge("🔒", "Verified"),
        ),
        isOnline  = true,
        nextTrip  = "Jun 24 • Haridwar → Badrinath"
    ),
)

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
// Map UserDto from Supabase → existing DriverProfile UI model
private fun UserDto.toDriverProfile() = DriverProfile(
    id            = id,
    name          = name,
    emoji         = emoji,
    rating        = avgRating.toFloat(),
    totalTrips    = totalTrips,
    vehicle       = "SUV / Jeep",
    vehicleEmoji  = "🚐",
    speciality    = speciality ?: "Mountain routes",
    routes        = emptyList(),
    fare          = "₹500+",
    seatsAvailable = 4,
    yearsActive   = yearsActive,
    languages     = languages.ifEmpty { listOf("Hindi", "English") },
    badges        = buildList {
        if (avgRating >= 4.8) add(DriverBadge("⭐", "Top Rated"))
        if (totalTrips >= 100) add(DriverBadge("🏅", "Experienced"))
        if (isOnline) add(DriverBadge("🟢", "Online Now"))
    },
    isOnline      = isOnline,
    nextTrip      = null
)

@Composable
fun BrowseDriversScreen(
    onBack: () -> Unit,
    onDriverSelect: (String) -> Unit,
    userVm: UserViewModel = hiltViewModel()
) {
    val focusManager   = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val driversState   by userVm.allDrivers.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { userVm.loadAllDrivers() }

    // Map to UI model
    val allDrivers = when (val s = driversState) {
        is UiState.Success -> s.data.map { it.toDriverProfile() }
        else -> emptyList()
    }

    // ── State ─────────────────────────────────────────────────────────────────
    var searchQuery   by remember { mutableStateOf("") }
    var activeFilter  by remember { mutableStateOf(DriverFilter.ALL) }
    var sortBy        by remember { mutableStateOf(DriverSortOption.RATING) }
    var showFilters   by remember { mutableStateOf(false) }
    var minRating     by remember { mutableFloatStateOf(0f) }
    var vehicleFilter by remember { mutableStateOf<String?>(null) }
    var onlineOnly    by remember { mutableStateOf(false) }

    // ── Filtered + sorted list ────────────────────────────────────────────────
    val results = remember(allDrivers, searchQuery, activeFilter, sortBy, minRating, vehicleFilter, onlineOnly) {
        allDrivers
            .filter { d ->
                val matchSearch  = searchQuery.isBlank() ||
                        d.name.contains(searchQuery, ignoreCase = true) ||
                        d.routes.any { it.contains(searchQuery, ignoreCase = true) } ||
                        d.speciality.contains(searchQuery, ignoreCase = true)
                val matchFilter  = when (activeFilter) {
                    DriverFilter.ALL         -> true
                    DriverFilter.ONLINE      -> d.isOnline
                    DriverFilter.TOP_RATED   -> d.rating >= 4.7f
                    DriverFilter.EXPERIENCED -> d.totalTrips >= 100
                }
                val matchRating  = d.rating >= minRating
                val matchVehicle = vehicleFilter == null ||
                        d.vehicle.contains(vehicleFilter!!, ignoreCase = true)
                val matchOnline  = !onlineOnly || d.isOnline
                matchSearch && matchFilter && matchRating && matchVehicle && matchOnline
            }
            .sortedWith(
                when (sortBy) {
                    DriverSortOption.RATING       -> compareByDescending { it.rating }
                    DriverSortOption.TRIPS        -> compareByDescending { it.totalTrips }
                    DriverSortOption.FARE_LOW     -> compareBy { it.fare.replace("₹","").replace(",","").toIntOrNull() ?: 0 }
                    DriverSortOption.FARE_HIGH    -> compareByDescending { it.fare.replace("₹","").replace(",","").toIntOrNull() ?: 0 }
                    DriverSortOption.AVAILABILITY -> compareByDescending { if (it.isOnline) 1 else 0 }
                }
            )
    }

    val onlineCount      = allDrivers.count { it.isOnline }
    val topRatedCount    = allDrivers.count { it.rating >= 4.7f }
    val experiencedCount = allDrivers.count { it.totalTrips >= 100 }

    // ── Entrance animations ───────────────────────────────────────────────────
    var started by remember { mutableStateOf(false) }
    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(400), label = "ha")
    val headerOffset by animateFloatAsState(if (started) 0f else -20f, tween(500, easing = EaseOutCubic), label = "hY")
    val listAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 150), label = "la")
    val listOffset   by animateFloatAsState(if (started) 0f else 24f, tween(600, delayMillis = 150, easing = EaseOutCubic), label = "lY")
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "DRIVERS", style = EyebrowStyle.copy(fontSize = 10.sp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = "Browse Drivers",
                        style = PahadiRaahTypography.titleLarge.copy(color = Snow)
                    )
                }
                // Online indicator
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(PillShape)
                        .background(Moss.copy(alpha = 0.15f))
                        .border(1.dp, Sage.copy(alpha = 0.25f), PillShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Sage)
                    )
                    Text(
                        text  = "$onlineCount online",
                        style = PahadiRaahTypography.labelSmall.copy(
                            color         = Sage,
                            fontSize      = 11.sp,
                            letterSpacing = 0.sp
                        )
                    )
                }
            }

            // ── SEARCH BAR ────────────────────────────────────────────────────
            DriverSearchBar(
                value          = searchQuery,
                onValueChange  = { searchQuery = it },
                onClear        = { searchQuery = "" },
                onSearch       = { focusManager.clearFocus() },
                focusRequester = focusRequester,
                modifier       = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 14.dp)
                    .alpha(headerAlpha)
                    .graphicsLayer { translationY = headerOffset }
            )

            // ── FILTER TABS ───────────────────────────────────────────────────
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier
                    .padding(bottom = 10.dp)
                    .alpha(listAlpha)
            ) {
                val tabs = listOf(
                    DriverFilter.ALL         to "All (${allDrivers.size})",
                    DriverFilter.ONLINE      to "🟢 Online ($onlineCount)",
                    DriverFilter.TOP_RATED   to "⭐ Top Rated ($topRatedCount)",
                    DriverFilter.EXPERIENCED to "🥇 Veteran ($experiencedCount)",
                )
                items(tabs) { (filter, label) ->
                    DriverFilterChip(
                        label      = label,
                        isSelected = activeFilter == filter,
                        onClick    = { activeFilter = filter }
                    )
                }
            }

            // ── FILTERS TOGGLE ────────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 14.dp)
                    .alpha(listAlpha)
            ) {
                // Results count
                Text(
                    text  = "${results.size} driver${if (results.size != 1) "s" else ""} found",
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 12.sp)
                )

                // Filter toggle
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(PillShape)
                        .background(if (showFilters) Moss.copy(alpha = 0.15f) else SurfaceLight)
                        .border(
                            1.dp,
                            if (showFilters) Sage.copy(alpha = 0.35f) else BorderSubtle,
                            PillShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = { showFilters = !showFilters }
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(text = "⚙️", fontSize = 12.sp)
                    Text(
                        text  = "Filters",
                        style = PahadiRaahTypography.labelSmall.copy(
                            color         = if (showFilters) Sage else Mist,
                            letterSpacing = 0.sp,
                            fontSize      = 12.sp
                        )
                    )
                    Icon(
                        imageVector        = if (showFilters) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint               = Sage.copy(alpha = 0.5f),
                        modifier           = Modifier.size(15.dp)
                    )
                }
            }

            // ── MAIN SCROLL ───────────────────────────────────────────────────
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
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Filters panel
                item {
                    AnimatedVisibility(
                        visible = showFilters,
                        enter   = fadeIn(tween(200)) + expandVertically(tween(300, easing = EaseOutCubic)),
                        exit    = fadeOut(tween(150)) + shrinkVertically(tween(250))
                    ) {
                        DriverFiltersPanel(
                            minRating       = minRating,
                            onRatingChange  = { minRating = it },
                            vehicleFilter   = vehicleFilter,
                            onVehicleChange = { vehicleFilter = it },
                            onlineOnly      = onlineOnly,
                            onOnlineToggle  = { onlineOnly = it },
                            sortBy          = sortBy,
                            onSortChange    = { sortBy = it }
                        )
                    }
                    if (showFilters) Spacer(modifier = Modifier.height(16.dp))
                }

                // Empty state
                if (results.isEmpty()) {
                    item {
                        DriverEmptyState(
                            onReset = {
                                searchQuery   = ""
                                activeFilter  = DriverFilter.ALL
                                minRating     = 0f
                                vehicleFilter = null
                                onlineOnly    = false
                            }
                        )
                    }
                } else {
                    // Driver cards
                    itemsIndexed(results, key = { _, d -> d.id }) { index, driver ->
                        AnimatedVisibility(
                            visible = true,
                            enter   = fadeIn(tween(250, index * 60)) +
                                    slideInVertically(tween(300, index * 60)) { it / 3 }
                        ) {
                            DriverCard(
                                driver   = driver,
                                onClick  = { onDriverSelect(driver.id) }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEARCH BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (isFocused) BorderFocus else BorderSubtle, tween(200), label = "dsb"
    )
    val bgColor by animateColorAsState(
        if (isFocused) Sage.copy(alpha = 0.06f) else SurfaceLight, tween(200), label = "dsbg"
    )

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(bgColor)
            .border(1.dp, borderColor, PillShape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector        = Icons.Default.Search,
            contentDescription = "Search",
            tint               = if (isFocused) Sage else Sage.copy(alpha = 0.4f),
            modifier           = Modifier.size(18.dp)
        )
        BasicTextField(
            value           = value,
            onValueChange   = onValueChange,
            singleLine      = true,
            textStyle       = PahadiRaahTypography.bodyMedium.copy(color = Snow, fontSize = 15.sp),
            cursorBrush     = SolidColor(Sage),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier        = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            decorationBox   = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text  = "Search by name, route, speciality…",
                            style = PahadiRaahTypography.bodyMedium.copy(
                                color    = Sage.copy(alpha = 0.35f),
                                fontSize = 14.sp
                            )
                        )
                    }
                    inner()
                }
            }
        )
        if (value.isNotEmpty()) {
            Icon(
                imageVector        = Icons.Default.Close,
                contentDescription = "Clear",
                tint               = Sage.copy(alpha = 0.5f),
                modifier           = Modifier
                    .size(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onClear
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DRIVER CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverCard(
    driver: DriverProfile,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "dc"
    )

    // Online pulse
    val infiniteTransition = rememberInfiniteTransition(label = "onlinePulse")
    val dotScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = if (driver.isOnline) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(if (driver.isOnline) 1000 else 0, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(
                1.dp,
                when {
                    isPressed       -> BorderFocus
                    driver.isOnline -> Sage.copy(alpha = 0.2f)
                    else            -> BorderSubtle
                },
                PahadiRaahShapes.large
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(18.dp)
    ) {
        // ── Top row ───────────────────────────────────────────────────────────
        Row(
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar + online dot
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(PahadiRaahShapes.medium)
                        .background(
                            Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.7f)))
                        )
                        .border(2.dp, BorderSubtle, PahadiRaahShapes.medium)
                ) {
                    Text(text = driver.emoji, fontSize = 28.sp)
                }
                // Online indicator dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .scale(dotScale)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(if (driver.isOnline) Sage else SurfaceMedium)
                        .border(2.dp, Pine, CircleShape)
                )
            }

            // Name + info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text     = driver.name,
                        style    = PahadiRaahTypography.titleMedium.copy(color = Snow),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (driver.isOnline) {
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(Moss.copy(alpha = 0.2f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text  = "Online",
                                style = BadgeStyle.copy(color = Sage, fontSize = 9.sp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text  = driver.speciality,
                    style = PahadiRaahTypography.bodySmall.copy(
                        color    = Amber,
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RatingStars(rating = driver.rating)
                    Text(
                        text  = "${driver.rating}",
                        style = PahadiRaahTypography.labelMedium.copy(
                            color         = Amber,
                            letterSpacing = 0.sp
                        )
                    )
                    Text(
                        text  = "•",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.3f))
                    )
                    Text(
                        text  = "${driver.totalTrips} trips",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
                    )
                    Text(
                        text  = "•",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.3f))
                    )
                    Text(
                        text  = "${driver.yearsActive} yrs",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
                    )
                }
            }

            // Fare + vehicle
            Column(horizontalAlignment = Alignment.End) {
                Text(text = driver.vehicleEmoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = driver.fare,
                    style = FareStyle.copy(fontSize = 16.sp)
                )
                Text(
                    text  = "/ seat",
                    style = PahadiRaahTypography.bodySmall.copy(
                        color    = Sage.copy(alpha = 0.5f),
                        fontSize = 9.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Badges ────────────────────────────────────────────────────────────
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(driver.badges) { badge ->
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(PillShape)
                        .background(Gold.copy(alpha = 0.09f))
                        .border(1.dp, Gold.copy(alpha = 0.18f), PillShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = badge.emoji, fontSize = 10.sp)
                    Text(
                        text  = badge.label,
                        style = BadgeStyle.copy(
                            color    = Amber.copy(alpha = 0.8f),
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Routes ────────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            driver.routes.forEach { route ->
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PahadiRaahShapes.small)
                        .background(SurfaceMedium)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Sage.copy(alpha = 0.5f))
                    )
                    Text(
                        text     = route,
                        style    = PahadiRaahTypography.bodySmall.copy(color = Mist, fontSize = 12.sp),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Bottom row: next trip + languages + seats ─────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Next trip
            if (driver.nextTrip != null) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(PahadiRaahShapes.small)
                        .background(Moss.copy(alpha = 0.1f))
                        .border(1.dp, Sage.copy(alpha = 0.2f), PahadiRaahShapes.small)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(text = "📅", fontSize = 11.sp)
                    Text(
                        text     = driver.nextTrip,
                        style    = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 10.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PahadiRaahShapes.small)
                        .background(SurfaceMedium)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text  = "No trips scheduled",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = Sage.copy(alpha = 0.35f),
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Seats left
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(
                        if (driver.seatsAvailable <= 1) Gold.copy(alpha = 0.15f)
                        else Moss.copy(alpha = 0.12f)
                    )
                    .border(
                        1.dp,
                        if (driver.seatsAvailable <= 1) Gold.copy(alpha = 0.3f)
                        else Moss.copy(alpha = 0.2f),
                        PillShape
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    text  = "${driver.seatsAvailable} 🪑",
                    style = BadgeStyle.copy(
                        color    = if (driver.seatsAvailable <= 1) Amber else Sage,
                        fontSize = 11.sp
                    )
                )
            }

            // Book button
            BookDriverButton(onClick = onClick)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  RATING STARS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RatingStars(rating: Float) {
    val fullStars = rating.toInt()
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(5) { i ->
            Text(
                text  = if (i < fullStars) "★" else "☆",
                style = PahadiRaahTypography.bodySmall.copy(
                    color    = if (i < fullStars) Gold else Sage.copy(alpha = 0.25f),
                    fontSize = 11.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BOOK BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BookDriverButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.93f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "bdb"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .scale(scale)
            .clip(PillShape)
            .background(Brush.horizontalGradient(GradientMoss))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text  = "View →",
            style = PahadiRaahTypography.labelMedium.copy(
                color         = Snow,
                letterSpacing = 0.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DRIVER FILTERS PANEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverFiltersPanel(
    minRating: Float,
    onRatingChange: (Float) -> Unit,
    vehicleFilter: String?,
    onVehicleChange: (String?) -> Unit,
    onlineOnly: Boolean,
    onOnlineToggle: (Boolean) -> Unit,
    sortBy: DriverSortOption,
    onSortChange: (DriverSortOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Online only toggle
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🟢", fontSize = 14.sp)
                Text(
                    text  = "Online drivers only",
                    style = PahadiRaahTypography.bodyMedium.copy(color = Mist)
                )
            }
            Switch(
                checked         = onlineOnly,
                onCheckedChange = onOnlineToggle,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor       = Snow,
                    checkedTrackColor       = Moss,
                    uncheckedThumbColor     = Mist.copy(alpha = 0.5f),
                    uncheckedTrackColor     = SurfaceMedium,
                    uncheckedBorderColor    = BorderSubtle,
                    checkedBorderColor      = Sage
                )
            )
        }

        // Min rating
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "MIN RATING", style = FormLabelStyle)
                Text(
                    text  = if (minRating == 0f) "Any" else "⭐ ${"%.1f".format(minRating)}+",
                    style = PahadiRaahTypography.labelMedium.copy(color = Amber, letterSpacing = 0.sp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value         = minRating,
                onValueChange = onRatingChange,
                valueRange    = 0f..5f,
                steps         = 9,
                colors        = SliderDefaults.colors(
                    thumbColor         = Amber,
                    activeTrackColor   = Amber,
                    inactiveTrackColor = SurfaceMedium
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Vehicle type
        Column {
            Text(text = "VEHICLE TYPE", style = FormLabelStyle)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "All", "Sedan" to "🚙", "SUV" to "🚐", "Tempo" to "🚌")
                    .forEach { (value, label) ->
                        val isSelected = vehicleFilter == value
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(PahadiRaahShapes.small)
                                .background(
                                    if (isSelected) Moss.copy(alpha = 0.2f) else SurfaceMedium
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Sage.copy(alpha = 0.4f) else BorderSubtle,
                                    PahadiRaahShapes.small
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                    onClick           = { onVehicleChange(value) }
                                )
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text  = label,
                                style = PahadiRaahTypography.labelSmall.copy(
                                    color         = if (isSelected) Sage else Mist.copy(alpha = 0.6f),
                                    fontSize      = 13.sp,
                                    letterSpacing = 0.sp
                                )
                            )
                        }
                    }
            }
        }

        // Sort by
        Column {
            Text(text = "SORT BY", style = FormLabelStyle)
            Spacer(modifier = Modifier.height(10.dp))
            val sortOptions = listOf(
                DriverSortOption.RATING    to "⭐ Rating",
                DriverSortOption.TRIPS     to "🛣️ Most Trips",
                DriverSortOption.FARE_LOW  to "₹ Cheapest",
                DriverSortOption.FARE_HIGH to "₹ Priciest",
                DriverSortOption.AVAILABILITY to "🟢 Available",
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sortOptions) { (option, label) ->
                    val isSelected = sortBy == option
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (isSelected) Moss.copy(alpha = 0.2f) else SurfaceMedium)
                            .border(
                                1.dp,
                                if (isSelected) Sage.copy(alpha = 0.4f) else BorderSubtle,
                                PillShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                                onClick           = { onSortChange(option) }
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text  = label,
                            style = PahadiRaahTypography.labelSmall.copy(
                                color         = if (isSelected) Sage else Mist.copy(alpha = 0.6f),
                                fontSize      = 11.sp,
                                letterSpacing = 0.sp
                            )
                        )
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
fun DriverFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (isSelected) Moss else SurfaceLight.copy(alpha = 0.5f), tween(200), label = "dfcBg"
    )
    val border by animateColorAsState(
        if (isSelected) Sage.copy(alpha = 0.5f) else BorderSubtle, tween(200), label = "dfcB"
    )
    val textClr by animateColorAsState(
        if (isSelected) Snow else Sage.copy(alpha = 0.6f), tween(200), label = "dfcT"
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
            style = PahadiRaahTypography.labelMedium.copy(color = textClr, letterSpacing = 0.sp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EMPTY STATE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverEmptyState(onReset: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier            = Modifier.fillMaxWidth().padding(40.dp)
    ) {
        Text(text = "👤", fontSize = 52.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text      = "No Drivers Found",
            style     = PahadiRaahTypography.titleMedium.copy(color = Snow),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = "Try adjusting your search\nor clearing the filters",
            style     = PahadiRaahTypography.bodySmall.copy(
                color     = Sage.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(PillShape)
                .background(SurfaceLight)
                .border(1.dp, BorderSubtle, PillShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onReset
                )
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text  = "Clear Filters",
                style = PahadiRaahTypography.labelMedium.copy(color = Mist, letterSpacing = 0.sp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PREVIEW
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BrowseDriversPreview() {
    PahadiRaahTheme {
        BrowseDriversScreen(onBack = {}, onDriverSelect = {})
    }
}