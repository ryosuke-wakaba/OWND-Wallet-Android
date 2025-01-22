package com.ownd_project.tw2023_wallet_android.ui.issuer_detail

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.FragmentIssuerDetailBinding
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.utils.DisplayUtil
import com.ownd_project.tw2023_wallet_android.utils.viewBinding
import com.fasterxml.jackson.databind.ObjectMapper
import com.ownd_project.tw2023_wallet_android.utils.CertificateUtil

class IssuerDetailFragment : Fragment(R.layout.fragment_issuer_detail) {
    private val args: IssuerDetailFragmentArgs by navArgs()

    // viewModeのインスタンスを生成
    private var viewModelFactory: ViewModelProvider.Factory? = null
    private val viewModel: IssuerDetailViewModel by viewModels {
        viewModelFactory ?: IssuerDetailViewModelFactory(
            CredentialDataStore.getInstance(requireContext())
        )
    }

    // viewBindingを使用してインスタンスを初期化
    private val binding by viewBinding(FragmentIssuerDetailBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        val menuProvider = IssuerDetailFragmentMenuProvider(this, activity.menuInflater)
        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // タイトルに表示するテキストをセット
        DisplayUtil.setFragmentTitle(
            activity as? AppCompatActivity,
            getString(R.string.title_issuer_detail)
        )

        val credentialId = args.credentialId
        // ここでcredentialIdを使って必要な処理を行う
        viewModel.getCredentialIssuerMetadataById(credentialId)
            .observe(viewLifecycleOwner) { metadata ->
                // ロゴがあればImageViewにセット、なければ非表示にする
                val logoUrl = metadata?.display?.firstOrNull()?.logo?.uri
                if (!logoUrl.isNullOrEmpty()) {
                    Glide.with(this).load(logoUrl).into(binding.ivIssuerLogo)
                } else {
                    // ロゴがない場合はImageViewを隠す
                    binding.ivIssuerLogo.visibility = View.GONE
                }
                val issuerName = metadata?.display?.firstOrNull()?.name
                if (!issuerName.isNullOrEmpty()) {
                    binding.tvIssuerName.text = issuerName
                } else {
                    // Todo 必要に応じて文言を変更
                    binding.tvIssuerName.text = "表示すべき発行者名がありません"
                }
                // LiveDataの監視を開始して証明書のデータを取得
                viewModel.certificatesLiveData.observe(viewLifecycleOwner) { certificates ->
                    if (certificates!!.isNotEmpty()) {
                        val certificate =
                            CertificateUtil.x509Certificate2CertificateInfo(certificates[0])

                        val org = certificate.issuer?.organization
                        val verifiedByText = getString(R.string.verified_by_format, org)
                        binding.tvVerifierText.text = verifiedByText

                        // 所在地、国名、ドメインを設定
                        val st = certificate.state
                        val l = certificate.locality ?: ""
                        val street = certificate.street ?: ""
                        val locationText = getString(R.string.location_format, st, l, street)
                        binding.labelLocation.text = locationText


                        binding.labelCountry.text = certificate.country
                        val domain = certificate.domain ?: ""
                        binding.labelDomain.text =
                            if (domain.isNotEmpty()) "https://$domain" else metadata?.credentialIssuer
                        // 以下はcertificatesがから出ない場合のみ表示するもの
                        binding.ivVerifierMark.visibility = View.VISIBLE
                        binding.tvVerifierText.visibility = View.VISIBLE
                        binding.issuerDetailsLayout.visibility = View.VISIBLE
                        binding.subTextLocation.visibility = View.VISIBLE
                        binding.labelLocation.visibility = View.VISIBLE
                        binding.subTextCountry.visibility = View.VISIBLE
                        binding.labelCountry.visibility = View.VISIBLE
                    } else {
                        // certificatesが空の場合、metadataからissを使用
                        binding.labelDomain.text = metadata?.credentialIssuer ?: ""
                    }
                }

            }
        binding.tvVerifierText.setOnClickListener {
            // フラグメントの遷移や他のアクションを実行
        }
    }
}

class IssuerDetailViewModelFactory(
    private val credentialDataStore: CredentialDataStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IssuerDetailViewModel::class.java)) {
            return IssuerDetailViewModel(
                credentialDataStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
