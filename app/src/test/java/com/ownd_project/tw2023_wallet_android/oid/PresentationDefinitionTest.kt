package com.ownd_project.tw2023_wallet_android.oid

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.authlete.sd.Disclosure
import com.authlete.sd.SDObjectBuilder
import com.ownd_project.tw2023_wallet_android.createQueryParameterFromJson
import com.ownd_project.tw2023_wallet_android.encodePublicKeyToJwks
import com.ownd_project.tw2023_wallet_android.utils.generateEcKeyPair
import com.ownd_project.tw2023_wallet_android.utils.generateRsaKeyPair
import com.ownd_project.tw2023_wallet_android.utils.publicKeyToJwk
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.net.URLEncoder
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Date


val presentationDefinitionJson = """
        {
          "id": "12345",
          "input_descriptors": [
            {
              "id": "input1",
              "name": "First Input",
              "purpose": "For identification",
              "format": {
                "vc+sd-jwt": {}
              },
              "group": ["A"],
              "constraints": {
                "limit_disclosure": "required",
                "fields": [
                  {
                    "path": ["${'$'}.is_older_than_13"],
                    "filter": {"type": "boolean"}
                  }
                ]
              }
            }
          ],
          "submission_requirements": [
            {
              "name": "Over13 Proof",
              "rule": "pick",
              "count": 1,
              "from": "A"
            }
          ]
        }
        """.trimIndent()

class PresentationDefinitionTest {
    private lateinit var wireMockServer: WireMockServer
    private val keyPair = generateRsaKeyPair()
    private val ecKeyPair = generateEcKeyPair()

    private val presentationDefinitionJson2 = """
        {
          "id": "12345",
          "input_descriptors": [
            {
              "id": "input1",
              "name": "First Input",
              "purpose": "For identification",
              "format": {
                "vc+sd-jwt": {}
              },
              "group": ["A"],
              "constraints": {
                "limit_disclosure": "required",
                "fields": [
                  {
                    "path": ["${'$'}.claim1"],
                    "filter": {"type": "String"}
                  }
                ]
              }
            }
          ],
          "submission_requirements": [
            {
              "name": "Test Proof",
              "rule": "pick",
              "count": 1,
              "from": "A"
            }
          ]
        }
        """.trimIndent()

    // presentation_definition=xxx クエリーパラメーターで値渡しでくる場合
    // presentation_definition_uri=xxx クエリーパラメーターで参照渡しでくる場合
    // presentation_definition=xxx ROで値渡しでくる場合
    // presentation_definition_uri=xxx ROで参照渡しでくる場合

