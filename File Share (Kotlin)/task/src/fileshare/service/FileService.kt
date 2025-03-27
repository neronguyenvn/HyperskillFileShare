package fileshare.service

import fileshare.md5
import fileshare.model.UploadedFile
import fileshare.repository.FileRepository
import fileshare.usecase.FileValidator
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
    private val fileValidator: FileValidator,
    env: Environment,
) {
    class InvalidFileSize : Exception()
    class InvalidFileType : Exception()

    private val uploadDirPath = env.getRequiredProperty("uploads.dir")
    private val uploadDir = File(uploadDirPath).apply { mkdirs() }

    fun save(file: MultipartFile): UploadedFile {
        val originalFilename = StringUtils.cleanPath(file.originalFilename.orEmpty())
        val fileExtension = originalFilename.substringAfterLast('.')
        val fileMd5 = file.bytes.md5()

        repository.findAll().find { it.id == fileMd5 }?.let { foundFile ->
            val newFile = foundFile.copy(name = originalFilename)
            repository.save(newFile)
            return newFile
        }

        val remainingAvailableSpace = STORAGE_LIMIT - findFiles().sumOf(File::length)
        if (file.size > remainingAvailableSpace || file.size > FILE_LIMIT) {
            throw InvalidFileSize()
        }

        val isValidFiile = fileValidator.isValidFile(
            mediaType = file.contentType.orEmpty(),
            fileBytes = file.bytes
        )

        if (!isValidFiile) {
            throw InvalidFileType()
        }

        val savedFile = UploadedFile(
            id = file.bytes.md5(),
            name = originalFilename,
            extension = fileExtension,
            contentType = file.contentType.orEmpty(),
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

    companion object {
        private const val STORAGE_LIMIT = 200 * 1000
        private const val FILE_LIMIT = 50 * 1000
    }
}
