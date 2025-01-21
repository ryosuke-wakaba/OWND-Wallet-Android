package com.ownd_project.tw2023_wallet_android.ui.siop_vp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.datastore.CredentialData
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.oid.OpenIdProvider
import com.ownd_project.tw2023_wallet_android.oid.PresentationDefinition
import com.ownd_project.tw2023_wallet_android.utils.MetadataUtil
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import com.ownd_project.tw2023_wallet_android.vci.CredentialsSupportedDisplay
import kotlinx.coroutines.launch
import java.util.Locale

open class CertificateSelectionViewModel() : ViewModel() {
    private val _credentialDataList = MutableLiveData<List<CredentialInfo>?>()
    val credentialDataList: LiveData<List<CredentialInfo>?> = _credentialDataList

    private var _credentialDataStore: CredentialDataStore? = null
    open fun setCredentialDataStore(credentialDataStore: CredentialDataStore) {
        _credentialDataStore = credentialDataStore
    }

    open fun getData(presentationDefinition: PresentationDefinition) {
        viewModelScope.launch {
            _credentialDataStore?.credentialDataListFlow?.collect { schema ->
                val objectMapper = jacksonObjectMapper()
                val items = schema?.itemsList
                if (items != null) {
                    val infos = items.mapNotNull {
                        val credential = it.credential
                        val format = it.format
                        val types =
                            MetadataUtil.extractTypes(format, credential)
                        if (types.contains("CommentCredential")) {
                            // filter comment vc
                            null
                        } else {
                            if (format == "vc+sd-jwt") {
                                val selectedClaims = OpenIdProvider.selectRequestedClaims(
                                    credential,
                                    presentationDefinition.inputDescriptors
                                )
                                if (selectedClaims.satisfied) {
                                    val metadata: CredentialIssuerMetadata = objectMapper.readValue(
                                        it.credentialIssuerMetadata,
                                        CredentialIssuerMetadata::class.java
                                    )
                                    val credentialSupported =
                                        metadata.credentialsSupported[types.firstOrNull()]
                                    // val displayData = credentialSupported?.display?.firstOrNull()
                                    val displayData = credentialSupported?.display?.let { display ->
                                        getLocalizedDisplayName(display)
                                    }
                                    CredentialInfo(
                                        id = it.id,
                                        name = displayData ?: "Metadata not found",
                                        issuer = it.iss
                                    )
                                } else {
                                    null
                                }
                            } else {
                                null
                            }
                        }
                    }.toMutableList()
                    infos.add(CredentialInfo(useCredential = false))
                    setCredentialData(infos)
                }
            }
        }
    }

    open fun setCredentialData(data: List<CredentialInfo>) {
        _credentialDataList.value = data
    }

}

fun getLocalizedDisplayName(
    displays: List<CredentialsSupportedDisplay>,
    defaultLocale: Locale = Locale.getDefault()
): String? {
    // 現在のロケール（例: "ja_JP" → "ja"）を取得
    val currentLocale = defaultLocale.toLanguageTag()

    // 完全一致するロケールを検索
    val exactMatch = displays.firstOrNull { it.locale == currentLocale }
    if (exactMatch != null) {
        return exactMatch.name
    }

    // 言語コードだけが一致するものを検索 (例: "en-US" と "en")
    val languageMatch = displays.firstOrNull {
        it.locale?.split("-")?.firstOrNull() == defaultLocale.language
    }
    if (languageMatch != null) {
        return languageMatch.name
    }

    return displays.firstOrNull()?.name
}

data class CredentialInfo(
    var id: String = "",
    var name: String = "",
    var issuer: String = "",
    var useCredential: Boolean = true,
)
