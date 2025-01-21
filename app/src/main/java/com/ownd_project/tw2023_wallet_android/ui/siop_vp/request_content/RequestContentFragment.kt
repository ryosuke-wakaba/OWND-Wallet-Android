package com.ownd_project.tw2023_wallet_android.ui.siop_vp.request_content

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ownd_project.tw2023_wallet_android.MainActivity
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.oid.PostResult
import com.ownd_project.tw2023_wallet_android.ui.shared.CredentialSharingViewModel
import com.ownd_project.tw2023_wallet_android.ui.siop_vp.TokenSharingFragmentMenuProvider
import com.ownd_project.tw2023_wallet_android.utils.DisplayUtil

class RequestContentFragment : Fragment() {
    companion object {
        private val tag = RequestContentFragment::class.simpleName
    }

//    private val binding by viewBinding(FragmentIdTokenSharringBinding::bind)
//    private lateinit var issuerDetailBinding: FragmentIssuerDetailBinding

    private val args: RequestContentFragmentArgs by navArgs()
    private val sharedViewModel by activityViewModels<CredentialSharingViewModel>()
    private val viewModel: IdTokenSharringViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        val rootView = inflater.inflate(R.layout.fragment_id_token_sharring, container, false)

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
        }
        DisplayUtil.setFragmentTitle(
            activity as? AppCompatActivity,
            ""
        )
//        val composeView = rootView.findViewById<ComposeView>(R.id.compose_view)
        return ComposeView(requireContext()).apply {
            viewModel.presentationDefinition.observe(viewLifecycleOwner) {
                sharedViewModel.setPresentationDefinition(it)
            }
            viewModel.subJwk.observe(viewLifecycleOwner) {
                if (!it.isNullOrBlank()) {
                    sharedViewModel.setSubJwk(it)
                }
            }
            setContent {
                RequestContentView(viewModel = viewModel, linkOpener = { url ->
                    val builder = CustomTabsIntent.Builder()
                    val customTabsIntent = builder.build()
                    // Custom Tabs(アプリ内ブラウザ)のキャンセル時にロックさせないため(暫定)
                    // MainActivityのisLockingをfalseにセット
                    (activity as? MainActivity)?.setIsLocking(true)
                    customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
                }, closeHandler = {
                    onUpdateCloseFragment(true)
                }) {
                    sharedViewModel.setRequestInfo(it)
                    val action =
                        RequestContentFragmentDirections.actionIdTokenSharringToFlow2()
                    findNavController().navigate(action)
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
                RequestContentFragmentDirections.actionIdTokenSharringToWebViewFragment(
                    url,
                    cookies
                )
            findNavController().navigate(action)
        }
    }
}