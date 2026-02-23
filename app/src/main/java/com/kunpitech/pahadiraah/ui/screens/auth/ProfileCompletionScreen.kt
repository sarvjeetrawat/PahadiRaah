package com.kunpitech.pahadiraah.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.pahadiraah.data.model.ActionResult
import com.kunpitech.pahadiraah.ui.theme.*
import com.kunpitech.pahadiraah.viewmodel.ProfileViewModel

// ─────────────────────────────────────────────────────────────────────────────
//  ProfileCompletionScreen
//
//  Shown once after OTP verify for new users.
//  Collects:
//    All users  → name, emoji
//    Drivers    → bio, languages, speciality + vehicle details
//    Passengers → bio (optional)
//
//  On save → navigate to dashboard (back stack cleared so they can't return)
// ─────────────────────────────────────────────────────────────────────────────

// Available emoji avatars
private val avatarEmojis = listOf(
    "🧑", "👩", "👨", "🧔", "👱", "👩‍🦱", "👨‍🦱",
    "👩‍🦳", "👨‍🦳", "🧕", "👲", "🧑‍🦯", "🧑‍🌾", "🧑‍✈️"
)

private val vehicleTypes = listOf(
    Triple("sedan",  "🚙", "Sedan / Hatchback"),
    Triple("suv",    "🚐", "SUV / Jeep"),
    Triple("tempo",  "🚌", "Tempo Traveller"),
    Triple("bus",    "🚎", "Bus"),
)

private val languageOptions = listOf(
    "Hindi", "English", "Pahadi", "Punjabi", "Gujarati", "Tamil", "Telugu", "Kannada"
)

