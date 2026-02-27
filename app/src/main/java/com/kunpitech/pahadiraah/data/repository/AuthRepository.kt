package com.kunpitech.pahadiraah.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.kunpitech.pahadiraah.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  INTERFACE
// ─────────────────────────────────────────────────────────────────────────────

interface AuthRepository {
    /** Send a 6-digit OTP to the given email address. */
    suspend fun sendOtp(email: String): Result<Unit>

    /** Verify the OTP code entered by the user. Returns the Supabase UserInfo on success. */
    suspend fun verifyOtp(email: String, token: String): Result<UserInfo>

    /**
     * Sign in with Google using Android Credential Manager.
     *
     * Strategy:
     *  1. Silent/auto — tries the previously-used account first (no UI shown).
     *  2. Full picker  — falls back if no previously-authorized account exists.
     *
     * Returns [GoogleSignInResult] with isNew=true when the user has no role set yet.
     */
    suspend fun signInWithGoogle(context: Context): Result<GoogleSignInResult>

    fun currentUser(): UserInfo?
    fun currentUserId(): String?
    fun authStateFlow(): Flow<UserInfo?>
    suspend fun signOut(): Result<Unit>
    suspend fun updateProfile(
        userId: String,
        name:   String,
        role:   String,
        email:  String? = null,   // pass for Google users so it lands in public.users
        emoji:  String  = "🧑"
    ): Result<Unit>
}

/** Returned from [AuthRepository.signInWithGoogle]. */
data class GoogleSignInResult(
    val user:  UserInfo,
    /** true = brand-new user who has no role yet → route to RoleSelect */
    val isNew: Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
//  IMPLEMENTATION
// ─────────────────────────────────────────────────────────────────────────────

class AuthRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : AuthRepository {

    private val auth get() = client.auth

    // ── OTP ──────────────────────────────────────────────────────────────────

    override suspend fun sendOtp(email: String): Result<Unit> = runCatching {
        auth.signInWith(OTP) {
            this.email      = email
            this.createUser = true
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

    // ── Google Sign-In ────────────────────────────────────────────────────────

    override suspend fun signInWithGoogle(context: Context): Result<GoogleSignInResult> = runCatching {
        val idToken = getGoogleIdToken(context)
        signInToSupabaseWithToken(idToken)
    }

    /**
     * Two-step Credential Manager strategy:
     *
     * Step 1 — SILENT (filterByAuthorizedAccounts = true, autoSelect = true)
     *   Credential Manager silently returns the previously-used Google account.
     *   No bottom-sheet or picker is shown at all.
     *
     * Step 2 — FULL PICKER fallback (filterByAuthorizedAccounts = false)
     *   Only reached when NoCredentialException is thrown in Step 1 — meaning
     *   the user has never signed in before or revoked access.
     *   Shows the standard Google account chooser bottom-sheet.
     */
    private suspend fun getGoogleIdToken(context: Context): String {
        val credentialManager = CredentialManager.create(context)

        // ── Step 1: try silent / auto sign-in ────────────────────────────────
        val silentOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)   // only previously-used accounts
            .setServerClientId("184950571528-chrf3kpm2hij3jq2lg3lm8ug2d1tapau.apps.googleusercontent.com")
            .setAutoSelectEnabled(true)            // pick automatically, no UI
            .build()

        val silentRequest = GetCredentialRequest.Builder()
            .addCredentialOption(silentOption)
            .build()

        try {
            val response = credentialManager.getCredential(
                request = silentRequest,
                context = context
            )
            return extractIdToken(response.credential)
        } catch (e: NoCredentialException) {
            // No previously-authorized account — fall through to picker
        } catch (e: GetCredentialCancellationException) {
            throw e   // user explicitly cancelled — propagate immediately
        }

        // ── Step 2: full account picker ──────────────────────────────────────
        val pickerOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)  // show all Google accounts
            .setServerClientId("184950571528-chrf3kpm2hij3jq2lg3lm8ug2d1tapau.apps.googleusercontent.com")
            .setAutoSelectEnabled(false)           // show picker even with 1 account
            .build()

        val pickerRequest = GetCredentialRequest.Builder()
            .addCredentialOption(pickerOption)
            .build()

        val response = credentialManager.getCredential(
            request = pickerRequest,
            context = context
        )
        return extractIdToken(response.credential)
    }

    private fun extractIdToken(credential: androidx.credentials.Credential): String {
        require(
            credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Unexpected credential type: ${credential.type}" }

        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    private suspend fun signInToSupabaseWithToken(idToken: String): GoogleSignInResult {
        // Correct supabase-kt syntax for native ID token sign-in
        auth.signInWith(IDToken) {
            this.idToken  = idToken
            this.provider = Google
        }

        val user = auth.currentUserOrNull()
            ?: error("Google sign-in succeeded but user is null")

        // Detect first-time user: check if public.users row has a role set
        val hasRole = runCatching {
            client.postgrest["users"]
                .select { filter { eq("id", user.id) } }
                .decodeList<Map<String, String>>()
                .firstOrNull()
                ?.get("role")
                ?.isNotBlank() == true
        }.getOrDefault(false)

        return GoogleSignInResult(user = user, isNew = !hasRole)
    }

    // ── Core ──────────────────────────────────────────────────────────────────

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
        role:   String,
        email:  String?,
        emoji:  String
    ): Result<Unit> = runCatching {
        client.postgrest["users"].update({
            set("name",  name)
            set("role",  role)
            set("emoji", emoji)
            // Only write email when explicitly provided (Google sign-in)
            if (!email.isNullOrBlank()) set("email", email)
        }) {
            filter { eq("id", userId) }
            select()
        }
        Unit
    }
}