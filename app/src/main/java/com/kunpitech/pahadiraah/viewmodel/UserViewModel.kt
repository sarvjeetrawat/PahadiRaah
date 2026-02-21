package com.kunpitech.pahadiraah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.pahadiraah.data.model.UiState
import com.kunpitech.pahadiraah.data.model.UserDto
import com.kunpitech.pahadiraah.data.repository.AuthRepository
import com.kunpitech.pahadiraah.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    // ── My Profile ────────────────────────────────────────────────────────────
    private val _myProfile = MutableStateFlow<UiState<UserDto>>(UiState.Idle)
    val myProfile: StateFlow<UiState<UserDto>> = _myProfile.asStateFlow()

    fun loadMyProfile() {
        val uid = authRepo.currentUserId() ?: return
        viewModelScope.launch {
            _myProfile.value = UiState.Loading
            userRepo.getMyProfile(uid)
                .onSuccess { _myProfile.value = UiState.Success(it) }
                .onFailure { _myProfile.value = UiState.Error(it.message ?: "Failed to load profile") }
        }
    }

    // ── Driver Profile (for DriverProfileScreen) ───────────────────────────────
    private val _driverProfile = MutableStateFlow<UiState<UserDto>>(UiState.Idle)
    val driverProfile: StateFlow<UiState<UserDto>> = _driverProfile.asStateFlow()

    fun loadDriverProfile(driverId: String) {
        viewModelScope.launch {
            _driverProfile.value = UiState.Loading
            userRepo.getDriverProfile(driverId)
                .onSuccess { _driverProfile.value = UiState.Success(it) }
                .onFailure { _driverProfile.value = UiState.Error(it.message ?: "Failed") }
        }
    }

    // ── All Drivers (for BrowseDriversScreen) ─────────────────────────────────
    private val _allDrivers = MutableStateFlow<UiState<List<UserDto>>>(UiState.Idle)
    val allDrivers: StateFlow<UiState<List<UserDto>>> = _allDrivers.asStateFlow()

    fun loadAllDrivers() {
        viewModelScope.launch {
            _allDrivers.value = UiState.Loading
            userRepo.getAllDrivers()
                .onSuccess { _allDrivers.value = UiState.Success(it) }
                .onFailure { _allDrivers.value = UiState.Error(it.message ?: "Failed to load drivers") }
        }
    }

    // ── Online Toggle (Driver Dashboard) ──────────────────────────────────────
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun setOnline(isOnline: Boolean) {
        val uid = authRepo.currentUserId() ?: return
        _isOnline.value = isOnline   // optimistic update
        viewModelScope.launch {
            userRepo.setOnlineStatus(uid, isOnline)
                .onFailure { _isOnline.value = !isOnline }  // revert on error
        }
    }

    fun initOnlineStatus(currentValue: Boolean) {
        _isOnline.value = currentValue
    }
}