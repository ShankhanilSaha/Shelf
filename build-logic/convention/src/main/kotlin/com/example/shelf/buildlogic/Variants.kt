package com.example.shelf.buildlogic

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder

/**
 * Unit tests run against the debug variant only, so `./gradlew test` does not execute every
 * JVM test twice. Release correctness is covered by lint (lintVital) and the R8 build.
 */
internal fun AndroidComponentsExtension<*, *, *>.disableReleaseUnitTests() {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        (variantBuilder as? HasHostTestsBuilder)
            ?.hostTests
            ?.get(HostTestBuilder.UNIT_TEST_TYPE)
            ?.enable = false
    }
}
