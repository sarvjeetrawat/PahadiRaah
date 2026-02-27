package com.kunpitech.pahadiraah.ui.screens.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.pahadiraah.data.model.ActionResult
import com.kunpitech.pahadiraah.data.model.NewVehicle
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.data.model.UserDto
import com.kunpitech.pahadiraah.data.model.VehicleDto
import com.kunpitech.pahadiraah.ui.screens.auth.BackButton
import com.kunpitech.pahadiraah.ui.theme.*
import com.kunpitech.pahadiraah.viewmodel.ProfileViewModel
import com.kunpitech.pahadiraah.viewmodel.UserViewModel
import com.kunpitech.pahadiraah.viewmodel.VehicleViewModel

// ─────────────────────────────────────────────────────────────────────────────
//  CONSTANTS
// ─────────────────────────────────────────────────────────────────────────────

private val AvatarEmojis = listOf(
    "🧑", "👩", "👨", "🧔", "👱", "👩‍🦱", "👨‍🦱",
    "👩‍🦳", "👨‍🦳", "🧕", "👲", "🧑‍🦯", "🧑‍🌾", "🧑‍✈️"
)

private val LanguageOptions = listOf(
    "Hindi", "English", "Pahadi", "Punjabi", "Gujarati", "Tamil", "Telugu", "Kannada"
)

