package com.kunpitech.pahadiraah.ui.screens.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
//  SignInScreen
//
//  Returning users enter their email → OTP is sent → navigate to OtpVerifyScreen
//  This screen does NOT ask for role (loaded from DB after OTP verify).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SignInScreen(
    onNavigateToOtp:        (email: String) -> Unit,
    onGoogleSignInSuccess:  (isNewUser: Boolean) -> Unit,
    onNavigateBack:         () -> Unit,
    viewModel:              AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var emailFocused by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var started by remember { mutableStateOf(false) }

    val otpResult by viewModel.otpResult.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val context       = androidx.compose.ui.platform.LocalContext.current

    val googleResult by viewModel.googleResult.collectAsStateWithLifecycle()
    val isNewGoogleUser by viewModel.isNewGoogleUser.collectAsStateWithLifecycle()

    // ── Entrance animations ───────────────────────────────────────────────────
    val bgAlpha      by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "bg")
    val backAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(400, delayMillis = 100), label = "back")
    val headerOffset by animateFloatAsState(if (started) 0f else 40f, tween(600, delayMillis = 200, easing = EaseOutCubic), label = "hdrY")
    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 200), label = "hdrA")
    val fieldOffset  by animateFloatAsState(if (started) 0f else 40f, tween(600, delayMillis = 400, easing = EaseOutCubic), label = "fieldY")
    val fieldAlpha   by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 400), label = "fieldA")
    val btnOffset    by animateFloatAsState(if (started) 0f else 40f, tween(600, delayMillis = 550, easing = EaseOutCubic), label = "btnY")
    val btnAlpha     by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 550), label = "btnA")

    // Ambient blobs
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val blobScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(5000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "blob"
    )

    // ── Side effects ──────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        started = true
        delay(200)
        focusRequester.requestFocus()
    }

    LaunchedEffect(otpResult) {
        when (val result = otpResult) {
            is ActionResult.Success -> {
                viewModel.resetOtpResult()
                onNavigateToOtp(email.trim())
            }
            is ActionResult.Error -> {
                emailError = result.message
            }
            else -> {}
        }
    }

    LaunchedEffect(googleResult) {
        when (val result = googleResult) {
            is ActionResult.Success -> {
                viewModel.resetGoogleResult()
                onGoogleSignInSuccess(isNewGoogleUser)
            }
            is ActionResult.Error -> {
                emailError = result.message
            }
            else -> {}
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────
    fun validateAndSend() {
        focusManager.clearFocus()
        emailError = null
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            emailError = "Please enter your email"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            emailError = "Enter a valid email address"
            return
        }
        viewModel.sendOtp(trimmed)
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI
    // ═════════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate)
    ) {
        // ── Ambient blob ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(bgAlpha)
        ) {
            Box(
                modifier = Modifier
                    .size(380.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 120.dp, y = (-80).dp)
                    .scale(blobScale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GlacierTeal.copy(alpha = 0.08f), Color.Transparent)
                        ),
                        RoundedCornerShape(50)
                    )
            )
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-80).dp, y = 60.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PineMid.copy(alpha = 0.12f), Color.Transparent)
                        ),
                        RoundedCornerShape(50)
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = Dimens.ScreenPaddingHorizontal),
        ) {
            Spacer(modifier = Modifier.height(Dimens.SpaceXL))

            // ── Back button ──────────────────────────────────────────────────
            BackButton(
                onClick = onNavigateBack,
                modifier = Modifier.alpha(backAlpha)
            )

            Spacer(modifier = Modifier.height(Dimens.Space3XL))

            // ── Header ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(headerAlpha)
                    .graphicsLayer { translationY = headerOffset }
            ) {
                Text(
                    text  = "WELCOME BACK",
                    style = EyebrowStyle
                )
                Spacer(modifier = Modifier.height(Dimens.SpaceXS))
                Text(
                    text  = "Sign In to\nYour Journey",
                    style = PahadiRaahTypography.headlineMedium
                )
                Spacer(modifier = Modifier.height(Dimens.SpaceSM))
                Text(
                    text  = "We'll send a one-time code to your email",
                    style = PahadiRaahTypography.bodyMedium.copy(color = MistVeil)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.Space4XL))

            // ── Email field ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(fieldAlpha)
                    .graphicsLayer { translationY = fieldOffset }
            ) {
                Text(
                    text  = "EMAIL ADDRESS",
                    style = FormLabelStyle
                )
                Spacer(modifier = Modifier.height(Dimens.SpaceXS))

                val borderColor by animateColorAsState(
                    targetValue = when {
                        emailError != null -> StatusError
                        emailFocused       -> GlacierTeal
                        else               -> BorderSubtle
                    },
                    animationSpec = tween(200),
                    label = "emailBorder"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.InputHeight)
                        .clip(RoundedCornerShape(Dimens.InputCorner))
                        .background(SurfaceLow)
                        .border(1.dp, borderColor, RoundedCornerShape(Dimens.InputCorner))
                        .padding(horizontal = Dimens.SpaceMD),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (email.isEmpty()) {
                        Text(
                            text  = "you@example.com",
                            style = PahadiRaahTypography.bodyLarge.copy(
                                color = MistVeil.copy(alpha = 0.4f)
                            )
                        )
                    }
                    BasicTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
                        textStyle = PahadiRaahTypography.bodyLarge.copy(color = SnowPeak),
                        cursorBrush = SolidColor(GlacierTeal),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { validateAndSend() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { emailFocused = it.isFocused }
                    )
                }

                // Error message
                if (emailError != null) {
                    Spacer(modifier = Modifier.height(Dimens.SpaceXXS))
                    Text(
                        text  = emailError!!,
                        style = PahadiRaahTypography.labelMedium.copy(color = StatusError),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Send OTP button ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(btnAlpha)
                    .graphicsLayer { translationY = btnOffset }
                    .padding(bottom = Dimens.Space3XL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthPrimaryButton(
                    text      = if (otpResult is ActionResult.Loading) "" else "Send Code  →",
                    isLoading = otpResult is ActionResult.Loading,
                    onClick   = { validateAndSend() }
                )

                Spacer(modifier = Modifier.height(Dimens.SpaceLG))

                // ── Or divider ────────────────────────────────────────────────
                AuthOrDivider()

                Spacer(modifier = Modifier.height(Dimens.SpaceLG))

                // ── Google Sign-In button ─────────────────────────────────────
                GoogleSignInButton(
                    isLoading = googleResult is ActionResult.Loading,
                    onClick   = { viewModel.signInWithGoogle(context) }
                )

                Spacer(modifier = Modifier.height(Dimens.SpaceLG))

                Text(
                    text      = "New here? Use \"Begin Your Journey\" on the start screen",
                    style     = PahadiRaahTypography.bodySmall.copy(
                        color     = MistVeil.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        fontSize  = 11.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}