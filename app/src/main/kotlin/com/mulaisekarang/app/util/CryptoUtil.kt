package com.mulaisekarang.app.util

import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtil {
    // Kunci rahasia 32-byte (256-bit). Idealnya disimpan di Android Keystore.
    private val SECRET_KEY = "MulaiSekarangOfflineSecretKey123".toByteArray() 
    
    // AES/CTR/NoPadding adalah stream cipher, memungkinkan pencarian posisi (seek) yang sangat cepat
    private const val TRANSFORMATION = "AES/CTR/NoPadding"

    fun getEncryptingOutputStream(outputStream: OutputStream, lessonId: Int, offset: Long = 0L): OutputStream {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val blockSize = cipher.blockSize
        val blockOffset = offset / blockSize
        
        val ivBytes = generateIv(lessonId).iv
        var carry = blockOffset
        for (i in ivBytes.indices.reversed()) {
            if (carry == 0L) break
            val sum = (ivBytes[i].toLong() and 0xFF) + carry
            ivBytes[i] = sum.toByte()
            carry = sum ushr 8
        }
        
        val keySpec = SecretKeySpec(SECRET_KEY, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(ivBytes))
        
        val cos = CipherOutputStream(outputStream, cipher)
        
        // Advance cipher state by the remaining bytes in the block
        val bytesToSkip = (offset % blockSize).toInt()
        if (bytesToSkip > 0) {
            val dummy = ByteArray(bytesToSkip)
            cipher.update(dummy)
        }
        
        return cos
    }

    fun getDecryptingCipher(lessonId: Int, offset: Long): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        val blockSize = cipher.blockSize
        val blockOffset = offset / blockSize
        
        val ivBytes = generateIv(lessonId).iv
        
        // Tambahkan blockOffset ke counter IV 128-bit
        var carry = blockOffset
        for (i in ivBytes.indices.reversed()) {
            if (carry == 0L) break
            val sum = (ivBytes[i].toLong() and 0xFF) + carry
            ivBytes[i] = sum.toByte()
            carry = sum ushr 8
        }
        
        val keySpec = SecretKeySpec(SECRET_KEY, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(ivBytes))
        return cipher
    }

    private fun generateIv(lessonId: Int): IvParameterSpec {
        val iv = ByteArray(16)
        iv[0] = (lessonId shr 24).toByte()
        iv[1] = (lessonId shr 16).toByte()
        iv[2] = (lessonId shr 8).toByte()
        iv[3] = lessonId.toByte()
        return IvParameterSpec(iv)
    }
}
