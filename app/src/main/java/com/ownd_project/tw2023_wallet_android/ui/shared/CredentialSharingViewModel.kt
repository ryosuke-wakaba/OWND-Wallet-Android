package com.ownd_project.tw2023_wallet_android.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ownd_project.tw2023_wallet_android.oid.PresentationDefinition
import com.ownd_project.tw2023_wallet_android.oid.SubmissionCredential
import com.ownd_project.tw2023_wallet_android.ui.siop_vp.request_content.RequestInfo
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata

class CredentialSharingViewModel: ViewModel() {
    val selectedCredential = MutableLiveData<SubmissionCredential?>()

    val _presentationDefinition = MutableLiveData<PresentationDefinition?>()
    val presentationDefinition: LiveData<PresentationDefinition?> = _presentationDefinition

    private val _subJwk = MutableLiveData<String?>()
    val subJwk: LiveData<String?> = _subJwk

    lateinit var credentialIssuerMetadata: CredentialIssuerMetadata
    lateinit var credentialType: String

    fun reset() {
        selectedCredential.value = null
        // presentationDefinition.value = null
        setPresentationDefinition(null)
    }

    fun setSelectedCredential(type: String, data: SubmissionCredential, metaData: CredentialIssuerMetadata) {
        credentialType = type
        selectedCredential.value = data
        credentialIssuerMetadata = metaData
    }

    fun setPresentationDefinition(data: PresentationDefinition?) {
        _presentationDefinition.value = data
    }

    fun setSubJwk(value: String) {
        _subJwk.value = value
    }

    private val _requestInfo = MutableLiveData<RequestInfo>()
    val requestInfo: LiveData<RequestInfo> = _requestInfo
    public fun setRequestInfo(requestInfo: RequestInfo) {
        _requestInfo.value = requestInfo
    }
}