private data class VehicleType(val key: String, val emoji: String, val label: String, val defaultSeats: Int)
private val VehicleTypes = listOf(
    VehicleType("sedan",  "🚙", "Sedan",   4),
    VehicleType("suv",    "🚐", "SUV",     6),
    VehicleType("tempo",  "🚌", "Tempo",  12),
    VehicleType("bus",    "🚎", "Bus",    30),
)

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MyProfileScreen(
    onBack:           () -> Unit,
    onSignOut:        () -> Unit,
    userViewModel:    UserViewModel    = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    vehicleViewModel: VehicleViewModel = hiltViewModel()
) {
    val profileState by userViewModel.myProfile.collectAsStateWithLifecycle()
    val vehicleState by vehicleViewModel.myVehicle.collectAsStateWithLifecycle()
    val saveResult   by profileViewModel.saveResult.collectAsStateWithLifecycle()
    val vehicleSave  by vehicleViewModel.saveResult.collectAsStateWithLifecycle()

    var isEditing by remember { mutableStateOf(false) }
    var started   by remember { mutableStateOf(false) }

    // ── Profile edit form state ───────────────────────────────────────────────
    var editName       by remember { mutableStateOf("") }
    var editEmoji      by remember { mutableStateOf("🧑") }
    var editBio        by remember { mutableStateOf("") }
    var editLanguages  by remember { mutableStateOf(setOf<String>()) }
    var editSpeciality by remember { mutableStateOf("") }
    var nameError      by remember { mutableStateOf<String?>(null) }

    // ── Vehicle edit form state (driver only) ─────────────────────────────────
    var editVehicleType  by remember { mutableStateOf("suv") }
    var editVehicleModel by remember { mutableStateOf("") }
    var editRegNumber    by remember { mutableStateOf("") }
    var editSeatCapacity by remember { mutableStateOf(6) }
    var vehicleModelError by remember { mutableStateOf<String?>(null) }
    var regError          by remember { mutableStateOf<String?>(null) }

    // ── Animations ────────────────────────────────────────────────────────────
    val headerAlpha by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "ha")
    val cardOffset  by animateFloatAsState(if (started) 0f else 40f, tween(600, 200, EaseOutCubic), label = "cY")
    val cardAlpha   by animateFloatAsState(if (started) 1f else 0f, tween(600, 200), label = "cA")
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        0.07f, 0.16f,
        infiniteRepeatable(tween(2800, easing = EaseInOutSine), RepeatMode.Reverse), "glow"
    )

    // ── Load data ─────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        userViewModel.loadMyProfile()
        started = true
    }

    // Pre-fill edit form when profile data arrives
    LaunchedEffect(profileState) {
        val d = (profileState as? UiState.Success<UserDto>)?.data ?: return@LaunchedEffect
        if (!isEditing) {
            editName       = d.name
            editEmoji      = d.emoji.ifBlank { "🧑" }
            editBio        = d.bio ?: ""
            editLanguages  = d.languages.toSet()
            editSpeciality = d.speciality ?: ""
            // Load vehicle when profile loads (driver only)
            if (d.role == "driver") vehicleViewModel.loadMyVehicle(d.id)
        }
    }

    // Pre-fill vehicle edit form when vehicle data arrives
    LaunchedEffect(vehicleState) {
        val v = (vehicleState as? UiState.Success<VehicleDto?>)?.data ?: return@LaunchedEffect
        if (v != null && !isEditing) {
            editVehicleType  = v.type
            editVehicleModel = v.model
            editRegNumber    = v.regNumber
            editSeatCapacity = v.seatCapacity
        }
    }

    // Exit edit mode on successful profile save
    LaunchedEffect(saveResult) {
        if (saveResult is ActionResult.Success) {
            profileViewModel.resetSaveResult()
            isEditing = false
            userViewModel.loadMyProfile()
        }
    }

    // Refresh vehicle on vehicle save
    LaunchedEffect(vehicleSave) {
        if (vehicleSave is ActionResult.Success) {
            vehicleViewModel.resetSaveResult()
            val uid = (profileState as? UiState.Success<UserDto>)?.data?.id ?: return@LaunchedEffect
            vehicleViewModel.loadMyVehicle(uid)
        }
    }

    fun validateAndSave(profile: UserDto) {
        nameError         = null
        vehicleModelError = null
        regError          = null
        var ok = true
        if (editName.isBlank()) { nameError = "Name cannot be empty"; ok = false }
        val isDriver = profile.role == "driver"
        if (isDriver) {
            if (editVehicleModel.isBlank()) { vehicleModelError = "Enter vehicle model"; ok = false }
            if (editRegNumber.isBlank())    { regError = "Enter registration number"; ok = false }
        }
        if (!ok) return

        val existingVehicleId = (vehicleState as? UiState.Success<VehicleDto?>)?.data?.id
        profileViewModel.saveProfile(
            name               = editName.trim(),
            emoji              = editEmoji,
            bio                = editBio.trim().ifBlank { null },
            languages          = editLanguages.toList(),
            speciality         = editSpeciality.trim().ifBlank { null },
            isDriver           = isDriver,
            vehicleType        = editVehicleType,
            vehicleModel       = editVehicleModel.trim(),
            regNumber          = editRegNumber.trim().uppercase(),
            seatCapacity       = editSeatCapacity,
            existingVehicleId  = existingVehicleId
        )
    }

    fun resetEditForm(profile: UserDto, vehicle: VehicleDto?) {
        editName         = profile.name
        editEmoji        = profile.emoji.ifBlank { "🧑" }
        editBio          = profile.bio ?: ""
        editLanguages    = profile.languages.toSet()
        editSpeciality   = profile.speciality ?: ""
        nameError        = null
        vehicleModelError = null
        regError          = null
        vehicle?.let {
            editVehicleType  = it.type
            editVehicleModel = it.model
            editRegNumber    = it.regNumber
            editSeatCapacity = it.seatCapacity
        }
        profileViewModel.resetSaveResult()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI ROOT
    // ═════════════════════════════════════════════════════════════════════════
    Box(modifier = Modifier.fillMaxSize().background(Slate)) {

        // Background glow
        Box(
            modifier = Modifier
                .size(440.dp).align(Alignment.TopCenter).offset(y = (-80).dp).alpha(glowAlpha)
                .background(
                    Brush.radialGradient(listOf(PineMid.copy(alpha = 0.35f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize().systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Dimens.SpaceXL))

            // ── Top bar ───────────────────────────────────────────────────────
            val profile = (profileState as? UiState.Success<UserDto>)?.data
            val vehicle = (vehicleState as? UiState.Success<VehicleDto?>)?.data

            Row(
                modifier = Modifier.fillMaxWidth().alpha(headerAlpha),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                BackButton(onClick = if (isEditing) ({ isEditing = false }) else onBack)

                Text(
                    text  = if (isEditing) "EDIT PROFILE" else "MY PROFILE",
                    style = EyebrowStyle
                )

                if (profile != null) {
                    SmallActionButton(
                        label   = if (isEditing) "Cancel" else "Edit",
                        tint    = if (isEditing) StatusError else GlacierTeal,
                        onClick = {
                            if (isEditing) {
                                resetEditForm(profile, vehicle)
                                isEditing = false
                            } else {
                                isEditing = true
                            }
                        }
                    )
                } else {
                    Spacer(Modifier.size(Dimens.BackButtonSize))
                }
            }

            Spacer(Modifier.height(Dimens.SpaceXXL))

            // ── Content ───────────────────────────────────────────────────────
            when (val state = profileState) {
                is UiState.Loading -> {
                    Spacer(Modifier.height(80.dp))
                    CircularProgressIndicator(color = GlacierTeal, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                }
                is UiState.Error -> {
                    Spacer(Modifier.height(60.dp))
                    Text("Couldn't load profile", style = PahadiRaahTypography.bodyMedium.copy(color = StatusError), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(Dimens.SpaceMD))
                    SmallActionButton("Retry", GlacierTeal) { userViewModel.loadMyProfile() }
                }
                else -> {
                    val p = (state as? UiState.Success<UserDto>)?.data
                    val v = (vehicleState as? UiState.Success<VehicleDto?>)?.data

                    val mod = Modifier.alpha(cardAlpha).graphicsLayer { translationY = cardOffset }

                    if (isEditing && p != null) {
                        EditContent(
                            profile            = p,
                            vehicle            = v,
                            editName           = editName,           onNameChange       = { editName = it; nameError = null },        nameError = nameError,
                            editEmoji          = editEmoji,          onEmojiChange      = { editEmoji = it },
                            editBio            = editBio,            onBioChange        = { editBio = it },
                            editLanguages      = editLanguages,      onLanguageToggle   = { lang -> editLanguages = if (lang in editLanguages) editLanguages - lang else editLanguages + lang },
                            editSpeciality     = editSpeciality,     onSpecialityChange = { editSpeciality = it },
                            editVehicleType    = editVehicleType,    onVehicleTypeChange = { t -> editVehicleType = t; editSeatCapacity = VehicleTypes.find { it.key == t }?.defaultSeats ?: 4 },
                            editVehicleModel   = editVehicleModel,   onVehicleModelChange = { editVehicleModel = it; vehicleModelError = null }, vehicleModelError = vehicleModelError,
                            editRegNumber      = editRegNumber,      onRegChange          = { editRegNumber = it.uppercase(); regError = null }, regError = regError,
                            editSeatCapacity   = editSeatCapacity,   onSeatChange         = { editSeatCapacity = it },
                            saveResult         = saveResult,
                            onSave             = { validateAndSave(p) },
                            modifier           = mod
                        )
                    } else {
                        ViewContent(
                            profile  = p,
                            vehicle  = v,
                            modifier = mod
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.Space3XL))

            if (!isEditing) {
                SignOutButton(onClick = onSignOut, modifier = Modifier.alpha(cardAlpha).padding(bottom = Dimens.Space3XL))
            } else {
                Spacer(Modifier.height(Dimens.Space3XL))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  VIEW MODE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ViewContent(
    profile:  UserDto?,
    vehicle:  VehicleDto?,
    modifier: Modifier = Modifier
) {
    val isDriver = profile?.role == "driver"

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

        // ── Avatar ────────────────────────────────────────────────────────────
        AvatarCircle(emoji = profile?.emoji ?: "🧑", isDriver = isDriver, size = 96)

        Spacer(Modifier.height(Dimens.SpaceMD))

        Text(
            text      = profile?.name ?: "—",
            style     = PahadiRaahTypography.headlineSmall.copy(color = SnowPeak),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Dimens.SpaceXS))

        // Role badge
        RoleBadge(isDriver = isDriver)

        // Email
        if (!profile?.email.isNullOrBlank()) {
            Spacer(Modifier.height(Dimens.SpaceXS))
            Text(profile!!.email!!, style = PahadiRaahTypography.bodySmall.copy(color = MistVeil.copy(alpha = 0.5f)))
        }

        // Phone
        if (!profile?.phone.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(profile!!.phone!!, style = PahadiRaahTypography.bodySmall.copy(color = MistVeil.copy(alpha = 0.4f)))
        }

        Spacer(Modifier.height(Dimens.SpaceXXL))

        // ── Driver stats ──────────────────────────────────────────────────────
        if (isDriver) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "🛣️", profile?.totalTrips?.toString() ?: "0", "Trips")
                StatCard(Modifier.weight(1f), "⭐",
                    if ((profile?.avgRating ?: 0.0) > 0) String.format("%.1f", profile!!.avgRating) else "—", "Rating")
                StatCard(Modifier.weight(1f), "📅",
                    if ((profile?.yearsActive ?: 0) > 0) "${profile!!.yearsActive}yr" else "New", "Active")
            }
            Spacer(Modifier.height(Dimens.SpaceXL))
        }

        // ── Info section ──────────────────────────────────────────────────────
        SectionLabel("PERSONAL INFO")
        Spacer(Modifier.height(Dimens.SpaceSM))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoCard("Name",  profile?.name?.ifBlank { "Not set" } ?: "Not set", muted = profile?.name.isNullOrBlank())
            if (!profile?.email.isNullOrBlank())  InfoCard("Email", profile!!.email!!)
            if (!profile?.phone.isNullOrBlank())  InfoCard("Phone", profile!!.phone!!)
            InfoCard("Bio",
                profile?.bio?.ifBlank { null } ?: "No bio added yet",
                muted = profile?.bio.isNullOrBlank()
            )
        }

        // ── Driver-only sections ──────────────────────────────────────────────
        if (isDriver) {
            Spacer(Modifier.height(Dimens.SpaceXL))
            SectionLabel("DRIVER INFO")
            Spacer(Modifier.height(Dimens.SpaceSM))

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoCard("Languages",
                    if (profile?.languages?.isNotEmpty() == true) profile.languages.joinToString(" • ")
                    else "None added",
                    muted = profile?.languages.isNullOrEmpty()
                )
                InfoCard("Route Speciality",
                    profile?.speciality?.ifBlank { null } ?: "None added",
                    muted = profile?.speciality.isNullOrBlank()
                )
                InfoCard("Online Status",
                    if (profile?.isOnline == true) "🟢 Currently Online" else "⚫ Currently Offline"
                )
            }

            Spacer(Modifier.height(Dimens.SpaceXL))
            SectionLabel("VEHICLE")
            Spacer(Modifier.height(Dimens.SpaceSM))

            if (vehicle != null) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val typeInfo = VehicleTypes.find { it.key == vehicle.type }
                    InfoCard("Type",    "${typeInfo?.emoji ?: "🚗"}  ${typeInfo?.label ?: vehicle.type}")
                    InfoCard("Model",   vehicle.model.ifBlank { "Not set" }, muted = vehicle.model.isBlank())
                    InfoCard("Reg No.", vehicle.regNumber.ifBlank { "Not set" }, muted = vehicle.regNumber.isBlank())
                    InfoCard("Seats",   "${vehicle.seatCapacity} seats")
                    InfoCard("Verified", if (vehicle.isVerified) "✅ Verified" else "⏳ Pending verification")
                }
            } else {
                InfoCard("Vehicle", "No vehicle registered yet", muted = true)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  EDIT MODE — PASSENGER layout
//  Fields: avatar, name, bio
//
//  EDIT MODE — DRIVER layout
//  Fields: avatar, name, bio, languages, speciality + vehicle section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditContent(
    profile:             UserDto,
    vehicle:             VehicleDto?,
    editName:            String,  onNameChange:         (String)  -> Unit, nameError: String?,
    editEmoji:           String,  onEmojiChange:        (String)  -> Unit,
    editBio:             String,  onBioChange:          (String)  -> Unit,
    editLanguages:       Set<String>, onLanguageToggle: (String)  -> Unit,
    editSpeciality:      String,  onSpecialityChange:   (String)  -> Unit,
    editVehicleType:     String,  onVehicleTypeChange:  (String)  -> Unit,
    editVehicleModel:    String,  onVehicleModelChange: (String)  -> Unit, vehicleModelError: String?,
    editRegNumber:       String,  onRegChange:          (String)  -> Unit, regError: String?,
    editSeatCapacity:    Int,     onSeatChange:         (Int)     -> Unit,
    saveResult:          ActionResult,
    onSave:              () -> Unit,
    modifier:            Modifier = Modifier
) {
    val isDriver = profile.role == "driver"

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {

        // ══════════════════════════════════════════════════════════════════════
        //  SECTION 1 — PERSONAL
        // ══════════════════════════════════════════════════════════════════════
        SectionLabel("PERSONAL INFO")
        Spacer(Modifier.height(Dimens.SpaceXS))

        // Avatar picker
        EmojiPicker(selectedEmoji = editEmoji, isDriver = isDriver, onEmojiChange = onEmojiChange)

        // Name
        EditField("FULL NAME", editName, onNameChange, "Your full name", nameError, KeyboardType.Text, ImeAction.Next)

        // Bio
        EditTextArea(
            label         = if (isDriver) "ABOUT YOU (OPTIONAL)" else "BIO (OPTIONAL)",
            value         = editBio,
            onValueChange = onBioChange,
            placeholder   = if (isDriver) "e.g. 8 years driving mountain routes" else "e.g. Solo traveller exploring the Himalayas"
        )

        // ══════════════════════════════════════════════════════════════════════
        //  SECTION 2 — DRIVER INFO (driver only)
        // ══════════════════════════════════════════════════════════════════════
        if (isDriver) {
            Spacer(Modifier.height(Dimens.SpaceXS))
            SectionDivider("DRIVER INFO")
            Spacer(Modifier.height(Dimens.SpaceXS))

            // Languages
            Column {
                Text("LANGUAGES SPOKEN", style = FormLabelStyle)
                Spacer(Modifier.height(Dimens.SpaceXS))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguageOptions.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { lang ->
                                val sel = lang in editLanguages
                                SelectableChip(
                                    label      = lang,
                                    isSelected = sel,
                                    accent     = GlacierTeal,
                                    onClick    = { onLanguageToggle(lang) }
                                )
                            }
                        }
                    }
                }
            }

            // Route speciality
            EditField("ROUTE SPECIALITY (OPTIONAL)", editSpeciality, onSpecialityChange,
                "e.g. Spiti Valley, Rohtang Pass", imeAction = ImeAction.Done)

            // ══════════════════════════════════════════════════════════════════
            //  SECTION 3 — VEHICLE (driver only)
            // ══════════════════════════════════════════════════════════════════
            Spacer(Modifier.height(Dimens.SpaceXS))
            SectionDivider("VEHICLE DETAILS")
            Spacer(Modifier.height(Dimens.SpaceXS))

            // Vehicle type chips
            Column {
                Text("VEHICLE TYPE", style = FormLabelStyle)
                Spacer(Modifier.height(Dimens.SpaceXS))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VehicleTypes.forEach { vt ->
                        VehicleTypeChip(
                            modifier   = Modifier.weight(1f),
                            emoji      = vt.emoji,
                            label      = vt.label,
                            isSelected = editVehicleType == vt.key,
                            onClick    = { onVehicleTypeChange(vt.key) }
                        )
                    }
                }
            }

            // Vehicle model
            EditField("VEHICLE MODEL", editVehicleModel, onVehicleModelChange,
                "e.g. Mahindra Thar, Innova", vehicleModelError, KeyboardType.Text, ImeAction.Next)

            // Registration number
            EditField("REGISTRATION NUMBER", editRegNumber, onRegChange,
                "e.g. HP-12-AB-1234", regError, KeyboardType.Text, ImeAction.Next)

            // Seat capacity counter
            Column {
                Text("TOTAL SEATS", style = FormLabelStyle)
                Spacer(Modifier.height(Dimens.SpaceXS))
                SeatCounter(
                    value    = editSeatCapacity,
                    maxValue = when (editVehicleType) { "sedan" -> 4; "suv" -> 8; "tempo" -> 16; else -> 50 },
                    onChanged = onSeatChange
                )
            }

            // Note about verification
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Amber.copy(alpha = 0.06f))
                    .border(1.dp, Amber.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text  = "⚠️  Registration number changes will require re-verification",
                    style = PahadiRaahTypography.bodySmall.copy(color = Amber.copy(alpha = 0.8f), fontSize = 11.sp)
                )
            }
        }

        Spacer(Modifier.height(Dimens.SpaceLG))

        // Error
        if (saveResult is ActionResult.Error) {
            Text(
                (saveResult as ActionResult.Error).message,
                style     = PahadiRaahTypography.labelMedium.copy(color = StatusError),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Dimens.SpaceSM))
        }

        // Save button
        SaveButton(isLoading = saveResult is ActionResult.Loading, onClick = onSave)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SMALL REUSABLE COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AvatarCircle(emoji: String, isDriver: Boolean, size: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(
                if (isDriver) listOf(PineDeep, PineMid) else listOf(StoneWarm, DustTaupe)
            ))
            .border(2.dp,
                if (isDriver) GlacierTeal.copy(alpha = 0.5f) else Saffron.copy(alpha = 0.5f),
                CircleShape)
    ) {
        Text(emoji, fontSize = (size * 0.46f).sp)
    }
}

