package com.ownd_project.tw2023_wallet_android.oid

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.signature.JWT
import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil
import java.security.MessageDigest
import java.util.Base64

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VpJwtPayload(
    val iss: String?,
    val jti: String?,
    val aud: String?,
    val nbf: Long?,
    val iat: Long?,
    val exp: Long?,
    val nonce: String?,
    val vp: Map<String, Any>
)
// https://www.rfc-editor.org/rfc/rfc7515.html
data class HeaderOptions(
    val alg: String = "ES256",
    val typ: String = "JWT",
    val jwk: String? = null
)

data class JwtVpJsonPayloadOptions(
    var iss: String? = null,
    var jti: String? = null,
    var aud: String,
    var nbf: Long? = null,
    var iat: Long? = null,
    var exp: Long? = null,
    var nonce: String
)

object SdJwtVcPresentation {
    fun genKeyBindingJwtParts(
        sdJwt: String,
        selectedDisclosures: List<SDJwtUtil.Disclosure>,
        aud: String,
        nonce: String,
        iat: Long? = null
    ): Pair<Map<String, Any>, Map<String, Any>> {
        val header = mapOf("typ" to "kb+jwt", "alg" to "ES256")

        val parts = sdJwt.split('~')
        val issuerSignedJwt = parts[0]
        // It MUST be taken over the US-ASCII bytes preceding the KB-JWT in the Presentation
        val sd =
            issuerSignedJwt + "~" + selectedDisclosures.joinToString("~") { it.disclosure } + "~"
        // The bytes of the digest MUST then be base64url-encoded.
        val sdHash = sd.toByteArray(Charsets.US_ASCII).sha256ToBase64Url()

        val _iat = iat ?: (System.currentTimeMillis() / 1000)
        val payload = mapOf(
            "aud" to aud,
            "iat" to _iat,
            "_sd_hash" to sdHash,
            "nonce" to nonce
        )
        return Pair(header, payload)
    }

    private fun ByteArray.sha256ToBase64Url(): String {
        val sha = MessageDigest.getInstance("SHA-256").digest(this)
        return Base64.getUrlEncoder().encodeToString(sha).trimEnd('=')
    }

    fun createPresentation(
        credential: SubmissionCredential,
        selectedDisclosures: List<SDJwtUtil.Disclosure>,
        authRequest: RequestObjectPayload,
        keyBinding: KeyBinding
    ): Triple<String, DescriptorMap, List<DisclosedClaim>> {
        val sdJwt = credential.credential
        val keyBindingJwt = keyBinding.generateJwt(
            sdJwt,
            selectedDisclosures,
            authRequest.clientId!!,
            authRequest.nonce!!,
        )
        // 絞ったdisclosureでチルダ連結してsd-jwtを構成
        val parts = sdJwt.split('~')
        val issuerSignedJwt = parts[0]
        val vpToken =
            issuerSignedJwt + "~" + selectedDisclosures.joinToString("~") { it.disclosure } + "~" + keyBindingJwt

        val dm = DescriptorMap(
            id = credential.inputDescriptor.id,
            format = credential.format,
            path = "$"
        )
        val disclosedClaims =
            selectedDisclosures.map { DisclosedClaim(credential.id, credential.types, it.key!!) }
        return Triple(vpToken, dm, disclosedClaims)
    }
}

object JwtVpJsonPresentation {
    private fun genDescriptorMap(
        inputDescriptorId: String,
        pathIndex: Int? = -1,
        pathNestedIndex: Int? = 0
    ): DescriptorMap {

        /*
        a non-normative example of the content of the presentation_submission parameter:
        ```
            {
              "definition_id": "example_jwt_vc",
              "id": "example_jwt_vc_presentation_submission",
              "descriptor_map": [
                {
                  "id": "id_credential",
                  "path": "$",
                  "format": "jwt_vp_json",
                  "path_nested": {
                    "path": "$.vp.verifiableCredential[0]",
                    "format": "jwt_vc_json"
                  }
                }
              ]
            }
        ```
         */
        return DescriptorMap(
            id = inputDescriptorId,
            path = if (pathIndex == -1) "$" else "$[${pathIndex}]",
            format = "jwt_vp_json",
            pathNested = Path(
                format = "jwt_vc_json",
                path = "$.vp.verifiableCredential[${pathNestedIndex}]"
            )
        )
    }

