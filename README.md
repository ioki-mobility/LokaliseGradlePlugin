# LokaliseGradlePlugin

[![Test Plugin](https://github.com/ioki-mobility/LokaliseGradlePlugin/actions/workflows/test-plugin.yml/badge.svg)](https://github.com/ioki-mobility/LokaliseGradlePlugin/actions/workflows/test-plugin.yml)
[![Jitpack](https://jitpack.io/v/ioki-mobility/LokaliseGradlePlugin.svg)](https://jitpack.io/#ioki-mobility/LokaliseGradlePlugin)
[![MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/ioki-mobility/LokaliseGradlePlugin/blob/master/LICENSE.md)

A Gradle plugin that can up- and downloads strings from [lokalise](https://lokalise.com).

## Usage

### Apply the plugin

Add [JitPack](https://jitpack.io/) to the `settings.gradle[.kts]` file:

```groovy
pluginManagement {
    repositories {
        maven { 
            url("https://jitpack.io")
            content {
              includeGroupByRegex("com.github.ioki-mobility.*")
            }
        }
        resolutionStrategy {
            it.eachPlugin {
                if (requested.id.id == "com.ioki.lokalise") {
                    useModule(
                        "com.github.ioki-mobility.LokaliseGradlePlugin:lokalise:${requested.version}"
                    )
                }
            }
        }
    }
}
```

> **Note**: If you use JitPack, the `[CURRENT_VERSION]` can either be a (git) tag (recommended), branch name, or hash.

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

# Release

* Checkout `main` branch
* Update the `version` in [`build.gradle.kts`](build.gradle.kts)
* Update the `version` in the instrumentation test `consuming of plugin marker publication works`
* Commit with message `Next release`
* Tag the version with the same version and push
    * `git tag -a [VERSION] -m "Next release`
    * `git push origin [VERSION]`
* Create a new [release](https://github.com/ioki-mobility/LokaliseGradlePlugin/releases/new) 
