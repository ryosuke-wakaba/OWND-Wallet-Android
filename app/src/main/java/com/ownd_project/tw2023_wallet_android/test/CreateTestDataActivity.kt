package com.ownd_project.tw2023_wallet_android.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.TestCredentialDetailActivityBinding
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistoryStore
import com.ownd_project.tw2023_wallet_android.test.DummyData.generateHistoryData
import com.ownd_project.tw2023_wallet_android.test.DummyData.generateJwtVcCredentialData
import com.ownd_project.tw2023_wallet_android.test.DummyData.generateSdJwtCredentialData
import com.ownd_project.tw2023_wallet_android.test.DummyData.resetDataStore
import kotlinx.coroutines.launch

class CreateTestDataActivity : AppCompatActivity() {
    val credentialDataStore = CredentialDataStore.getInstance(this)
    val historyStore = CredentialSharingHistoryStore.getInstance(this)
    private val x5uJwt =
        "eyJraWQiOiJodHRwOi8vdW5pdmVyc2l0eS5leGFtcGxlL2NyZWRlbnRpYWxzLzM3MzIiLCJ4NXUiOiJodHRwOi8vMTAuMC4yLjI6ODA4MC90ZXN0LWNlcnRpZmljYXRlIiwiYWxnIjoiRVMyNTYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL3VuaXZlcnNpdHkuZXhhbXBsZS9pc3N1ZXJzLzU2NTA0OSIsInN1YiI6ImRpZDpleGFtcGxlOmViZmViMWY3MTJlYmM2ZjFjMjc2ZTEyZWMyMSIsInZjIjp7IkBjb250ZXh0IjpbImh0dHBzOi8vd3d3LnczLm9yZy9ucy9jcmVkZW50aWFscy92MiIsImh0dHBzOi8vd3d3LnczLm9yZy9ucy9jcmVkZW50aWFscy9leGFtcGxlcy92MiJdLCJpZCI6Imh0dHA6Ly91bml2ZXJzaXR5LmV4YW1wbGUvY3JlZGVudGlhbHMvMzczMiIsInR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJFeGFtcGxlRGVncmVlQ3JlZGVudGlhbCJdLCJpc3N1ZXIiOiJodHRwczovL3VuaXZlcnNpdHkuZXhhbXBsZS9pc3N1ZXJzLzU2NTA0OSIsInZhbGlkRnJvbSI6IjIwMTAtMDEtMDFUMDA6MDA6MDBaIiwiY3JlZGVudGlhbFN1YmplY3QiOnsiaWQiOiJkaWQ6ZXhhbXBsZTplYmZlYjFmNzEyZWJjNmYxYzI3NmUxMmVjMjEiLCJuYW1lIjoiU2FtcGxlIEV2ZW50IEFCQyIsImRhdGUiOiIyMDI0LTAxLTI0VDAwOjAwOjAwWiJ9fSwiaWF0IjoxNzAyNTM0MDMwfQ.DoMHojQUGoixFV8bwdjCDIb9sm2QKOG-AhmpdG8I-pNhTTlos9pvJ6YchnoPylpZngvFCb_WQaSd9tmGiHN_Mg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val credentialID = "testCredentialID"
        lifecycleScope.launch {
            resetDataStore(credentialDataStore, historyStore)
            generateJwtVcCredentialData(credentialDataStore)
            generateSdJwtCredentialData(credentialDataStore)
            generateHistoryData(historyStore)
        }

        val binding = TestCredentialDetailActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_credential_detail_test) as NavHostFragment
        val navController = navHostFragment.navController

        val arguments = Bundle().apply {
            putString("credentialId", credentialID)
        }
        navController.navigate(R.id.navigation_certificate, arguments)

    }
}
