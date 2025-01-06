package com.ownd_project.tw2023_wallet_android.test

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.authlete.sd.Disclosure
import com.authlete.sd.SDObjectBuilder
import com.google.protobuf.Timestamp
import com.ownd_project.tw2023_wallet_android.datastore.Claim
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistoryStore
import com.ownd_project.tw2023_wallet_android.utils.KeyPairUtil
import com.ownd_project.tw2023_wallet_android.utils.generateEcKeyPair
import com.ownd_project.tw2023_wallet_android.utils.generateRsaKeyPair
import com.ownd_project.tw2023_wallet_android.utils.publicKeyToJwk
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.time.Instant
import java.util.UUID


object DummyData {
    private fun generateJwtVcMetaData(): String {
        return """
{
  "credential_issuer": "https://datasign-demo-vci.tunnelto.dev",
  "authorization_servers": ["https://datasign-demo-vci.tunnelto.dev"],
  "credential_endpoint": "https://datasign-demo-vci.tunnelto.dev/credentials",
  "display": [
    {
      "name": "一般社団法人Privacy by Design Lab",
      "locale": "ja_JP",
      "logo": {
        "uri": "https://datasign-demo-vci.tunnelto.dev/public/privacy-design-lab-logo.png",
        "alt_text": "一般社団法人Privacy by Design Labのロゴ"
      },
      "background_color": "#12107c",
      "text_color": "#FFFFFF"
    },
    {
      "name": "Privacy by Design Lab",
      "locale": "en-US",
      "logo": {
        "uri": "https://datasign-demo-vci.tunnelto.dev/public/privacy-design-lab-logo.png",
        "alt_text": "a square logo of a Privacy by Design Lab"
      },
      "background_color": "#12107c",
      "text_color": "#FFFFFF"
    }
  ],
  "credential_configurations_supported": {
    "ParticipationCertificate": {
      "format": "jwt_vc_json",
      "scope": "ProofOfParticipation",
      "cryptographic_binding_methods_supported": [
        "jwk"
      ],
      "credential_signing_alg_values_supported": [
        "ES256K"
      ],
      "proof_types_supported": {
        "jwt": {
          "proof_signing_alg_values_supported": [
            "ES256",
            "ES256K"
          ]
        }
      },
      "display": [
        {
          "name": "イベント参加証",
          "locale": "ja",
          "logo": {},
          "background_color": "#ffe4e1",
          "text_color": "#333333"
        },
        {
          "name": "Event Participation Certificate",
          "locale": "en-US",
          "logo": {},
          "background_color": "#ffe4e1",
          "text_color": "#333333"
        }
      ],
      "credential_definition": {
        "type": ["ParticipationCertificate"],
        "credentialSubject": {
          "name": {
            "display": [
              {
                "name": "イベント名",
                "locale": "ja-JP"
              },
              {
                "name": "Event Name",
                "locale": "en-US"
              }
            ]
          },
          "description": {
            "display": [
              {
                "name": "イベント説明",
                "locale": "ja-JP"
              },
              {
                "name": "Description",
                "locale": "en-US"
              }
            ]
          },
          "location": {
            "display": [
              {
                "name": "場所",
                "locale": "ja-JP"
              },
              {
                "name": "Location",
                "locale": "en-US"
              }
            ]
          },
          "startDate": {
            "display": [
              {
                "name": "開始日時",
                "locale": "ja-JP"
              },
              {
                "name": "Start Date",
                "locale": "en-US"
              }
            ]
          },
          "endDate": {
            "display": [
              {
                "name": "終了日時",
                "locale": "ja-JP"
              },
              {
                "name": "End Date",
                "locale": "en-US"
              }
            ]
          }
        }
      }
    }
  }
}
        """.trimIndent()
    }

