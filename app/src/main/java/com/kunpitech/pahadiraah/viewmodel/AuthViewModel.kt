package com.kunpitech.pahadiraah.viewmodel

import android.content.Context
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

    // ── OTP flow ──────────────────────────────────────────────────────────────
    private val _otpResult    = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val otpResult: StateFlow<ActionResult> = _otpResult.asStateFlow()

    private val _verifyResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val verifyResult: StateFlow<ActionResult> = _verifyResult.asStateFlow()

    // ── Google Sign-In ────────────────────────────────────────────────────────
    private val _googleResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val googleResult: StateFlow<ActionResult> = _googleResult.asStateFlow()

    /** Set to true when Google sign-in succeeds for a brand-new user (no role yet). */
    private val _isNewGoogleUser = MutableStateFlow(false)
    val isNewGoogleUser: StateFlow<Boolean> = _isNewGoogleUser.asStateFlow()

    // ── Current user ──────────────────────────────────────────────────────────
    val currentUser: StateFlow<UserInfo?> = authRepo
        .authStateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepo.currentUser())

    val authState: StateFlow<UserInfo?> = currentUser  // alias for NavGraph clarity

    val isLoggedIn: Boolean get() = authRepo.currentUserId() != null
    val userId:     String? get() = authRepo.currentUserId()

    // ── Role ──────────────────────────────────────────────────────────────────
    private val _role = MutableStateFlow<String?>(null)
    val role: StateFlow<String?> = _role.asStateFlow()

    init {
        // Load role automatically whenever a session is established
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
    //  GOOGLE SIGN-IN
    // ─────────────────────────────────────────────────────────────────────────

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _googleResult.value = ActionResult.Loading
            authRepo.signInWithGoogle(context)
                .onSuccess { result ->
                    _isNewGoogleUser.value = result.isNew
                    if (!result.isNew) {
                        // Returning user — load their existing role
                        loadRole(result.user.id)
                    }
                    _googleResult.value = ActionResult.Success
                }
                .onFailure { e ->
                    val msg = when {
                        e.message?.contains("cancel", ignoreCase = true) == true ->
                            "Sign-in cancelled"
                        e.message?.contains("network", ignoreCase = true) == true ||
                                e.message?.contains("unable to resolve", ignoreCase = true) == true ->
                            "No internet connection"
                        else -> e.message ?: "Google sign-in failed"
                    }
                    _googleResult.value = ActionResult.Error(msg)
                }
        }
    }

    fun resetGoogleResult() { _googleResult.value = ActionResult.Idle }

    // ─────────────────────────────────────────────────────────────────────────
    //  SET ROLE  (first-time user picks driver / passenger after Google sign-in)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called after Google sign-in when a new user picks their role.
     * Writes name + role + email + emoji into public.users so the
     * profile screen shows the correct data immediately.
     */
    fun setRole(
        name:  String,
        role:  String,
        email: String? = null,   // Google account email
        emoji: String  = "\ud83e\uddd1"  // default 🧑 avatar
    ) {
        val uid = userId ?: return
        viewModelScope.launch {
            authRepo.updateProfile(uid, name, role, email, emoji)
                .onSuccess { _role.value = role }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SIGN OUT
    // ─────────────────────────────────────────────────────────────────────────

    fun signOut() {
        viewModelScope.launch {
            authRepo.signOut()
            _role.value            = null
            _isNewGoogleUser.value = false
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