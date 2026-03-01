package com.kunpitech.pahadiraah.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
//  USER
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class UserDto(
    val id:           String        = "",
    val name:         String        = "",
    val email:        String?       = null,
    val phone:        String?       = null,
    val role:         String        = "passenger",
    val emoji:        String        = "🧑",
    @SerialName("avatar_url")
    val avatarUrl:    String?       = null,
    val bio:          String?       = null,
    @SerialName("avg_rating")
    val avgRating:    Double        = 0.0,
    @SerialName("total_trips")
    val totalTrips:   Int           = 0,
    @SerialName("years_active")
    val yearsActive:  Int           = 0,
    val languages:    List<String>  = emptyList(),
    val speciality:   String?       = null,
    @SerialName("is_online")
    val isOnline:     Boolean       = false,
    @SerialName("created_at")
    val createdAt:    String?       = null
)

// ─────────────────────────────────────────────────────────────────────────────
//  VEHICLE
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class VehicleDto(
    val id:            String  = "",
    @SerialName("driver_id")
    val driverId:      String  = "",
    val type:          String  = "sedan",
    val model:         String  = "",
    @SerialName("reg_number")
    val regNumber:     String  = "",
    @SerialName("seat_capacity")
    val seatCapacity:  Int     = 4,
    @SerialName("photo_url")
    val photoUrl:      String? = null,
    @SerialName("is_verified")
    val isVerified:    Boolean = false
)

@Serializable
data class NewVehicle(
    @SerialName("driver_id")
    val driverId:     String,
    val type:         String,
    val model:        String,
    @SerialName("reg_number")
    val regNumber:    String,
    @SerialName("seat_capacity")
    val seatCapacity: Int
)

// ─────────────────────────────────────────────────────────────────────────────
//  ROUTE BOOKING  — flat booking nested inside RouteDto (no back-ref to RouteDto)
//  Used only for the passenger list in ActiveRoutesScreen.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class RouteBookingDto(
    val id:              String   = "",
    @SerialName("passenger_id")
    val passengerId:     String   = "",
    val seats:           Int      = 1,
    @SerialName("grand_total")
    val grandTotal:      Int      = 0,
    val status:          String   = "pending",
    // joined passenger info
    val users:           UserDto? = null
)

// ─────────────────────────────────────────────────────────────────────────────
//  ROUTE
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class RouteDto(
    val id:             String  = "",
    @SerialName("driver_id")
    val driverId:       String  = "",
    @SerialName("vehicle_id")
    val vehicleId:      String? = null,
    val origin:         String  = "",
    val destination:    String  = "",
    @SerialName("origin_lat")
    val originLat:      Double? = null,
    @SerialName("origin_lng")
    val originLng:      Double? = null,
    @SerialName("dest_lat")
    val destLat:        Double? = null,
    @SerialName("dest_lng")
    val destLng:        Double? = null,
    val date:           String  = "",
    val time:           String  = "",
    @SerialName("duration_hrs")
    val durationHrs:    String  = "",
    @SerialName("seats_total")
    val seatsTotal:     Int     = 4,
    @SerialName("seats_left")
    val seatsLeft:      Int     = 4,
    @SerialName("fare_per_seat")
    val farePerSeat:    Int     = 0,
    val status:         String  = "upcoming",
    @SerialName("created_at")
    val createdAt:      String? = null,
    // joined driver info
    val users:          UserDto?            = null,
    // joined vehicle info
    val vehicles:       VehicleDto?           = null,
    // joined bookings with passenger info — uses RouteBookingDto to avoid circular ref
    val bookings:       List<RouteBookingDto> = emptyList()
)

@Serializable
data class NewRoute(
    @SerialName("driver_id")
    val driverId:       String,
    @SerialName("vehicle_id")
    val vehicleId:      String?,
    val origin:         String,
    val destination:    String,
    val date:           String,
    val time:           String,
    @SerialName("duration_hrs")
    val durationHrs:    String,
    @SerialName("seats_total")
    val seatsTotal:     Int,
    @SerialName("seats_left")
    val seatsLeft:      Int,
    @SerialName("fare_per_seat")
    val farePerSeat:    Int
)

