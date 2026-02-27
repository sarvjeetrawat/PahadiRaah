package com.kunpitech.pahadiraah.data.repository

import com.kunpitech.pahadiraah.data.model.BookingDto
import com.kunpitech.pahadiraah.data.model.UserDto
import com.kunpitech.pahadiraah.data.model.VehicleDto
import com.kunpitech.pahadiraah.data.model.NewBooking
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
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

interface BookingRepository {
    /** Passenger: confirm a booking for a route. */
    suspend fun confirmBooking(booking: NewBooking): Result<BookingDto>

    /** Passenger: fetch all bookings (with joined route + driver info). */
    suspend fun getMyBookings(passengerId: String): Result<List<BookingDto>>

    /** Driver: fetch all bookings on a specific route. */
    suspend fun getBookingsForRoute(routeId: String): Result<List<BookingDto>>

    /** Driver: fetch ALL bookings across all their routes (for dashboard view). */
    suspend fun getDriverBookings(driverId: String): Result<List<BookingDto>>

    /** Driver: accept or decline a booking. */
    suspend fun updateBookingStatus(bookingId: String, status: String): Result<Unit>

    /** Driver: decrease seats_left on a route when a booking is accepted. */
    suspend fun decreaseSeatsLeft(routeId: String, seats: Int): Result<Unit>

    /** Driver: mark a booking's payment as received (cash). */
    suspend fun markPaid(bookingId: String): Result<Unit>

    /** Passenger: cancel a booking. */
    suspend fun cancelBooking(bookingId: String): Result<Unit>

    /** Passenger: update seats + fares on an existing booking. */
    suspend fun updateBookingSeats(
        bookingId:  String,
        seats:      Int,
        totalFare:  Int,
        serviceFee: Int,
        grandTotal: Int
    ): Result<Unit>

    /**
     * Realtime: returns a Flow that emits a new BookingDto every time a
     * passenger books on [routeId]. Used by the driver's BookingRequests screen.
     */
    fun listenForNewBookings(routeId: String): Flow<BookingDto>

    /**
     * Realtime: returns a Flow that emits updated BookingDto whenever a
     * booking's status changes. Used by the passenger's TripProgress screen.
     */
    fun listenForBookingUpdates(bookingId: String): Flow<BookingDto>
}

// ─────────────────────────────────────────────────────────────────────────────
//  IMPLEMENTATION
// ─────────────────────────────────────────────────────────────────────────────

@kotlinx.serialization.Serializable
private data class RouteIdOnly(val id: String = "")

class BookingRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : BookingRepository {

    // Columns: join route and passenger user info
    private val bookingColumns = Columns.raw(
        "*," +
                "routes(id,driver_id,origin,destination,date,time,duration_hrs,fare_per_seat,vehicle_id,status)," +
                "users!passenger_id(id,name,emoji,avg_rating,total_trips)"
    )

    private val table get() = client.postgrest["bookings"]

    override suspend fun confirmBooking(booking: NewBooking): Result<BookingDto> = runCatching {
        table
            .insert(booking) { select(bookingColumns) }
            .decodeSingle<BookingDto>()
    }

