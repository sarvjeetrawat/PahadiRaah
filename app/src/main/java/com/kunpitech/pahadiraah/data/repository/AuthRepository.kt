package com.kunpitech.pahadiraah.data.repository

import com.kunpitech.pahadiraah.data.model.UserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import io.github.jan.supabase.auth.OtpType
import javax.inject.Inject
// ─────────────────────────────────────────────────────────────────────────────
//  INTERFACE
// ─────────────────────────────────────────────────────────────────────────────

interface AuthRepository {
    /** Send a 6-digit OTP to the given email address. */
    suspend fun sendOtp(email: String): Result<Unit>

    /** Verify the OTP code entered by the user. Returns the Supabase UserInfo on success. */
    suspend fun verifyOtp(email: String, token: String): Result<UserInfo>

    /** Returns the currently signed-in user, or null if not logged in. */
    fun currentUser(): UserInfo?

    /** Returns the current user's UUID, or null. */
    fun currentUserId(): String?

    /** Observe auth state changes as a Flow of UserInfo? */
    fun authStateFlow(): Flow<UserInfo?>

    /** Sign out the current user. */
    suspend fun signOut(): Result<Unit>

    /**
     * After OTP verification, the DB trigger creates a public.users row.
     * Call this to update the name + role that the user sets on first launch.
     */
    suspend fun updateProfile(userId: String, name: String, role: String): Result<Unit>
}

// ─────────────────────────────────────────────────────────────────────────────
//  IMPLEMENTATION
// ─────────────────────────────────────────────────────────────────────────────

class AuthRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : AuthRepository {

    private val auth get() = client.auth

    override suspend fun sendOtp(email: String): Result<Unit> = runCatching {
        auth.signInWith(OTP) {
            this.email        = email
            this.createUser   = true      // creates auth.users row if new
        }
    }

    override suspend fun verifyOtp(email: String, token: String): Result<UserInfo> = runCatching {
        auth.verifyEmailOtp(
            type  = OtpType.Email.EMAIL,
            email = email,
            token = token
        )
        auth.currentUserOrNull() ?: error("Verification succeeded but user is null")
    }

    override fun currentUser(): UserInfo? = auth.currentUserOrNull()

    override fun currentUserId(): String? = auth.currentUserOrNull()?.id

    override fun authStateFlow(): Flow<UserInfo?> =
        auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> auth.currentUserOrNull()
                else -> null
            }
        }
    override suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }

    override suspend fun updateProfile(
        userId: String,
        name:   String,
        role:   String
    ): Result<Unit> = runCatching {
        client.postgrest["users"].update(
            mapOf("name" to name, "role" to role)
        ) {
            filter { eq("id", userId) }
        }
    }
}