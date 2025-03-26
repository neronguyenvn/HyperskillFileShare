package fileshare.usecase

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

@Component
class FileValidator {

    fun isValidFile(mediaType: String, fileBytes: ByteArray): ResponseEntity<Unit>? {
        if (mediaType !in allowedMediaTypes) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build()
        }

        val isValid = when (mediaType) {
            "image/png" -> checkPngSignature(fileBytes)
            "image/jpeg" -> checkJpegSignature(fileBytes)
            "text/plain" -> isValidUtf8(fileBytes)
            else -> return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build()
        }

        return if (isValid) null
        else ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build()
    }

    private fun checkPngSignature(bytes: ByteArray): Boolean {
        val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        return bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(pngSignature)
    }

    private fun checkJpegSignature(bytes: ByteArray): Boolean {
        val jpegStart = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        val jpegEnd = byteArrayOf(0xFF.toByte(), 0xD9.toByte())

        return bytes.size >= 2 &&
                bytes.copyOfRange(0, 2).contentEquals(jpegStart) &&
                bytes.copyOfRange(bytes.size - 2, bytes.size).contentEquals(jpegEnd)
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean {
        return try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
            decoder.decode(ByteBuffer.wrap(bytes))
            true  // No exception = valid UTF-8
        } catch (e: Exception) {
            false  // Decoding error = invalid UTF-8
        }
    }

    companion object {
        private val allowedMediaTypes = setOf("text/plain", "image/jpeg", "image/png")
    }
}
