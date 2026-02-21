package com.kunpitech.pahadiraah.data.repository

import com.kunpitech.pahadiraah.data.model.LocationDto
import com.kunpitech.pahadiraah.data.model.UpsertLocation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  INTERFACE
// ─────────────────────────────────────────────────────────────────────────────

interface LocationRepository {
    /**
     * Driver: upsert current GPS position for an active trip.
     * Uses upsert on trip_id (PK) so only ONE row per trip is kept — no unbounded growth.
     */
    suspend fun upsertLocation(location: UpsertLocation): Result<Unit>

    /**
     * Passenger: get the latest known location for a trip.
     * Called once on TripProgressScreen load.
     */
    suspend fun getLatestLocation(tripId: String): Result<LocationDto?>

    /**
     * Passenger: Realtime Flow — emits a new LocationDto every time the driver
     * sends a GPS update. Powers the live pin movement on TripProgressScreen.
     */
    fun listenForLocation(tripId: String): Flow<LocationDto>
}

// ─────────────────────────────────────────────────────────────────────────────
//  IMPLEMENTATION
// ─────────────────────────────────────────────────────────────────────────────

class LocationRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : LocationRepository {

    private val table get() = client.postgrest["locations"]

    override suspend fun upsertLocation(location: UpsertLocation): Result<Unit> = runCatching {
        table.upsert(location) {
            onConflict = "trip_id"   // primary key — replaces the single row
        }
    }

    override suspend fun getLatestLocation(tripId: String): Result<LocationDto?> = runCatching {
        val results = table
            .select(Columns.ALL) {
                filter { eq("trip_id", tripId) }
                limit(1)
            }
            .decodeList<LocationDto>()
        results.firstOrNull()
    }

    override fun listenForLocation(tripId: String): Flow<LocationDto> {
        val channel = client.realtime.channel("location-$tripId")
        return channel
            .postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                this.table = "locations"
                filter("trip_id", FilterOperator.EQ, tripId)
            }
            .map { action -> action.decodeRecord<LocationDto>() }
    }
}