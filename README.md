# LokaliseGradlePlugin

[![Test Plugin](https://github.com/ioki-mobility/LokaliseGradlePlugin/actions/workflows/test-plugin.yml/badge.svg)](https://github.com/ioki-mobility/LokaliseGradlePlugin/actions/workflows/test-plugin.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.ioki.lokalise/lokalise-gradle-plugin?labelColor=%2324292E&color=%233246c8)](https://central.sonatype.com/namespace/com.ioki.lokalise)<!-- Disbaled because of:  https://github.com/badges/shields/pull/10997
[![Snapshot](https://img.shields.io/nexus/s/com.ioki.lokalise/lokalise-gradle-plugin?labelColor=%2324292E&color=%234f78ff&server=https://s01.oss.sonatype.org)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/ioki/lokalise/) -->
[![MIT](https://img.shields.io/badge/license-MIT-blue.svg?labelColor=%2324292E&color=%23d11064)](https://github.com/ioki-mobility/LokaliseGradlePlugin/blob/main/LICENSE.md)

A Gradle plugin that can up- and downloads strings from [lokalise](https://lokalise.com).

## Usage

### Apply the plugin

Add the plugin to the `build.gradle[.kts]` file:

```kotlin
plugins {
    id("com.ioki.lokalise") version "[CURRENT_VERSION]"
}
```

### Configuration

After you applied the plugin, you have access to the `lokalise` extension.
You need to set up the `apiToken` as well as the `projectId` there:
```kotlin
lokalise {
    apiToken.set(providers.environmentVariable("LOKALISE_API_TOKEN"))
    projectId.set(providers.environmentVariable("LOKALISE_PROJECT_ID"))
}
```

#### Upload configuration

To configure the upload you can use the `lokalise.uploadStringsConfig` function:
```kotlin
val filesToUpload = provider {
  fileTree(rootDir) {
    include("**/values/strings.xml")
    exclude("**/build/**")
  }
}
lokalise {
    uploadStringsConfig {
        translationsFilesToUpload.set(filesToUpload)
        params = mapOf(
          "replace_modified" to true,
          "cleanup_mode" to true,
          "distinguish_by_file" to true,
          "lang_iso" to "en_BZ",
        )
    }    
}
```

The plugin provides a `uploadTranslations` task that uses the configuration you upload the given translation files.
Which parameter you can use can be found in the [Lokalise API documentation "Upload a file"](https://developers.lokalise.com/reference/upload-a-file).

#### Download configuration

To configure the download you can use the `lokalise.downloadStringsConfigs` function.
Be note that, in contrast to the upload config, you can create multiple download configurations:
```kotlin
lokalise {
    downloadStringsConfigs {
        register("main") {
            arguments = listOf(
                "--format", "xml",
                "--filter-langs", "en,de,de_CH,fr_CH,es,it,nl,ca,ar",
            )
        }
    }
}
```

The `lokalise.downloadStringsConfigs` function is a [NamedDomainObjectContainer](https://docs.gradle.org/8.1.1/javadoc/org/gradle/api/NamedDomainObjectContainer.html) that
configured a `DownloadStringsConfig`.
Which parameter you can use can be found in the [Lokalise API documentation "Download files"](https://developers.lokalise.com/reference/download-files).
Each of the created configurations will create a Gradle tasks named like the following:
```
downloadTranslationsFor[name]
```

Mostly you don't need registering more than one. 
But you can register multiple ones in case you want to download only a subset of strings for a specific use case. 
For example, if you only want to download spanish strings you can do this:

```kotlin
downloadStringsConfigs {
    register("main") {
        arguments = listOf(
            "--format", "xml",
            "--filter-langs", "en,de,de_CH,fr_CH,es,it,nl,ca,ar",
        )
    }
    register("spanishOnly") {
        arguments = listOf(
            "--format", "xml",
            "--filter-langs", "es",
        )
    }
}

```

This will generate two tasks: `downloadTranslationsForMain` and `downloadTranslationsForSpanishhOnly`.
If you run the latter, it will only download the translated strings for spanish.

There is also an `downloadTranslationsForAll` task that aggregates all created tasks to run all of them together.

Optional, you can set to each download config the boolean `checkTranslationProcess` to `true`.
If enabled, it will check if everything is translated on Lokalise **before** downloading the strings.
If set, and it is not translated by 100%, the build will fail.
This is quite useful on CI pipelines to make sure that you don't ship half translated apps
**before starting a heavy build/lint task**.

You can set this property like this:
```kotlin
lokalise {
    downloadStringsConfigs {
        register("main") {
            checkTranslationProcess = true
        }
    }
}
```

#### Polling configuration (optional, default `true`)

By default, the plugin will poll the Lokalise API until the upload is finished.
This is helpful to catch errors with the uploaded files.

However, if you want to disable this behaviour you can set the `pollUploadProcess` property to `false`:
```kotlin
lokalise {
  pollUploadProcess.set(false)
}
```

# Release

## Snapshot release

By default, each merge to the `main` branch will create a new SNAPSHOT release.
If you want to use the latest and greatest use the SNAPSHOT version of the plugin.
But please be aware that they might contain bugs or behaviour changes.

To use the SNAPSHOT version you have to include the sonatype snapshot repository to your `settings.gradle[.kts]`
```kotlin
pluginManagement {
    repositories {
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/")
    }
}
```

## Proper release

* Checkout `main` branch
* Update the `version` in [`build.gradle.kts`](build.gradle.kts)
* Update the `version` in the instrumentation test `consuming of plugin publication via mavenLocal works`
* Commit with message `Prepare next relaese`
* Tag the version with the same version and push
    * `git tag [VERSION]`
    * `git push origin [VERSION]`
* Update the `version` in [`build.gradle.kts`](build.gradle.kts) to the next **patch** version +`-SNAPSHOT`
* Update the `version` in the instrumentation test `consuming of plugin publication via mavenLocal works`
* Commit and push
* Create a new [release](https://github.com/ioki-mobility/LokaliseGradlePlugin/releases/new) 
