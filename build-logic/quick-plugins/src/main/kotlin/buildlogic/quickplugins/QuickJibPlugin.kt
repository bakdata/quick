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

import com.google.cloud.tools.jib.gradle.JibExtension
import com.google.cloud.tools.jib.gradle.JibPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class QuickJibPlugin : Plugin<Project> {
    companion object {
        const val IMAGE_REPO = "bakdata/"
        const val TAG_ENV_NAME = "QUICK_DEFAULT_IMAGE_TAG"
    }

    override fun apply(project: Project) {
        with(project)
        {
            plugins.apply(JibPlugin::class)

            afterEvaluate {
                configure<JibExtension> {
                    to {
                        image = IMAGE_REPO + "quick-" + name
                        tags = setOf(version.toString())
                        container {
                            environment = mapOf(TAG_ENV_NAME to version.toString())
                        }
                    }
                }
            }
        }
    }
}
