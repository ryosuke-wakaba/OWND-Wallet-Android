package com.ownd_project.tw2023_wallet_android.ui.confirmation

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.ui.shared.Constants
import com.ownd_project.tw2023_wallet_android.utils.CredentialRequest
import com.ownd_project.tw2023_wallet_android.utils.CredentialRequestJwtVc
import com.ownd_project.tw2023_wallet_android.utils.CredentialRequestSdJwtVc
import com.ownd_project.tw2023_wallet_android.utils.KeyPairUtil
import com.ownd_project.tw2023_wallet_android.utils.Proof
import com.ownd_project.tw2023_wallet_android.utils.TokenErrorResponseException
import com.ownd_project.tw2023_wallet_android.utils.VCIClient
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import com.ownd_project.tw2023_wallet_android.vci.CredentialOffer
import com.ownd_project.tw2023_wallet_android.vci.CredentialSupported
import com.ownd_project.tw2023_wallet_android.vci.CredentialSupportedJwtVcJson
import com.ownd_project.tw2023_wallet_android.vci.CredentialSupportedVcSdJwt
import com.ownd_project.tw2023_wallet_android.vci.IssuerCredentialSubject
import com.ownd_project.tw2023_wallet_android.vci.MetadataClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import java.util.Locale
import kotlin.coroutines.resume

