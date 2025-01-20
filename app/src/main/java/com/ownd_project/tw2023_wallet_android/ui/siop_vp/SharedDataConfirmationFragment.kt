package com.ownd_project.tw2023_wallet_android.ui.siop_vp

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ownd_project.tw2023_wallet_android.MainActivity
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.oid.PostResult
import com.ownd_project.tw2023_wallet_android.oid.SubmissionCredential
import com.ownd_project.tw2023_wallet_android.ui.shared.CredentialSharingViewModel

class SharedDataConfirmationFragment : Fragment() {

    companion object {
        val tag = SharedDataConfirmationFragment::class.simpleName
    }

    private val args: SharedDataConfirmationFragmentArgs by navArgs()
    val sharedViewModel by activityViewModels<CredentialSharingViewModel>()
    val viewModel: SharedDataConfirmationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val activity = requireActivity()
        val menuProvider = TokenSharingFragmentMenuProvider(this, activity.menuInflater)
        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewModel.postResult.observe(viewLifecycleOwner, ::onPostResult)
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.resetErrorMessage()
            }
        }
        viewModel.doneSuccessfully.observe(viewLifecycleOwner, ::onUpdateProcessCompletion)

        val credentialDataStore = CredentialDataStore.getInstance(requireContext())
        viewModel.setCredentialDataStore(credentialDataStore)

        return ComposeView(requireContext()).apply {
            val credentialId = args.credentialId
            Log.d(SharedDataConfirmationFragment.tag, "credentialId:$credentialId")
            sharedViewModel.requestInfo.observe(viewLifecycleOwner) {
                viewModel.setRequestInfo(it)
            }
            sharedViewModel.subJwk.observe(viewLifecycleOwner) {
                if (!it.isNullOrBlank()) {
                    viewModel.setSubJwk(it)
                }
            }
            sharedViewModel.presentationDefinition.observe(viewLifecycleOwner) { it ->
                if (it != null) {
                    if (credentialId != null) {
                        viewModel.getData(credentialId, it)
                    } else {
                        viewModel.setEmptyClaims()
                    }
                }
            }
            setContent {
                SharedDataConfirmationView(
                    viewModel = viewModel,
                    linkOpener = { url ->
                        val builder = CustomTabsIntent.Builder()
                        val customTabsIntent = builder.build()
                        // Custom Tabs(アプリ内ブラウザ)のキャンセル時にロックさせないため(暫定)
                        // MainActivityのisLockingをfalseにセット
                        (activity as? MainActivity)?.setIsLocking(true)
                        customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
                    }) { selected ->
                    Log.d(SharedDataConfirmationFragment.tag, "size:${selected.size}")
                    viewModel.shareVpToken(selected)
                }
            }
        }
    }

    private fun onUpdateProcessCompletion(done: Boolean) {
        if (done) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(getString(R.string.sharing_credential_done))
            builder.setMessage(getString(R.string.sharing_credential_done_support_text))
            builder.setPositiveButton(R.string.close) { dialog, id ->
                onUpdateCloseFragment(true)
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    private fun onUpdateCloseFragment(close: Boolean) {
        if (close) {
            requireActivity().finish()
        }
    }

    private fun onPostResult(postResult: PostResult) {
        if (postResult.location != null) {
            val url = postResult.location
            // todo
//            val action =
//                TokenSharingFragmentDirections.actionIdTokenSharringToWebViewFragment(url, cookies)
//            findNavController().navigate(action)
        }
    }
}
