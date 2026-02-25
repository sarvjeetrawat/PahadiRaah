package com.kunpitech.pahadiraah.data.repository

import com.kunpitech.pahadiraah.data.model.NewVehicle
import com.kunpitech.pahadiraah.data.model.VehicleDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  INTERFACE
// ─────────────────────────────────────────────────────────────────────────────

interface VehicleRepository {
    /** Insert a new vehicle for the driver. Returns the created vehicle. */
    suspend fun addVehicle(vehicle: NewVehicle): Result<VehicleDto>

    /** Fetch the vehicle belonging to this driver (first match). */
    suspend fun getDriverVehicle(driverId: String): Result<VehicleDto?>

    /** Update vehicle fields by vehicle id. */
    suspend fun updateVehicle(vehicleId: String, updates: Map<String, Any>): Result<Unit>
}

// ─────────────────────────────────────────────────────────────────────────────
//  IMPLEMENTATION
// ─────────────────────────────────────────────────────────────────────────────

class VehicleRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : VehicleRepository {

    private val table get() = client.postgrest["vehicles"]

    override suspend fun addVehicle(vehicle: NewVehicle): Result<VehicleDto> = runCatching {
        table
            .insert(vehicle) { select() }
            .decodeSingle<VehicleDto>()
    }

    override suspend fun getDriverVehicle(driverId: String): Result<VehicleDto?> = runCatching {
        val results = table
            .select(Columns.ALL) {
                filter { eq("driver_id", driverId) }
                limit(1)
            }
            .decodeList<VehicleDto>()
        results.firstOrNull()
    }

    override suspend fun updateVehicle(
        vehicleId: String,
        updates:   Map<String, Any>
    ): Result<Unit> = runCatching {
        table.update({
            updates.forEach { (key, value) -> set(key, value.toString()) }
        }) {
            filter { eq("id", vehicleId) }
            select()
        }
        Unit
    }
}