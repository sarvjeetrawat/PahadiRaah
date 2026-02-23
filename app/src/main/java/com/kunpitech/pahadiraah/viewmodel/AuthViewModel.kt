package com.kunpitech.pahadiraah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.pahadiraah.data.model.ActionResult
import com.kunpitech.pahadiraah.data.repository.AuthRepository
import com.kunpitech.pahadiraah.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    // ── OTP flow state ────────────────────────────────────────────────────────
    private val _otpResult  = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val otpResult: StateFlow<ActionResult> = _otpResult.asStateFlow()

    private val _verifyResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val verifyResult: StateFlow<ActionResult> = _verifyResult.asStateFlow()

    // ── Current user (null = not logged in) ───────────────────────────────────
    val currentUser: StateFlow<UserInfo?> = authRepo
        .authStateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepo.currentUser())

    // Convenience: is someone logged in right now?
    val isLoggedIn: Boolean get() = authRepo.currentUserId() != null
    val userId:     String? get() = authRepo.currentUserId()

    // ── Role (loaded from public.users after login) ───────────────────────────
    private val _role = MutableStateFlow<String?>(null)   // "driver" | "passenger"
    val role: StateFlow<String?> = _role.asStateFlow()

    init {
        // Watch auth state — load role whenever a session becomes available.
        // We cannot call currentUserId() synchronously at init because Supabase
        // hasn't restored the session from storage yet; the flow is the source of truth.
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null && _role.value == null) {
                    loadRole(user.id)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SEND OTP
    // ─────────────────────────────────────────────────────────────────────────

    fun sendOtp(email: String) {
        if (email.isBlank()) return
        viewModelScope.launch {
            _otpResult.value = ActionResult.Loading
            authRepo.sendOtp(email.trim())
                .onSuccess { _otpResult.value = ActionResult.Success }
                .onFailure { _otpResult.value = ActionResult.Error(it.message ?: "Failed to send OTP") }
        }
    }

    fun resetOtpResult() { _otpResult.value = ActionResult.Idle }

    // ─────────────────────────────────────────────────────────────────────────
    //  VERIFY OTP
    // ─────────────────────────────────────────────────────────────────────────

    fun verifyOtp(email: String, token: String) {
        if (token.length != 6) return
        viewModelScope.launch {
            _verifyResult.value = ActionResult.Loading
            authRepo.verifyOtp(email.trim(), token.trim())
                .onSuccess { user ->
                    loadRole(user.id)
                    _verifyResult.value = ActionResult.Success
                }
                .onFailure {
                    _verifyResult.value = ActionResult.Error(it.message ?: "Invalid OTP")
                }
        }
    }

    fun resetVerifyResult() { _verifyResult.value = ActionResult.Idle }

    // ─────────────────────────────────────────────────────────────────────────
    //  SET ROLE (first time user picks driver / passenger)
    // ─────────────────────────────────────────────────────────────────────────

    fun setRole(name: String, role: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            authRepo.updateProfile(uid, name, role)
                .onSuccess { _role.value = role }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SIGN OUT
    // ─────────────────────────────────────────────────────────────────────────

    fun signOut() {
        viewModelScope.launch {
            authRepo.signOut()
            _role.value = null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INTERNAL
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun loadRole(userId: String) {
        userRepo.getMyProfile(userId)
            .onSuccess { profile -> _role.value = profile.role }
    }
}