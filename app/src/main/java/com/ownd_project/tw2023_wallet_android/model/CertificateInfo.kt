package com.ownd_project.tw2023_wallet_android.model

data class CertificateInfo(
    val domain: String?,
    val organization: String?,
    val locality: String?,
    val state: String?,
    val country: String?,
    val street: String? = null,
    val email: String? = null,
    val issuer: CertificateInfo? = null
) {
    fun getFullAddress(): String {
        val addressParts = listOf(locality, state, country).filterNotNull()
        return addressParts.joinToString(", ")
    }
}