package com.ownd_project.tw2023_wallet_android.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object ZipUtil {
    fun compressString(input: String): String {
        val bos = ByteArrayOutputStream(input.length)
        GZIPOutputStream(bos).bufferedWriter(Charsets.UTF_8).use { it.write(input) }
        return Base64.getEncoder().encodeToString(bos.toByteArray())
    }

    fun decompressString(compressed: String): String {
        val bytes = Base64.getDecoder().decode(compressed)
        ByteArrayInputStream(bytes).use { bis ->
            GZIPInputStream(bis).bufferedReader(Charsets.UTF_8).use { reader ->
                return reader.readText()
            }
        }
    }
}
