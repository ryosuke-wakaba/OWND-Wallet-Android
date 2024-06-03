package com.ownd_project.tw2023_wallet_android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.oid.HeaderOptions
import com.ownd_project.tw2023_wallet_android.oid.JwtVpJsonPayloadOptions
import com.ownd_project.tw2023_wallet_android.ui.shared.JwtVpJsonGeneratorImpl
import com.ownd_project.tw2023_wallet_android.utils.KeyPairUtil
import junit.framework.TestCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JwtVpJsonGeneratorTest {

    private lateinit var jwtVpJsonGenerator: JwtVpJsonGeneratorImpl
    private val keyAlias = "testKeyAlias"

    @Before
    fun setUp() {
        jwtVpJsonGenerator = JwtVpJsonGeneratorImpl(keyAlias)
        if (!KeyPairUtil.isKeyPairExist(keyAlias)) {
            KeyPairUtil.generateSignVerifyKeyPair(keyAlias)
        }
    }

    @Test
    fun testGenerateJwt() {
        val vcJwt = "testVcJwt"
        val headerOptions = HeaderOptions()
        val payloadOptions = JwtVpJsonPayloadOptions(
            iss = "issuer",
            jti = "testJti",
            aud = "testAud",
            nonce = "testNonce"
        )

        val vpToken = jwtVpJsonGenerator.generateJwt(vcJwt, headerOptions, payloadOptions)

        // 生成されたJWTを検証するためのロジックを追加してください。
        // ここでは例として、JWTが非空であることを確認しています。
        assert(!vpToken.isEmpty())

        val decodedJwt = KeyPairUtil.decodeJwt(vpToken)
        val header = decodedJwt.first

        val jwk = header.get("jwk") as Map<String, String>
        val result = KeyPairUtil.verifyJwt(jwk, vpToken)
        TestCase.assertTrue(result)
    }
}
