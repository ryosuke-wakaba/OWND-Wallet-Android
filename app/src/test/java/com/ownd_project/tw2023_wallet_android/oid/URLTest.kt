package com.ownd_project.tw2023_wallet_android.oid
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date

class URLTest {

    @Test
    fun testDecodeUriAsJsonWithVariousTypes() {
        val uri = "http://example.com?stringParam=hello&intParam=123&boolParam=true&jsonParam=%7B%22key%22%3A%20%22value%22%7D"
        val result = decodeUriAsJson(uri)

        assertEquals("hello", result["stringParam"])
        assertEquals(123, result["intParam"])
        assertEquals(true, result["boolParam"])
        assertTrue(result["jsonParam"] is Map<*, *>)
        assertEquals("value", (result["jsonParam"] as Map<*, *>)["key"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDecodeUriAsJsonWithEmptyUri() {
        decodeUriAsJson("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDecodeUriAsJsonWithInvalidUri() {
        decodeUriAsJson("http://example.com")
    }

    @Test
    fun testParse() {
        val url = "https://server.example.com/authorize?" +
                "response_type=code%20id_token" +
                "&client_id=s6BhdRkqt3" +
                "&redirect_uri=https%3A%2F%2Fclient.example.org%2Fcb" +
                "&scope=openid" +
                "&state=af0ifjsldkj" +
                "&nonce=n-0S6_WzA2Mj" +
                "&request=eyJhbGciO"

        val result = parse(url)

        assertEquals("https", result.first)
        assertNotNull(result.second)
        assertEquals("code id_token", result.second.responseType)
        assertEquals("s6BhdRkqt3", result.second.clientId)
        assertEquals("https://client.example.org/cb", result.second.redirectUri)
        assertEquals("openid", result.second.scope)
        assertEquals("af0ifjsldkj", result.second.state)
        assertEquals("n-0S6_WzA2Mj", result.second.nonce)
    }

    @Test()
    fun testDeseriarize() {
        val json = """
       {
          "authorization_endpoint": "https://example.com/authorize",
          "issuer": "https://example.com",
          "response_types_supported": ["id_token"],
          "scopes_supported": ["openid", "profile", "email"],
          "subject_types_supported": ["public", "pairwise"]
        }
        """
        val mapper = jacksonObjectMapper().apply {
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
//        mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(
//            JsonAutoDetect.Visibility.ANY))
        val metadata = mapper.readValue(json, AuthorizationServerMetadata::class.java)
        assertEquals("https://example.com/authorize", metadata.authorizationEndpoint)
        assertEquals("id_token", metadata.responseTypesSupported?.get(0) ?: "bad value")
    }

    class ParseAndResolveTest {
        private lateinit var wireMockServer: WireMockServer
        private lateinit var token: String

        val mockedResponse = """{
            "scopes_supported": ["openid"],
            "subject_types_supported": ["public"],
            "id_token_signing_alg_values_supported": ["RS256"],
            "request_object_signing_alg_values_supported": ["RS256"],
            "subject_syntax_types_supported": ["syntax1", "syntax2"],
            "request_object_encryption_alg_values_supported": ["ES256"],
            "request_object_encryption_enc_values_supported": ["ES256"],
            "client_id": "client123",
            "client_name": "ClientName",
            "logo_uri": "https://example.com/logo.png",
            "client_purpose": "authentication"
        }"""
        @Before
        fun setup() {
            wireMockServer = WireMockServer(8080)
            wireMockServer.start()
            WireMock.configureFor("localhost", 8080)

            val algorithm: Algorithm = Algorithm.HMAC512("secret")
            token = JWT.create()
                .withIssuer("auth0")
                .withClaim("name", "John Doe")
                .withClaim("role", "admin")
                .withExpiresAt(Date(System.currentTimeMillis() + 60 * 1000))
                .sign(algorithm)
        }

        @After
        fun teardown() {
            wireMockServer.stop()
        }
        @Test
        fun testParseAndResolveWithRequestAndClientMetadataUri() = runBlocking {
            val clientMetadataUriPath = "/client-metadata-uri"
            wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(clientMetadataUriPath))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withBody(mockedResponse)))
            val metadataUri = "http://localhost:${wireMockServer.port()}$clientMetadataUriPath"
            val encodedJwtString = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
            val testUri = "https://example.com/authorize?request=${encodedJwtString}&client_metadata_uri=$metadataUri"

            // parseAndResolve関数の呼び出し
            val result = parseAndResolve(testUri)

            // 結果の検証
            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertEquals(token, result.requestObjectJwt)
            assertNotNull(result.registrationMetadata)
            assertEquals("client123", result.registrationMetadata.clientId)
        }
        @Test
        fun testParseAndResolveWithRequestUriAndClientMetadata() = runBlocking {
            val requestUriPath = "/request-uri"
            wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(requestUriPath))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withBody(token)))

            val requestUri = "http://localhost:${wireMockServer.port()}$requestUriPath"
            val encodedRequestUri = URLEncoder.encode(requestUri, StandardCharsets.UTF_8.toString())

            val encodedMockedResponse = URLEncoder.encode(mockedResponse, StandardCharsets.UTF_8.toString())
            val testUri = "https://example.com/authorize?request_uri=$requestUri&client_metadata=$encodedMockedResponse"

            // parseAndResolve関数の呼び出し
            val result = parseAndResolve(testUri)

            // 結果の検証
            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertEquals(token, result.requestObjectJwt)
            assertNotNull(result.registrationMetadata)
            assertEquals("client123", result.registrationMetadata.clientId)
        }
    }
}