// ─────────────────────────────────────────────────────────────────────────────
//  BOOKING
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class BookingDto(
    val id:               String   = "",
    @SerialName("route_id")
    val routeId:          String   = "",
    @SerialName("passenger_id")
    val passengerId:      String   = "",
    val seats:            Int      = 1,
    @SerialName("total_fare")
    val totalFare:        Int      = 0,
    @SerialName("service_fee")
    val serviceFee:       Int      = 0,
    @SerialName("grand_total")
    val grandTotal:       Int      = 0,
    @SerialName("payment_method")
    val paymentMethod:    String   = "cash",
    @SerialName("payment_status")
    val paymentStatus:    String   = "pending",
    @SerialName("paid_at")
    val paidAt:           String?  = null,
    val status:           String   = "pending",
    @SerialName("booking_ref")
    val bookingRef:       String   = "",
    @SerialName("has_review")
    val hasReview:        Boolean  = false,
    @SerialName("created_at")
    val createdAt:        String?  = null,
    // joins
    val routes:           RouteDto? = null,
    val users:            UserDto?  = null
)

@Serializable
data class NewBooking(
    @SerialName("route_id")
    val routeId:       String,
    @SerialName("passenger_id")
    val passengerId:   String,
    val seats:         Int,
    @SerialName("total_fare")
    val totalFare:     Int,
    @SerialName("service_fee")
    val serviceFee:    Int,
    @SerialName("grand_total")
    val grandTotal:    Int,
    @SerialName("payment_method")
    val paymentMethod: String = "cash"
)

// ─────────────────────────────────────────────────────────────────────────────
//  REVIEW
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ReviewDto(
    val id:               String              = "",
    @SerialName("booking_id")
    val bookingId:        String              = "",
    @SerialName("driver_id")
    val driverId:         String              = "",
    @SerialName("reviewer_id")
    val reviewerId:       String              = "",
    @SerialName("overall_rating")
    val overallRating:    Int                 = 5,
    @SerialName("aspect_ratings")
    val aspectRatings:    Map<String, Int>    = emptyMap(),
    val tags:             List<String>        = emptyList(),
    val comment:          String              = "",
    @SerialName("created_at")
    val createdAt:        String?             = null,
    val users:            UserDto?            = null
)

@Serializable
data class NewReview(
    @SerialName("booking_id")
    val bookingId:      String,
    @SerialName("driver_id")
    val driverId:       String,
    @SerialName("reviewer_id")
    val reviewerId:     String,
    @SerialName("overall_rating")
    val overallRating:  Int,
    @SerialName("aspect_ratings")
    val aspectRatings:  Map<String, Int>,
    val tags:           List<String>,
    val comment:        String
)

// ─────────────────────────────────────────────────────────────────────────────
//  LOCATION
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class LocationDto(
    @SerialName("trip_id")
    val tripId:     String  = "",
    @SerialName("driver_id")
    val driverId:   String  = "",
    val lat:        Double  = 0.0,
    val lng:        Double  = 0.0,
    @SerialName("speed_kmh")
    val speedKmh:   Float   = 0f,
    @SerialName("heading_deg")
    val headingDeg: Float?  = null,
    @SerialName("recorded_at")
    val recordedAt: String? = null
)

@Serializable
data class UpsertLocation(
    @SerialName("trip_id")
    val tripId:     String,
    @SerialName("driver_id")
    val driverId:   String,
    val lat:        Double,
    val lng:        Double,
    @SerialName("speed_kmh")
    val speedKmh:   Float   = 0f,
    @SerialName("heading_deg")
    val headingDeg: Float?  = null
)

// ─────────────────────────────────────────────────────────────────────────────
//  UI STATE WRAPPERS
// ─────────────────────────────────────────────────────────────────────────────

sealed class UiState<out T> {
    data object Loading                        : UiState<Nothing>()
    data class  Success<T>(val data: T)        : UiState<T>()
    data class  Error(val message: String)     : UiState<Nothing>()
    data object Idle                           : UiState<Nothing>()
}

sealed class ActionResult {
    data object Loading                        : ActionResult()
    data object Success                        : ActionResult()
    data class  Error(val message: String)     : ActionResult()
    data object Idle                           : ActionResult()
}