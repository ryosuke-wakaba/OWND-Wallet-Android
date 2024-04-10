package com.ownd_project.tw2023_wallet_android.ui.backup

data class IdTokenSharingHistory(
    val rp: String,
    val accountIndex: Int,
    val createdAt: String
)

data class CredentialSharingHistory(
    val rp: String,
    val accountIndex: Int,
    val createdAt: String,
    val credentialID: String,
    var claims: List<Claim>,
    var rpName: String,

    var location: String,
    var contactUrl: String,
    var privacyPolicyUrl: String,
    var logoUrl: String
)

data class Claim(
    val name: String,
    val value: String,
    val purpose: String
)

data class BackupData(
    val seed: String,
    val idTokenSharingHistories: List<IdTokenSharingHistory>,
    val credentialSharingHistories: List<CredentialSharingHistory>
)
