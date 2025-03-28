package fileshare.controller

import fileshare.model.UpdatedFilesInfo
import fileshare.service.FileService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.net.URI

@RestController
@RequestMapping("\${api.base-path}")
class FileController(private val service: FileService) {

    @PostMapping("/upload")
    fun uploadFile(@RequestParam file: MultipartFile): ResponseEntity<Unit> {
        val savedFile = service.save(file)
        return ResponseEntity.created(URI.create(savedFile.downloadUri)).build()
    }

    @GetMapping("/info")
    fun getUpdatedFilesInfo(): ResponseEntity<UpdatedFilesInfo> =
        ResponseEntity.ok(service.getFilesInfo())

    @GetMapping("/download/{fileId}")
    fun downloadFile(@PathVariable fileId: String): ResponseEntity<StreamingResponseBody> =
        service.downloadFile(fileId)?.let { (headers, body) ->
            ResponseEntity.ok().headers(headers).body(body)
        } ?: ResponseEntity.notFound().build()
}
