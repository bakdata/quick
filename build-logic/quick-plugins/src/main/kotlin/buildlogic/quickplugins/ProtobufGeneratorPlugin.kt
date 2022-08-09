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


import com.google.protobuf.gradle.ProtobufPlugin
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get

/**
 * Plugin for code generation of Protobuf files.
 */
class ProtobufGeneratorPlugin : Plugin<Project> {
    companion object {
        const val PROTO_VERSION = "3.19.0"
        const val PROTO_ARTIFACT = "com.google.protobuf:protoc"
        const val SOURCE_DIR_FORMAT = "build/generated/source/proto/%s/java"
        val SOURCES = listOf("testFixtures")
    }

    override fun apply(project: Project) {
        with(project)
        {
            plugins.apply(ProtobufPlugin::class.java)

            protobuf {
                protoc {
                    artifact = "$PROTO_ARTIFACT:$PROTO_VERSION"
                }
            }

            // IntelliJ might not recognize generated source otherwise
            SOURCES.forEach {
                val sourceDir = String.format(SOURCE_DIR_FORMAT, it)
                extensions.getByType(SourceSetContainer::class.java)[it].java.srcDirs(sourceDir);
            }
        }
    }
}
