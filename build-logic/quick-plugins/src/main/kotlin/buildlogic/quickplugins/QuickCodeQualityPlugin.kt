/*
 *    Copyright 2022 bakdata GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package buildlogic.quickplugins

import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.ErrorPronePlugin
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

class QuickCodeQualityPlugin : Plugin<Project> {
    companion object {
        // group for gradle tasks
        const val CODE_QUALITY_GROUP = "code quality"

        private const val CHECKSTYLE_VERSION = "9.0"

        private const val ERROR_PRONE_VERSION = "2.9.0"
        private const val ERROR_PRONE_CORE = "com.google.errorprone:error_prone_core:$ERROR_PRONE_VERSION"

        private const val NULL_AWAY = "com.uber.nullaway:nullaway:0.9.2"
    }

    override fun apply(project: Project) {
        project.addCheckstylePlugin()
        project.configureStaticCodeAnalyzer()
        project.configureTestCoverage()
    }

    private fun Project.configureStaticCodeAnalyzer() {
        project.plugins.apply(ErrorPronePlugin::class)

        project.dependencies.add("errorprone", ERROR_PRONE_CORE)
        project.dependencies.add("errorprone", NULL_AWAY)

        project.tasks.withType(JavaCompile::class.java).configureEach {
            options.errorprone {
                excludedPaths.set(".*/build/generated.*")
                disableWarningsInGeneratedCode.set(true)

                // exception when lombok's @Value and @Builder are used
                disable("MissingSummary")
                // generates alot of warning when used with lombok
                disable("SameNameButDifferent")

                // nullaway only for non-tests
                val nullAwaySeverity =
                        if (name.toLowerCase().contains("test")) CheckSeverity.OFF else CheckSeverity.ERROR
                check("NullAway", nullAwaySeverity)
                option("NullAway:AnnotatedPackages", "com.bakdata.quick")
            }
        }
    }

    private fun Project.addCheckstylePlugin() {
        project.plugins.apply(CheckstylePlugin::class)
        project.configure<CheckstyleExtension> {
            this.toolVersion = CHECKSTYLE_VERSION
            this.isIgnoreFailures = false
            this.maxErrors = 0
            this.maxWarnings = 0
        }

        tasks.withType(Checkstyle::class) {
            group = CODE_QUALITY_GROUP
        }
    }

    private fun Project.configureTestCoverage() {
        plugins.apply(JacocoPlugin::class)

        // Do not generate reports for individual projects
        tasks.named("jacocoTestReport", JacocoReport::class).configure {
            enabled = false
        }

        val implementation = configurations.named("implementation").get()

        // Share sources folder with other projects for aggregated Javadoc and JaCoCo reports
        configurations.create("sourceElements") {
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true
            extendsFrom(implementation)
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
            }
            val mainSources = the<JavaPluginExtension>().sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            mainSources.java.srcDirs.forEach { outgoing.artifact(it) }
        }

        // Share the coverage data to be aggregated for the whole product
        configurations.create("coverageDataElements") {
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true
            extendsFrom(implementation)
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("jacoco-coverage-data"))
            }

            // run 'test' task when this configuration is request
            outgoing.artifact(tasks.named("test").map { task ->
                task.extensions.getByType<JacocoTaskExtension>().destinationFile!!
            })
        }
    }
}
