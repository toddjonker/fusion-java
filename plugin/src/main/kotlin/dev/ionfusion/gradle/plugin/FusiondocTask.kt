package dev.ionfusion.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class FusiondocTask : DefaultTask() {
    @TaskAction
    fun greet() {
        println("hello from fusiondoc")
    }
}
