package com.ownd_project.tw2023_wallet_android.test

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.authlete.sd.Disclosure
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.TestFragmentActivityBinding
import com.ownd_project.tw2023_wallet_android.datastore.CredentialData
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.test.DummyData.generateSdJwt
import com.ownd_project.tw2023_wallet_android.ui.credential_detail.CredentialDetailFragment
import com.ownd_project.tw2023_wallet_android.ui.shared.Constants
import com.ownd_project.tw2023_wallet_android.ui.siop_vp.TokenSharingFragment
import com.ownd_project.tw2023_wallet_android.utils.KeyPairUtil
import com.ownd_project.tw2023_wallet_android.utils.ZipUtil
import kotlinx.coroutines.launch
import java.util.UUID

class TestFragmentActivity : AppCompatActivity() {
    private val credentialDataStore = CredentialDataStore.getInstance(this)
    private val x5uJwt =
        "eyJraWQiOiJodHRwOi8vdW5pdmVyc2l0eS5leGFtcGxlL2NyZWRlbnRpYWxzLzM3MzIiLCJ4NXUiOiJodHRwOi8vMTAuMC4yLjI6ODA4MC90ZXN0LWNlcnRpZmljYXRlIiwiYWxnIjoiRVMyNTYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL3VuaXZlcnNpdHkuZXhhbXBsZS9pc3N1ZXJzLzU2NTA0OSIsInN1YiI6ImRpZDpleGFtcGxlOmViZmViMWY3MTJlYmM2ZjFjMjc2ZTEyZWMyMSIsInZjIjp7IkBjb250ZXh0IjpbImh0dHBzOi8vd3d3LnczLm9yZy9ucy9jcmVkZW50aWFscy92MiIsImh0dHBzOi8vd3d3LnczLm9yZy9ucy9jcmVkZW50aWFscy9leGFtcGxlcy92MiJdLCJpZCI6Imh0dHA6Ly91bml2ZXJzaXR5LmV4YW1wbGUvY3JlZGVudGlhbHMvMzczMiIsInR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJFeGFtcGxlRGVncmVlQ3JlZGVudGlhbCJdLCJpc3N1ZXIiOiJodHRwczovL3VuaXZlcnNpdHkuZXhhbXBsZS9pc3N1ZXJzLzU2NTA0OSIsInZhbGlkRnJvbSI6IjIwMTAtMDEtMDFUMDA6MDA6MDBaIiwiY3JlZGVudGlhbFN1YmplY3QiOnsiaWQiOiJkaWQ6ZXhhbXBsZTplYmZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLCJuYW1lIjoiU2FtcGxlIEV2ZW50IEFCQyIsImRhdGUiOiIyMDI0LTAxLTI0VDAwOjAwOjAwWiJ9fSwiaWF0IjoxNzAyNTM0MDMwfQ.DoMHojQUGoixFV8bwdjCDIb9sm2QKOG-AhmpdG8I-pNhTTlos9pvJ6YchnoPylpZngvFCb_WQaSd9tmGiHN_Mg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isKeyPairExist = KeyPairUtil.isKeyPairExist(Constants.KEY_PAIR_ALIAS_FOR_KEY_BINDING)
        if (!isKeyPairExist) {
            KeyPairUtil.generateSignVerifyKeyPair(Constants.KEY_PAIR_ALIAS_FOR_KEY_BINDING)
        }
        supportActionBar?.apply {
            displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
            setCustomView(R.layout.custom_action_bar)
            show()
        }
        lifecycleScope.launch {
            createTestData()
        }

