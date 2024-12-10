package com.ioki.lokalise.gradle.plugin.tasks

import com.ioki.lokalise.gradle.plugin.LokaliseProjectApi
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

internal abstract class CheckEverythingTranslatedTask : DefaultTask() {

    @get:Input
    abstract val lokaliseApiFactory: Property<() -> LokaliseProjectApi>

    @TaskAction
    fun f() {
        val project = runBlocking { lokaliseApiFactory.get().invoke().getProject() }
        if(project.statistics.progressTotal != 9) {
            throw GradleException("Not all keys are translated")
        }
    }
}