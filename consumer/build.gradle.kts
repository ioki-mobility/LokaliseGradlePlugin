plugins {
    kotlin("jvm") version "1.9.0"
    id("com.ioki.lokalise") version "2.3.0-SNAPSHOT"
}

lokalise {
    apiToken.set("your-api-token")
    projectId.set("your-project-id")
    downloadStringsConfigs {
        create("strings") {
            params(
                "format" to "json",
                "original_filenames" to true,
                "export_empty_as" to "skip",
                "bundle_structure" to "%LANG_ISO%.%FORMAT%"
            )
        }
    }
}