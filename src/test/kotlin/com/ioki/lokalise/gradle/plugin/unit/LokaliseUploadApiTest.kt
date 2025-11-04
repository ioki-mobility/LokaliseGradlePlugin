package com.ioki.lokalise.gradle.plugin.unit

import com.ioki.lokalise.api.Lokalise
import com.ioki.lokalise.api.models.Error
import com.ioki.lokalise.api.models.FileDownload
import com.ioki.lokalise.api.models.FileDownloadAsync
import com.ioki.lokalise.api.models.FileUpload
import com.ioki.lokalise.api.models.FileUploadDetails
import com.ioki.lokalise.api.models.Process
import com.ioki.lokalise.api.models.Project
import com.ioki.lokalise.api.models.Projects
import com.ioki.lokalise.api.models.RetrievedProcess
import com.ioki.lokalise.gradle.plugin.DefaultLokaliseApi
import com.ioki.lokalise.gradle.plugin.FileInfo
import com.ioki.result.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@OptIn(ExperimentalCoroutinesApi::class)
class LokaliseUploadApiTest {

    @Test
    fun `concurrency uploadFile with 1-6 files does not delay and is done after 0 millis`() = runTest {
        val lokalise = createLokalise()
        val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

        (0..6).forEach {
            expectThat(currentTime).isEqualTo(0)
            lokaliseApi.uploadFiles(
                fileInfos = mutableListOf<FileInfo>().apply {
                    repeat(it) { add(createFileInfo()) }
                },
                langIso = "langIso",
                params = mapOf(),
            )
            expectThat(currentTime).isEqualTo(0)
        }
    }

    @Test
    fun `concurrency uploadFile with 7-12 files delay once and is done after 1000 millis`() = runTest {
        val lokalise = createLokalise()
        val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

        var expectedTime = 0L
        (7..12).forEach {
            expectThat(currentTime).isEqualTo(expectedTime)
            lokaliseApi.uploadFiles(
                fileInfos = mutableListOf<FileInfo>().apply {
                    repeat(it) { add(createFileInfo()) }
                },
                langIso = "langIso",
                params = mapOf(),
            )
            expectedTime += 1000
            expectThat(currentTime).isEqualTo(expectedTime)
        }
    }

    @Test
    fun `concurrency uploadFile with 13-18 files delay twice and is done after 2000 millis`() = runTest {
        val lokalise = createLokalise()
        val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

        var expectedTime = 0L
        (13..18).forEach {
            expectThat(currentTime).isEqualTo(expectedTime)
            lokaliseApi.uploadFiles(
                fileInfos = mutableListOf<FileInfo>().apply {
                    repeat(it) { add(createFileInfo()) }
                },
                langIso = "langIso",
                params = mapOf(),
            )
            expectedTime += 2000
            expectThat(currentTime).isEqualTo(expectedTime)
        }
    }

    @Test
    fun `concurrency uploadFile with 5 files and 1200 delay in upload should wait for upload only`() = runTest {
        val lokalise = createLokalise(
            uploadFileResult = {
                delay(1200)
                Result.Success(createFileUpload())
            }
        )
        val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

        expectThat(currentTime).isEqualTo(0L)
        lokaliseApi.uploadFiles(
            fileInfos = mutableListOf<FileInfo>().apply {
                repeat(5) { add(createFileInfo()) }
            },
            langIso = "langIso",
            params = mapOf(),
        )
        expectThat(currentTime).isEqualTo(1200)
    }

    @Test
    fun `concurrency uploadFile with 7 files and 700 delay in upload should wait for delay of files plus last upload`() =
        runTest {
            val lokalise = createLokalise(
                uploadFileResult = {
                    delay(700)
                    Result.Success(createFileUpload())
                }
            )
            val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

            expectThat(currentTime).isEqualTo(0L)
            lokaliseApi.uploadFiles(
                fileInfos = mutableListOf<FileInfo>().apply {
                    repeat(7) { add(createFileInfo()) }
                },
                langIso = "langIso",
                params = mapOf(),
            )
            expectThat(currentTime).isEqualTo(1700)
        }

    @Test
    fun `concurrency checkProcess with 1-6 files does not delay and is done after 0 millis`() = runTest {
        val lokalise = createLokalise()
        val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

        (0..6).forEach {
            expectThat(currentTime).isEqualTo(0)
            lokaliseApi.checkProcess(
                fileUploads = mutableListOf<FileUpload>().apply {
                    repeat(it) { add(createFileUpload()) }
                },
            )
            expectThat(currentTime).isEqualTo(0)
        }
    }

    @Test
    fun `concurrency checkProcess with 7-12 files delay once and is done after 1000 millis`() = runTest {
        val lokalise = createLokalise()
        val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

        var expectedTime = 0L
        (7..12).forEach {
            expectThat(currentTime).isEqualTo(expectedTime)
            lokaliseApi.checkProcess(
                fileUploads = mutableListOf<FileUpload>().apply {
                    repeat(it) { add(createFileUpload()) }
                },
            )
            expectedTime += 1000
            expectThat(currentTime).isEqualTo(expectedTime)
        }
    }

