package com.kunpitech.pahadiraah.ui.screens.passenger

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.pahadiraah.data.model.RouteDto
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.viewmodel.RouteViewModel
import com.kunpitech.pahadiraah.ui.theme.*
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
//  DATA
// ─────────────────────────────────────────────────────────────────────────────

data class RouteResult(
    val id: String,
    val emoji: String,
    val origin: String,
    val destination: String,
    val date: String,
    val time: String,
    val driverName: String,
    val driverEmoji: String,
    val driverRating: Float,
    val driverTrips: Int,
    val vehicle: String,
    val fare: String,
    val seatsLeft: Int,
    val totalSeats: Int,
    val duration: String
)

enum class SortOption { PRICE_LOW, PRICE_HIGH, RATING, SEATS, TIME }

val allRoutes = listOf(
    RouteResult("1","🏔️","Shimla",     "Manali",       "Jun 22","6:00 AM","Ramesh Kumar","🧔",4.9f,134,"SUV / Jeep",   "₹850",  2,4,"6–7 hrs"),
    RouteResult("2","🌄","Dehradun",   "Mussoorie",    "Jun 22","8:30 AM","Sita Devi",   "👩",4.7f,89, "Sedan",        "₹300",  4,4,"1.5 hrs"),
    RouteResult("3","⛰️","Nainital",   "Bhimtal",      "Jun 23","9:00 AM","Arjun Singh", "👨",4.5f,52, "Sedan",        "₹180",  3,4,"45 min"),
    RouteResult("4","🗻","Dharamshala","Spiti Valley", "Jun 25","5:00 AM","Dev Mehta",   "👨",4.8f,212,"SUV / Jeep",   "₹1,200",1,6,"8–9 hrs"),
    RouteResult("5","🌲","Rishikesh",  "Chopta",       "Jun 23","7:00 AM","Meena Rawat", "👩",4.6f,77, "Tempo",        "₹550",  6,12,"4 hrs"),
    RouteResult("6","🏔️","Shimla",     "Manali",       "Jun 23","7:30 AM","Vikram Thakur","👨",4.4f,38,"Sedan",        "₹750",  2,4,"7 hrs"),
    RouteResult("7","🌊","Haridwar",   "Badrinath",    "Jun 24","4:00 AM","Priyanka Dev","👩",4.9f,155,"SUV / Jeep",   "₹950",  3,6,"9 hrs"),
    RouteResult("8","🌿","Shimla",     "Kalpa",        "Jun 26","6:30 AM","Raj Kumar",   "👨",4.7f,91, "SUV / Jeep",   "₹700",  4,6,"6 hrs"),
)

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

// Helper: map RouteDto from Supabase → existing RouteResult UI model
private fun RouteDto.toRouteResult() = RouteResult(
    id           = id,
    emoji        = when {
        origin.contains("Shimla",      ignoreCase = true) -> "🏔️"
        origin.contains("Dehradun",    ignoreCase = true) -> "🌄"
        origin.contains("Nainital",    ignoreCase = true) -> "⛰️"
        origin.contains("Dharamshala", ignoreCase = true) -> "🌲"
        origin.contains("Rishikesh",   ignoreCase = true) -> "🌊"
        origin.contains("Haridwar",    ignoreCase = true) -> "🌊"
        else -> "🏕️"
    },
    origin       = origin,
    destination  = destination,
    date         = date,
    time         = time.take(5),
    driverName   = users?.name ?: "Driver",
    driverEmoji  = users?.emoji ?: "🧑",
    driverRating = users?.avgRating?.toFloat() ?: 0f,
    driverTrips  = users?.totalTrips ?: 0,
    vehicle      = "Vehicle",
    fare         = "₹$farePerSeat",
    seatsLeft    = seatsLeft,
    totalSeats   = seatsTotal,
    duration     = durationHrs
)

