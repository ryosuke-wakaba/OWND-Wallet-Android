package com.ownd_project.tw2023_wallet_android

//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.BeforeEach
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import com.ownd_project.tw2023_wallet_android.vci.MetadataClient
import com.ownd_project.tw2023_wallet_android.vci.openIdFetch
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FetchClientTest {

    private lateinit var wireMockServer: WireMockServer

    private var port: Int = 0
    private val endPointCredentialIssuerMetadata = "/.well-known/openid-credential-issuer"
    private val endPointAuthServer = "/.well-known/oauth-authorization-server"

    @Before
    fun setup() {
        wireMockServer = WireMockServer()
        wireMockServer.start()
        port = wireMockServer.port()

        var credentialIssuerMetadataJson =
            this::class.java.classLoader?.getResource("credential_issuer_metadata_jwt_vc.json")
                ?.readText()
                ?: throw IllegalArgumentException("Cannot read test_data.json")
        credentialIssuerMetadataJson = credentialIssuerMetadataJson.replace(
            "https://datasign-demo-vci.tunnelto.dev",
            "http://localhost:${port}"
        )
        println("FetchClientTest--------------credentialIssuerMetadataJson = $credentialIssuerMetadataJson")

        var authorizationServerJson =
            this::class.java.classLoader?.getResource("authorization_server.json")?.readText()
                ?: throw IllegalArgumentException("Cannot read test_data.json")
        authorizationServerJson =
            authorizationServerJson.replace("datasign-demo-vci.tunnelto.dev", "localhost:${port}")

        println("FetchClientTest--------------authorizationServerJson = $authorizationServerJson")

        // スタブの設定
        wireMockServer.stubFor(
            get(urlEqualTo(endPointCredentialIssuerMetadata)).willReturn(
                aResponse().withHeader("Content-Type", "application/json").withStatus(200)
                    .withBody(credentialIssuerMetadataJson)
            )
        )
        wireMockServer.stubFor(
            get(urlEqualTo(endPointAuthServer)).willReturn(
                aResponse().withHeader("Content-Type", "application/json").withStatus(200)
                    .withBody(authorizationServerJson)
            )
        )
    }

    @After
    fun teardown() {
        wireMockServer.stop()
    }

    // fun `should return correct response when calling your endpoint`() = runBlockingTest {
    @Test
    fun `should return correct response when calling your endpoint`() = runBlocking {
        val port = wireMockServer.port()
        val host = "http://localhost:${port}"
        // YourApiClientをセットアップ（APIのエンドポイントをWireMockがリッスンしているアドレスに設定）
        val url = "${host}${endPointCredentialIssuerMetadata}"
        // API呼び出し
        val response = openIdFetch(url, CredentialIssuerMetadata::class.java)

        // 期待されるレスポンスと実際のレスポンスを比較
        val body = response.responseBody
        assertEquals("http://localhost:${port}", body?.credentialIssuer)
        assertEquals("http://localhost:${port}", body?.authorizationServers?.get(0)) // 修正
        assertEquals("http://localhost:${port}/credentials", body?.credentialEndpoint)
        assertEquals("http://localhost:${port}/batch-credentials", body?.batchCredentialEndpoint)
    }

    @Test
    fun `should return correct response when calling your endpoint from meta data client`() =
        runBlocking {
            val port = wireMockServer.port()
            val host = "http://localhost:${port}"
            // val host = "http://localhost:${wireMockServer.port()}"
            // YourApiClientをセットアップ（APIのエンドポイントをWireMockがリッスンしているアドレスに設定）
//        val url = "${host}${endPointCredentialIssuerMetadata}"
            // API呼び出し
            val response = MetadataClient.retrieveAllMetadata(host)
//        val response = openIdFetch(url, CredentialIssuerMetadata::class.java)

            // 期待されるレスポンスと実際のレスポンスを比較
            val body = response.credentialIssuerMetadata
            assertEquals("http://localhost:${port}", body?.credentialIssuer)
            assertEquals("http://localhost:${port}", body?.authorizationServers?.get(0)) // 修正
            assertEquals("http://localhost:${port}/credentials", body?.credentialEndpoint)
            assertEquals(
                "http://localhost:${port}/batch-credentials",
                body?.batchCredentialEndpoint
            )
        }
}
