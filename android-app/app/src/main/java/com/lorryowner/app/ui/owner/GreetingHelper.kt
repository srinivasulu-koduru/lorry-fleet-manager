package com.lorryowner.app.ui.owner

import java.time.LocalTime

/**
 * Utility helper providing dynamic time-based greetings for the Owner Dashboard.
 *
 * Requirements:
 * - 05:00 AM to 11:59 AM -> "Good Morning, Owner"
 * - 12:00 PM to 04:59 PM -> "Good Afternoon, Owner"
 * - 05:00 PM to 08:59 PM -> "Good Evening, Owner"
 * - 09:00 PM to 04:59 AM -> "Good Night, Owner"
 */
object GreetingHelper {

    const val GREETING_MORNING = "Good Morning"
    const val GREETING_AFTERNOON = "Good Afternoon"
    const val GREETING_EVENING = "Good Evening"
    const val GREETING_NIGHT = "Good Night"

    /**
     * Resolves the greeting prefix based on device local time:
     * - hour 5..11 (05:00 to 11:59) -> "Good Morning"
     * - hour 12..16 (12:00 to 16:59) -> "Good Afternoon"
     * - hour 17..20 (17:00 to 20:59) -> "Good Evening"
     * - hour 21..23 or 0..4 (21:00 to 04:59) -> "Good Night"
     */
    fun getGreeting(time: LocalTime = LocalTime.now()): String {
        return when (time.hour) {
            in 5..11 -> GREETING_MORNING
            in 12..16 -> GREETING_AFTERNOON
            in 17..20 -> GREETING_EVENING
            else -> GREETING_NIGHT
        }
    }

    /**
     * Formats full greeting string with owner name, e.g. "Good Morning, Owner".
     */
    fun getFullGreeting(ownerName: String = "Owner", time: LocalTime = LocalTime.now()): String {
        return "${getGreeting(time)}, $ownerName"
    }

    /**
     * Calculates milliseconds from [time] until the next transition boundary.
     * Transition boundaries occur at:
     * - 05:00:00 (Morning)
     * - 12:00:00 (Afternoon)
     * - 17:00:00 (Evening)
     * - 21:00:00 (Night)
     */
    fun getMillisUntilNextTransition(time: LocalTime = LocalTime.now()): Long {
        val currentSeconds = time.toSecondOfDay()
        val boundaryHours = listOf(5, 12, 17, 21)
        val boundarySeconds = boundaryHours.map { it * 3600 }

        // Find next boundary in today's remaining seconds
        val nextSec = boundarySeconds.firstOrNull { it > currentSeconds }
            ?: (24 * 3600 + boundarySeconds.first()) // Wraps to 05:00 next day

        val diffSeconds = nextSec - currentSeconds
        val nanoRemainderMs = (1_000_000_000L - time.nano) / 1_000_000L
        // Add 200ms safety buffer so upon wakeup we are strictly past the boundary
        return (diffSeconds - 1) * 1000L + nanoRemainderMs + 200L
    }
}
