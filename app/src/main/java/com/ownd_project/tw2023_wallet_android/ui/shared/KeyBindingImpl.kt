package com.ownd_project.tw2023_wallet_android.ui.shared

import com.ownd_project.tw2023_wallet_android.oid.KeyBinding
import com.ownd_project.tw2023_wallet_android.oid.SdJwtVcPresentation
import com.ownd_project.tw2023_wallet_android.signature.JWT
import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil

class KeyBindingImpl(val keyAlias: String) : KeyBinding {
    override fun generateJwt(
        sdJwt: String,
        selectedDisclosures: List<SDJwtUtil.Disclosure>,
        aud: String,
        nonce: String
    ): String {
        val (header, payload) = SdJwtVcPresentation.genKeyBindingJwtParts(
            sdJwt,
            selectedDisclosures,
            aud,
            nonce
        )
        return JWT.sign(Constants.KEY_PAIR_ALIAS_FOR_KEY_BINDING, header, payload)
    }
}