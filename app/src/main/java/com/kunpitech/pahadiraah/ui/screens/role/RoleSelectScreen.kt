package com.kunpitech.pahadiraah.ui.screens.role

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunpitech.pahadiraah.ui.theme.*

@Composable
fun RoleSelectScreen(
    onDriverSelected: () -> Unit,
    onPassengerSelected: () -> Unit
) {
    var started by remember { mutableStateOf(false) }

    val bgAlpha       by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "bg")
    val eyebrowAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 200), label = "eyebrow")
    val eyebrowOffset by animateFloatAsState(if (started) 0f else -20f, tween(500, delayMillis = 200, easing = EaseOutCubic), label = "eyebrowY")
    val titleAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 350), label = "title")
    val titleOffset   by animateFloatAsState(if (started) 0f else 30f, tween(600, delayMillis = 350, easing = EaseOutCubic), label = "titleY")
    val subAlpha      by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 600), label = "sub")
    val driverAlpha   by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 800), label = "driverA")
    val driverOffset  by animateFloatAsState(if (started) 0f else -60f, tween(700, delayMillis = 800, easing = EaseOutBack), label = "driverX")
    val passAlpha     by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 950), label = "passA")
    val passOffset    by animateFloatAsState(if (started) 0f else 60f, tween(700, delayMillis = 950, easing = EaseOutBack), label = "passX")
    val hintAlpha     by animateFloatAsState(if (started) 1f else 0f, tween(500, delayMillis = 1200), label = "hint")

    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val blob1Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(4000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "blob1"
    )
    val blob2Scale by infiniteTransition.animateFloat(
        initialValue = 1.1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(5000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "blob2"
    )

    LaunchedEffect(Unit) { started = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Pine)
    ) {
        // ── Ambient blobs ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(bgAlpha)
        ) {
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .offset(x = (-80).dp, y = (-60).dp)
                    .scale(blob1Scale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Moss.copy(alpha = 0.18f), Color.Transparent)
                        ),
                        RoundedCornerShape(50)
                    )
            )
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 80.dp, y = 80.dp)
                    .scale(blob2Scale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Gold.copy(alpha = 0.12f), Color.Transparent)
                        ),
                        RoundedCornerShape(50)
                    )
            )
        }

        // ── Scrollable content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(48.dp))

            // Eyebrow
            Text(
                text      = "WHO ARE YOU TODAY?",
                style     = EyebrowStyle,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .alpha(eyebrowAlpha)
                    .graphicsLayer { translationY = eyebrowOffset }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text  = "Choose Your\nMountain Role",
                style = PahadiRaahTypography.headlineLarge.copy(
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .alpha(titleAlpha)
                    .graphicsLayer { translationY = titleOffset }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text      = "Each journey starts with a simple choice",
                style     = PahadiRaahTypography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    color     = Sage
                ),
                modifier  = Modifier.alpha(subAlpha)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Cards Row — fixed height ensures equal size ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RoleCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .alpha(driverAlpha)
                        .graphicsLayer { translationX = driverOffset },
                    emoji          = "🚗",
                    role           = "Driver",
                    description    = "Share your route. Earn while you travel the peaks.",
                    gradientColors = listOf(Moss.copy(alpha = 0.25f), Color.Transparent),
                    accentColor    = Sage,
                    onClick        = onDriverSelected
                )

                RoleCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .alpha(passAlpha)
                        .graphicsLayer { translationX = passOffset },
                    emoji          = "🎒",
                    role           = "Passenger",
                    description    = "Find a trusted ride through the Himalayas with ease.",
                    gradientColors = listOf(Gold.copy(alpha = 0.2f), Color.Transparent),
                    accentColor    = Amber,
                    onClick        = onPassengerSelected
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(1.dp)
                    .alpha(hintAlpha)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, BorderSubtle, Color.Transparent)
                        )
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text      = "You can switch roles anytime",
                style     = PahadiRaahTypography.labelSmall.copy(
                    color         = Sage.copy(alpha = 0.6f),
                    letterSpacing = 1.5.sp,
                    fontSize      = 10.sp
                ),
                textAlign = TextAlign.Center,
                modifier  = Modifier.alpha(hintAlpha)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ROLE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RoleCard(
    modifier: Modifier = Modifier,
    emoji: String,
    role: String,
    description: String,
    gradientColors: List<Color>,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "cardScale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue   = if (isPressed) 0.8f else 0.2f,
        animationSpec = tween(150),
        label         = "borderAlpha"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(PahadiRaahShapes.large)
            .background(
                Brush.verticalGradient(
                    colors = listOf(gradientColors[0], gradientColors[1])
                )
            )
            .background(Snow.copy(alpha = if (isPressed) 0.08f else 0.03f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = borderAlpha),
                        accentColor.copy(alpha = borderAlpha * 0.3f)
                    )
                ),
                shape = PahadiRaahShapes.large
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Top content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Emoji icon box
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                ) {
                    Text(
                        text     = emoji,
                        fontSize = 28.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Role — single line, never wraps
                Text(
                    text      = role,
                    style     = PahadiRaahTypography.titleMedium.copy(
                        color    = accentColor,
                        fontSize = 18.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines  = 1,
                    overflow  = TextOverflow.Clip
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description — 3 lines max, ellipsis if too long
                Text(
                    text      = description,
                    style     = PahadiRaahTypography.bodySmall.copy(
                        textAlign  = TextAlign.Center,
                        color      = Sage.copy(alpha = 0.8f),
                        lineHeight = 17.sp,
                        fontSize   = 11.sp
                    ),
                    maxLines  = 3,
                    overflow  = TextOverflow.Ellipsis
                )
            }

            // Select button — always pinned at bottom of card
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(PillShape)
                    .background(accentColor.copy(alpha = 0.1f))
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.25f),
                        shape = PillShape
                    )
            ) {
                Text(
                    text     = "Select  →",
                    style    = PahadiRaahTypography.labelMedium.copy(color = accentColor),
                    maxLines = 1
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RoleSelectScreenPreview() {
    PahadiRaahTheme {
        RoleSelectScreen(
            onDriverSelected    = {},
            onPassengerSelected = {}
        )
    }
}