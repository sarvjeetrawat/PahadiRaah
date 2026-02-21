package com.kunpitech.pahadiraah.data.repository

import com.kunpitech.pahadiraah.data.model.BookingDto
import com.kunpitech.pahadiraah.data.model.NewBooking
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
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

interface BookingRepository {
    /** Passenger: confirm a booking for a route. */
    suspend fun confirmBooking(booking: NewBooking): Result<BookingDto>

    /** Passenger: fetch all bookings (with joined route + driver info). */
    suspend fun getMyBookings(passengerId: String): Result<List<BookingDto>>

    /** Driver: fetch all bookings on a specific route. */
    suspend fun getBookingsForRoute(routeId: String): Result<List<BookingDto>>

    /** Driver: accept or decline a booking. */
    suspend fun updateBookingStatus(bookingId: String, status: String): Result<Unit>

    /** Driver: mark a booking's payment as received (cash). */
    suspend fun markPaid(bookingId: String): Result<Unit>

    /** Passenger: cancel a booking. */
    suspend fun cancelBooking(bookingId: String): Result<Unit>

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

class BookingRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : BookingRepository {

    // Columns: join route and passenger user info
    private val bookingColumns = Columns.raw(
        "*, " +
                "routes(id, origin, destination, date, time, duration_hrs, fare_per_seat, vehicle_id, " +
                "  users!driver_id(id, name, emoji, avg_rating)), " +
                "users!passenger_id(id, name, emoji, avg_rating, total_trips)"
    )

    private val table get() = client.postgrest["bookings"]

    override suspend fun confirmBooking(booking: NewBooking): Result<BookingDto> = runCatching {
        table
            .insert(booking) { select(bookingColumns) }
            .decodeSingle<BookingDto>()
    }

    override suspend fun getMyBookings(passengerId: String): Result<List<BookingDto>> =
        runCatching {
            table
                .select(bookingColumns) {
                    filter { eq("passenger_id", passengerId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<BookingDto>()
        }

    override suspend fun getBookingsForRoute(routeId: String): Result<List<BookingDto>> =
        runCatching {
            table
                .select(Columns.raw("*, users!passenger_id(id, name, emoji, avg_rating, total_trips)")) {
                    filter { eq("route_id", routeId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<BookingDto>()
        }

    override suspend fun updateBookingStatus(bookingId: String, status: String): Result<Unit> =
        runCatching {
            table.update(mapOf("status" to status)) {
                filter { eq("id", bookingId) }
            }
        }

    override suspend fun markPaid(bookingId: String): Result<Unit> = runCatching {
        table.update(mapOf("payment_status" to "paid")) {
            filter { eq("id", bookingId) }
        }
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