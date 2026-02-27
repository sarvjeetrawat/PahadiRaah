package com.kunpitech.pahadiraah.ui.screens.driver

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.pahadiraah.data.model.ActionResult
import com.kunpitech.pahadiraah.ui.theme.*
import com.kunpitech.pahadiraah.viewmodel.RouteViewModel
import com.kunpitech.pahadiraah.viewmodel.UserViewModel
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
//  VEHICLE DATA
// ─────────────────────────────────────────────────────────────────────────────

data class VehicleOption(val emoji: String, val label: String, val maxSeats: Int, val type: String)

val vehicleOptions = listOf(
    VehicleOption("🚙", "Sedan / Hatchback", 4,  "sedan"),
    VehicleOption("🚐", "SUV / Jeep",        6,  "suv"),
    VehicleOption("🚌", "Tempo Traveller",   12, "tempo"),
)

// ─────────────────────────────────────────────────────────────────────────────
//  POST ROUTE SCREEN  — fully dynamic
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PostRouteScreen(
    onBack:         () -> Unit,
    onSuccess:      () -> Unit,
    routeViewModel: RouteViewModel = hiltViewModel(),
    userViewModel:  UserViewModel  = hiltViewModel()
) {
    val context     = LocalContext.current
    val postResult  by routeViewModel.postResult.collectAsStateWithLifecycle()
    val myVehicle   by userViewModel.myVehicle.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { userViewModel.loadMyVehicle() }

    // ── Form state ────────────────────────────────────────────────────────────
    var origin          by remember { mutableStateOf("") }
    var destination     by remember { mutableStateOf("") }
    var selectedDate    by remember { mutableStateOf("") }   // display: "22 / 06 / 2025"
    var selectedTime    by remember { mutableStateOf("") }   // display: "06 : 00 AM"
    var rawDate         by remember { mutableStateOf("") }   // DB: "2025-06-22"
    var rawTime         by remember { mutableStateOf("") }   // DB: "06:00:00"
    var rawHour         by remember { mutableIntStateOf(6) }
    var rawMinute       by remember { mutableIntStateOf(0) }
    var selectedVehicle by remember { mutableStateOf(vehicleOptions[1]) }
    var seats           by remember { mutableIntStateOf(2) }
    var price           by remember { mutableStateOf("") }
    var notes           by remember { mutableStateOf("") }
    var showSuccess     by remember { mutableStateOf(false) }
    var errorMsg        by remember { mutableStateOf<String?>(null) }

    // Clamp seats when vehicle changes
    LaunchedEffect(selectedVehicle) {
        if (seats > selectedVehicle.maxSeats) seats = selectedVehicle.maxSeats
    }

    // Watch postResult
    LaunchedEffect(postResult) {
        when (postResult) {
            is ActionResult.Success -> {
                routeViewModel.resetPostResult()
                showSuccess = true
            }
            is ActionResult.Error -> {
                errorMsg = (postResult as ActionResult.Error).message
            }
            else -> {}
        }
    }

    val isLoading  = postResult is ActionResult.Loading
    val isFormValid = origin.isNotBlank()
            && destination.isNotBlank()
            && rawDate.isNotBlank()
            && rawTime.isNotBlank()
            && price.isNotBlank()
            && price.toIntOrNull() != null

    // ── Date picker ───────────────────────────────────────────────────────────
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                selectedDate = "%02d / %02d / %04d".format(day, month + 1, year)
                rawDate      = "%04d-%02d-%02d".format(year, month + 1, day)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).also { it.datePicker.minDate = System.currentTimeMillis() }
    }

    // ── Time picker ───────────────────────────────────────────────────────────
    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                rawHour   = hour
                rawMinute = minute
                val amPm  = if (hour < 12) "AM" else "PM"
                val h     = if (hour % 12 == 0) 12 else hour % 12
                selectedTime = "%02d : %02d %s".format(h, minute, amPm)
                rawTime      = "%02d:%02d:00".format(hour, minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
    }

    // ── Entrance animations ───────────────────────────────────────────────────
    var started by remember { mutableStateOf(false) }
    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(400), label = "ha")
    val headerOffset by animateFloatAsState(if (started) 0f else -24f, tween(500, easing = EaseOutCubic), label = "hY")
    val formAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 150), label = "fa")
    val formOffset   by animateFloatAsState(if (started) 0f else 32f, tween(600, delayMillis = 150, easing = EaseOutCubic), label = "fY")
    LaunchedEffect(Unit) { started = true }

    if (showSuccess) {
        SuccessOverlay(onDone = onSuccess)
        return
    }

    // ═════════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Brush.verticalGradient(listOf(PineMid.copy(alpha = 0.12f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
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
                        .background(SurfaceLow)
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
                        tint               = MistVeil,
                        modifier           = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "POST A ROUTE", style = EyebrowStyle.copy(fontSize = 10.sp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = "Share Your Journey",
                        style = PahadiRaahTypography.titleLarge.copy(color = SnowPeak)
                    )
                }
            }

            // ── Form ──────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .alpha(formAlpha)
                    .graphicsLayer { translationY = formOffset }
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp)
            ) {

                // Route preview card
                RouteVisualCard(
                    origin      = origin.ifBlank { "Where does your journey begin?" },
                    destination = destination.ifBlank { "Where are you headed?" }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Route details
                FormSectionLabel("ROUTE DETAILS")
                Spacer(modifier = Modifier.height(12.dp))
                PahadiTextField(
                    value         = origin,
                    onValueChange = { origin = it },
                    label         = "Origin",
                    placeholder   = "e.g. Shimla Bus Stand",
                    leadingEmoji  = "📍"
                )
                Spacer(modifier = Modifier.height(12.dp))
                PahadiTextField(
                    value         = destination,
                    onValueChange = { destination = it },
                    label         = "Destination",
                    placeholder   = "e.g. Manali Town Center",
                    leadingEmoji  = "🏁"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Schedule
                FormSectionLabel("SCHEDULE")
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateTimePickerField(
                        value       = selectedDate,
                        label       = "Departure Date",
                        placeholder = "Select Date",
                        emoji       = "📅",
                        modifier    = Modifier.weight(1f),
                        onClick     = { datePickerDialog.show() }
                    )
                    DateTimePickerField(
                        value       = selectedTime,
                        label       = "Departure Time",
                        placeholder = "Select Time",
                        emoji       = "🕐",
                        modifier    = Modifier.weight(1f),
                        onClick     = { timePickerDialog.show() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Vehicle type
                FormSectionLabel("VEHICLE TYPE")
                Spacer(modifier = Modifier.height(12.dp))
                VehicleTypeSelector(
                    selected = selectedVehicle,
                    onSelect = { selectedVehicle = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Seats & price
                FormSectionLabel("SEATS & PRICING")
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SeatStepper(
                        seats      = seats,
                        maxSeats   = selectedVehicle.maxSeats,
                        onIncrease = { if (seats < selectedVehicle.maxSeats) seats++ },
                        onDecrease = { if (seats > 1) seats-- },
                        modifier   = Modifier.weight(1f)
                    )
                    PahadiTextField(
                        value         = price,
                        onValueChange = { price = it.filter { c -> c.isDigit() } },
                        label         = "Price / Seat",
                        placeholder   = "₹ 650",
                        leadingEmoji  = "💰",
                        keyboardType  = KeyboardType.Number,
                        modifier      = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Notes
                FormSectionLabel("NOTES (OPTIONAL)")
                Spacer(modifier = Modifier.height(12.dp))
                PahadiTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = "Notes",
                    placeholder   = "e.g. No smoking, luggage allowed, stop at Kullu...",
                    leadingEmoji  = "📝",
                    singleLine    = false,
                    minLines      = 3
                )

                // Error message
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PahadiRaahShapes.medium)
                            .background(StatusError.copy(alpha = 0.1f))
                            .border(1.dp, StatusError.copy(alpha = 0.3f), PahadiRaahShapes.medium)
                            .padding(14.dp)
                    ) {
                        Text(
                            text  = errorMsg!!,
                            style = PahadiRaahTypography.bodySmall.copy(color = StatusError)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Submit button
                PostRouteButton(
                    enabled   = isFormValid && !isLoading,
                    isLoading = isLoading,
                    onClick   = {
                        errorMsg = null

                        // Calculate duration from raw time for display
                        val durationHrs = "~${
                            when {
                                rawTime.startsWith("0") -> "4-5"
                                rawHour < 10 -> "5-6"
                                else -> "6-7"
                            }
                        } hrs"

                        routeViewModel.postRoute(
                            origin      = origin.trim(),
                            destination = destination.trim(),
                            date        = rawDate,
                            time        = rawTime,
                            durationHrs = durationHrs,
                            seatsTotal  = seats,
                            farePerSeat = price.toInt(),
                            vehicleId   = myVehicle?.id
                        )
                    }
                )

                if (!isFormValid && !isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text      = "Fill in origin, destination, date, time & price to continue",
                        style     = PahadiRaahTypography.bodySmall.copy(
                            color    = Sage.copy(alpha = 0.45f),
                            fontSize = 11.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUBMIT BUTTON  — shows spinner while loading
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PostRouteButton(enabled: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed && enabled) 0.96f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "btn"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale)
            .clip(PillShape)
            .background(
                if (enabled)
                    Brush.horizontalGradient(GradientPrimary)
                else
                    Brush.horizontalGradient(
                        listOf(PineDeep.copy(alpha = 0.5f), PineMid.copy(alpha = 0.3f))
                    )
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = enabled,
                onClick           = onClick
            )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color       = SnowPeak,
                strokeWidth = 2.dp,
                modifier    = Modifier.size(24.dp)
            )
        } else {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "🗺️", fontSize = 18.sp)
                Text(
                    text  = "Post Route",
                    style = PahadiRaahTypography.labelLarge.copy(
                        color    = if (enabled) SnowPeak else Sage.copy(alpha = 0.4f),
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  REMAINING COMPONENTS (unchanged UI, just color token updates)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DateTimePickerField(
    value: String, label: String, placeholder: String,
    emoji: String, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val borderColor by animateColorAsState(
        if (isPressed) BorderFocus else BorderSubtle, tween(150), label = "dtBorder"
    )
    Column(modifier = modifier) {
        Text(text = label.uppercase(), style = FormLabelStyle)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(PahadiRaahShapes.small)
                .background(SurfaceLow)
                .border(1.dp, borderColor, PahadiRaahShapes.small)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Text(
                text     = value.ifBlank { placeholder },
                style    = PahadiRaahTypography.bodyMedium.copy(
                    color    = if (value.isBlank()) Sage.copy(alpha = 0.35f) else SnowPeak,
                    fontSize = 14.sp
                ),
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Icon(
                imageVector        = Icons.Default.DateRange,
                contentDescription = null,
                tint               = Sage.copy(alpha = 0.5f),
                modifier           = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun VehicleTypeSelector(selected: VehicleOption, onSelect: (VehicleOption) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        vehicleOptions.forEach { vehicle ->
            val isSelected        = selected.label == vehicle.label
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed         by interactionSource.collectIsPressedAsState()
            val bgColor by animateColorAsState(
                if (isSelected) PineMid.copy(alpha = 0.14f) else if (isPressed) SurfaceMid else SurfaceLow,
                tween(150), label = "vcBg"
            )
            val borderColor by animateColorAsState(
                if (isSelected) Sage.copy(alpha = 0.45f) else BorderSubtle,
                tween(150), label = "vcBorder"
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PahadiRaahShapes.medium)
                    .background(bgColor)
                    .border(1.dp, borderColor, PahadiRaahShapes.medium)
                    .clickable(interactionSource = interactionSource, indication = null, onClick = { onSelect(vehicle) })
                    .padding(16.dp)
            ) {
                Text(text = vehicle.emoji, fontSize = 26.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = vehicle.label,
                        style = PahadiRaahTypography.bodyMedium.copy(
                            color = if (isSelected) SnowPeak else MistVeil
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = "Up to ${vehicle.maxSeats} seats",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color = if (isSelected) Sage else Sage.copy(alpha = 0.5f), fontSize = 11.sp
                        )
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Brush.verticalGradient(GradientPrimary)
                            else Brush.verticalGradient(listOf(SurfaceMid, SurfaceMid))
                        )
                        .border(1.5.dp, if (isSelected) Sage else BorderSubtle, CircleShape)
                ) {
                    if (isSelected) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SnowPeak))
                    }
                }
            }
        }
    }
}

@Composable
fun SeatStepper(
    seats: Int, maxSeats: Int,
    onIncrease: () -> Unit, onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = "AVAILABLE SEATS", style = FormLabelStyle)
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PahadiRaahShapes.small)
                .background(SurfaceLow)
                .border(1.dp, BorderSubtle, PahadiRaahShapes.small)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StepperButton(icon = Icons.Default.KeyboardArrowUp,   enabled = seats < maxSeats, onClick = onIncrease)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = seats.toString(),
                    style = PahadiRaahTypography.headlineMedium.copy(color = SnowPeak, fontSize = 32.sp)
                )
                Text(
                    text  = "of $maxSeats max",
                    style = PahadiRaahTypography.bodySmall.copy(color = Sage.copy(alpha = 0.5f), fontSize = 10.sp)
                )
            }
            StepperButton(icon = Icons.Default.KeyboardArrowDown, enabled = seats > 1,        onClick = onDecrease)
        }
    }
}

