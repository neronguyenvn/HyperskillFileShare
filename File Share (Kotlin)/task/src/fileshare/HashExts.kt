@file:OptIn(ExperimentalStdlibApi::class)

package fileshare

import java.security.MessageDigest

fun ByteArray.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(this)
    return digest.toHexString()
}
