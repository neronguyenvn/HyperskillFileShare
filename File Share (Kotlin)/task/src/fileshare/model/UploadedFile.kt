package fileshare.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
data class UploadedFile(

    val name: String,

    val extension: String,

    val contentType: String,

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",
)
