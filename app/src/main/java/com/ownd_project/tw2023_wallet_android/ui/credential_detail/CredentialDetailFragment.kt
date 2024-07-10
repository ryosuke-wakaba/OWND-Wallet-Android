package com.ownd_project.tw2023_wallet_android.ui.credential_detail

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.FragmentCredentialDetailBinding
import com.ownd_project.tw2023_wallet_android.datastore.CredentialData
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistoryStore
import com.ownd_project.tw2023_wallet_android.oid.OpenIdProvider
import com.ownd_project.tw2023_wallet_android.oid.SubmissionCredential
import com.ownd_project.tw2023_wallet_android.ui.shared.CredentialSharingViewModel
import com.ownd_project.tw2023_wallet_android.utils.DisplayUtil
import com.ownd_project.tw2023_wallet_android.utils.MetadataUtil
import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil
import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil.decodeSDJwt
import com.ownd_project.tw2023_wallet_android.utils.viewBinding
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import com.ownd_project.tw2023_wallet_android.vci.Display
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CredentialDetailFragment : Fragment(R.layout.fragment_credential_detail) {
    companion object {
        private val tag = CredentialDetailFragment::class.simpleName
    }

    private val binding by viewBinding(FragmentCredentialDetailBinding::bind)

    private val store by lazy { CredentialDataStore.getInstance(requireContext()) }
    private val historyStore by lazy { CredentialSharingHistoryStore.getInstance(requireContext()) }

    private val sharedViewModel by activityViewModels<CredentialSharingViewModel>()
    private val viewModel: CredentialDetailViewModel by viewModels {
        CredentialDetailViewModelFactory(
            store,
            historyStore,
        )
    }
    private val glideRequestOptions by lazy {
        RequestOptions().placeholder(R.drawable.logo_owned).error(R.drawable.logo_owned)
    }
    private val args: CredentialDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // メニューを有効にする
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_certificate_detail_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                viewModel.viewModelScope.launch {
                    viewModel.deleteCredentialById(args.credentialId)
                    findNavController().popBackStack()
                }
                true
            }
            else -> {
                false
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        val menuProvider = CertificateDetailFragmentMenuProvider(this, activity.menuInflater)
        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val credentialId = args.credentialId
        // クレデンシャル名
        viewModel.credentialTypeName.observe(viewLifecycleOwner) { typeName ->
            // ここでUIにtypeNameを設定
            DisplayUtil.setFragmentTitle(
                activity as? AppCompatActivity,
                typeName
            )
        }

        // PresentationDefinition
        if (sharedViewModel.presentationDefinition.value != null) {
            viewModel.presentationDefinition = sharedViewModel.presentationDefinition.value
        }

        // クレデンシャル内容
        viewModel.setCredentialDataById(credentialId)
        viewModel.credentialData.observe(viewLifecycleOwner) { data ->
            data?.let {
                bindDataToView(it, viewModel.displayMap)
                viewModel.findHistoriesByCredentialId(it.id)
            }
        }

        // issuer情報へのリンク
        viewModel.credentialData.observe(viewLifecycleOwner) { credentialData ->
            val mapper = jacksonObjectMapper()
            val issuerName = credentialData.let {
                val metadata: CredentialIssuerMetadata = mapper.readValue(
                    it.credentialIssuerMetadata, CredentialIssuerMetadata::class.java
                )
                metadata.display?.firstOrNull()?.name ?: "不明な発行者"
            }
            val underlinedText = SpannableString("${issuerName}が発行しました").apply {
                setSpan(UnderlineSpan(), 0, length, 0)
            }
            binding.linkTestIssuerDetail.text = underlinedText
        }

        // ここでissuer情報ページへの遷移するクリックリスナー
        binding.linkTestIssuerDetail.setOnClickListener {
            val action =
                CredentialDetailFragmentDirections.actionToIssuerDetailFragment(credentialId)
            it.findNavController().navigate(action)
        }
        // GRコード表示用リンクのクリックリスナー
        binding.linkTextQrcode.setOnClickListener {
            val bottomSheetFragment = QRCodeDisplayFragment.newInstance(credentialId)
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }

        // カードの表示
        viewModel.displayData.observe(viewLifecycleOwner) { displayData ->
            val hasBackgroundImage = displayData.backgroundImage != null
            val hasBackgroundColor = displayData.backgroundColor != null
            val hasLogo = displayData.logo?.url?.isNotEmpty() == true
            val cardView = binding.credentialCardView

            // ロゴの処理
            if (hasLogo) {
                Glide.with(requireContext())
                    .load(displayData.logo?.url)
                    .into(binding.detailCredentialLogo)
            } else if (!hasBackgroundImage) {
                // デフォルトロゴまたはデフォルト画像を設定
                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.logo_owned)
                binding.detailCredentialLogo.setImageDrawable(drawable)
                binding.detailCredentialLogo.visibility = View.VISIBLE
            }

            // 背景画像と背景色の処理
            when {
                hasBackgroundImage -> {
                    // バックグラウンドイメージがある場合
                    Glide.with(requireContext()).asBitmap()
                        .load(displayData.backgroundImage) // URLから背景画像をロード
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?,
                            ) {
                                val drawable =
                                    BitmapDrawable(resources, resource)
                                cardView.background = drawable
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                            }
                        })
                }

                hasBackgroundColor -> {
                    // バックグラウンドカラーがある場合
                    cardView.setCardBackgroundColor(Color.parseColor(displayData.backgroundColor))
                }

                else -> {
                    // どちらもない場合のデフォルト処理
                    val typedValue = TypedValue()
                    val theme = context?.theme
                    theme?.resolveAttribute(R.attr.colorSecondaryVariant, typedValue, true)
                    val defaultCardColor = typedValue.data
                    cardView.setCardBackgroundColor(defaultCardColor)
                }
            }
        }


        val presentationDefinition = sharedViewModel.presentationDefinition.value
        val fromSharingData = presentationDefinition != null

        // recyclerViewの設定
        val layoutManager = LinearLayoutManager(context)
        val adapter =
            DisclosureAdapter(
                viewModel.credentialDetails.value?.disclosures ?: emptyList(),
                viewModel.displayMap
            )
        binding.disclosureContainer.layoutManager = layoutManager
        binding.disclosureContainer.adapter = adapter

        viewModel.credentialDetails.observe(viewLifecycleOwner) { details ->
            val titleText = if (fromSharingData) "提供する情報" else "この証明書の内容"
            binding.textDisclosuresTitle.text = titleText

            // DisclosureAdapterの呼び出し
            adapter.updateDisclosures(details.disclosures)

            // QRコード表示用リンクの可視性とアンダーラインの設定
            if (details.showQRCode) {
                val qrCodeText = SpannableString(getString(R.string.show_qrcode)).apply {
                    setSpan(UnderlineSpan(), 0, length, 0)
                }
                binding.linkTextQrcode.text = qrCodeText
                binding.linkTextQrcode.visibility = View.VISIBLE
            } else {
                binding.linkTextQrcode.visibility = View.GONE
            }
        }

        if (fromSharingData) {
            binding.disclosuresNotSharingTitleTextView.visibility = View.VISIBLE
            binding.disclosureContainerSharing.visibility = View.VISIBLE
            binding.disclosureContainerNotSharing.visibility = View.VISIBLE
            binding.disclosureContainer.visibility = View.GONE
            binding.disclosureContainer.visibility = View.GONE
            binding.textHistoryTitle.visibility = View.GONE

            val selectButton = binding.buttonSelectCredentialPrimary
            selectButton.visibility = View.VISIBLE
            selectButton.setOnClickListener { onSelectButtonClick() }
        } else {
            // 提供履歴
            val historyLayoutManager = LinearLayoutManager(context)
            val historyAdapter = HistoryAdapter(emptyList(), viewModel.displayMap) // 初期状態では空リスト
            binding.historyContainer.layoutManager = historyLayoutManager
            binding.historyContainer.adapter = historyAdapter

            viewModel.matchedHistories.observe(viewLifecycleOwner) { histories ->
                if (histories.isNullOrEmpty()) {
                    // 履歴がない場合の処理
                    binding.textNoHistory.visibility = View.VISIBLE
                    binding.textHistoryTitle.visibility = View.GONE
                } else {
                    // 履歴がある場合の処理
                    binding.textNoHistory.visibility = View.GONE
                    historyAdapter.updateHistories(histories)
                }
            }
        }
    }

    private fun onSelectButtonClick() {
        Log.d(tag, "on click")
        val data = requireNotNull(viewModel.credentialData.value) {
            "credentialData.value must not be null"
        }
        val types = MetadataUtil.extractTypes(data.format, data.credential)
        sharedViewModel.setSelectedCredential(
            data.type,
            SubmissionCredential(
                id = data.id,
                format = data.format,
                types = types,
                credential = data.credential,
                inputDescriptor = viewModel.inputDescriptor
            ),
            viewModel.metadata
        )
        findNavController().popBackStack(R.id.id_token_sharring, false)
    }

    private fun bindDataToView(credential: CredentialData, displayMap: Map<String, List<Display>>) {
        val presentationDefinition = sharedViewModel.presentationDefinition.value
        val fromSharingData = presentationDefinition != null

        if (fromSharingData && credential.format == "vc+sd-jwt") {
            val (_, sharingData) = OpenIdProvider.selectDisclosure(
                credential.credential,
                presentationDefinition!!
            )!!
            // 共有データのRecyclerViewを設定
            val sharingDisclosureItems = convertToDisclosureItems(sharingData)
            val sharingLayoutManager = LinearLayoutManager(context) // ここで定義
            val sharingAdapter = DisclosureAdapter(sharingDisclosureItems, displayMap)
            binding.disclosureContainerSharing.layoutManager = sharingLayoutManager // 使用
            binding.disclosureContainerSharing.adapter = sharingAdapter

            // 提供しないデータ
            val notSharing = decodeSDJwt(credential.credential).filterNot {
                sharingData.any { sharing -> sharing.disclosure == it.disclosure }
            }

            // 共有しないデータのRecyclerViewを設定
            val notSharingDisclosureItems = convertToDisclosureItems(notSharing)
            val notSharingLayoutManager = LinearLayoutManager(context) // ここで定義
            val notSharingAdapter = DisclosureAdapter(notSharingDisclosureItems, displayMap)
            binding.disclosureContainerNotSharing.layoutManager = notSharingLayoutManager // 使用
            binding.disclosureContainerNotSharing.adapter = notSharingAdapter
        }
    }

    // Todo disclosureとsharing,not_sharingの型が合わない
    //  そのうち型を合わせるのでその後は変換をやめる
    private fun convertToDisclosureItems(disclosures: List<SDJwtUtil.Disclosure>): List<CredentialDetailViewModel.DisclosureItem> {
        return disclosures.map { disclosure ->
            CredentialDetailViewModel.DisclosureItem(
                key = disclosure.key!!,
                value = disclosure.value!!
            )
        }
    }
}