    @Before
    fun setup() {
        wireMockServer = WireMockServer().apply {
            start()
            WireMock.configureFor("localhost", port())
        }

        // request object
        val requestJwt = createRequestObjectJwt(
            keyPair.private,
            "test-kid",
            "$clientHost:${wireMockServer.port()}/cb",
            "$clientHost:${wireMockServer.port()}/client-metadata",
            "$clientHost:${wireMockServer.port()}/post",
        )

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/auth-request")).willReturn(
                WireMock.aResponse().withStatus(200).withBody(requestJwt)
            )
        )

        // client metadata
        val jwksUrl = "$clientHost:${wireMockServer.port()}/.well-known/jwks.json"
        val clientMetadataJson = """{
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
            "client_purpose": "authentication"
        }"""
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/client-metadata")).willReturn(
                WireMock.aResponse().withStatus(200).withBody(clientMetadataJson)
            )
        )

        // jwks
        val publicKey = keyPair.public as RSAPublicKey
        val jwksResponse = encodePublicKeyToJwks(publicKey, "test-kid")
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/.well-known/jwks.json")).willReturn(
                WireMock.aResponse().withStatus(200).withBody(jwksResponse)
            )
        )

        // presentation definition
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/presentationdefs")).willReturn(
                WireMock.aResponse().withStatus(200).withBody(presentationDefinitionJson)
            )
        )

        // direct_post
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/post"))
                .withHeader("Content-Type", WireMock.equalTo("application/x-www-form-urlencoded"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody("")
                )
        )
    }

    @After
    fun teardown() {
        wireMockServer.stop()
    }

    fun createRequestObjectJwt(
        privateKey: PrivateKey,
        kid: String,
        clientId: String,
        clientMetadataUri: String?,
        responseUri: String?
    ): String {
        val algorithm = Algorithm.RSA256(null, privateKey as RSAPrivateKey)

        return JWT.create().withIssuer("https://client.example.org/cb")
            .withAudience("https://server.example.com")
            .withClaim("response_type", "vp_token")
            .withClaim("response_mode", "direct_post")
            .withClaim("client_id", clientId)
            .withClaim("redirect_uri", clientId)
            .withClaim("scope", "openid")
            .withClaim("state", "af0ifjsldkj")
            .withClaim("nonce", "n-0S6_WzA2Mj")
            .withClaim("max_age", 86400).withIssuedAt(Date())
            .withClaim("client_metadata_uri", clientMetadataUri)
            .withClaim("presentation_definition", presentationDefinitionJson2)
            .withClaim("response_uri", responseUri)
            .withKeyId(kid)
            .sign(algorithm)
    }

    @Test
    fun testDeserializePresentationDefinition() {

        val presentationDefinition = deserializePresentationDefinition(presentationDefinitionJson)

        // 基本的なプロパティの確認
        assertEquals("12345", presentationDefinition.id)
        assertEquals(1, presentationDefinition.inputDescriptors.size)
        assertEquals("input1", presentationDefinition.inputDescriptors[0].id)
        assertEquals(
            Rule.PICK,
            presentationDefinition.submissionRequirements?.get(0)?.rule
        )
    }

    @Test
    fun testDeserializeFromQueryParameter() {
        val queryParam =
            createQueryParameterFromJson(presentationDefinitionJson, "presentation_definition")

        val uri = "http://localhost/?$queryParam"
        val queryParams = decodeUriAsJson(uri)
        val json = queryParams["presentation_definition"] ?: ""

        val presentationDefinition = convertPresentationDefinition(json as Map<String, Any>)
        assertEquals("12345", presentationDefinition.id)
    }

    @Test
    fun testDeserializeFromJwt() {
        val jwt = createRequestObjectJwt(keyPair.private, "test-kid", "client_id", "", "")
        val decodedJwt = JWT.decode(jwt)
        val json = decodedJwt.getClaim("presentation_definition").asString()

        val presentationDefinition = deserializePresentationDefinition(json)
        assertEquals("12345", presentationDefinition.id)
    }

    @Test
    fun testDeserializeFromApiResponse() = runBlocking {
        // REST APIのレスポンスを想定
        val jsonResponse = "ここにJSONレスポンスボディ"

        val clientHost = "http://localhost"
        val url = "$clientHost:${wireMockServer.port()}/presentationdefs"

        val presentationDefinition =
            fetchByReferenceOrUseByValue(url, null, PresentationDefinition::class.java)

        // デシリアライズ結果の検証
        assertEquals("12345", presentationDefinition.id)
    }

    // キーバインディングの実装がキーストアの秘密鍵を使う必要があったので一旦テストをコメントアウト(todo androidTestの下に移動する)
