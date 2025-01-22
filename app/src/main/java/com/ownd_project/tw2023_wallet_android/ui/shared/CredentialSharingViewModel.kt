package com.ownd_project.tw2023_wallet_android.ui.shared

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ownd_project.tw2023_wallet_android.oid.PresentationDefinition
import com.ownd_project.tw2023_wallet_android.oid.SubmissionCredential
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata

class CredentialSharingViewModel : ViewModel() {
    val selectedCredential = MutableLiveData<SubmissionCredential?>()
    val presentationDefinition = MutableLiveData<PresentationDefinition?>()
    lateinit var credentialIssuerMetadata: CredentialIssuerMetadata
    lateinit var credentialType: String

    fun reset() {
        selectedCredential.value = null
        presentationDefinition.value = null
    }

    fun setSelectedCredential(
        type: String,
        data: SubmissionCredential,
        metaData: CredentialIssuerMetadata
    ) {
        credentialType = type
        selectedCredential.value = data
        credentialIssuerMetadata = metaData
    }

    fun setPresentationDefinition(data: PresentationDefinition) {
        presentationDefinition.value = data
    }
}