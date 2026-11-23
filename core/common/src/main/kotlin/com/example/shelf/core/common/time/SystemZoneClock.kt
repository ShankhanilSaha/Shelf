package com.example.shelf.core.common.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * A system clock that always reports the *current* default time zone. `Clock.systemDefaultZone()`
 * captures the zone once, which would keep using the old zone after the user travels.
 */
object SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()

    override fun millis(): Long = System.currentTimeMillis()
}
