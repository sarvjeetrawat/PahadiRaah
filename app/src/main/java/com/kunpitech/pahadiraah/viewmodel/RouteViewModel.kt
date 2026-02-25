package com.kunpitech.pahadiraah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.pahadiraah.data.model.*
import com.kunpitech.pahadiraah.data.repository.AuthRepository
import com.kunpitech.pahadiraah.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val routeRepo: RouteRepository,
    private val authRepo:  AuthRepository
) : ViewModel() {

    // ── Search (Passenger) ────────────────────────────────────────────────────
    private val _searchState = MutableStateFlow<UiState<List<RouteDto>>>(UiState.Idle)
    val searchState: StateFlow<UiState<List<RouteDto>>> = _searchState.asStateFlow()

    // Keep raw query strings so we can re-search on filter change
    private val _originQuery = MutableStateFlow("")
    private val _destQuery   = MutableStateFlow("")
    private val _minSeats    = MutableStateFlow(1)

    val originQuery: StateFlow<String> = _originQuery.asStateFlow()
    val destQuery:   StateFlow<String> = _destQuery.asStateFlow()
    val minSeats:    StateFlow<Int>    = _minSeats.asStateFlow()

    // Auto-search whenever any query field changes (debounced 400ms)
    @OptIn(FlowPreview::class)
    private val searchTrigger = combine(_originQuery, _destQuery, _minSeats) {
            o, d, s -> Triple(o, d, s)
    }.debounce(400)

    init {
        viewModelScope.launch {
            searchTrigger.collect { (origin, dest, seats) ->
                doSearch(origin, dest, seats)
            }
        }
    }

    fun setOrigin(value: String)   { _originQuery.value = value }
    fun setDest(value: String)     { _destQuery.value   = value }
    fun setMinSeats(value: Int)    { _minSeats.value    = value }

    fun searchRoutes(origin: String = "", dest: String = "", minSeats: Int = 1) {
        _originQuery.value = origin
        _destQuery.value   = dest
        _minSeats.value    = minSeats
    }

    private fun doSearch(origin: String, dest: String, minSeats: Int) {
        viewModelScope.launch {
            _searchState.value = UiState.Loading
            routeRepo.searchRoutes(origin, dest, minSeats)
                .onSuccess { routes ->
                    // Filter out routes whose departure datetime has already passed
                    val now = LocalDateTime.now()
                    val upcoming = routes.filter { route ->
                        try {
                            val routeDate = LocalDate.parse(route.date)
                            val timeParts = route.time.split(":").map { it.toIntOrNull() ?: 0 }
                            val routeTime = LocalTime.of(
                                timeParts.getOrElse(0) { 0 },
                                timeParts.getOrElse(1) { 0 }
                            )
                            !LocalDateTime.of(routeDate, routeTime).isBefore(now)
                        } catch (e: Exception) { true }
                    }
                    _searchState.value = UiState.Success(upcoming)
                }
                .onFailure { _searchState.value = UiState.Error(it.message ?: "Search failed") }
        }
    }

    // ── Single Route (for BookingConfirm) ─────────────────────────────────────
    private val _selectedRoute = MutableStateFlow<UiState<RouteDto>>(UiState.Idle)
    val selectedRoute: StateFlow<UiState<RouteDto>> = _selectedRoute.asStateFlow()

    fun loadRoute(routeId: String) {
        viewModelScope.launch {
            _selectedRoute.value = UiState.Loading
            routeRepo.getRouteById(routeId)
                .onSuccess { _selectedRoute.value = UiState.Success(it) }
                .onFailure { _selectedRoute.value = UiState.Error(it.message ?: "Failed to load route") }
        }
    }

    // ── Driver — My Routes ────────────────────────────────────────────────────
    private val _myRoutes = MutableStateFlow<UiState<List<RouteDto>>>(UiState.Idle)
    val myRoutes: StateFlow<UiState<List<RouteDto>>> = _myRoutes.asStateFlow()

    fun loadMyRoutes() {
        val uid = authRepo.currentUserId() ?: return
        viewModelScope.launch {
            _myRoutes.value = UiState.Loading
            routeRepo.getMyRoutes(uid)
                .onSuccess { routes ->
                    val now   = LocalDateTime.now()
                    val today = now.toLocalDate()
                    val updated = routes.map { route ->
                        try {
                            val routeDate = LocalDate.parse(route.date)
                            val timeParts = route.time.split(":").map { it.toIntOrNull() ?: 0 }
                            val routeTime = LocalTime.of(
                                timeParts.getOrElse(0) { 0 },
                                timeParts.getOrElse(1) { 0 },
                                timeParts.getOrElse(2) { 0 }
                            )
                            val departureDt = LocalDateTime.of(routeDate, routeTime)
                            when {
                                route.status == "upcoming" && departureDt.isBefore(now) &&
                                        !routeDate.isBefore(today) -> {
                                    routeRepo.updateRouteStatus(route.id, "ongoing")
                                    route.copy(status = "ongoing")
                                }
                                (route.status == "upcoming" || route.status == "ongoing") &&
                                        routeDate.isBefore(today) -> {
                                    routeRepo.updateRouteStatus(route.id, "completed")
                                    route.copy(status = "completed")
                                }
                                else -> route
                            }
                        } catch (e: Exception) { route }
                    }
                    _myRoutes.value = UiState.Success(updated)
                }
                .onFailure { _myRoutes.value = UiState.Error(it.message ?: "Failed to load routes") }
        }
    }

    // ── Driver — Active Routes ─────────────────────────────────────────────────
    private val _activeRoutes = MutableStateFlow<UiState<List<RouteDto>>>(UiState.Idle)
    val activeRoutes: StateFlow<UiState<List<RouteDto>>> = _activeRoutes.asStateFlow()

    fun loadActiveRoutes() {
        val uid = authRepo.currentUserId() ?: return
        viewModelScope.launch {
            _activeRoutes.value = UiState.Loading
            routeRepo.getActiveRoutes(uid)
                .onSuccess { routes ->
                    val now   = LocalDateTime.now()
                    val today = now.toLocalDate()

                    val updated = routes.map { route ->
                        try {
                            val routeDate = LocalDate.parse(route.date)
                            // Parse time — stored as "HH:mm:ss" or "HH:mm"
                            val timeParts = route.time.split(":").map { it.toIntOrNull() ?: 0 }
                            val routeTime = LocalTime.of(
                                timeParts.getOrElse(0) { 0 },
                                timeParts.getOrElse(1) { 0 },
                                timeParts.getOrElse(2) { 0 }
                            )
                            val departureDt = LocalDateTime.of(routeDate, routeTime)

                            when {
                                // Past the departure time today or on a past date → ongoing
                                route.status == "upcoming" && departureDt.isBefore(now) &&
                                        !routeDate.isBefore(today) -> {
                                    routeRepo.updateRouteStatus(route.id, "ongoing")
                                    route.copy(status = "ongoing")
                                }
                                // Route date fully in the past → completed
                                (route.status == "upcoming" || route.status == "ongoing") &&
                                        routeDate.isBefore(today) -> {
                                    routeRepo.updateRouteStatus(route.id, "completed")
                                    route.copy(status = "completed")
                                }
                                else -> route
                            }
                        } catch (e: Exception) { route }
                    }
                    _activeRoutes.value = UiState.Success(updated)
                }
                .onFailure { _activeRoutes.value = UiState.Error(it.message ?: "Failed to load active routes") }
        }
    }

    // ── Driver — Upcoming Routes for Profile ──────────────────────────────────
    private val _driverUpcoming = MutableStateFlow<UiState<List<RouteDto>>>(UiState.Idle)
    val driverUpcoming: StateFlow<UiState<List<RouteDto>>> = _driverUpcoming.asStateFlow()

    fun loadDriverUpcomingRoutes(driverId: String) {
        viewModelScope.launch {
            _driverUpcoming.value = UiState.Loading
            routeRepo.getDriverUpcomingRoutes(driverId)
                .onSuccess { _driverUpcoming.value = UiState.Success(it) }
                .onFailure { _driverUpcoming.value = UiState.Error(it.message ?: "Failed") }
        }
    }

    // ── Post Route ─────────────────────────────────────────────────────────────
    private val _postResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val postResult: StateFlow<ActionResult> = _postResult.asStateFlow()

    fun postRoute(
        origin:      String,
        destination: String,
        date:        String,   // "2025-06-22"
        time:        String,   // "06:00:00"
        durationHrs: String,
        seatsTotal:  Int,
        farePerSeat: Int,
        vehicleId:   String?   = null
    ) {
        val uid = authRepo.currentUserId() ?: run {
            _postResult.value = ActionResult.Error("Not logged in")
            return
        }
        viewModelScope.launch {
            _postResult.value = ActionResult.Loading
            val newRoute = NewRoute(
                driverId    = uid,
                vehicleId   = vehicleId,
                origin      = origin,
                destination = destination,
                date        = date,
                time        = time,
                durationHrs = durationHrs,
                seatsTotal  = seatsTotal,
                seatsLeft   = seatsTotal,
                farePerSeat = farePerSeat
            )
            routeRepo.postRoute(newRoute)
                .onSuccess { _postResult.value = ActionResult.Success }
                .onFailure { _postResult.value = ActionResult.Error(it.message ?: "Failed to post route") }
        }
    }

    fun resetPostResult() { _postResult.value = ActionResult.Idle }

    // ── Update / Cancel Route ─────────────────────────────────────────────────
    fun updateRouteStatus(routeId: String, status: String) {
        viewModelScope.launch {
            routeRepo.updateRouteStatus(routeId, status)
            loadActiveRoutes()   // refresh list
        }
    }

    fun cancelRoute(routeId: String) {
        viewModelScope.launch {
            // Remove from in-memory list immediately for instant UI feedback
            val current = _activeRoutes.value
            if (current is UiState.Success) {
                _activeRoutes.value = UiState.Success(
                    current.data.filter { it.id != routeId }
                )
            }
            routeRepo.cancelRoute(routeId)
        }
    }
}