package com.ownd_project.tw2023_wallet_android.oid

import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ownd_project.tw2023_wallet_android.encodePublicKeyToJwks
import com.ownd_project.tw2023_wallet_android.pairwise.HDKeyRing
import com.ownd_project.tw2023_wallet_android.signature.SignatureUtil
import com.ownd_project.tw2023_wallet_android.signature.ECPrivateJwk
import com.ownd_project.tw2023_wallet_android.utils.generateRsaKeyPair
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.ownd_project.tw2023_wallet_android.utils.generateEcKeyPair
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date

const val clientHost = "http://localhost"
fun createRequestObjectJwt(privateKey: PrivateKey, kid: String, clientId: String, clientMetadataUri: String): String {
    val algorithm = Algorithm.RSA256(null, privateKey as RSAPrivateKey)

    return JWT.create().withIssuer("https://client.example.org/cb")
        .withAudience("https://server.example.com").withClaim("response_type", "code id_token")
        .withClaim("client_id", clientId)
        .withClaim("redirect_uri", clientId).withClaim("scope", "openid")
        .withClaim("client_metadata_uri", clientMetadataUri).withClaim("scope", "openid")
        .withClaim("state", "af0ifjsldkj").withClaim("nonce", "n-0S6_WzA2Mj")
        .withClaim("max_age", 86400).withIssuedAt(Date()).withKeyId(kid).sign(algorithm)
}

@RunWith(Enclosed::class)
class OpenIdProviderTest {
    class ResponseTest {
        private val keyPair = generateRsaKeyPair()
        private lateinit var wireMockServer: WireMockServer

