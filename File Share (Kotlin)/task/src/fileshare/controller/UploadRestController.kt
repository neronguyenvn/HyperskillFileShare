package fileshare.controller

import fileshare.model.UpdatedFilesInfo
import org.springframework.core.env.Environment
import org.springframework.core.io.PathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.notExists

@RestController
class UploadRestController(env: Environment) {

    private val uploadDirPath = env.getRequiredProperty("uploads.dir")
    private val uploadDir = File(uploadDirPath).apply { mkdirs() }

    @PostMapping("/api/v1/upload")
    fun uploadFile(@RequestParam file: MultipartFile): ResponseEntity<Unit> {
        val originalFilename = file
            .originalFilename?.trim()
            ?: return ResponseEntity.badRequest().build()

        val safeFilename = URLEncoder.encode(
            originalFilename,
            StandardCharsets.UTF_8
        )

        val path = Path.of(uploadDirPath, safeFilename)
        file.transferTo(path)

        val downloadUri = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/api/v1/download/")
            .path(safeFilename)
            .toUriString()

        return ResponseEntity
            .created(URI.create(downloadUri))
            .build()
    }

    @GetMapping("/api/v1/info")
    fun getUpdatedFilesInfo(): ResponseEntity<UpdatedFilesInfo> {
        val files = uploadDir.listFiles()?.filter { it.isFile } ?: emptyList()

        val updatedFilesInfo = UpdatedFilesInfo(
            totalFiles = files.size,
            totalBytes = files.sumOf { it.length() }
        )

        return ResponseEntity.ok(updatedFilesInfo)
    }

    @GetMapping("/api/v1/download/{fileName}")
    fun downloadFile(@PathVariable fileName: String): ResponseEntity<StreamingResponseBody> {
        val path = Path.of(uploadDirPath, fileName)
        if (path.notExists()) {
            return ResponseEntity.notFound().build()
        }

        val resource = PathResource(path)
        val responseBody = StreamingResponseBody { outputStream ->
            resource.inputStream.copyTo(outputStream)
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_OCTET_STREAM
            contentLength = resource.contentLength()
            setContentDispositionFormData("attachment", resource.filename)
        }

        return ResponseEntity
            .ok()
            .headers(headers)
            .body(responseBody)
    }
}
