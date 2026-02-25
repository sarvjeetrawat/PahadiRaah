package com.kunpitech.pahadiraah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.pahadiraah.data.model.*
import com.kunpitech.pahadiraah.data.repository.AuthRepository
import com.kunpitech.pahadiraah.data.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepo: BookingRepository,
    private val authRepo:    AuthRepository
) : ViewModel() {

    // ── Action Error (accept/decline failures) ────────────────────────────────
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()
    fun clearActionError() { _actionError.value = null }

    // ── My Bookings (Passenger) ────────────────────────────────────────────────
    private val _myBookings = MutableStateFlow<UiState<List<BookingDto>>>(UiState.Idle)
    val myBookings: StateFlow<UiState<List<BookingDto>>> = _myBookings.asStateFlow()

    fun loadMyBookings() {
        val uid = authRepo.currentUserId() ?: return
        viewModelScope.launch {
            _myBookings.value = UiState.Loading
            bookingRepo.getMyBookings(uid)
                .onSuccess { _myBookings.value = UiState.Success(it) }
                .onFailure { _myBookings.value = UiState.Error(it.message ?: "Failed to load bookings") }
        }
    }

    // ── Bookings on a Route (Driver) ──────────────────────────────────────────
    private val _routeBookings = MutableStateFlow<UiState<List<BookingDto>>>(UiState.Idle)
    val routeBookings: StateFlow<UiState<List<BookingDto>>> = _routeBookings.asStateFlow()

    fun loadBookingsForRoute(routeId: String) {
        // Safety guard — "all" or blank is a sentinel value, never a real UUID
        if (routeId == "all" || routeId.isBlank()) { loadDriverBookings(); return }
        viewModelScope.launch {
            _routeBookings.value = UiState.Loading
            bookingRepo.getBookingsForRoute(routeId)
                .onSuccess { _routeBookings.value = UiState.Success(it) }
                .onFailure { _routeBookings.value = UiState.Error(it.message ?: "Failed to load") }
        }
    }

    // ── All Bookings for Driver (across all routes) ───────────────────────────
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    fun loadDriverBookings() {
        val uid = authRepo.currentUserId() ?: return
        viewModelScope.launch {
            _routeBookings.value = UiState.Loading
            bookingRepo.getDriverBookings(uid)
                .onSuccess { list ->
                    _routeBookings.value = UiState.Success(list)
                    // Update pending count separately so DriverDashboard can read it
                    // without touching routeBookings StateFlow
                    _pendingCount.value = list.count { it.status == "pending" }
                }
                .onFailure { _routeBookings.value = UiState.Error(it.message ?: "Failed to load bookings") }
        }
    }

    // ── Confirm Booking (Passenger) ────────────────────────────────────────────
    private val _confirmResult   = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val confirmResult: StateFlow<ActionResult> = _confirmResult.asStateFlow()

    // Holds the booking reference returned after successful confirm
    private val _confirmedRef    = MutableStateFlow<String?>(null)
    val confirmedRef: StateFlow<String?> = _confirmedRef.asStateFlow()

    fun confirmBooking(
        routeId:       String,
        seats:         Int,
        farePerSeat:   Int,
        paymentMethod: String = "cash"
    ) {
        val uid = authRepo.currentUserId() ?: run {
            _confirmResult.value = ActionResult.Error("Not logged in")
            return
        }
        viewModelScope.launch {
            _confirmResult.value = ActionResult.Loading

            val totalFare  = farePerSeat * seats
            val serviceFee = (totalFare * 0.05).toInt()
            val grandTotal = totalFare + serviceFee

            val newBooking = NewBooking(
                routeId       = routeId,
                passengerId   = uid,
                seats         = seats,
                totalFare     = totalFare,
                serviceFee    = serviceFee,
                grandTotal    = grandTotal,
                paymentMethod = paymentMethod
            )

            bookingRepo.confirmBooking(newBooking)
                .onSuccess { booking ->
                    _confirmedRef.value  = booking.bookingRef
                    _confirmResult.value = ActionResult.Success
                    loadMyBookings()   // refresh list immediately
                }
                .onFailure { e ->
                    val msg = when {
                        e.message?.contains("unique_passenger_route", ignoreCase = true) == true ||
                                e.message?.contains("duplicate key", ignoreCase = true) == true ->
                            "You already have an active booking on this route."
                        e.message?.contains("seats_left", ignoreCase = true) == true ->
                            "Sorry, no seats available on this route."
                        else -> e.message ?: "Booking failed — please try again."
                    }
                    _confirmResult.value = ActionResult.Error(msg
                    )
                }
        }
    }

    fun resetConfirmResult() {
        _confirmResult.value = ActionResult.Idle
        _confirmedRef.value  = null
    }

    // ── Accept / Decline (Driver) ─────────────────────────────────────────────
    fun acceptBooking(bookingId: String, routeId: String) {
        viewModelScope.launch {
            bookingRepo.updateBookingStatus(bookingId, "accepted")
                .onSuccess {
                    // Update in-memory immediately — no reload needed, avoids re-triggering error state
                    updateBookingStatusInMemory(bookingId, "accepted")
                }
                .onFailure { e ->
                    _actionError.value = e.message ?: "Failed to accept booking"
                }
        }
    }

    fun declineBooking(bookingId: String, routeId: String) {
        viewModelScope.launch {
            bookingRepo.updateBookingStatus(bookingId, "cancelled")
                .onSuccess {
                    updateBookingStatusInMemory(bookingId, "cancelled")
                }
                .onFailure { e ->
                    _actionError.value = e.message ?: "Failed to decline booking"
                }
        }
    }

    // Updates booking status in the current in-memory list for instant feedback
    private fun updateBookingStatusInMemory(bookingId: String, newStatus: String) {
        val current = _routeBookings.value
        if (current is UiState.Success) {
            _routeBookings.value = UiState.Success(
                current.data.map { if (it.id == bookingId) it.copy(status = newStatus) else it }
            )
        }
    }

    // ── Mark Paid (Driver — cash) ─────────────────────────────────────────────
    fun markPaid(bookingId: String, routeId: String) {
        viewModelScope.launch {
            bookingRepo.markPaid(bookingId)
            if (routeId == "all") loadDriverBookings() else loadBookingsForRoute(routeId)
        }
    }

    // ── Update Booking Seats (Passenger) ─────────────────────────────────────
    fun updateBooking(
        bookingId:   String,
        seats:       Int,
        farePerSeat: Int
    ) {
        viewModelScope.launch {
            _confirmResult.value = ActionResult.Loading
            val totalFare  = farePerSeat * seats
            val serviceFee = (totalFare * 0.05).toInt()
            val grandTotal = totalFare + serviceFee
            bookingRepo.updateBookingSeats(bookingId, seats, totalFare, serviceFee, grandTotal)
                .onSuccess {
                    _confirmResult.value = ActionResult.Success
                    loadMyBookings()
                }
                .onFailure { e ->
                    _confirmResult.value = ActionResult.Error(
                        e.message ?: "Failed to update booking — please try again."
                    )
                }
        }
    }

    // ── Cancel Booking (Passenger) ────────────────────────────────────────────
    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            bookingRepo.cancelBooking(bookingId)
            loadMyBookings()
        }
    }

    // ── Realtime — new bookings to driver ─────────────────────────────────────
    // Emits each newly inserted booking as it arrives.
    private val _newBookingAlert = MutableSharedFlow<BookingDto>(replay = 0, extraBufferCapacity = 10)
    val newBookingAlert: SharedFlow<BookingDto> = _newBookingAlert.asSharedFlow()

    private var realtimeJob: Job? = null

    /**
     * Call this from BookingRequestsScreen when the driver loads a route.
     * Cancels any previous subscription first.
     */
    fun subscribeToBookings(routeId: String) {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            bookingRepo.listenForNewBookings(routeId)
                .collect { booking ->
                    _newBookingAlert.emit(booking)
                    // Also refresh the full list so the card appears
                    if (routeId == "all") loadDriverBookings() else loadBookingsForRoute(routeId)
                }
        }
    }

    // ── Realtime — booking status updates to passenger ────────────────────────
    private val _bookingUpdate = MutableSharedFlow<BookingDto>(replay = 0, extraBufferCapacity = 10)
    val bookingUpdate: SharedFlow<BookingDto> = _bookingUpdate.asSharedFlow()

    private var updateJob: Job? = null

    fun subscribeToBookingUpdates(bookingId: String) {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            bookingRepo.listenForBookingUpdates(bookingId)
                .collect { _bookingUpdate.emit(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
        updateJob?.cancel()
    }
}