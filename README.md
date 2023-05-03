# LokaliseGradlePlugin

A Gradle plugin that uploads and downloads strings from [lokalise](https://lokalise.com) using the [lokalise CLI](https://github.com/lokalise/lokalise-cli-2-go) under the hood.   

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

Add the plugin to the `build.gradle[.kts]` file and configure it:

```kotlin
plugins {
    id("com.ioki.lokalise") version "[CURRENT_VERSION]"
}

val filesToUpload = provider {
    fileTree(rootDir) {
        include("**/values/strings.xml")
    }
}
lokalise {
    apiToken.set(providers.environmentVariable("LOKALISE_API_TOKEN"))
    projectId.set(providers.environmentVariable("LOKALISE_PROJECT_ID"))
    translationsFilesToUpload.set(filesToUpload)
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

The `lokalise.downloadStringsConfigs` function is a [NamedDomainObjectContainer](https://docs.gradle.org/8.1.1/javadoc/org/gradle/api/NamedDomainObjectContainer.html) thats
needs to be configure a `DownloadStringsConfig`. 
With that you can configure the arguments you want to put to the CLI [file download](https://github.com/lokalise/lokalise-cli-2-go/blob/604673f0b9bdb4faf1e94fe77a0b5ceb249f4c6c/docs/lokalise2_file_download.md) command.
Each of the created configurations will create a Gradle tasks named like the following:
```
downloadTranslationsFor[name]
```

Mostly you don't need more than registering one. 
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
    register("frenchOnly) {
        arguments = listOf(
            "--format", "xml",
            "--filter-langs", "es",
        )
    }
}

```

This will generate two tasks `downloadTranslationsForMain` and `downloadTranslationsForFrenchOnly`.
If you run the latter, it will only download the translated strings for french.

# Release

* Checkout `main` branch
* Update the `version` in [`build.gradle.kts`](build.gradle.kts)
* Commit with message `Next version`
* Tag the version with the same version and push it to origin
* Update the version to the "next **minor** version" (including `-SNAPSHOT`)
* Push to origin
* Create a new [release](https://github.com/ioki-mobility/LokaliseGradlePlugin/releases/new) 
