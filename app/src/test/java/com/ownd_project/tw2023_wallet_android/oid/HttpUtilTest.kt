package com.ownd_project.tw2023_wallet_android.oid

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking
import java.io.IOException

class HttpUtilTest {

    private lateinit var wireMockServer: WireMockServer

    @Before
    fun setup() {
        wireMockServer = WireMockServer(8080)
        wireMockServer.start()
        configureFor("localhost", 8080)
    }

    @After
    fun teardown() {
        wireMockServer.stop()
    }

    @Test
    fun testGetWithUrlForJSONResponse() {
        stubFor(get(urlEqualTo("/test-json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"key\":\"value\"}")))

        val url = "http://localhost:8080/test-json"
        val result: Map<*, *> = runBlocking { getWithUrl(url, Map::class.java) }

        assertEquals("value", result["key"])
    }

    @Test
    fun testGetWithUrlForTextResponse() {
        stubFor(get(urlEqualTo("/test-text"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("Hello World")))

        val url = "http://localhost:8080/test-text"
        val result: String = runBlocking { getWithUrl(url, String::class.java, true) }

        assertEquals("Hello World", result)
    }

    @Test
    fun testFetchByReferenceOrUseByValueWithReference() {
        stubFor(get(urlEqualTo("/test-reference"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("Reference Response")))

        val referenceUrl = "http://localhost:8080/test-reference"
        val fallbackValue = "Fallback Value"
        val result: String = runBlocking { fetchByReferenceOrUseByValue(referenceUrl, fallbackValue, String::class.java, true) }

        assertEquals("Reference Response", result)
    }

    @Test
    fun testFetchByReferenceOrUseByValueWithoutReference() {
        val fallbackValue = "Fallback Value"
        val result: String = runBlocking { fetchByReferenceOrUseByValue(null, fallbackValue, String::class.java, true) }

        assertEquals(fallbackValue, result)
    }

    @Test(expected = IOException::class)
    fun testFetchByReferenceOrUseByValueWithError() {
        stubFor(get(urlEqualTo("/test-error"))
            .willReturn(aResponse()
                .withStatus(500)))

        val errorUrl = "http://localhost:8080/test-error"
        val fallbackValue = "Fallback Value"
        runBlocking { fetchByReferenceOrUseByValue(errorUrl, fallbackValue, String::class.java, true) }
    }
}
