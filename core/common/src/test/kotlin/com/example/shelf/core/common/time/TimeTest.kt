package com.example.shelf.core.common.time

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimeTest {
    private val newYork = ZoneId.of("America/New_York")

    private fun clockAt(dateTime: String, zone: ZoneId = newYork): Clock =
        Clock.fixed(ZonedDateTime.of(java.time.LocalDateTime.parse(dateTime), zone).toInstant(), zone)

    @Test
    fun `reminder later today is scheduled today`() {
        val clock = clockAt("2026-03-02T08:00")
        assertEquals(Duration.ofHours(12), clock.durationUntilNext(LocalTime.of(20, 0)))
    }

    @Test
    fun `reminder time already passed is scheduled tomorrow`() {
        val clock = clockAt("2026-03-02T21:00")
        val next = clock.nextOccurrenceOf(LocalTime.of(20, 0))
        assertEquals(LocalDate.of(2026, 3, 3), next.toLocalDate())
        assertEquals(Duration.ofHours(23), clock.durationUntilNext(LocalTime.of(20, 0)))
    }

    @Test
    fun `reminder at exactly now is scheduled tomorrow, never zero`() {
        val clock = clockAt("2026-03-02T20:00")
        assertEquals(Duration.ofHours(24), clock.durationUntilNext(LocalTime.of(20, 0)))
    }

    @Test
    fun `next day across spring-forward is 23 hours of real time`() {
        // DST starts in New York on 2026-03-08 at 02:00.
        val clock = clockAt("2026-03-07T20:00")
        assertEquals(Duration.ofHours(23), clock.durationUntilNext(LocalTime.of(20, 0)))
    }

    @Test
    fun `time inside the DST gap resolves to an existing instant`() {
        val clock = clockAt("2026-03-07T23:00")
        val next = clock.nextOccurrenceOf(LocalTime.of(2, 30))
        assertEquals(LocalTime.of(3, 30), next.toLocalTime())
        assertTrue(next.toInstant().isAfter(clock.instant()))
    }

    @Test
    fun `today follows the clock zone`() {
        val instant = ZonedDateTime.of(2026, 1, 2, 3, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals(LocalDate.of(2026, 1, 2), Clock.fixed(instant, ZoneId.of("UTC")).today())
        assertEquals(LocalDate.of(2026, 1, 1), Clock.fixed(instant, ZoneId.of("America/Los_Angeles")).today())
    }

    @Test
    fun `system zone clock reports the current default zone`() {
        assertEquals(ZoneId.systemDefault(), SystemZoneClock.zone)
        assertEquals(ZoneId.of("UTC"), SystemZoneClock.withZone(ZoneId.of("UTC")).zone)
    }
}
