package com.kunpitech.pahadiraah.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunpitech.pahadiraah.ui.theme.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
//  SplashScreen
//
//  Animation sequence (total ~2800ms before navigate):
//   0ms   → Background gradient fades in
//   200ms → Stars twinkle in (staggered)
//   400ms → Mountain layers slide up from bottom
//   800ms → Logo icon scales + fades in with glow
//  1100ms → App name slides up + fades in
//  1400ms → Tagline fades in with letter spacing
//  1700ms → Buttons fade + slide up
//  2800ms → Navigate to RoleSelectScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SplashScreen(
    onNavigateToRoleSelect: () -> Unit
) {
    // ── Animation States ──────────────────────────────────────────────────────
    var animationStarted by remember { mutableStateOf(false) }

    val bgAlpha          by animateFloatAsState(if (animationStarted) 1f else 0f,    tween(600), label = "bg")
    val mountainOffset   by animateFloatAsState(if (animationStarted) 0f else 300f,  tween(900, easing = EaseOutCubic), label = "mountain")
    val mountainAlpha    by animateFloatAsState(if (animationStarted) 1f else 0f,    tween(900), label = "mountainAlpha")
    val logoScale        by animateFloatAsState(if (animationStarted) 1f else 0.4f,  tween(600, delayMillis = 600, easing = EaseOutBack), label = "logoScale")
    val logoAlpha        by animateFloatAsState(if (animationStarted) 1f else 0f,    tween(500, delayMillis = 600), label = "logoAlpha")
    val nameOffset       by animateFloatAsState(if (animationStarted) 0f else 40f,   tween(600, delayMillis = 850, easing = EaseOutCubic), label = "nameOffset")
    val nameAlpha        by animateFloatAsState(if (animationStarted) 1f else 0f,    tween(600, delayMillis = 850), label = "nameAlpha")
    val taglineAlpha     by animateFloatAsState(if (animationStarted) 1f else 0f,    tween(600, delayMillis = 1100), label = "taglineAlpha")
    val buttonsOffset    by animateFloatAsState(if (animationStarted) 0f else 50f,   tween(600, delayMillis = 1300, easing = EaseOutCubic), label = "btnOffset")
    val buttonsAlpha     by animateFloatAsState(if (animationStarted) 1f else 0f,    tween(600, delayMillis = 1300), label = "btnAlpha")

    // Infinite logo float animation
    val infiniteTransition = rememberInfiniteTransition(label = "logoFloat")
    val logoFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoFloat"
    )

    // Glow pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Star twinkle values — 12 stars
    val starAlphas = (0..11).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue  = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1500 + (i * 200),
                    easing = EaseInOutSine
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "star$i"
        )
    }

    // ── Launch sequence ───────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        animationStarted = true
        delay(2800)
        onNavigateToRoleSelect()
    }

    // ── Star positions (fixed) ────────────────────────────────────────────────
    val starPositions = remember {
        listOf(
            Offset(0.08f, 0.04f), Offset(0.22f, 0.09f), Offset(0.45f, 0.03f),
            Offset(0.67f, 0.07f), Offset(0.82f, 0.02f), Offset(0.91f, 0.11f),
            Offset(0.15f, 0.15f), Offset(0.55f, 0.12f), Offset(0.73f, 0.18f),
            Offset(0.33f, 0.19f), Offset(0.88f, 0.22f), Offset(0.04f, 0.22f),
        )
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI
    // ═════════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Pine)
    ) {

        // ── 1. Background radial gradient ────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(bgAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Moss.copy(alpha = 0.25f),
                            Forest.copy(alpha = 0.15f),
                            Pine.copy(alpha = 0f)
                        ),
                        center = Offset(0.5f, 0f),
                        radius = 1200f
                    )
                )
        )

        // ── 2. Stars ─────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            starPositions.forEachIndexed { i, pos ->
                val size = if (i % 3 == 0) 3.dp else if (i % 2 == 0) 2.dp else 2.5.dp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(starAlphas[i].value * bgAlpha)
                ) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (pos.x * 400).dp,
                                y = (pos.y * 800).dp
                            )
                            .size(size)
                            .background(Snow.copy(alpha = 0.8f), RoundedCornerShape(50))
                    )
                }
            }
        }

        // ── 3. Mountain layers ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .align(Alignment.BottomCenter)
                .alpha(mountainAlpha)
                .graphicsLayer { translationY = mountainOffset }
        ) {
            MountainLayers()
        }

        // ── 4. Main content (centered) ───────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.ScreenPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(modifier = Modifier.weight(0.5f))

            // ── Logo Icon ────────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .graphicsLayer { translationY = logoFloat }
            ) {
                // Glow ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .alpha(glowAlpha * logoAlpha)
                        .blur(20.dp)
                        .background(
                            Brush.radialGradient(listOf(Sage.copy(alpha = 0.8f), Color.Transparent)),
                            RoundedCornerShape(50)
                        )
                )
                // Icon card
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(LogoShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Moss, Sage),
                                start = Offset(0f, 0f),
                                end = Offset(80f, 80f)
                            )
                        )
                ) {
                    Text(
                        text = "🏔️",
                        fontSize = 36.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpaceXL))

            // ── App Name ─────────────────────────────────────────────────────
            Text(
                text = "PahadiRaah",
                style = AppNameStyle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(nameAlpha)
                    .graphicsLayer { translationY = nameOffset }
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceXS))

            // ── Tagline ──────────────────────────────────────────────────────
            Text(
                text = "YOUR MOUNTAIN JOURNEY",
                style = TaglineStyle,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha)
            )

            Spacer(modifier = Modifier.weight(0.5f))

            // ── Buttons ──────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(buttonsAlpha)
                    .graphicsLayer { translationY = buttonsOffset }
                    .padding(bottom = Dimens.Space3XL)
            ) {
                // Primary — Begin Journey
                SplashPrimaryButton(
                    text = "Begin Your Journey",
                    onClick = onNavigateToRoleSelect
                )

                Spacer(modifier = Modifier.height(Dimens.SpaceMD))

                // Ghost — Sign In
                SplashGhostButton(
                    text = "Sign In",
                    onClick = onNavigateToRoleSelect
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MOUNTAIN LAYERS COMPOSABLE
//  3 layered mountain silhouettes drawn with Canvas paths
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MountainLayers() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // ── Layer 1: Farthest mountains ──────────────────────────────────────
        val farPath = Path().apply {
            moveTo(0f, h * 0.65f)
            lineTo(w * 0.12f, h * 0.30f)
            lineTo(w * 0.22f, h * 0.48f)
            lineTo(w * 0.35f, h * 0.10f)
            lineTo(w * 0.46f, h * 0.38f)
            lineTo(w * 0.57f, h * 0.22f)
            lineTo(w * 0.70f, h * 0.42f)
            lineTo(w * 0.80f, h * 0.18f)
            lineTo(w * 0.92f, h * 0.45f)
            lineTo(w * 1.0f,  h * 0.32f)
            lineTo(w * 1.0f,  h * 1.0f)
            lineTo(0f, h * 1.0f)
            close()
        }
        drawPath(path = farPath, color = Pine.copy(alpha = 0.85f))

        // Snow caps
        val snowCap1 = Path().apply {
            moveTo(w * 0.35f, h * 0.10f)
            lineTo(w * 0.39f, h * 0.20f)
            lineTo(w * 0.31f, h * 0.20f)
            close()
        }
        drawPath(snowCap1, Snow.copy(alpha = 0.6f))

        val snowCap2 = Path().apply {
            moveTo(w * 0.80f, h * 0.18f)
            lineTo(w * 0.84f, h * 0.28f)
            lineTo(w * 0.76f, h * 0.28f)
            close()
        }
        drawPath(snowCap2, Snow.copy(alpha = 0.4f))

        // ── Layer 2: Mid mountains ───────────────────────────────────────────
        val midPath = Path().apply {
            moveTo(0f, h * 0.75f)
            lineTo(w * 0.08f, h * 0.50f)
            lineTo(w * 0.18f, h * 0.60f)
            lineTo(w * 0.30f, h * 0.35f)
            lineTo(w * 0.40f, h * 0.55f)
            lineTo(w * 0.52f, h * 0.42f)
            lineTo(w * 0.65f, h * 0.62f)
            lineTo(w * 0.75f, h * 0.45f)
            lineTo(w * 0.88f, h * 0.58f)
            lineTo(w * 1.0f,  h * 0.50f)
            lineTo(w * 1.0f,  h * 1.0f)
            lineTo(0f, h * 1.0f)
            close()
        }
        drawPath(
            path  = midPath,
            brush = Brush.verticalGradient(
                colors = listOf(Forest.copy(alpha = 0.95f), Pine),
                startY = 0f,
                endY   = h
            )
        )

        // ── Layer 3: Foreground hills ────────────────────────────────────────
        val fgPath = Path().apply {
            moveTo(0f, h * 1.0f)
            lineTo(0f, h * 0.72f)
            lineTo(w * 0.05f, h * 0.68f)
            lineTo(w * 0.12f, h * 0.75f)
            lineTo(w * 0.20f, h * 0.62f)
            lineTo(w * 0.28f, h * 0.70f)
            lineTo(w * 0.40f, h * 0.58f)
            lineTo(w * 0.50f, h * 0.68f)
            lineTo(w * 0.60f, h * 0.60f)
            lineTo(w * 0.72f, h * 0.72f)
            lineTo(w * 0.82f, h * 0.62f)
            lineTo(w * 0.92f, h * 0.70f)
            lineTo(w * 1.0f,  h * 0.65f)
            lineTo(w * 1.0f,  h * 1.0f)
            close()
        }
        drawPath(path = fgPath, color = Color(0xFF0D1A0D))

        // Pine tree clusters
        drawTreeCluster(w * 0.06f, h * 0.90f, 0.7f, Color(0xFF0A150A))
        drawTreeCluster(w * 0.14f, h * 0.88f, 0.9f, Color(0xFF0A150A))
        drawTreeCluster(w * 0.86f, h * 0.88f, 0.8f, Color(0xFF0A150A))
        drawTreeCluster(w * 0.94f, h * 0.90f, 0.65f, Color(0xFF0A150A))

        // Bottom fade to background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Pine),
                startY = h * 0.85f,
                endY   = h
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TREE HELPER — draws pine tree triangles at given position
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawTreeCluster(
    centerX: Float,
    baseY: Float,
    scale: Float,
    color: Color
) {
    val treeWidth  = 28f * scale
    val treeHeight = 55f * scale

    listOf(
        Triple(treeHeight * 1.0f, treeWidth * 1.0f,  0f),
        Triple(treeHeight * 0.75f, treeWidth * 0.8f, treeHeight * 0.25f),
        Triple(treeHeight * 0.5f,  treeWidth * 0.6f, treeHeight * 0.52f),
    ).forEach { (_, layerW, yOffset) ->
        val path = Path().apply {
            moveTo(centerX,              baseY - treeHeight + yOffset - treeHeight * 0.08f)
            lineTo(centerX + layerW / 2f, baseY - yOffset * 0.2f)
            lineTo(centerX - layerW / 2f, baseY - yOffset * 0.2f)
            close()
        }
        drawPath(path, color)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BUTTON COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SplashPrimaryButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btnScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.ButtonHeight)
            .scale(scale)
            .clip(PillShape)
            .background(
                Brush.linearGradient(colors = GradientMoss)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Inner highlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Snow.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )
        Text(
            text  = text,
            style = PahadiRaahTypography.labelLarge.copy(
                color         = Snow,
                fontSize      = 16.sp,
                letterSpacing = 0.5.sp
            )
        )
    }
}

@Composable
fun SplashGhostButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ghostScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.ButtonHeight)
            .scale(scale)
            .clip(PillShape)
            .background(Color.Transparent)
            // Border via inner box
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Mist.copy(alpha = 0.3f), Sage.copy(alpha = 0.4f))
                ),
                shape = PillShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            text  = text,
            style = PahadiRaahTypography.labelLarge.copy(
                color         = Mist,
                fontSize      = 16.sp,
                letterSpacing = 0.5.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PREVIEW
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    PahadiRaahTheme {
        SplashScreen(onNavigateToRoleSelect = {})
    }
}