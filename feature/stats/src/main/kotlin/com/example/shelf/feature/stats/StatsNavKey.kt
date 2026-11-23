package com.example.shelf.feature.stats

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Destinations of the stats feature. */
@Serializable
sealed interface StatsNavKey : NavKey {
    @Serializable
    data object Overview : StatsNavKey

    /** The "Log reading" bottom sheet, shown above [Overview]. Target of `shelf://log`. */
    @Serializable
    data object LogReading : StatsNavKey
}
