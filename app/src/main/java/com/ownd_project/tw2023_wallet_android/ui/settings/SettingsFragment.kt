package com.ownd_project.tw2023_wallet_android.ui.settings

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.FragmentSettingsBinding
import com.ownd_project.tw2023_wallet_android.utils.DisplayUtil

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // タイトルに表示するテキストをセット
        DisplayUtil.setFragmentTitle(
            activity as? AppCompatActivity, getString(R.string.title_settings)
        )

        binding.linkTextBackup.setOnClickListener {
            findNavController().navigate(R.id.backupFragment)
        }
//        binding.linkTextOwnedMessenger.setOnClickListener {
//            val url = getString(R.string.MESSENGER_URL)
//            openUrlInCustomTab(url)
//        }
        binding.linkTextTermsOfUse.setOnClickListener {
            val url = getString(R.string.TERM_OF_USE_URL)
            openUrlInCustomTab(url)
        }
        binding.linkTextPrivacyPolicy.setOnClickListener {
            val url = getString(R.string.PRIVACY_POLICY_URL)
            openUrlInCustomTab(url)
        }
//        binding.linkTextSupport.setOnClickListener {
//            val url = getString(R.string.SUPPORT_URL)
//            openUrlInCustomTab(url)
//
//        binding.linkTextLicense.setOnClickListener {
//            val url = getString(R.string.LICENCE_URL)
//            openUrlInCustomTab(url)
//        }

        // アプリのバージョン名をテキストビューに設定
//        binding.textVersionName.text = "v." + BuildConfig.VERSION_NAME
        // アプリのバージョン名をテキストビューに設定
        val versionText = getString(
            R.string.version_text,
            com.ownd_project.tw2023_wallet_android.BuildConfig.VERSION_NAME
        )
        binding.textVersionName.text = versionText


        return root
    }


    private fun openUrlInCustomTab(url: String) {
        Log.d("SettingsFragment", "Opening URL in Custom Tab: $url")
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}