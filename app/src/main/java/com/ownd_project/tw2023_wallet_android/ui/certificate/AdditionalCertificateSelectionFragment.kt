package com.ownd_project.tw2023_wallet_android.ui.certificate

import android.app.Dialog
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.ownd_project.tw2023_wallet_android.MainActivity
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.utils.QRCodeScannerUtil
import com.ownd_project.tw2023_wallet_android.vci.CredentialOffer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.net.URLDecoder

class AdditionalCertificateSelectionFragment : BottomSheetDialogFragment() {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var scanLauncher: ActivityResultLauncher<ScanOptions>
    private lateinit var qrCodeUtil: QRCodeScannerUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                qrCodeUtil.launchQRCodeScanner()
            } else {
                Toast.makeText(context, "カメラの権限が必要です", Toast.LENGTH_SHORT).show()
            }
        }

        scanLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) {
                Toast.makeText(context, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Scanned: ${result.contents}", Toast.LENGTH_LONG).show()
                // ここでQRコードの内容に基づいて処理を行う
            }
        }
        scanLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) {
                Toast.makeText(context, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                // QRコードの内容を解析
                val credentialOffer = parseQRCodeData(result.contents)
                // QRコードの中身がcredentialOffer
                if (isCredentialOfferValid(credentialOffer)) {
                    // 有効なcredentialOfferの場合
                    val bundle = bundleOf("parameterValue" to credentialOffer)
                    findNavController().navigate(R.id.confirmationFragment, bundle)
                } else {
                    // 無効なcredentialOfferの場合
                    Toast.makeText(context, "Invalid Credential Offer", Toast.LENGTH_LONG).show()
                }
            }
        }

        qrCodeUtil =
            QRCodeScannerUtil(requireContext(), scanLauncher, requestPermissionLauncher)
    }

    private fun isCredentialOfferValid(credentialOfferJson: String): Boolean {
        return try {
            Log.d("AddItemBottomSheetFragment", "src url = $credentialOfferJson")
            val mapper = jacksonObjectMapper().apply {
                propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
            }
            val mappedCredentialOffer: CredentialOffer = mapper.readValue(credentialOfferJson)
            Log.d("AddItemBottomSheetFragment", "mappedCredentialOffer = $mappedCredentialOffer")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // ここでボトムシートのレイアウトをインフレートします。
        // 例: return inflater.inflate(R.layout.fragment_my_bottom_sheet, container, false)
        val view = inflater.inflate(R.layout.fragment_slide_up, container, false)

        // urlはbuild.gradleの環境変数から取得
        val url = getString(R.string.MYNA_TARGET_URL)

        val myna = view.findViewById<LinearLayout>(R.id.addcert_myna)
        myna.setOnClickListener {
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()

            // Custom Tabs(アプリ内ブラウザ)のキャンセル時にロックさせないため(暫定)
            // MainActivityのisLockingをfalseにセット
            (activity as? MainActivity)?.setIsLocking(true)

            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
        }

        // 社員証要求ボタンの動作
        val other = view.findViewById<LinearLayout>(R.id.addcert_other)
        other.setOnClickListener {
            if (qrCodeUtil.hasCameraPermission()) {
                qrCodeUtil.launchQRCodeScanner()
            } else {
                qrCodeUtil.requestCameraPermission()
            }
        }

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : BottomSheetDialog(requireContext(), theme) {
            override fun onStart() {
                super.onStart()

                val bottomSheet =
                    findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
                bottomSheet?.let {
                    val behavior = BottomSheetBehavior.from(it)
                    val layoutParams = it.layoutParams

                    // 画面の高さを取得
                    val windowHeight = Resources.getSystem().displayMetrics.heightPixels
                    // 画面の80%の高さを設定
                    layoutParams.height = (windowHeight * 0.5).toInt()
                    it.layoutParams = layoutParams

                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    private fun parseQRCodeData(qrCodeContents: String): String {
        val decodedContents = URLDecoder.decode(qrCodeContents, "UTF-8")
        val queryStartIndex = decodedContents.indexOf('?')
        if (queryStartIndex == -1 || queryStartIndex >= decodedContents.length - 1) {
            return "ERROR"
        }

        val queryParams = decodedContents.substring(queryStartIndex + 1)
        val keyValuePairs = queryParams.split("&").associate {
            val keyValue = it.split("=")
            if (keyValue.size >= 2) keyValue[0] to keyValue[1] else keyValue[0] to ""
        }

        return keyValuePairs["credential_offer"] ?: "ERROR"
    }
}