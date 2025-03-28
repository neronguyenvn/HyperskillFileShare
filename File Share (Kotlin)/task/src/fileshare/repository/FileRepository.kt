package fileshare.repository

import fileshare.model.FileStorageInfo
import org.springframework.data.repository.CrudRepository

interface FileRepository : CrudRepository<FileStorageInfo, String>
