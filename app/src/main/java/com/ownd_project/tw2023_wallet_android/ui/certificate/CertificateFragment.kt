package com.ownd_project.tw2023_wallet_android.ui.certificate

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.FragmentCertificateBinding
import com.ownd_project.tw2023_wallet_android.datastore.CredentialData
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.oid.OpenIdProvider
import com.ownd_project.tw2023_wallet_android.oid.PresentationDefinition
import com.ownd_project.tw2023_wallet_android.oid.satisfyConstrains
import com.ownd_project.tw2023_wallet_android.signature.JWT
import com.ownd_project.tw2023_wallet_android.ui.credential_detail.CredentialDetailFragment
import com.ownd_project.tw2023_wallet_android.ui.shared.CredentialSharingViewModel
import com.ownd_project.tw2023_wallet_android.utils.DisplayUtil
import com.ownd_project.tw2023_wallet_android.utils.MetadataUtil
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata

class CertificateFragment : Fragment() {
    companion object {
        val tag = CredentialDetailFragment::class.simpleName
    }

    private var _binding: FragmentCertificateBinding? = null
    private val sharedViewModel by activityViewModels<CredentialSharingViewModel>()

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {

        val credentialDataStore = CredentialDataStore.getInstance(requireContext())
        val dashboardViewModel =
            ViewModelProvider(this, CertificateViewModelFactory(credentialDataStore)).get(
                CertificateViewModel::class.java
            )

        _binding = FragmentCertificateBinding.inflate(inflater, container, false)
        val root: View = binding.root

        if (sharedViewModel.presentationDefinition.value != null) {
            binding.fabAddCertificate.visibility = View.GONE
        }

        DisplayUtil.setFragmentTitle(
            activity as? AppCompatActivity,
            getString(R.string.title_certificate)
        )

        val textView: TextView = binding.textCertificate
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // 下から迫り上がる動作で要素を追加する画面を表示する処理
        binding.imgAddCertificate.setOnClickListener {
            val bottomSheetFragment = AdditionalCertificateSelectionFragment()
            bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
        }

        dashboardViewModel.credentialDataList.observe(viewLifecycleOwner) { schema ->
            val itemsList = schema?.itemsList
            if (!itemsList.isNullOrEmpty()) {
                binding.textCertificate.visibility = View.GONE
                binding.imgAddCertificate.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE

                // Adapterをセット
                if (sharedViewModel.presentationDefinition.value != null) {
                    val presentationDefinition = sharedViewModel.presentationDefinition.value!!
                    // itemsListをpresentationDefinitionの内容でフィルターする
                    val filteredItemsList = itemsList?.filter { filterCredential(it, presentationDefinition) }
                    val adapter = CredentialAdapter(filteredItemsList ?: emptyList())
                    binding.recyclerView.layoutManager = LinearLayoutManager(context)
                    binding.recyclerView.adapter = adapter
                } else {
                    val adapter = CredentialAdapter(itemsList)
                    binding.recyclerView.layoutManager = LinearLayoutManager(context)
                    binding.recyclerView.adapter = adapter
                }
            } else {
                binding.textCertificate.visibility = View.VISIBLE
                binding.imgAddCertificate.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            }
        }

        binding.fabAddCertificate.setOnClickListener {
            val bottomSheetFragment = AdditionalCertificateSelectionFragment()
            bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (sharedViewModel.presentationDefinition.value != null) {
            // MenuProviderを追加
            val activity = requireActivity()
            val menuProvider = CertificateFragmentMenuProvider(this, activity.menuInflater)
            activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

fun filterCredential(credential: CredentialData, presentationDefinition: PresentationDefinition): Boolean {
    val format = credential.format
    println("format: $format")
    return try {
        when (format) {
            "vc+sd-jwt" -> {
                val ret = OpenIdProvider.selectDisclosure(sdJwt = credential.credential, presentationDefinition = presentationDefinition)
                ret?.let { (_, disclosures) ->
                    disclosures.isNotEmpty()
                } ?: false // retがnullの場合はfalseを返す
            }
            "jwt_vc_json" -> {
                val (_, payload, _) = JWT.decodeJwt(jwt = credential.credential)
                println("satisfyConstrains?")
                satisfyConstrains(credential = payload, presentationDefinition = presentationDefinition)
            }
            else -> {
                // その他のフォーマットに対する処理が必要な場合、ここに追加
                false
            }
        }
    } catch (e: Exception) {
        // JWTのデコードに失敗した場合の処理
        println("JWT decoding failed for credential with format: $format")
        false
    }
}

class CertificateViewModelFactory(private val credentialDataStore: CredentialDataStore) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CertificateViewModel::class.java)) {
            return CertificateViewModel(credentialDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class CredentialAdapter(private val credentials: List<com.ownd_project.tw2023_wallet_android.datastore.CredentialData>) :
    RecyclerView.Adapter<CredentialAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.credential_name)
        val logoImageView: ImageView = view.findViewById(R.id.credential_logo)
        val cardView: CardView = view.findViewById(R.id.card_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_verifiable_credential, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // themeの中からデフォルトカラーを取得する
        val typedValue = TypedValue()
        val theme = holder.itemView.context.theme
        theme.resolveAttribute(R.attr.colorSecondaryVariant, typedValue, true)
        val defaultCardColor = typedValue.data

        val credential = credentials[position]
        val objectMapper = jacksonObjectMapper()
        val metadata: CredentialIssuerMetadata = objectMapper.readValue(
            credential.credentialIssuerMetadata,
            CredentialIssuerMetadata::class.java
        )
        val format = credential.format
        val types =
            MetadataUtil.extractTypes(format, credential.credential)
        val credentialSupported = metadata.credentialConfigurationsSupported[types.firstOrNull()]
        val displayData = credentialSupported!!.display!!.firstOrNull()

        //  display.text_colorがある場合はクレデンシャル名表示にそれを適用
        holder.nameTextView.text = displayData!!.name
        displayData.textColor?.takeIf { it.isNotEmpty() }?.let { colorCode ->
            val color = Color.parseColor(colorCode)
            holder.nameTextView.setTextColor(color)
        }

        val hasBackgroundImage = displayData.backgroundImage != null
        val hasBackgroundColor = displayData.backgroundColor != null
        val hasLogo = displayData.logo?.uri?.isNotEmpty() == true
        // 以下の条件で出し分けを行う
        //  | #   | backgroundImage | backgroundColor | logo | 対応                                                                       |
        //  | --- | --------------- | --------------- | ---- | -------------------------------------------------------------------------- |
        //  | 1   | 無              | 無              | 無   | 予めアプリに組み込んだ画像、または色でカードを表示する。デフォルトロゴを表示する。 |
        //  | 2   | 無              | 無              | 有   | 上記にロゴを重ねる                                                         |
        //  | 3   | 無              | 有              | 無   | 指定の背景色でカードを描画する。                                           |
        //  | 4   | 無              | 有              | 有   | 指定の背景色でカードを描画し、ロゴを表示する。                             |
        //  | 5   | 有              | 無              | 無   | backgroundImage を用いる。背景色とロゴは使わない                          |
        //  | 6   | 有              | 無              | 有   | backgroundImage を用いる。ロゴを表示する。                                |
        //  | 7   | 有              | 有              | 無   | backgroundImage を用いる。背景色は使わない。                              |
        //  | 8   | 有              | 有              | 有   | backgroundImage とロゴを使う。背景色は使わない。                          |
        // ロゴの処理
        if (hasLogo) {
            Glide.with(holder.itemView.context)
                .load(displayData.logo) // logo is now a URL
                .into(holder.logoImageView)
        } else if (!hasBackgroundImage) {
            val drawable = ContextCompat.getDrawable(holder.itemView.context, R.drawable.logo_owned)
            holder.logoImageView.setImageDrawable(drawable)
            holder.logoImageView.visibility = View.VISIBLE
        }


        // 背景画像の処理
        // 背景画像はurlから取得してイメージにする
        if (hasBackgroundImage) {
            Glide.with(holder.itemView.context)
                .asBitmap()
                .load(displayData.backgroundImage) // URLから背景画像をロード
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?,
                    ) {
                        val drawable = BitmapDrawable(holder.itemView.context.resources, resource)
                        holder.cardView.background = drawable
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
            holder.nameTextView.visibility = View.GONE
        } else if (hasBackgroundColor) {
            holder.cardView.setCardBackgroundColor(Color.parseColor(displayData.backgroundColor))
        } else {
            holder.cardView.setCardBackgroundColor(defaultCardColor)
        }

        holder.cardView.setOnClickListener {
            val action = CertificateFragmentDirections.actionToCredentialDetail(credential.id)
            it.findNavController().navigate(action)
        }
    }

    private fun Bundle.putCredentialData(
        key: String,
        credentialData: com.ownd_project.tw2023_wallet_android.datastore.CredentialData,
    ) {
        putByteArray(key, credentialData.toByteArray())
    }

    override fun getItemCount() = credentials.size
}
