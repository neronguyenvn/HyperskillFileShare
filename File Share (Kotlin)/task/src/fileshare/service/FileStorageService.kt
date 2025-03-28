package fileshare.service

import fileshare.model.UpdatedFilesInfo
import fileshare.model.UploadedFile
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

@Service
class FileStorageService {

    @Value("\${uploads.dir}")
    private lateinit var uploadDirPath: String

    private val uploadDir by lazy {
        File(uploadDirPath).also {
            Files.createDirectories(Path(uploadDirPath))
        }
    }

    fun storeFile(multipartFile: MultipartFile, uploadedFile: UploadedFile) {
        multipartFile.transferTo(getStoredFilePath(uploadedFile))
    }

    fun getStoredFilesInfo(): UpdatedFilesInfo {
        val storedFiles = uploadDir.walk().maxDepth(1).filter(File::isFile)
        return UpdatedFilesInfo(
            totalFiles = storedFiles.count(),
            totalBytes = storedFiles.sumOf { it.length() }
        )
    }

    fun fileExists(file: UploadedFile): Boolean {
        return getStoredFilePath(file).exists()
    }

    fun getStoredFilePath(file: UploadedFile): Path {
        return Path.of(uploadDirPath, "${file.id}d.${file.extension}")
    }

    fun getUsedStorage(): Long {
        return uploadDir.walk().sumOf { it.length() }
    }
}
