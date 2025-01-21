package com.ownd_project.tw2023_wallet_android.features.data_sharing.flow3

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.authlete.sd.Disclosure
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.datastore.CredentialData
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.features.data_sharing.generateSdJwtCredential
import com.ownd_project.tw2023_wallet_android.features.data_sharing.generateVCJwtCredential
import com.ownd_project.tw2023_wallet_android.features.data_sharing.inputDescriptor1
import com.ownd_project.tw2023_wallet_android.features.data_sharing.json
import com.ownd_project.tw2023_wallet_android.features.data_sharing.requestInfo
import com.ownd_project.tw2023_wallet_android.oid.PresentationDefinition
import com.ownd_project.tw2023_wallet_android.ui.siop_vp.shared_data_confirmation.SharedDataConfirmationFragment
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata

@RunWith(AndroidJUnit4::class)
class SharedDataConfirmationFragmentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFragmentLaunch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val credentialDataStore = CredentialDataStore.getInstance(context)

        // -------------- sd-jwt ------------------
        // スネークケースのオリジナルを一度シリアライズしてからデシリアライズしてキャメルケースで保存させる
        val credentialId = "test-id1"
        val mapper = jacksonObjectMapper().apply {
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
        val sdJwtMetadataStr =
            jacksonObjectMapper().writeValueAsString(
                mapper.readValue(
                    json,
                    CredentialIssuerMetadata::class.java
                )
            )
        val vct = "OrganizationalAffiliationCertificate"
        val disclosures =
            listOf(
                Disclosure("organization_name", "test org"),
                Disclosure("family_name", "test family name"),
                Disclosure("given_name", "test given name")
            )
        val sdJwtCredential = generateSdJwtCredential(vct, disclosures)
        val testCredential1 = CredentialData.newBuilder()
            .setId(credentialId)
            .setFormat("vc+sd-jwt")
            .setCredential(sdJwtCredential)
            .setIss("https://event.company/issuers/565049")
            .setIat(123456789L)
            .setExp(987654321L)
            .setType(vct)
            .setCredentialIssuerMetadata(sdJwtMetadataStr)
            .build()

        // -------------- jwt-vc-json ------------------
        val vcJwtCredential = generateVCJwtCredential()
        val testCredential2 = CredentialData.newBuilder()
            .setId("test-id2")
            .setFormat("jwt_vc_json")
            .setCredential(vcJwtCredential)
            .setIss("me")
            .setIat(123456789L)
            .setExp(987654321L)
            .setType("CommentCredential")
            .setCredentialIssuerMetadata("dummy-metadata")
            .build()

        runBlocking {
            credentialDataStore.deleteAllCredentials()
            credentialDataStore.saveCredentialData(testCredential1)
            credentialDataStore.saveCredentialData(testCredential2)
        }

        val fragmentArgs = bundleOf("credentialId" to credentialId)
        val scenario = launchFragmentInContainer<SharedDataConfirmationFragment>(fragmentArgs)
        scenario.onFragment { fragment ->
            fragment.viewModel.setCredentialDataStore(credentialDataStore)
            fragment.sharedViewModel.setPresentationDefinition(
                PresentationDefinition(
                    id = "dummy id",
                    inputDescriptors = listOf(inputDescriptor1),
                    submissionRequirements = null,
                    name = "test",
                    purpose = "test",
                )
            )
            fragment.sharedViewModel.setRequestInfo(requestInfo)
            fragment.sharedViewModel.setSubJwk("dummy id")
        }
        composeTestRule.onRoot().printToLog("TAG")
        composeTestRule.onNodeWithTag("title").assertIsDisplayed()
    }
}

