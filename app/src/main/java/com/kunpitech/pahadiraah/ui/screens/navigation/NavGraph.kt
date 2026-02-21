package com.kunpitech.pahadiraah.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kunpitech.pahadiraah.ui.screens.driver.*
import com.kunpitech.pahadiraah.ui.screens.passenger.*
import com.kunpitech.pahadiraah.ui.screens.role.RoleSelectScreen
import com.kunpitech.pahadiraah.ui.screens.splash.SplashScreen
import com.kunpitech.pahadiraah.viewmodel.AuthViewModel

// ─────────────────────────────────────────────────────────────────────────────
//  ROUTES
// ─────────────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object Splash             : Screen("splash")
    object RoleSelect         : Screen("role_select")
    object DriverDashboard    : Screen("driver_dashboard")
    object PostRoute          : Screen("post_route")
    object ActiveRoutes       : Screen("active_routes")
    object BookingRequests    : Screen("booking_requests/{routeId}") {
        fun createRoute(routeId: String) = "booking_requests/$routeId"
    }
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
    object RateReview         : Screen("rate_review/{bookingId}/{driverId}") {
        fun createRoute(bookingId: String, driverId: String) = "rate_review/$bookingId/$driverId"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  NAV GRAPH
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PahadiRaahNavGraph(
    navController: NavHostController = rememberNavController()
) {
    // Shared AuthViewModel — one instance across all screens
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val role        by authViewModel.role.collectAsStateWithLifecycle()

    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route
    ) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToRoleSelect = {
                    // If already logged in, skip role select and go straight to dashboard
                    val dest = if (currentUser != null) {
                        if (role == "driver") Screen.DriverDashboard.route
                        else Screen.PassengerDashboard.route
                    } else {
                        Screen.RoleSelect.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Role Select ───────────────────────────────────────────────────────
        composable(Screen.RoleSelect.route) {
            RoleSelectScreen(
                onDriverSelected    = {
                    authViewModel.setRole("Driver", "driver")
                    navController.navigate(Screen.DriverDashboard.route) {
                        popUpTo(Screen.RoleSelect.route) { inclusive = true }
                    }
                },
                onPassengerSelected = {
                    authViewModel.setRole("Passenger", "passenger")
                    navController.navigate(Screen.PassengerDashboard.route) {
                        popUpTo(Screen.RoleSelect.route) { inclusive = true }
                    }
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
                onBookingRequests = {
                    navController.navigate(Screen.BookingRequests.createRoute("all"))
                },
                onBack = {
                    authViewModel.signOut()
                    navController.navigate(Screen.RoleSelect.route) {
                        popUpTo(0) { inclusive = true }
                    }
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
                onBack            = { navController.popBackStack() },
                onViewRequests    = { routeId ->
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
                onBack = {
                    authViewModel.signOut()
                    navController.navigate(Screen.RoleSelect.route) {
                        popUpTo(0) { inclusive = true }
                    }
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
                onBack       = { navController.popBackStack() },
                onTrackTrip  = { bookingId ->
                    navController.navigate(Screen.TripProgress.createRoute(bookingId))
                },
                onRateTrip   = { bookingId, driverId ->
                    navController.navigate(Screen.RateReview.createRoute(bookingId, driverId))
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
                onRateTrip = { bId, dId ->
                    navController.navigate(Screen.RateReview.createRoute(bId, dId))
                }
            )
        }

        composable(Screen.RateReview.route) { backStack ->
            val bookingId = backStack.arguments?.getString("bookingId") ?: ""
            val driverId  = backStack.arguments?.getString("driverId")  ?: ""
            RateReviewScreen(
                bookingId = bookingId,
                driverId  = driverId,
                onBack    = { navController.popBackStack() },
                onDone    = {
                    navController.navigate(Screen.PassengerDashboard.route) {
                        popUpTo(Screen.PassengerDashboard.route) { inclusive = false }
                    }
                }
            )
        }
    }
}