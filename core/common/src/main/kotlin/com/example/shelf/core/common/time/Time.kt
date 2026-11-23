package com.example.shelf.core.common.time

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Today's date in the clock's time zone. */
fun Clock.today(): LocalDate = LocalDate.now(this)

/**
 * The next moment the wall clock shows [time], strictly after now. Daylight-saving gaps are
 * resolved by [ZonedDateTime.of] (moved forward by the gap length), so the result always exists.
 */
fun Clock.nextOccurrenceOf(time: LocalTime): ZonedDateTime {
    val now = ZonedDateTime.now(this)
    val todayAtTime = ZonedDateTime.of(now.toLocalDate(), time, zone)
    return if (todayAtTime.isAfter(now)) {
        todayAtTime
    } else {
        ZonedDateTime.of(now.toLocalDate().plusDays(1), time, zone)
    }
}

/** How long until the next [time] on the wall clock. Always positive. */
fun Clock.durationUntilNext(time: LocalTime): Duration = Duration.between(instant(), nextOccurrenceOf(time).toInstant())

/** Start of [date] in the clock's zone, as an instant. */
fun Clock.startOfDay(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant()
