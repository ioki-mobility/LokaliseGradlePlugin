# LokaliseGradlePlugin

[![Test Plugin](https://github.com/ioki-mobility/LokaliseGradlePlugin/actions/workflows/test-plugin.yml/badge.svg)](https://github.com/ioki-mobility/LokaliseGradlePlugin/actions/workflows/test-plugin.yml)
[![Jitpack](https://jitpack.io/v/ioki-mobility/LokaliseGradlePlugin.svg)](https://jitpack.io/#ioki-mobility/LokaliseGradlePlugin)
[![MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/ioki-mobility/LokaliseGradlePlugin/blob/master/LICENSE.md)

A Gradle plugin that up- and downloads strings from [lokalise](https://lokalise.com) using the [lokalise CLI](https://github.com/lokalise/lokalise-cli-2-go) under the hood.

## Usage

### Apply the plugin

Add [JitPack](https://jitpack.io/) to the `settings.gradle[.kts]` file:

```groovy
pluginManagement {
    repositories {
        maven { 
            url("https://jitpack.io")
            content {
                includeGroup("com.github.ioki-mobility.LokaliseGradlePlugin")
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
lokalise {
    uploadStringsConfig {
        translationsFilesToUpload.set(filesToUpload)
        arguments(
            "--replace-modified",
            "--cleanup-mode",
            "--include-path",
            "--distinguish-by-file",
            "--lang-iso", "en_BZ",
            "--poll"
        )
    }    
}
```

The plugin provides a `uploadTranslations` task that uses the configuration you upload the given translation files.
The base arguments for that tasks that are put to the lokalise CLI are:
```
file upload --token [TOKEN] --project-id [PROJECT_ID] --file [trnslationsFilesToUploadAsString]
```
The arguments you provide via the extension will be added afterward. 
In our example you would end up with:
```
file upload --token [TOKEN] --project-id [PROJECT_ID] --file [trnslationsFilesToUploadAsString] /
    --replace-modified --cleanup-mode --include-path --distinguish-by-file --lang-iso en_BZ --poll
```

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
With that you can configure the arguments you want to put to the CLI [file download](https://github.com/lokalise/lokalise-cli-2-go/blob/604673f0b9bdb4faf1e94fe77a0b5ceb249f4c6c/docs/lokalise2_file_download.md) command.
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

This will generate two tasks `downloadTranslationsForMain` and `downloadTranslationsForSpanishhOnly`.
If you run the latter, it will only download the translated strings for spanish.

By default, there is also on `downloadTranslationsForAll` task that will execute all the created tasks in case you 
have to execute all of them after each other.

# Release

* Checkout `main` branch
* Update the `version` in [`build.gradle.kts`](build.gradle.kts)
* Update the `version` in the instrumentation test `consuming of plugin marker publication works`
* Commit with message `Next release`
* Tag the version with the same version and push
    * `git tag -a [VERSION] -m "Next release`
    * `git push origin [VERSION]`
* Create a new [release](https://github.com/ioki-mobility/LokaliseGradlePlugin/releases/new) 