@Composable
fun SearchRoutesScreen(
    onBack: () -> Unit,
    onRouteClick: (String) -> Unit,
    routeVm: RouteViewModel = hiltViewModel()
) {
    val context        = LocalContext.current
    val focusManager   = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val searchState    by routeVm.searchState.collectAsStateWithLifecycle()
    val suggestions    by routeVm.suggestions.collectAsStateWithLifecycle()

    // ── Search state ──────────────────────────────────────────────────────────
    var originQuery    by remember { mutableStateOf("") }
    var destQuery      by remember { mutableStateOf("") }
    var activeField    by remember { mutableStateOf<String?>(null) } // "origin" | "dest" | null
    var selectedDate by remember { mutableStateOf("") }
    var seatCount    by remember { mutableIntStateOf(1) }
    var sortBy       by remember { mutableStateOf(SortOption.TIME) }
    var showFilters  by remember { mutableStateOf(false) }
    var hasSearched  by remember { mutableStateOf(false) }

    val isSearching = searchState is UiState.Loading

    // Filter state (applied client-side after fetch)
    var maxFare       by remember { mutableFloatStateOf(2000f) }
    var minRating     by remember { mutableFloatStateOf(0f) }
    var vehicleFilter by remember { mutableStateOf<String?>(null) }

    // Date picker
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                selectedDate = "%02d / %02d / %04d".format(day, month + 1, year)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).also { it.datePicker.minDate = System.currentTimeMillis() }
    }

    // ── Search logic — calls ViewModel which hits Supabase ────────────────────
    fun doSearch() {
        hasSearched = true
        focusManager.clearFocus()
        routeVm.clearSuggestions()
        activeField = null
        // Convert display date "DD / MM / YYYY" → DB format "YYYY-MM-DD"
        val dbDate = if (selectedDate.isNotBlank()) {
            val parts = selectedDate.split(" / ")
            if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else ""
        } else ""
        routeVm.searchRoutes(originQuery, destQuery, dbDate, seatCount)
    }

    // Map Supabase results → UI model with client-side filter/sort
    val rawResults = when (val s = searchState) {
        is UiState.Success -> s.data.map { it.toRouteResult() }
        else -> emptyList()
    }

    val results = remember(rawResults, sortBy, maxFare, minRating, vehicleFilter) {
        rawResults
            .filter { r ->
                val matchFare    = r.fare.replace("₹","").replace(",","").toFloatOrNull()?.let { it <= maxFare } ?: true
                val matchRating  = r.driverRating >= minRating
                val matchVehicle = vehicleFilter == null || r.vehicle.contains(vehicleFilter!!, ignoreCase = true)
                matchFare && matchRating && matchVehicle
            }
            .sortedWith(when (sortBy) {
                SortOption.PRICE_LOW  -> compareBy { it.fare.replace("₹","").replace(",","").toIntOrNull() ?: 0 }
                SortOption.PRICE_HIGH -> compareByDescending { it.fare.replace("₹","").replace(",","").toIntOrNull() ?: 0 }
                SortOption.RATING     -> compareByDescending { it.driverRating }
                SortOption.SEATS      -> compareByDescending { it.seatsLeft }
                SortOption.TIME       -> compareBy { it.time }
            })
    }

    /*fun doSearch() {
        focusManager.clearFocus()
        isSearching = true
        hasSearched = true
        isSearching = false
    }*/

    // ── Entrance animation ────────────────────────────────────────────────────
    var started by remember { mutableStateOf(false) }
    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(400), label = "ha")
    val headerOffset by animateFloatAsState(if (started) 0f else -20f, tween(500, easing = EaseOutCubic), label = "hY")
    val formAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 100), label = "fa")
    val formOffset   by animateFloatAsState(if (started) 0f else 20f, tween(600, delayMillis = 100, easing = EaseOutCubic), label = "fY")
    LaunchedEffect(Unit) {
        started = true
        focusRequester.requestFocus()
    }

    // ═════════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Pine)
    ) {
        // Ambient glow top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
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
                    Text(text = "FIND A RIDE", style = EyebrowStyle.copy(fontSize = 10.sp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = "Search Routes",
                        style = PahadiRaahTypography.titleLarge.copy(color = Snow)
                    )
                }
            }

            LazyColumn(
                modifier       = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // ── SEARCH FORM ───────────────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .alpha(formAlpha)
                            .graphicsLayer { translationY = formOffset }
                    ) {
                        // Origin field
                        SearchInputField(
                            value           = originQuery,
                            onValueChange   = {
                                originQuery = it
                                activeField = "origin"
                                if (it.length >= 2) routeVm.suggestPlaces(it)
                                else routeVm.clearSuggestions()
                            },
                            placeholder     = "From — e.g. Shimla",
                            leadingEmoji    = "📍",
                            focusRequester  = focusRequester,
                            onSearch        = { doSearch() },
                            trailingContent = if (originQuery.isNotEmpty()) {{
                                Icon(
                                    imageVector        = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint               = Sage.copy(alpha = 0.5f),
                                    modifier           = Modifier
                                        .size(16.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { originQuery = "" }
                                        )
                                )
                            }} else null
                        )

                        // Autocomplete dropdown
                        if (suggestions.isNotEmpty() && activeField != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PahadiRaahShapes.medium)
                                    .background(SurfaceLow)
                                    .border(1.dp, BorderSubtle, PahadiRaahShapes.medium)
                            ) {
                                suggestions.forEach { place ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                when (activeField) {
                                                    "origin" -> originQuery = place
                                                    "dest"   -> destQuery   = place
                                                }
                                                routeVm.clearSuggestions()
                                                activeField = null
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Text("📍", fontSize = 14.sp)
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text  = place,
                                            style = PahadiRaahTypography.bodyMedium.copy(color = SnowPeak)
                                        )
                                    }
                                    if (suggestions.last() != place)
                                        Divider(color = BorderGhost, thickness = 0.5.dp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Spacer(modifier = Modifier.height(1.dp))

                        // Gradient connector
                        Row(modifier = Modifier.padding(start = 30.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(10.dp)
                                    .background(
                                        Brush.verticalGradient(listOf(Sage.copy(alpha = 0.5f), Amber.copy(alpha = 0.5f)))
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(1.dp))

                        // Destination field
                        SearchInputField(
                            value           = destQuery,
                            onValueChange   = {
                                destQuery = it
                                activeField = "dest"
                                if (it.length >= 2) routeVm.suggestPlaces(it)
                                else routeVm.clearSuggestions()
                            },
                            placeholder     = "To — e.g. Manali",
                            leadingEmoji    = "🏁",
                            onSearch        = { doSearch() },
                            trailingContent = if (destQuery.isNotEmpty()) {{
                                Icon(
                                    imageVector        = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint               = Sage.copy(alpha = 0.5f),
                                    modifier           = Modifier
                                        .size(16.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { destQuery = "" }
                                        )
                                )
                            }} else null
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Date + Seats row
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Date
                            DatePickerField(
                                value    = selectedDate,
                                modifier = Modifier.weight(1.4f),
                                onClick  = { datePickerDialog.show() }
                            )
                            // Seat counter
                            SeatCountField(
                                seats      = seatCount,
                                onIncrease = { if (seatCount < 8) seatCount++ },
                                onDecrease = { if (seatCount > 1) seatCount-- },
                                modifier   = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Search button
                        SearchButton(
                            onClick = { doSearch() }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Filters toggle
                        FiltersToggle(
                            showFilters = showFilters,
                            onClick     = { showFilters = !showFilters }
                        )

                        // Expandable filters panel
                        AnimatedVisibility(
                            visible = showFilters,
                            enter   = fadeIn(tween(200)) + expandVertically(tween(300, easing = EaseOutCubic)),
                            exit    = fadeOut(tween(150)) + shrinkVertically(tween(250))
                        ) {
                            FiltersPanel(
                                maxFare       = maxFare,
                                onFareChange  = { maxFare = it },
                                minRating     = minRating,
                                onRatingChange = { minRating = it },
                                vehicleFilter  = vehicleFilter,
                                onVehicleChange = { vehicleFilter = it },
                                sortBy         = sortBy,
                                onSortChange   = { sortBy = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── RESULTS or INITIAL STATE ───────────────────────────────────
                if (!hasSearched) {
                    item {
                        SearchInitialState(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .alpha(formAlpha)
                        )
                    }
                } else if (results.isEmpty()) {
                    item {
                        SearchEmptyState(
                            onReset = {
                                originQuery   = ""
                                destQuery     = ""
                                selectedDate  = ""
                                vehicleFilter = null
                                minRating     = 0f
                                maxFare       = 2000f
                                hasSearched   = false
                            },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                } else {
                    // Results header
                    item {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text  = "${results.size} route${if (results.size > 1) "s" else ""} found",
                                style = PahadiRaahTypography.titleSmall.copy(color = Snow)
                            )
                            SortLabel(sortBy = sortBy)
                        }
                    }

                    // Result cards
                    itemsIndexed(results, key = { _, r -> r.id }) { index, result ->
                        AnimatedVisibility(
                            visible = true,
                            enter   = fadeIn(tween(250, index * 50)) +
                                    slideInVertically(tween(300, index * 50)) { it / 3 }
                        ) {
                            RouteResultCard(
                                result  = result,
                                onClick = { onRouteClick(result.id) },
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEARCH INPUT FIELD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingEmoji: String,
    onSearch: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    trailingContent: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (isFocused) BorderFocus else BorderSubtle, tween(200), label = "sib"
    )
    val bgColor by animateColorAsState(
        if (isFocused) Sage.copy(alpha = 0.06f) else SurfaceLight, tween(200), label = "sibg"
    )

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.small)
            .background(bgColor)
            .border(1.dp, borderColor, PahadiRaahShapes.small)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(text = leadingEmoji, fontSize = 16.sp)
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
                            text  = placeholder,
                            style = PahadiRaahTypography.bodyMedium.copy(
                                color    = Sage.copy(alpha = 0.35f),
                                fontSize = 15.sp
                            )
                        )
                    }
                    inner()
                }
            }
        )
        trailingContent?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DATE PICKER FIELD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DatePickerField(
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val borderColor by animateColorAsState(
        if (isPressed) BorderFocus else BorderSubtle, tween(150), label = "dpb"
    )

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(PahadiRaahShapes.small)
            .background(SurfaceLight)
            .border(1.dp, borderColor, PahadiRaahShapes.small)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector        = Icons.Default.DateRange,
            contentDescription = "Date",
            tint               = if (value.isNotBlank()) Sage else Sage.copy(alpha = 0.35f),
            modifier           = Modifier.size(16.dp)
        )
        Text(
            text     = value.ifBlank { "Any date" },
            style    = PahadiRaahTypography.bodySmall.copy(
                color    = if (value.isNotBlank()) Snow else Sage.copy(alpha = 0.35f),
                fontSize = 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEAT COUNT FIELD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SeatCountField(
    seats: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .clip(PahadiRaahShapes.small)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.small)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        MiniStepBtn(
            icon    = Icons.Default.KeyboardArrowDown,
            enabled = seats > 1,
            onClick = onDecrease
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = seats.toString(),
                style = PahadiRaahTypography.titleMedium.copy(color = Snow, fontSize = 18.sp)
            )
            Text(
                text  = "seat${if (seats > 1) "s" else ""}",
                style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 9.sp)
            )
        }
        MiniStepBtn(
            icon    = Icons.Default.KeyboardArrowUp,
            enabled = seats < 8,
            onClick = onIncrease
        )
    }
}

@Composable
fun MiniStepBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed && enabled) 0.85f else 1f,
        spring(stiffness = Spring.StiffnessHigh),
        label = "ms"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (enabled) SurfaceMedium else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = enabled,
                onClick           = onClick
            )
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = if (enabled) Sage else Sage.copy(alpha = 0.2f),
            modifier           = Modifier.size(18.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEARCH BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "sbtn"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(scale)
            .clip(PillShape)
            .background(Brush.horizontalGradient(GradientMoss))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Search,
                contentDescription = "Search",
                tint               = Snow,
                modifier           = Modifier.size(20.dp)
            )
            Text(
                text  = "Search Routes",
                style = PahadiRaahTypography.labelLarge.copy(color = Snow, fontSize = 16.sp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FILTERS TOGGLE + PANEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FiltersToggle(showFilters: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                onClick           = onClick
            )
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(text = "⚙️", fontSize = 13.sp)
        Text(
            text  = if (showFilters) "Hide Filters" else "Filters & Sort",
            style = PahadiRaahTypography.labelMedium.copy(
                color         = if (showFilters) Sage else Mist,
                letterSpacing = 0.sp
            )
        )
        Icon(
            imageVector        = if (showFilters) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint               = Sage.copy(alpha = 0.6f),
            modifier           = Modifier.size(16.dp)
        )
    }
}

@Composable
fun FiltersPanel(
    maxFare: Float,
    onFareChange: (Float) -> Unit,
    minRating: Float,
    onRatingChange: (Float) -> Unit,
    vehicleFilter: String?,
    onVehicleChange: (String?) -> Unit,
    sortBy: SortOption,
    onSortChange: (SortOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Max fare slider
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "MAX FARE", style = FormLabelStyle)
                Text(
                    text  = if (maxFare >= 2000f) "Any" else "₹${maxFare.toInt()}",
                    style = PahadiRaahTypography.labelMedium.copy(color = Amber, letterSpacing = 0.sp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value         = maxFare,
                onValueChange = onFareChange,
                valueRange    = 100f..2000f,
                steps         = 18,
                colors        = SliderDefaults.colors(
                    thumbColor         = Amber,
                    activeTrackColor   = Amber,
                    inactiveTrackColor = SurfaceMedium
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Min rating
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "MIN DRIVER RATING", style = FormLabelStyle)
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

        // Vehicle filter
        Column {
            Text(text = "VEHICLE TYPE", style = FormLabelStyle)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "All", "Sedan" to "🚙 Sedan", "SUV" to "🚐 SUV", "Tempo" to "🚌 Tempo")
                    .forEach { (value, label) ->
                        val isSelected = vehicleFilter == value
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(
                                    if (isSelected) Moss.copy(alpha = 0.2f) else SurfaceMedium
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Sage.copy(alpha = 0.4f) else BorderSubtle,
                                    PillShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                    onClick           = { onVehicleChange(value) }
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

        // Sort
        Column {
            Text(text = "SORT BY", style = FormLabelStyle)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val sortOptions = listOf(
                    SortOption.TIME       to "🕐 Time",
                    SortOption.PRICE_LOW  to "₹ Cheapest",
                    SortOption.PRICE_HIGH to "₹ Priciest",
                    SortOption.RATING     to "⭐ Rating",
                    SortOption.SEATS      to "🪑 Seats",
                )
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
//  ROUTE RESULT CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RouteResultCard(
    result: RouteResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "rrc"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(
                1.dp,
                if (result.seatsLeft == 1) Gold.copy(alpha = 0.35f)
                else if (isPressed) BorderFocus else BorderSubtle,
                PahadiRaahShapes.large
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(16.dp)
    ) {
        // ── Top row ───────────────────────────────────────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Route icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(PahadiRaahShapes.medium)
                    .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.65f))))
            ) {
                Text(text = result.emoji, fontSize = 22.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                // Route title
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text     = result.origin,
                        style    = PahadiRaahTypography.titleSmall.copy(color = Snow, fontSize = 14.sp),
                        maxLines = 1
                    )
                    Text(
                        text  = "→",
                        style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.5f))
                    )
                    Text(
                        text     = result.destination,
                        style    = PahadiRaahTypography.titleSmall.copy(color = Snow, fontSize = 14.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text  = "📅 ${result.date}  •  🕐 ${result.time}  •  ⏱ ${result.duration}",
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 10.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Fare
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text  = result.fare,
                    style = FareStyle.copy(fontSize = 18.sp)
                )
                Text(
                    text  = "/ seat",
                    style = PahadiRaahTypography.bodySmall.copy(
                        color    = Sage.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Divider ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, BorderSubtle, Color.Transparent))
                )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Bottom row ────────────────────────────────────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Driver mini avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.6f))))
            ) {
                Text(text = result.driverEmoji, fontSize = 14.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = result.driverName,
                    style    = PahadiRaahTypography.labelMedium.copy(
                        color         = Mist,
                        letterSpacing = 0.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = "⭐ ${result.driverRating}  •  ${result.driverTrips} trips  •  ${result.vehicle}",
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 10.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Seats badge
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(
                        if (result.seatsLeft == 1) Gold.copy(alpha = 0.15f)
                        else Moss.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (result.seatsLeft == 1) Gold.copy(alpha = 0.3f)
                        else Moss.copy(alpha = 0.2f),
                        PillShape
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text  = "${result.seatsLeft} seat${if (result.seatsLeft > 1) "s" else ""} left",
                    style = BadgeStyle.copy(
                        color    = if (result.seatsLeft == 1) Amber else Sage,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SORT LABEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SortLabel(sortBy: SortOption) {
    val label = when (sortBy) {
        SortOption.TIME       -> "🕐 By time"
        SortOption.PRICE_LOW  -> "₹ Cheapest first"
        SortOption.PRICE_HIGH -> "₹ Priciest first"
        SortOption.RATING     -> "⭐ Top rated"
        SortOption.SEATS      -> "🪑 Most seats"
    }
    Text(
        text  = label,
        style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  INITIAL STATE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchInitialState(modifier: Modifier = Modifier) {
    val popular = listOf("Shimla → Manali", "Dehradun → Mussoorie", "Nainital → Bhimtal", "Rishikesh → Chopta")

    Column(
        modifier            = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🔍", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text      = "Search your mountain route",
            style     = PahadiRaahTypography.titleSmall.copy(color = Snow),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text      = "Enter origin & destination to discover rides",
            style     = PahadiRaahTypography.bodySmall.copy(
                color     = Sage.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text  = "POPULAR SEARCHES",
            style = EyebrowStyle.copy(fontSize = 9.sp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        popular.forEach { route ->
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(PahadiRaahShapes.small)
                    .background(SurfaceLight)
                    .border(1.dp, BorderSubtle, PahadiRaahShapes.small)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(text = "🔥", fontSize = 14.sp)
                Text(
                    text     = route,
                    style    = PahadiRaahTypography.bodyMedium.copy(color = Mist, fontSize = 14.sp),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text  = "→",
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.4f))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EMPTY STATE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchEmptyState(onReset: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🗺️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text      = "No Routes Found",
            style     = PahadiRaahTypography.titleMedium.copy(color = Snow),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = "Try adjusting your search or\nexpanding the filters",
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
                text  = "Clear Search",
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
fun SearchRoutesPreview() {
    PahadiRaahTheme {
        SearchRoutesScreen(onBack = {}, onRouteClick = {})
    }
}