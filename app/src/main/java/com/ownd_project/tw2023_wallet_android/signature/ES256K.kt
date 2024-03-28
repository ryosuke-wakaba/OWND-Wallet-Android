package com.ownd_project.tw2023_wallet_android.signature

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPair
import java.security.Security
import java.security.Signature
import java.util.Base64


object ES256K {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun sign(keyPair: KeyPair, data: ByteArray, derEncoding: Boolean = true): ByteArray {
        // 署名の生成
        val signature = Signature.getInstance("SHA256withECDSA", "BC")
        signature.initSign(keyPair.private)
        signature.update(data)
        val signBytes = signature.sign()
        signature.initVerify(keyPair.public)
        signature.update(data)

        val isValid = signature.verify(signBytes)
        assert(isValid)
        return if (derEncoding) {
            signBytes
        } else {
            SignatureUtil.derToRaw(signBytes)
        }
    }

    fun verify(keyPair: KeyPair, data: ByteArray, signedData: ByteArray): Boolean {
        val signature = Signature.getInstance("SHA256withECDSA", "BC")
        signature.initVerify(keyPair.public)
        signature.update(data)
        return signature.verify(signedData)
    }

    fun createJws(keyPair: KeyPair, payload: String, derEncoding: Boolean = true): String {
        val header = mapOf(
            "alg" to "ES256K",
            "typ" to "JWT",
        )

        // ヘッダーをBase64Urlエンコード
        val encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(
            jacksonObjectMapper().writeValueAsBytes(header)
        )

        // ペイロードをBase64Urlエンコード
        val encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            payload.toByteArray(Charsets.UTF_8)
        )

        // 署名の生成
        val signature = sign(
            keyPair,
            "$encodedHeader.$encodedPayload".toByteArray(Charsets.UTF_8),
            derEncoding
        )

        // 署名をBase64Urlエンコード
        val encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature)

        // JWSの生成
        return "$encodedHeader.$encodedPayload.$encodedSignature"
    }
}