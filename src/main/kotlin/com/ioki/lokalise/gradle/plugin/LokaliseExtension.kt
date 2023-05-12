package com.ioki.lokalise.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import javax.inject.Inject

internal fun ExtensionContainer.createLokaliseExtension(): LokaliseExtension =
    create("lokalise", LokaliseExtension::class.java)

abstract class LokaliseExtension(
    objects: ObjectFactory
) {
    val apiToken: Property<String> = objects.property(String::class.java)

    val projectId: Property<String> = objects.property(String::class.java)

    internal val downloadStringsConfigs: NamedDomainObjectContainer<DownloadStringsConfig> =
        objects.domainObjectContainer(DownloadStringsConfig::class.java)

    fun downloadStringsConfigs(action: Action<NamedDomainObjectContainer<DownloadStringsConfig>>) {
        action.execute(downloadStringsConfigs)
    }

    internal val uploadStringsConfig: UploadStringsConfig = objects.newInstance(UploadStringsConfig::class.java)

    fun uploadStringsConfig(action: Action<UploadStringsConfig>) {
        action.execute(uploadStringsConfig)
    }
}

abstract class DownloadStringsConfig(
    private val name: String,
) : Named, Argumenter {
    override fun getName(): String = name
}

abstract class UploadStringsConfig @Inject constructor(objects: ObjectFactory) : Argumenter {
    val translationsFilesToUpload: Property<ConfigurableFileTree> = objects.property(ConfigurableFileTree::class.java)
}

interface Argumenter {
    var arguments: List<String>
    fun arguments(vararg arguments: String) {
        this.arguments = listOf(*arguments)
    }
}