    fun generateSdJwtMetaData(): String {
        return """
{
  "credential_issuer": "https://datasign-demo-vci.tunnelto.dev",
  "authorization_servers": ["https://datasign-demo-vci.tunnelto.dev"],
  "credential_endpoint": "https://datasign-demo-vci.tunnelto.dev/credentials",
  "display": [
    {
      "name": "オウンドプロジェクト",
      "locale": "ja_JP",
      "logo": {
        "uri": "https://datasign-demo-vci.tunnelto.dev/public/ownd-project-logo.png",
        "alt_text": "オウンドプロジェクトのロゴ"
      },
      "background_color": "#12107c",
      "text_color": "#FFFFFF"
    },
    {
      "name": "OWND Project4",
      "locale": "en-US",
      "logo": {
        "uri": "https://datasign-demo-vci.tunnelto.dev/public/ownd-project-logo.png",
        "alt_text": "a square logo of a OWND Project"
      },
      "background_color": "#12107c",
      "text_color": "#FFFFFF"
    }
  ],
  "credential_configurations_supported": {
    "IdentityCredential": {
      "format": "vc+sd-jwt",
      "scope": "IdentityIdentification",
      "cryptographic_binding_methods_supported": [
        "jwk"
      ],
      "credential_signing_alg_values_supported": [
        "ES256K"
      ],
            "proof_types_supported": {
              "jwt": {
                "proof_signing_alg_values_supported": [
                  "ES256",
                  "ES256K"
                ]
              }
            },
      "display": [
        {
          "name": "アイデンティティクレデンシャル",
          "locale": "ja",
          "logo": {},
          "background_color": "#12107c",
          "background_image": {"uri": "http://10.0.2.2:3002/images/my-number-card-image.png"},
          "text_color": "#696969"
        },
        {
          "name": "Identity Credential",
          "locale": "en-US",
          "logo": {},
          "background_color": "#12107c",
          "background_image": {"uri": "http://10.0.2.2:3002/images/my-number-card-image.png"},
          "text_color": "#696969"
        }
      ],
        vct": "IdentityCredential",
        "claims": {
          "first_name": {
            "display": [
              {
                "name": "名",
                "locale": "ja-JP"
              },
              {
                "name": "First Name",
                "locale": "en-US"
              }
            ]
          },
          "last_name": {
            "display": [
              {
                "name": "姓",
                "locale": "ja-JP"
              },
              {
                "name": "Last Name",
                "locale": "en-US"
              }
            ]
          },
          "previous_name": {
            "display": [
              {
                "name": "旧姓",
                "locale": "ja-JP"
              },
              {
                "name": "Previous Name",
                "locale": "en-US"
              }
            ]
          },
          "year": {
            "display": [
              {
                "name": "生年月日（年）",
                "locale": "ja-JP"
              },
              {
                "name": "Year of birth",
                "locale": "en-US"
              }
            ]
          },
          "month": {
            "display": [
              {
                "name": "生年月日（月）",
                "locale": "ja-JP"
              },
              {
                "name": "Month of birth",
                "locale": "en-US"
              }
            ]
          },
          "date": {
            "display": [
              {
                "name": "生年月日（日）",
                "locale": "ja-JP"
              },
              {
                "name": "Day of birth",
                "locale": "en-US"
              }
            ]
          },
          "is_older_than_13": {
            "display": [
              {
                "name": "13歳以上",
                "locale": "ja-JP"
              },
              {
                "name": "Is older than 13",
                "locale": "en-US"
              }
            ]
          },
          "is_older_than_18": {
            "display": [
              {
                "name": "18歳以上",
                "locale": "ja-JP"
              },
              {
                "name": "Is older than 18",
                "locale": "en-US"
              }
            ]
          },
          "is_older_than_20": {
            "display": [
              {
                "name": "20歳以上",
                "locale": "ja-JP"
              },
              {
                "name": "Is older than 20",
                "locale": "en-US"
              }
            ]
          },
          "gender": {
            "display": [
              {
                "name": "性別情報",
                "locale": "ja-JP"
              },
              {
                "name": "Gender",
                "locale": "en-US"
              }
            ]
          },
          "prefecture": {
            "display": [
              {
                "name": "都道府県",
                "locale": "ja-JP"
              },
              {
                "name": "Prefecture",
                "locale": "en-US"
              }
            ]
          },
          "city": {
            "display": [
              {
                "name": "市区町村",
                "locale": "ja-JP"
              },
              {
                "name": "City",
                "locale": "en-US"
              }
            ]
          },
          "address": {
            "display": [
              {
                "name": "住所",
                "locale": "ja-JP"
              },
              {
                "name": "Address",
                "locale": "en-US"
              }
            ]
          },
          "sub_char_common_name": {
            "display": [
              {
                "name": "代替文字使用位置(姓名)",
                "locale": "ja-JP"
              },
              {
                "name": "Alternative character usage position(last first name)",
                "locale": "en-US"
              }
            ]
          },
          "sub_char_previous_name": {
            "display": [
              {
                "name": "代替文字使用位置(旧姓)",
                "locale": "ja-JP"
              },
              {
                "name": "Alternative character usage position(previous name)",
                "locale": "en-US"
              }
            ]
          },
          "sub_char_address": {
            "display": [
              {
                "name": "代替文字使用位置(居住地)",
                "locale": "ja-JP"
              },
              {
                "name": "Alternative character usage position(address)",
                "locale": "en-US"
              }
            ]
          },
          "verified_at": {
            "display": [
              {
                "name": "公的個人認証実施日時",
                "locale": "ja-JP"
              },
              {
                "name": "Verified At",
                "locale": "en-US"
              }
            ]
          }
        }
    }
  }
}
        """.trimIndent()
    }

