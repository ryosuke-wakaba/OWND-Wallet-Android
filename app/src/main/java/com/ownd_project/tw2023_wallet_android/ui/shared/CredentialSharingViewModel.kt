package com.ownd_project.tw2023_wallet_android.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ownd_project.tw2023_wallet_android.oid.PresentationDefinition
import com.ownd_project.tw2023_wallet_android.oid.SubmissionCredential
import com.ownd_project.tw2023_wallet_android.ui.siop_vp.RequestInfo
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata

class CredentialSharingViewModel: ViewModel() {
    val selectedCredential = MutableLiveData<SubmissionCredential?>()
    val _presentationDefinition = MutableLiveData<PresentationDefinition?>()
    val presentationDefinition: LiveData<PresentationDefinition?> = _presentationDefinition
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

    private val _requestInfo = MutableLiveData<RequestInfo>()
    val requestInfo: LiveData<RequestInfo> = _requestInfo
    public fun setRequestInfo(requestInfo: RequestInfo) {
        _requestInfo.value = requestInfo
    }
}