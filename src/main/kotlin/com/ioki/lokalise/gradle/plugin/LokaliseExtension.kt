package com.ioki.lokalise.gradle.plugin

import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property

internal fun ExtensionContainer.createLokaliseExtension(): LokaliseExtension =
    create("lokalise", LokaliseExtension::class.java)

interface LokaliseExtension {
    val apiToken: Property<String>
    val projectId: Property<String>
    val translationsFilesToUpload: Property<ConfigurableFileTree>
}