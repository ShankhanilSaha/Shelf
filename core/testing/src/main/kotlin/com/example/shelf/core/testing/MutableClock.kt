package com.example.shelf.core.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock tests can move forward (and between time zones). */
class MutableClock(var now: Instant = TestData.NOW, var timeZone: ZoneId = ZoneOffset.UTC) : Clock() {
    override fun getZone(): ZoneId = timeZone

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)

    override fun instant(): Instant = now

    fun advanceBy(duration: Duration) {
        now += duration
    }
}
