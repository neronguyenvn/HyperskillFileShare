package fileshare.controller

import fileshare.model.UpdatedFilesInfo
import fileshare.service.FileService
import fileshare.usecase.FileValidator
import org.springframework.core.env.Environment
import org.springframework.core.io.PathResource
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.io.File
import java.net.URI
import java.nio.file.Path

@RestController
class FileController(
    private val service: FileService,
    private val fileValidator: FileValidator,
    env: Environment,
) {
    private val uploadDirPath = env.getRequiredProperty("uploads.dir")

    @PostMapping("/api/v1/upload")
    fun uploadFile(@RequestParam file: MultipartFile): ResponseEntity<Unit> {
        val remainingAvailableSpace = STORAGE_LIMIT - service.findFiles().sumOf(File::length)
        if (file.size > remainingAvailableSpace || file.size > FILE_LIMIT) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build()
        }

        val isValid = fileValidator.isValidFile(file.contentType.orEmpty(), file.bytes)
        if (isValid != null) {
            return isValid
        }

        val savedFile = service.save(file)

        val downloadUri = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/api/v1/download/")
            .path(savedFile.id)
            .toUriString()

        return ResponseEntity
            .created(URI.create(downloadUri))
            .build()
    }

    @GetMapping("/api/v1/info")
    fun getUpdatedFilesInfo(): ResponseEntity<UpdatedFilesInfo> {
        val savedFiles = service.findFiles()

        val updatedFilesInfo = UpdatedFilesInfo(
            totalFiles = savedFiles.size,
            totalBytes = savedFiles.sumOf(File::length)
        )

        return ResponseEntity.ok(updatedFilesInfo)
    }

    @GetMapping("/api/v1/download/{fileName}")
    fun downloadFile(@PathVariable fileName: String): ResponseEntity<StreamingResponseBody> {
        val savedFile = service.findFileById(fileName) ?: run {
            return ResponseEntity.notFound().build()
        }

        val path = Path.of(uploadDirPath, "${savedFile.id}.${savedFile.extension}")
        val resource = PathResource(path)
        val responseBody = StreamingResponseBody { outputStream ->
            resource.inputStream.copyTo(outputStream)
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType(savedFile.contentType)
            contentLength = resource.contentLength()
            contentDisposition = ContentDisposition.attachment().filename(savedFile.name).build()
        }

        return ResponseEntity
            .ok()
            .headers(headers)
            .body(responseBody)
    }

    companion object {
        private const val STORAGE_LIMIT = 200 shl 10 // 200 KB
        private const val FILE_LIMIT = 50 shl 10 // 50 KB
    }
}