    private fun generateVCJwtCredential(): String {
        val keyPair = generateRsaKeyPair()
        val algorithm = when (keyPair.private) {
            is RSAPrivateKey -> Algorithm.RSA256(null, keyPair.private as RSAPrivateKey)
            is ECPrivateKey -> Algorithm.ECDSA256(null, keyPair.private as ECPrivateKey)
            else -> throw IllegalArgumentException("未サポートの秘密鍵のタイプです。")
        }

        val x5uMap = mapOf(
            "x5u" to "https://event.company/issuers"
        )

        val x5cMap = mapOf(
            "x5c" to listOf(
                "MIIC1jCCAb6gAwIBAgIUWwvaSTr281Wz5GL+B3l+VN5mXZowDQYJKoZIhvcNAQELBQAwJTEjMCEGA1UEAwwaVGVzdCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwHhcNMjMxMTA5MDE1NTMzWhcNMjQxMTA4MDE1NTMzWjBqMQswCQYDVQQGEwJKUDESMBAGA1UECAwJ5p2x5Lqs6YO9MRIwEAYDVQQHDAnmlrDlrr/ljLoxHTAbBgNVBAoMFOagquW8j+S8muekvkRhdGFTaWduMRQwEgYDVQQDDAtkYXRhc2lnbi5qcDBWMBAGByqGSM49AgEGBSuBBAAKA0IABM25nwomPvVuvGs8ggeU6vu32d++B7yby1b5GBTnG+hRqwXg/LYLX4FWsCHmeqGg1Ug050HNLs9YPj2GZTJkYQKjgYYwgYMwFgYDVR0RBA8wDYILZGF0YXNpZ24uanAwHQYDVR0OBBYEFEUSeJM8KSqx53G4eU1B4/1njAHYMEoGA1UdIwRDMEGhKaQnMCUxIzAhBgNVBAMMGlRlc3QgQ2VydGlmaWNhdGUgQXV0aG9yaXR5ghQtwA/xs2lqo1SEBWNXmmeEhbjuqzANBgkqhkiG9w0BAQsFAAOCAQEAHdR3uutoC+RQ750McLz9eFtzEruYkGU0aCnCMzpMJ3HMW63pOKFVVhpNxirz+pm/FpDwAcLT1jgKvdbH4cai8oTfd84GuEldxOyNYVrIybkJOJla1tZloW6WjGfKVY8YAaKwHVQBcwa/std18j3g7CA/h9V4wKUtPYLKNobAOk/CSD2BCHSdt49MRdkgyigjxh654qk/DIsrKz6VUR7/UPvuGuwPtZhhIs/89OoNZ2yvMKCffMGHLL9TKeGGVVf9ozVxV/lNbneXmGD2kvZ1zFbRwaYCmw4DcIAYLij29nahboY80hdt86HZe42esQSBDBzaTyA4EvXH5l5AUtSHmg==",
                "MIIC0TCCAbkCFC3AD/GzaWqjVIQFY1eaZ4SFuO6rMA0GCSqGSIb3DQEBCwUAMCUxIzAhBgNVBAMMGlRlc3QgQ2VydGlmaWNhdGUgQXV0aG9yaXR5MB4XDTIzMTEwOTAxNDk0NVoXDTI0MTEwODAxNDk0NVowJTEjMCEGA1UEAwwaVGVzdCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDCj2g1m7YQZb1LMOlMy2zrOCg9cAEzrK7rctymFFd9r77JMOi1c3nzIm6ZWemwSNxGY2yUSB+CNHJDQ+W9vO2M/9FFvuKxMfVCDDEBV1w9rkNdjIGcvhLA6VjhxoAN0X4VRm8pzW7KKsr9PMr2HZVbqorLTnTkC5aHhoqVcLe/OFnm4NzU02B9xecaaoqajPAXHltFtD+DVKE6mQuRtD8KOIRhPfH9UuorOYV2emLKw1b7MFM5O8IETcKD2tazcHRQbFio/6VSYXikBzHI9bttfd2qmmTmTOLIhFsgTbnZqlGc9mYb3HA2mSynXg0/NzdO4MsF/bhFmwSvCxBPCYmRAgMBAAEwDQYJKoZIhvcNAQELBQADggEBALzHBevOxbVXHD8iHdhjLqbUDiZkOO84UNIFOIceq4J685qxJaJZ65SJV6f3gwbRa3BCeJsLGSktcKJ5kcN8S3sFABmMoYSBn/KLG2672oOCmdaZV/notuhx2pwqrTtN/5Zw14UlA78bH8suCzHKpdSbGR7rZQmXyAkIl2VUGOFCklWKylsXFYcoT/jwggTlf26zwbzlkjIFFY+sXr0n8gCal9o40eHaNWAskNdA2Cviqx5FIlto7/OK0lrKiJxkNI8EcBBQRe0mDnzj6QXxYLOqihC1owQukUcNFHdya4lvVX1f1hx2RDLYG2qgvZePzf6GyRH0mUS9asp6cBR8Asc="
            )
        )
        val type = listOf<String>("ParticipationCertificate")
//        val type = listOf<String>("VerifiableCredential", "ParticipationCertificate")
        val context = listOf<String>(
            "https://www.w3.org/ns/credentials/v2", "https://www.w3.org/ns/credentials/examples/v2"
        )
        return JWT.create().withIssuer("https://event.company/issuers/565049") // iss
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
                        "id" to "did:example:ebfeb1f712ebc6f1c276e12ec21",
                        "name" to "Sample Business Event",
                        "location" to "Shinjuku Tokyo",
                        "organizer" to "Sample Event Company"
                    )
                )
            )
            .withHeader(x5uMap)
