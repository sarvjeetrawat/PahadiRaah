package com.kunpitech.pahadiraah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.pahadiraah.data.model.ActionResult
import com.kunpitech.pahadiraah.data.model.NewVehicle
import com.kunpitech.pahadiraah.data.repository.AuthRepository
import com.kunpitech.pahadiraah.data.repository.ProfileFieldsUpdate
import com.kunpitech.pahadiraah.data.repository.UserRepository
import com.kunpitech.pahadiraah.data.repository.VehicleFieldsUpdate
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

    /**
     * Save profile fields + vehicle (driver only).
     *
     * @param existingVehicleId pass the current vehicle's id if one already
     *   exists so we UPDATE instead of INSERT a duplicate row.
     *   Pass null (default) on first-time setup (ProfileCompletionScreen).
     */
    fun saveProfile(
        name:              String,
        emoji:             String,
        bio:               String?,
        languages:         List<String>,
        speciality:        String?,
        isDriver:          Boolean,
        vehicleType:       String  = "suv",
        vehicleModel:      String  = "",
        regNumber:         String  = "",
        seatCapacity:      Int     = 4,
        existingVehicleId: String? = null   // null → INSERT, non-null → UPDATE
    ) {
        val userId = authRepo.currentUserId() ?: run {
            _saveResult.value = ActionResult.Error("Not logged in")
            return
        }

        viewModelScope.launch {
            _saveResult.value = ActionResult.Loading

            // ── Step 1: Ensure public.users row exists (handles auth trigger delay)
            var userExists = false
            repeat(5) { attempt ->
                if (userRepo.getMyProfile(userId).isSuccess) {
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

            // ── Step 2: Update profile fields in public.users ─────────────────
            userRepo.updateProfile(
                userId  = userId,
                updates = ProfileFieldsUpdate(
                    name       = name,
                    emoji      = emoji,
                    role       = if (isDriver) "driver" else "passenger",
                    bio        = bio,
                    languages  = languages,
                    speciality = speciality
                )
            ).onFailure { e ->
                _saveResult.value = ActionResult.Error(e.message ?: "Failed to save profile")
                return@launch
            }

            // ── Step 3: Vehicle (driver only) ─────────────────────────────────
            if (isDriver && vehicleModel.isNotBlank() && regNumber.isNotBlank()) {

                if (existingVehicleId != null) {
                    // UPDATE — vehicle row already exists, just patch it
                    vehicleRepo.updateVehicle(
                        vehicleId = existingVehicleId,
                        updates   = VehicleFieldsUpdate(
                            type         = vehicleType,
                            model        = vehicleModel,
                            regNumber    = regNumber,
                            seatCapacity = seatCapacity
                        )
                    ).onFailure { e ->
                        _saveResult.value = ActionResult.Error(
                            "Profile saved but vehicle update failed: ${e.message}"
                        )
                        return@launch
                    }
                } else {
                    // INSERT — first time registering a vehicle
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
            }

            _saveResult.value = ActionResult.Success
        }
    }

    fun resetSaveResult() {
        _saveResult.value = ActionResult.Idle
    }
}