        val binding = TestFragmentActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        val navController = findNavController(R.id.nav_host_fragment_activity_test)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_test) as NavHostFragment
        val navController = navHostFragment.navController

        val args = Bundle().apply {
            val url =
                "xxx://?client_id=https%3A%2F%2Fwww.honda.co.jp&redirect_uri=https%3A%2F%2Fwww.honda.co.jp&request_uri=http%3A%2F%2F10.0.2.2%3A8080%2Fauth-request"
            // val url = "siopv2://?client_id=https%3A%2F%2Fwww.honda.co.jp&redirect_uri=https%3A%2F%2Fdatasign.jp&request=eyJraWQiOiJ0ZXN0LWtpZCIsImFsZyI6IlJTMjU2IiwidHlwIjoiSldUIn0.eyJpc3MiOiJodHRwczovL2NsaWVudC5leGFtcGxlLm9yZy9jYiIsImF1ZCI6Imh0dHBzOi8vc2VydmVyLmV4YW1wbGUuY29tIiwicmVzcG9uc2VfdHlwZSI6ImNvZGUgaWRfdG9rZW4iLCJjbGllbnRfaWQiOiJodHRwczovL2NsaWVudC5leGFtcGxlLm9yZy9jYiIsInJlZGlyZWN0X3VyaSI6Imh0dHBzOi8vY2xpZW50LmV4YW1wbGUub3JnL2NiIiwic2NvcGUiOiJvcGVuaWQiLCJzdGF0ZSI6ImFmMGlmanNsZGtqIiwibm9uY2UiOiJuLTBTNl9XekEyTWoiLCJtYXhfYWdlIjo4NjQwMCwiaWF0IjoxNzAwNDU2MTIwfQ.oQ2EGIC130J0ztO3mN9qpOsQIL6Wowh-2Xd0I-in2LNEybtab7tSNJP4mi58BtkLIVBZGp_BZxk2vSJkSvqTbjnzvaeO3O6mlonjZPQF0-1Af6yB8kHZar2PzggV1ct2RUppndpIFmlTKzSx1jy4diYTrWAAFKcQqlugyRAwlt-VkWBnylkBe6QaetoMCkPPwlz-XYIiJ1lRo8i4N0vt-DY_p89uHnP3R9KeiVzoNDqyNpdooU63DPlfwRSLKw2rYd8UjPxiB-tWKLuPlxz1vR82Lt0X5ofhdN3hUD93c5f15z_88Cj5uYPW9mBVWgueeK0TvzePq40UYUnbaw_z6w&client_metadata_uri=http%3A%2F%2F10.0.2.2%3A8080%2Fclient-metadata-uri"
            putString("siopRequest", url)
            putInt("index", -1) // 一つ前の画面でアカウントを選択した場合のインデックス
        }
//        navController.navigate(R.id.id_token_sharring, args)
        val scanned =
            "H4sIAAAAAAAAAKVTy46jRhT9F9bdEYa208yONgYXQ+Ex5mEqjiwe7qHKVYAa2xhG/QkjZZf8QDTb2ed38viNXOyk00kUKVEWV1CPe+4995z6ID1WTyI5SG8k1h62p2zLmqqUbqTsaZfvygNNOBztOvspiZZ0Qe0qn3vtgt6f8mhc5yLsMoXLu9WY7yzzkFln7ihuRyKPp6VXx9G5d3qs4h5RZ2rfuevgTxjY1494etc5DE0Whn6Hp5pM1q7sRC7PIJ8IzuK1xxFtKWCViFXUC3Hn+vGAJ+/W+oD3Nhwt6ePyC2izzlT8UiLrq5OjhsdkTQDOrfP5/kjWRZGuHxqy0uCuOyJrG1oMFNfX5cUKNah0RzFFEySgvFG/0FqIkBIIHMUq9m0eC6wQZjLMMoX4M6CMO3zJJwyx+ku0f2CpMgY6hQzrFIlCzucP/WV0aq46ZdY7QutIpx2zTmOZCPckGstJZDawVjAFrP+W89LrNd/OYUQ1mQ5cBhx94nTaKBVcGTgna2/siPAujkZtagWnWLU5scJjbvEineMT7rP+0kPpjTMLZOMNDUHuJCIgq92Q4LXM7SCH+Xt9zwpL6I0vX2NOr/1cZ47+p0akSC2+90r7lK6AH0MtWOmAjRmEGWBDn1zjIblY53UfJugrah6rYOeyob/Zeg/fyUtdH84F4TEzhdsP2trMFfE57pHqiuCMRQi6zwbOR5gfH2wZKFfuaBYqgyZoZk6X3aChV+TD/CjqsIFkx9fPjo/k0NDbBbtGRG3tcdVCLyZ4pTq7vQ4WxzI2cAu2Nio8r9gysCp6NsP7tM3Z1ECp1ghl+XZh3eqFqHPrHt3WbuH7vGq0+mRP4qwoq3cdr0n5/mRO0220TFa5dhAWnbtb/B7eeE6bmicdPPAPG6lMxG4jvfnqj9+N5F5+bjYSr7KEX/d25W2w2kjPN68v/vTNx18+ff7rVZZs7Xdw9WvYzpPD3+CNy96/gP/52+9//OG7f4R/lp5/BRca96TIBAAA"
        val decompressed = try {
            ZipUtil.decompressString(scanned)
        } catch (e: Exception) {
            null
        }
        if (!decompressed.isNullOrEmpty()) {
            val mapper = jacksonObjectMapper()
            val typeRef = object : TypeReference<Map<String, Any>>() {}
            val deserialized = mapper.readValue(decompressed, typeRef)
            val format = deserialized["format"] as String
            val credential = deserialized["credential"] as String
            val display = deserialized["display"] as String
            val args = Bundle().apply {
                putString("format", format)
                putString("credential", credential)
                putString("display", display)
            }
            navController.navigate(R.id.credential_verification, args)
        }
        val args3 = Bundle().apply {
            putString("credentialId", "test_data_3")
        }
        // setContentView(R.layout.activity_test_fragment);
        // val fragment = IdTokenSharringFragment()
        val fragment = TokenSharingFragment().apply {
            arguments = args
        }
        val fragment3 = CredentialDetailFragment().apply {
            arguments = args3
        }
