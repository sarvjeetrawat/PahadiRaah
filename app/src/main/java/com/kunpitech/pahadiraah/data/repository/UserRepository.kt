package com.kunpitech.pahadiraah.data.repository

import com.kunpitech.pahadiraah.data.model.UserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

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

    /** Update profile fields (name, bio, languages, etc.). */
    suspend fun updateProfile(userId: String, updates: Map<String, Any>): Result<Unit>
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
                order("avg_rating", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<UserDto>()
    }

    override suspend fun setOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> =
        runCatching {
            table.update(mapOf("is_online" to isOnline)) {
                filter { eq("id", userId) }
            }
        }

    override suspend fun updateProfile(
        userId:  String,
        updates: Map<String, Any>
    ): Result<Unit> = runCatching {
        table.update(updates) {
            filter { eq("id", userId) }
        }
    }
}