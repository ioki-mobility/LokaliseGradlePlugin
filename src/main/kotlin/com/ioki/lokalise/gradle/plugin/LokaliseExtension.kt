package com.ioki.lokalise.gradle.plugin

import com.ioki.lokalise.api.models.DownloadFilesRequest
import com.ioki.lokalise.api.models.UploadFileRequest
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
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

/**
 * Configuration for downloading translations from Lokalise.
 * Each property mirrors a field of [DownloadFilesRequest]. See also the
 * [Lokalise API documentation "Download files"](https://developers.lokalise.com/reference/download-files)
 * for a description of what each property does.
 */
abstract class DownloadStringsConfig(
    private val name: String,
) : Named {
    /**
     * Checks if all translations are done.
     * If set to true and translations are not done, the task will fail.
     */
    abstract val checkTranslationProcess: Property<Boolean>

    abstract val format: Property<String>
    abstract val originalFilenames: Property<Boolean>
    abstract val bundleStructure: Property<String>
    abstract val directoryPrefix: Property<String>
    abstract val allPlatforms: Property<Boolean>
    abstract val filterLangs: ListProperty<String>
    abstract val filterData: ListProperty<String>
    abstract val filterFilenames: ListProperty<String>
    abstract val addNewlineEof: Property<Boolean>
    abstract val customTranslationStatusIds: ListProperty<String>
    abstract val includeTags: ListProperty<String>
    abstract val excludeTags: ListProperty<String>
    abstract val exportSort: Property<String>
    abstract val exportEmptyAs: Property<String>
    abstract val exportNullAs: Property<String>
    abstract val includeComments: Property<Boolean>
    abstract val includeDescription: Property<Boolean>
    abstract val includePids: ListProperty<String>
    abstract val triggers: ListProperty<String>
    abstract val filterRepositories: ListProperty<String>
    abstract val replaceBreaks: Property<Boolean>
    abstract val disableReferences: Property<Boolean>
    abstract val pluralFormat: Property<String>
    abstract val placeholderFormat: Property<String>
    abstract val webhookUrl: Property<String>
    abstract val languageMapping: ListProperty<String>
    abstract val icuNumeric: Property<Boolean>
    abstract val escapePercent: Property<Boolean>
    abstract val indentation: Property<String>
    abstract val yamlIncludeRoot: Property<Boolean>
    abstract val jsonUnescapedSlashes: Property<Boolean>
    abstract val javaPropertiesEncoding: Property<String>
    abstract val javaPropertiesSeparator: Property<String>
    abstract val bundleDescription: Property<String>
    abstract val filterProjectId: Property<String>
    abstract val compact: Property<Boolean>

    /**
     * Builds the [DownloadFilesRequest] used by the underlying `kmp-lokalise-api` library
     * from the properties configured above.
     */
    internal fun toRequestBody(): DownloadFilesRequest = DownloadFilesRequest(
        format = format.get(),
        originalFilenames = originalFilenames.getOrNull(),
        bundleStructure = bundleStructure.getOrNull(),
        directoryPrefix = directoryPrefix.getOrNull(),
        allPlatforms = allPlatforms.getOrNull(),
        filterLangs = filterLangs.getOrNull(),
        filterData = filterData.getOrNull(),
        filterFilenames = filterFilenames.getOrNull(),
        addNewlineEof = addNewlineEof.getOrNull(),
        customTranslationStatusIds = customTranslationStatusIds.getOrNull(),
        includeTags = includeTags.getOrNull(),
        excludeTags = excludeTags.getOrNull(),
        exportSort = exportSort.getOrNull(),
        exportEmptyAs = exportEmptyAs.getOrNull(),
        exportNullAs = exportNullAs.getOrNull(),
        includeComments = includeComments.getOrNull(),
        includeDescription = includeDescription.getOrNull(),
        includePids = includePids.getOrNull(),
        triggers = triggers.getOrNull(),
        filterRepositories = filterRepositories.getOrNull(),
        replaceBreaks = replaceBreaks.getOrNull(),
        disableReferences = disableReferences.getOrNull(),
        pluralFormat = pluralFormat.getOrNull(),
        placeholderFormat = placeholderFormat.getOrNull(),
        webhookUrl = webhookUrl.getOrNull(),
        languageMapping = languageMapping.getOrNull(),
        icuNumeric = icuNumeric.getOrNull(),
        escapePercent = escapePercent.getOrNull(),
        indentation = indentation.getOrNull(),
        yamlIncludeRoot = yamlIncludeRoot.getOrNull(),
        jsonUnescapedSlashes = jsonUnescapedSlashes.getOrNull(),
        javaPropertiesEncoding = javaPropertiesEncoding.getOrNull(),
        javaPropertiesSeparator = javaPropertiesSeparator.getOrNull(),
        bundleDescription = bundleDescription.getOrNull(),
        filterProjectId = filterProjectId.getOrNull(),
        compact = compact.getOrNull(),
    )

    override fun getName(): String = name
}

