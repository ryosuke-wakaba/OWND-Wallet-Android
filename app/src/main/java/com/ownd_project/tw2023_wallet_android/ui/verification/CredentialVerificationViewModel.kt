package com.ownd_project.tw2023_wallet_android.ui.verification

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ownd_project.tw2023_wallet_android.signature.JWT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CredentialVerificationViewModel : ViewModel() {
    companion object {
        val TAG = CredentialVerificationViewModel::class.simpleName
    }

    private val _claims = MutableLiveData<List<Pair<String, String>>>()
    val claims: LiveData<List<Pair<String, String>>> = _claims

    fun updateClaims(newClaims: List<Pair<String, String>>) {
        _claims.value = newClaims
    }

    // 検証完了通知
    private val _initDone = MutableLiveData<Boolean>()
    val initDone: LiveData<Boolean> = _initDone

    private val _result = MutableLiveData<Boolean>()
    val result: LiveData<Boolean> = _result

    fun verifyCredential(format: String, credential: String) {
        Log.d(com.ownd_project.tw2023_wallet_android.ui.siop_vp.request_content.TAG, "verifyCredential seed successfully")
        viewModelScope.launch(Dispatchers.IO) {
            val result = JWT.verifyJwtByX5U(credential)
            result.fold(
                ifLeft = {
                    // todo error handling
                    println("error")
                    withContext(Dispatchers.Main) {
                        _result.value = false
                        _initDone.value = true
                    }
                },
                ifRight = {
                    if (format == "jwt_vc_json") {
                        val vc = it.getClaim("vc").asMap()
                        println("vc: $vc")
                        val credentialSubjectMap = requireNotNull(vc["credentialSubject"]) as Map<String, String>
                        val pairList = credentialSubjectMap.map { (key, value) -> key to value }
                        println(pairList)

                        withContext(Dispatchers.Main) {
                            updateClaims(pairList)
                            _result.value = true
                            _initDone.value = true
                        }
                    } else if (format == "vc+sd-jwt") {
                        // todo implement
                    } else {
                        // todo implement
                    }
                }
            )
        }
    }
}