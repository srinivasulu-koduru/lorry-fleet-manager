package com.lorryowner.app.ui.owner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class GreetingHelperTest {

    // =========================================================================
    // 1. MORNING: 5:00 AM to 11:59 AM -> "Good Morning, Owner"
    // =========================================================================

    @Test
    fun testMorning_atExactStartBoundary_5_00_AM() {
        val time = LocalTime.of(5, 0, 0, 0)
        assertEquals("Good Morning", GreetingHelper.getGreeting(time))
        assertEquals("Good Morning, Owner", GreetingHelper.getFullGreeting("Owner", time))
    }

    @Test
    fun testMorning_oneSecondAfterStart_5_00_01_AM() {
        val time = LocalTime.of(5, 0, 1)
        assertEquals("Good Morning", GreetingHelper.getGreeting(time))
    }

    @Test
    fun testMorning_midMorning_8_30_AM() {
        val time = LocalTime.of(8, 30, 0)
        assertEquals("Good Morning", GreetingHelper.getGreeting(time))
        assertEquals("Good Morning, Owner", GreetingHelper.getFullGreeting("Owner", time))
    }

    @Test
    fun testMorning_atExactEndBoundary_11_59_59_AM() {
        val time = LocalTime.of(11, 59, 59, 999_999_999)
        assertEquals("Good Morning", GreetingHelper.getGreeting(time))
        assertEquals("Good Morning, Owner", GreetingHelper.getFullGreeting("Owner", time))
    }

    // =========================================================================
    // 2. AFTERNOON: 12:00 PM to 4:59 PM -> "Good Afternoon, Owner"
    // =========================================================================

    @Test
    fun testAfternoon_atExactStartBoundary_12_00_PM() {
        val time = LocalTime.of(12, 0, 0, 0)
        assertEquals("Good Afternoon", GreetingHelper.getGreeting(time))
        assertEquals("Good Afternoon, Owner", GreetingHelper.getFullGreeting("Owner", time))
    }

    @Test
    fun testAfternoon_oneSecondAfterStart_12_00_01_PM() {
        val time = LocalTime.of(12, 0, 1)
        assertEquals("Good Afternoon", GreetingHelper.getGreeting(time))
    }

    @Test
    fun testAfternoon_midAfternoon_14_30_PM() {
        val time = LocalTime.of(14, 30, 0)
        assertEquals("Good Afternoon", GreetingHelper.getGreeting(time))
        assertEquals("Good Afternoon, Owner", GreetingHelper.getFullGreeting("Owner", time))
    }

    @Test
    fun testAfternoon_atExactEndBoundary_16_59_59_PM() {
        val time = LocalTime.of(16, 59, 59, 999_999_999)
        assertEquals("Good Afternoon", GreetingHelper.getGreeting(time))
        assertEquals("Good Afternoon, Owner", GreetingHelper.getFullGreeting("Owner", time))
    }

    // =========================================================================
    // 3. EVENING: 5:00 PM to 8:59 PM -> "Good Evening, Owner"
    // =========================================================================

    @Test
    fun testEvening_atExactStartBoundary_17_00_PM() {
        val time = LocalTime.of(17, 0, 0, 0)
        assertEquals("Good Evening", GreetingHelper.getGreeting(time))
        assertEquals("Good Evening, Owner", GreetingHelper.getFullGreeting("Owner", time))
    }

    @Test
    fun testEvening_oneSecondAfterStart_17_00_01_PM() {
        val time = LocalTime.of(17, 0, 1)
        assertEquals("Good Evening", GreetingHelper.getGreeting(time))
    }

    @Test
    fun testEvening_midEvening_19_15_PM() {
        val time = LocalTime.of(19, 15, 0)
        assertEquals("Good Evening", GreetingHelper.getGreeting(time))
        assertEquals("Good Evening, Owner", GreetingHelper.getFullGreeting("Owner", time))
    }

    @Test
    fun testEvening_atExactEndBoundary_20_59_59_PM() {
        val time = LocalTime.of(20, 59, 59, 999_999_999)
        assertEquals("Good Evening", GreetingHelper.getGreeting(time))
        assertEquals("Good Evening, Owner", GreetingHelper.getFullGreeting("Owner", time))
    }

    // =========================================================================
    // 4. NIGHT: 9:00 PM to 4:59 AM -> "Good Night, Owner"
    // =========================================================================

    @Test
    fun testNight_atExactStartBoundary_21_00_PM() {
        val time = LocalTime.of(21, 0, 0, 0)
        assertEquals("Good Night", GreetingHelper.getGreeting(time))
        assertEquals("Good Night, Owner", GreetingHelper.getFullGreeting("Owner", time))
    }

    @Test
    fun testNight_oneSecondAfterStart_21_00_01_PM() {
        val time = LocalTime.of(21, 0, 1)
        assertEquals("Good Night", GreetingHelper.getGreeting(time))
    }

    @Test
    fun testNight_lateNight_23_30_PM() {
        val time = LocalTime.of(23, 30, 0)
        assertEquals("Good Night", GreetingHelper.getGreeting(time))
    }

    @Test
    fun testNight_justBeforeMidnight_23_59_59_PM() {
        val time = LocalTime.of(23, 59, 59, 999_999_999)
        assertEquals("Good Night", GreetingHelper.getGreeting(time))
    }

    @Test
    fun testNight_exactMidnight_00_00_AM() {
        val time = LocalTime.of(0, 0, 0, 0)
        assertEquals("Good Night", GreetingHelper.getGreeting(time))
        assertEquals("Good Night, Owner", GreetingHelper.getFullGreeting("Owner", time))
    }

    @Test
    fun testNight_earlyHours_02_30_AM() {
        val time = LocalTime.of(2, 30, 0)
        assertEquals("Good Night", GreetingHelper.getGreeting(time))
    }

    @Test
    fun testNight_atExactEndBoundary_04_59_59_AM() {
        val time = LocalTime.of(4, 59, 59, 999_999_999)
        assertEquals("Good Night", GreetingHelper.getGreeting(time))
        assertEquals("Good Night, Owner", GreetingHelper.getFullGreeting("Owner", time))
    }

    // =========================================================================
    // 5. TRANSITIONS: Millis until next boundary
    // =========================================================================

    @Test
    fun testMillisUntilNextTransition_approachingNoon() {
        // 11:59:50 AM -> 10 seconds before 12:00 PM
        val time = LocalTime.of(11, 59, 50, 0)
        val millis = GreetingHelper.getMillisUntilNextTransition(time)
        // 10 seconds = 10,000 ms (+ 200ms buffer = 10,200 ms)
        assertTrue("Expected approx 10,200ms, was $millis", millis in 9_000L..11_000L)
    }

    @Test
    fun testMillisUntilNextTransition_pastNightBoundaryWrapsTo5AM() {
        // 22:00 (10 PM) -> next boundary is 05:00 next day (7 hours = 25,200 seconds)
        val time = LocalTime.of(22, 0, 0, 0)
        val millis = GreetingHelper.getMillisUntilNextTransition(time)
        val expectedMin = 6 * 3600 * 1000L // > 6 hours
        assertTrue("Expected > 6 hours until 5 AM, was $millis", millis > expectedMin)
    }

    // =========================================================================
    // 6. Custom Owner Name formatting
    // =========================================================================

    @Test
    fun testFullGreeting_withCustomName() {
        val morningTime = LocalTime.of(9, 0)
        assertEquals("Good Morning, Srinivasulu", GreetingHelper.getFullGreeting("Srinivasulu", morningTime))

        val nightTime = LocalTime.of(22, 0)
        assertEquals("Good Night, Ramesh", GreetingHelper.getFullGreeting("Ramesh", nightTime))
    }
}