/**
 * Configuration for uploading translations to Lokalise.
 * Each property (besides [translationsFilesToUpload]) mirrors a field of [UploadFileRequest].
 * `data` and `filename` are filled in by the plugin per uploaded file and are therefore not
 * configurable here. See also the
 * [Lokalise API documentation "Upload a file"](https://developers.lokalise.com/reference/upload-a-file)
 * for a description of what each property does.
 */
abstract class UploadStringsConfig @Inject constructor(objects: ObjectFactory) {
    val translationsFilesToUpload: Property<ConfigurableFileTree> = objects.property(ConfigurableFileTree::class.java)

    abstract val langIso: Property<String>
    abstract val convertPlaceholders: Property<Boolean>
    abstract val detectIcuPlurals: Property<Boolean>
    abstract val tags: ListProperty<String>
    abstract val tagInsertedKeys: Property<Boolean>
    abstract val tagUpdatedKeys: Property<Boolean>
    abstract val tagSkippedKeys: Property<Boolean>
    abstract val replaceModified: Property<Boolean>
    abstract val slashnToLinebreak: Property<Boolean>
    abstract val keysToValues: Property<Boolean>
    abstract val distinguishByFile: Property<Boolean>
    abstract val applyTm: Property<Boolean>
    abstract val useAutomations: Property<Boolean>
    abstract val hiddenFromContributors: Property<Boolean>
    abstract val cleanupMode: Property<Boolean>
    abstract val customTranslationStatusIds: ListProperty<String>
    abstract val customTranslationStatusInsertedKeys: Property<Boolean>
    abstract val customTranslationStatusUpdatedKeys: Property<Boolean>
    abstract val customTranslationStatusSkippedKeys: Property<Boolean>
    abstract val skipDetectLangIso: Property<Boolean>
    abstract val format: Property<String>
    abstract val filterTaskId: Property<Int>

    /**
     * Builds the [UploadFileRequest] used by the underlying `kmp-lokalise-api` library from the
     * properties configured above. `data` and `filename` are passed as empty strings here since
     * they are overwritten per uploaded file, see [DefaultLokaliseApi.uploadFiles].
     */
    internal fun toRequestBody(): UploadFileRequest = UploadFileRequest(
        data = "",
        filename = "",
        langIso = langIso.get(),
        convertPlaceholders = convertPlaceholders.getOrNull(),
        detectIcuPlurals = detectIcuPlurals.getOrNull(),
        tags = tags.getOrNull(),
        tagInsertedKeys = tagInsertedKeys.getOrNull(),
        tagUpdatedKeys = tagUpdatedKeys.getOrNull(),
        tagSkippedKeys = tagSkippedKeys.getOrNull(),
        replaceModified = replaceModified.getOrNull(),
        slashnToLinebreak = slashnToLinebreak.getOrNull(),
        keysToValues = keysToValues.getOrNull(),
        distinguishByFile = distinguishByFile.getOrNull(),
        applyTm = applyTm.getOrNull(),
        useAutomations = useAutomations.getOrNull(),
        hiddenFromContributors = hiddenFromContributors.getOrNull(),
        cleanupMode = cleanupMode.getOrNull(),
        customTranslationStatusIds = customTranslationStatusIds.getOrNull(),
        customTranslationStatusInsertedKeys = customTranslationStatusInsertedKeys.getOrNull(),
        customTranslationStatusUpdatedKeys = customTranslationStatusUpdatedKeys.getOrNull(),
        customTranslationStatusSkippedKeys = customTranslationStatusSkippedKeys.getOrNull(),
        skipDetectLangIso = skipDetectLangIso.getOrNull(),
        format = format.getOrNull(),
        filterTaskId = filterTaskId.getOrNull(),
    )
}