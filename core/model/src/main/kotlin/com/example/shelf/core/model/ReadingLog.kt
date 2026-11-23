package com.example.shelf.core.model

import java.time.LocalDate

/**
 * One reading session. [date] is the calendar day in the user's time zone at the moment the
 * session was logged, so later time-zone changes do not move it to another day.
 */
data class ReadingLog(
    val id: String,
    val date: LocalDate,
    val pages: Int,
    val minutes: Int,
    val workKey: String? = null,
) {
    init {
        require(pages >= 0 && minutes >= 0) { "pages and minutes must not be negative" }
        require(pages > 0 || minutes > 0) { "a log needs pages or minutes" }
    }
}