@Composable
private fun RoleBadge(isDriver: Boolean) {
    val accent = if (isDriver) GlacierTeal else Saffron
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.35f), PillShape)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            if (isDriver) "🚗  Driver" else "🎒  Passenger",
            style = PahadiRaahTypography.labelMedium.copy(color = accent)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text,
        style    = EyebrowStyle.copy(color = MistVeil.copy(alpha = 0.5f), letterSpacing = 2.sp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SectionDivider(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, BorderSubtle))))
        Text("  $label  ", style = FormLabelStyle.copy(color = MistVeil.copy(alpha = 0.5f)))
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(BorderSubtle, Color.Transparent))))
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, emoji: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(PahadiRaahShapes.medium).background(SurfaceLow)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.medium)
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, style = PahadiRaahTypography.headlineSmall.copy(color = SnowPeak, fontSize = 20.sp), textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(label, style = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun InfoCard(label: String, value: String, muted: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(PahadiRaahShapes.medium).background(SurfaceLow)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.medium).padding(14.dp)
    ) {
        Text(label.uppercase(), style = FormLabelStyle.copy(color = Sage.copy(alpha = 0.65f)))
        Spacer(Modifier.height(5.dp))
        Text(
            value,
            style    = PahadiRaahTypography.bodyMedium.copy(color = if (muted) MistVeil.copy(alpha = 0.3f) else SnowPeak),
            maxLines = 5, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmojiPicker(selectedEmoji: String, isDriver: Boolean, onEmojiChange: (String) -> Unit) {
    val accent = if (isDriver) GlacierTeal else Marigold
    Column {
        Text("AVATAR", style = FormLabelStyle)
        Spacer(Modifier.height(Dimens.SpaceSM))

        // Big preview
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            ) { Text(selectedEmoji, fontSize = 32.sp) }
        }

        Spacer(Modifier.height(Dimens.SpaceSM))

        AvatarEmojis.chunked(7).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { emoji ->
                    val sel = emoji == selectedEmoji
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f).aspectRatio(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (sel) accent.copy(alpha = 0.15f) else SurfaceGhost)
                            .border(if (sel) 1.dp else 0.dp,
                                if (sel) accent.copy(alpha = 0.6f) else Color.Transparent,
                                RoundedCornerShape(9.dp))
                            .clickable(remember { MutableInteractionSource() }, null) { onEmojiChange(emoji) }
                    ) { Text(emoji, fontSize = 20.sp) }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
private fun SelectableChip(label: String, isSelected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(PillShape)
            .background(if (isSelected) accent.copy(alpha = 0.14f) else SurfaceGhost)
            .border(1.dp, if (isSelected) accent.copy(alpha = 0.5f) else BorderSubtle, PillShape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, style = PahadiRaahTypography.labelMedium.copy(
            color = if (isSelected) accent else MistVeil
        ))
    }
}

