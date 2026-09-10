package com.mulaisekarang.app.util

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.crypto.CipherInputStream

@UnstableApi
class EncryptedFileDataSource(
    private val lessonId: Int
) : BaseDataSource(false) {

    private var fileInputStream: FileInputStream? = null
    private var cipherInputStream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0
    private var opened: Boolean = false

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val path = uri?.path ?: throw IllegalArgumentException("URI path is null")
        val file = File(path)
        
        transferInitializing(dataSpec)
        
        fileInputStream = FileInputStream(file)
        val skipped = fileInputStream!!.skip(dataSpec.position)
        if (skipped < dataSpec.position) {
            throw java.io.EOFException()
        }
        
        val cipher = CryptoUtil.getDecryptingCipher(lessonId, dataSpec.position)
        cipherInputStream = CipherInputStream(fileInputStream, cipher)
        
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            file.length() - dataSpec.position
        }
        
        opened = true
        transferStarted(dataSpec)
        
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (readLength == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            readLength
        } else {
            readLength.toLong().coerceAtMost(bytesRemaining).toInt()
        }

        val bytesRead = cipherInputStream?.read(buffer, offset, bytesToRead) ?: -1
        
        if (bytesRead == -1) {
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                throw java.io.EOFException()
            }
            return C.RESULT_END_OF_INPUT
        }

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }
        
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        try {
            cipherInputStream?.close()
        } catch (e: Exception) {
            // Ignore
        } finally {
            cipherInputStream = null
            try {
                fileInputStream?.close()
            } catch (e: Exception) {
                // Ignore
            } finally {
                fileInputStream = null
                if (opened) {
                    opened = false
                    transferEnded()
                }
            }
        }
    }
}
