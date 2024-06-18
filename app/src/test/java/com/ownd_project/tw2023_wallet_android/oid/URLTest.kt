package com.ownd_project.tw2023_wallet_android.oid
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date

open class URLBaseTest {
    lateinit var wireMockServer: WireMockServer
    lateinit var token: String
    lateinit var clientMetadataMap: Map<String, Any>
    lateinit var presentationDefinitionMap: Map<String, Any>
    val algorithm: Algorithm = Algorithm.HMAC512("secret")

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

        val mapper = jacksonObjectMapper()
        clientMetadataMap = mapper.readValue<Map<String, Any>>(mockedResponse)
        presentationDefinitionMap = mapper.readValue<Map<String, Any>>(presentationDefinitionJson)
    }

    @After
    fun teardown() {
        wireMockServer.stop()
    }

    fun prepareClientMetadataUri(response: String): String {
        val clientMetadataUriPath = "/client-metadata-uri"
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(clientMetadataUriPath))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody(response)))
        val uri = "http://localhost:${wireMockServer.port()}$clientMetadataUriPath"
        return uri
    }

    fun preparePresentationDefinitionUri(response: String): String {
        val presentationDefinitionUriPath = "/presentation-definition-uri"
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(presentationDefinitionUriPath))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody(response)))
        val uri = "http://localhost:${wireMockServer.port()}$presentationDefinitionUriPath"
        return uri
    }
}

@RunWith(Enclosed::class)
class URLTest {

    class Misc {
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
    }

    class ParseAndResolveSignedRequestTest: URLBaseTest() {
        @Test
        fun testClientMetadata() = runBlocking {
            val requestObjectJwt = JWT.create()
                .withClaim("client_metadata", clientMetadataMap)
                .withExpiresAt(Date(System.currentTimeMillis() + 60 * 1000))
                .sign(algorithm)
            val encodedJwtString = URLEncoder.encode(requestObjectJwt, StandardCharsets.UTF_8.toString())
            val testUri = "https://example.com/authorize?request=${encodedJwtString}"

            val result = parseAndResolve(testUri)

            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertEquals(requestObjectJwt, result.requestObjectJwt)
            assertNotNull(result.registrationMetadata)
            assertEquals("client123", result.registrationMetadata.clientId)
        }


        @Test
        fun testClientMetadataUri() = runBlocking {
            val uri = prepareClientMetadataUri(mockedResponse)

            val requestObjectJwt = JWT.create()
                .withClaim("client_metadata_uri", uri)
                .withExpiresAt(Date(System.currentTimeMillis() + 60 * 1000))
                .sign(algorithm)
            val encodedJwtString = URLEncoder.encode(requestObjectJwt, StandardCharsets.UTF_8.toString())
            val testUri = "https://example.com/authorize?request=${encodedJwtString}"

            val result = parseAndResolve(testUri)

            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertEquals(requestObjectJwt, result.requestObjectJwt)
            assertNotNull(result.registrationMetadata)
            assertEquals("client123", result.registrationMetadata.clientId)
        }

        @Test
        fun testPresentationDefinition() = runBlocking {
            val requestObjectJwt = JWT.create()
                .withClaim("presentation_definition", presentationDefinitionMap)
                .withExpiresAt(Date(System.currentTimeMillis() + 60 * 1000))
                .sign(algorithm)
            val encodedJwtString = URLEncoder.encode(requestObjectJwt, StandardCharsets.UTF_8.toString())
            val testUri = "https://example.com/authorize?client_id=client123&request=${encodedJwtString}"

            val result = parseAndResolve(testUri)

            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertNotNull(result.presentationDefinition)
            assertEquals("client123", result.authorizationRequestPayload.clientId)
        }

        @Test
        fun testPresentationDefinitionUri() = runBlocking {
            val uri = preparePresentationDefinitionUri(presentationDefinitionJson)
            val requestObjectJwt = JWT.create()
                .withClaim("presentation_definition_uri", uri)
                .withExpiresAt(Date(System.currentTimeMillis() + 60 * 1000))
                .sign(algorithm)
            val encodedJwtString = URLEncoder.encode(requestObjectJwt, StandardCharsets.UTF_8.toString())
            val testUri = "https://example.com/authorize?client_id=client123&request=${encodedJwtString}"

            val result = parseAndResolve(testUri)

            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertNotNull(result.presentationDefinition)
            assertEquals("client123", result.authorizationRequestPayload.clientId)
        }
    }

