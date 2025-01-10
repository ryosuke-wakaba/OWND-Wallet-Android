package com.ownd_project.tw2023_wallet_android.ui.siop_vp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore

class CertificateSelectionFragment : Fragment() {
    val viewModel: CertificateSelectionViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val credentialDataStore = CredentialDataStore.getInstance(requireContext())
        viewModel.setCredentialDataStore(credentialDataStore)
        viewModel.getData()
        return ComposeView(requireContext()).apply {
            setContent {
                CertificateSelectionView(viewModel = viewModel) {
                    // todo move to next view
                }
            }
        }
    }
}