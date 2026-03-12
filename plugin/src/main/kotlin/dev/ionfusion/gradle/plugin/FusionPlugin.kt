// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project


class FusionPlugin: Plugin<Project>
{
    override fun apply(project: Project)
    {
        project.tasks.register("greeting") {
            task -> task.doLast {
                println("Hello from plugin 'fusion'")
            }
        }
    }
}