    class ParseAndResolveSignedRequestByUrlTest: URLBaseTest() {
        private fun prepareRequestUri(jwt: String): String? {
            val requestUriPath = "/request-uri"
            wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(requestUriPath))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withBody(jwt)))

            val requestUri = "http://localhost:${wireMockServer.port()}$requestUriPath"
            return URLEncoder.encode(requestUri, StandardCharsets.UTF_8.toString())
        }

        @Test
        fun testClientMetadata() = runBlocking {
            val requestObjectJwt = JWT.create()
                .withClaim("client_metadata", clientMetadataMap)
                .withExpiresAt(Date(System.currentTimeMillis() + 60 * 1000))
                .sign(algorithm)
            val encodedJwtString = URLEncoder.encode(requestObjectJwt, StandardCharsets.UTF_8.toString())
            val encodedRequestUri = prepareRequestUri(encodedJwtString)

            val testUri = "https://example.com/authorize?request_uri=$encodedRequestUri"

            // parseAndResolve関数の呼び出し
            val result = parseAndResolve(testUri)

            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertEquals(encodedJwtString, result.requestObjectJwt)
            assertNotNull(result.registrationMetadata)
            assertEquals("client123", result.registrationMetadata.clientId)
        }

        @Test
        fun testClientMetadataUri() = runBlocking {
            val uri = prepareClientMetadataUri(mockedResponse)

            val requestObjectJwt = JWT.create()
                .withClaim("client_metadata_uri", uri)
                .withExpiresAt(Date(System.currentTimeMillis() + 60 * 1000))
                .sign(algorithm)
            val encodedJwtString = URLEncoder.encode(requestObjectJwt, StandardCharsets.UTF_8.toString())
            val encodedRequestUri = prepareRequestUri(encodedJwtString)

            val testUri = "https://example.com/authorize?request_uri=$encodedRequestUri"

            // parseAndResolve関数の呼び出し
            val result = parseAndResolve(testUri)

            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertEquals(encodedJwtString, result.requestObjectJwt)
            assertNotNull(result.registrationMetadata)
            assertEquals("client123", result.registrationMetadata.clientId)
        }

        @Test
        fun testPresentationDefinition() = runBlocking {
            val requestObjectJwt = JWT.create()
                .withClaim("presentation_definition", presentationDefinitionMap)
                .withExpiresAt(Date(System.currentTimeMillis() + 60 * 1000))
                .sign(algorithm)
            val encodedJwtString = URLEncoder.encode(requestObjectJwt, StandardCharsets.UTF_8.toString())
            val encodedRequestUri = prepareRequestUri(encodedJwtString)

            val testUri = "https://example.com/authorize?client_id=client123&request_uri=$encodedRequestUri"

            val result = parseAndResolve(testUri)

            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertNotNull(result.presentationDefinition)
            assertEquals("client123", result.authorizationRequestPayload.clientId)
        }

        @Test
        fun testPresentationDefinitionUri() = runBlocking {
            val uri = preparePresentationDefinitionUri(presentationDefinitionJson)
            val requestObjectJwt = JWT.create()
                .withClaim("presentation_definition_uri", uri)
                .withExpiresAt(Date(System.currentTimeMillis() + 60 * 1000))
                .sign(algorithm)
            val encodedJwtString = URLEncoder.encode(requestObjectJwt, StandardCharsets.UTF_8.toString())

            val requestUriPath = "/request-uri"
            wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(requestUriPath))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withBody(encodedJwtString)))

            val requestUri = "http://localhost:${wireMockServer.port()}$requestUriPath"
            val encodedRequestUri = URLEncoder.encode(requestUri, StandardCharsets.UTF_8.toString())

            val testUri = "https://example.com/authorize?client_id=client123&request_uri=$encodedRequestUri"

            val result = parseAndResolve(testUri)

            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertNotNull(result.presentationDefinition)
            assertEquals("client123", result.authorizationRequestPayload.clientId)
        }
    }

    class ParseAndResolveUnSignedRequestTest: URLBaseTest() {
        @Test
        fun testClientMetadata() = runBlocking {
            val encodedMetadata = URLEncoder.encode(mockedResponse, StandardCharsets.UTF_8.toString())
            val testUri = "https://example.com/authorize?client_metadata=${encodedMetadata}"

            val result = parseAndResolve(testUri)

            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertNotNull(result.registrationMetadata)
            assertEquals("client123", result.registrationMetadata.clientId)
        }

        @Test
        fun testClientMetadataUri() = runBlocking {
            val uri = prepareClientMetadataUri(mockedResponse)
            val testUri = "https://example.com/authorize?client_metadata_uri=${uri}"

            val result = parseAndResolve(testUri)

            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertNotNull(result.registrationMetadata)
            assertEquals("client123", result.registrationMetadata.clientId)
        }

        @Test
        fun testPresentationDefinition() = runBlocking {
            val encodedPresentationDefinitionJson = URLEncoder.encode(presentationDefinitionJson, StandardCharsets.UTF_8.toString())
            val testUri = "https://example.com/authorize?client_id=client123&presentation_definition=${encodedPresentationDefinitionJson}"

            val result = parseAndResolve(testUri)

            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertNotNull(result.presentationDefinition)
            assertEquals("client123", result.authorizationRequestPayload.clientId)
        }

        @Test
        fun testPresentationDefinitionUri() = runBlocking {
            val uri = preparePresentationDefinitionUri(presentationDefinitionJson)
            val testUri = "https://example.com/authorize?client_id=client123&presentation_definition_uri=${uri}"

            val result = parseAndResolve(testUri)

            assertNotNull(result)
            assertEquals("https", result.scheme)
            assertNotNull(result.authorizationRequestPayload)
            assertNotNull(result.registrationMetadata)
            assertEquals("client123", result.authorizationRequestPayload.clientId)
        }
    }
}
