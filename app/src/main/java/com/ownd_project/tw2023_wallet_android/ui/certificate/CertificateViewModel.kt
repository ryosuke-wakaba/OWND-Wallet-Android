package com.ownd_project.tw2023_wallet_android.ui.certificate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import kotlinx.coroutines.launch

class CertificateViewModel(private val credentialDataStore: CredentialDataStore) : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "証明書がありません"
    }
    val text: LiveData<String> = _text

    // リストデータを保持するLiveData
    private val _credentialDataList = MutableLiveData<com.ownd_project.tw2023_wallet_android.datastore.CredentialDataList?>()
    val credentialDataList: LiveData<com.ownd_project.tw2023_wallet_android.datastore.CredentialDataList?> = _credentialDataList

    private fun setCredentialData(schema: com.ownd_project.tw2023_wallet_android.datastore.CredentialDataList) {
        _credentialDataList.value = schema
    }

    init {
        viewModelScope.launch {
            credentialDataStore.credentialDataListFlow.collect { schema ->
                setCredentialData(schema)
            }
        }
    }
}