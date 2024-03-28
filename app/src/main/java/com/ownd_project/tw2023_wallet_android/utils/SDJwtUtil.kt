package com.ownd_project.tw2023_wallet_android.utils

import com.ownd_project.tw2023_wallet_android.signature.SignatureUtil
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64

object SDJwtUtil {
    data class SDJwtParts(
        val issuerSignedJwt: String,
        val disclosures: List<String>,
        val keyBindingJwt: String?,
    )

    fun divideSDJwt(sdJwt: String): SDJwtParts {
        val parts = sdJwt.split('~')
        if (parts.isEmpty()) {
            throw IllegalArgumentException("Invalid SD-JWT: No parts found")
        }

        val issuerSignedJwt = parts.first()
        val disclosures = parts.subList(1, parts.size - 1)
        val keyBindingJwt = parts.last().takeIf { it.isNotEmpty() }

        return SDJwtParts(issuerSignedJwt, disclosures, keyBindingJwt)
    }

    data class Disclosure(val disclosure: String, val key: String?, val value: String?)

    fun decodeDisclosure(disclosures: List<String>): List<Disclosure> {
        return disclosures.map { d ->
            val decodedString = Base64.getUrlDecoder().decode(d).toString(Charsets.UTF_8)
            val decoded = Json.parseToJsonElement(decodedString) as JsonArray

            val key: String?
            val value: String?

            when (decoded.size) {
                2 -> {
                    key = null
                    value = decoded[1].jsonPrimitive.content
                }

                3 -> {
                    key = decoded[1].jsonPrimitive.content
                    value = decoded[2].jsonPrimitive.content
                }

                else -> {
                    key = null
                    value = null
                }
            }

            Disclosure(disclosure = d, key = key, value = value)
        }
    }

    fun decodeSDJwt(credential: String): List<Disclosure> {
        val dividedJwt = divideSDJwt(credential)
        return decodeDisclosure(dividedJwt.disclosures)
    }

    // SD-JWTからIssuer-signed JWTの部分を抽出し、デコードする関数
    fun getDecodedJwtHeader(sdJwt: String): JSONObject? {
        val parts = sdJwt.split('~')
        val issuerJwt = parts.firstOrNull() ?: return null
        val issuerParts = issuerJwt.split('.')
        val header = issuerParts.firstOrNull()
        return JSONObject(String(Base64.getUrlDecoder().decode(header), Charsets.UTF_8))
    }

    // Base64エンコードされた文字列をX509Certificateにデコードする関数

    // JWTのヘッダーから"x5c"フィールドの値を抽出する関数
    private fun extractX5cValues(header: JSONObject): List<String>? {
        val x5cJsonArray = header.optJSONArray("x5c") ?: return null
        return (0 until x5cJsonArray.length()).mapNotNull { index ->
            x5cJsonArray.optString(index)
        }
    }

    // JWTから"x5c"フィールドの証明書を取得してLiveDataに格納する
    fun getX509CertificatesFromJwt(jwt: JSONObject): List<X509Certificate>? {
        val x5cValues = extractX5cValues(jwt)
        return x5cValues?.mapNotNull { SignatureUtil.decodeBase64ToX509Certificate(it) }
    }

}