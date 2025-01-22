package com.ownd_project.tw2023_wallet_android.ui.credential_detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ownd_project.tw2023_wallet_android.datastore.CredentialData
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistoryStore
import com.ownd_project.tw2023_wallet_android.oid.InputDescriptor
import com.ownd_project.tw2023_wallet_android.oid.OpenIdProvider
import com.ownd_project.tw2023_wallet_android.oid.PresentationDefinition
import com.ownd_project.tw2023_wallet_android.utils.MetadataUtil
import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil
import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil.decodeSDJwt
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import com.ownd_project.tw2023_wallet_android.vci.CredentialsSupportedDisplay
import com.ownd_project.tw2023_wallet_android.vci.Display
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Base64
import java.util.Locale


class CredentialDetailViewModel(
    private val credentialStore: CredentialDataStore,
    private val historyStore: CredentialSharingHistoryStore,
) : ViewModel() {
    private val mapper = jacksonObjectMapper()

    @JsonIgnoreProperties(ignoreUnknown = true) // Public/Private Claims Namesが存在する可能性を考慮
    data class JwtPayload(
        val iss: String? = null,
        val iat: String? = null,
        val jti: String? = null,
        val sub: String? = null,
        val aud: String? = null,
        val exp: String? = null,
        val nbf: String? = null,
        val vc: Map<String, Any>? = null,
    )
// Todo 必要に応じてVcの方をちゃんと定義する
//data class Vc(
//    val id: String? = null,
//    val type: List<String>? = null,
//    val issuer: String? = null,
//    val validFrom: String? = null,
//    val credentialSubject: Map<String, Any>? = null,
//)

    data class CredentialDetails(
        val credential: CredentialData,
        val disclosures: List<DisclosureItem>, // DisclosureItemは適切なデータ構造に置き換えてください
        val showQRCode: Boolean,
    )

    data class DisclosureItem(
        val key: String,
        val value: String,
    )

    private val _credentialData = MutableLiveData<CredentialData>()
    val credentialData: LiveData<CredentialData> get() = _credentialData

    var presentationDefinition: PresentationDefinition? = null
    private var sharingData: List<SDJwtUtil.Disclosure> = emptyList()
    lateinit var inputDescriptor: InputDescriptor
    lateinit var metadata: CredentialIssuerMetadata
    lateinit var displayMap: Map<String, List<Display>>

    fun setCredentialDataById(id: String) {
        viewModelScope.launch() {
            val credentialData = credentialStore.getCredentialById(id)
            withContext(Dispatchers.Main) {
                if (credentialData != null) {
                    metadata = mapper.readValue(
                        credentialData.credentialIssuerMetadata,
                        CredentialIssuerMetadata::class.java
                    )
                    presentationDefinition?.let { pd ->
                        if (credentialData.format == "vc+sd-jwt") {
                            val (id, selected) =
                                OpenIdProvider.selectDisclosure(
                                    credentialData.credential,
                                    pd
                                )!!
                            inputDescriptor = id
                            sharingData = selected
                        } else {
                            inputDescriptor = pd.inputDescriptors[0] // 選択開示できないので先頭固定
                        }
                    }
                    val format = credentialData.format
                    val types =
                        MetadataUtil.extractTypes(format, credentialData.credential)
                    val cs = requireNotNull(
                        MetadataUtil.findMatchingCredentials(
                            format,
                            types,
                            metadata
                        )
                    )
                    displayMap = MetadataUtil.extractDisplayByClaim(cs)
                    _credentialData.value = credentialData!!
                } else {
                    throw IllegalArgumentException("wrong id is specified: $id")
                }
            }
        }
    }

    fun setCredentialData(byteArray: ByteArray) {
        val credentialDataSchema = CredentialData.parseFrom(byteArray)
        metadata = mapper.readValue(
            credentialDataSchema.credentialIssuerMetadata,
            CredentialIssuerMetadata::class.java
        )
        _credentialData.value = credentialDataSchema
    }

    private val _matchedHistories =
        MutableLiveData<List<com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory>>()
    val matchedHistories: LiveData<List<com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory>> get() = _matchedHistories

    fun findHistoriesByCredentialId(credentialId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val histories = historyStore.findAllByCredentialId(credentialId)
            _matchedHistories.postValue(histories)
        }
    }

    val credentialTypeName: LiveData<String> = _credentialData.map { credentialData ->
        // CredentialDataからCredentialsSupportedDisplayのnameを取得
        val format = credentialData.format
        val types =
            MetadataUtil.extractTypes(format, credentialData.credential)
        val credentialSupported =
            types.mapNotNull { it -> metadata.credentialConfigurationsSupported[it] }
        val displayData = credentialSupported.firstOrNull()?.display
        val display = selectDisplay(displayData)
        display?.name ?: "不明なタイプ"
    }

    private fun selectDisplay(displayList: List<CredentialsSupportedDisplay>?): CredentialsSupportedDisplay? {
        if (displayList.isNullOrEmpty()) {
            return null
        }
        val currentLocale = Locale.getDefault().toString()
        val defaultDisplay = displayList.firstOrNull { it.locale.isNullOrEmpty() }
        return displayList.find { it.locale == currentLocale } ?: defaultDisplay
        ?: displayList.first()
    }

    suspend fun deleteCredentialById(id: String) {
        credentialStore.deleteCredentialById(id)
    }

    val credentialDetails: LiveData<CredentialDetails> = _credentialData.map { credential ->
        when (credential.format) {
            "vc+sd-jwt" -> {
                val disclosures = decodeSDJwt(credential.credential).map {
                    DisclosureItem(it.key ?: "Unknown", it.value ?: "N/A")
                }
                disclosures.forEach { println(it) }
                CredentialDetails(credential, disclosures, showQRCode = false)
            }

            "jwt_vc_json" -> {
                // JWTのデコードとペイロードの取得
                val jwtPayload = decodeJwtVCJson(credential.credential)
                val credentialSubject =
                    jwtPayload.vc?.get("credentialSubject") as? Map<String, Any>

                // credentialSubject MapをDisclosureItemリストに変換
                val disclosures = credentialSubject?.map { (key, value) ->
                    DisclosureItem(key, value.toString())
                } ?: listOf() // credentialSubjectがnullの場合は空のリストを使用

                CredentialDetails(credential, disclosures, showQRCode = true)
            }

            else -> CredentialDetails(credential, emptyList(), showQRCode = false)
        }
    }

    val displayData: LiveData<CredentialsSupportedDisplay> = _credentialData.map { credential ->
        credential?.credentialIssuerMetadata?.let {
            val format = credential.format
            val types =
                MetadataUtil.extractTypes(format, credential.credential)
            val credentialSupported =
                types.mapNotNull { it -> metadata.credentialConfigurationsSupported[it] }
            val displayData = credentialSupported.firstOrNull()?.display
            selectDisplay(displayData)
        }!!
    }

    // JwtVCJsonをデコードする関数
    // android.util.Base64.decodeを使っていたけど、
    // Android フレームワークのAPIのためUnitテストできなかった
    // なので、java.util.Base64を使うようにした
    private fun decodeJwtVCJson(jwt: String): JwtPayload {
        val parts = jwt.split(".")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid JWT token format")
        }
        // URL_SAFEデコーダーを使用してペイロードをデコード
        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))
        val objectMapper = jacksonObjectMapper()
        return objectMapper.readValue(payloadJson)
    }
}