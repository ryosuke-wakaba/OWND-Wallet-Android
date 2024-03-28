package com.ownd_project.tw2023_wallet_android.ui.shared

import com.ownd_project.tw2023_wallet_android.oid.KeyBinding
import com.ownd_project.tw2023_wallet_android.signature.JWT
import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil
import java.security.MessageDigest
import java.util.Base64

class KeyBindingImpl(val keyAlias: String): KeyBinding {
    override fun generateJwt(
        sdJwt: String,
        selectedDisclosures: List<SDJwtUtil.Disclosure>,
        aud: String,
        nonce: String
    ): String {
        val parts = sdJwt.split('~')
        val issuerSignedJwt = parts[0]
        // It MUST be taken over the US-ASCII bytes preceding the KB-JWT in the Presentation
        val sd =
            issuerSignedJwt + "~" + selectedDisclosures.joinToString("~") { it.disclosure } + "~"

        // The bytes of the digest MUST then be base64url-encoded.
        val sdHash = sd.toByteArray(Charsets.US_ASCII).sha256ToBase64Url()
        val header = mapOf("typ" to "kb+jwt", "alg" to "ES256")
        val payload = mapOf(
            "aud" to aud,
            "iat" to (System.currentTimeMillis() / 1000).toInt(),
            "_sd_hash" to sdHash,
            "nonce" to nonce
        )
        return JWT.sign(Constants.KEY_PAIR_ALIAS_FOR_KEY_BINDING, header, payload)
    }

    private fun ByteArray.sha256ToBase64Url(): String {
        val sha = MessageDigest.getInstance("SHA-256").digest(this)
        return Base64.getUrlEncoder().encodeToString(sha).trimEnd('=')
    }
}