package fileshare.controller

import fileshare.model.UpdatedFilesInfo
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Path
import kotlin.io.path.fileSize

@RestController
class UploadRestController(env: Environment) {

    private val uploadDirPath = env.getRequiredProperty("uploads.dir")
    private val uploadDir = File(uploadDirPath).apply { mkdirs() }

    @PostMapping("/api/v1/upload")
    fun uploadFile(@RequestParam file: MultipartFile): ResponseEntity<Unit> {
        val path = Path.of(uploadDirPath, file.originalFilename)
        file.transferTo(path)
        return ResponseEntity.status(HttpStatus.CREATED).build()
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
}
