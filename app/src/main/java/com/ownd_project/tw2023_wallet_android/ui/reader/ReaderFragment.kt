package com.ownd_project.tw2023_wallet_android.ui.reader

import com.ownd_project.tw2023_wallet_android.TokenSharingActivity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.FragmentReaderBinding
import com.ownd_project.tw2023_wallet_android.utils.QRCodeScannerUtil
import com.ownd_project.tw2023_wallet_android.utils.ZipUtil
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class ReaderFragment : Fragment() {

    private var _binding: FragmentReaderBinding? = null

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var scanLauncher: ActivityResultLauncher<ScanOptions>
    private lateinit var qrCodeUtil: QRCodeScannerUtil
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
                findNavController().navigate(R.id.navigation_certificate)
            } else {
                // QRコードの内容を解析
                val scanned = result.contents
                println("scanned: $scanned")
                val decompressed = try {
                    ZipUtil.decompressString(scanned)
                } catch (e: Exception) {
                    null
                }
                if (!decompressed.isNullOrEmpty()) {
                    val mapper = jacksonObjectMapper()
                    val typeRef = object : TypeReference<Map<String, Any>>() {}
                    val deserialized = mapper.readValue(decompressed, typeRef)
                    val format = deserialized["format"] as String
                    val credential = deserialized["credential"] as String
                    val display = deserialized["display"] as String
                    val args = Bundle().apply {
                        putString("format", format)
                        putString("credential", credential)
                        putString("display", display)
                    }
                    findNavController().navigate(R.id.credential_verification, args)
                } else {
                    println(scanned)
                    if (scanned.startsWith("openid4vp://") || scanned.startsWith("siopv2://")) {
                        val intent = Intent(context, TokenSharingActivity::class.java).apply {
                            putExtra("siopRequest", scanned)
                            putExtra("index", -1) // 一つ前の画面でアカウントを選択した場合のインデックス
                        }
                        resultLauncher.launch(intent)
                    } else {
                        Toast.makeText(
                            context,
                            "QR code is not Verifiable Credential",
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().navigate(R.id.navigation_certificate)
                    }
                }
            }
        }

        qrCodeUtil =
            QRCodeScannerUtil(requireContext(), scanLauncher, requestPermissionLauncher)

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                // アクティビティからの結果を処理
                findNavController().navigate(R.id.navigation_certificate)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReaderBinding.inflate(inflater, container, false)
        val root: View = binding.root

        if (qrCodeUtil.hasCameraPermission()) {
            qrCodeUtil.launchQRCodeScanner()
        } else {
            qrCodeUtil.requestCameraPermission()
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}