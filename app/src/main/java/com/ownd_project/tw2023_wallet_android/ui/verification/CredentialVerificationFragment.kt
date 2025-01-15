package com.ownd_project.tw2023_wallet_android.ui.verification

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.FragmentCredentialVerificationBinding
import com.ownd_project.tw2023_wallet_android.utils.DisplayUtil
import com.ownd_project.tw2023_wallet_android.utils.MetadataUtil
import com.ownd_project.tw2023_wallet_android.utils.viewBinding

class CredentialVerificationFragment : Fragment(R.layout.fragment_credential_verification) {
    companion object {
        private val TAG = CredentialVerificationFragment::class.simpleName
    }

    private val args: CredentialVerificationFragmentArgs by navArgs()

    private val binding by viewBinding(FragmentCredentialVerificationBinding::bind)
    private val viewModel: CredentialVerificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        // プログレスインジケーター表示
        binding.progressOverlay.visibility = View.VISIBLE

        // アクションバーの設定
        val activity = requireActivity()
        val menuProvider = CredentialVerificationFragmentMenuProvider(this, activity.menuInflater)
        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        DisplayUtil.setFragmentTitle(
            activity as? AppCompatActivity,
            getString(R.string.credential_verification_result)
        )

        // クレデンシャル検証
        val displayMap = MetadataUtil.deserializeDisplayByClaimMap(args.display)
        viewModel.verifyCredential(args.format, args.credential)

        viewModel.initDone.observe(viewLifecycleOwner, ::onInitDone)
        viewModel.result.observe(viewLifecycleOwner, ::onUpdateResult)

        viewModel.claims.observe(viewLifecycleOwner) {
            val detailsContainer = binding.detailsContainer
            for ((title, date) in it) {
                // タイトル用のTextViewを作成してLinearLayoutに追加
                val titleTextView = TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also {
                        val density = resources.displayMetrics.density
                        it.topMargin = (32 * density).toInt()
                    }
                    text = displayMap[title]?.get(0)?.name ?: title // todo ロケール見て切り替える
                    setTextAppearance(R.style.text_sub_text)
                }
                detailsContainer.addView(titleTextView)

                // 日付用のTextViewを作成してLinearLayoutに追加
                val dateTextView = TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = date
                    setTextAppearance(R.style.text_label_m)
                }
                detailsContainer.addView(dateTextView)
            }
        }
    }

    private fun onUpdateResult(result: Boolean) {
        if (result) {
            binding.invalidBox.visibility = View.GONE
            binding.invalidCredential.visibility = View.GONE
        } else {
            binding.confirmationBox.visibility = View.GONE
            binding.detailsContainer.visibility = View.GONE
        }
    }

    private fun onInitDone(done: Boolean) {
        if (done) {
            binding.progressOverlay.visibility = View.GONE
        }
    }
}