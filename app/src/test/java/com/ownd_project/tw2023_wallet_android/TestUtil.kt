package com.ownd_project.tw2023_wallet_android

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECPoint
import java.util.Base64

fun encodePublicKeyToJwks(publicKey: RSAPublicKey, kid: String): String {
    val publicKeyModulus = Base64.getUrlEncoder().encodeToString(publicKey.modulus.toByteArray())
    val publicKeyExponent =
        Base64.getUrlEncoder().encodeToString(publicKey.publicExponent.toByteArray())

    return """
        {
            "keys": [
                {
                    "kty": "RSA",
                    "alg": "RS256",
                    "use": "sig",
                    "kid": "$kid",
                    "n": "$publicKeyModulus",
                    "e": "$publicKeyExponent"
                }
            ]
        }
    """.trimIndent()
}

fun encodeECPublicKeyToJwks(publicKey: ECPublicKey, kid: String): String {
    val ecPoint: ECPoint = publicKey.w
    val publicKeyX = Base64.getUrlEncoder().encodeToString(ecPoint.affineX.toByteArray())
    val publicKeyY = Base64.getUrlEncoder().encodeToString(ecPoint.affineY.toByteArray())

    return """
        {
            "keys": [
                {
                    "kty": "EC",
                    "alg": "ES256",
                    "use": "sig",
                    "kid": "$kid",
                    "crv": "P-256",
                    "x": "$publicKeyX",
                    "y": "$publicKeyY"
                }
            ]
        }
    """.trimIndent()
}

fun createQueryParameterFromJson(json: String, paramName: String): String {
    val encodedJson = URLEncoder.encode(json, StandardCharsets.UTF_8.toString())
    return "$paramName=$encodedJson"
}