@Composable
fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed && enabled) 0.88f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "sb"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (enabled) Brush.verticalGradient(listOf(PineDeep, PineMid.copy(alpha = 0.7f)))
                else Brush.verticalGradient(listOf(SurfaceMid, SurfaceMid))
            )
            .border(1.dp, if (enabled) Sage.copy(alpha = 0.3f) else BorderSubtle, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = if (enabled) SnowPeak else Sage.copy(alpha = 0.25f),
            modifier           = Modifier.size(20.dp)
        )
    }
}

@Composable
fun RouteVisualCard(origin: String, destination: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLow)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Sage).border(3.dp, Sage.copy(alpha = 0.25f), CircleShape))
            Column {
                Text(text = "FROM", style = FormLabelStyle.copy(fontSize = 9.sp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = origin,
                    style = PahadiRaahTypography.bodyMedium.copy(
                        color    = if (origin.contains("begin")) Sage.copy(alpha = 0.35f) else SnowPeak,
                        fontSize = 14.sp
                    ),
                    maxLines = 1
                )
            }
        }
        Row(modifier = Modifier.padding(start = 5.dp)) {
            Box(modifier = Modifier.width(2.dp).height(28.dp).background(Brush.verticalGradient(listOf(Sage, Amber))))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Marigold).border(3.dp, Marigold.copy(alpha = 0.25f), CircleShape))
            Column {
                Text(text = "TO", style = FormLabelStyle.copy(fontSize = 9.sp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = destination,
                    style = PahadiRaahTypography.bodyMedium.copy(
                        color    = if (destination.contains("headed")) Sage.copy(alpha = 0.35f) else SnowPeak,
                        fontSize = 14.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PahadiTextField(
    value: String, onValueChange: (String) -> Unit,
    label: String, placeholder: String, leadingEmoji: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true, minLines: Int = 1
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(if (isFocused) BorderFocus else BorderSubtle, tween(200), label = "tfBorder")
    val bgColor     by animateColorAsState(if (isFocused) Sage.copy(alpha = 0.06f) else SurfaceLow, tween(200), label = "tfBg")

    Column(modifier = modifier) {
        Text(text = label.uppercase(), style = FormLabelStyle)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PahadiRaahShapes.small)
                .background(bgColor)
                .border(1.dp, borderColor, PahadiRaahShapes.small)
        ) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(horizontal = 14.dp)) {
                Text(text = leadingEmoji, fontSize = 16.sp, modifier = Modifier.padding(top = if (singleLine) 16.dp else 14.dp))
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value           = value,
                    onValueChange   = onValueChange,
                    singleLine      = singleLine,
                    minLines        = minLines,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle       = PahadiRaahTypography.bodyMedium.copy(color = SnowPeak, fontSize = 15.sp),
                    modifier        = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .onFocusChanged { isFocused = it.isFocused },
                    decorationBox   = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text  = placeholder,
                                    style = PahadiRaahTypography.bodyMedium.copy(color = Sage.copy(alpha = 0.35f), fontSize = 15.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FormSectionLabel(label: String) {
    Text(text = label, style = EyebrowStyle.copy(fontSize = 10.sp))
}

@Composable
fun SuccessOverlay(onDone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(400), label = "sAlpha")
    val scale by animateFloatAsState(
        if (visible) 1f else 0.6f, tween(500, easing = EaseOutBack), label = "sScale"
    )
    LaunchedEffect(Unit) {
        visible = true
        kotlinx.coroutines.delay(2400)
        onDone()
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Slate.copy(alpha = 0.96f))
            .alpha(alpha)
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(scale)
                .background(Brush.radialGradient(listOf(PineMid.copy(alpha = 0.22f), Color.Transparent)), CircleShape)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(88.dp).clip(CircleShape).background(Brush.verticalGradient(GradientPrimary))
            ) {
                Text(text = "✓", fontSize = 40.sp, color = SnowPeak)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text      = "Route Posted!",
                style     = PahadiRaahTypography.headlineMedium.copy(color = SnowPeak),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text      = "Your mountain route is now live\nfor passengers to discover",
                style     = PahadiRaahTypography.bodyMedium.copy(color = Sage, textAlign = TextAlign.Center)
            )
            Spacer(modifier = Modifier.height(32.dp))
            LinearProgressIndicator(
                modifier   = Modifier.width(120.dp).clip(PillShape),
                color      = Sage,
                trackColor = SurfaceLow
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text  = "Returning to dashboard...",
                style = PahadiRaahTypography.labelSmall.copy(
                    color = Sage.copy(alpha = 0.45f), letterSpacing = 1.sp, fontSize = 10.sp
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PostRouteScreenPreview() {
    PahadiRaahTheme {
        PostRouteScreen(onBack = {}, onSuccess = {})
    }
}