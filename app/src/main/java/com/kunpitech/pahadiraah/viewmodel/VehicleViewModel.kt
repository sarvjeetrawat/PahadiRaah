package com.kunpitech.pahadiraah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.pahadiraah.data.model.ActionResult
import com.kunpitech.pahadiraah.data.model.NewVehicle
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.data.model.VehicleDto
import com.kunpitech.pahadiraah.data.repository.AuthRepository
import com.kunpitech.pahadiraah.data.repository.VehicleFieldsUpdate
import com.kunpitech.pahadiraah.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val vehicleRepo: VehicleRepository,
    private val authRepo:    AuthRepository
) : ViewModel() {

    // ── My vehicle (driver's own vehicle) ─────────────────────────────────────
    private val _myVehicle = MutableStateFlow<UiState<VehicleDto?>>(UiState.Idle)
    val myVehicle: StateFlow<UiState<VehicleDto?>> = _myVehicle.asStateFlow()

    // ── Save result ───────────────────────────────────────────────────────────
    private val _saveResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val saveResult: StateFlow<ActionResult> = _saveResult.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    //  LOAD
    // ─────────────────────────────────────────────────────────────────────────

    fun loadMyVehicle(driverId: String) {
        viewModelScope.launch {
            _myVehicle.value = UiState.Loading
            vehicleRepo.getDriverVehicle(driverId)
                .onSuccess { _myVehicle.value = UiState.Success(it) }
                .onFailure { _myVehicle.value = UiState.Error(it.message ?: "Failed to load vehicle") }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SAVE — creates or updates vehicle
    // ─────────────────────────────────────────────────────────────────────────

    fun saveVehicle(
        type:         String,
        model:        String,
        regNumber:    String,
        seatCapacity: Int
    ) {
        val uid = authRepo.currentUserId() ?: run {
            _saveResult.value = ActionResult.Error("Not logged in")
            return
        }

        viewModelScope.launch {
            _saveResult.value = ActionResult.Loading

            val existing = (myVehicle.value as? UiState.Success<VehicleDto?>)?.data

            if (existing != null) {
                // Update existing vehicle
                vehicleRepo.updateVehicle(
                    vehicleId = existing.id,
                    updates   = VehicleFieldsUpdate(
                        type         = type,
                        model        = model,
                        regNumber    = regNumber,
                        seatCapacity = seatCapacity
                    )
                )
                    .onSuccess { _saveResult.value = ActionResult.Success }
                    .onFailure { _saveResult.value = ActionResult.Error(it.message ?: "Failed to update vehicle") }
            } else {
                // Insert new vehicle
                vehicleRepo.addVehicle(
                    NewVehicle(
                        driverId     = uid,
                        type         = type,
                        model        = model,
                        regNumber    = regNumber,
                        seatCapacity = seatCapacity
                    )
                )
                    .onSuccess { _saveResult.value = ActionResult.Success }
                    .onFailure { _saveResult.value = ActionResult.Error(it.message ?: "Failed to register vehicle") }
            }
        }
    }

    fun resetSaveResult() {
        _saveResult.value = ActionResult.Idle
    }
}