        @Before
        fun setup() {
            wireMockServer = WireMockServer().apply {
                start()
                WireMock.configureFor("localhost", port())
            }

            val jwksUrl = "$clientHost:${wireMockServer.port()}/.well-known/jwks.json"
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
            "jwks_uri": "$jwksUrl",
            "logo_uri": "https://example.com/logo.png",
            "client_purpose": "authentication",
            "vp_formats": {
                "vc+sd-jwt": {
                  "sd-jwt_alg_values": [
                    "ES256",
                    "ES256K",
                    "ES384",
                    "ES512",
                    "EdDSA"
                  ],
                  "kb-jwt_alg_values": [
                    "ES256",
                    "ES256K",
                    "ES384",
                    "ES512",
                    "EdDSA"
                  ]
                }
            }
        }"""

            wireMockServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/client-metadata-uri")).willReturn(
                    WireMock.aResponse().withStatus(200).withBody(mockedResponse)
                )
            )
            wireMockServer.stubFor(
                WireMock.post(WireMock.urlEqualTo("/cb"))
                    .withHeader("Content-Type", WireMock.equalTo("application/x-www-form-urlencoded"))
                    .willReturn(
                        WireMock.aResponse()
                            .withStatus(200)
                            .withBody("レスポンスの内容")
                    )
            )

            val publicKey = keyPair.public as RSAPublicKey
            val jwksResponse = encodePublicKeyToJwks(publicKey, "test-kid")

            wireMockServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/.well-known/jwks.json")).willReturn(
                    WireMock.aResponse().withStatus(200).withBody(jwksResponse)
                )
            )
        }

        @After
        fun teardown() {
            wireMockServer.stop()
        }


        @Test
        fun testSendRequestWithDirectPost() = runBlocking {
            // MockWebServerに対するレスポンスを設定します。
            wireMockServer.stubFor(
                WireMock.post(WireMock.urlEqualTo("/"))
                    .withHeader("Content-Type", WireMock.equalTo("application/x-www-form-urlencoded"))
                    .willReturn(
                        WireMock.aResponse()
                            .withStatus(200)
                            .withBody("response body")
                    )
            )

            // テスト対象のメソッドを呼び出します。
            val result = sendRequest("$clientHost:${wireMockServer.port()}/", mapOf("key" to "value"), ResponseMode.DIRECT_POST)

            // レスポンスが期待通りであることを確認します。
            assertEquals(200, result.statusCode)

            // リクエストが期待通りであることを確認します。
            val recordedRequest = wireMockServer.findAll(WireMock.postRequestedFor(WireMock.urlEqualTo("/"))).first()
            assertEquals("POST", recordedRequest.method.toString())
            assertEquals("key=value", recordedRequest.bodyAsString)
        }

        @Test
        fun testRespondIdTokenResponse() = runBlocking {
            val clientMetadataUri = "$clientHost:${wireMockServer.port()}/client-metadata-uri"
            val requestJwt = createRequestObjectJwt(
                keyPair.private,
                "test-kid",
                "$clientHost:${wireMockServer.port()}/cb",
                clientMetadataUri
            )
            val uri =
                "siopv2://?client_id=123&redirect_uri=123&request=$requestJwt"

            val op = OpenIdProvider(uri)
            val result = op.processAuthorizationRequest()
            result.fold(
                ifLeft = { value ->
                    fail("エラーが発生しました: ${value}")
                },
                ifRight = { value ->
                    val (scheme, requestObject, authorizationRequestPayload, requestObjectJwt, registrationMetadata) = value
                    // RequestObjectPayloadオブジェクトの内容を検証
                    assertEquals("openid", requestObject?.scope)
                    assertEquals("code id_token", requestObject?.responseType)
                    assertEquals("$clientHost:${wireMockServer.port()}/cb", requestObject?.clientId)
                    assertEquals("$clientHost:${wireMockServer.port()}/cb", requestObject?.redirectUri)
                    // nonceとstateはランダムに生成される可能性があるため、存在することのみを確認
                    assertNotNull(requestObject?.nonce)
                    assertNotNull(requestObject?.state)
                    assertEquals(86400, requestObject?.maxAge)

                    assertEquals("ClientName", registrationMetadata.clientName)
                    assertEquals("https://example.com/logo.png", registrationMetadata.logoUri)
                })

            // 結果の検証
            assertNotNull(result)
            assertTrue(result.isRight())

            // SIOP Response送信
            var keyRing = HDKeyRing(null)
            val jwk = keyRing.getPrivateJwk(1)
            val privateJwk = object : ECPrivateJwk {
                override val kty = jwk.kty
                override val crv = jwk.crv
                override val x = jwk.x
                override val y = jwk.y
                override val d = jwk.d
            }
            val keyPair = SignatureUtil.generateECKeyPair(privateJwk)
            op.setKeyPair(keyPair)
            val responseResult = op.respondIdTokenResponse()
            assertTrue(responseResult.isRight())
        }

        @Test
        fun testMergeOAuth2AndOpenIdInRequestPayload() {
            val payload = AuthorizationRequestPayloadImpl(
                scope = "openid email",
                responseType = "code",
                clientId = "client123",
                redirectUri = "https://client.example.com/cb",
                nonce = "nonce123",
                state = "state123",
                clientIdScheme = "redirect_uri"
            )

            val requestObject = RequestObjectPayloadImpl(
                iss = "issuer2",
                aud = Audience.Single("aud2"),
                responseType = "code id_token",
                clientId = "client456",
                redirectUri = "https://client.example.com/redirect",
                idTokenHint = "idTokenHint",
                clientIdScheme = "redirect_uri"
            )

            val mergedMap = mergeOAuth2AndOpenIdInRequestPayload(payload, requestObject)

            // マージされたプロパティを検証
            assertEquals("issuer2", mergedMap.iss)
            assertEquals(Audience.Single("aud2"), mergedMap.aud)
            assertEquals("code id_token", mergedMap.responseType)
            assertEquals("client456", mergedMap.clientId)
            assertEquals("https://client.example.com/redirect", mergedMap.redirectUri)
            assertEquals("idTokenHint", mergedMap.idTokenHint)
            assertEquals("nonce123", mergedMap.nonce)
            assertEquals("state123", mergedMap.state)
        }
    }

    class ProcessAuthorizationRequestTest {
        private val keyPairTestCA = generateEcKeyPair()
        private val keyPairTestIssuer = generateEcKeyPair()
        lateinit var wireMockServer: WireMockServer
        lateinit var clientMetadataMap: Map<String, Any>
        lateinit var presentationDefinitionMap: Map<String, Any>
        val algorithm =
            Algorithm.ECDSA256(
                keyPairTestIssuer.public as ECPublicKey,
                keyPairTestIssuer.private as ECPrivateKey?
            )

        val clientMetadataJson = """{
            "scopes_supported": ["openid"],
            "subject_types_supported": ["public"],
            "id_token_signing_alg_values_supported": ["ES256"],
            "request_object_signing_alg_values_supported": ["ES256"],
            "subject_syntax_types_supported": ["syntax1", "syntax2"],
            "request_object_encryption_alg_values_supported": ["ES256"],
            "request_object_encryption_enc_values_supported": ["ES256"],
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
            clientMetadataMap = mapper.readValue<Map<String, Any>>(clientMetadataJson)
            presentationDefinitionMap = mapper.readValue<Map<String, Any>>(presentationDefinitionJson)
        }

        @After
        fun teardown() {
            wireMockServer.stop()
        }

        private fun prepareRequestUri(jwt: String): String? {
            val requestUriPath = "/request-uri"
            wireMockServer.stubFor(
                WireMock.get(WireMock.urlEqualTo(requestUriPath))
                    .willReturn(
                        WireMock.aResponse()
                            .withStatus(200)
                            .withBody(jwt)
                    )
            )

            val requestUri = "http://localhost:${wireMockServer.port()}$requestUriPath"
            return URLEncoder.encode(requestUri, StandardCharsets.UTF_8.toString())
        }

