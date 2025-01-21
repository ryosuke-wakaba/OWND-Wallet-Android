package com.ownd_project.tw2023_wallet_android.features.data_sharing.flow2

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.authlete.sd.Disclosure
import com.authlete.sd.SDObjectBuilder
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.datastore.CredentialData
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.ui.siop_vp.credential_selection.CredentialSelectionFragment
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.ownd_project.tw2023_wallet_android.utils.generateEcKeyPair
import com.ownd_project.tw2023_wallet_android.utils.generateRsaKeyPair
import com.ownd_project.tw2023_wallet_android.utils.publicKeyToJwk
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class CredentialSelectionFragmentTest {

//     @get:Rule
//    val composeTestRule = createAndroidComposeRule<TestActivity>()
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testFragmentLaunch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val credentialDataStore = CredentialDataStore.getInstance(context)

        // -------------- sd-jwt ------------------
        // スネークケースのオリジナルを一度シリアライズしてからデシリアライズしてキャメルケースで保存させる
        val mapper = jacksonObjectMapper().apply {
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
        val sdJwtMetadataStr =
            jacksonObjectMapper().writeValueAsString(mapper.readValue(json, CredentialIssuerMetadata::class.java))
        val sdJwtCredential = generateSdJwtCredential()
        val testCredential1 = CredentialData.newBuilder()
            .setId("test-id")
            .setFormat("vc+sd-jwt")
            .setCredential(sdJwtCredential)
            .setIss("https://event.company/issuers/565049")
            .setIat(123456789L)
            .setExp(987654321L)
            .setType("OrganizationalAffiliationCertificate")
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

        val scenario = launchFragmentInContainer<CredentialSelectionFragment>()
        scenario.onFragment { fragment ->
            fragment.viewModel.setCredentialDataStore(credentialDataStore)
        }
        composeTestRule.onRoot().printToLog("TAG")
        composeTestRule.onNodeWithTag("title").assertIsDisplayed()
        val currentLocale = Locale.getDefault().toLanguageTag()
        if (currentLocale == "en-US") {
            composeTestRule.onNodeWithText("Organizational Affiliation Credential").assertIsDisplayed()
        } else {
            composeTestRule.onNodeWithText("組織所属証明書").assertIsDisplayed()
        }
    }

    private fun generateSdJwtCredential(): String {
        val ecKeyPair = generateEcKeyPair()
        val algorithm =
            Algorithm.ECDSA256(ecKeyPair.public as ECPublicKey, ecKeyPair.private as ECPrivateKey?)
        val disclosures =
            listOf<Disclosure>(Disclosure("claim1", "value1"), Disclosure("claim2", "value2"))
        val builder = SDObjectBuilder()
        disclosures.forEach { it ->
            builder.putSDClaim(it)
        }
        val claims = builder.build()

        val jwk = publicKeyToJwk(ecKeyPair.public)
        val cnf = mapOf("jwk" to jwk)
        val issuerSignedJwt = JWT.create().withIssuer("https://client.example.org/cb")
            .withAudience("https://server.example.com")
            .withClaim("cnf", cnf)
            .withClaim("vct", "OrganizationalAffiliationCertificate")
            .withClaim("_sd", (claims["_sd"] as List<*>))
            .sign(algorithm)
        return "$issuerSignedJwt~${disclosures.joinToString("~") { it.disclosure }}"
    }
}

private fun generateVCJwtCredential(): String {
    val keyPair = generateRsaKeyPair()
    val algorithm = when (keyPair.private) {
        is RSAPrivateKey -> Algorithm.RSA256(null, keyPair.private as RSAPrivateKey)
        is ECPrivateKey -> Algorithm.ECDSA256(null, keyPair.private as ECPrivateKey)
        else -> throw IllegalArgumentException("未サポートの秘密鍵のタイプです。")
    }

    val type = listOf<String>("VerifiableCredential", "CommentCredential")
    val context = listOf<String>(
        "https://www.w3.org/ns/credentials/v2",
        "https://www.w3.org/ns/credentials/examples/v2"
    )
    return JWT.create()
        .withIssuer("me") // iss
        .withJWTId("http://event.company/credentials/3732") // jti
        .withSubject("did:example:ebfeb1f712ebc6f1c276e12ec21") // sub
        .withClaim(
            "vc", mapOf(
                "@context" to context,
                "id" to "http://event.company/credentials/3732",
                "type" to type,
                "issuer" to "https://event.company/issuers/565049",
                "validFrom" to "2010-01-01T00:00:00Z",
                "credentialSubject" to mapOf(
                    "url" to "https://example.com",
                    "bool_value" to 1,
                    "comment" to "test comment"
                )
            )
        )
        .sign(algorithm)
}

val json = """
{
  "credential_issuer": "https://datasign-demo-vci.tunnelto.dev",
  "authorization_servers": [
    "https://datasign-demo-vci.tunnelto.dev"
  ],
  "credential_endpoint": "https://datasign-demo-vci.tunnelto.dev/credentials",
  "batch_credential_endpoint": "https://datasign-demo-vci.tunnelto.dev/batch-credentials",
  "deferred_credential_endpoint": "https://datasign-demo-vci.tunnelto.dev/deferred_credential",
  "display": [
    {
      "name": "DataSign Inc.",
      "locale": "en-US",
      "logo": {
        "url": "https://datasign.jp/public/logo.png",
        "alt_text": "a square logo of a company"
      },
      "background_color": "#12107c",
      "text_color": "#FFFFFF"
    },
    {
      "name": "株式会社DataSign",
      "locale": "ja-JP",
      "logo": {
        "url": "https://datasign.jp/public/logo.png",
        "alt_text": "a square logo of a company"
      },
      "background_color": "#12107c",
      "text_color": "#FFFFFF"
    }
  ],
  "credentials_supported": {
    "OrganizationalAffiliationCertificate": {
      "format": "vc+sd-jwt",
      "scope": "OrganizationalAffiliationCertificate",
      "cryptographic_binding_methods_supported": [
        "did"
      ],
      "cryptographic_suites_supported": [
        "ES256K"
      ],
      "credential_definition": {
        "vct": "OrganizationalAffiliationCertificate",
        "claims": {
          "given_name": {
            "display": [
              { "name": "Given Name", "locale": "en-US" },
              { "name": "名", "locale": "ja-JP" }
            ]
          },
          "family_name": {
            "display": [
              { "name": "Family Name", "locale": "en-US" },
              { "name": "姓", "locale": "ja-JP" }
            ]
          },
          "gender": {
            "display": [
              { "name": "Gender", "locale": "en-US" },
              { "name": "性別", "locale": "ja-JP" }
            ]
          },
          "division": {
            "display": [
              { "name": "Division", "locale": "en-US" },
              { "name": "部署", "locale": "ja-JP" }
            ]
          }
        }
      },
      "display": [
        {
          "name": "Organizational Affiliation Credential",
          "locale": "en-US",
          "logo": {
            "url": "https://datasign.jp/id/logo.png",
            "alt_text": "a square logo of a employee identification"
          },
          "background_color": "#12107c",
          "text_color": "#FFFFFF"
        },
        {
          "name": "組織所属証明書",
          "locale": "ja",
          "logo": {
            "url": "https://datasign.jp/id/logo.png",
            "alt_text": "a square logo of a employee identification"
          },
          "background_color": "#12107c",
          "text_color": "#FFFFFF"
        }
      ]
    }
  }
}
""".trimIndent()