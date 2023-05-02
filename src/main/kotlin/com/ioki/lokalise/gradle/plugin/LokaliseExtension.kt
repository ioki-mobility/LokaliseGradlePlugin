package com.ioki.lokalise.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property

internal fun ExtensionContainer.createLokaliseExtension(): LokaliseExtension =
    create("lokalise", LokaliseExtension::class.java)

abstract class LokaliseExtension(
    objects: ObjectFactory
) {
    val apiToken: Property<String> = objects.property(String::class.java)

    val projectId: Property<String> = objects.property(String::class.java)

    val translationsFilesToUpload: Property<ConfigurableFileTree> = objects.property(ConfigurableFileTree::class.java)

    internal val downloadStringsConfigs: NamedDomainObjectContainer<DownloadStringsConfig> =
        objects.domainObjectContainer(DownloadStringsConfig::class.java)

    fun downloadStringsConfigs(action: Action<NamedDomainObjectContainer<DownloadStringsConfig>>) {
        action.execute(downloadStringsConfigs)
    }
}

abstract class DownloadStringsConfig(
    private val name: String,
    var arguments: List<String>,
) : Named {
    override fun getName(): String = name
}