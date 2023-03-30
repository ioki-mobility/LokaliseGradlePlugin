package com.ioki.lokalise.gradle.plugin.tasks

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

internal abstract class UnzipLokaliseCliTask : Copy() {

    override fun copy() {
        super.copy()
        project.exec { t ->
            t.commandLine(
                "chmod",
                "+x",
                "${destinationDir}/lokalise",
            )
        }
    }

}

internal fun TaskContainer.registerUnzipLokaliseCliTask(
    downloadLokaliseCliTask: Provider<DownloadLokaliseCliTask>
): TaskProvider<UnzipLokaliseCliTask> = register("unzipLokaliseCli", UnzipLokaliseCliTask::class.java) {
    it.from(it.project.tarTree(downloadLokaliseCliTask.map { task -> task.lokaliseCliZipFile.get() }))
    it.rename("lokalise2", "lokalise")
    it.into(it.project.buildDir.toString() + "/lokalise/cli")
}