        val cert0 = SignatureUtil.generateCertificate(keyPairTestIssuer, keyPairTestCA, false, listOf("alt1.verifier.com"))
        val encodedCert0 = Base64.getEncoder().encodeToString(cert0.encoded)
        val cert1 =
            SignatureUtil.generateCertificate(keyPairTestCA, keyPairTestCA, true) // 認証局は自己証明
        val encodedCert1 = Base64.getEncoder().encodeToString(cert1.encoded)
        val certs = listOf(encodedCert0, encodedCert1)

        @Test
        fun testClientIdSchemeX509SanDnsFailure() = runBlocking {
            val clientId = "https://not-registered-in-san.verifier.com/cb"
            val requestObjectJwt = JWT.create()
                .withClaim("client_id_scheme", "x509_san_dns")
                .withClaim("response_type", "vp_token")
                .withClaim("response_uri", clientId)
                .withClaim("client_metadata", clientMetadataMap)
                .withClaim("presentation_definition", presentationDefinitionMap)
                .withHeader(mapOf("x5c" to certs))
                .withExpiresAt(Date(System.currentTimeMillis() + 60 * 1000))
                .sign(algorithm)
            val encodedJwtString =
                URLEncoder.encode(requestObjectJwt, StandardCharsets.UTF_8.toString())
            val encodedRequestUri = prepareRequestUri(encodedJwtString)

            val uri =
                "siopv2://?client_id=$clientId&request_uri=$encodedRequestUri"

            val op = OpenIdProvider(uri)
            val result = op.processAuthorizationRequest()
            println(result)
            assertTrue(result.isLeft())
        }

        @Test
        fun testClientIdSchemeX509SanDnsSuccess() = runBlocking {
            val clientId = "https://alt1.verifier.com/cb"
            val requestObjectJwt = JWT.create()
                .withClaim("client_id_scheme", "x509_san_dns")
                .withClaim("response_type", "vp_token")
                .withClaim("response_uri", clientId)
                .withClaim("client_metadata", clientMetadataMap)
                .withClaim("presentation_definition", presentationDefinitionMap)
                .withHeader(mapOf("x5c" to certs))
                .withExpiresAt(Date(System.currentTimeMillis() + 60 * 1000))
                .sign(algorithm)
            val encodedJwtString =
                URLEncoder.encode(requestObjectJwt, StandardCharsets.UTF_8.toString())
            val encodedRequestUri = prepareRequestUri(encodedJwtString)

            val uri =
                "siopv2://?client_id=$clientId&request_uri=$encodedRequestUri"

            val op = OpenIdProvider(uri)
            val result = op.processAuthorizationRequest()
            println(result)

            assertTrue(result.isRight())
        }

        @Test
        fun testClientIdSchemeRedirectUriFailure() = runBlocking {
            val clientId = "https://www.verifier.com/cb"
            val badRedirectUri = "https://bad-site.com/cb"
            val encodedClientId = URLEncoder.encode(clientId, StandardCharsets.UTF_8.toString())
            val encodedBadRedirectUri = URLEncoder.encode(badRedirectUri, StandardCharsets.UTF_8.toString())
            val encodedClientMetadata =
                URLEncoder.encode(clientMetadataJson, StandardCharsets.UTF_8.toString())
            val encodedPresentationDefinition =
                URLEncoder.encode(presentationDefinitionJson, StandardCharsets.UTF_8.toString())
            val uri =
                "openid4vp:///?client_id=${encodedClientId}" +
                        "&client_id_scheme=redirect_uri" +
                        "&response_type=vp_token" +
                        "&response_uri=${encodedBadRedirectUri}" +
                        "&client_metadata=${encodedClientMetadata}" +
                        "&presentation_definition=${encodedPresentationDefinition}"
            val op = OpenIdProvider(uri)
            val result = op.processAuthorizationRequest()
            println(result)

            assertTrue(result.isLeft())
        }

        @Test
        fun testClientIdSchemeRedirectUriSuccess() = runBlocking {
            val clientId = "https://www.verifier.com/cb"
            val encodedClientId = URLEncoder.encode(clientId, StandardCharsets.UTF_8.toString())
            val encodedClientMetadata =
                URLEncoder.encode(clientMetadataJson, StandardCharsets.UTF_8.toString())
            val encodedPresentationDefinition =
                URLEncoder.encode(presentationDefinitionJson, StandardCharsets.UTF_8.toString())
            val uri =
                "openid4vp:///?client_id=${encodedClientId}" +
                        "&client_id_scheme=redirect_uri" +
                        "&response_type=vp_token" +
                        "&response_uri=${encodedClientId}" +
                        "&client_metadata=${encodedClientMetadata}" +
                        "&presentation_definition=${encodedPresentationDefinition}"
            val op = OpenIdProvider(uri)
            val result = op.processAuthorizationRequest()
            println(result)

            assertTrue(result.isRight())
        }
    }
}

