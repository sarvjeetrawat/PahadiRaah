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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.pahadiraah.data.model.ActionResult
import com.kunpitech.pahadiraah.ui.theme.*
import com.kunpitech.pahadiraah.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
//  SignUpScreen
//
//  New users:
//    1. Pick role (driver / passenger) — shown as mini cards
//    2. Enter name
//    3. Enter email → sendOtp() → OtpVerifyScreen
//
//  Role & name are stored locally until OTP is verified, then saved to DB
//  via AuthViewModel.setRole().
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SignUpScreen(
    onNavigateToOtp: (email: String, name: String, role: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var selectedRole   by remember { mutableStateOf("passenger") }  // "driver" | "passenger"
    var name           by remember { mutableStateOf("") }
    var email          by remember { mutableStateOf("") }
    var nameFocused    by remember { mutableStateOf(false) }
    var emailFocused   by remember { mutableStateOf(false) }
    var nameError      by remember { mutableStateOf<String?>(null) }
    var emailError     by remember { mutableStateOf<String?>(null) }
    var started        by remember { mutableStateOf(false) }

    val otpResult    by viewModel.otpResult.collectAsStateWithLifecycle()
    val focusManager  = LocalFocusManager.current
    val nameFocus     = remember { FocusRequester() }

    // ── Animations ────────────────────────────────────────────────────────────
    val bgAlpha      by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "bg")
    val backAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(400, delayMillis = 100), label = "back")
    val headerOffset by animateFloatAsState(if (started) 0f else 40f, tween(600, delayMillis = 200, easing = EaseOutCubic), label = "hdrY")
    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 200), label = "hdrA")
    val roleOffset   by animateFloatAsState(if (started) 0f else 40f, tween(600, delayMillis = 350, easing = EaseOutCubic), label = "roleY")
    val roleAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 350), label = "roleA")
    val fieldOffset  by animateFloatAsState(if (started) 0f else 40f, tween(600, delayMillis = 500, easing = EaseOutCubic), label = "fieldY")
    val fieldAlpha   by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 500), label = "fieldA")
    val btnOffset    by animateFloatAsState(if (started) 0f else 40f, tween(600, delayMillis = 650, easing = EaseOutCubic), label = "btnY")
    val btnAlpha     by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 650), label = "btnA")

    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val blobScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(5500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "blob"
    )

    LaunchedEffect(Unit) {
        started = true
        delay(300)
        nameFocus.requestFocus()
    }

    LaunchedEffect(otpResult) {
        when (val result = otpResult) {
            is ActionResult.Success -> {
                viewModel.resetOtpResult()
                onNavigateToOtp(email.trim(), name.trim(), selectedRole)
            }
            is ActionResult.Error -> {
                emailError = result.message
            }
            else -> {}
        }
    }

    fun validateAndSend() {
        focusManager.clearFocus()
        nameError  = null
        emailError = null
        var valid  = true
        if (name.isBlank()) {
            nameError = "Please enter your name"
            valid = false
        }
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            emailError = "Please enter your email"
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            emailError = "Enter a valid email address"
            valid = false
        }
        if (valid) viewModel.sendOtp(trimmed)
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
                    .size(400.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-100).dp, y = (-60).dp)
                    .scale(blobScale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PineMid.copy(alpha = 0.12f), Color.Transparent)
                        ),
                        RoundedCornerShape(50)
                    )
            )
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 80.dp, y = 80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Saffron.copy(alpha = 0.08f), Color.Transparent)
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
            Spacer(modifier = Modifier.height(Dimens.SpaceXL))

            // ── Back ─────────────────────────────────────────────────────────
            BackButton(
                onClick = onNavigateBack,
                modifier = Modifier.alpha(backAlpha)
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceXXL))

            // ── Header ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(headerAlpha)
                    .graphicsLayer { translationY = headerOffset }
            ) {
                Text(text = "NEW TO PAHADIRAAH", style = EyebrowStyle)
                Spacer(modifier = Modifier.height(Dimens.SpaceXS))
                Text(
                    text  = "Begin Your\nMountain Story",
                    style = PahadiRaahTypography.headlineMedium
                )
                Spacer(modifier = Modifier.height(Dimens.SpaceSM))
                Text(
                    text  = "Tell us who you are — you can always change this later",
                    style = PahadiRaahTypography.bodyMedium.copy(color = MistVeil)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceXXL))

            // ── Role selector ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(roleAlpha)
                    .graphicsLayer { translationY = roleOffset }
            ) {
                Text(text = "I AM A", style = FormLabelStyle)
                Spacer(modifier = Modifier.height(Dimens.SpaceSM))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
                ) {
                    RolePill(
                        modifier   = Modifier.weight(1f),
                        emoji      = "🚗",
                        label      = "Driver",
                        isSelected = selectedRole == "driver",
                        accentColor = GlacierTeal,
                        onClick     = { selectedRole = "driver" }
                    )
                    RolePill(
                        modifier   = Modifier.weight(1f),
                        emoji      = "🎒",
                        label      = "Passenger",
                        isSelected = selectedRole == "passenger",
                        accentColor = Marigold,
                        onClick     = { selectedRole = "passenger" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceXL))

            // ── Form fields ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(fieldAlpha)
                    .graphicsLayer { translationY = fieldOffset },
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)
            ) {
                // Name
                AuthTextField(
                    label       = "YOUR NAME",
                    value       = name,
                    onValueChange = {
                        name      = it
                        nameError = null
                    },
                    placeholder   = "e.g. Riya Sharma",
                    error         = nameError,
                    isFocused     = nameFocused,
                    onFocusChange = { nameFocused = it },
                    focusRequester = nameFocus,
                    keyboardType  = KeyboardType.Text,
                    imeAction     = ImeAction.Next
                )

                // Email
                AuthTextField(
                    label       = "EMAIL ADDRESS",
                    value       = email,
                    onValueChange = {
                        email      = it
                        emailError = null
                    },
                    placeholder   = "you@example.com",
                    error         = emailError,
                    isFocused     = emailFocused,
                    onFocusChange = { emailFocused = it },
                    keyboardType  = KeyboardType.Email,
                    imeAction     = ImeAction.Done,
                    onDone        = { validateAndSend() }
                )
            }

            Spacer(modifier = Modifier.height(Dimens.Space3XL))

            // ── CTA Button ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(btnAlpha)
                    .graphicsLayer { translationY = btnOffset }
                    .padding(bottom = Dimens.Space3XL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthPrimaryButton(
                    text      = if (otpResult is ActionResult.Loading) "" else "Continue  →",
                    isLoading = otpResult is ActionResult.Loading,
                    onClick   = { validateAndSend() }
                )

                Spacer(modifier = Modifier.height(Dimens.SpaceLG))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "Already have an account? ",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = MistVeil.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text  = "Sign In",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = GlacierTeal,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNavigateBack
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ROLE PILL — compact toggle chip for driver / passenger
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RolePill(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "pillScale"
    )
    val bgAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 0.15f else 0.05f,
        animationSpec = tween(200),
        label         = "pillBg"
    )
    val borderAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 0.7f else 0.15f,
        animationSpec = tween(200),
        label         = "pillBorder"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = bgAlpha))
            .border(1.dp, accentColor.copy(alpha = borderAlpha), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(Dimens.SpaceXS))
            Text(
                text  = label,
                style = PahadiRaahTypography.titleSmall.copy(
                    color = if (isSelected) accentColor else MistVeil.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpScreenPreview() {
    PahadiRaahTheme {
        SignUpScreen(
            onNavigateToOtp  = { _, _, _ -> },
            onNavigateBack   = {}
        )
    }
}