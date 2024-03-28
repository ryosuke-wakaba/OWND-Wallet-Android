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
    var claims: List<String>,
    var rpName: String,
    var privacyPolicyUrl: String,
    var logoUrl: String
)

data class BackupData(
    val seed: String,
    val idTokenSharingHistories: List<IdTokenSharingHistory>,
    val credentialSharingHistories: List<CredentialSharingHistory>
)
