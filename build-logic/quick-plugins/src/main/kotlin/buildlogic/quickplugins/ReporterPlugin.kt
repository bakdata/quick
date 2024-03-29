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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.sonarqube.gradle.SonarQubeExtension
import org.sonarqube.gradle.SonarQubePlugin
import org.sonarqube.gradle.SonarQubeTask


/**
 * Plugin for reporting coverage metrics.
 *
 * This plugin aggregates binary code coverage reports generated by JaCoCo, merges them and creates an unified XML report.
 * Furthermore, it allows the export to sonarqube.
 *
 * @see <a href="https://docs.gradle.org/7.1/samples/sample_jvm_multi_project_with_code_coverage.html">Gradle Documentation</a>
 * @see <a href="https://github.com/open-telemetry/opentelemetry-java/blob/main/all/build.gradle.kts">Example</a>
 */
class ReporterPlugin : Plugin<Project> {
    companion object {
        // Path for the aggregated code coverage report
        const val REPORT_PATH = "codeCoverageReport/codeCoverageReport.xml"
    }

    override fun apply(project: Project) {
        with(project) {
            plugins.apply(JavaLibraryPlugin::class)
            plugins.apply(SonarQubePlugin::class)
            plugins.apply(JacocoPlugin::class)
            plugins.apply(VersionPlugin::class)

            configureJacoco()
            configureSonarqube()
        }
    }

    private fun Project.configureJacoco() {

        // A resolvable configuration to collect source code
        val aggregate by configurations.creating {
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = false
        }

        // This plugin collects only reports from dependent projects
        rootProject.subprojects.forEach {
            if (it.name != this.name) {
                this.dependencies {
                    aggregate(it)
                }
            }
        }

        // Resolvable configuration to resolve the classes of all dependencies
        val classPath by configurations.creating {
            isVisible = false
            isCanBeResolved = true
            isCanBeConsumed = false
            extendsFrom(aggregate)
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
                attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
            }
        }

        val sourcesPath: Configuration by configurations.creating {
            isVisible = false
            isCanBeResolved = true
            isCanBeConsumed = false
            extendsFrom(aggregate)
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
            }
        }

        // A resolvable configuration to collect JaCoCo coverage data
        val coverageDataPath: Configuration by configurations.creating {
            isVisible = false
            isCanBeResolved = true
            isCanBeConsumed = false
            extendsFrom(aggregate)
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("jacoco-coverage-data"))
            }
        }

        // Register a code coverage report task to generate the aggregated report
        val codeCoverageReport by tasks.registering(JacocoReport::class) {
            group = QuickCodeQualityPlugin.CODE_QUALITY_GROUP
            description = "Merges Jacoco reports from all dependent projects."

            additionalClassDirs(classPath.filter { it.isDirectory })
            additionalSourceDirs(sourcesPath.incoming.artifactView { lenient(true) }.files)
            executionData(coverageDataPath.incoming.artifactView { lenient(true) }.files.filter { it.exists() })

            reports {
                html.required.set(true) // human readable format
                xml.required.set(true) // required for sonarqube
            }
        }

        // Make JaCoCo report generation part of the 'check' lifecycle phase
        tasks.named("check").configure {
            dependsOn(codeCoverageReport)
        }
    }

    private fun Project.configureSonarqube() {
        // Sets the path for the aggregated jacoco report generated by this plugin
        val reportPath = extensions.getByType(JacocoPluginExtension::class).reportsDirectory.dir(REPORT_PATH)
        extensions.configure(SonarQubeExtension::class) {
            properties {
                property("sonar.coverage.jacoco.xmlReportPaths", reportPath)
            }
        }

        tasks.withType(SonarQubeTask::class) {
            group = QuickCodeQualityPlugin.CODE_QUALITY_GROUP
        }
    }
}
