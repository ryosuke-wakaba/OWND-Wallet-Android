package com.ownd_project.tw2023_wallet_android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import com.ownd_project.tw2023_wallet_android.vci.CredentialConfigurationJwtVcJson
import com.ownd_project.tw2023_wallet_android.vci.CredentialsSupportedDisplay
import com.ownd_project.tw2023_wallet_android.vci.Display
import com.ownd_project.tw2023_wallet_android.vci.IssuerCredentialSubject
import com.ownd_project.tw2023_wallet_android.vci.JwtVcJsonCredentialDefinition
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.vci.ProofSigningAlgValuesSupported
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CredentialDataStoreInstrumentedTest {

    private lateinit var credentialDataStore: CredentialDataStore
    private lateinit var testCredential: com.ownd_project.tw2023_wallet_android.datastore.CredentialData
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testFileName = "test_data_${UUID.randomUUID()}.pb"

    //    private val testFileName = "test_data_${UUID.randomUUID()}.pb"
    private val objectMapper by lazy { jacksonObjectMapper() }

    @Before
    fun setUp() {
        credentialDataStore = CredentialDataStore.getInstance(context, testFileName)


        // テスト前に既存のデータをクリア（必要に応じて）
//        val credentialsSupportedList = listOf<CredentialSupported>(
//            CredentialSupportedJwtVcJson(
//                cryptographicBindingMethodsSupported = listOf("method1"),
//                cryptographicSuitesSupported = listOf("suite1"),
//                types = listOf("type1"),
//                credentialSubject = mapOf(
//                    "subject1" to IssuerCredentialSubject(
//                        mandatory = true,
//                        valueType = "String",
//                        display = listOf(Display(name = "Display Name", locale = "en"))
//                    )
//                ),
//                order = listOf("order1")
//            )
//        )
        val credentialDefinition = JwtVcJsonCredentialDefinition(
            type = listOf("IdentityCredential"),
            credentialSubject = mapOf(
                "given_name" to IssuerCredentialSubject(
                    mandatory = true,
                    valueType = "String",
                    display = listOf(Display(name = "Given Name", locale = "en-US"))
                )
            )
        )

        val credentialsSupportedMap = mapOf(
            "UniversityDegreeCredential" to CredentialConfigurationJwtVcJson(
                format = "dummy",
                scope = "UniversityDegree",
                cryptographicBindingMethodsSupported = listOf("method1"),
                credentialSigningAlgValuesSupported = listOf("suite1"),
                credentialDefinition = credentialDefinition,
                proofTypesSupported = mapOf(
                    "jwt" to ProofSigningAlgValuesSupported(listOf("ES256"))
                ),
                order = listOf("order1")
            )
        )

        val displayList = listOf(
            CredentialsSupportedDisplay(
                description = "display1", name = "Display Name 1", locale = "en"
            ),
            CredentialsSupportedDisplay(
                description = "display2", name = "Display Name 2", locale = "en"
            )
        )
        val metadata = CredentialIssuerMetadata(
            credentialIssuer = "some_issuer",
            credentialConfigurationsSupported = credentialsSupportedMap,
            display = displayList
        )

        val metadataJson = objectMapper.writeValueAsString(metadata)
        testCredential = com.ownd_project.tw2023_wallet_android.datastore.CredentialData.newBuilder()
            .setId("some_id")
            .setFormat("some_format")
            .setCredential("some_credential")
            .setCNonce("some_cNonce")
            .setCNonceExpiresIn(86400)
            .setIss("test_credential_data")
            .setIat(1683000000)
            .setExp(1883000000)
            .setType("some_type")
            .setCredentialIssuerMetadata(metadataJson)
            .build()
    }

    @After
    fun tearDown() {
        context.dataDir.resolve(testFileName).delete()
        CredentialDataStore.resetInstance()
    }

    @Test
    fun testGetAllNoData() = runBlocking {
        val credentials = credentialDataStore.getAllCredentials()
        assertEquals(0, credentials.size)
    }

    @Test
    fun testSaveAndGetAllCredentials() = runBlocking {
        credentialDataStore.saveCredentialData(testCredential)
        val credentials = credentialDataStore.getAllCredentials()
        assertEquals(1, credentials.size)
        assertEquals("test_credential_data", credentials[0].iss)

        // metadataをパース
        val metadata = objectMapper.readValue(
            credentials[0].credentialIssuerMetadata, CredentialIssuerMetadata::class.java
        )
        assertEquals("display1", metadata.display?.get(0)?.description)
        assertEquals("display2", metadata.display?.get(1)?.description)
    }

    @Test
    fun testDeleteCredentialByRandomId() = runBlocking {
        // 1. 保存するテストCredential
        credentialDataStore.saveCredentialData(testCredential)

        // 2. すべてのCredentialsを取得
        val credentialsBeforeDelete = credentialDataStore.getAllCredentials()

        // 3. 1件目のidを取得
        val idToDelete = credentialsBeforeDelete[0].id

        // 4. そのidを使用してCredentialを削除
        credentialDataStore.deleteCredentialById(idToDelete)

        // 5. 最後に、Credentialsが正しく削除されたか確認
        val credentialsAfterDelete = credentialDataStore.getAllCredentials()
        assertTrue(credentialsAfterDelete.none { it.id == idToDelete })
    }
}