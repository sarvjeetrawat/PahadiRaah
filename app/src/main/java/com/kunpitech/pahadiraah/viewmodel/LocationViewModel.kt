package com.kunpitech.pahadiraah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.pahadiraah.data.model.LocationDto
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.data.model.UpsertLocation
import com.kunpitech.pahadiraah.data.repository.AuthRepository
import com.kunpitech.pahadiraah.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepo: LocationRepository,
    private val authRepo:     AuthRepository
) : ViewModel() {

    // ── Latest known location (loaded once on screen open) ────────────────────
    private val _location = MutableStateFlow<UiState<LocationDto?>>(UiState.Idle)
    val location: StateFlow<UiState<LocationDto?>> = _location.asStateFlow()

    fun loadLatestLocation(tripId: String) {
        viewModelScope.launch {
            _location.value = UiState.Loading
            locationRepo.getLatestLocation(tripId)
                .onSuccess { _location.value = UiState.Success(it) }
                .onFailure { _location.value = UiState.Error(it.message ?: "Failed") }
        }
    }

    // ── Realtime live location (passenger's TripProgress) ─────────────────────
    private val _liveLocation = MutableStateFlow<LocationDto?>(null)
    val liveLocation: StateFlow<LocationDto?> = _liveLocation.asStateFlow()

    private var listenJob: Job? = null

    fun startListeningLocation(tripId: String) {
        listenJob?.cancel()
        listenJob = viewModelScope.launch {
            locationRepo.listenForLocation(tripId)
                .collect { loc -> _liveLocation.value = loc }
        }
    }

    fun stopListeningLocation() {
        listenJob?.cancel()
        listenJob = null
    }

    // ── Driver sends GPS update ───────────────────────────────────────────────
    /**
     * Called by a foreground service or directly from the driver's ActiveRoutes
     * screen every ~15 seconds with the device's current GPS coordinates.
     */
    fun sendLocation(
        tripId:     String,
        lat:        Double,
        lng:        Double,
        speedKmh:   Float  = 0f,
        headingDeg: Float? = null
    ) {
        val uid = authRepo.currentUserId() ?: return
        viewModelScope.launch {
            locationRepo.upsertLocation(
                UpsertLocation(
                    tripId     = tripId,
                    driverId   = uid,
                    lat        = lat,
                    lng        = lng,
                    speedKmh   = speedKmh,
                    headingDeg = headingDeg
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenJob?.cancel()
    }
}