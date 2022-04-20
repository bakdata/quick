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

package buildlogic.convention

import buildlogic.libraries.QuickLibraries
import buildlogic.libraries.QuickLibrariesPlugin
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.repositories
import java.net.URI

class VersionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {

            repositories {
                mavenCentral()
                maven {
                    url = URI.create("https://packages.confluent.io/maven/")
                }
            }

            plugins.apply(BaseDependenciesPlugin::class)
            plugins.apply(QuickLibrariesPlugin::class)
            val librariesExtension = extensions.getByType(QuickLibraries::class)
            plugins.apply(DependencyManagementPlugin::class)
            extensions.getByType(DependencyManagementExtension::class).imports {
                mavenBom("io.micronaut:micronaut-bom:" + librariesExtension.MICRONAUT_VERSION) {
                    bomProperty("kafka.version", librariesExtension.KAFKA_VERSION)
                }
            }
        }
    }
}