    fun genVpJwtPayload(vcJwt: String, payloadOptions: JwtVpJsonPayloadOptions): VpJwtPayload {
        val vpClaims = mapOf(
            "@context" to listOf("https://www.w3.org/2018/credentials/v1"),
            "type" to listOf("VerifiablePresentation"),
            "verifiableCredential" to listOf(vcJwt)
        )

        val currentTimeSeconds = System.currentTimeMillis() / 1000
        return VpJwtPayload(
            iss = payloadOptions.iss,
            jti = payloadOptions.jti,
            aud = payloadOptions.aud,
            nbf = payloadOptions.nbf ?: currentTimeSeconds,
            iat = payloadOptions.iat ?: currentTimeSeconds,
            exp = payloadOptions.exp ?: (currentTimeSeconds + 2 * 3600),
            nonce = payloadOptions.nonce,
            vp = vpClaims
        )
    }

    fun createPresentation(
        credential: SubmissionCredential,
        authRequest: RequestObjectPayload,
        jwtVpJsonGenerator: JwtVpJsonGenerator
    ): Triple<String, DescriptorMap, List<DisclosedClaim>> {
        val objectMapper = jacksonObjectMapper()
        val (_, payload, _) = JWT.decodeJwt(jwt = credential.credential)
        val disclosedClaims = payload.mapNotNull { (key, value) ->
            if (key == "vc") {
                val vcMap = objectMapper.readValue(value as String, Map::class.java)
                vcMap.mapNotNull { (vcKey, vcValue) ->
                    if (vcKey == "credentialSubject") {
                        (vcValue as Map<String, Any>).mapNotNull { (subKey, subValue) ->
                            DisclosedClaim(
                                id = credential.id,
                                types = credential.types,
                                name = subKey as String
                            )
                        }
                    } else {
                        null
                    }
                }.flatten()
            } else {
                null
            }
        }.flatten()
        val vpToken = jwtVpJsonGenerator.generateJwt(
            credential.credential,
            HeaderOptions(),
            JwtVpJsonPayloadOptions(
                aud = authRequest.clientId!!,
                nonce = authRequest.nonce!!
            )
        )

        return Triple(
            first = vpToken,
            second = genDescriptorMap(credential.inputDescriptor.id),
            third = disclosedClaims
        )
    }
}

// https://openid.net/specs/openid-4-verifiable-presentations-1_0-20.html#name-presentation-response
interface JwtVpJsonGenerator {
    /*
    a non-normative example of the payload of the Verifiable Presentation in the vp_token parameter
    ```
    {
        "iss": "did:example:ebfeb1f712ebc6f1c276e12ec21",
        "jti": "urn:uuid:3978344f-8596-4c3a-a978-8fcaba3903c5",
        "aud": "https://client.example.org/cb",
        "nbf": 1541493724,
        "iat": 1541493724,
        "exp": 1573029723,
        "nonce": "n-0S6_WzA2Mj",
        "vp": {
            "@context": [
                "https://www.w3.org/2018/credentials/v1"
            ],
            "type": [
                "VerifiablePresentation"
            ],
            "verifiableCredential": [
                "eyJhb...ssw5c"
            ]
        }
    }
    ```
    Note: The VP's nonce claim contains the value of the nonce of the presentation request and the aud claim contains the Client Identifier of the Verifier.
    This allows the Verifier to detect replay of a Presentation as recommended in Section 12.1.
     */
    fun generateJwt(
        vcJwt: String,
        headerOptions: HeaderOptions,
        payloadOptions: JwtVpJsonPayloadOptions
    ): String

    fun getJwk(): Map<String, String>
}