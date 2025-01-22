package com.ownd_project.tw2023_wallet_android.utils

import com.ownd_project.tw2023_wallet_android.signature.JWT
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import com.ownd_project.tw2023_wallet_android.vci.CredentialConfiguration
import com.ownd_project.tw2023_wallet_android.vci.CredentialConfigurationJwtVcJson
import com.ownd_project.tw2023_wallet_android.vci.CredentialConfigurationVcSdJwt
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
    ): CredentialConfiguration? {
        return metadata.credentialConfigurationsSupported.entries.firstOrNull { (_, credentialSupported) ->
            when (credentialSupported) {
                is CredentialConfigurationVcSdJwt -> {
                    // VcSdJwtの場合、vctとtypesの最初の要素を比較
                    format == "vc+sd-jwt" && types.firstOrNull() == credentialSupported.vct
                }

                is CredentialConfigurationJwtVcJson -> {
                    // JwtVcJsonの場合、typesとcredentialDefinition.typeを両方ソートして比較
                    format == "jwt_vc_json" && containsAllElements(
                        credentialSupported.credentialDefinition.type,
                        types
                    )
                }

                else -> false
            }
        }?.value
    }

    fun extractDisplayByClaim(credentialsSupported: CredentialConfiguration): MutableMap<String, List<Display>> {
        val displayMap = mutableMapOf<String, List<Display>>()
        when (credentialsSupported) {
            is CredentialConfigurationJwtVcJson -> {
                val credentialSubject = credentialsSupported.credentialDefinition.credentialSubject
                credentialSubject?.map { (k, v) ->
                    v.display?.let { displayMap.put(k, it) }
                }
            }

            is CredentialConfigurationVcSdJwt -> {
                val credentialSubject = credentialsSupported.claims
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