//            .withHeader(x5cMap)
            .sign(algorithm)
    }

    suspend fun generateJwtVcCredentialData(credentialDataStore: CredentialDataStore): com.ownd_project.tw2023_wallet_android.datastore.CredentialData? {
        val data = com.ownd_project.tw2023_wallet_android.datastore.CredentialData.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setFormat("jwt_vc_json")
            .setCredential(generateVCJwtCredential())
            .setCNonce("test_CNonce")
            .setCNonceExpiresIn(86400)
            .setIss("https://event.company/issuers/565049")
            .setIat(86400L)
            .setExp(86400L)
            .setType("EventParticipationCredential")
            .setAccessToken("test_accessToken")
            .setCredentialIssuerMetadata(generateJwtVcMetaData())
            .build()
        credentialDataStore.saveCredentialData(data)
        return data
    }


    suspend fun resetDataStore(
        credentialDataStore: CredentialDataStore,
        historyStore: CredentialSharingHistoryStore,
    ) {
        credentialDataStore.deleteAllCredentials()
        historyStore.deleteAllCredentials()
    }

    suspend fun generateSdJwtCredentialData(credentialDataStore: CredentialDataStore): com.ownd_project.tw2023_wallet_android.datastore.CredentialData? {
        val disclosure1 = Disclosure("given_name", "value1")
        val disclosure2 = Disclosure("family_name", "value2")
        val sdJwt = generateSdJwt(listOf<Disclosure>(disclosure1, disclosure2))

        val data = com.ownd_project.tw2023_wallet_android.datastore.CredentialData.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setFormat("vc+sd-jwt")
            .setCredential(sdJwt)
            .setCNonce("test_CNonce")
            .setCNonceExpiresIn(86400)
            .setIss("https://event.company/issuers/565049")
            .setIat(86400L)
            .setExp(86400L)
            .setType("EventParticipationCredential")
            .setAccessToken("test_accessToken")
            .setCredentialIssuerMetadata(generateSdJwtMetaData())
            .build()
        credentialDataStore.saveCredentialData(data)
        return data
    }


    suspend fun generateHistoryData(historyStore: CredentialSharingHistoryStore): com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory? {
        val claim1 = Claim.newBuilder()
            .setName("name")
            .setValue("john smith")
            .setPurpose("purpose of sharing name")
            .build()
        val claim2 = Claim.newBuilder()
            .setName("location")
            .setValue("tokyo")
            .setPurpose("purpose of sharing address")
            .build()

        val history =
            com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory.newBuilder()
                .setRp("someRp")
                .setAccountIndex(1)
                .setCreatedAt(
                    Timestamp.newBuilder().setSeconds(Instant.now().epochSecond)
                        .setNanos(Instant.now().nano).build()
                )
                .setCredentialID("testCredentialID")
                .addAllClaims(listOf(claim1, claim2)).build()
        historyStore.save(history)
        return history
    }

    fun generateSdJwt(disclosures: List<Disclosure>): String {
//        val keyAliasTestIssuer = "test_issuer_1"
        val keyAliasKeyBinding = "bindingKey.test"
//        if (!KeyPairUtil.isKeyPairExist(keyAliasTestIssuer)) {
//            KeyPairUtil.generateSignVerifyKeyPair(keyAliasTestIssuer)
//        }
        if (!KeyPairUtil.isKeyPairExist(keyAliasKeyBinding)) {
            KeyPairUtil.generateSignVerifyKeyPair(keyAliasKeyBinding)
        }
        val keyPairTestIssuer = generateEcKeyPair()
        val keyPairKeyBinding = KeyPairUtil.getKeyPair(keyAliasKeyBinding)

        val algorithm =
            Algorithm.ECDSA256(
                keyPairTestIssuer.public as ECPublicKey,
                keyPairTestIssuer.private as ECPrivateKey?
            )
        val builder = SDObjectBuilder()
        disclosures.forEach { it ->
            builder.putSDClaim(it)
        }
        val claims = builder.build()
        val jwk = publicKeyToJwk(keyPairKeyBinding!!.public)
        val cnf = mapOf("jwk" to jwk)
        val issuerSignedJwt = JWT.create().withIssuer("https://client.example.org/cb")
            .withAudience("https://server.example.com")
            .withClaim("cnf", cnf)
            .withClaim("vct", "IdentityCredential")
            .withClaim("_sd", (claims["_sd"] as List<*>))
            .sign(algorithm)
        return "$issuerSignedJwt~${disclosures.joinToString("~") { it.disclosure }}~"
    }
}
