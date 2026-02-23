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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunpitech.pahadiraah.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  AuthComponents.kt
//  Shared UI building blocks for Sign In, Sign Up, and OTP screens.
// ─────────────────────────────────────────────────────────────────────────────


// ── Back Button ───────────────────────────────────────────────────────────────

@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "backScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(Dimens.BackButtonSize)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceLow)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
    ) {
        Text("←", style = PahadiRaahTypography.titleMedium.copy(color = SnowPeak))
    }
}


// ── Primary CTA Button ────────────────────────────────────────────────────────

@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "btnScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ButtonHeight)
            .scale(scale)
            .clip(PillShape)
            .background(
                Brush.linearGradient(colors = GradientPrimary)
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = !isLoading,
                onClick           = onClick
            )
    ) {
        // Inner top highlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SnowPeak.copy(alpha = 0.07f), Color.Transparent)
                    )
                )
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(22.dp),
                color       = SnowPeak,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text  = text,
                style = PahadiRaahTypography.labelLarge.copy(
                    color         = SnowPeak,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}


// ── Auth Text Field ───────────────────────────────────────────────────────────

@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String       = "",
    error: String?            = null,
    isFocused: Boolean        = false,
    onFocusChange: (Boolean) -> Unit = {},
    focusRequester: FocusRequester? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction       = ImeAction.Done,
    onDone: (() -> Unit)?      = null,
    modifier: Modifier         = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            error != null -> StatusError
            isFocused     -> GlacierTeal
            else          -> BorderSubtle
        },
        animationSpec = tween(200),
        label         = "fieldBorder"
    )

    Column(modifier = modifier) {
        Text(text = label, style = FormLabelStyle)
        Spacer(modifier = Modifier.height(Dimens.SpaceXS))

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
            if (value.isEmpty()) {
                Text(
                    text  = placeholder,
                    style = PahadiRaahTypography.bodyLarge.copy(
                        color = MistVeil.copy(alpha = 0.35f)
                    )
                )
            }
            val fieldModifier = Modifier
                .fillMaxWidth()
                .then(
                    if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
                )
                .onFocusChanged { onFocusChange(it.isFocused) }

            BasicTextField(
                value           = value,
                onValueChange   = onValueChange,
                textStyle       = PahadiRaahTypography.bodyLarge.copy(color = SnowPeak),
                cursorBrush     = SolidColor(GlacierTeal),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction    = imeAction
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onDone?.invoke() },
                    onNext = {}
                ),
                modifier = fieldModifier
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(Dimens.SpaceXXS))
            Text(
                text  = error,
                style = PahadiRaahTypography.labelMedium.copy(color = StatusError)
            )
        }
    }
}