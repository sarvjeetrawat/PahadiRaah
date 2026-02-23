package com.kunpitech.pahadiraah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.pahadiraah.data.model.ActionResult
import com.kunpitech.pahadiraah.data.model.NewVehicle
import com.kunpitech.pahadiraah.data.repository.AuthRepository
import com.kunpitech.pahadiraah.data.repository.ProfileFieldsUpdate
import com.kunpitech.pahadiraah.data.repository.UserRepository
import com.kunpitech.pahadiraah.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo:    AuthRepository,
    private val userRepo:    UserRepository,
    private val vehicleRepo: VehicleRepository
) : ViewModel() {

    private val _saveResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val saveResult: StateFlow<ActionResult> = _saveResult.asStateFlow()

    fun saveProfile(
        name:         String,
        emoji:        String,
        bio:          String?,
        languages:    List<String>,
        speciality:   String?,
        isDriver:     Boolean,
        vehicleType:  String = "suv",
        vehicleModel: String = "",
        regNumber:    String = "",
        seatCapacity: Int    = 4
    ) {
        val userId = authRepo.currentUserId() ?: run {
            _saveResult.value = ActionResult.Error("Not logged in")
            return
        }

        viewModelScope.launch {
            _saveResult.value = ActionResult.Loading

            // ── Step 1: Ensure the public.users row exists ─────────────────────
            // The Supabase trigger creates it on auth.users insert, but it may
            // take a moment. Retry up to 5 times with 500ms delay.
            var userExists = false
            repeat(5) { attempt ->
                val result = userRepo.getMyProfile(userId)
                if (result.isSuccess) {
                    userExists = true
                    return@repeat
                }
                if (attempt < 4) delay(500)
            }

            if (!userExists) {
                _saveResult.value = ActionResult.Error(
                    "Account setup is still in progress. Please wait a moment and try again."
                )
                return@launch
            }

            // ── Step 2: Update public.users with profile fields ────────────────
            userRepo.updateProfile(
                userId  = userId,
                updates = ProfileFieldsUpdate(
                    name       = name,
                    emoji      = emoji,
                    role       = if (isDriver) "driver" else "passenger",  // ← ADD THIS
                    bio        = bio,
                    languages  = languages,
                    speciality = speciality
                )
            ).onFailure { e ->
                _saveResult.value = ActionResult.Error(
                    e.message ?: "Failed to save profile"
                )
                return@launch
            }

            // ── Step 3: Insert vehicle row if driver ───────────────────────────
            if (isDriver && vehicleModel.isNotBlank() && regNumber.isNotBlank()) {
                vehicleRepo.addVehicle(
                    NewVehicle(
                        driverId     = userId,
                        type         = vehicleType,
                        model        = vehicleModel,
                        regNumber    = regNumber,
                        seatCapacity = seatCapacity
                    )
                ).onFailure { e ->
                    _saveResult.value = ActionResult.Error(
                        "Profile saved but vehicle registration failed: ${e.message}"
                    )
                    return@launch
                }
            }

            _saveResult.value = ActionResult.Success
        }
    }

    fun resetSaveResult() {
        _saveResult.value = ActionResult.Idle
    }
}