@Composable
private fun VehicleTypeChip(modifier: Modifier = Modifier, emoji: String, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, spring(Spring.StiffnessMedium), label = "vtc")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) GlacierTeal.copy(alpha = 0.12f) else SurfaceGhost)
            .border(1.dp, if (isSelected) GlacierTeal.copy(alpha = 0.5f) else BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(interactionSource, null, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, style = PahadiRaahTypography.labelSmall.copy(
            color = if (isSelected) GlacierTeal else MistVeil, letterSpacing = 0.sp, fontSize = 10.sp
        ), textAlign = TextAlign.Center)
    }
}

@Composable
private fun SeatCounter(value: Int, maxValue: Int, onChanged: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
        CounterBtn("−", value > 1)           { onChanged(value - 1) }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = 64.dp, height = 44.dp)
                .clip(RoundedCornerShape(10.dp)).background(SurfaceLow).border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
        ) { Text(value.toString(), style = PahadiRaahTypography.titleMedium.copy(color = SnowPeak)) }
        CounterBtn("+", value < maxValue) { onChanged(value + 1) }
        Text("seats", style = PahadiRaahTypography.bodyMedium.copy(color = MistVeil))
    }
}

@Composable
private fun CounterBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.91f else 1f, spring(Spring.StiffnessMedium), label = "cb")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(38.dp).scale(scale)
            .clip(RoundedCornerShape(9.dp))
            .background(if (enabled) SurfaceLow else SurfaceGhost)
            .border(1.dp, if (enabled) BorderSubtle else BorderGhost, RoundedCornerShape(9.dp))
            .clickable(interactionSource, null, enabled = enabled, onClick = onClick)
    ) {
        Text(label, style = PahadiRaahTypography.titleMedium.copy(
            color = if (enabled) SnowPeak else MistVeil.copy(alpha = 0.25f)
        ))
    }
}

