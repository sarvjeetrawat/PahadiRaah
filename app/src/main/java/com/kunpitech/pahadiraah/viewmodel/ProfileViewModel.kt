package com.kunpitech.pahadiraah.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunpitech.pahadiraah.data.model.ActionResult
import com.kunpitech.pahadiraah.data.model.NewVehicle
import com.kunpitech.pahadiraah.data.repository.AuthRepository
import com.kunpitech.pahadiraah.data.repository.ProfileFieldsUpdate
import com.kunpitech.pahadiraah.data.repository.UserRepository
import com.kunpitech.pahadiraah.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
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
    private val vehicleRepo: VehicleRepository,
    private val supabase:    SupabaseClient
) : ViewModel() {

    private val _saveResult = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val saveResult: StateFlow<ActionResult> = _saveResult.asStateFlow()

    private val _photoUrl = MutableStateFlow<String?>(null)
    val photoUrl: StateFlow<String?> = _photoUrl.asStateFlow()

    private val _uploadState = MutableStateFlow<ActionResult>(ActionResult.Idle)
    val uploadState: StateFlow<ActionResult> = _uploadState.asStateFlow()

    // Cached uid — survives camera activity pause/resume
    private var cachedUid: String? = null

    fun cacheUid() {
        cachedUid = authRepo.currentUserId()
        Log.d("ProfileVM", "cacheUid: $cachedUid")
    }

    fun uploadPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uploadState.value = ActionResult.Loading
            try {
                // Wait for session to be available (needed after camera activity resume)
                var uid: String? = cachedUid ?: authRepo.currentUserId()
                if (uid == null) {
                    // Session briefly null after camera — wait up to 3s for it to restore
                    repeat(6) {
                        delay(500)
                        uid = authRepo.currentUserId()
                        if (uid != null) return@repeat
                    }
                }
                if (uid == null) {
                    Log.e("ProfileVM", "uploadPhoto: uid still null after wait")
                    _uploadState.value = ActionResult.Error("Session error — please try again")
                    return@launch
                }
                cachedUid = uid  // refresh cache

                Log.d("ProfileVM", "uploadPhoto: uid=$uid, uri=$uri")

                // Read image bytes
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw Exception("Could not read image")
                Log.d("ProfileVM", "uploadPhoto: read ${bytes.size} bytes")

                // Delete old file first (avoids upsert RLS issues with UPDATE policy)
                val path = "$uid.jpg"
                try {
                    supabase.storage["avatars"].delete(listOf(path))
                    Log.d("ProfileVM", "Deleted old avatar ✓")
                } catch (e: Exception) {
                    Log.d("ProfileVM", "No existing file to delete (first upload)")
                }
                // Now INSERT (not upsert) — no UPDATE policy needed
                supabase.storage["avatars"].upload(path, bytes) { upsert = false }

                // Get public URL with cache-bust
                val url = "${supabase.storage["avatars"].publicUrl(path)}?t=${System.currentTimeMillis()}"
                Log.d("ProfileVM", "uploadPhoto: success url=$url")

                _photoUrl.value = url

                // Save avatar_url to DB
                val saveResult = userRepo.updateAvatarUrl(uid!!, url)
                if (saveResult.isSuccess) {
                    Log.d("ProfileVM", "avatar_url saved to DB ✓")
                } else {
                    Log.e("ProfileVM", "DB save failed: ${saveResult.exceptionOrNull()?.message}")
                }

                _uploadState.value = ActionResult.Success

            } catch (e: Exception) {
                Log.e("ProfileVM", "uploadPhoto failed: ${e.message}", e)
                _uploadState.value = ActionResult.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun setPhotoUrl(url: String?) { _photoUrl.value = url }
    fun resetUploadState()        { _uploadState.value = ActionResult.Idle }

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

            var userExists = false
            repeat(5) { attempt ->
                val result = userRepo.getMyProfile(userId)
                if (result.isSuccess) { userExists = true; return@repeat }
                if (attempt < 4) delay(500)
            }

            if (!userExists) {
                _saveResult.value = ActionResult.Error(
                    "Account setup is still in progress. Please wait a moment and try again."
                )
                return@launch
            }

            userRepo.updateProfile(
                userId  = userId,
                updates = ProfileFieldsUpdate(
                    name       = name,
                    emoji      = emoji,
                    role       = if (isDriver) "driver" else "passenger",
                    bio        = bio,
                    languages  = languages,
                    speciality = speciality,
                    photoUrl   = _photoUrl.value
                )
            ).onFailure { e ->
                _saveResult.value = ActionResult.Error(e.message ?: "Failed to save profile")
                return@launch
            }

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

    fun resetSaveResult() { _saveResult.value = ActionResult.Idle }
}