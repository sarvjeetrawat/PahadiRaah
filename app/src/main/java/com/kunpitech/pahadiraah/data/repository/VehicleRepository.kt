package com.kunpitech.pahadiraah.data.repository

import com.kunpitech.pahadiraah.data.model.NewVehicle
import com.kunpitech.pahadiraah.data.model.VehicleDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  TYPED UPDATE PAYLOAD
//  supabase-kt does NOT support Map<String, Any> for updates because Any has
//  no kotlinx.serialization serializer. Always use @Serializable data classes.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class VehicleFieldsUpdate(
    val type:          String,
    val model:         String,
    @SerialName("reg_number")
    val regNumber:     String,
    @SerialName("seat_capacity")
    val seatCapacity:  Int,
    // Re-flag as needing verification whenever reg number changes
    @SerialName("is_verified")
    val isVerified:    Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
//  INTERFACE
// ─────────────────────────────────────────────────────────────────────────────

interface VehicleRepository {
    /** Insert a new vehicle row for the driver. */
    suspend fun addVehicle(vehicle: NewVehicle): Result<VehicleDto>

    /** Fetch the first vehicle belonging to this driver. */
    suspend fun getDriverVehicle(driverId: String): Result<VehicleDto?>

    /** Update vehicle fields by vehicle id using a typed payload. */
    suspend fun updateVehicle(vehicleId: String, updates: VehicleFieldsUpdate): Result<Unit>
}

// ─────────────────────────────────────────────────────────────────────────────
//  IMPLEMENTATION
// ─────────────────────────────────────────────────────────────────────────────

class VehicleRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : VehicleRepository {

    private val table get() = client.postgrest["vehicles"]

    override suspend fun addVehicle(vehicle: NewVehicle): Result<VehicleDto> = runCatching {
        table.insert(vehicle) { select() }.decodeSingle<VehicleDto>()
    }

    override suspend fun getDriverVehicle(driverId: String): Result<VehicleDto?> = runCatching {
        table
            .select(Columns.ALL) {
                filter { eq("driver_id", driverId) }
                limit(1)
            }
            .decodeList<VehicleDto>()
            .firstOrNull()
    }

    override suspend fun updateVehicle(
        vehicleId: String,
        updates:   VehicleFieldsUpdate
    ): Result<Unit> = runCatching {
        table.update({
            set("type",          updates.type)
            set("model",         updates.model)
            set("reg_number",    updates.regNumber)
            set("seat_capacity", updates.seatCapacity)
            set("is_verified",   updates.isVerified)
        }) {
            filter { eq("id", vehicleId) }
            select()
        }
        Unit
    }
}