//         navController.navigate(R.id.credentialDetailFragment, args3)
//        val action = IdTokenSharringFragmentDirections.actionIdTokenSharringToNavigationCertificate()
//        navController.navigate(action)
        // val action = CertificateFragmentDirections.actionToCredentialDetail("")
//        supportFragmentManager.beginTransaction()
//            .add(R.id.fragment_container, fragment)
//            .commit();
    }

    suspend fun createTestData() {
        credentialDataStore.deleteAllCredentials()
        val credentialIssuerMetadataJson = json
        // 動作確認用ダミーデータ
        val disclosure1 = Disclosure("given_name", "value1")
        val disclosure2 = Disclosure("family_name", "value2")
        val sdJwt = generateSdJwt(listOf<Disclosure>(disclosure1, disclosure2))

        val vcData1 = com.ownd_project.tw2023_wallet_android.datastore.CredentialData.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setFormat("test_format")
            .setCredential("test_credential")
            .setCNonce("test_CNonce")
            .setCNonceExpiresIn(86400)
            .setIss("test_iss")
            .setIat(86400L)
            .setExp(86400L)
            .setType("test_type")
            .setAccessToken("test_accessToken")
            .setCredentialIssuerMetadata(credentialIssuerMetadataJson)
            .build()
        credentialDataStore?.saveCredentialData(vcData1)

        val vcData2 = com.ownd_project.tw2023_wallet_android.datastore.CredentialData.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setFormat("vc+sd-jwt")
            .setCredential(sdJwt)
            .setCNonce("test_CNonce")
            .setCNonceExpiresIn(86400)
            .setIss("test_iss")
            .setIat(86400L)
            .setExp(86400L)
            .setType("test_type")
            .setAccessToken("test_accessToken")
            .setCredentialIssuerMetadata(credentialIssuerMetadataJson)
            .build()
        credentialDataStore?.saveCredentialData(vcData2)

        val vcData3 = CredentialData.newBuilder()
            .setId("test_data_3")
            .setFormat("jwt_vc_json")
            .setCredential(x5uJwt)
            .setCNonce("test_CNonce")
            .setCNonceExpiresIn(86400)
            .setIss("test_iss")
            .setIat(86400L)
            .setExp(86400L)
            .setType("test_type")
            .setAccessToken("test_accessToken")
            .setCredentialIssuerMetadata(json2)
            .build()
        credentialDataStore?.saveCredentialData(vcData3)
    }
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
      "name": "OWND Project3",
      "locale": "en-US",
      "logo": {
        "uri": "https://exampleuniversity.com/public/logo.png",
        "alt_text": "a square logo of a university"
      },
      "background_color": "#12107c",
      "text_color": "#FFFFFF"
    },
    {
      "name": "オウンドプロジェクト",
      "locale": "ja_JP",
      "logo": {
        "uri": "https://exampleuniversity.com/public/logo.png",
        "alt_text": "a square logo of a university"
      },
      "background_color": "#12107c",
      "text_color": "#FFFFFF"
    }
  ],
  "credential_configurations_supported": {
    "UniversityDegreeCredential": {
      "format": "jwt_vc_json",
      "scope": "UniversityDegree",
      "cryptographic_binding_methods_supported": [
        "did"
      ],
      "credential_signing_alg_values_supported": [
        "ES256K"
      ],
      "credential_definition": {
        "type": [
          "IdentityCredential"
        ],
        "credentialSubject": {
          "given_name": {
            "display": [
              { "name": "Given Name", "locale": "en-US" },
              { "name": "名", "locale": "ja_JP" }
            ]
          },
          "last_name": {
            "display": [
              { "name": "Surname", "locale": "en-US" },
              { "name": "姓", "locale": "ja_JP" }
            ]
          },
          "degree": {},
          "gpa": {
            "display": [
              { "name": "GPA" }
            ]
          }
        }
      },
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
          "name": "IdentityCredential",
          "locale": "en-US",
          "logo": {
            "uri": "https://exampleuniversity.com/public/logo.png",
            "alt_text": "a square logo of a university"
          },
          "background_image": {"uri": "http://localhost:3002/images/my-number-card-image.png"},
          "text_color": "#FFFFFF"
        },
        {
          "name": "IdentityCredential",
          "locale": "ja_JP",
          "logo": {
            "uri": "https://exampleuniversity.com/public/logo.png",
            "alt_text": "a square logo of a university"
          },
          "background_image": {"uri": "http://localhost:3002/images/my-number-card-image.png"},
          "text_color": "#FFFFFF"
        }
      ]
    }, 
    "IdentityCredential": {
      "format": "vc+sd-jwt",
      "scope": "EmployeeIdentification",
      "cryptographic_binding_methods_supported": [
        "did"
      ],
      "credential_signing_alg_values_supported": [
        "ES256K"
      ],
        "vct": "IdentityCredential",
        "claims": {
          "employee_no": {
            "display": [
              {
                "name": "Employee No",
                "locale": "en-US"
              },
              {
                "name": "社員番号",
                "locale": "ja-JP"
              }
            ]
          },
          "given_name": {
            "display": [
              {
                "name": "Given Name",
                "locale": "en-US"
              },
              {
                "name": "名",
                "locale": "ja-JP"
              }
            ]
          },
          "family_name": {
            "display": [
              {
                "name": "Family Name",
                "locale": "en-US"
              },
              {
                "name": "姓",
                "locale": "ja-JP"
              }
            ]
          },
          "gender": {
            "display": [
              {
                "name": "Gender",
                "locale": "en-US"
              },
              {
                "name": "性別",
                "locale": "ja-JP"
              }
            ]
          },
          "division": {
            "display": [
              {
                "name": "Division",
                "locale": "en-US"
              },
              {
                "name": "部署",
                "locale": "ja-JP"
              }
            ]
          }
        }
      ,
      "display": [
        {
          "name": "Employee Credential",
          "locale": "en-US",
          "logo": {
            "uri": "https://datasign.jp/id/logo.png",
            "alt_text": "a square logo of a employee identification"
          },
          "background_color": "#12107c",
          "text_color": "#FFFFFF"
        },
        {
          "name": "社員証明書",
          "locale": "ja",
          "logo": {
            "uri": "https://datasign.jp/id/logo.png",
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

val json2 = """
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
      "name": "OWND Project5",
      "locale": "en-US",
      "logo": {
        "uri": "https://exampleuniversity.com/public/logo.png",
        "alt_text": "a square logo of a university"
      },
      "background_color": "#2521ff",
      "text_color": "#FFFFFF"
    },
    {
      "name": "オウンドプロジェクト",
      "locale": "ja_JP",
      "logo": {
        "uri": "https://exampleuniversity.com/public/logo.png",
        "alt_text": "a square logo of a university"
      },
      "background_color": "#12107c",
      "text_color": "#FFFFFF"
    }
  ],
  "credential_configurations_supported": {
    "ExampleDegreeCredential": {
      "format": "jwt_vc_json",
      "scope": "UniversityDegree",
      "cryptographic_binding_methods_supported": [
        "did"
      ],
      "credential_signing_alg_values_supported": [
        "ES256K"
      ],
      "credential_definition": {
        "type": [
          "VerifiableCredential",
          "ExampleDegreeCredential"
        ],
        "credentialSubject": {
          "name": {
            "display": [
              {
                "name": "Name",
                "locale": "en-US"
              },
              {
                "name": "名称",
                "locale": "ja_JP"
              }
            ]
          },
          "date": {
            "display": [
              {
                "name": "Date",
                "locale": "en-US"
              },
              {
                "name": "日付",
                "locale": "ja_JP"
              }
            ]
          }
        }
      },
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
          "name": "ExampleDegreeCredential",
          "locale": "en-US",
          "logo": {
            "uri": "https://exampleuniversity.com/public/logo.png",
            "alt_text": "a square logo of a university"
          },
          "background_color": "#12107c",
          "text_color": "#FFFFFF"
        },
        {
          "name": "ExampleDegreeCredential",
          "locale": "ja_JP",
          "logo": {
            "uri": "https://exampleuniversity.com/public/logo.png",
            "alt_text": "a square logo of a university"
          },
          "background_color": "#12107c",
          "text_color": "#FFFFFF"
        }
      ]
    }
  }
}
""".trimIndent()