package com.ownd_project.tw2023_wallet_android.ui.issuer_detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import com.ownd_project.tw2023_wallet_android.vci.CredentialsSupportedDisplay
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.signature.SignatureUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.cert.X509Certificate
import java.util.Base64

data class IssuerMetadata(
    val credentialIssuer: String?,
    val display: List<CredentialsSupportedDisplay>?,
)

class IssuerDetailViewModel(
    private val credentialDataStore: CredentialDataStore,
) : ViewModel() {
    private val _certificatesLiveData = MutableLiveData<List<X509Certificate>?>()
    private val objectMapper = jacksonObjectMapper()
    val certificatesLiveData: MutableLiveData<List<X509Certificate>?> = _certificatesLiveData

    fun getCredentialIssuerMetadataById(id: String): LiveData<IssuerMetadata?> {
        return liveData {
            val credentialData = credentialDataStore.getCredentialById(id)
            val metadata = credentialData?.credentialIssuerMetadata?.let {
                objectMapper.readValue(it, CredentialIssuerMetadata::class.java)
            }
            emit(IssuerMetadata(metadata?.credentialIssuer, metadata?.display))


            credentialData?.credential?.let { jwt ->
                if (credentialData?.format == "vc+sd-jwt") {
                    val decodedJwtHeader = SDJwtUtil.getDecodedJwtHeader(jwt)
                    if (decodedJwtHeader == null) {
                        println("Failed to decode issuer-signed JWT")
                        _certificatesLiveData.postValue(emptyList())
                    } else {
                        val certificates = SDJwtUtil.getX509CertificatesFromJwt(decodedJwtHeader)
                        if (certificates.isNullOrEmpty()) {
                            println("No certificates found in JWT")
                            _certificatesLiveData.postValue(emptyList())
                        } else {
                            val isTestEnvironment =
                                System.getProperty("isTestEnvironment")?.toBoolean() ?: false
                            val b = if (isTestEnvironment) {
                                SignatureUtil.validateCertificateChain(
                                    certificates.toTypedArray(),
                                    certificates.last()
                                )
                            } else {
                                SignatureUtil.validateCertificateChain(certificates.toTypedArray())
                            }
                            if (b) {
                                println("validateCertificateChain success")
                                // 検証できたデータをUI側に渡す
                                _certificatesLiveData.postValue(certificates)
                            }
                        }
                    }
                } else {
                    val parts = jwt.split(".")
                    if (parts.size != 3) {
                        throw IllegalArgumentException("Invalid JWT token format")
                    }
                    val header = String(Base64.getUrlDecoder().decode(parts[0]), Charsets.UTF_8)
                    val jsonHeader = JSONObject(header)
                    val x5uValue = jsonHeader.optString("x5u", null)
                    if (x5uValue != null) {
                        println("x5u URL: $x5uValue")
                        // ここでは証明書のチェーンだけ検証する
                        // JWT.verifyJwtByX5U(jwt)
                        viewModelScope.launch(Dispatchers.IO) {
                            val certificates = SignatureUtil.getX509CertificatesFromUrl(x5uValue)
                            if (!certificates.isNullOrEmpty()) {
                                val isTestEnvironment =
                                    System.getProperty("isTestEnvironment")?.toBoolean() ?: false
                                val b = if (isTestEnvironment) {
                                    SignatureUtil.validateCertificateChain(
                                        certificates,
                                        certificates.last()
                                    )
                                } else {
                                    SignatureUtil.validateCertificateChain(certificates)
                                }
                                if (b) {
                                    println("validateCertificateChain success")
                                    // 検証できたデータをUI側に渡す
                                    viewModelScope.launch(Dispatchers.Main) {
                                        _certificatesLiveData.postValue(certificates.toList())
                                    }
                                }
                            }
                        }
                    } else {
                        println("x5u property is not present in the JWT header")
                    }
                }
            }
        }
    }
}