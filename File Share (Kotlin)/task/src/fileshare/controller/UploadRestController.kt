package fileshare.controller

import fileshare.model.UpdatedFilesInfo
import org.springframework.core.env.Environment
import org.springframework.core.io.PathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.notExists

@RestController
class UploadRestController(env: Environment) {

    private val uploadDirPath = env.getRequiredProperty("uploads.dir")
    private val uploadDir = File(uploadDirPath).apply { mkdirs() }

    @PostMapping("/api/v1/upload")
    fun uploadFile(@RequestParam file: MultipartFile): ResponseEntity<Unit> {

        val safeFilename = URLEncoder.encode(
            file.originalFilename,
            StandardCharsets.UTF_8
        )

        val path = Path.of(uploadDirPath, safeFilename)
        file.transferTo(path)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header(
                "Location",
                "http://localhost:8888/api/v1/download/$safeFilename"
            )
            .build()
    }

    @GetMapping("/api/v1/info")
    fun getUpdatedFilesInfo(): ResponseEntity<UpdatedFilesInfo> {

        val updatedFilesInfo = uploadDir.listFiles().let { files ->
            UpdatedFilesInfo(
                totalFiles = files?.size ?: 0,
                totalBytes = files?.sumOf { file -> file.toPath().fileSize() } ?: 0
            )
        }

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
