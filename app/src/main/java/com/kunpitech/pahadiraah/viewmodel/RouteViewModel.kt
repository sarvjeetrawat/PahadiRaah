package com.kunpitech.pahadiraah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.pahadiraah.data.model.*
import com.kunpitech.pahadiraah.data.repository.AuthRepository
import com.kunpitech.pahadiraah.data.repository.RouteRepository
import com.kunpitech.pahadiraah.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

fun arrivalDateTime(date: String, time: String, durationHrs: String): LocalDateTime? {
    return try {
        val hrs = durationHrs.trim().toDoubleOrNull() ?: return null
        val depDate  = LocalDate.parse(date.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        val parts    = time.trim().split(":").map { it.toIntOrNull() ?: 0 }
        val depTime  = LocalTime.of(parts.getOrElse(0){0}, parts.getOrElse(1){0}, parts.getOrElse(2){0})
        LocalDateTime.of(depDate, depTime).plusMinutes((hrs * 60).toLong())
    } catch (e: Exception) { null }
}

fun arrivalLabel(date: String, time: String, durationHrs: String): String? {
    val arr = arrivalDateTime(date, time, durationHrs) ?: return null
    val dep = try { LocalDate.parse(date.trim()) } catch (e: Exception) { return null }
    val fmt = DateTimeFormatter.ofPattern("h:mm a")
    return if (arr.toLocalDate().isAfter(dep)) "${arr.format(fmt)} (next day)" else arr.format(fmt)
}

fun resolvedStatus(route: RouteDto, now: LocalDateTime = LocalDateTime.now()): String {
    if (route.status == "cancelled") return "cancelled"
    return try {
        val depDate  = LocalDate.parse(route.date.trim())
        val parts    = route.time.trim().split(":").map { it.toIntOrNull() ?: 0 }
        val depTime  = LocalTime.of(parts.getOrElse(0){0}, parts.getOrElse(1){0}, parts.getOrElse(2){0})
        val depDt = LocalDateTime.of(depDate, depTime)
        val arrDt = arrivalDateTime(route.date, route.time, route.durationHrs)
        when {
            now.isBefore(depDt) -> "upcoming"
            arrDt != null -> if (now.isBefore(arrDt)) "ongoing" else "completed"
            depDate.isBefore(now.toLocalDate()) -> "completed"
            else -> "ongoing"
        }
    } catch (e: Exception) { route.status }
}

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val routeRepo: RouteRepository,
    private val authRepo:  AuthRepository,
    private val userRepo:  UserRepository
) : ViewModel() {

    // ── Search (Passenger) ────────────────────────────────────────────────────
    private val _searchState = MutableStateFlow<UiState<List<RouteDto>>>(UiState.Idle)
    val searchState: StateFlow<UiState<List<RouteDto>>> = _searchState.asStateFlow()

    private val _originQuery = MutableStateFlow("")
    private val _destQuery   = MutableStateFlow("")
    private val _dateQuery   = MutableStateFlow("")
    private val _minSeats    = MutableStateFlow(1)

    val originQuery: StateFlow<String> = _originQuery.asStateFlow()
    val destQuery:   StateFlow<String> = _destQuery.asStateFlow()
    val dateQuery:   StateFlow<String> = _dateQuery.asStateFlow()
    val minSeats:    StateFlow<Int>    = _minSeats.asStateFlow()

    // ── Autocomplete suggestions ──────────────────────────────────────────────
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

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

    fun setOrigin(value: String) { _originQuery.value = value }
    fun setDest(value: String)   { _destQuery.value   = value }
    fun setMinSeats(value: Int)  { _minSeats.value    = value }

    fun searchRoutes(origin: String = "", dest: String = "", date: String = "", minSeats: Int = 1) {
        _originQuery.value = origin
        _destQuery.value   = dest
        _dateQuery.value   = date
        _minSeats.value    = minSeats
    }

    fun suggestPlaces(query: String) {
        viewModelScope.launch {
            routeRepo.suggestPlaces(query)
                .onSuccess { _suggestions.value = it }
                .onFailure { _suggestions.value = emptyList() }
        }
    }

    fun clearSuggestions() { _suggestions.value = emptyList() }

    private fun doSearch(origin: String, dest: String, minSeats: Int) {
        viewModelScope.launch {
            _searchState.value = UiState.Loading
            routeRepo.searchRoutes(origin, dest, _dateQuery.value, minSeats)
                .onSuccess { routes ->
                    val now = LocalDateTime.now()
                    val bookable = routes.filter { route ->
                        try {
                            val depDate  = LocalDate.parse(route.date)
                            val parts    = route.time.split(":").map { it.toIntOrNull() ?: 0 }
                            val depTime  = LocalTime.of(parts.getOrElse(0){0}, parts.getOrElse(1){0}, parts.getOrElse(2){0})
                            LocalDateTime.of(depDate, depTime).isAfter(now)
                        } catch (e: Exception) { false }
                    }
                    _searchState.value = UiState.Success(bookable)
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
                .onSuccess { routes -> _myRoutes.value = UiState.Success(autoTransitionStatuses(routes)) }
                .onFailure { _myRoutes.value = UiState.Error(it.message ?: "Failed to load routes") }
        }
    }

    // ── Driver — Active Routes ────────────────────────────────────────────────
    private val _activeRoutes = MutableStateFlow<UiState<List<RouteDto>>>(UiState.Idle)
    val activeRoutes: StateFlow<UiState<List<RouteDto>>> = _activeRoutes.asStateFlow()

    fun loadActiveRoutes() {
        val uid = authRepo.currentUserId() ?: return
        viewModelScope.launch {
            _activeRoutes.value = UiState.Loading
            routeRepo.getActiveRoutes(uid)
                .onSuccess { routes -> _activeRoutes.value = UiState.Success(autoTransitionStatuses(routes)) }
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
                .onSuccess { routes ->
                    val now = LocalDateTime.now()
                    val upcoming = routes.filter { route ->
                        try {
                            val depDate = LocalDate.parse(route.date)
                            val parts   = route.time.split(":").map { it.toIntOrNull() ?: 0 }
                            val depTime = LocalTime.of(parts.getOrElse(0){0}, parts.getOrElse(1){0}, parts.getOrElse(2){0})
                            LocalDateTime.of(depDate, depTime).isAfter(now)
                        } catch (e: Exception) { false }
                    }
                    _driverUpcoming.value = UiState.Success(upcoming)
                }
                .onFailure { _driverUpcoming.value = UiState.Error(it.message ?: "Failed") }
        }
    }

    // ── Post Route ────────────────────────────────────────────────────────────
    private val _postResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val postResult: StateFlow<ActionResult> = _postResult.asStateFlow()

    fun postRoute(
        origin:      String,
        destination: String,
        date:        String,
        time:        String,
        durationHrs: String,
        seatsTotal:  Int,
        farePerSeat: Int,
        vehicleId:   String? = null
    ) {
        val uid = authRepo.currentUserId() ?: run {
            _postResult.value = ActionResult.Error("Not logged in")
            return
        }
        viewModelScope.launch {
            _postResult.value = ActionResult.Loading
            routeRepo.postRoute(
                NewRoute(
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
            )
                .onSuccess { _postResult.value = ActionResult.Success }
                .onFailure { _postResult.value = ActionResult.Error(it.message ?: "Failed to post route") }
        }
    }

    fun resetPostResult() { _postResult.value = ActionResult.Idle }

    // ── Update / Cancel ───────────────────────────────────────────────────────
    fun updateRouteStatus(routeId: String, status: String) {
        viewModelScope.launch {
            routeRepo.updateRouteStatus(routeId, status)
            loadActiveRoutes()
        }
    }

    fun cancelRoute(routeId: String) {
        viewModelScope.launch {
            fun patch(s: UiState<List<RouteDto>>) =
                if (s is UiState.Success) UiState.Success(s.data.filter { it.id != routeId }) else s
            _activeRoutes.value = patch(_activeRoutes.value)
            _myRoutes.value     = patch(_myRoutes.value)
            routeRepo.cancelRoute(routeId)
        }
    }

    // ── Total Trips ───────────────────────────────────────────────────────────
    private val _totalTrips = MutableStateFlow<Int?>(null)
    val totalTrips: StateFlow<Int?> = _totalTrips.asStateFlow()

    private fun refreshTotalTrips() {
        val uid = authRepo.currentUserId() ?: return
        viewModelScope.launch {
            userRepo.getMyProfile(uid)
                .onSuccess { _totalTrips.value = it.totalTrips }
        }
    }

    // ── Status Auto-Transition ────────────────────────────────────────────────
    private suspend fun autoTransitionStatuses(routes: List<RouteDto>): List<RouteDto> {
        val now = LocalDateTime.now()
        var anyNewlyCompleted = false
        val updated = routes.map { route ->
            val correct = resolvedStatus(route, now)
            if (correct != route.status) {
                routeRepo.updateRouteStatus(route.id, correct)
                if (correct == "completed") anyNewlyCompleted = true
                route.copy(status = correct)
            } else route
        }
        if (anyNewlyCompleted) refreshTotalTrips()
        return updated
    }
}