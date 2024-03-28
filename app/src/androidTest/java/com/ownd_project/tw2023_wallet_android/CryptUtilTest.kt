package com.ownd_project.tw2023_wallet_android

import com.ownd_project.tw2023_wallet_android.utils.EncryptionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class CryptUtilTest {
    @Test
    fun testEncryptionDecryption() {
        // テストデータの生成
        val originalData = Random.nextBytes(256)

        // 暗号化
        val (encryptedData, iv) = EncryptionHelper.encrypt(originalData)

        // 暗号化されたデータとオリジナルデータが異なることを確認
        assertNotEquals(originalData.toList(), encryptedData.toList())

        // 復号化
        val decryptedData = EncryptionHelper.decrypt(encryptedData, iv)

        // 復号化されたデータとオリジナルデータが同じであることを確認
        assertEquals(originalData.toList(), decryptedData.toList())
    }

    @Test
    fun testEncryptionDecryptionBase64() {
        // テストデータの生成
        val originalData = "test"

        // 暗号化
        val encryptedData = EncryptionHelper.encryptStringData(originalData)

        // 復号化
        val decryptedData = EncryptionHelper.decryptStringData(encryptedData)

        // 復号化されたデータとオリジナルデータが同じであることを確認
        assertEquals(originalData, decryptedData)
    }
}