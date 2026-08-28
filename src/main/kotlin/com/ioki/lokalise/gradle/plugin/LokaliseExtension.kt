package com.ioki.lokalise.gradle.plugin

import com.ioki.lokalise.api.models.DownloadFilesRequest
import com.ioki.lokalise.api.models.UploadFileRequest
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

    /**
     * Downloads translations "async" via the LokaliseApi.
     * This should set to `true` if the project contains more than 10.000 key-language pairs.
     * See also https://developers.lokalise.com/reference/file-download-limitations
     */
    val downloadStringsAsynchronously: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

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
) : Named {
    /**
     * Checks if all translations are done.
     * If set to true and translations are not done, the task will fail.
     */
    abstract val checkTranslationProcess: Property<Boolean>

    /**
     * The request body used for downloading translations.
     * See also the [Lokalise API documentation "Download files"](https://developers.lokalise.com/reference/download-files).
     */
    abstract val requestBody: Property<DownloadFilesRequest>

    override fun getName(): String = name
}

abstract class UploadStringsConfig @Inject constructor(objects: ObjectFactory) {
    val translationsFilesToUpload: Property<ConfigurableFileTree> = objects.property(ConfigurableFileTree::class.java)

    /**
     * The request body used for uploading translations.
     * The `data` and `filename` fields are ignored/overwritten per uploaded file, so they can be left empty.
     * See also the [Lokalise API documentation "Upload a file"](https://developers.lokalise.com/reference/upload-a-file).
     */
    abstract val requestBody: Property<UploadFileRequest>
}