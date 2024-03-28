package com.ownd_project.tw2023_wallet_android

import com.ownd_project.tw2023_wallet_android.utils.Constants
import com.ownd_project.tw2023_wallet_android.utils.KeyPairUtil
import com.ownd_project.tw2023_wallet_android.utils.KeyPairUtil.verifyJwt
import com.ownd_project.tw2023_wallet_android.utils.KeyStoreHelper
import com.ownd_project.tw2023_wallet_android.utils.KeyStoreHelper.generateSecretKey
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import javax.crypto.SecretKey

class KeyPairUtilTest {
    @Test
    fun testGenerateKeyPair() {
        val keyPair = KeyPairUtil.generateSignVerifyKeyPair("test-alias")
        assertNotNull(keyPair)
        assertNotNull(keyPair.private)
        assertNotNull(keyPair.public)
    }

    @Test
    fun testIsKeyPairExist() {
        // キーペアを先に生成
        KeyPairUtil.generateSignVerifyKeyPair("test-alias")

        val isExist = KeyPairUtil.isKeyPairExist("test-alias")
        assertTrue(isExist)
    }

    @Test
    fun testGetPrivateKey() {
        // キーペアを先に生成
        KeyPairUtil.generateSignVerifyKeyPair("test-alias")

        val privateKey = KeyPairUtil.getPrivateKey("test-alias")
        assertNotNull(privateKey)
    }

    @Test
    fun testGetPublicKey() {
        // キーペアを先に生成
        KeyPairUtil.generateSignVerifyKeyPair("test-alias")

        val publicKey = KeyPairUtil.getPublicKey("test-alias")
        assertNotNull(publicKey)
    }

    @Test
    fun testConvertPublicKey() {
        // キーペアを先に生成
        KeyPairUtil.generateSignVerifyKeyPair("test-alias")
        val publicKey = KeyPairUtil.getPublicKey("test-alias")

        assertNotNull(publicKey)
    }


    @Test
    fun testSignJwt() {
        // キーペアを先に生成
        KeyPairUtil.generateSignVerifyKeyPair("test-alias")
        val jwt = KeyPairUtil.createProofJwt("test-alias", "aud", "nonce")
        val result = KeyPairUtil.decodeJwt(jwt)
        val header = result.first

        val jwk = header.get("jwk") as Map<String, String>

        assertTrue(verifyJwt(jwk, jwt))
    }
}

class KeyStoreHelperTest {

    private val keyAlias = "test-alias"

    // テスト前にKeyStoreをクリーンアップするためのメソッド
    @Before
    fun cleanup() {
        val keyStore = KeyStore.getInstance(Constants.KEYSTORE_TYPE).apply {
            load(null)
        }
        if (keyStore.containsAlias(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }
    }

    @Test
    fun testGenerateSecretKey() {
        val secretKey = generateSecretKey()
        assertNotNull(secretKey)
        assertTrue(secretKey is SecretKey)
    }

    @Test
    fun testGetSecretKey() {
        // 最初にキーが存在しないことを確認
        val keyStore = KeyStore.getInstance(Constants.KEYSTORE_TYPE).apply {
            load(null)
        }
        assertFalse(keyStore.containsAlias(keyAlias))

        // キーを取得（生成されるはず）
        val secretKey1 = KeyStoreHelper.getSecretKey()
        assertNotNull(secretKey1)

        // キーをもう一度取得（前回生成されたキーが返るはず）
        val secretKey2 = KeyStoreHelper.getSecretKey()
        assertNotNull(secretKey2)

        // 両方のキーが同じであることを確認
        assertEquals(secretKey1, secretKey2)
    }
}
