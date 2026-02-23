package com.kunpitech.pahadiraah.data.repository

import com.kunpitech.pahadiraah.data.model.NewRoute
import com.kunpitech.pahadiraah.data.model.RouteDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  INTERFACE
// ─────────────────────────────────────────────────────────────────────────────

interface RouteRepository {
    /** Passenger: search available routes by origin/destination keywords. */
    suspend fun searchRoutes(origin: String, destination: String, minSeats: Int = 1): Result<List<RouteDto>>

    /** Driver: get all routes posted by this driver. */
    suspend fun getMyRoutes(driverId: String): Result<List<RouteDto>>

    /** Driver: get active (ongoing) routes for this driver. */
    suspend fun getActiveRoutes(driverId: String): Result<List<RouteDto>>

    /** Get upcoming routes for a specific driver (shown on their profile). */
    suspend fun getDriverUpcomingRoutes(driverId: String): Result<List<RouteDto>>

    /** Get a single route by id. */
    suspend fun getRouteById(routeId: String): Result<RouteDto>

    /** Driver: post a new route. */
    suspend fun postRoute(route: NewRoute): Result<RouteDto>

    /** Driver: update route status (upcoming → ongoing → completed / cancelled). */
    suspend fun updateRouteStatus(routeId: String, status: String): Result<Unit>

    /** Driver: cancel a route. */
    suspend fun cancelRoute(routeId: String): Result<Unit>
}

// ─────────────────────────────────────────────────────────────────────────────
//  IMPLEMENTATION
// ─────────────────────────────────────────────────────────────────────────────

class RouteRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : RouteRepository {

    // Join driver info into each route for display
    private val routeColumns = Columns.raw("*, users!driver_id(id, name, emoji, avg_rating, is_online)")

    private val table get() = client.postgrest["routes"]

    override suspend fun searchRoutes(
        origin:      String,
        destination: String,
        minSeats:    Int
    ): Result<List<RouteDto>> = runCatching {
        table
            .select(routeColumns) {
                filter {
                    if (origin.isNotBlank())
                        ilike("origin", "%$origin%")
                    if (destination.isNotBlank())
                        ilike("destination", "%$destination%")
                    eq("status", "upcoming")
                    gte("seats_left", minSeats)
                }
                order("date", Order.ASCENDING)
                order("time", Order.ASCENDING)
            }
            .decodeList<RouteDto>()
    }

    override suspend fun getMyRoutes(driverId: String): Result<List<RouteDto>> = runCatching {
        table
            .select(Columns.ALL) {
                filter { eq("driver_id", driverId) }
                order("date", Order.DESCENDING)
            }
            .decodeList<RouteDto>()
    }

    override suspend fun getActiveRoutes(driverId: String): Result<List<RouteDto>> = runCatching {
        // Fetch ALL driver routes (upcoming, ongoing, completed) so the Completed tab
        // in ActiveRoutesScreen is populated. Cancelled routes are excluded.
        table
            .select(Columns.raw("*, bookings(id, passenger_id, seats, status, users!passenger_id(name, emoji, avg_rating))")) {
                filter {
                    eq("driver_id", driverId)
                    neq("status", "cancelled")
                }
                order("date", Order.DESCENDING)
            }
            .decodeList<RouteDto>()
    }

    override suspend fun getDriverUpcomingRoutes(driverId: String): Result<List<RouteDto>> =
        runCatching {
            table
                .select(Columns.ALL) {
                    filter {
                        eq("driver_id", driverId)
                        eq("status", "upcoming")
                        gte("seats_left", 1)
                    }
                    order("date", Order.ASCENDING)
                    limit(5)
                }
                .decodeList<RouteDto>()
        }

    override suspend fun getRouteById(routeId: String): Result<RouteDto> = runCatching {
        table
            .select(routeColumns) {
                filter { eq("id", routeId) }
                limit(1)
            }
            .decodeSingle<RouteDto>()
    }

    override suspend fun postRoute(route: NewRoute): Result<RouteDto> = runCatching {
        table
            .insert(route) { select() }
            .decodeSingle<RouteDto>()
    }

    override suspend fun updateRouteStatus(routeId: String, status: String): Result<Unit> =
        runCatching {
            table.update(mapOf("status" to status)) {
                filter { eq("id", routeId) }
            }
        }

    override suspend fun cancelRoute(routeId: String): Result<Unit> =
        updateRouteStatus(routeId, "cancelled")
}