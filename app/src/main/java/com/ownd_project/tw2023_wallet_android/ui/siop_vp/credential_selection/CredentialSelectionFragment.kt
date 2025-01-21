package com.ownd_project.tw2023_wallet_android.ui.siop_vp.credential_selection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.ui.shared.CredentialSharingViewModel
import com.ownd_project.tw2023_wallet_android.ui.siop_vp.TokenSharingFragmentMenuProvider

class CredentialSelectionFragment : Fragment() {
    val sharedViewModel by activityViewModels<CredentialSharingViewModel>()
    val viewModel: CertificateSelectionViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val activity = requireActivity()
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
        val menuProvider = TokenSharingFragmentMenuProvider(this, activity.menuInflater)
        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val credentialDataStore = CredentialDataStore.getInstance(requireContext())
        viewModel.setCredentialDataStore(credentialDataStore)
        sharedViewModel.presentationDefinition.observe(viewLifecycleOwner) {
            if (it != null) {
                viewModel.getData(it)
            }
        }
        return ComposeView(requireContext()).apply {
            setContent {
                CertificateSelectionView(viewModel = viewModel) { credentialId ->
                    val action =
                        CredentialSelectionFragmentDirections.actionIdTokenSharringToFlow3(
                            credentialId = credentialId
                        )
                    findNavController().navigate(action)
                }
            }
        }
    }
}