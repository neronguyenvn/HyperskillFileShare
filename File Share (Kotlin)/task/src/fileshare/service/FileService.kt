package fileshare.service

import fileshare.md5
import fileshare.model.UpdatedFilesInfo
import fileshare.model.UploadedFile
import fileshare.repository.FileRepository
import fileshare.usecase.FileValidator
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
import java.nio.file.Path
import kotlin.io.path.exists

@Service
class FileService(
    private val repository: FileRepository,
    private val fileValidator: FileValidator,
) {
    @Value("\${api.base-path}")
    private lateinit var basePath: String

    @Value("\${uploads.dir}")
    private lateinit var uploadDirPath: String

    private val uploadDir by lazy { File(uploadDirPath).apply { mkdirs() } }

    fun save(file: MultipartFile): UploadedFile {
        val originalFilename = StringUtils.cleanPath(file.originalFilename.orEmpty())
        val fileExtension = originalFilename.substringAfterLast('.')
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
            UploadedFile(
                id = fileMd5,
                name = originalFilename,
                extension = fileExtension,
                contentType = file.contentType.orEmpty(),
                downloadUri = downloadUri
            )
        )

        file.transferTo(Path.of(uploadDirPath, "${savedFile.id}.${savedFile.extension}"))

        return savedFile
    }

    fun getFilesInfo(): UpdatedFilesInfo {
        val storedFiles = uploadDir.listFiles()?.filter { it.isFile }.orEmpty()
        return UpdatedFilesInfo(
            totalFiles = storedFiles.size,
            totalBytes = storedFiles.sumOf { it.length() }
        )
    }

    fun downloadFile(fileId: String): Pair<HttpHeaders, StreamingResponseBody>? {
        val savedFile = findFileById(fileId) ?: return null
        val path = Path.of(uploadDirPath, "${savedFile.id}.${savedFile.extension}")

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

    private fun findFileById(id: String): UploadedFile? {
        val savedFile = repository.findByIdOrNull(id) ?: return null
        val filePath = Path.of(uploadDirPath, "${savedFile.id}.${savedFile.extension}")
        return if (filePath.exists()) savedFile else null
    }

    private fun isFileValid(file: MultipartFile): Boolean {
        return fileValidator.isValidFile(file.contentType.orEmpty(), file.bytes)
    }

    private fun hasEnoughSpace(fileSize: Long): Boolean {
        val usedSpace = uploadDir.listFiles()?.sumOf { it.length() } ?: 0L
        return fileSize <= STORAGE_LIMIT - usedSpace && fileSize <= FILE_LIMIT
    }

    companion object {
        private const val STORAGE_LIMIT = 200 * 1000
        private const val FILE_LIMIT = 50 * 1000
    }
}
