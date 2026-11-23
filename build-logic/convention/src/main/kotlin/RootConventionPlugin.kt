import com.example.shelf.buildlogic.CoverageSummaryTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.register

/**
 * Applied to the root project only. Registers `coverageReport`, which runs every module's
 * JaCoCo report and prints a per-module summary (also written to build/reports/coverage-summary.md).
 *
 * Enforcement: `-Pcoverage.minimum=0.7` fails the build when `:core:data` or the ViewModel
 * aggregate falls below the threshold. The default comes from gradle.properties.
 */
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) { "shelf.root must only be applied to the root project" }
        with(target) {
            val coverageReport = tasks.register<CoverageSummaryTask>("coverageReport") {
                group = "verification"
                description = "Runs unit tests with JaCoCo and prints line coverage per module."
                enforcedTargets.set(listOf(":core:data", CoverageSummaryTask.VIEW_MODELS))
                minimumLineCoverage = providers.gradleProperty("coverage.minimum").map(String::toDouble).orElse(0.0)
                summaryFile = layout.buildDirectory.file("reports/coverage-summary.md")
            }

            subprojects {
                val module = this
                // AGP's report task fails without execution data, so only modules with tests take part.
                if (!module.file("src/test").isDirectory) return@subprojects
                pluginManager.withPlugin("com.android.base") {
                    coverageReport.configure {
                        dependsOn("${module.path}:createDebugUnitTestCoverageReport")
                        reportPaths.put(
                            module.path,
                            module.layout.buildDirectory.file("reports/coverage/test/debug/report.xml")
                                .map { it.asFile.absolutePath },
                        )
                    }
                }
                pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                    coverageReport.configure {
                        dependsOn("${module.path}:jacocoTestReport")
                        reportPaths.put(
                            module.path,
                            module.layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml")
                                .map { it.asFile.absolutePath },
                        )
                    }
                }
            }
        }
    }
}
