package com.kunpitech.pahadiraah.data.repository

import com.kunpitech.pahadiraah.data.model.UserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  TYPED UPDATE PAYLOADS
//  Supabase uses kotlinx.serialization — Map<String, Any> is NOT supported
//  because Any has no serializer. Always use @Serializable data classes.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
private data class OnlineStatusUpdate(
    @SerialName("is_online") val isOnline: Boolean
)

@Serializable
data class ProfileFieldsUpdate(
    val name:       String,
    val emoji:      String,
    val role:       String,
    val bio:        String?      = null,
    val languages:  List<String> = emptyList(),
    val speciality: String?      = null,
    @SerialName("avatar_url")
    val photoUrl:   String?      = null
)

// ─────────────────────────────────────────────────────────────────────────────
//  INTERFACE
// ─────────────────────────────────────────────────────────────────────────────

interface UserRepository {
    /** Fetch logged-in user's own profile. */
    suspend fun getMyProfile(userId: String): Result<UserDto>

    /** Fetch a driver's public profile by their user id. */
    suspend fun getDriverProfile(driverId: String): Result<UserDto>

    /** Fetch all drivers (for BrowseDrivers screen). */
    suspend fun getAllDrivers(): Result<List<UserDto>>

    /** Update the current user's online status. */
    suspend fun setOnlineStatus(userId: String, isOnline: Boolean): Result<Unit>

    /** Update profile fields (name, emoji, bio, languages, speciality). */
    suspend fun updateProfile(userId: String, updates: ProfileFieldsUpdate): Result<Unit>

    /** Update only avatar_url — safe partial update. */
    suspend fun updateAvatarUrl(userId: String, url: String): Result<Unit>
}

// ─────────────────────────────────────────────────────────────────────────────
//  IMPLEMENTATION
// ─────────────────────────────────────────────────────────────────────────────

class UserRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : UserRepository {

    private val table get() = client.postgrest["users"]

    override suspend fun getMyProfile(userId: String): Result<UserDto> = runCatching {
        table
            .select(Columns.ALL) {
                filter { eq("id", userId) }
                limit(1)
            }
            .decodeSingle<UserDto>()
    }

    override suspend fun getDriverProfile(driverId: String): Result<UserDto> = runCatching {
        table
            .select(Columns.ALL) {
                filter {
                    eq("id", driverId)
                    eq("role", "driver")
                }
                limit(1)
            }
            .decodeSingle<UserDto>()
    }

    override suspend fun getAllDrivers(): Result<List<UserDto>> = runCatching {
        table
            .select(Columns.ALL) {
                filter { eq("role", "driver") }
                order("avg_rating", Order.DESCENDING)
            }
            .decodeList<UserDto>()
    }

    override suspend fun setOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> =
        runCatching {
            table.update({
                set("is_online", isOnline)
            }) {
                filter { eq("id", userId) }
                select()
            }
            Unit
        }

    override suspend fun updateProfile(
        userId:  String,
        updates: ProfileFieldsUpdate
    ): Result<Unit> = runCatching {
        table.update({
            if (updates.name.isNotBlank())  set("name",  updates.name)
            if (updates.emoji.isNotBlank()) set("emoji", updates.emoji)
            if (updates.role.isNotBlank())  set("role",  updates.role)
            if (updates.bio  != null)       set("bio",   updates.bio)
            if (updates.languages.isNotEmpty()) set("languages",  updates.languages)
            if (updates.speciality != null) set("speciality", updates.speciality)
            if (updates.photoUrl   != null) set("avatar_url", updates.photoUrl)
        }) {
            filter { eq("id", userId) }
            select()
        }
        Unit
    }

    override suspend fun updateAvatarUrl(userId: String, url: String): Result<Unit> = runCatching {
        table.update({ set("avatar_url", url) }) {
            filter { eq("id", userId) }
        }
        Unit
    }
}