package com.ownd_project.tw2023_wallet_android

import com.ownd_project.tw2023_wallet_android.utils.CredentialRequest
import com.ownd_project.tw2023_wallet_android.utils.Proof
import com.ownd_project.tw2023_wallet_android.utils.VCIClient
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.ownd_project.tw2023_wallet_android.utils.CredentialRequestJwtVc
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test


class VCIClientTest {
    private lateinit var wireMockServer: WireMockServer
    private lateinit var vciClient: VCIClient
    private var port: Int = 0

    @Before
    fun setup() {
        wireMockServer = WireMockServer()  // ポートを指定する場合は、WireMockServer(8080) のように
        wireMockServer.start()
        port = wireMockServer.port()

        // テスト用のJSONレスポンスを読み込む
        val tokenResponseJson =
            this::class.java.classLoader?.getResource("token_response.json")?.readText()
                ?: throw IllegalArgumentException("Cannot read token_response.json")

        // スタブの設定
        wireMockServer.stubFor(
            post(urlEqualTo("/token")).willReturn(
                aResponse().withHeader("Content-Type", "application/json").withStatus(200)
                    .withBody(tokenResponseJson)
            )
        )
        val credentialResponseJson =
            this::class.java.classLoader?.getResource("credential_response.json")?.readText()
                ?: throw IllegalArgumentException("Cannot read credential_response.json")
        wireMockServer.stubFor(
            post(urlEqualTo("/credentials")).willReturn(
                aResponse().withHeader("Content-Type", "application/json").withStatus(200)
                    .withBody(credentialResponseJson)
            )
        )
        vciClient = VCIClient()  // VCIClientのコンストラクタにURLを指定
    }

    @After
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    fun testPostTokenRequest() {
        val tokenResponse =
            vciClient.postTokenRequest("http://localhost:${port}/token", "preAuthCode")
        assertEquals("example-access-token", tokenResponse?.accessToken)
        assertEquals("bearer", tokenResponse?.tokenType)
        assertEquals(86400, tokenResponse?.expiresIn)
        assertEquals("example-c-nonce", tokenResponse?.cNonce)
        assertEquals(86400, tokenResponse?.cNonceExpiresIn)
    }

    @Test
    fun testPostCredentialRequest() {
        val credential = CredentialRequestJwtVc(
            format = "jwt_vc_json", credentialDefinition = mapOf("type" to "IdentityCredential"), proof = Proof(
                proofType = "jwt", jwt = "eyJraWQiOiJkaW...（中略）...1nOzM"
            )
        )
        val credentialResponse = vciClient.postCredentialRequest(
            "http://localhost:${port}/credentials", credential, "accessToken"
        )
        assertEquals("jwt_vc_json", credentialResponse?.format)
        assertEquals("example-credential", credentialResponse?.credential)
        assertEquals("example-c-nonce", credentialResponse?.cNonce)
        assertEquals(86400, credentialResponse?.cNonceExpiresIn)
    }
}
