package com.ownd_project.tw2023_wallet_android.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.ownd_project.tw2023_wallet_android.signature.JWT
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.signature.toBase64Url
import org.jose4j.jwk.EllipticCurveJsonWebKey
import org.jose4j.jwk.JsonWebKey
import org.jose4j.lang.JoseException
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPoint
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


object Constants {
    const val KEYSTORE_TYPE = "AndroidKeyStore"
    const val KEY_ALGORITHM_EC = "EC"
    const val SIGNING_ALGORITHM = "SHA256withECDSA"
    const val CURVE_SPEC = "secp256r1"
}

object KeyPairUtil {

    private val objectMapper = jacksonObjectMapper()
    private val keyStore: KeyStore = KeyStore.getInstance(Constants.KEYSTORE_TYPE).apply {
        load(null)
    }

    fun generateSignVerifyKeyPair(alias: String): KeyPair {
        // キーペアジェネレータを初期化
        val keyPairGenerator =
            KeyPairGenerator.getInstance(Constants.KEY_ALGORITHM_EC, Constants.KEYSTORE_TYPE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            // 署名と検証の目的で使用
            // https://developer.android.com/reference/kotlin/android/security/keystore/KeyProperties
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            // https://developer.android.com/reference/kotlin/android/security/keystore/KeyGenParameterSpec#example:-nist-p-256-ec-key-pair-for-signingverification-using-ecdsa
            .setAlgorithmParameterSpec(ECGenParameterSpec(Constants.CURVE_SPEC))
            .setDigests(KeyProperties.DIGEST_SHA256).build()

        keyPairGenerator.initialize(keyGenParameterSpec)

        // キーペアを生成
        return keyPairGenerator.generateKeyPair()
    }

    fun isKeyPairExist(alias: String): Boolean {
        return keyStore.containsAlias(alias)
    }

    fun getPrivateKey(alias: String): PrivateKey? {
        return keyStore.getEntry(alias, null)?.let { entry ->
            if (entry is KeyStore.PrivateKeyEntry) {
                try {
                    entry.privateKey
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else null
        }
    }

    fun getPublicKey(alias: String): PublicKey? {
        return keyStore.getCertificate(alias)?.publicKey
    }

    fun getKeyPair(alias: String): KeyPair? {
        val privateKey = getPrivateKey(alias)
        val publicKey = getPublicKey(alias)
        return KeyPair(publicKey, privateKey)
    }

    fun createProofJwt(keyAlias: String, audience: String, nonce: String): String {
        // todo 関数の場所が適切じゃ無いので移動する
        val jwk = publicKeyToJwk(getPublicKey(keyAlias))
        val header = mapOf("typ" to "openid4vci-proof+jwt", "alg" to "ES256", "jwk" to jwk)
        val payload = mapOf(
            "aud" to audience,
            "iat" to (System.currentTimeMillis() / 1000).toInt(),
            "nonce" to nonce
        )

        val unsignedToken = "${
            objectMapper.writeValueAsString(header).toByteArray().toBase64Url()
        }.${objectMapper.writeValueAsString(payload).toByteArray().toBase64Url()}"

        val privateKey = getPrivateKey(keyAlias)
        val signatureBase64url = JWT.signJwt(privateKey!!, unsignedToken).toBase64Url()

        return "$unsignedToken.$signatureBase64url"
    }

    private fun encodeBase64Url(bytes: ByteArray): String {
        return Base64.getUrlEncoder().encodeToString(bytes).replace('+', '-').replace('/', '_')
            .replace("=", "")
    }

    fun decodeJwt(jwt: String): Triple<Map<String, Any>, Map<String, Any>, String> {
        return JWT.decodeJwt(jwt)
    }

    private fun jwkToX509(jwkJson: Map<String, String>): X509EncodedKeySpec {
        try {
            val jwk = JsonWebKey.Factory.newJwk(jwkJson)
            if (jwk is EllipticCurveJsonWebKey) {
                val ecPublicKey = jwk.publicKey as ECPublicKey
                val x509Data = ecPublicKey.encoded
                return X509EncodedKeySpec(x509Data)
            }
        } catch (e: JoseException) {
            e.printStackTrace()
        }
        throw Error()
    }

    private fun createPublicKey(
        jwkJson: Map<String, String>
    ): PublicKey {
        val keyFactory = KeyFactory.getInstance("EC")
        val keySpec = jwkToX509(jwkJson)

        return keyFactory.generatePublic(keySpec)
    }
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

    fun generateEcPublicKeyJwk(
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

    fun generateRsaPublicKeyJwk(rsaPublicKey: RSAPublicKey): Map<String, String> {
        val n = Base64.getUrlEncoder().encodeToString(rsaPublicKey.modulus.toByteArray())
        val e = Base64.getUrlEncoder().encodeToString(rsaPublicKey.publicExponent.toByteArray())

        // return """{"kty":"RSA","n":"$n","e":"$e"}"""
        return mapOf(
            "kty" to "RSA",
            "n" to n,
            "e" to e
        )
    }

    fun generatePublicKeyJwk(keyPair: KeyPair, option: SigningOption): Map<String, String> {
        val publicKey: PublicKey = keyPair.public
        return generatePublicKeyJwk(publicKey, option)
    }
    fun generatePublicKeyJwk(publicKey: PublicKey, option: SigningOption): Map<String, String> {
        return when (publicKey) {
            is RSAPublicKey -> generateRsaPublicKeyJwk(publicKey)
            is ECPublicKey -> generateEcPublicKeyJwk(publicKey, option)
            else -> throw IllegalArgumentException("Unsupported Key Type: ${publicKey::class.java.name}")
        }
    }

    fun verifyJwt(jwkJson: Map<String, String>, jwt: String): Boolean {
        val publicKey = createPublicKey(jwkJson)
        val result = JWT.verifyJwt(jwt, publicKey)
        return result.isRight()
    }
}

object KeyStoreHelper {

    private const val KEY_ALIAS = "datastore_encryption_key"
    private val keyStore: KeyStore = KeyStore.getInstance(Constants.KEYSTORE_TYPE).apply {
        load(null)
    }

    fun getSecretKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } else {
            generateSecretKey()
        }
    }

    fun generateSecretKey(): SecretKey {
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, Constants.KEYSTORE_TYPE)
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7).setKeySize(256).build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }
}