class CredentialDetailViewModelFactory(
    private val store: CredentialDataStore,
    private val historyStore: CredentialSharingHistoryStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CredentialDetailViewModel::class.java)) {
            return CredentialDetailViewModel(store, historyStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class DisclosureAdapter(
    private var disclosures: List<CredentialDetailViewModel.DisclosureItem>,
    private var displayMap: Map<String, List<Display>>,
) : RecyclerView.Adapter<DisclosureAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val key: TextView = view.findViewById(R.id.disclosure_key)
        val value: TextView = view.findViewById(R.id.disclosure_value)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_disclosure_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = disclosures[position]
        val display = displayMap[item.key]
        holder.key.text = display?.get(0)?.name ?: item.key //todo 動的にロケールと合わせる
        holder.value.text = item.value
    }

    fun updateDisclosures(newDisclosures: List<CredentialDetailViewModel.DisclosureItem>) {
        // DiffUtilを使用して古いリストと新しいリストの違いを計算する
        val diffCallback = DisclosureDiffCallback(disclosures, newDisclosures)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        // 新しいデータを設定
        disclosures = newDisclosures

        // DiffResultを使用して具体的な変更をRecyclerViewに通知する
        diffResult.dispatchUpdatesTo(this)
    }


    override fun getItemCount() = disclosures.size
}

