package fileshare.service

import fileshare.model.UploadedFile
import fileshare.repository.FileRepository
import org.springframework.core.env.Environment
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists

@Service
class FileService(
    private val repository: FileRepository,
    env: Environment,
) {
    private val uploadDirPath = env.getRequiredProperty("uploads.dir")
    private val uploadDir = File(uploadDirPath).apply { mkdirs() }

    fun save(file: MultipartFile): UploadedFile {
        val originalFilename = StringUtils.cleanPath(file.originalFilename.orEmpty())
        val fileExtension = originalFilename.substringAfterLast('.')

        val savedFile = UploadedFile(
            name = originalFilename,
            extension = fileExtension,
            contentType = file.contentType.orEmpty()
        ).run {
            repository.save(this)
        }

        val path = Path.of(
            uploadDirPath,
            "${savedFile.id}.${savedFile.extension}"
        )
        file.transferTo(path)

        return savedFile
    }

    fun findFiles(): List<File> {
        return uploadDir.listFiles()?.filter { it.isFile }.orEmpty()
    }

    fun findFileById(id: String): UploadedFile? {
        return repository.findByIdOrNull(id)?.let { file ->
            val filePath = Path.of(uploadDirPath, "${file.id}.${file.extension}")
            if (filePath.exists()) file else null
        }
    }
}
