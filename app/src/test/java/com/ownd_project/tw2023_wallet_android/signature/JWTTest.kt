package com.ownd_project.tw2023_wallet_android.signature

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.ownd_project.tw2023_wallet_android.encodeECPublicKeyToJwks
import com.ownd_project.tw2023_wallet_android.encodePublicKeyToJwks
import com.ownd_project.tw2023_wallet_android.utils.generateEcKeyPair
import com.ownd_project.tw2023_wallet_android.utils.generateRsaKeyPair
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey


fun createValidJwt(privateKey: PrivateKey, kid: String): String {
    val algorithm = when (privateKey) {
        is RSAPrivateKey -> Algorithm.RSA256(null, privateKey)
        is ECPrivateKey -> Algorithm.ECDSA256(null, privateKey)
        else -> throw IllegalArgumentException("未サポートの秘密鍵のタイプです。")
    }
    // val algorithm = Algorithm.RSA256(null, privateKey as RSAPrivateKey)
    return com.auth0.jwt.JWT.create().withIssuer("iss.example.com").withClaim("username", "user123")
        .withKeyId(kid).sign(algorithm)
}

class JWTTest {
    private val keyPair = generateRsaKeyPair()
    private val ecKeyPair = generateEcKeyPair()
    private lateinit var wireMockServer: WireMockServer

    @Before
    fun setup() {
        wireMockServer = WireMockServer().apply {
            start()
            WireMock.configureFor("localhost", port())
        }

        val publicKey = keyPair.public as RSAPublicKey
        val jwksResponse = encodePublicKeyToJwks(publicKey, "test-kid")

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/.well-known/jwks.json")).willReturn(
                WireMock.aResponse().withStatus(200).withBody(jwksResponse)
            )
        )

        val ecPublicKey = ecKeyPair.public as ECPublicKey
        val jwksResponse2 = encodeECPublicKeyToJwks(ecPublicKey, "test-kid")

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/.well-known/ec-jwks.json")).willReturn(
                WireMock.aResponse().withStatus(200).withBody(jwksResponse2)
            )
        )
    }

    @After
    fun teardown() {
        wireMockServer.stop()
    }

    @Test
    fun testValidateJwtWithValidToken() = runBlocking {
        val validJwt = createValidJwt(keyPair.private, "test-kid")
        val jwksUrl = "http://localhost:${wireMockServer.port()}/.well-known/jwks.json"

        val result = JWT.verifyJwtWithJwks(validJwt, jwksUrl)

        TestCase.assertNotNull(result)
        TestCase.assertEquals("iss.example.com", result.issuer)
        TestCase.assertEquals("user123", result.getClaim("username").asString())
    }

    @Test
    fun testValidateJwtWithValidTokenUsingEcKey() = runBlocking {
        val validJwt = createValidJwt(ecKeyPair.private, "test-kid")
        val jwksUrl = "http://localhost:${wireMockServer.port()}/.well-known/ec-jwks.json"

        val result = JWT.verifyJwtWithJwks(validJwt, jwksUrl)

        TestCase.assertNotNull(result)
        TestCase.assertEquals("iss.example.com", result.issuer)
        TestCase.assertEquals("user123", result.getClaim("username").asString())
    }

    @Test
    fun testValidateJwtWithInvalidToken() = runBlocking {
        val wrongKeyPair = generateRsaKeyPair()
        val invalidJwt = createValidJwt(wrongKeyPair.private, "test-kid")
        val jwksUrl = "http://localhost:${wireMockServer.port()}/.well-known/jwks.json"

        try {
            JWT.verifyJwtWithJwks(invalidJwt, jwksUrl)
            TestCase.fail("JWTVerificationExceptionがスローされるべきです。")
        } catch (e: JWTVerificationException) {
            // 期待される例外。テスト成功
        }
    }
}