class DisclosureDiffCallback(
    private val oldList: List<CredentialDetailViewModel.DisclosureItem>,
    private val newList: List<CredentialDetailViewModel.DisclosureItem>,
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // 2つのアイテムが同じかどうかを判断するロジック
        return oldList[oldItemPosition].key == newList[newItemPosition].key
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // 2つのアイテムの内容が同じかどうかを判断するロジック
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

class HistoryAdapter(
    private var histories: List<CredentialSharingHistory>,
    private val displayMap: Map<String, List<Display>>
) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val createdAt: TextView = view.findViewById(R.id.createdAt) // 履歴アイテムのTextViewのID
        val claims: TextView = view.findViewById(R.id.claims) // 履歴アイテムのTextViewのID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_history_item, parent, false) // 履歴アイテムのレイアウト
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val history = histories[position]
        val dateFormat = SimpleDateFormat("yyyy/MM/dd H:mm", Locale.JAPAN)
        val date = Date(history.createdAt.seconds * 1000L) // TimestampからDateに変換
        holder.createdAt.text = dateFormat.format(date)
        val claimsList = history.claimsList
        val claimsText = claimsList.mapNotNull { it ->
            val display = displayMap[it.name]
            display?.get(0)?.name ?: it.name // todo get value dynamically by current locale.
        }.joinToString(" | ") + " 全${claimsList.size}項目"
        holder.claims.text = claimsText
    }

    override fun getItemCount() = histories.size

    fun updateHistories(newHistories: List<CredentialSharingHistory>) {
        histories = newHistories
        notifyDataSetChanged() // ここでは単純な更新を行っていますが、必要に応じてDiffUtilを使用することもできます
    }
}
