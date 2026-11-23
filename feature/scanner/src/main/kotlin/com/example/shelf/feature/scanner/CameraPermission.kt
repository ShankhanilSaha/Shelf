package com.example.shelf.feature.scanner

/** Camera permission as the scanner screen needs to know it. */
enum class CameraPermission {
    GRANTED,

    /** Never asked: the scanner requests it when it opens. */
    NOT_REQUESTED,

    /** Refused, but Android will still show the dialog again. */
    DENIED,

    /** Refused with "don't ask again" (or twice): only system settings can grant it now. */
    PERMANENTLY_DENIED,
}
