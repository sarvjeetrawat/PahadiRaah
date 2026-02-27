package com.kunpitech.pahadiraah.ui.screens.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kunpitech.pahadiraah.ui.screens.auth.OtpVerifyScreen
import com.kunpitech.pahadiraah.ui.screens.auth.ProfileCompletionScreen
import com.kunpitech.pahadiraah.ui.screens.auth.SignInScreen
import com.kunpitech.pahadiraah.ui.screens.auth.SignUpScreen
import com.kunpitech.pahadiraah.ui.screens.driver.*
import com.kunpitech.pahadiraah.ui.screens.passenger.*
import com.kunpitech.pahadiraah.ui.screens.profile.MyProfileScreen
import com.kunpitech.pahadiraah.ui.screens.role.RoleSelectScreen
import com.kunpitech.pahadiraah.ui.screens.splash.SplashScreen
import com.kunpitech.pahadiraah.viewmodel.AuthViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ─────────────────────────────────────────────────────────────────────────────
//  ROUTES
// ─────────────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object Splash             : Screen("splash")

    // fromGoogle = true  → user just signed in with Google, skip OTP flow
    // fromGoogle = false → normal email signup flow
    object RoleSelect         : Screen("role_select/{fromGoogle}") {
        fun createRoute(fromGoogle: Boolean = false) = "role_select/$fromGoogle"
    }

    object SignIn             : Screen("sign_in")
    object SignUp             : Screen("sign_up/{role}") {
        fun createRoute(role: String) = "sign_up/$role"
    }
    object OtpVerify          : Screen("otp_verify/{email}/{name}/{role}") {
        fun createRoute(email: String, name: String, role: String): String {
            val enc = { s: String -> URLEncoder.encode(s.ifBlank { "_" }, StandardCharsets.UTF_8.toString()) }
            return "otp_verify/${enc(email)}/${enc(name)}/${enc(role)}"
        }
    }
    object ProfileCompletion  : Screen("profile_completion/{role}") {
        fun createRoute(role: String) = "profile_completion/$role"
    }

    // ── Profile ───────────────────────────────────────────────────────────────
    object MyProfile          : Screen("my_profile")

    // ── Driver ────────────────────────────────────────────────────────────────
    object DriverDashboard    : Screen("driver_dashboard")
    object PostRoute          : Screen("post_route")
    object ActiveRoutes       : Screen("active_routes")
    object BookingRequests    : Screen("booking_requests/{routeId}") {
        fun createRoute(routeId: String) = "booking_requests/$routeId"
    }

    // ── Passenger ─────────────────────────────────────────────────────────────
    object PassengerDashboard : Screen("passenger_dashboard")
    object SearchRoutes       : Screen("search_routes")
    object BrowseDrivers      : Screen("browse_drivers")
    object MyBookings         : Screen("my_bookings")
    object DriverProfile      : Screen("driver_profile/{driverId}") {
        fun createRoute(driverId: String) = "driver_profile/$driverId"
    }
    object BookingConfirm     : Screen("booking_confirm/{routeId}") {
        fun createRoute(routeId: String) = "booking_confirm/$routeId"
    }
    object TripProgress       : Screen("trip_progress/{bookingId}") {
        fun createRoute(bookingId: String) = "trip_progress/$bookingId"
    }
    object RateReview : Screen(
        "rate_review/{bookingId}/{routeId}/{driverId}/{driverName}/{driverEmoji}"
    ) {
        fun createRoute(
            bookingId:   String,
            routeId:     String,
            driverId:    String,
            driverName:  String,
            driverEmoji: String
        ): String {
            val enc = { s: String ->
                java.net.URLEncoder.encode(s.ifBlank { "_" }, "UTF-8")
            }
            return "rate_review/${enc(bookingId)}/${enc(routeId)}/${enc(driverId)}/${enc(driverName)}/${enc(driverEmoji)}"
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPERS
// ─────────────────────────────────────────────────────────────────────────────

private fun decode(s: String): String {
    val decoded = URLDecoder.decode(s, StandardCharsets.UTF_8.toString())
    return if (decoded == "_") "" else decoded
}

// ─────────────────────────────────────────────────────────────────────────────
//  NAV GRAPH
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PahadiRaahNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val role        by authViewModel.role.collectAsStateWithLifecycle()

    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route
    ) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            LaunchedEffect(currentUser, role) {
                if (currentUser == null) return@LaunchedEffect
                if (role == null) return@LaunchedEffect
                val dest = if (role == "driver") Screen.DriverDashboard.route
                else Screen.PassengerDashboard.route
                navController.navigate(dest) { popUpTo(0) { inclusive = true } }
            }

            SplashScreen(
                onNavigateToSignUp = {
                    // Normal signup → RoleSelect → SignUp → OTP → ProfileCompletion
                    navController.navigate(Screen.RoleSelect.createRoute(fromGoogle = false)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                isLoggedIn = currentUser != null
            )
        }

        // ── Role Select ───────────────────────────────────────────────────────
        //
        //  fromGoogle = false → normal path: go to SignUp screen (email + OTP)
        //  fromGoogle = true  → Google user already authenticated: just call
        //                       setRole() and go straight to the dashboard.
        //                       No email, no OTP, no ProfileCompletion needed.
        composable(
            route     = Screen.RoleSelect.route,
            arguments = listOf(navArgument("fromGoogle") { type = NavType.BoolType })
        ) { backStack ->
            val fromGoogle = backStack.arguments?.getBoolean("fromGoogle") ?: false

            RoleSelectScreen(
                onDriverSelected = {
                    if (fromGoogle) {
                        // Google user — save role directly, go to dashboard
                        val gUser = authViewModel.currentUser.value
                        authViewModel.setRole(
                            name  = gUser?.userMetadata?.get("full_name")
                                ?.toString()?.trim('"') ?: "",
                            role  = "driver",
                            email = gUser?.email
                                ?: gUser?.userMetadata?.get("email")?.toString()?.trim('"'),
                            emoji = "\ud83e\uddd1"
                        )
                        navController.navigate(Screen.DriverDashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        // Normal email signup — continue with OTP flow
                        navController.navigate(Screen.SignUp.createRoute("driver"))
                    }
                },
                onPassengerSelected = {
                    if (fromGoogle) {
                        val gUser = authViewModel.currentUser.value
                        authViewModel.setRole(
                            name  = gUser?.userMetadata?.get("full_name")
                                ?.toString()?.trim('"') ?: "",
                            role  = "passenger",
                            email = gUser?.email
                                ?: gUser?.userMetadata?.get("email")?.toString()?.trim('"'),
                            emoji = "\ud83e\uddd1"
                        )
                        navController.navigate(Screen.PassengerDashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.SignUp.createRoute("passenger"))
                    }
                }
            )
        }

        // ── Sign Up ───────────────────────────────────────────────────────────
        composable(
            route     = Screen.SignUp.route,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) {
            SignUpScreen(
                onNavigateToOtp = { email, name, role ->
                    navController.navigate(Screen.OtpVerify.createRoute(email, name, role))
                },
                onNavigateBack  = { navController.popBackStack() }
            )
        }

        // ── Sign In ───────────────────────────────────────────────────────────
        composable(Screen.SignIn.route) {
            SignInScreen(
                onNavigateToOtp       = { email ->
                    navController.navigate(Screen.OtpVerify.createRoute(email, "", ""))
                },
                onGoogleSignInSuccess = { isNewUser ->
                    if (isNewUser) {
                        // First-time Google user → pick a role (no OTP needed)
                        navController.navigate(Screen.RoleSelect.createRoute(fromGoogle = true)) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    } else {
                        // Returning Google user → role already loaded by ViewModel
                        val dest = if (authViewModel.role.value == "driver")
                            Screen.DriverDashboard.route else Screen.PassengerDashboard.route
                        navController.navigate(dest) { popUpTo(0) { inclusive = true } }
                    }
                },
                onNavigateBack        = { navController.popBackStack() }
            )
        }

        // ── OTP Verify ────────────────────────────────────────────────────────
        composable(
            route     = Screen.OtpVerify.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("name")  { type = NavType.StringType },
                navArgument("role")  { type = NavType.StringType }
            )
        ) { backStack ->
            val email    = decode(backStack.arguments?.getString("email") ?: "_")
            val name     = decode(backStack.arguments?.getString("name")  ?: "_")
            val roleArg  = decode(backStack.arguments?.getString("role")  ?: "_")
            val isSignUp = roleArg.isNotBlank()

            OtpVerifyScreen(
                email          = email,
                name           = name,
                role           = roleArg,
                onSuccess      = {
                    if (isSignUp) {
                        navController.navigate(Screen.ProfileCompletion.createRoute(roleArg)) {
                            popUpTo(Screen.RoleSelect.createRoute(false)) { inclusive = true }
                        }
                    } else {
                        val dest = if (authViewModel.role.value == "driver")
                            Screen.DriverDashboard.route else Screen.PassengerDashboard.route
                        navController.navigate(dest) { popUpTo(0) { inclusive = true } }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Profile Completion ────────────────────────────────────────────────
        composable(
            route     = Screen.ProfileCompletion.route,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStack ->
            val roleArg = backStack.arguments?.getString("role") ?: "passenger"
            ProfileCompletionScreen(
                role       = roleArg,
                onComplete = {
                    val dest = if (roleArg == "driver") Screen.DriverDashboard.route
                    else Screen.PassengerDashboard.route
                    navController.navigate(dest) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // ── My Profile ────────────────────────────────────────────────────────
        composable(Screen.MyProfile.route) {
            MyProfileScreen(
                onBack    = { navController.popBackStack() },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // ─────────────────────────────────────────────────────────────────────
        //  DRIVER
        // ─────────────────────────────────────────────────────────────────────

        composable(Screen.DriverDashboard.route) {
            DriverDashboardScreen(
                onPostRoute       = { navController.navigate(Screen.PostRoute.route) },
                onActiveRoutes    = { navController.navigate(Screen.ActiveRoutes.route) },
                onBookingRequests = { navController.navigate(Screen.BookingRequests.createRoute("all")) },
                onProfile         = { navController.navigate(Screen.MyProfile.route) },
                onBack            = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Screen.PostRoute.route) {
            PostRouteScreen(
                onBack    = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(Screen.DriverDashboard.route) {
                        popUpTo(Screen.DriverDashboard.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.ActiveRoutes.route) {
            ActiveRoutesScreen(
                onBack         = { navController.popBackStack() },
                onViewRequests = { routeId ->
                    navController.navigate(Screen.BookingRequests.createRoute(routeId))
                }
            )
        }

        composable(Screen.BookingRequests.route) { backStack ->
            val routeId = backStack.arguments?.getString("routeId") ?: "all"
            BookingRequestsScreen(
                routeId = routeId,
                onBack  = { navController.popBackStack() }
            )
        }

        // ─────────────────────────────────────────────────────────────────────
        //  PASSENGER
        // ─────────────────────────────────────────────────────────────────────

        composable(Screen.PassengerDashboard.route) {
            PassengerDashboardScreen(
                onSearchRoutes  = { navController.navigate(Screen.SearchRoutes.route) },
                onBrowseDrivers = { navController.navigate(Screen.BrowseDrivers.route) },
                onMyBookings    = { navController.navigate(Screen.MyBookings.route) },
                onProfile       = { navController.navigate(Screen.MyProfile.route) },
                onBack          = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Screen.SearchRoutes.route) {
            SearchRoutesScreen(
                onBack       = { navController.popBackStack() },
                onRouteClick = { routeId ->
                    navController.navigate(Screen.BookingConfirm.createRoute(routeId))
                }
            )
        }

        composable(Screen.BrowseDrivers.route) {
            BrowseDriversScreen(
                onBack         = { navController.popBackStack() },
                onDriverSelect = { driverId ->
                    navController.navigate(Screen.DriverProfile.createRoute(driverId))
                }
            )
        }

        composable(Screen.MyBookings.route) {
            MyBookingsScreen(
                onBack      = { navController.popBackStack() },
                onTrackTrip = { bookingId ->
                    navController.navigate(Screen.TripProgress.createRoute(bookingId))
                },
                onRateTrip  = { bookingId, routeId, driverId, driverName, driverEmoji ->
                    navController.navigate(
                        Screen.RateReview.createRoute(bookingId, routeId, driverId, driverName, driverEmoji)
                    )
                }
            )
        }

        composable(Screen.DriverProfile.route) { backStack ->
            val driverId = backStack.arguments?.getString("driverId") ?: ""
            DriverProfileScreen(
                driverId   = driverId,
                onBack     = { navController.popBackStack() },
                onBookSeat = { routeId ->
                    navController.navigate(Screen.BookingConfirm.createRoute(routeId))
                }
            )
        }

        composable(Screen.BookingConfirm.route) { backStack ->
            val routeId = backStack.arguments?.getString("routeId") ?: ""
            BookingConfirmScreen(
                routeId     = routeId,
                onBack      = { navController.popBackStack() },
                onTrackTrip = { bookingId ->
                    navController.navigate(Screen.TripProgress.createRoute(bookingId))
                },
                onHome = {
                    navController.navigate(Screen.PassengerDashboard.route) {
                        popUpTo(Screen.PassengerDashboard.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.TripProgress.route) { backStack ->
            val bookingId = backStack.arguments?.getString("bookingId") ?: ""
            TripProgressScreen(
                bookingId  = bookingId,
                onBack     = { navController.popBackStack() },
                onRateTrip = { bId, rId, dId, dName, dEmoji ->
                    navController.navigate(Screen.RateReview.createRoute(bId, rId, dId, dName, dEmoji))
                }
            )
        }

        composable(
            route     = Screen.RateReview.route,
            arguments = listOf(
                navArgument("bookingId")   { type = NavType.StringType },
                navArgument("routeId")     { type = NavType.StringType },
                navArgument("driverId")    { type = NavType.StringType },
                navArgument("driverName")  { type = NavType.StringType },
                navArgument("driverEmoji") { type = NavType.StringType }
            )
        ) { backStack ->
            val bookingId   = decode(backStack.arguments?.getString("bookingId")   ?: "_")
            val routeId     = decode(backStack.arguments?.getString("routeId")     ?: "_")
            val driverId    = decode(backStack.arguments?.getString("driverId")    ?: "_")
            val driverName  = decode(backStack.arguments?.getString("driverName")  ?: "_")
            val driverEmoji = decode(backStack.arguments?.getString("driverEmoji") ?: "_")
            RateReviewScreen(
                bookingId   = bookingId,
                routeId     = routeId,
                driverId    = driverId,
                driverName  = driverName,
                driverEmoji = driverEmoji,
                onBack      = { navController.popBackStack() },
                onDone      = {
                    navController.navigate(Screen.PassengerDashboard.route) {
                        popUpTo(Screen.PassengerDashboard.route) { inclusive = false }
                    }
                }
            )
        }
    }
}