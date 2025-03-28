package fileshare.service

import fileshare.md5
import fileshare.model.FileStorageInfo
import fileshare.model.UpdatedFilesInfo
import fileshare.repository.FileRepository
import fileshare.usecase.FileValidator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.PathResource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.io.File
import kotlin.io.path.exists

@Service
class FileManagementService(
    private val repository: FileRepository,
    private val fileStorageService: FileStorageService,
    private val fileValidator: FileValidator,
) {
    @Value("\${api.base-path}")
    private lateinit var basePath: String

    private val logger = LoggerFactory.getLogger(FileManagementService::class.java)

    fun save(file: MultipartFile): FileStorageInfo {
        val originalFilename = StringUtils.cleanPath(file.originalFilename.orEmpty())
        val fileExtension = File(originalFilename).extension.lowercase()
        val fileMd5 = file.bytes.md5()

        repository.findByIdOrNull(fileMd5)?.let { existingFile ->
            return existingFile.copy(name = originalFilename).also { repository.save(it) }
        }

        if (!hasEnoughSpace(file.size)) throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE)
        if (!isFileValid(file)) throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE)

        val downloadUri = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("$basePath/download/")
            .path(fileMd5)
            .toUriString()

        val savedFile = repository.save(
            FileStorageInfo(
                id = fileMd5,
                name = originalFilename,
                extension = fileExtension,
                contentType = file.contentType.orEmpty(),
                downloadUri = downloadUri
            )
        )

        fileStorageService.storeFile(file, savedFile)

        return savedFile
    }

    fun getFilesInfo(): UpdatedFilesInfo {
        return fileStorageService.getStoredFilesInfo()
    }

    fun downloadFile(fileId: String): Pair<HttpHeaders, StreamingResponseBody>? {
        val savedFile = findFileById(fileId) ?: return null
        if (!fileStorageService.fileExists(savedFile)) return null

        val path = fileStorageService.getStoredFilePath(savedFile)

        val resource = PathResource(path)
        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType(savedFile.contentType)
            contentLength = resource.contentLength()
            contentDisposition = ContentDisposition.attachment().filename(savedFile.name).build()
        }

        return headers to StreamingResponseBody { outputStream ->
            resource.inputStream.copyTo(outputStream)
        }
    }

    private fun findFileById(id: String): FileStorageInfo? {
        val savedFile = repository.findByIdOrNull(id) ?: return null
        val filePath = fileStorageService.getStoredFilePath(savedFile)

        return savedFile.takeIf { filePath.exists() } ?: run {
            logger.warn("File metadata exists but file is missing: $filePath")
            null
        }
    }

    private fun isFileValid(file: MultipartFile): Boolean {
        return fileValidator.validate(file.contentType.orEmpty(), file.bytes)
    }

    private fun hasEnoughSpace(fileSize: Long): Boolean {
        val usedSpace = fileStorageService.getUsedStorage()
        return fileSize <= STORAGE_LIMIT - usedSpace && fileSize <= FILE_LIMIT
    }

    companion object {
        private const val STORAGE_LIMIT = 200 * 1000
        private const val FILE_LIMIT = 50 * 1000
    }
}