@Composable
private fun EditField(
    label: String, value: String, onValueChange: (String) -> Unit,
    placeholder: String = "", error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text, imeAction: ImeAction = ImeAction.Next
) {
    Column {
        Text(label, style = FormLabelStyle)
        Spacer(Modifier.height(Dimens.SpaceXS))
        Box(
            modifier = Modifier.fillMaxWidth().height(Dimens.InputHeight)
                .clip(RoundedCornerShape(Dimens.InputCorner))
                .background(SurfaceLow)
                .border(1.dp, if (error != null) StatusError else BorderSubtle, RoundedCornerShape(Dimens.InputCorner))
                .padding(horizontal = Dimens.SpaceMD),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) Text(placeholder, style = PahadiRaahTypography.bodyLarge.copy(color = MistVeil.copy(alpha = 0.35f)))
            BasicTextField(
                value = value, onValueChange = onValueChange,
                textStyle     = PahadiRaahTypography.bodyLarge.copy(color = SnowPeak),
                cursorBrush   = SolidColor(GlacierTeal), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (error != null) {
            Spacer(Modifier.height(Dimens.SpaceXXS))
            Text(error, style = PahadiRaahTypography.labelMedium.copy(color = StatusError))
        }
    }
}

@Composable
private fun EditTextArea(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String = "") {
    Column {
        Text(label, style = FormLabelStyle)
        Spacer(Modifier.height(Dimens.SpaceXS))
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                .clip(RoundedCornerShape(Dimens.InputCorner)).background(SurfaceLow)
                .border(1.dp, BorderSubtle, RoundedCornerShape(Dimens.InputCorner)).padding(Dimens.SpaceMD)
        ) {
            if (value.isEmpty()) Text(placeholder, style = PahadiRaahTypography.bodyMedium.copy(color = MistVeil.copy(alpha = 0.35f)))
            BasicTextField(
                value = value, onValueChange = { if (it.length <= 200) onValueChange(it) },
                textStyle     = PahadiRaahTypography.bodyMedium.copy(color = SnowPeak),
                cursorBrush   = SolidColor(GlacierTeal),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text("${value.length}/200",
            style    = PahadiRaahTypography.labelSmall.copy(color = MistVeil.copy(alpha = 0.3f), letterSpacing = 0.sp),
            modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
        )
    }
}

@Composable
private fun SmallActionButton(label: String, tint: Color = GlacierTeal, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, spring(Spring.StiffnessMedium), label = "sab")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(Dimens.BackButtonSize).scale(scale)
            .clip(RoundedCornerShape(11.dp)).background(SurfaceLow)
            .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(11.dp))
            .clickable(interactionSource, null, onClick = onClick)
    ) {
        Text(label, style = PahadiRaahTypography.labelMedium.copy(color = tint, fontSize = 11.sp))
    }
}

@Composable
private fun SaveButton(isLoading: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && !isLoading) 0.97f else 1f, spring(Spring.StiffnessMedium), label = "svb")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().height(Dimens.ButtonHeight).scale(scale).clip(PillShape)
            .background(Brush.horizontalGradient(listOf(GlacierTeal, PineMid)))
            .clickable(interactionSource, null, enabled = !isLoading, onClick = onClick)
    ) {
        if (isLoading) CircularProgressIndicator(color = SnowPeak, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        else Text("Save Changes", style = PahadiRaahTypography.labelLarge.copy(color = SnowPeak))
    }
}

@Composable
private fun SignOutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, spring(Spring.StiffnessMedium), label = "sob")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().height(Dimens.ButtonHeight).scale(scale).clip(PillShape)
            .background(StatusError.copy(alpha = 0.1f)).border(1.dp, StatusError.copy(alpha = 0.35f), PillShape)
            .clickable(interactionSource, null, onClick = onClick)
    ) {
        Text("Sign Out", style = PahadiRaahTypography.labelLarge.copy(color = StatusError))
    }
}