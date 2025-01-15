package com.ownd_project.tw2023_wallet_android.ui.credential_detail

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.FragmentQrcodeDisplayBinding
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.utils.MetadataUtil
import com.ownd_project.tw2023_wallet_android.utils.ZipUtil
import com.ownd_project.tw2023_wallet_android.vci.CredentialIssuerMetadata
import com.ownd_project.tw2023_wallet_android.vci.CredentialsSupportedDisplay
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.GZIPOutputStream

class QRCodeDisplayFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentQrcodeDisplayBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_CREDENTIAL_ID = "credentialId"

        fun newInstance(credentialId: String): QRCodeDisplayFragment {
            val fragment = QRCodeDisplayFragment()
            val args = Bundle().apply {
                putString(ARG_CREDENTIAL_ID, credentialId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val credentialId = arguments?.getString(ARG_CREDENTIAL_ID)
        credentialId?.let {
            loadCredentialData(it)
        }
        binding.buttonClose.setOnClickListener {
            dismiss() // フラグメントを閉じる
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentQrcodeDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    //    エンコードされたjwtデータ量が多すぎてqrコードにできないため、デコードしてから圧縮する
    private fun decodeJwt(jwt: String): String {
        return jwt.split(".").joinToString(".") { part ->
            String(Base64.decode(part, Base64.URL_SAFE), Charsets.UTF_8)
        }
    }

    private fun decodeJwtHeader(jwt: String): String {
        return jwt.split(".")[0].let { part ->
            String(Base64.decode(part, Base64.URL_SAFE), Charsets.UTF_8)
        }
    }

    private fun compressString(input: String): String {
//        val bos = ByteArrayOutputStream(input.length)
//        GZIPOutputStream(bos).bufferedWriter(Charsets.UTF_8).use { it.write(input) }
//        return Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT)
        return ZipUtil.compressString(input)
    }

    fun loadCredentialData(credentialId: String) {
        val dataStore = CredentialDataStore.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val credentialData = dataStore.getCredentialById(credentialId)
            Log.d("QRCodeDisplayFragment", "=============> $credentialData")
            credentialData?.let {
                // JWT のヘッダーをデコードして解析する
                val jwtHeader = decodeJwtHeader(credentialData.credential)
                if (jwtHeader.contains("x5u")) {
                    // レデンシャル情報の取得と表示
                    val metadata = jacksonObjectMapper().readValue(
                        credentialData.credentialIssuerMetadata,
                        CredentialIssuerMetadata::class.java
                    )
                    val displayList =
                        metadata.credentialConfigurationsSupported.values.flatMap { it.display ?: emptyList() }
                    val display = selectDisplay(displayList)
                    display?.name?.let { name ->
                        binding.textTitle.text = name
                    }
                    // displayを取得
                    val types =
                        MetadataUtil.extractTypes(credentialData.format, credentialData.credential)
                    val cs = requireNotNull(
                        MetadataUtil.findMatchingCredentials(
                            credentialData.format,
                            types,
                            metadata
                        )
                    )
                    val displayData = MetadataUtil.serializeDisplayByClaimMap(MetadataUtil.extractDisplayByClaim(cs))
                    // x5u が含まれている場合は QR コードを生成
                    val qrData = JSONObject().apply {
                        put("format", credentialData.format)
                        put("credential", credentialData.credential)
                        put("display", displayData)
                    }
                    val compressedJwt = compressString(qrData.toString())
                    val qrBitmap = generateQRCode(compressedJwt)
                    qrBitmap?.let {
                        binding.qrCodeImageView.setImageBitmap(it)
                    }
                } else {
                    // x5u が含まれていない場合はエラーメッセージを表示
                    // QRコード生成に失敗した場合、エラーメッセージを表示
                    binding.qrCodeImageView.visibility = View.GONE // QRコード画像を非表示にする
                    binding.textDescription.visibility = View.GONE
                    binding.errorMessage.visibility = View.VISIBLE // エラーメッセージを表示
                }

            }
        }
    }

    private fun selectDisplay(displayList: List<CredentialsSupportedDisplay>?): CredentialsSupportedDisplay? {
        if (displayList.isNullOrEmpty()) {
            return null // リストが空の場合
        }
        val currentLocale = Locale.getDefault().toString()
        val defaultDisplay = displayList.firstOrNull { it.locale.isNullOrEmpty() } // ロケールが含まれていない要素
        return displayList.find { it.locale == currentLocale } ?: defaultDisplay
        ?: displayList.first()
    }

    private fun generateQRCode(text: String): Bitmap? {
        val size = 1024 // QRコードのサイズ
        return try {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            Log.e("QRCodeDisplayFragment", "Error generating QR code: ${e.message}")
            null // またはエラーを示すビットマップを返す
        }
    }

    // フラグメントの高さを調整
    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        val bottomSheet =
            dialog!!.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            val layoutParams = it.layoutParams

            // 画面の高さを取得し、90%の高さを設定
            val windowHeight = Resources.getSystem().displayMetrics.heightPixels
            layoutParams.height = (windowHeight * 0.7).toInt()
            it.layoutParams = layoutParams

            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}