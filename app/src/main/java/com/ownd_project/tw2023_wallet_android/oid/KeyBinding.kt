package com.ownd_project.tw2023_wallet_android.oid

import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil

interface KeyBinding {
    fun generateJwt(
        sdJwt: String,
        selectedDisclosures: List<SDJwtUtil.Disclosure>, // todo 一段階抽象的な型を指定する
        aud: String,
        nonce: String
    ): String
}