// Common Base64 utilities
private fun ByteArray.toBase64Url() = Base64.getUrlEncoder().encodeToString(this).trimEnd('=')
private fun String.fromBase64Url() = Base64.getUrlDecoder().decode(this)
private fun BigInteger.toBase64() = Base64.getEncoder().encodeToString(this.toByteArray())
fun generateRsaKeyPair(): KeyPair {
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    return keyPairGenerator.generateKeyPair()
}

fun generateEcKeyPair(): KeyPair {
    val keyPairGenerator = KeyPairGenerator.getInstance("EC")
    val ecSpec = ECGenParameterSpec("secp256r1")
    keyPairGenerator.initialize(ecSpec)
    return keyPairGenerator.generateKeyPair()
}

fun publicKeyToJwk(publicKey: PublicKey?): Map<String, String>? {
    return (publicKey as? ECPublicKey)?.let {
        mapOf(
            "kty" to "EC",
            "alg" to "ES256",
            "crv" to "P-256", // Consider changing based on the curve used
            "x" to it.w.affineX.toBase64(),
            "y" to it.w.affineY.toBase64()
        )
    }
}

fun privateKeyToJwk(privateKey: ECPrivateKey, publicKey: ECPublicKey): Map<String, String> {
//    val publicKey = privateKey.publicKey as ECPublicKey

    val encoder = Base64.getUrlEncoder().withoutPadding()

    return mapOf(
        "kty" to "EC",
        "alg" to "ES256",
        "crv" to "P-256", // 使用する曲線に基づいて変更してください
        "x" to encoder.encodeToString(publicKey.w.affineX.toByteArray()),
        "y" to encoder.encodeToString(publicKey.w.affineY.toByteArray()),
        "d" to encoder.encodeToString(privateKey.s.toByteArray())
    )
}

data class SigningOption(
    val signingAlgo: String = "ES256",
    val signingCurve: String = "P-256",
)