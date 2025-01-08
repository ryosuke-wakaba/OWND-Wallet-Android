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
import androidx.compose.ui.platform.ComposeView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ownd_project.tw2023_wallet_android.MainActivity
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.FragmentIdTokenSharringBinding
import com.ownd_project.tw2023_wallet_android.databinding.FragmentIssuerDetailBinding
import com.ownd_project.tw2023_wallet_android.model.ClientInfo
import com.ownd_project.tw2023_wallet_android.oid.PostResult
import com.ownd_project.tw2023_wallet_android.oid.PresentationDefinition
import com.ownd_project.tw2023_wallet_android.oid.SubmissionCredential
import com.ownd_project.tw2023_wallet_android.ui.shared.CredentialSharingViewModel
import com.ownd_project.tw2023_wallet_android.utils.MetadataUtil
import com.ownd_project.tw2023_wallet_android.utils.viewBinding


// todo レイアウト調整
// todo SVGをURLから表示

class TokenSharingFragment : Fragment() {
    companion object {
        private val tag = TokenSharingFragment::class.simpleName
    }

//    private val binding by viewBinding(FragmentIdTokenSharringBinding::bind)
//    private lateinit var issuerDetailBinding: FragmentIssuerDetailBinding

    private val args: TokenSharingFragmentArgs by navArgs()
    private val sharedViewModel by activityViewModels<CredentialSharingViewModel>()
    private val viewModel: IdTokenSharringViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                RequestContentView(viewModel = viewModel, linkOpener = { url ->
                    val builder = CustomTabsIntent.Builder()
                    val customTabsIntent = builder.build()
                    // Custom Tabs(アプリ内ブラウザ)のキャンセル時にロックさせないため(暫定)
                    // MainActivityのisLockingをfalseにセット
                    (activity as? MainActivity)?.setIsLocking(true)
                    customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
                }) {
                    // todo move to next view
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        val menuProvider = TokenSharingFragmentMenuProvider(this, activity.menuInflater)
        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

//        viewModel.initDone.observe(viewLifecycleOwner, ::onInitDone)
        viewModel.postResult.observe(viewLifecycleOwner, ::onPostResult)

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.resetErrorMessage()
            }
        }

        if (!viewModel.isInitialized) {
            // 初期化処理を行う
            viewModel.isInitialized = true
            val url = args.siopRequest
            val index = args.index
            viewModel.accessPairwiseAccountManager(this, url, index)
        } else {
            Log.d(tag, "onViewCreated finish")
        }

        // 画面クローズ要求処理
        viewModel.shouldClose.observe(viewLifecycleOwner, ::onUpdateCloseFragment)

        // 処理成功通知
        viewModel.doneSuccessfully.observe(viewLifecycleOwner, ::onUpdateProcessCompletion)
    }

//
//    private fun onOpenSelectCredential(view: View) {
//        Log.d(tag, "on click")
//        val action = TokenSharingFragmentDirections.actionIdTokenSharringToNavigationCertificate()
//        findNavController().navigate(action)
//    }

    private fun onUpdateProcessCompletion(done: Boolean) {
        if (done) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(getString(R.string.sharing_credential_done))
            builder.setMessage(getString(R.string.sharing_credential_done_support_text))
            builder.setPositiveButton(R.string.close) { dialog, id ->
                sharedViewModel.reset()
                onUpdateCloseFragment(true)
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    private fun onUpdateCloseFragment(close: Boolean) {
        // フラグメントを終了させる処理
        // 例: FragmentManagerを使用してフラグメントを削除
        if (close) {
//            parentFragmentManager.beginTransaction().remove(this).commit()
            requireActivity().finish()
        }
    }

//    private fun onInitDone(done: Boolean) {
//        if (done) {
//            binding.progressOverlay.visibility = View.GONE
//        }
//    }

    private fun onPostResult(postResult: PostResult) {
        if (postResult.location != null) {
            val url = postResult.location
            val cookies = postResult.cookies
            val action =
                TokenSharingFragmentDirections.actionIdTokenSharringToWebViewFragment(url, cookies)
            findNavController().navigate(action)
        }
    }
}