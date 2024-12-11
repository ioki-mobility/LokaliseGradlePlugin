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

    val pollUploadProcess: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

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
) : Named, Parameter {
    /**
     * Checks if all translations are done.
     * If set to true and translations are not done, the task will fail.
     */
    abstract val checkTranslationProcess: Property<Boolean>

    override fun getName(): String = name
}

abstract class UploadStringsConfig @Inject constructor(objects: ObjectFactory) : Parameter {
    val translationsFilesToUpload: Property<ConfigurableFileTree> = objects.property(ConfigurableFileTree::class.java)
}

interface Parameter {
    var params: Map<String, Any>
    fun params(vararg params: Pair<String, Any>) {
        this.params = params.toMap()
    }
}