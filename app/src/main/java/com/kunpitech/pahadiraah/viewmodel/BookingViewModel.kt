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
        viewModelScope.launch {
            _routeBookings.value = UiState.Loading
            bookingRepo.getBookingsForRoute(routeId)
                .onSuccess { _routeBookings.value = UiState.Success(it) }
                .onFailure { _routeBookings.value = UiState.Error(it.message ?: "Failed to load") }
        }
    }

    // ── All Bookings for Driver (across all routes) ───────────────────────────
    fun loadDriverBookings() {
        val uid = authRepo.currentUserId() ?: return
        viewModelScope.launch {
            _routeBookings.value = UiState.Loading
            bookingRepo.getDriverBookings(uid)
                .onSuccess { _routeBookings.value = UiState.Success(it) }
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
                .onFailure {
                    _confirmResult.value = ActionResult.Error(
                        it.message ?: "Booking failed — seats may no longer be available"
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
            loadBookingsForRoute(routeId)
        }
    }

    fun declineBooking(bookingId: String, routeId: String) {
        viewModelScope.launch {
            bookingRepo.updateBookingStatus(bookingId, "cancelled")
            loadBookingsForRoute(routeId)
        }
    }

    // ── Mark Paid (Driver — cash) ─────────────────────────────────────────────
    fun markPaid(bookingId: String, routeId: String) {
        viewModelScope.launch {
            bookingRepo.markPaid(bookingId)
            loadBookingsForRoute(routeId)
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
                    loadBookingsForRoute(routeId)
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