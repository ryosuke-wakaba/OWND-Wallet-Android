package com.ownd_project.tw2023_wallet_android.utils

import android.util.Base64
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

object EncryptionHelper {

    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"


    fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = KeyStoreHelper.getSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return Pair(encrypted, iv)
    }

    fun decrypt(data: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = KeyStoreHelper.getSecretKey()
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    fun encryptStringData(data: String): String {
        val (encryptedData, iv) = encrypt(data.toByteArray(Charsets.UTF_8))
        val ivSize = iv.size
        val ivSizeBytes = ByteBuffer.allocate(4).putInt(ivSize).array() // IVサイズをバイト配列に変換
        val combinedArray = ivSizeBytes + iv + encryptedData // IVサイズ、IV、暗号化データを結合
        return Base64.encodeToString(combinedArray, Base64.DEFAULT)
    }

    fun decryptStringData(data: String): String {
        val combinedArray = Base64.decode(data, Base64.DEFAULT)
        val ivSize = ByteBuffer.wrap(combinedArray, 0, 4).int
        val iv = combinedArray.copyOfRange(4, 4 + ivSize)
        val encryptedData = combinedArray.copyOfRange(4 + ivSize, combinedArray.size)
        val decryptedData = decrypt(encryptedData, iv)
        return String(decryptedData, Charsets.UTF_8)
    }
}
