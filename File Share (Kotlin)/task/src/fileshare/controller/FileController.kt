package fileshare.controller

import fileshare.model.UpdatedFilesInfo
import fileshare.service.FileService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.net.URI

@RestController
class FileController(private val service: FileService) {
    @PostMapping("/api/v1/upload")
    fun uploadFile(@RequestParam file: MultipartFile): ResponseEntity<Unit> {
        return runCatching { service.save(file) }
            .fold(
                onSuccess = { savedFile ->
                    ResponseEntity.created(URI.create(savedFile.downloadUri)).build()
                },
                onFailure = { e ->
                    ResponseEntity.status(
                        when (e) {
                            is FileService.InvalidFileType -> HttpStatus.UNSUPPORTED_MEDIA_TYPE
                            is FileService.InvalidFileSize -> HttpStatus.PAYLOAD_TOO_LARGE
                            else -> HttpStatus.BAD_REQUEST
                        }
                    ).build()
                }
            )
    }

    @GetMapping("/api/v1/info")
    fun getUpdatedFilesInfo(): ResponseEntity<UpdatedFilesInfo> {
        val updatedFilesInfo = service.getFilesInfo()
        return ResponseEntity.ok(updatedFilesInfo)
    }

    @GetMapping("/api/v1/download/{fileId}")
    fun downloadFile(@PathVariable fileId: String): ResponseEntity<StreamingResponseBody> {
        return service.downloadFile(fileId)?.let { (headers, body) ->
            ResponseEntity.ok().headers(headers).body(body)
        } ?: ResponseEntity.notFound().build()
    }
}
