package com.ownd_project.tw2023_wallet_android.ui.confirmation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.FragmentConfirmationBinding
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.ui.tx_code_input.TxCodeInputBottomSheetFragment
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import com.ownd_project.tw2023_wallet_android.vci.IssuerCredentialSubjectMap
import java.util.Locale

class ConfirmationFragment : Fragment() {
    private var _binding: FragmentConfirmationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // キャンセルリンクテキストを調整するため、アクションバーを非表示にする
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        // ConfirmationViewModelのインスタンスを作成
        val viewModel = ViewModelProvider(this)[ConfirmationViewModel::class.java]
        _binding = FragmentConfirmationBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textConfirmation
        viewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // CredentialDataStoreのインスタンスをViewModelにセット
        viewModel.setCredentialDataStore(CredentialDataStore.getInstance(requireContext()))

        // Observe the credentialSubject LiveData
        viewModel.credentialSubject.observe(viewLifecycleOwner) { credentialSubjectMap ->
            updateUI(credentialSubjectMap)
        }
        viewModel.navigateToCertificateFragment.observe(viewLifecycleOwner,
            Observer { shouldNavigate ->
                if (shouldNavigate) {
                    // ナビゲーションを実行
                    findNavController().navigate(R.id.navigation_certificate)
                    // ナビゲーションが終わったらすぐに値をリセットして二重遷移を防ぐ
                    viewModel.resetNavigationEvent()
                }
            })

        viewModel.credentialIssuerMetadata.observe(viewLifecycleOwner) { metadata ->
            updateIssuerInfo(metadata)
        }

        // Get the passed parameterValue
        val parameterValue = arguments?.getString("parameterValue")
        parameterValue?.let {
            viewModel.fetchMetadata(it)
            viewModel.checkIfTxCodeIsRequired(it)
        }

        binding.buttonIssue.setOnClickListener {
            viewModel.isTxCodeRequired.observe(viewLifecycleOwner) { isTxCodeRequired ->
                if (isTxCodeRequired) {
                    // PinInputBottomSheetFragmentを表示する
                    val txCodeInputFragment =
                        TxCodeInputBottomSheetFragment.newInstance(parameterValue!!)
                    txCodeInputFragment.listener =
                        object : TxCodeInputBottomSheetFragment.TxCodeInputListener {
                            override fun onTxCodeEntered(txCode: String) {
                                viewModel.sendRequest(requireContext(), txCode)
                            }
                        }
                    txCodeInputFragment.show(parentFragmentManager, "TxCodeInputBottomSheet")
                } else {
                    // PINコードが不要な場合、通常の処理を行う
                    viewModel.sendRequest(requireContext())
                }
            }
        }
        // キャンセルリンクのクリックリスナーを設定
        binding.tvCancel.setOnClickListener {
            handleCancel()
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.resetErrorMessage() // エラーメッセージをリセット
            }
        }

        return root
    }

    private fun handleCancel() {
        // キャンセルボタンが押された時の処理
        findNavController().popBackStack()
    }

    private fun updateUI(credentialSubjectMap: IssuerCredentialSubjectMap) {
        val displayNames = credentialSubjectMap.values.flatMap { subject ->
            subject.display?.mapNotNull { it.name } ?: emptyList()
        }
        val recyclerView: RecyclerView = binding.claimRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        val headerText = "発行する情報の項目"
        recyclerView.adapter = DisplayNameAdapter(displayNames, headerText)
    }

    private fun updateIssuerInfo(metadata: CredentialIssuerMetadata) {
        val currentLocale = Locale.getDefault().toString()
        val issuerDisplay = metadata.display?.firstOrNull { it.locale == currentLocale }
        binding.tvIssuerName.text = issuerDisplay?.name
        if (issuerDisplay?.logo?.uri != null) {
            // 画像のロード (Glideなどのライブラリを使用)
            Glide.with(this).load(issuerDisplay.logo.uri).into(binding.ivIssuerLogo)
            binding.ivIssuerLogo.visibility = View.VISIBLE
        }
        binding.issuerDomain.text = metadata.credentialIssuer
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }
}

class DisplayNameAdapter(private val names: List<String>, private val headerText: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class NameViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.display_name_header)
    }


    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.display_name_header, parent, false) as TextView
            )

            else -> NameViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.display_name_list, parent, false) as TextView
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_ITEM
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_HEADER -> {
                val headerViewHolder = holder as HeaderViewHolder
                headerViewHolder.textView.text = headerText
            }

            TYPE_ITEM -> {
                val nameViewHolder = holder as NameViewHolder
                nameViewHolder.textView.text = names[position - 1]
            }
        }
    }

    override fun getItemCount(): Int {
        return names.size + 1
    }
}