class ConfirmationViewModel() :
    ViewModel() {
    // CredentialDataStoreのインスタンスを保持するプライベート変数
    private var credentialDataStore: CredentialDataStore? = null

    // CredentialDataStoreのインスタンスをセットするためのメソッド
    fun setCredentialDataStore(dataStore: CredentialDataStore) {
        credentialDataStore = dataStore
    }

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    private fun updateText(
        credentialIssuerMetadata: CredentialIssuerMetadata,
        credentialSupported: CredentialSupported,
    ) {
        val currentLocale = Locale.getDefault().toString() // 例: "en-US"、"ja_JP"
        val issuerDisplay = credentialIssuerMetadata.display
            ?.firstOrNull { it.locale == currentLocale }
            ?: credentialIssuerMetadata.display?.firstOrNull()

        val issuerName = issuerDisplay?.name ?: "Unknown Issuer"
        val credentialType = when (credentialSupported) {
            is CredentialSupportedJwtVcJson -> credentialSupported.display?.get(0)?.name
            is CredentialSupportedVcSdJwt -> credentialSupported.display?.get(0)?.name
            else -> "Unknown Type"
        }

        _text.value = "$issuerName が $credentialType の証明書を発行します"
    }

    private val _credentialSubject = MutableLiveData<Map<String, IssuerCredentialSubject>>()
    val credentialSubject: LiveData<Map<String, IssuerCredentialSubject>> = _credentialSubject

    // 1. subject.displayがnullまたは空の場合、そのままnullを返す
    // 2. subject.displayが1つの要素のみを持っている場合、その要素をそのまま使用
    // 3. 複数のDisplay要素がある場合、現在のロケールに一致するものを探し、見つからない場合は最初の要素を使用
    // 4. ロケールが含まれていないDisplay要素がある場合、それはそのまま保持
    private fun setCredentialSubject(map: Map<String, IssuerCredentialSubject>) {
        val localizedMap = map.mapValues { (_, subject) ->

            val localizedDisplay = if (subject.display.isNullOrEmpty()) {
                null
            } else if (subject.display.size == 1) {
                subject.display.first()
            } else {
                val currentLocale = Locale.getDefault().toString()
                subject.display.firstOrNull { it.locale == currentLocale }
                    ?: subject.display.first()
            }

            IssuerCredentialSubject(
                mandatory = subject.mandatory,
                valueType = subject.valueType,
                display = localizedDisplay?.let { listOf(it) } ?: subject.display
            )
        }
        _credentialSubject.value = localizedMap
    }

    private val _format = MutableLiveData<String>()
    val format: LiveData<String> = _format

    private val _vct = MutableLiveData<String>()
    val vct: LiveData<String> = _vct

    private val _isPinRequired = MutableLiveData<Boolean>()
    val isPinRequired: LiveData<Boolean> = _isPinRequired

    fun checkIfPinIsRequired(credentialOffer: String) {
        val jsonObject = JSONObject(credentialOffer)
        val grants = jsonObject.getJSONObject("grants")
        val preAuthCodeInfo =
            grants.getJSONObject("urn:ietf:params:oauth:grant-type:pre-authorized_code")
        val userPinRequired = preAuthCodeInfo.getBoolean("user_pin_required")

        _isPinRequired.value = userPinRequired
    }

    private val _pinError = MutableLiveData<String?>()
    val pinError: LiveData<String?> = _pinError

    private var credentialIssuer: String = ""
    private var tokenEndpoint: String? = null
    private var credentialEndpoint: String? = null
    private var preAuthCode: String? = null

    //    private var credentialIssuerMetadata: CredentialIssuerMetadata? = null
    private val _credentialIssuerMetadata = MutableLiveData<CredentialIssuerMetadata>()
    val credentialIssuerMetadata: LiveData<CredentialIssuerMetadata> = _credentialIssuerMetadata


    private val _navigateToCertificateFragment = MutableLiveData<Boolean>()
    val navigateToCertificateFragment: LiveData<Boolean> get() = _navigateToCertificateFragment

    suspend fun <T> LiveData<T>.awaitFirstValue(): T {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val observer = object : Observer<T> {
                    override fun onChanged(value: T) {
                        continuation.resume(value)
                        removeObserver(this)
                    }
                }
                observeForever(observer)
                continuation.invokeOnCancellation { removeObserver(observer) }
            }
        }
    }

    //エラー表示用のLiveData
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun fetchMetadata(
        parameterValue: String,
    ) {
        val mapper = jacksonObjectMapper().apply {
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        }
        val credentialOffer = mapper.readValue(parameterValue, CredentialOffer::class.java)
        credentialIssuer = credentialOffer.credentialIssuer
        preAuthCode = credentialOffer.grants?.urnIetfParams?.preAuthorizedCode

        viewModelScope.launch(Dispatchers.IO) {
            processMetadata(credentialIssuer, credentialOffer)
        }
    }

    suspend fun processMetadata(credentialIssuer: String, credentialOffer: CredentialOffer) {
        // 動作確認用
        // val mapper = jacksonObjectMapper()
        // メタデータをJSON文字列からオブジェクトに変換
        // val metadata = mapper.readValue(metaData, CredentialIssuerMetadata::class.java)
        // _credentialIssuerMetadata.postValue(metadata)

        val response = MetadataClient.retrieveAllMetadata(credentialIssuer)
        _credentialIssuerMetadata.postValue(response.credentialIssuerMetadata!!)

        // LiveDataから値を取得
        val currentMetadata = _credentialIssuerMetadata.awaitFirstValue()
        tokenEndpoint = currentMetadata?.tokenEndpoint
        credentialEndpoint = currentMetadata?.credentialEndpoint

        withContext(Dispatchers.Main) {
            currentMetadata?.credentialsSupported?.forEach { (_, credentialSupported) ->
                when (credentialSupported) {
                    is CredentialSupportedJwtVcJson -> {
                        _format.value = "jwt_vc_json"
                        val firstCredential = credentialOffer.credentials.firstOrNull()

                        if (credentialSupported.credentialDefinition.type.contains(firstCredential)) {
                            _vct.value = firstCredential ?: ""
                            setCredentialSubject(
                                credentialSupported.credentialDefinition.credentialSubject
                                    ?: emptyMap()
                            )
                            updateText(currentMetadata, credentialSupported)
                        }
                    }

                    is CredentialSupportedVcSdJwt -> {
                        _format.value = "vc+sd-jwt"
                        val firstCredential = credentialOffer.credentials.firstOrNull()

                        if (credentialSupported.credentialDefinition.vct == firstCredential) {
                            _vct.value = firstCredential ?: ""
                            setCredentialSubject(
                                credentialSupported.credentialDefinition.claims
                            )
                            updateText(currentMetadata, credentialSupported)
                        }
                    }
                }
            }
        }
    }

    private fun extractSDJwtInfo(credential: String, format: String): Map<String, Any> {
        val issuerSignedJwt = credential.split("~")[0]
        return extractInfoFromJwt(issuerSignedJwt, format)
    }

    private fun extractJwtVcJsonInfo(credential: String, format: String): Map<String, Any>{
        return extractInfoFromJwt(credential, format)
    }

    private fun extractInfoFromJwt(jwt: String, format: String): Map<String, Any> {
        val decodedPayload = String(Base64.getUrlDecoder().decode(jwt.split(".")[1]))

        val jwtNode: JsonNode = jacksonObjectMapper().readTree(decodedPayload)

        val iss = jwtNode.get("iss").asText()
        val iat = jwtNode.get("iat").asLong()
        val exp = jwtNode.get("exp").asLong()
        val typeOrVct = when (format) {
            "vc+sd-jwt" -> jwtNode.get("vct").asText() // "vct" を使用
            else -> jwtNode.get("type").asText() // その他のフォーマットでは "type" を使用
        }

        return mapOf(
            "iss" to iss, "iat" to iat, "exp" to exp, "typeOrVct" to typeOrVct
        )
    }


    fun sendRequest(context: Context, userPin: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val vciClient = VCIClient()

            try {
                val tokenResponse =
                    vciClient.postTokenRequest(tokenEndpoint!!, preAuthCode!!, userPin)
                val isProofRequired = tokenResponse?.cNonce != null

                val isKeyPairExist =
                    KeyPairUtil.isKeyPairExist(Constants.KEY_PAIR_ALIAS_FOR_KEY_BINDING)
                if (isProofRequired && !isKeyPairExist) {
                    KeyPairUtil.generateSignVerifyKeyPair(Constants.KEY_PAIR_ALIAS_FOR_KEY_BINDING)
                }

                var proofJwt: Proof? = null
                if (isProofRequired) {
                    proofJwt = Proof(
                        proofType = "jwt",
                        jwt = KeyPairUtil.createProofJwt(
                            Constants.KEY_PAIR_ALIAS_FOR_KEY_BINDING,
                            credentialIssuer,
                            tokenResponse?.cNonce!!))
                }

                var credentialRequest: CredentialRequest? = null
                if (_format.value == "vc+sd-jwt") {
                    credentialRequest = CredentialRequestSdJwtVc(
                        format = format.value!!,
                        credentialDefinition = mapOf("vct" to "${vct.value}"),
                        proof = proofJwt
                    )
                } else {
                    credentialRequest = CredentialRequestJwtVc(
                        format = format.value!!,
                        proof = proofJwt ,
                        credentialDefinition = mapOf(
                            "type" to listOf(vct.value),
                            "credentialSubject" to mapOf<String, Any>()
                            ),
                    )
                }

                val credentialResponse = vciClient.postCredentialRequest(
                    credentialEndpoint!!, credentialRequest, tokenResponse?.accessToken!!
                )

                val basicInfo = credentialResponse?.let {
                    if (_format.value == "vc+sd-jwt")
                        extractSDJwtInfo(it.credential, format.value!!)
                    else
                        extractJwtVcJsonInfo(it.credential, format.value!!)
                }

                val credentialIssuerMetadataStr =
                    jacksonObjectMapper().writeValueAsString(credentialIssuerMetadata.value)


                // Protobufのスキーマを使用してCredentialDataを直接作成
                val vcData = credentialDataStore?.responseToSchema(
                    credentialResponse = credentialResponse!!,
                    credentialBasicInfo = basicInfo!!,
                    credentialIssuerMetadata = credentialIssuerMetadataStr
                )

                if (vcData != null) {
                    credentialDataStore?.saveCredentialData(vcData)
                }
                withContext(Dispatchers.Main) {
                    _navigateToCertificateFragment.value = true
                }
            } catch (e: TokenErrorResponseException) {
                println(e)
                val res = e.errorResponse
                _errorMessage.postValue("${res.error}, ${res.errorDescription} ")
                // todo 内部的なエラー情報としてerror responseを渡して、フラグメント側でstring valuesに事前に用意したエラーメッセージと対応させて表示する
            } catch (e: IOException) {
                // エラー時の処理
                println(e)
                _pinError.postValue("PINコードが間違っています。")
                _errorMessage.postValue("エラーが発生しました: ${e.message}")
            }
        }
    }

    fun resetPinError() {
        _pinError.value = null
    }

    fun resetErrorMessage() {
        _errorMessage.value = null
    }

    fun resetNavigationEvent() {
        _navigateToCertificateFragment.value = false
    }
}