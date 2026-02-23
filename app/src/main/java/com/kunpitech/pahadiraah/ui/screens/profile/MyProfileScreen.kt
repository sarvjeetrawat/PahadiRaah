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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.data.model.UserDto
import com.kunpitech.pahadiraah.ui.screens.auth.BackButton
import com.kunpitech.pahadiraah.ui.theme.*
import com.kunpitech.pahadiraah.viewmodel.UserViewModel

// ─────────────────────────────────────────────────────────────────────────────
//  MyProfileScreen
//  Shows the logged-in user's full profile and stats.
//  Avatar taps open ProfileCompletion for editing (future).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MyProfileScreen(
    onBack:    () -> Unit,
    onSignOut: () -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val profileState by viewModel.myProfile.collectAsStateWithLifecycle()
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadMyProfile()
        started = true
    }

    val headerAlpha  by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "ha")
    val cardOffset   by animateFloatAsState(if (started) 0f else 40f, tween(600, delayMillis = 200, easing = EaseOutCubic), label = "cY")
    val cardAlpha    by animateFloatAsState(if (started) 1f else 0f, tween(600, delayMillis = 200), label = "cA")

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate)
    ) {
        // Background glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-80).dp)
                .alpha(glowAlpha)
                .background(
                    Brush.radialGradient(listOf(PineMid.copy(alpha = 0.4f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Dimens.SpaceXL))

            // Back button row
            Row(
                modifier = Modifier.fillMaxWidth().alpha(headerAlpha),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack)
                Text(
                    text  = "MY PROFILE",
                    style = EyebrowStyle
                )
                Spacer(Modifier.size(Dimens.BackButtonSize))
            }

            Spacer(Modifier.height(Dimens.SpaceXXL))

            when (profileState) {
                is UiState.Loading -> {
                    Spacer(Modifier.height(80.dp))
                    CircularProgressIndicator(
                        color       = GlacierTeal,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(32.dp)
                    )
                }

                is UiState.Error -> {
                    Spacer(Modifier.height(60.dp))
                    Text(
                        text  = "Couldn't load profile",
                        style = PahadiRaahTypography.bodyMedium.copy(color = StatusError),
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    val profile = (profileState as? UiState.Success<UserDto>)?.data
                    val isDriver = profile?.role == "driver"

                    // ── Avatar + name ─────────────────────────────────────────
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .alpha(cardAlpha)
                            .graphicsLayer { translationY = cardOffset }
                    ) {
                        // Avatar
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        if (isDriver) listOf(PineDeep, PineMid)
                                        else          listOf(StoneWarm, DustTaupe)
                                    )
                                )
                                .border(
                                    2.dp,
                                    if (isDriver) GlacierTeal.copy(alpha = 0.5f)
                                    else          Saffron.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        ) {
                            Text(text = profile?.emoji ?: "🧑", fontSize = 44.sp)
                        }

                        Spacer(Modifier.height(Dimens.SpaceMD))

                        Text(
                            text  = profile?.name ?: "—",
                            style = PahadiRaahTypography.headlineSmall.copy(color = SnowPeak),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(Dimens.SpaceXS))

                        // Role badge
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(
                                    (if (isDriver) GlacierTeal else Saffron).copy(alpha = 0.12f)
                                )
                                .border(
                                    1.dp,
                                    (if (isDriver) GlacierTeal else Saffron).copy(alpha = 0.35f),
                                    PillShape
                                )
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text  = if (isDriver) "🚗  Driver" else "🎒  Passenger",
                                style = PahadiRaahTypography.labelMedium.copy(
                                    color = if (isDriver) GlacierTeal else Saffron
                                )
                            )
                        }

                        if (!profile?.email.isNullOrBlank()) {
                            Spacer(Modifier.height(Dimens.SpaceXS))
                            Text(
                                text  = profile!!.email!!,
                                style = PahadiRaahTypography.bodySmall.copy(
                                    color = MistVeil.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(Dimens.SpaceXXL))

                    // ── Stats row ─────────────────────────────────────────────
                    if (isDriver) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(cardAlpha)
                                .graphicsLayer { translationY = cardOffset },
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProfileStatCard(
                                modifier = Modifier.weight(1f),
                                emoji    = "🛣️",
                                value    = profile?.totalTrips?.toString() ?: "0",
                                label    = "Trips"
                            )
                            ProfileStatCard(
                                modifier = Modifier.weight(1f),
                                emoji    = "⭐",
                                value    = if ((profile?.avgRating ?: 0.0) > 0)
                                    String.format("%.1f", profile!!.avgRating)
                                else "—",
                                label    = "Rating"
                            )
                            ProfileStatCard(
                                modifier = Modifier.weight(1f),
                                emoji    = "📅",
                                value    = if ((profile?.yearsActive ?: 0) > 0)
                                    "${profile!!.yearsActive}yr"
                                else "New",
                                label    = "Active"
                            )
                        }
                        Spacer(Modifier.height(Dimens.SpaceXL))
                    }

                    // ── Info cards ────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(cardAlpha)
                            .graphicsLayer { translationY = cardOffset },
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!profile?.bio.isNullOrBlank()) {
                            ProfileInfoCard(label = "About", value = profile!!.bio!!)
                        }

                        if (profile?.languages?.isNotEmpty() == true) {
                            ProfileInfoCard(
                                label = "Languages",
                                value = profile.languages.joinToString(" • ")
                            )
                        }

                        if (!profile?.speciality.isNullOrBlank()) {
                            ProfileInfoCard(label = "Speciality", value = profile!!.speciality!!)
                        }

                        // Online status (driver only)
                        if (isDriver) {
                            ProfileInfoCard(
                                label = "Status",
                                value = if (profile?.isOnline == true) "🟢 Currently Online"
                                else "⚫ Currently Offline"
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Dimens.Space3XL))

            // ── Sign Out button ───────────────────────────────────────────────
            SignOutButton(
                onClick  = onSignOut,
                modifier = Modifier
                    .alpha(cardAlpha)
                    .padding(bottom = Dimens.Space3XL)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileStatCard(
    modifier: Modifier = Modifier,
    emoji:    String,
    value:    String,
    label:    String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(PahadiRaahShapes.medium)
            .background(SurfaceLow)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.medium)
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text      = value,
            style     = PahadiRaahTypography.headlineSmall.copy(color = SnowPeak, fontSize = 22.sp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text      = label,
            style     = PahadiRaahTypography.bodySmall.copy(color = Sage, fontSize = 11.sp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileInfoCard(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PahadiRaahShapes.medium)
            .background(SurfaceLow)
            .border(1.dp, BorderSubtle, PahadiRaahShapes.medium)
            .padding(16.dp)
    ) {
        Text(
            text  = label.uppercase(),
            style = FormLabelStyle.copy(color = Sage.copy(alpha = 0.7f))
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text     = value,
            style    = PahadiRaahTypography.bodyMedium.copy(color = SnowPeak),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SignOutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.96f else 1f,
        spring(stiffness = Spring.StiffnessMedium), label = "so"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ButtonHeight)
            .scale(scale)
            .clip(PillShape)
            .background(StatusError.copy(alpha = 0.1f))
            .border(1.dp, StatusError.copy(alpha = 0.35f), PillShape)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
    ) {
        Text(
            text  = "Sign Out",
            style = PahadiRaahTypography.labelLarge.copy(color = StatusError)
        )
    }
}