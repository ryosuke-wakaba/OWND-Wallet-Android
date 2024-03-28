package com.ownd_project.tw2023_wallet_android.utils

import com.ownd_project.tw2023_wallet_android.signature.JWT
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import com.ownd_project.tw2023_wallet_android.vci.CredentialSupported
import com.ownd_project.tw2023_wallet_android.vci.CredentialSupportedJwtVcJson
import com.ownd_project.tw2023_wallet_android.vci.CredentialSupportedVcSdJwt
import com.ownd_project.tw2023_wallet_android.vci.Display
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object MetadataUtil {

    fun extractTypes(format: String, credential: String): List<String> {
        val types = if (format == "vc+sd-jwt") {
            val jwt = SDJwtUtil.divideSDJwt(credential).issuerSignedJwt
            val decoded = JWT.decodeJwt(jwt)
            val vct = decoded.second["vct"] as String
            listOf<String>(vct)
        } else if (format == "jwt_vc_json") {
            // todo ちょっとやっつけ実装なので後日改善する
            val decoded = JWT.decodeJwt(credential)
            val vc = decoded.second["vc"] as Map<*, *>
            vc["type"] as List<String>
        } else {
            emptyList()
        }
        return types
    }

    fun findMatchingCredentials(
        format: String,
        types: List<String>,
        metadata: CredentialIssuerMetadata
    ): CredentialSupported? {
        return metadata.credentialsSupported.entries.firstOrNull { (_, credentialSupported) ->
            when (credentialSupported) {
                is CredentialSupportedVcSdJwt -> {
                    // VcSdJwtの場合、vctとtypesの最初の要素を比較
                    format == "vc+sd-jwt" && types.firstOrNull() == credentialSupported.credentialDefinition.vct
                }

                is CredentialSupportedJwtVcJson -> {
                    // JwtVcJsonの場合、typesとcredentialDefinition.typeを両方ソートして比較
                    format == "jwt_vc_json" && containsAllElements(credentialSupported.credentialDefinition.type, types)
                }

                else -> false
            }
        }?.value
    }

    fun extractDisplayByClaim(credentialsSupported: CredentialSupported): MutableMap<String, List<Display>> {
        val displayMap = mutableMapOf<String, List<Display>>()
        when(credentialsSupported) {
            is CredentialSupportedJwtVcJson -> {
                val credentialSubject = credentialsSupported.credentialDefinition.credentialSubject
                credentialSubject?.map { (k, v) ->
                    v.display?.let { displayMap.put(k, it) }
                }
            }
            is CredentialSupportedVcSdJwt -> {
                val credentialSubject = credentialsSupported.credentialDefinition.claims
                credentialSubject?.map { (k, v) ->
                    v.display?.let { displayMap.put(k, it) }
                }
            }
            else -> {
                println("not implemented yet")
            }
        }
        return displayMap
    }

    fun serializeDisplayByClaimMap(displayMap: Map<String, List<Display>>): String {
        val mapper = jacksonObjectMapper()
        return mapper.writeValueAsString(displayMap)
    }
    fun deserializeDisplayByClaimMap(displayMap: String): Map<String, List<Display>> {
        val mapper = jacksonObjectMapper()
        val typeRef = object : TypeReference<Map<String, List<Display>>>() {}
        return mapper.readValue(displayMap, typeRef)
    }

    private fun <T> containsAllElements(array1: List<T>, array2: List<T>): Boolean {
        return array1.toSet().containsAll(array2.toList())
    }
}