package com.kunpitech.pahadiraah.data.repository

import com.kunpitech.pahadiraah.data.model.NewReview
import com.kunpitech.pahadiraah.data.model.ReviewDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  INTERFACE
// ─────────────────────────────────────────────────────────────────────────────

interface ReviewRepository {
    /** Submit a review for a completed trip. */
    suspend fun submitReview(review: NewReview): Result<ReviewDto>

    /** Get all reviews for a driver (shown on DriverProfileScreen). */
    suspend fun getDriverReviews(driverId: String): Result<List<ReviewDto>>

    /** Check if a booking already has a review (to show ✓ in MyBookings). */
    suspend fun hasReview(bookingId: String): Result<Boolean>
}

// ─────────────────────────────────────────────────────────────────────────────
//  IMPLEMENTATION
// ─────────────────────────────────────────────────────────────────────────────

class ReviewRepositoryImpl @Inject constructor(
    private val client: SupabaseClient
) : ReviewRepository {

    private val table get() = client.postgrest["reviews"]

    override suspend fun submitReview(review: NewReview): Result<ReviewDto> = runCatching {
        // 1. Insert the review row
        val saved = table
            .insert(review) { select() }
            .decodeSingle<ReviewDto>()

        // 2. Mark the booking as reviewed so the "Rate Trip" button hides in MyBookings
        runCatching {
            client.postgrest["bookings"].update({
                set("has_review", true)
            }) {
                filter { eq("id", review.bookingId) }
                select()
            }
        }   // fire-and-forget — don't fail the whole submit if this update fails

        saved
    }

    override suspend fun getDriverReviews(driverId: String): Result<List<ReviewDto>> =
        runCatching {
            table
                .select(
                    Columns.raw("*, users!reviewer_id(name, emoji)")
                ) {
                    filter { eq("driver_id", driverId) }
                    order("created_at", Order.DESCENDING)
                    limit(20)
                }
                .decodeList<ReviewDto>()
        }

    override suspend fun hasReview(bookingId: String): Result<Boolean> = runCatching {
        val results = table
            .select(Columns.raw("id")) {
                filter { eq("booking_id", bookingId) }
                limit(1)
            }
            .decodeList<ReviewDto>()
        results.isNotEmpty()
    }
}