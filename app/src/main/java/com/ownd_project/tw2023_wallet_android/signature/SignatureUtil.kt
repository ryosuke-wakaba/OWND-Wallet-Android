package com.ownd_project.tw2023_wallet_android.signature

import com.fasterxml.jackson.core.json.JsonWriteFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.DERSequenceGenerator
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Security
import java.security.cert.CertPathValidator
import java.security.cert.CertificateFactory
import java.security.cert.PKIXParameters
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import java.security.spec.EllipticCurve
import java.util.Base64
import java.util.Date
import javax.security.auth.x500.X500Principal

interface ECPrivateJwk {
    val kty: String
    val crv: String
    val x: String
    val y: String
    val d: String
}

interface ECPublicJwk {
    val kty: String
    val crv: String
    val x: String
    val y: String
}

object SignatureUtil {
    init {
        // https://developer.android.com/about/versions/12/behavior-changes-all?hl=ja#bouncy-castle
        // androidがシステムレベルで登録するBCのセキュリティプロバイダが一部の実装を削除しているため、
        // 一度そのプロバイダーの登録を削除してからdependencyで追加したライブラリのプロバイダーを直接登録し直す
        Security.removeProvider("BC");
        Security.addProvider(BouncyCastleProvider())
    }

    fun derToRaw(derSig: ByteArray): ByteArray {
        val r = BigInteger(1, derSig.copyOfRange(4, 4 + derSig[3].toInt()))
        val s = BigInteger(
            1, derSig.copyOfRange(
                6 + derSig[3].toInt(),
                6 + derSig[3].toInt() + derSig[5 + derSig[3].toInt()].toInt()
            )
        )

        val rBytes =
            r.toByteArray().let { if (it.size > 32) it.copyOfRange(1, it.size) else it }
        val sBytes =
            s.toByteArray().let { if (it.size > 32) it.copyOfRange(1, it.size) else it }

        val rLength = rBytes.size
        val sLength = sBytes.size

        val raw = ByteArray(64)

        System.arraycopy(rBytes, 0, raw, 32 - rLength, rLength)
        System.arraycopy(sBytes, 0, raw, 64 - sLength, sLength)

        return raw
    }

    fun rawToDer(signatureBytes: ByteArray): ByteArray {
        val r = BigInteger(1, signatureBytes.copyOfRange(0, signatureBytes.size / 2))
        val s =
            BigInteger(1, signatureBytes.copyOfRange(signatureBytes.size / 2, signatureBytes.size))

        val rBytes = toFixedLengthBytes(r, 32)
        val sBytes = toFixedLengthBytes(s, 32)

        val bos = ByteArrayOutputStream()
        val seqGen = DERSequenceGenerator(bos)
        seqGen.addObject(DEROctetString(rBytes))
        seqGen.addObject(DEROctetString(sBytes))
//        seqGen.addObject(DEROctetString(r.toByteArray()))
//        seqGen.addObject(DEROctetString(s.toByteArray()))
        seqGen.close()

        return bos.toByteArray()
    }

    fun toFixedLengthBytes(value: BigInteger, length: Int): ByteArray {
        val valueBytes = value.toByteArray()
        if (valueBytes.size == length) {
            return valueBytes
        }

        val result = ByteArray(length)
        if (valueBytes.size > length) {
            // バイト配列が長すぎる場合、末尾のバイトをコピー
            System.arraycopy(valueBytes, valueBytes.size - length, result, 0, length)
        } else {
            // バイト配列が短すぎる場合、左側をゼロで埋める
            System.arraycopy(valueBytes, 0, result, length - valueBytes.size, valueBytes.size)
        }

        return result
    }

    fun generateECKeyPair(jwk: ECPrivateJwk): KeyPair {
        val curveParams = ECNamedCurveTable.getParameterSpec(jwk.crv)
        val ellipticCurve = EllipticCurve(
            ECFieldFp(curveParams.curve.field.characteristic),
            curveParams.curve.a.toBigInteger(),
            curveParams.curve.b.toBigInteger()
        )
        val g = curveParams.g.normalize()
        val ecPoint = ECPoint(
            g.affineXCoord.toBigInteger(),
            g.affineYCoord.toBigInteger()
        )
        val ecSpec = ECParameterSpec(
            ellipticCurve,
            ecPoint,
            curveParams.n,
            curveParams.h.intValueExact()
        )
        val point = curveParams.curve.createPoint(
            BigInteger(1, Base64.getUrlDecoder().decode(jwk.x)),
            BigInteger(1, Base64.getUrlDecoder().decode(jwk.y))
        ).normalize()

        val publicKeySpec = ECPublicKeySpec(
            ECPoint(point.affineXCoord.toBigInteger(), point.affineYCoord.toBigInteger()),
            ecSpec
        )
        val privateKeyValue = BigInteger(1, Base64.getUrlDecoder().decode(jwk.d))
        val privateKeySpec = ECPrivateKeySpec(privateKeyValue, ecSpec)

        val keyFactory = KeyFactory.getInstance("ECDSA", "BC")
        val publicKey = keyFactory.generatePublic(publicKeySpec)
        val privateKey = keyFactory.generatePrivate(privateKeySpec)

        return KeyPair(publicKey, privateKey)
    }

