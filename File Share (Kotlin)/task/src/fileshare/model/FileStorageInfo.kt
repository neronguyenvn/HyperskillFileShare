package fileshare.model

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class FileStorageInfo(

    @Id
    val id: String,

    val name: String,

    val extension: String,

    val contentType: String,

    val downloadUri: String
)