    override suspend fun getMyBookings(passengerId: String): Result<List<BookingDto>> =
        runCatching {
            // Step 1: fetch bookings with plain routes join (no FK hints — avoids PostgREST failures)
            val bookings = table
                .select(bookingColumns) {
                    filter { eq("passenger_id", passengerId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<BookingDto>()

            if (bookings.isEmpty()) return@runCatching bookings

            // Step 2: collect IDs for batch lookups
            val driverIds  = bookings.mapNotNull { it.routes?.driverId?.takeIf(String::isNotBlank) }.distinct()
            val vehicleIds = bookings.mapNotNull { it.routes?.vehicleId?.takeIf(String::isNotBlank) }.distinct()
            android.util.Log.d("BookingRepo", "bookings=${bookings.size} routes=${bookings.count{it.routes!=null}} driverIds=$driverIds vehicleIds=$vehicleIds")
            bookings.take(2).forEach { b -> android.util.Log.d("BookingRepo", "  booking ${b.id.take(8)} routeId=${b.routeId} routes=${b.routes?.id?.take(8)} driverId=${b.routes?.driverId?.take(8)} vehicleId=${b.routes?.vehicleId?.take(8)}") }

            // Step 3: batch fetch drivers
            val driverMap: Map<String, UserDto> = if (driverIds.isNotEmpty()) {
                client.postgrest["users"]
                    .select(Columns.raw("id,name,emoji,avg_rating")) {
                        filter { isIn("id", driverIds) }
                    }
                    .decodeList<UserDto>()
                    .associateBy { it.id }
            } else emptyMap()

            // Step 4: batch fetch vehicles
            val vehicleMap: Map<String, VehicleDto> = if (vehicleIds.isNotEmpty()) {
                client.postgrest["vehicles"]
                    .select(Columns.raw("id,type,model,reg_number")) {
                        filter { isIn("id", vehicleIds) }
                    }
                    .decodeList<VehicleDto>()
                    .associateBy { it.id }
            } else emptyMap()

            // Step 5: assemble
            bookings.map { booking ->
                val route   = booking.routes ?: return@map booking
                val driver  = driverMap[route.driverId]
                val vehicle = vehicleMap[route.vehicleId ?: ""]
                booking.copy(routes = route.copy(users = driver, vehicles = vehicle))
            }
        }

    override suspend fun getBookingsForRoute(routeId: String): Result<List<BookingDto>> {
        // Hard guard — "all" is a sentinel value, never a real route UUID
        if (routeId == "all" || routeId.isBlank()) return Result.success(emptyList())
        return runCatching {
            table
                .select(Columns.raw("*, users!passenger_id(id, name, emoji, avg_rating, total_trips), routes(id, origin, destination)")) {
                    filter { eq("route_id", routeId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<BookingDto>()
        }
    }

    override suspend fun getDriverBookings(driverId: String): Result<List<BookingDto>> =
        runCatching {
            // Step 1: fetch all route IDs that belong to this driver
            val routeIds = client.postgrest["routes"]
                .select(Columns.raw("id")) {
                    filter { eq("driver_id", driverId) }
                }
                .decodeList<RouteIdOnly>()
                .map { it.id }

            if (routeIds.isEmpty()) return@runCatching emptyList()

            // Step 2: fetch bookings whose route_id is in that set
            table
                .select(Columns.raw("*, users!passenger_id(id, name, emoji, avg_rating, total_trips), routes(id, origin, destination, driver_id)")) {
                    filter { isIn("route_id", routeIds) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<BookingDto>()
        }

    override suspend fun updateBookingStatus(bookingId: String, status: String): Result<Unit> =
        runCatching {
            table.update({
                set("status", status)
            }) {
                filter { eq("id", bookingId) }
                select()
            }
            Unit
        }

    override suspend fun decreaseSeatsLeft(routeId: String, seats: Int): Result<Unit> =
        runCatching {
            // Use rpc to safely decrement seats_left
            client.postgrest.rpc(
                "decrease_seats_left",
                mapOf("route_id" to routeId, "seat_count" to seats)
            )
        }

    override suspend fun markPaid(bookingId: String): Result<Unit> = runCatching {
        table.update({
            set("payment_status", "paid")
        }) {
            filter { eq("id", bookingId) }
            select()
        }
        Unit
    }

    override suspend fun updateBookingSeats(
        bookingId:  String,
        seats:      Int,
        totalFare:  Int,
        serviceFee: Int,
        grandTotal: Int
    ): Result<Unit> = runCatching {
        table.update({
            set("seats",       seats)
            set("total_fare",  totalFare)
            set("service_fee", serviceFee)
            set("grand_total", grandTotal)
        }) {
            filter { eq("id", bookingId) }
            select()
        }
        Unit
    }

    override suspend fun cancelBooking(bookingId: String): Result<Unit> =
        updateBookingStatus(bookingId, "cancelled")

    // ── Realtime ──────────────────────────────────────────────────────────────

    override fun listenForNewBookings(routeId: String): Flow<BookingDto> {
        val channel = client.realtime.channel("bookings-driver-$routeId")
        return channel
            .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                this.table = "bookings"
                filter("route_id", FilterOperator.EQ, routeId)
            }
            .map { action -> action.decodeRecord<BookingDto>() }
    }

    override fun listenForBookingUpdates(bookingId: String): Flow<BookingDto> {
        val channel = client.realtime.channel("booking-status-$bookingId")
        return channel
            .postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                this.table = "bookings"
                filter("id", FilterOperator.EQ, bookingId)
            }
            .map { action -> action.decodeRecord<BookingDto>() }
    }
}