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

import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
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

            // https://docs.gradle.org/7.5.1/userguide/platforms.html#sub:type-unsafe-access-to-catalog
            val librariesExtension = extensions.getByType<VersionCatalogsExtension>().named("libs")

            val micronautVersion = librariesExtension.findVersion("micronaut").get().requiredVersion
            val kafkaVersion = librariesExtension.findVersion("kafka").get().requiredVersion
            plugins.apply(DependencyManagementPlugin::class)
            extensions.getByType(DependencyManagementExtension::class).imports {
                mavenBom("io.micronaut:micronaut-bom:$micronautVersion") {
                    bomProperty("kafka.version", kafkaVersion)
                }
            }
        }
    }
}
