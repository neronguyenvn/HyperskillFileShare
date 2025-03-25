package fileshare.controller

import fileshare.model.UpdatedFilesInfo
import fileshare.service.FileService
import org.springframework.core.env.Environment
import org.springframework.core.io.PathResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
    env: Environment,
) {
    private val uploadDirPath = env.getRequiredProperty("uploads.dir")

    @PostMapping("/api/v1/upload")
    fun uploadFile(@RequestParam file: MultipartFile): ResponseEntity<Unit> {
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
}
