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

import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.TestLoggerPlugin
import com.adarshr.gradle.testlogger.theme.ThemeType
import io.freefair.gradle.plugins.lombok.LombokExtension
import io.freefair.gradle.plugins.lombok.LombokPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.JavaTestFixturesPlugin
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin

class BasePlugin : Plugin<Project> {
    companion object {
        // Compiler Micronaut
        const val ANNOTATION_PATH = "com.bakdata.quick.*"
        const val INCREMENTAL_PROCESSING = true

        // Versions
        val JAVA_VERSION = JavaLanguageVersion.of(11)
    }

    override fun apply(project: Project) {
        with(project) {
            group = "com.bakdata.quick"
            version = findProperty("version")!!.toString()


            val baseExtension = extensions.create("quick", BaseExtension::class)

            configureJava()
            configureLogging()
            configureTests()
            configureDependencyToCommon()
            configureMicronautCompiler()
            configureLombok()

            plugins.apply(VersionPlugin::class)
            plugins.apply(QuickCodeQualityPlugin::class)

            // Do not apply docker release for libraries like common as they are not executable
            afterEvaluate {
                if (baseExtension.type == BaseExtension.ProjectType.APPLICATION) {
                    plugins.apply(QuickJibPlugin::class)
                }
            }
        }
    }

    private fun Project.configureLombok() {
        plugins.apply(LombokPlugin::class)
        // disable config tasks - we only need a single one in the root directory
        configure<LombokExtension> {
            disableConfig.set(true)
        }
    }

    private fun Project.configureTests() {
        tasks.withType<Test> {
            maxHeapSize = "1048m"
            useJUnitPlatform()
            // improve startup time for tests
            jvmArgs = listOf("-noverify", "-XX:TieredStopAtLevel=1")
        }

        val unitTest = tasks.register<Test>("unitTest") {
            description = "Runs unit tests."
            group = LifecycleBasePlugin.VERIFICATION_GROUP

            useJUnitPlatform {
                excludeTags("integration-test")
            }
        }

        val integrationTest = tasks.register<Test>("integrationTest") {
            description = "Runs integration tests."
            group = LifecycleBasePlugin.VERIFICATION_GROUP

            useJUnitPlatform {
                // see com.bakdata.quick.common.tags.IntegrationTest
                includeTags("integration-test")
            }

            shouldRunAfter(unitTest)
        }

        tasks.named("check", DefaultTask::class) {
            dependsOn(integrationTest)
        }
    }

    private fun Project.configureJava() {
        plugins.apply(JavaPlugin::class)
        plugins.apply(JavaTestFixturesPlugin::class)
        configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JAVA_VERSION)
        }

        if (extensions.getByType(BaseExtension::class).type == BaseExtension.ProjectType.APPLICATION) {
            plugins.apply(ApplicationPlugin::class)
        }
    }

    private fun Project.configureMicronautCompiler() {
        tasks.withType<JavaCompile> {
            options.compilerArgs.addAll(
                listOf(
                    // retain parameter names
                    // https://docs.micronaut.io/latest/guide/index.html#retainparameternames
                    "-parameters",
                    // allows incremental annotation processing
                    // https://docs.micronaut.io/latest/guide/index.html#incrementalannotationgradle
                    "-Amicronaut.processing.incremental=$INCREMENTAL_PROCESSING",
                    "-Amicronaut.processing.annotations=$ANNOTATION_PATH",
                )
            )
        }
    }

    private fun Project.configureDependencyToCommon() {
        if (name != "common") {
            dependencies {
                add("implementation", (project(":common")))
                add("testImplementation", (testFixtures(project(":common"))))
            }
        }
    }

    private fun Project.configureLogging() {
        // make sure we never include these as transitive dependencies
        configurations.all {
            exclude(group = "org.slf4j", module = "slf4j-log4j12")
            exclude(group = "log4j", module = "log4j")
        }

        plugins.apply(TestLoggerPlugin::class)
        configure<TestLoggerExtension> {
            logLevel = LogLevel.LIFECYCLE
            theme = ThemeType.MOCHA
            slowThreshold = 3000

            showExceptions = true
            showFullStackTraces = true
            showCauses = true

            showStandardStreams = true
            showPassedStandardStreams = false
            showSkippedStandardStreams = false
            showFailedStandardStreams = true
        }
    }
}
