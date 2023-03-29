package com.ioki.lokalise.gradle.plugin.tasks

import org.gradle.api.tasks.Copy

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