    // todo [プロジェクト完了後] RSAにも対応する
    fun toJwkThumbprint(jwk: ECPublicJwk): String {
        /*
        https://openid.github.io/SIOPv2/openid-connect-self-issued-v2-wg-draft.html#section-11-3.2.1
        The thumbprint value of JWK Thumbprint Subject Syntax Type is computed
         as the SHA-256 hash of the octets of the UTF-8 representation of a JWK constructed containing only the REQUIRED members to represent the key,
         with the member names sorted into lexicographic order, and with no white space or line breaks.
         */
        // JSONオブジェクトマッパーの設定
        val objectMapper = ObjectMapper()
        objectMapper.configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true)
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)

        // PublicJwkオブジェクトをMapに変換
        val jwkMap = objectMapper.convertValue(jwk, Map::class.java) as Map<String, Any>

        // 辞書順にソートしてJSON文字列にエンコード
        val sortedJsonString = objectMapper.writeValueAsString(jwkMap.toSortedMap())

        // SHA-256でハッシュを計算
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = messageDigest.digest(sortedJsonString.toByteArray(Charsets.UTF_8))

        // Base64Urlエンコード
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedBytes)
    }

    fun generateCertificate(
        keyPair: KeyPair,
        signerKeyPair: KeyPair,
        isCa: Boolean
    ): X509Certificate {
        val now = Date()
        val notBefore = Date(now.time)
        val notAfter = Date(now.time + 365 * 24 * 60 * 60 * 1000L) // 1 year validity
        val subjectDN =
            X500Principal("CN=Example CA, O=Example Company, L=City, ST=State, C=Country")

        val serialNumber = BigInteger.valueOf(now.time) // unique serial number for each certificate
        val certBuilder = JcaX509v3CertificateBuilder(
            subjectDN,
            serialNumber,
            notBefore,
            notAfter,
            subjectDN,
            keyPair.public
        )
        // CAフラグを持つ基本制約の追加
        if (isCa) {
            certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                BasicConstraints(true)
            )
        }

        // Signing the certificate using the private key
        val signer = JcaContentSignerBuilder("SHA256withECDSA").build(signerKeyPair.private)
        val holder = certBuilder.build(signer)

        // Converting to X509Certificate
        return JcaX509CertificateConverter().getCertificate(holder)
    }

    fun generateSelfSignedCertificate(keyPair: KeyPair): X509Certificate {
        val now = Date()
        val notBefore = Date(now.time)
        val notAfter = Date(now.time + 365 * 24 * 60 * 60 * 1000L) // 1 year validity
        val subjectDN =
            X500Principal("CN=Example CA, O=Example Company, L=City, ST=State, C=Country")

        val serialNumber = BigInteger.valueOf(now.time) // unique serial number for each certificate
//        val publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
        val certBuilder = JcaX509v3CertificateBuilder(
            subjectDN,
            serialNumber,
            notBefore,
            notAfter,
            subjectDN,
            keyPair.public
        )

        // Signing the certificate using the private key
        val signer = JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)
        val holder = certBuilder.build(signer)

        // Converting to X509Certificate
        return JcaX509CertificateConverter().getCertificate(holder)
    }

    fun certificateToPem(certificate: X509Certificate): String {
        val encodedCert = Base64.getEncoder().encodeToString(certificate.encoded)
        return "-----BEGIN CERTIFICATE-----\n$encodedCert\n-----END CERTIFICATE-----"
    }

    fun decodeBase64ToX509Certificate(encodedCert: String): X509Certificate? {
        val decodedCert = Base64.getDecoder().decode(encodedCert)
        return try {
            val certFactory = CertificateFactory.getInstance("X.509")
            val certInputStream = ByteArrayInputStream(decodedCert)
            certFactory.generateCertificate(certInputStream) as X509Certificate
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getX509CertificatesFromUrl(url: String): Array<X509Certificate>? {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to download file: $response")

            val responseBody = response.body()?.string() ?: return null
            return convertPemToX509Certificates(responseBody)
        }
    }

    private fun convertPemToX509Certificates(pem: String): Array<X509Certificate>? {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificates = mutableListOf<X509Certificate>()

        val pemCertificates = pem.trim().split("-----END CERTIFICATE-----")
            .filter { it.contains("-----BEGIN CERTIFICATE-----") }
            .map { it.trim() + "\n-----END CERTIFICATE-----" }

        pemCertificates.forEach { certPem ->
            val certBytes = Base64.getMimeDecoder().decode(
                certPem.lineSequence()
                    .filterNot { it.startsWith("-----BEGIN CERTIFICATE-----") || it.startsWith("-----END CERTIFICATE-----") }
                    .joinToString(separator = "")
            )
            val certInputStream = ByteArrayInputStream(certBytes)
            val cert = certificateFactory.generateCertificate(certInputStream) as X509Certificate
            certificates.add(cert)
        }

        return certificates.toTypedArray()
    }

    fun validateCertificateChain(
        certificates: Array<X509Certificate>,
        rootCertificate: X509Certificate
    ): Boolean {
        try {
            // 証明書ファクトリのインスタンスを作成
            val certificateFactory = CertificateFactory.getInstance("X.509", "BC")

            // 証明書リストを証明書パスに変換
            val certPath = certificateFactory.generateCertPath(certificates.toList())

            // デフォルトのKeyStoreを使用して信頼されたルート証明書をロード
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            // todo テストコードの場合だけ実行する様に制御を入れる
            keyStore.setCertificateEntry("root", rootCertificate)

            // PKIXパラメータの設定（ルートCAの検証なし）
            val pkixParams = PKIXParameters(keyStore)
            pkixParams.isRevocationEnabled = false // CRLチェックを無効化

            // 証明書パスバリデータを使用して証明書チェーンの検証
            val pathValidator = CertPathValidator.getInstance("PKIX", "BC")
            val result = pathValidator.validate(certPath, pkixParams)
            println(result)

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
//    fun sign(privateKey: PrivateKey, header: Map<String, Any>, payload: Map<String, Any>): String {
//        // sign target
//        val objectMapper = jacksonObjectMapper()
//        val h = objectMapper.writeValueAsString(header).toByteArray().toBase64Url()
//        val p =
//            objectMapper.writeValueAsString(payload).toByteArray().toBase64Url()
//        val unsignedToken = "$h.$p"
//        val dataBytes = unsignedToken.toByteArray(Charsets.UTF_8)
//
//        val signature: ByteArray = Signature.getInstance(Constants.SIGNING_ALGORITHM).run {
//            initSign(privateKey)
//            update(dataBytes)
//            sign()
//        }
//        val signatureBase64url = derToRaw(signature).toBase64Url()
//        return "$unsignedToken.$signatureBase64url"
//    }
//    fun verifySignature(certificate: X509Certificate, data: ByteArray, signature: ByteArray): Boolean {
//        try {
//            val publicKey = certificate.publicKey
//
//            // 署名アルゴリズムを指定（証明書に基づいて適切なアルゴリズムを選択する必要があります）
//            val signatureInstance = Signature.getInstance("SHA256withECDSA")
//            // val signatureInstance = Signature.getInstance("SHA256withRSA")
//
//            signatureInstance.initVerify(publicKey)
//            signatureInstance.update(data)
//            return signatureInstance.verify(signature)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return false
//    }


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
        option: ProviderOption
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

    fun generatePublicKeyJwk(keyPair: KeyPair, option: ProviderOption): Map<String, String> {
        val publicKey: PublicKey = keyPair.public
        return generatePublicKeyJwk(publicKey, option)
    }
    fun generatePublicKeyJwk(publicKey: PublicKey, option: ProviderOption): Map<String, String> {
        return when (publicKey) {
            is RSAPublicKey -> generateRsaPublicKeyJwk(publicKey)
            is ECPublicKey -> generateEcPublicKeyJwk(publicKey, option)
            else -> throw IllegalArgumentException("Unsupported Key Type: ${publicKey::class.java.name}")
        }
    }
    fun byte2Base64Url(b: ByteArray) = b.toBase64Url()

    fun int2Base64Url(i: BigInteger) = i.toBase64Url()
}

fun ByteArray.toBase64Url() = Base64.getUrlEncoder().encodeToString(this).trimEnd('=')
fun BigInteger.toBase64Url() =
    Base64.getUrlEncoder().encodeToString(this.toByteArray()).trimEnd('=')

data class ProviderOption(
    val expiresIn: Int = 600,
    val signingAlgo: String = "ES256",
    val signingCurve: String = "P-256",
)