@Composable
fun ProfileCompletionScreen(
    role:          String,   // "driver" | "passenger"
    onComplete:    () -> Unit,
    viewModel:     ProfileViewModel = hiltViewModel()
) {
    // ── Form state ────────────────────────────────────────────────────────────
    var selectedEmoji     by remember { mutableStateOf("🧑") }
    var name              by remember { mutableStateOf("") }
    var bio               by remember { mutableStateOf("") }
    var selectedLanguages by remember { mutableStateOf(setOf<String>()) }
    var speciality        by remember { mutableStateOf("") }

    // Driver vehicle fields
    var vehicleType       by remember { mutableStateOf("suv") }
    var vehicleModel      by remember { mutableStateOf("") }
    var regNumber         by remember { mutableStateOf("") }
    var seatCapacity      by remember { mutableStateOf("4") }

    // Validation errors
    var nameError         by remember { mutableStateOf<String?>(null) }
    var vehicleModelError by remember { mutableStateOf<String?>(null) }
    var regError          by remember { mutableStateOf<String?>(null) }

    var started           by remember { mutableStateOf(false) }
    val saveResult        by viewModel.saveResult.collectAsStateWithLifecycle()
    val isDriver          = role == "driver"

    // ── Animations ────────────────────────────────────────────────────────────
    val bgAlpha      by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "bg")
    val headerOffset by animateFloatAsState(if (started) 0f else 30f, tween(600, delayMillis = 150, easing = EaseOutCubic), label = "hdrY")
    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 150), label = "hdrA")
    val emojiOffset  by animateFloatAsState(if (started) 0f else 30f, tween(600, delayMillis = 300, easing = EaseOutCubic), label = "emojiY")
    val emojiAlpha   by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 300), label = "emojiA")
    val formOffset   by animateFloatAsState(if (started) 0f else 30f, tween(600, delayMillis = 450, easing = EaseOutCubic), label = "formY")
    val formAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 450), label = "formA")
    val btnOffset    by animateFloatAsState(if (started) 0f else 30f, tween(600, delayMillis = 600, easing = EaseOutCubic), label = "btnY")
    val btnAlpha     by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 600), label = "btnA")

    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val blobScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(6000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "blob"
    )

    LaunchedEffect(Unit) { started = true }

    LaunchedEffect(saveResult) {
        if (saveResult is ActionResult.Success) {
            viewModel.resetSaveResult()
            onComplete()
        }
    }

    fun validate(): Boolean {
        nameError         = null
        vehicleModelError = null
        regError          = null
        var ok = true
        if (name.isBlank()) { nameError = "Please enter your name"; ok = false }
        if (isDriver) {
            if (vehicleModel.isBlank()) { vehicleModelError = "Enter your vehicle model"; ok = false }
            if (regNumber.isBlank())    { regError = "Enter registration number"; ok = false }
        }
        return ok
    }

    fun onSave() {
        if (!validate()) return
        val seats = seatCapacity.toIntOrNull()?.coerceIn(1, 50) ?: 4
        viewModel.saveProfile(
            name        = name.trim(),
            emoji       = selectedEmoji,
            bio         = bio.trim().ifBlank { null },
            languages   = selectedLanguages.toList(),
            speciality  = speciality.trim().ifBlank { null },
            isDriver    = isDriver,
            vehicleType  = vehicleType,
            vehicleModel = vehicleModel.trim(),
            regNumber    = regNumber.trim().uppercase(),
            seatCapacity = seats
        )
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI
    // ═════════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate)
    ) {
        // ── Ambient blobs ─────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize().alpha(bgAlpha)) {
            Box(
                modifier = Modifier
                    .size(360.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-60).dp)
                    .scale(blobScale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isDriver) GlacierTeal.copy(alpha = 0.09f)
                                else          Marigold.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        RoundedCornerShape(50)
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPaddingHorizontal)
        ) {
            Spacer(modifier = Modifier.height(Dimens.Space3XL))

            // ── Header ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(headerAlpha)
                    .graphicsLayer { translationY = headerOffset }
            ) {
                Text(
                    text  = if (isDriver) "DRIVER PROFILE" else "YOUR PROFILE",
                    style = EyebrowStyle
                )
                Spacer(modifier = Modifier.height(Dimens.SpaceXS))
                Text(
                    text  = if (isDriver) "Set Up Your\nDriver Identity"
                    else          "Complete Your\nProfile",
                    style = PahadiRaahTypography.headlineMedium
                )
                Spacer(modifier = Modifier.height(Dimens.SpaceXS))
                Text(
                    text  = if (isDriver)
                        "Passengers will see this — make a great first impression"
                    else
                        "Just a few details to personalise your experience",
                    style = PahadiRaahTypography.bodyMedium.copy(color = MistVeil)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceXXL))

            // ── Emoji avatar picker ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(emojiAlpha)
                    .graphicsLayer { translationY = emojiOffset }
            ) {
                Text(text = "CHOOSE YOUR AVATAR", style = FormLabelStyle)
                Spacer(modifier = Modifier.height(Dimens.SpaceSM))

                // Current selection — large display
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.SpaceSM)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        if (isDriver) PineMid.copy(alpha = 0.3f)
                                        else          Saffron.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                if (isDriver) GlacierTeal.copy(alpha = 0.4f)
                                else          Marigold.copy(alpha = 0.4f),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        Text(text = selectedEmoji, fontSize = 36.sp)
                    }
                }

                // Emoji grid
                val rows = avatarEmojis.chunked(7)
                rows.forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { emoji ->
                            val isSelected = emoji == selectedEmoji
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected)
                                            (if (isDriver) GlacierTeal else Marigold).copy(alpha = 0.15f)
                                        else SurfaceGhost
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected)
                                            (if (isDriver) GlacierTeal else Marigold).copy(alpha = 0.6f)
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedEmoji = emoji }
                            ) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                        // Fill remaining spots in last row
                        repeat(7 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceXL))

            // ── Form fields ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(formAlpha)
                    .graphicsLayer { translationY = formOffset },
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
            ) {

                // Name
                AuthTextField(
                    label         = "FULL NAME",
                    value         = name,
                    onValueChange = { name = it; nameError = null },
                    placeholder   = "e.g. Riya Sharma",
                    error         = nameError,
                    keyboardType  = KeyboardType.Text,
                    imeAction     = ImeAction.Next
                )

                // Bio
                ProfileTextArea(
                    label         = if (isDriver) "ABOUT YOU (OPTIONAL)" else "BIO (OPTIONAL)",
                    value         = bio,
                    onValueChange = { bio = it },
                    placeholder   = if (isDriver)
                        "e.g. 8 years driving mountain routes, love sharing the journey"
                    else
                        "e.g. Solo traveller exploring the Himalayas"
                )

                // Driver-only fields
                if (isDriver) {

                    // Languages
                    Column {
                        Text(text = "LANGUAGES SPOKEN", style = FormLabelStyle)
                        Spacer(modifier = Modifier.height(Dimens.SpaceXS))
                        LanguageChips(
                            options   = languageOptions,
                            selected  = selectedLanguages,
                            onToggle  = { lang ->
                                selectedLanguages = if (lang in selectedLanguages)
                                    selectedLanguages - lang
                                else
                                    selectedLanguages + lang
                            }
                        )
                    }

                    // Speciality
                    AuthTextField(
                        label         = "ROUTE SPECIALITY (OPTIONAL)",
                        value         = speciality,
                        onValueChange = { speciality = it },
                        placeholder   = "e.g. Spiti Valley, Rohtang Pass",
                        keyboardType  = KeyboardType.Text,
                        imeAction     = ImeAction.Next
                    )

                    // ── Vehicle section ───────────────────────────────────────
                    SectionDivider(label = "VEHICLE DETAILS")

                    // Vehicle type selector
                    Column {
                        Text(text = "VEHICLE TYPE", style = FormLabelStyle)
                        Spacer(modifier = Modifier.height(Dimens.SpaceXS))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            vehicleTypes.forEach { (type, emoji, label) ->
                                VehicleTypeChip(
                                    modifier   = Modifier.weight(1f),
                                    emoji      = emoji,
                                    label      = label.split(" ").first(), // short label
                                    isSelected = vehicleType == type,
                                    onClick    = {
                                        vehicleType  = type
                                        // reset seat capacity to sensible default
                                        seatCapacity = when (type) {
                                            "sedan" -> "4"
                                            "suv"   -> "6"
                                            "tempo" -> "12"
                                            "bus"   -> "30"
                                            else    -> "4"
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Vehicle model
                    AuthTextField(
                        label         = "VEHICLE MODEL",
                        value         = vehicleModel,
                        onValueChange = { vehicleModel = it; vehicleModelError = null },
                        placeholder   = "e.g. Mahindra Thar, Innova Crysta",
                        error         = vehicleModelError,
                        keyboardType  = KeyboardType.Text,
                        imeAction     = ImeAction.Next
                    )

                    // Registration number
                    AuthTextField(
                        label         = "REGISTRATION NUMBER",
                        value         = regNumber,
                        onValueChange = { regNumber = it.uppercase(); regError = null },
                        placeholder   = "e.g. HP-12-AB-1234",
                        error         = regError,
                        keyboardType  = KeyboardType.Text,
                        imeAction     = ImeAction.Next
                    )

                    // Seat capacity
                    Column {
                        Text(text = "TOTAL SEATS", style = FormLabelStyle)
                        Spacer(modifier = Modifier.height(Dimens.SpaceXS))
                        SeatCounter(
                            value     = seatCapacity.toIntOrNull() ?: 4,
                            maxValue  = when (vehicleType) {
                                "sedan" -> 4; "suv" -> 8; "tempo" -> 16; else -> 50
                            },
                            onChanged = { seatCapacity = it.toString() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Space3XL))

            // ── Save button ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(btnAlpha)
                    .graphicsLayer { translationY = btnOffset }
                    .padding(bottom = Dimens.Space3XL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error from VM
                if (saveResult is ActionResult.Error) {
                    Text(
                        text      = (saveResult as ActionResult.Error).message,
                        style     = PahadiRaahTypography.labelMedium.copy(color = StatusError),
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(bottom = Dimens.SpaceSM)
                    )
                }

                AuthPrimaryButton(
                    text      = if (saveResult is ActionResult.Loading) "" else "Save & Continue  →",
                    isLoading = saveResult is ActionResult.Loading,
                    onClick   = { onSave() }
                )

                Spacer(modifier = Modifier.height(Dimens.SpaceMD))

                Text(
                    text      = "You can update these details anytime from your profile",
                    style     = PahadiRaahTypography.bodySmall.copy(
                        color     = MistVeil.copy(alpha = 0.4f),
                        fontSize  = 11.sp,
                        textAlign = TextAlign.Center
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUPPORTING COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileTextArea(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    Column {
        Text(text = label, style = FormLabelStyle)
        Spacer(modifier = Modifier.height(Dimens.SpaceXS))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .clip(RoundedCornerShape(Dimens.InputCorner))
                .background(SurfaceLow)
                .border(1.dp, BorderSubtle, RoundedCornerShape(Dimens.InputCorner))
                .padding(Dimens.SpaceMD)
        ) {
            if (value.isEmpty()) {
                Text(
                    text  = placeholder,
                    style = PahadiRaahTypography.bodyMedium.copy(
                        color = MistVeil.copy(alpha = 0.35f)
                    )
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value         = value,
                onValueChange = { if (it.length <= 200) onValueChange(it) },
                textStyle     = PahadiRaahTypography.bodyMedium.copy(color = SnowPeak),
                cursorBrush   = androidx.compose.ui.graphics.SolidColor(GlacierTeal),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Default
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text      = "${value.length}/200",
            style     = PahadiRaahTypography.labelSmall.copy(
                color         = MistVeil.copy(alpha = 0.3f),
                letterSpacing = 0.sp
            ),
            modifier  = Modifier
                .align(Alignment.End)
                .padding(top = 2.dp)
        )
    }
}

@Composable
private fun LanguageChips(
    options:  List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    val rows = options.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { lang ->
                    val isSelected = lang in selected
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(PillShape)
                            .background(
                                if (isSelected) GlacierTeal.copy(alpha = 0.15f)
                                else SurfaceGhost
                            )
                            .border(
                                1.dp,
                                if (isSelected) GlacierTeal.copy(alpha = 0.5f)
                                else BorderSubtle,
                                PillShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onToggle(lang) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text  = lang,
                            style = PahadiRaahTypography.labelMedium.copy(
                                color = if (isSelected) GlacierTeal else MistVeil
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleTypeChip(
    modifier:   Modifier = Modifier,
    emoji:      String,
    label:      String,
    isSelected: Boolean,
    onClick:    () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "chipScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) GlacierTeal.copy(alpha = 0.12f)
                else SurfaceGhost
            )
            .border(
                1.dp,
                if (isSelected) GlacierTeal.copy(alpha = 0.5f)
                else BorderSubtle,
                RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text      = label,
            style     = PahadiRaahTypography.labelSmall.copy(
                color         = if (isSelected) GlacierTeal else MistVeil,
                letterSpacing = 0.sp,
                fontSize      = 10.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SeatCounter(
    value:    Int,
    maxValue: Int,
    onChanged: (Int) -> Unit
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
    ) {
        // Minus
        CounterButton(
            label   = "−",
            enabled = value > 1,
            onClick = { onChanged(value - 1) }
        )

        // Value display
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 72.dp, height = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceLow)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
        ) {
            Text(
                text  = value.toString(),
                style = PahadiRaahTypography.titleMedium.copy(color = SnowPeak)
            )
        }

        // Plus
        CounterButton(
            label   = "+",
            enabled = value < maxValue,
            onClick = { onChanged(value + 1) }
        )

        Text(
            text  = "seats",
            style = PahadiRaahTypography.bodyMedium.copy(color = MistVeil)
        )
    }
}

@Composable
private fun CounterButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.92f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "counterBtnScale"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (enabled) SurfaceLow else SurfaceGhost
            )
            .border(1.dp, if (enabled) BorderSubtle else BorderGhost, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = enabled,
                onClick           = onClick
            )
    ) {
        Text(
            text  = label,
            style = PahadiRaahTypography.titleMedium.copy(
                color = if (enabled) SnowPeak else MistVeil.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SectionDivider(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, BorderSubtle))
                )
        )
        Text(
            text     = "  $label  ",
            style    = FormLabelStyle.copy(color = MistVeil.copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(BorderSubtle, Color.Transparent))
                )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfileCompletionDriverPreview() {
    PahadiRaahTheme {
        ProfileCompletionScreen(role = "driver", onComplete = {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfileCompletionPassengerPreview() {
    PahadiRaahTheme {
        ProfileCompletionScreen(role = "passenger", onComplete = {})
    }
}