package com.kunpitech.pahadiraah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.pahadiraah.data.model.*
import com.kunpitech.pahadiraah.data.repository.AuthRepository
import com.kunpitech.pahadiraah.data.repository.ReviewRepository
import com.kunpitech.pahadiraah.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepo: ReviewRepository,
    private val routeRepo:  RouteRepository,
    private val authRepo:   AuthRepository
) : ViewModel() {

    // ── Driver Reviews (DriverProfileScreen) ──────────────────────────────────
    private val _reviews = MutableStateFlow<UiState<List<ReviewDto>>>(UiState.Idle)
    val reviews: StateFlow<UiState<List<ReviewDto>>> = _reviews.asStateFlow()

    fun loadDriverReviews(driverId: String) {
        viewModelScope.launch {
            _reviews.value = UiState.Loading
            reviewRepo.getDriverReviews(driverId)
                .onSuccess { _reviews.value = UiState.Success(it) }
                .onFailure { _reviews.value = UiState.Error(it.message ?: "Failed to load reviews") }
        }
    }

    // ── Submit Review (RateReviewScreen) ──────────────────────────────────────
    private val _submitResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val submitResult: StateFlow<ActionResult> = _submitResult.asStateFlow()

    fun submitReview(
        bookingId:    String,
        routeId:      String,   // used to resolve driverId from DB when hint is blank
        driverIdHint: String,   // pass whatever is already known — may be blank
        overallRating: Int,
        aspectRatings: Map<String, Int>,
        tags:          List<String>,
        comment:       String
    ) {
        val uid = authRepo.currentUserId() ?: run {
            _submitResult.value = ActionResult.Error("Not logged in")
            return
        }
        viewModelScope.launch {
            _submitResult.value = ActionResult.Loading

            // Resolve driverId — use hint if valid, otherwise fetch route from DB
            val driverId = when {
                driverIdHint.isNotBlank() -> driverIdHint
                routeId.isNotBlank()      -> routeRepo.getRouteById(routeId)
                    .getOrNull()?.driverId ?: ""
                else                      -> ""
            }

            if (driverId.isBlank()) {
                _submitResult.value = ActionResult.Error("Could not identify driver. Please try again.")
                return@launch
            }

            reviewRepo.submitReview(
                NewReview(
                    bookingId     = bookingId,
                    driverId      = driverId,
                    reviewerId    = uid,
                    overallRating = overallRating,
                    aspectRatings = aspectRatings,
                    tags          = tags,
                    comment       = comment
                )
            )
                .onSuccess { _submitResult.value = ActionResult.Success }
                .onFailure { _submitResult.value = ActionResult.Error(it.message ?: "Failed to submit") }
        }
    }

    fun resetSubmitResult() { _submitResult.value = ActionResult.Idle }
}