package com.ownd_project.tw2023_wallet_android.features.data_sharing

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.authlete.sd.Disclosure
import com.authlete.sd.SDObjectBuilder
import com.ownd_project.tw2023_wallet_android.model.CertificateInfo
import com.ownd_project.tw2023_wallet_android.model.ClientInfo
import com.ownd_project.tw2023_wallet_android.oid.Field
import com.ownd_project.tw2023_wallet_android.oid.InputDescriptor
import com.ownd_project.tw2023_wallet_android.oid.InputDescriptorConstraints
import com.ownd_project.tw2023_wallet_android.ui.siop_vp.request_content.RequestInfo
import com.ownd_project.tw2023_wallet_android.utils.generateEcKeyPair
import com.ownd_project.tw2023_wallet_android.utils.generateRsaKeyPair
import com.ownd_project.tw2023_wallet_android.utils.publicKeyToJwk
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey

data class SdJwtOptions (
    var issuer: String? = null,
    var audience: String? = null
)

fun generateSdJwtCredential(vct: String, disclosures: List<Disclosure>, options: SdJwtOptions? = null): String {
    val ecKeyPair = generateEcKeyPair()
    val algorithm =
        Algorithm.ECDSA256(ecKeyPair.public as ECPublicKey, ecKeyPair.private as ECPrivateKey?)
    val builder = SDObjectBuilder()
    disclosures.forEach { it ->
        builder.putSDClaim(it)
    }
    val claims = builder.build()

    val iss = options?.let { it.issuer ?: "https://client.example.org/cb" }
    val aud = options?.let { it.audience ?: "https://server.example.com" }
    val jwk = publicKeyToJwk(ecKeyPair.public)
    val cnf = mapOf("jwk" to jwk)
    val issuerSignedJwt = JWT.create().withIssuer(iss)
        .withAudience(aud)
        .withClaim("cnf", cnf)
        .withClaim("vct", vct)
        .withClaim("_sd", (claims["_sd"] as List<*>))
        .sign(algorithm)
    return "$issuerSignedJwt~${disclosures.joinToString("~") { it.disclosure }}"
}

fun generateVCJwtCredential(): String {
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

val inputDescriptor1: InputDescriptor = InputDescriptor(
    id = "",
    constraints = InputDescriptorConstraints(
        fields = listOf(
            Field(
                path = listOf("\$.vct"),
                optional = false,
                filter = mapOf("const" to "OrganizationalAffiliationCertificate"),
            ),
            Field(
                path = listOf("\$.organization_name"),
                optional = false,
                filter = null,
            ),
            Field(
                path = listOf("\$.family_name"),
                optional = false,
                filter = null,
            ),
            Field(
                path = listOf("\$.given_name"),
                optional = true,
                filter = null,
            ),
            Field(
                path = listOf("\$.portrait"),
                optional = true,
                filter = null,
            ),
        ),
        limitDisclosure = null,
    ),
    name = "test name",
    purpose = "test purpose",
    format = mapOf("vc+sd-jwt" to mapOf("alg" to listOf("ES256"))),
    group = listOf("A")
)

val issuerCertInfo = CertificateInfo(
    domain = "",
    organization = "Amazon",
    country = "US",
    state = "",
    locality = "",
    street = "",
    email = ""
)
val certInfo = CertificateInfo(
    domain = "boolcheck.com",
    organization = "datasign.inc",
    country = "JP",
    state = "Tokyo",
    locality = "Sinzyuku-ku",
    street = "",
    email = "by-dev@datasign.jp",
    issuer = issuerCertInfo
)
val clientInfo = ClientInfo(
    name = "Boolcheck",
    certificateInfo = certInfo,
    tosUrl = "https://datasign.jp/tos",
    policyUrl = "https://datasign.jp/policy"
)

val requestInfo = RequestInfo(
    title = "真偽情報に署名を行い、その情報をBoolcheckに送信します",
    boolValue = true,
    comment = "このXアカウントはXXX本人のものです",
    url = "https://example.com",
    clientInfo = clientInfo
)
