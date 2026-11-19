package com.openfy.music.data.remote

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

actual fun decryptSaavnUrl(encrypted: String): String? {
    return try {
        val key = "38346591".toByteArray(Charsets.UTF_8)
        val keySpec = SecretKeySpec(key, "DES")
        val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        val decoded = Base64.getDecoder().decode(encrypted.trim())
        val decrypted = cipher.doFinal(decoded)
        String(decrypted, Charsets.UTF_8).trim()
    } catch (_: Throwable) {
        null
    }
}