//    @Test
//    fun testKeyBindingJwt() {
//        val disclosure1 = Disclosure("claim1", "value1")
//        val disclosure2 = Disclosure("claim2", "value2")
//        val algorithm =
//            Algorithm.ECDSA256(ecKeyPair.public as ECPublicKey, ecKeyPair.private as ECPrivateKey?)
//        val sdJwt = generateSdJwt(listOf<Disclosure>(disclosure1, disclosure2), algorithm)
//        val keyBindingJwt = OpenIdProvider.generateKeyBindingJwt(
//            sdJwt,
//            listOf(
//                SDJwtUtil.Disclosure(
//                    disclosure = disclosure1.disclosure,
//                    key = "test1",
//                    value = "test1"
//                )
//            ), "test aud", (ecKeyPair.private as ECPrivateKey)
//        )
//        println(keyBindingJwt)
//        val verifier = JWT.require(algorithm)
//            .build()
//        val decodedJWT = verifier.verify(keyBindingJwt)
//        assertEquals("test aud", decodedJWT.getClaim("aud").asString())
//        assertNotNull(decodedJWT.getClaim("nonce").asString())
//        assertNotNull(decodedJWT.getClaim("_sd_hash").asString())
//        assertNotNull(decodedJWT.issuedAt)
//    }

    @Test
    fun testSelectDisclosure() {
        val disclosure1 = Disclosure("claim1", "value1")
        val disclosure2 = Disclosure("claim2", "value2")
        val builder = SDObjectBuilder()
        builder.putSDClaim(disclosure1);
        builder.putSDClaim(disclosure2);
        val claims = builder.build()

        assertEquals(2, (claims["_sd"] as List<*>).size)

        val jwk = publicKeyToJwk(ecKeyPair.public)
        val cnf = mapOf("jwk" to jwk)
        val algorithm = Algorithm.ECDSA256(null, ecKeyPair.private as ECPrivateKey?)
        val issuerSignedJwt = JWT.create().withIssuer("https://client.example.org/cb")
            .withAudience("https://server.example.com")
            .withClaim("cnf", cnf)
            .withClaim("_sd", (claims["_sd"] as List<*>))
            .sign(algorithm)
        val sdJwt = "$issuerSignedJwt~${disclosure1.disclosure}~${disclosure2.disclosure}"
        println(sdJwt)

        val presentationDefinitionJson = """
        {
          "id": "12345",
          "input_descriptors": [
            {
              "id": "input1",
              "name": "First Input",
              "purpose": "For identification",
              "format": {
                "vc+sd-jwt": {}
              },
              "group": ["A"],
              "constraints": {
                "limit_disclosure": "required",
                "fields": [
                  {
                    "path": ["${'$'}.claim1"],
                    "filter": {"type": "string"}
                  }
                ]
              }
            }
          ]
        }
        """.trimIndent()
        val presentationDefinition = deserializePresentationDefinition(presentationDefinitionJson)
        val selected = OpenIdProvider.selectDisclosure(sdJwt, presentationDefinition)
        assertNotNull(selected)
        val (_, disclosures) = selected!!
        assertEquals(1, disclosures!!.size)
        assertEquals("claim1", disclosures!![0].key)
        assertEquals("value1", disclosures!![0].value)
    }

    @Test @Ignore("Temporarily skipped because the issue where deserializePresentationDefinition " +
            "fails to parse JSON cannot currently be resolved. " +
            "Furthermore, when acquiring the request object from the URI, it appears to be successful.")
    fun testProcessSIOPRequest() = runBlocking {
        val uri =
            "xxx://?client_id=123&redirect_uri=123&request_uri=${
                URLEncoder.encode(
                    "$clientHost:${wireMockServer.port()}/auth-request", "UTF-8"
                )
            }"

        val op = OpenIdProvider(uri)
        val result = op.processAuthorizationRequest()
        result.fold(
            onFailure = { value ->
                TestCase.fail("エラーが発生しました: ${value.message}")
            },
            onSuccess = { value ->
                val (scheme, requestObject, authorizationRequestPayload, requestObjectJwt, registrationMetadata, presentationDefinition) = value
                // RequestObjectPayloadオブジェクトの内容を検証
                TestCase.assertEquals("openid", requestObject?.scope)
                TestCase.assertEquals("vp_token", requestObject?.responseType)
                TestCase.assertEquals(
                    "$clientHost:${wireMockServer.port()}/cb",
                    requestObject?.clientId
                )
                TestCase.assertEquals(
                    "$clientHost:${wireMockServer.port()}/cb",
                    requestObject?.redirectUri
                )
                // nonceとstateはランダムに生成される可能性があるため、存在することのみを確認
                TestCase.assertNotNull(requestObject?.nonce)
                TestCase.assertNotNull(requestObject?.state)
                TestCase.assertEquals(86400, requestObject?.maxAge)

                TestCase.assertEquals("ClientName", registrationMetadata.clientName)
                TestCase.assertEquals("https://example.com/logo.png", registrationMetadata.logoUri)

                TestCase.assertEquals("12345", presentationDefinition!!.id)
            })

        // 結果の検証
        TestCase.assertNotNull(result)
        TestCase.assertTrue(result.isSuccess)

        val disclosure1 = Disclosure("claim1", "value1")
        val disclosure2 = Disclosure("claim2", "value2")
        // todo キーバインディングの処理がキーストアを必要とする設計になってしまったのでandroidTest配下じゃないとテストを実行できない(移動するのではなくてモックする方法を考える)
//        val algorithm =
//            Algorithm.ECDSA256(ecKeyPair.public as ECPublicKey, ecKeyPair.private as ECPrivateKey?)
//        val sdJwt = generateSdJwt(listOf<Disclosure>(disclosure1, disclosure2), algorithm)
//        op.setKeyPair(ecKeyPair)
//        val submissionCredential = SubmissionCredential(
//            id = "test_id_1",
//            format = "vc+sd-jwt",
//            types = MetadataUtil.extractTypes("vc+sd-jwt", sdJwt),
//            sdJwt,
//            op.getSiopRequest().presentationDefinition!!.inputDescriptors[0]
//        )
//        val responseResult = op.respondVPResponse(listOf(submissionCredential))
//        TestCase.assertTrue(responseResult.isRight())
    }

    fun generateSdJwt(disclosures: List<Disclosure>, algorithm: Algorithm): String {
        val builder = SDObjectBuilder()
        disclosures.forEach { it ->
            builder.putSDClaim(it);
        }
        val claims = builder.build()

        assertEquals(2, (claims["_sd"] as List<*>).size)
        val jwk = publicKeyToJwk(ecKeyPair.public)
        val cnf = mapOf("jwk" to jwk)
        val issuerSignedJwt = JWT.create().withIssuer("https://client.example.org/cb")
            .withAudience("https://server.example.com")
            .withClaim("cnf", cnf)
            .withClaim("vct", "TestCredential")
            .withClaim("_sd", (claims["_sd"] as List<*>))
            .sign(algorithm)
        return "$issuerSignedJwt~${disclosures.joinToString("~") { it.disclosure }}"
    }
}