    @Test
    fun `concurrency checkProcess with 13-18 files delay twice and is done after 2000 millis`() = runTest {
        val lokalise = createLokalise()
        val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

        var expectedTime = 0L
        (13..18).forEach {
            expectThat(currentTime).isEqualTo(expectedTime)
            lokaliseApi.checkProcess(
                fileUploads = mutableListOf<FileUpload>().apply {
                    repeat(it) { add(createFileUpload()) }
                },
            )
            expectedTime += 2000
            expectThat(currentTime).isEqualTo(expectedTime)
        }
    }

    @Test
    fun `concurrency checkProcess with 5 files and 1200 delay in upload should wait for upload only`() = runTest {
        val lokalise = createLokalise(
            retrieveProcessResult = {
                delay(1200)
                Result.Success(createRetrieveProcess(status = "finished"))
            }
        )
        val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

        expectThat(currentTime).isEqualTo(0L)
        lokaliseApi.checkProcess(
            fileUploads = mutableListOf<FileUpload>().apply {
                repeat(5) { add(createFileUpload()) }
            },
        )
        expectThat(currentTime).isEqualTo(1200)
    }

    @Test
    fun `concurrency checkProcess with 7 files and 700 delay in upload should wait for delay of files plus last upload`() =
        runTest {
            val lokalise = createLokalise(
                retrieveProcessResult = {
                    delay(700)
                    Result.Success(createRetrieveProcess(status = "finished"))
                }
            )
            val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

            expectThat(currentTime).isEqualTo(0L)
            lokaliseApi.checkProcess(
                fileUploads = mutableListOf<FileUpload>().apply {
                    repeat(7) { add(createFileUpload()) }
                },
            )
            expectThat(currentTime).isEqualTo(1700)
        }

    @Test
    fun `concurrency checkProcess with 1 file should delay inside while for 1500`() = runTest {
        var timesCheckProcess = 0
        val lokalise = createLokalise(
            retrieveProcessResult = {
                timesCheckProcess += 1
                val status = if (timesCheckProcess > 3) "finished" else "notFinished"
                Result.Success(createRetrieveProcess(status = status))
            }
        )
        val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

        expectThat(currentTime).isEqualTo(0L)
        lokaliseApi.checkProcess(
            fileUploads = mutableListOf<FileUpload>().apply {
                repeat(1) { add(createFileUpload()) }
            },
        )
        expectThat(currentTime).isEqualTo(1500)
    }
}

private fun createFileUpload(
    projectId: String = "",
    processId: String = "",
    status: String = "",
    type: String = "",
    message: String = "",
    createdBy: Int = 0,
    createdByEmail: String = "",
    createdAt: String = "",
    createdAtTimestamp: Int = 0
): FileUpload = FileUpload(
    projectId = projectId,
    process = FileUpload.Process(
        processId = processId,
        status = status,
        type = type,
        message = message,
        createdBy = createdBy,
        createdByEmail = createdByEmail,
        createdAt = createdAt,
        createdAtTimestamp = createdAtTimestamp
    )
)

private fun createFileInfo(
    fileName: String = "",
    base64FileContent: String = ""
): FileInfo = FileInfo(
    fileName = fileName,
    base64FileContent = base64FileContent
)

private fun createRetrieveProcess(
    processId: String = "",
    status: String = "",
    type: String = "",
    message: String = "",
    createdBy: Int = 0,
    createdByEmail: String = "",
    createdAt: String = "",
    createdAtTimestamp: Long = 0
): RetrievedProcess = RetrievedProcess(
    process = Process.FileUpload(
        processId = processId,
        status = status,
        type = type,
        message = message,
        createdBy = createdBy,
        createdByEmail = createdByEmail,
        createdAt = createdAt,
        createdAtTimestamp = createdAtTimestamp,
        details = FileUploadDetails(
            emptyList()
        ),
    )
)

private fun createLokalise(
    uploadFileResult: suspend () -> Result<FileUpload, Error> = { Result.Success(createFileUpload()) },
    retrieveProcessResult: suspend () -> Result<RetrievedProcess, Error> = { Result.Success(createRetrieveProcess(status = "finished")) }
): Lokalise = object : FakeLokalise() {
    override suspend fun uploadFile(
        projectId: String,
        data: String,
        filename: String,
        langIso: String,
        bodyParams: Map<String, Any>
    ): Result<FileUpload, Error> = uploadFileResult()

    override suspend fun retrieveProcess(
        projectId: String,
        processId: String
    ): Result<RetrievedProcess, Error> = retrieveProcessResult()
}

private open class FakeLokalise : Lokalise {
    override suspend fun retrieveProject(projectId: String): Result<Project, Error> {
        error("Not overriden")
    }

    override suspend fun allProjects(queryParams: Map<String, Any>): Result<Projects, Error> {
        error("Not overriden")
    }

    override suspend fun downloadFiles(
        projectId: String,
        format: String,
        bodyParams: Map<String, Any>
    ): Result<FileDownload, Error> {
        error("Not overriden")
    }

    override suspend fun downloadFilesAsync(
        projectId: String,
        format: String,
        bodyParams: Map<String, Any>
    ): Result<FileDownloadAsync, Error> {
        error("Not overriden")
    }

    override suspend fun retrieveProcess(projectId: String, processId: String): Result<RetrievedProcess, Error> {
        error("Not overriden")
    }

    override suspend fun uploadFile(
        projectId: String,
        data: String,
        filename: String,
        langIso: String,
        bodyParams: Map<String, Any>
    ): Result<FileUpload, Error> {
        error("Not overriden")
    }
}
