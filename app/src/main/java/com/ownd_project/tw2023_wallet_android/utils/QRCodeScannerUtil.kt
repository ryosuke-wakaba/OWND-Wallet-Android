package com.ownd_project.tw2023_wallet_android.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.ownd_project.tw2023_wallet_android.QRCodeActivity
import com.journeyapps.barcodescanner.ScanOptions

class QRCodeScannerUtil(
    private val context: Context,
    private val scanLauncher: ActivityResultLauncher<ScanOptions>,
    private val requestPermissionLauncher: ActivityResultLauncher<String>,
) {

    // カメラの権限があるかどうかをチェック
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // カメラの権限を要求
    fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun launchQRCodeScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("QRコードをスキャンしてください")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true) // 画面の向きを縦に固定
        options.setCameraId(0) // バックカメラの使用
        options.setBarcodeImageEnabled(true)
        options.captureActivity = QRCodeActivity::class.java

        scanLauncher.launch(options)
    }
}
