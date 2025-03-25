package fileshare.repository

import fileshare.model.UploadedFile
import org.springframework.data.repository.CrudRepository

interface FileRepository : CrudRepository<UploadedFile, String>
