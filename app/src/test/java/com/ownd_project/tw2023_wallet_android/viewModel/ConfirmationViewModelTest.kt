package com.ownd_project.tw2023_wallet_android.viewModel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.ui.confirmation.ConfirmationViewModel
import com.ownd_project.tw2023_wallet_android.vci.CredentialOffer
import com.ownd_project.tw2023_wallet_android.vci.Display
import com.ownd_project.tw2023_wallet_android.vci.IssuerCredentialSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import java.util.Locale

class ConfirmationViewModelTest {
    private lateinit var wireMockServer: WireMockServer
    private lateinit var viewModel: ConfirmationViewModel

    private var port: Int = 0
    private val endPointCredentialIssuerMetadata = "/.well-known/openid-credential-issuer"
    private val endPointAuthServer = "/.well-known/oauth-authorization-server"
    private val endPointTokenResponse = "/token"

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var textObserver: Observer<String>

    @Mock
    private lateinit var credentialSubjectObserver: Observer<Map<String, IssuerCredentialSubject>>

    private lateinit var mockCredentialDataStore: CredentialDataStore
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        MockitoAnnotations.openMocks(this)
        // テスト用のロケールを設定
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.JAPAN) // または任意のロケールに設定

        wireMockServer = WireMockServer()
        wireMockServer.start()
        port = wireMockServer.port()
        mockCredentialDataStore = mock(CredentialDataStore::class.java)

        // ここでViewModelのインスタンスを生成し、セッターを使ってモックを注入
        viewModel = ConfirmationViewModel()
        viewModel.setCredentialDataStore(mockCredentialDataStore)

        // LiveDataのObserverを設定
        viewModel.text.observeForever(textObserver)
        viewModel.credentialSubject.observeForever(credentialSubjectObserver)

        var credentialIssuerMetadataJson =
            this::class.java.classLoader.getResource("credential_issuer_metadata_jwt_vc.json")
                ?.readText()
                ?: throw IllegalArgumentException("Cannot read retrieved_metadata.json")
        credentialIssuerMetadataJson = credentialIssuerMetadataJson.replace(
            "https://datasign-demo-vci.tunnelto.dev", "http://localhost:${port}"
        )
        wireMockServer.stubFor(
            get(urlEqualTo(endPointCredentialIssuerMetadata)).willReturn(
                aResponse().withHeader("Content-Type", "application/json").withStatus(200)
                    .withBody(credentialIssuerMetadataJson)
            )
        )

        var authorizationServerJson =
            this::class.java.classLoader?.getResource("authorization_server.json")?.readText()
                ?: throw IllegalArgumentException("Cannot read authorization_server.json")
        authorizationServerJson =
            authorizationServerJson.replace("datasign-demo-vci.tunnelto.dev", "localhost:${port}")
        wireMockServer.stubFor(
            get(urlEqualTo(endPointAuthServer)).willReturn(
                aResponse().withHeader("Content-Type", "application/json").withStatus(200)
                    .withBody(authorizationServerJson)
            )
        )
        var tokenResponseJson =
            this::class.java.classLoader?.getResource("token_response.json")?.readText()
                ?: throw IllegalArgumentException("Cannot read token_response.json")
        wireMockServer.stubFor(
            get(urlEqualTo(endPointTokenResponse)).willReturn(
                aResponse().withHeader("Content-Type", "application/json").withStatus(200)
                    .withBody(tokenResponseJson)
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()

        // ロケールを元に戻す
        Locale.setDefault(originalLocale)

        // LiveDataのObserverを解除
        viewModel.text.removeObserver(textObserver)
        viewModel.credentialSubject.removeObserver(credentialSubjectObserver)

        wireMockServer.stop()
    }

    //displaysがインスタンス化されて比較できないため、中身を比較するための関数を用意
    private fun displaysAreEqual(list1: List<Display>?, list2: List<Display>?): Boolean {
        if (list1 == null && list2 == null) return true
        if (list1 == null || list2 == null) return false
        if (list1.size != list2.size) return false

        return list1.zip(list2).all { (d1, d2) ->
            if (d1.locale != null && d2.locale != null) {
                d1.name == d2.name && d1.locale == d2.locale
            } else {
                d1.name == d2.name
            }
        }
    }

    @Test
    fun processMetadataTest() = runBlocking {
        // 必要なデータの準備
        var testParameterValue =
            this::class.java.classLoader!!.getResource("credential_offer.json")?.readText()
                ?: throw IllegalArgumentException("Cannot read credential_offer.json")
        testParameterValue =
            testParameterValue.replace(
                "https://datasign-demo-vci.tunnelto.dev",
                "http://localhost:${port}"
            )

        val mapper = jacksonObjectMapper().apply {
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
        val credentialOffer = mapper.readValue(testParameterValue, CredentialOffer::class.java)

        // メソッドの実行
        viewModel.processMetadata(credentialOffer.credentialIssuer, credentialOffer)

        // アサーション
        verify(textObserver).onChanged("オウンドプロジェクト が University Credential の証明書を発行します")
        val expectedMap = mapOf(
            "given_name" to IssuerCredentialSubject(
                mandatory = null,
                valueType = null,
                display = listOf(Display(name = "名", locale = "ja_JP"))
            ),
            "last_name" to IssuerCredentialSubject(
                mandatory = null,
                valueType = null,
                display = listOf(Display(name = "姓", locale = "ja_JP"))
            ),
            "degree" to IssuerCredentialSubject(
                mandatory = null,
                valueType = null,
                display = null
            ),
            "gpa" to IssuerCredentialSubject(
                mandatory = null,
                valueType = null,
                display = listOf(Display(name = "GPA"))
            ),
        )

        val actualMap = viewModel.credentialSubject.value

        assertTrue(
            displaysAreEqual(
                actualMap?.get("given_name")?.display,
                expectedMap["given_name"]?.display
            )
        )
        assertTrue(
            displaysAreEqual(
                actualMap?.get("last_name")?.display,
                expectedMap["last_name"]?.display
            )
        )
        println("ConfirmationViewModelTest===actualMap = ${actualMap?.get("gpa")?.display!![0].name}")
        println("ConfirmationViewModelTest===expectedMap = ${expectedMap?.get("gpa")?.display!![0].name}")
        assertTrue(
            displaysAreEqual(
                actualMap?.get("gpa")?.display,
                expectedMap["gpa"]?.display
            )
        )

        // 他のLiveDataや状態の確認
    }

    @Test
    fun testExtractSDJwtInfoWithType() {
        //val header = "{\"alg\":\"RS256\"}"
        //val payload = "{\"iss\":\"testIssuer\",\"iat\":1635082800,\"exp\":1635082830,\"type\":\"testType\"}"
        //val signature = "signature"  // 実際の署名生成ロジックを用いる場合は、この値を更新します。
        //val issuerSignedJwt = "${Base64.getUrlEncoder().encodeToString(header.toByteArray())}.${Base64.getUrlEncoder().encodeToString(payload.toByteArray())}.$signature"
        //val disclosure1 = "Disclosure1"  // これは実際のDisclosureデータに置き換える必要があります。
        //val disclosure2 = "Disclosure2"  // 同上
        //val kbJwt = "KB-JWT"  // 実際のKey Binding JWTに置き換える必要があります。
        //val testJwt = "$issuerSignedJwt~$disclosure1~$disclosure2~$kbJwt"
        val testTypeJwt =
            "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ0ZXN0SXNzdWVyIiwiaWF0IjoxNjM1MDgyODAwLCJleHAiOjE2MzUwODI4MzAsInR5cGUiOiJ0ZXN0VHlwZSJ9.~signature~Disclosure1~Disclosure2~KB-JWT"
        val formatWithType = "jwt_vc_json"


        val method = ConfirmationViewModel::class.java.getDeclaredMethod(
            "extractSDJwtInfo", String::class.java, String::class.java
        )
        method.isAccessible = true

        val typeResult = method.invoke(viewModel, testTypeJwt, formatWithType) as Map<*, *>

        assertEquals("testIssuer", typeResult["iss"])
        assertEquals(1635082800L, typeResult["iat"])
        assertEquals(1635082830L, typeResult["exp"])
        assertEquals("testType", typeResult["typeOrVct"])

    }

    @Test
    fun testExtractSDJwtInfoWithVct() {
        val testJwtWithVct =
            "eyJhbGciOiAiUlMyNTYifQ.eyJpc3MiOiAidGVzdElzc3VlciIsICJpYXQiOiAxNjM1MDgyODAwLCAiZXhwIjogMTYzNTA4MjgzMCwgInZjdCI6ICJ0ZXN0VmN0In0.signature\n"

        val formatWithVct = "vc+sd-jwt" // または "vc+sd-jwt" に応じて設定

        val method = ConfirmationViewModel::class.java.getDeclaredMethod(
            "extractSDJwtInfo", String::class.java, String::class.java
        )
        method.isAccessible = true

        val result = method.invoke(viewModel, testJwtWithVct, formatWithVct) as Map<*, *>

        assertEquals("testIssuer", result["iss"])
        assertEquals(1635082800L, result["iat"])
        assertEquals(1635082830L, result["exp"])
        assertEquals("testVct", result["typeOrVct"])
    }
}
