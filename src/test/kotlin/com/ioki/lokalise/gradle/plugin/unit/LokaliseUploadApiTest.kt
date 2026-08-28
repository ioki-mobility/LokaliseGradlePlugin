package com.ioki.lokalise.gradle.plugin.unit

import com.ioki.lokalise.api.Lokalise
import com.ioki.lokalise.api.models.AllProjectsRequest
import com.ioki.lokalise.api.models.AllProjectsResponse
import com.ioki.lokalise.api.models.DownloadFilesAsyncResponse
import com.ioki.lokalise.api.models.DownloadFilesRequest
import com.ioki.lokalise.api.models.DownloadFilesResponse
import com.ioki.lokalise.api.models.Error
import com.ioki.lokalise.api.models.FileUploadDetails
import com.ioki.lokalise.api.models.Process
import com.ioki.lokalise.api.models.RetrieveProcessResponse
import com.ioki.lokalise.api.models.RetrieveProjectResponse
import com.ioki.lokalise.api.models.UploadFileRequest
import com.ioki.lokalise.api.models.UploadFileResponse
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
                requestBody = createUploadFileRequest(),
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
                requestBody = createUploadFileRequest(),
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
                requestBody = createUploadFileRequest(),
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
                Result.Success(createUploadFileResponse())
            }
        )
        val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

        expectThat(currentTime).isEqualTo(0L)
        lokaliseApi.uploadFiles(
            fileInfos = mutableListOf<FileInfo>().apply {
                repeat(5) { add(createFileInfo()) }
            },
            requestBody = createUploadFileRequest(),
        )
        expectThat(currentTime).isEqualTo(1200)
    }

    @Test
    fun `concurrency uploadFile with 7 files and 700 delay in upload should wait for delay of files plus last upload`() =
        runTest {
            val lokalise = createLokalise(
                uploadFileResult = {
                    delay(700)
                    Result.Success(createUploadFileResponse())
                }
            )
            val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

            expectThat(currentTime).isEqualTo(0L)
            lokaliseApi.uploadFiles(
                fileInfos = mutableListOf<FileInfo>().apply {
                    repeat(7) { add(createFileInfo()) }
                },
                requestBody = createUploadFileRequest(),
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
                fileUploads = mutableListOf<UploadFileResponse>().apply {
                    repeat(it) { add(createUploadFileResponse()) }
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
                fileUploads = mutableListOf<UploadFileResponse>().apply {
                    repeat(it) { add(createUploadFileResponse()) }
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
                fileUploads = mutableListOf<UploadFileResponse>().apply {
                    repeat(it) { add(createUploadFileResponse()) }
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
                Result.Success(createRetrieveProcessResponse(status = "finished"))
            }
        )
        val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

        expectThat(currentTime).isEqualTo(0L)
        lokaliseApi.checkProcess(
            fileUploads = mutableListOf<UploadFileResponse>().apply {
                repeat(5) { add(createUploadFileResponse()) }
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
                    Result.Success(createRetrieveProcessResponse(status = "finished"))
                }
            )
            val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

            expectThat(currentTime).isEqualTo(0L)
            lokaliseApi.checkProcess(
                fileUploads = mutableListOf<UploadFileResponse>().apply {
                    repeat(7) { add(createUploadFileResponse()) }
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
                Result.Success(createRetrieveProcessResponse(status = status))
            }
        )
        val lokaliseApi = DefaultLokaliseApi(lokalise, "projectId")

        expectThat(currentTime).isEqualTo(0L)
        lokaliseApi.checkProcess(
            fileUploads = mutableListOf<UploadFileResponse>().apply {
                repeat(1) { add(createUploadFileResponse()) }
            },
        )
        expectThat(currentTime).isEqualTo(1500)
    }
}

private fun createUploadFileRequest(
    data: String = "",
    filename: String = "",
    langIso: String = "langIso",
): UploadFileRequest = UploadFileRequest(
    data = data,
    filename = filename,
    langIso = langIso,
)

private fun createUploadFileResponse(
    projectId: String = "",
    processId: String = "",
    status: String = "",
    type: String = "",
    message: String = "",
    createdBy: Int = 0,
    createdByEmail: String = "",
    createdAt: String = "",
    createdAtTimestamp: Int = 0
): UploadFileResponse = UploadFileResponse(
    projectId = projectId,
    process = UploadFileResponse.Process(
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

private fun createRetrieveProcessResponse(
    processId: String = "",
    status: String = "",
    type: String = "",
    message: String = "",
    createdBy: Int = 0,
    createdByEmail: String = "",
    createdAt: String = "",
    createdAtTimestamp: Long = 0
): RetrieveProcessResponse = RetrieveProcessResponse(
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
    uploadFileResult: suspend () -> Result<UploadFileResponse, Error> = { Result.Success(createUploadFileResponse()) },
    retrieveProcessResult: suspend () -> Result<RetrieveProcessResponse, Error> =
        { Result.Success(createRetrieveProcessResponse(status = "finished")) }
): Lokalise = object : FakeLokalise() {
    override suspend fun uploadFile(
        projectId: String,
        requestBody: UploadFileRequest,
    ): Result<UploadFileResponse, Error> = uploadFileResult()

    override suspend fun retrieveProcess(
        projectId: String,
        processId: String
    ): Result<RetrieveProcessResponse, Error> = retrieveProcessResult()
}

private open class FakeLokalise : Lokalise {
    override suspend fun retrieveProject(projectId: String): Result<RetrieveProjectResponse, Error> {
        error("Not overriden")
    }

    override suspend fun allProjects(params: AllProjectsRequest?): Result<AllProjectsResponse, Error> {
        error("Not overriden")
    }

    override suspend fun downloadFiles(
        projectId: String,
        requestBody: DownloadFilesRequest,
    ): Result<DownloadFilesResponse, Error> {
        error("Not overriden")
    }

    override suspend fun downloadFilesAsync(
        projectId: String,
        requestBody: DownloadFilesRequest,
    ): Result<DownloadFilesAsyncResponse, Error> {
        error("Not overriden")
    }

    override suspend fun retrieveProcess(projectId: String, processId: String): Result<RetrieveProcessResponse, Error> {
        error("Not overriden")
    }

    override suspend fun uploadFile(
        projectId: String,
        requestBody: UploadFileRequest,
    ): Result<UploadFileResponse, Error> {
        error("Not overriden")
    }
}
