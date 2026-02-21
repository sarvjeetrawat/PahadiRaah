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
import com.kunpitech.pahadiraah.ui.theme.*
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
//  VEHICLE DATA — max seats per vehicle type
// ─────────────────────────────────────────────────────────────────────────────

data class VehicleOption(
    val emoji:    String,
    val label:    String,
    val maxSeats: Int
)

val vehicleOptions = listOf(
    VehicleOption("🚙", "Sedan / Hatchback",  4),
    VehicleOption("🚐", "SUV / Jeep",         6),
    VehicleOption("🚌", "Tempo Traveller",    12),
)

// ─────────────────────────────────────────────────────────────────────────────
//  POST ROUTE SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PostRouteScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current

    // ── Form State ────────────────────────────────────────────────────────────
    var origin        by remember { mutableStateOf("") }
    var destination   by remember { mutableStateOf("") }
    var selectedDate  by remember { mutableStateOf("") }
    var selectedTime  by remember { mutableStateOf("") }
    var selectedVehicle by remember { mutableStateOf(vehicleOptions[1]) }  // default SUV
    var seats         by remember { mutableIntStateOf(2) }
    var price         by remember { mutableStateOf("") }
    var notes         by remember { mutableStateOf("") }
    var showSuccess   by remember { mutableStateOf(false) }

    // When vehicle changes → clamp seats to new max
    LaunchedEffect(selectedVehicle) {
        if (seats > selectedVehicle.maxSeats) {
            seats = selectedVehicle.maxSeats
        }
    }

    val isFormValid = origin.isNotBlank()
            && destination.isNotBlank()
            && selectedDate.isNotBlank()
            && selectedTime.isNotBlank()
            && price.isNotBlank()

    // ── Date Picker ───────────────────────────────────────────────────────────
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

    // ── Time Picker ───────────────────────────────────────────────────────────
    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val amPm = if (hour < 12) "AM" else "PM"
                val h    = if (hour % 12 == 0) 12 else hour % 12
                selectedTime = "%02d : %02d %s".format(h, minute, amPm)
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
            .background(Pine)
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(listOf(Moss.copy(alpha = 0.12f), Color.Transparent))
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
                    Text(text = "POST A ROUTE", style = EyebrowStyle.copy(fontSize = 10.sp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = "Share Your Journey",
                        style = PahadiRaahTypography.titleLarge.copy(color = Snow)
                    )
                }
            }

            // ── FORM ──────────────────────────────────────────────────────────
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

                // ── Route ─────────────────────────────────────────────────────
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

                // ── Schedule ──────────────────────────────────────────────────
                FormSectionLabel("SCHEDULE")
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                    // DATE — tappable, opens DatePickerDialog
                    DateTimePickerField(
                        value       = selectedDate,
                        label       = "Departure Date",
                        placeholder = "Select Date",
                        emoji       = "📅",
                        modifier    = Modifier.weight(1f),
                        onClick     = { datePickerDialog.show() }
                    )

                    // TIME — tappable, opens TimePickerDialog
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

                // ── Vehicle ───────────────────────────────────────────────────
                FormSectionLabel("VEHICLE TYPE")
                Spacer(modifier = Modifier.height(12.dp))
                VehicleTypeSelector(
                    selected = selectedVehicle,
                    onSelect = { selectedVehicle = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Seats & Price ─────────────────────────────────────────────
                FormSectionLabel("SEATS & PRICING")
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Seat stepper — max driven by vehicle
                    SeatStepper(
                        seats    = seats,
                        maxSeats = selectedVehicle.maxSeats,
                        onIncrease = { if (seats < selectedVehicle.maxSeats) seats++ },
                        onDecrease = { if (seats > 1) seats-- },
                        modifier = Modifier.weight(1f)
                    )
                    // Price
                    PahadiTextField(
                        value         = price,
                        onValueChange = { price = it },
                        label         = "Price / Seat",
                        placeholder   = "₹ 650",
                        leadingEmoji  = "💰",
                        keyboardType  = KeyboardType.Number,
                        modifier      = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Notes ─────────────────────────────────────────────────────
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

                Spacer(modifier = Modifier.height(32.dp))

                // ── Submit ────────────────────────────────────────────────────
                PostRouteButton(
                    enabled = isFormValid,
                    onClick = { showSuccess = true }
                )

                if (!isFormValid) {
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
//  DATE / TIME PICKER FIELD  — read-only tap to open native dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DateTimePickerField(
    value: String,
    label: String,
    placeholder: String,
    emoji: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val borderColor by animateColorAsState(
        if (isPressed) BorderFocus else BorderSubtle,
        tween(150),
        label = "dtBorder"
    )

    Column(modifier = modifier) {
        Text(text = label.uppercase(), style = FormLabelStyle)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(PahadiRaahShapes.small)
                .background(SurfaceLight)
                .border(1.dp, borderColor, PahadiRaahShapes.small)
                .clickable(
                    interactionSource = interactionSource,
                    indication        = null,
                    onClick           = onClick
                )
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Text(
                text     = value.ifBlank { placeholder },
                style    = PahadiRaahTypography.bodyMedium.copy(
                    color    = if (value.isBlank()) Sage.copy(alpha = 0.35f) else Snow,
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

// ─────────────────────────────────────────────────────────────────────────────
//  VEHICLE TYPE SELECTOR — updates maxSeats
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VehicleTypeSelector(
    selected: VehicleOption,
    onSelect: (VehicleOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        vehicleOptions.forEach { vehicle ->
            val isSelected        = selected.label == vehicle.label
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed         by interactionSource.collectIsPressedAsState()
            val bgColor by animateColorAsState(
                if (isSelected) Moss.copy(alpha = 0.14f) else if (isPressed) SurfaceMedium else SurfaceLight,
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
                    .clickable(
                        interactionSource = interactionSource,
                        indication        = null,
                        onClick           = { onSelect(vehicle) }
                    )
                    .padding(16.dp)
            ) {
                Text(text = vehicle.emoji, fontSize = 26.sp)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = vehicle.label,
                        style = PahadiRaahTypography.bodyMedium.copy(
                            color = if (isSelected) Snow else Mist
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // Show max seats as a hint
                    Text(
                        text  = "Up to ${vehicle.maxSeats} seats",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = if (isSelected) Sage else Sage.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    )
                }

                // Custom radio button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected)
                                Brush.verticalGradient(GradientMoss)
                            else
                                Brush.verticalGradient(listOf(SurfaceMedium, SurfaceMedium))
                        )
                        .border(1.5.dp, if (isSelected) Sage else BorderSubtle, CircleShape)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Snow)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SEAT STEPPER — +/- with max from vehicle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SeatStepper(
    seats: Int,
    maxSeats: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animate seat count change
    val seatScale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "seatScale"
    )

    Column(modifier = modifier) {
        Text(text = "AVAILABLE SEATS", style = FormLabelStyle)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PahadiRaahShapes.small)
                .background(SurfaceLight)
                .border(1.dp, BorderSubtle, PahadiRaahShapes.small)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Increase button
            StepperButton(
                icon      = Icons.Default.KeyboardArrowUp,
                enabled   = seats < maxSeats,
                onClick   = onIncrease
            )

            // Seat count
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = seats.toString(),
                    style = PahadiRaahTypography.headlineMedium.copy(
                        color    = Snow,
                        fontSize = 32.sp
                    ),
                    modifier = Modifier.scale(seatScale)
                )
                Text(
                    text  = "of $maxSeats max",
                    style = PahadiRaahTypography.bodySmall.copy(
                        color    = Sage.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                )
            }

            // Decrease button
            StepperButton(
                icon    = Icons.Default.KeyboardArrowDown,
                enabled = seats > 1,
                onClick = onDecrease
            )
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
        if (isPressed && enabled) 0.88f else 1f,
        spring(stiffness = Spring.StiffnessHigh),
        label = "sb"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (enabled)
                    Brush.verticalGradient(listOf(Forest, Moss.copy(alpha = 0.7f)))
                else
                    Brush.verticalGradient(listOf(SurfaceMedium, SurfaceMedium))
            )
            .border(
                1.dp,
                if (enabled) Sage.copy(alpha = 0.3f) else BorderSubtle,
                CircleShape
            )
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
            tint               = if (enabled) Snow else Sage.copy(alpha = 0.25f),
            modifier           = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ROUTE VISUAL CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RouteVisualCard(origin: String, destination: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.large)
            .background(SurfaceLight)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.large)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Sage)
                    .border(3.dp, Sage.copy(alpha = 0.25f), CircleShape)
            )
            Column {
                Text(text = "FROM", style = FormLabelStyle.copy(fontSize = 9.sp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = origin,
                    style = PahadiRaahTypography.bodyMedium.copy(
                        color    = if (origin.contains("begin")) Sage.copy(alpha = 0.35f) else Snow,
                        fontSize = 14.sp
                    ),
                    maxLines = 1
                )
            }
        }

        // Gradient line
        Row(modifier = Modifier.padding(start = 5.dp)) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(28.dp)
                    .background(Brush.verticalGradient(listOf(Sage, Amber)))
            )
        }

        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Gold)
                    .border(3.dp, Gold.copy(alpha = 0.25f), CircleShape)
            )
            Column {
                Text(text = "TO", style = FormLabelStyle.copy(fontSize = 9.sp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = destination,
                    style = PahadiRaahTypography.bodyMedium.copy(
                        color    = if (destination.contains("headed")) Sage.copy(alpha = 0.35f) else Snow,
                        fontSize = 14.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PAHADI TEXT FIELD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PahadiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingEmoji: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (isFocused) BorderFocus else BorderSubtle, tween(200), label = "tfBorder"
    )
    val bgColor by animateColorAsState(
        if (isFocused) Sage.copy(alpha = 0.06f) else SurfaceLight, tween(200), label = "tfBg"
    )

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
            Row(
                verticalAlignment = Alignment.Top,
                modifier          = Modifier.padding(horizontal = 14.dp)
            ) {
                Text(
                    text     = leadingEmoji,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = if (singleLine) 16.dp else 14.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value           = value,
                    onValueChange   = onValueChange,
                    singleLine      = singleLine,
                    minLines        = minLines,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle       = PahadiRaahTypography.bodyMedium.copy(
                        color    = Snow,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .onFocusChanged { isFocused = it.isFocused },
                    decorationBox = { innerTextField ->
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
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FormSectionLabel(label: String) {
    Text(text = label, style = EyebrowStyle.copy(fontSize = 10.sp))
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUBMIT BUTTON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PostRouteButton(enabled: Boolean, onClick: () -> Unit) {
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
                    Brush.horizontalGradient(GradientMoss)
                else
                    Brush.horizontalGradient(
                        listOf(Forest.copy(alpha = 0.5f), Moss.copy(alpha = 0.3f))
                    )
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = enabled,
                onClick           = onClick
            )
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "🗺️", fontSize = 18.sp)
            Text(
                text  = "Post Route",
                style = PahadiRaahTypography.labelLarge.copy(
                    color    = if (enabled) Snow else Sage.copy(alpha = 0.4f),
                    fontSize = 16.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUCCESS OVERLAY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SuccessOverlay(onDone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(400), label = "sAlpha")
    val scale by animateFloatAsState(
        if (visible) 1f else 0.6f,
        tween(500, easing = EaseOutBack),
        label = "sScale"
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
            .background(Pine.copy(alpha = 0.96f))
            .alpha(alpha)
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(scale)
                .background(
                    Brush.radialGradient(listOf(Moss.copy(alpha = 0.22f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.scale(scale)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(GradientMoss))
            ) {
                Text(text = "✓", fontSize = 40.sp, color = Snow)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text      = "Route Posted!",
                style     = PahadiRaahTypography.headlineMedium.copy(color = Snow),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text      = "Your mountain route is now live\nfor passengers to discover",
                style     = PahadiRaahTypography.bodyMedium.copy(
                    color     = Sage,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            LinearProgressIndicator(
                modifier   = Modifier.width(120.dp).clip(PillShape),
                color      = Sage,
                trackColor = SurfaceLight
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text  = "Returning to dashboard...",
                style = PahadiRaahTypography.labelSmall.copy(
                    color         = Sage.copy(alpha = 0.45f),
                    letterSpacing = 1.sp,
                    fontSize      = 10.sp
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PREVIEW
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PostRouteScreenPreview() {
    PahadiRaahTheme {
        PostRouteScreen(onBack = {}, onSuccess = {})
    }
}