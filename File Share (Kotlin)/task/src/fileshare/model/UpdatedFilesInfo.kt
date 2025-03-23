package fileshare.model

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdatedFilesInfo(

    @JsonProperty("total_files")
    val totalFiles: Int,

    @JsonProperty("total_bytes")
    val totalBytes: Long,
)
