package com.kunpitech.pahadiraah.ui.screens.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
//  OtpVerifyScreen
//
//  Shows 6 individual digit boxes. Calls verifyOtp() on completion.
//
//  Parameters from nav:
//    email      — used to verify + to re-send if needed
//    name       — "" for sign-in, non-empty for sign-up
//    role       — "" for sign-in (loaded from DB), "driver"/"passenger" for sign-up
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OtpVerifyScreen(
    email:              String,
    name:               String,        // "" when signing in
    role:               String,        // "" when signing in
    onSuccess:          () -> Unit,    // navigate to appropriate dashboard
    onNavigateBack:     () -> Unit,
    viewModel:          AuthViewModel = hiltViewModel()
) {
    var otp          by remember { mutableStateOf("") }
    var started      by remember { mutableStateOf(false) }
    var resendTimer  by remember { mutableStateOf(30) }
    var canResend    by remember { mutableStateOf(false) }
    var resendSent   by remember { mutableStateOf(false) }
    var shakeError   by remember { mutableStateOf(false) }

    val verifyResult by viewModel.verifyResult.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    // ── Animations ────────────────────────────────────────────────────────────
    val bgAlpha      by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "bg")
    val backAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(400, delayMillis = 100), label = "back")
    val headerOffset by animateFloatAsState(if (started) 0f else 40f, tween(600, delayMillis = 200, easing = EaseOutCubic), label = "hdrY")
    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 200), label = "hdrA")
    val boxOffset    by animateFloatAsState(if (started) 0f else 40f, tween(600, delayMillis = 400, easing = EaseOutCubic), label = "boxY")
    val boxAlpha     by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 400), label = "boxA")
    val hintAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 700), label = "hintA")

    // Shake animation for wrong OTP
    val shakeOffset by animateFloatAsState(
        targetValue   = if (shakeError) 12f else 0f,
        animationSpec = if (shakeError) spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness    = Spring.StiffnessHigh
        ) else spring(),
        label = "shake",
        finishedListener = { shakeError = false }
    )

    // Ambient glow
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f, targetValue = 0.14f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    // ── Side effects ──────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        started = true
        delay(150)
        focusRequester.requestFocus()
    }

    // Resend countdown
    LaunchedEffect(Unit) {
        while (resendTimer > 0) {
            delay(1000)
            resendTimer--
        }
        canResend = true
    }

    // Auto-verify when 6 digits entered
    LaunchedEffect(otp) {
        if (otp.length == 6) {
            viewModel.verifyOtp(email, otp)
        }
    }

    LaunchedEffect(verifyResult) {
        when (val result = verifyResult) {
            is ActionResult.Success -> {
                viewModel.resetVerifyResult()
                // If signing up, save role + name to DB
                if (role.isNotBlank() && name.isNotBlank()) {
                    viewModel.setRole(name, role)
                }
                onSuccess()
            }
            is ActionResult.Error -> {
                shakeError = true
                otp = ""        // clear boxes so user can retry
                delay(100)
                focusRequester.requestFocus()
            }
            else -> {}
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI
    // ═════════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate)
    ) {
        // ── Ambient glow blob ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.Center)
                .alpha(glowAlpha * bgAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(GlacierTeal.copy(alpha = 0.5f), Color.Transparent)
                    ),
                    RoundedCornerShape(50)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = Dimens.ScreenPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.SpaceXL))

            // ── Back ─────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                BackButton(
                    onClick  = onNavigateBack,
                    modifier = Modifier.alpha(backAlpha)
                )
            }

            Spacer(modifier = Modifier.weight(0.4f))

            // ── Header ───────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(headerAlpha)
                    .graphicsLayer { translationY = headerOffset }
            ) {
                // Lock icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PineMid.copy(alpha = 0.4f), GlacierTeal.copy(alpha = 0.2f))
                            )
                        )
                        .border(1.dp, GlacierTeal.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Text("🔐", fontSize = 32.sp)
                }

                Spacer(modifier = Modifier.height(Dimens.SpaceXL))

                Text(
                    text      = "CHECK YOUR EMAIL",
                    style     = EyebrowStyle,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.SpaceXS))

                Text(
                    text      = "Enter the 6-digit code",
                    style     = PahadiRaahTypography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.SpaceSM))

                Text(
                    text      = "We sent it to",
                    style     = PahadiRaahTypography.bodyMedium.copy(color = MistVeil),
                    textAlign = TextAlign.Center
                )
                Text(
                    text      = email,
                    style     = PahadiRaahTypography.bodyMedium.copy(color = GlacierTeal),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(Dimens.Space3XL))

            // ── OTP Boxes ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .alpha(boxAlpha)
                    .graphicsLayer {
                        translationY = boxOffset
                        translationX = shakeOffset
                    }
            ) {
                // Hidden text field captures input
                BasicTextField(
                    value       = otp,
                    onValueChange = { new ->
                        if (new.length <= 6 && new.all { it.isDigit() }) {
                            otp = new
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    cursorBrush     = SolidColor(Color.Transparent),
                    modifier        = Modifier
                        .size(1.dp)     // invisible but focusable
                        .focusRequester(focusRequester)
                ) { _ -> }  // custom decoration box — actual UI is below

                // Visible digit boxes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(6) { index ->
                        val char         = otp.getOrNull(index)?.toString() ?: ""
                        val isCurrent    = index == otp.length
                        val isError      = verifyResult is ActionResult.Error
                        val isFilled     = char.isNotEmpty()

                        val boxBorderColor by animateColorAsState(
                            targetValue = when {
                                isError   -> StatusError
                                isCurrent -> GlacierTeal
                                isFilled  -> GlacierTeal.copy(alpha = 0.5f)
                                else      -> BorderSubtle
                            },
                            animationSpec = tween(150),
                            label = "otpBorder$index"
                        )
                        val boxBg by animateColorAsState(
                            targetValue = when {
                                isError  -> StatusError.copy(alpha = 0.08f)
                                isFilled -> GlacierTeal.copy(alpha = 0.06f)
                                else     -> SurfaceLow
                            },
                            animationSpec = tween(150),
                            label = "otpBg$index"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(width = 46.dp, height = 56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(boxBg)
                                .border(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = boxBorderColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            if (char.isNotEmpty()) {
                                Text(
                                    text  = char,
                                    style = PahadiRaahTypography.headlineSmall.copy(
                                        color    = SnowPeak,
                                        fontSize = 24.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            } else if (isCurrent) {
                                // Blinking cursor dot
                                val cursorAlpha by infiniteTransition.animateFloat(
                                    initialValue = 1f, targetValue = 0f,
                                    animationSpec = infiniteRepeatable(
                                        tween(500), RepeatMode.Reverse
                                    ),
                                    label = "cursor"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(2.dp, 22.dp)
                                        .alpha(cursorAlpha)
                                        .background(GlacierTeal, RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }
            }

            // Error message
            if (verifyResult is ActionResult.Error) {
                Spacer(modifier = Modifier.height(Dimens.SpaceSM))
                Text(
                    text  = (verifyResult as ActionResult.Error).message
                        .ifBlank { "Incorrect code. Please try again." },
                    style = PahadiRaahTypography.labelMedium.copy(color = StatusError),
                    textAlign = TextAlign.Center
                )
            }

            // Loading indicator while verifying
            if (verifyResult is ActionResult.Loading) {
                Spacer(modifier = Modifier.height(Dimens.SpaceSM))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier  = Modifier.size(16.dp),
                        color     = GlacierTeal,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpaceXS))
                    Text(
                        text  = "Verifying…",
                        style = PahadiRaahTypography.labelMedium.copy(color = GlacierTeal)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))

            // ── Resend row ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(hintAlpha)
                    .padding(bottom = Dimens.Space3XL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, BorderSubtle, Color.Transparent)
                            )
                        )
                )
                Spacer(modifier = Modifier.height(Dimens.SpaceMD))

                if (resendSent) {
                    Text(
                        text  = "✓ Code resent to $email",
                        style = PahadiRaahTypography.labelMedium.copy(color = GlacierTeal),
                        textAlign = TextAlign.Center
                    )
                } else if (canResend) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text  = "Didn't get it? ",
                            style = PahadiRaahTypography.bodySmall.copy(
                                color    = MistVeil.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        )
                        Text(
                            text  = "Resend code",
                            style = PahadiRaahTypography.bodySmall.copy(
                                color    = GlacierTeal,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication        = null
                                ) {
                                    viewModel.sendOtp(email)
                                    resendSent  = true
                                    canResend   = false
                                    resendTimer = 30
                                }
                        )
                    }
                } else {
                    Text(
                        text  = "Resend available in ${resendTimer}s",
                        style = PahadiRaahTypography.bodySmall.copy(
                            color    = MistVeil.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OtpVerifyScreenPreview() {
    PahadiRaahTheme {
        OtpVerifyScreen(
            email          = "riya@example.com",
            name           = "Riya",
            role           = "passenger",
            onSuccess      = {},
            onNavigateBack = {}
        )
    }
}