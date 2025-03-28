package fileshare.usecase

import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

@Component
class FileValidator {

    fun validate(mediaType: String, fileBytes: ByteArray): Boolean {
        if (mediaType !in allowedMediaTypes) {
            return false
        }

        return when (mediaType) {
            "image/png" -> checkPngSignature(fileBytes)
            "image/jpeg" -> checkJpegSignature(fileBytes)
            "text/plain" -> isValidUtf8(fileBytes)
            else -> false
        }
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
