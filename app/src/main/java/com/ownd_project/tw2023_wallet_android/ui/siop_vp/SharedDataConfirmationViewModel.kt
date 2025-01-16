package com.ownd_project.tw2023_wallet_android.ui.siop_vp

import android.credentials.Credential
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ownd_project.tw2023_wallet_android.datastore.CredentialData
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.oid.InputDescriptor
import com.ownd_project.tw2023_wallet_android.oid.OpenIdProvider
import com.ownd_project.tw2023_wallet_android.oid.PostResult
import com.ownd_project.tw2023_wallet_android.oid.PresentationDefinition
import com.ownd_project.tw2023_wallet_android.oid.RequestedClaim
import com.ownd_project.tw2023_wallet_android.oid.SubmissionCredential
import com.ownd_project.tw2023_wallet_android.utils.MetadataUtil
import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil
import kotlinx.coroutines.launch

class SharedDataConfirmationViewModel : ViewModel() {

    // todo viewのオプショナル情報つきクレデンシャルリストのアクセッサ
    // todo viewのrequestInfoのアクセッサ
    private val _doneSuccessfully = MutableLiveData<Boolean>()
    val doneSuccessfully: LiveData<Boolean> = _doneSuccessfully

    private val _postResult = MutableLiveData<PostResult>()
    val postResult: LiveData<PostResult> = _postResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var _credentialDataStore: CredentialDataStore? = null
    open fun setCredentialDataStore(credentialDataStore: CredentialDataStore) {
        _credentialDataStore = credentialDataStore
    }

    private val _requestInfo = MutableLiveData<RequestInfo>()
    val requestInfo: LiveData<RequestInfo> = _requestInfo
    public fun setRequestInfo(requestInfo: RequestInfo) {
        _requestInfo.value = requestInfo
    }

    private var _credential: CredentialData? = null
    private var _inputDescriptor: InputDescriptor? = null

    private val _claims = MutableLiveData<List<RequestedClaim>?>()
    val claims: LiveData<List<RequestedClaim>?> = _claims

    fun resetErrorMessage() {
        _errorMessage.value = null
    }

    open fun getData(credentialId: String, presentationDefinition: PresentationDefinition) {
        viewModelScope.launch {
            _credentialDataStore?.credentialDataListFlow?.collect { schema ->
                val items = schema?.itemsList
                if (items != null) {
                    val cred = items.find {
                        it.id == credentialId
                    }
                    if (cred != null) {
                        _credential = cred
                        if (cred.format == "vc+sd-jwt") {
                            val selectedClaims = OpenIdProvider.selectRequestedClaims(cred.credential, presentationDefinition.inputDescriptors)
                            if (selectedClaims.satisfied) {
                                _inputDescriptor = selectedClaims.matchedInputDescriptor
                                _claims.value = selectedClaims.selectedClaims
                            }
                            // val ret = OpenIdProvider.selectDisclosure(cred.credential, presentationDefinition)
//                            ret?.let { (inputDescriptor, closures) ->
//                                _inputDescriptor = inputDescriptor
//                                val optionalFields = inputDescriptor.constraints.fields?.filter { it.optional == true }
//                                val claims = closures.map { claim -> mapOf(claim.key to claim.value) }
//                            }
//                            val rt2 = ret
                        }
                    }
                }
            }
        }
    }

    fun shareVpToken(selectedDisclosures: List<SDJwtUtil.Disclosure>) {
        _credential?.let {
            val types = MetadataUtil.extractTypes(it.format, it.credential)
            val sc = SubmissionCredential(
                id = it.id,
                format = it.format,
                types = types,
                credential = it.credential,
                inputDescriptor = _inputDescriptor!!,
                selectedDisclosures = selectedDisclosures
            )
            // todo call original viewModel.shareVpToken(this, listOf(selectedCredential))
            _doneSuccessfully.value = true
            // todo _postResult.value = postResult
        }
    }
}

data class ClaimWrapper (
    var claim: SDJwtUtil.Disclosure,
    var optional:Boolean
)