package com.ownd_project.tw2023_wallet_android.utils

import com.ownd_project.tw2023_wallet_android.signature.toBase64Url
import java.math.BigInteger
import java.security.KeyPair
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECPoint
import java.util.Base64

object KeyUtil {

    fun correctBytes(value: BigInteger): ByteArray {
        /*
        BigInteger の toByteArray() メソッドは、数値をバイト配列に変換しますが、
        この数値が正の場合、最上位バイトが符号ビットとして解釈されることを避けるために、追加のゼロバイトが先頭に挿入されることがあります。
        これは、数値が正で、最上位バイトが 0x80 以上の場合（つまり、最上位ビットが 1 の場合）に起こります。
        その結果、期待していた 32 バイトではなく 33 バイトの配列が得られることがあります。

        期待する 32 バイトの配列を得るには、返されたバイト配列から余分なゼロバイトを取り除くか、
        または正確なバイト長を指定して配列を生成する必要があります。
         */
        val bytes = value.toByteArray()
        return if (bytes.size == 33 && bytes[0] == 0.toByte()) bytes.copyOfRange(
            1,
            bytes.size
        ) else bytes
    }

    private fun publicKeyToJwk(
        ecPublicKey: ECPublicKey,
        option: SigningOption
    ): Map<String, String> {
        val ecPoint: ECPoint = ecPublicKey.w
        val x = correctBytes(ecPoint.affineX).toBase64Url()
        val y = correctBytes(ecPoint.affineY).toBase64Url()

        // return """{"kty":"EC","crv":"P-256","x":"$x","y":"$y"}""" // crvは適宜変更してください
        return mapOf(
            "kty" to "EC",
            "crv" to option.signingCurve,
            "x" to x,
            "y" to y
        )
    }

    private fun publicKeyToJwk(rsaPublicKey: RSAPublicKey): Map<String, String> {
        val n = Base64.getUrlEncoder().encodeToString(rsaPublicKey.modulus.toByteArray())
        val e = Base64.getUrlEncoder().encodeToString(rsaPublicKey.publicExponent.toByteArray())

        // return """{"kty":"RSA","n":"$n","e":"$e"}"""
        return mapOf(
            "kty" to "RSA",
            "n" to n,
            "e" to e
        )
    }

    fun keyPairToPublicJwk(keyPair: KeyPair, option: SigningOption): Map<String, String> {
        val publicKey: PublicKey = keyPair.public
        return publicKeyToJwk(publicKey, option)
    }

    fun publicKeyToJwk(publicKey: PublicKey, option: SigningOption): Map<String, String> {
        return when (publicKey) {
            is RSAPublicKey -> publicKeyToJwk(publicKey)
            is ECPublicKey -> publicKeyToJwk(publicKey, option)
            else -> throw IllegalArgumentException("Unsupported Key Type: ${publicKey::class.java.name}")
        }
    }

}

data class SigningOption(
    val signingAlgo: String = "ES256",
    val signingCurve: String = "P-256",
)