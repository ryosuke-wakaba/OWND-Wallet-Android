package com.ownd_project.tw2023_wallet_android.ui.shared

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.oid.HeaderOptions
import com.ownd_project.tw2023_wallet_android.oid.VpJwtPayload
import com.ownd_project.tw2023_wallet_android.oid.JwtVpJsonGenerator
import com.ownd_project.tw2023_wallet_android.oid.JwtVpJsonPayloadOptions
import com.ownd_project.tw2023_wallet_android.signature.JWT
import com.ownd_project.tw2023_wallet_android.utils.SigningOption
import com.ownd_project.tw2023_wallet_android.utils.KeyPairUtil
import java.security.PublicKey

class JwtVpJsonGeneratorImpl(private val keyAlias: String = Constants.KEY_PAIR_ALIAS_FOR_KEY_JWT_VP_JSON) :
    JwtVpJsonGenerator {
    override fun generateJwt(
        vcJwt: String,
        headerOptions: HeaderOptions,
        payloadOptions: JwtVpJsonPayloadOptions
    ): String {
        val vpClaims = mapOf(
            "@context" to listOf("https://www.w3.org/2018/credentials/v1"),
            "type" to listOf("VerifiablePresentation"),
            "verifiableCredential" to listOf(vcJwt)
        )

        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val header =
            mapOf("alg" to headerOptions.alg, "typ" to headerOptions.typ, "jwk" to getJwk())
        val jwtPayload = VpJwtPayload(
            iss = payloadOptions.iss,
            jti = payloadOptions.jti,
            aud = payloadOptions.aud,
            nbf = payloadOptions.nbf ?: currentTimeSeconds,
            iat = payloadOptions.iat ?: currentTimeSeconds,
            exp = payloadOptions.exp ?: (currentTimeSeconds + 2 * 3600),
            nonce = payloadOptions.nonce,
            vp = vpClaims
        )
        val objectMapper = jacksonObjectMapper()
        val vpTokenPayload =
            objectMapper.convertValue(jwtPayload, Map::class.java) as Map<String, Any>
        return JWT.sign(keyAlias, header, vpTokenPayload)
    }

    override fun getJwk(): Map<String, String> {
        if (!KeyPairUtil.isKeyPairExist(keyAlias)) {
            KeyPairUtil.generateSignVerifyKeyPair(keyAlias)
        }
        val publicKey: PublicKey = KeyPairUtil.getPublicKey(keyAlias)
            ?: throw IllegalStateException("Public key not found for alias: $keyAlias")
        val jwk = KeyPairUtil.publicKeyToJwk(publicKey, SigningOption())
        return jwk
    }
}