package com.ownd_project.tw2023_wallet_android

import android.os.Bundle
import android.widget.TextView
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.ViewfinderView

class QRCodeActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)

        val cancelAction = findViewById<TextView>(R.id.cancel_action)
        cancelAction.setOnClickListener {
            finish()
        }

        val barcodeScannerView =
            findViewById<com.journeyapps.barcodescanner.CompoundBarcodeView>(R.id.qrcode_scanner)

        // デフォルトで表示される赤い線を消す
        disableLaser(barcodeScannerView!!)

        val capture = CaptureManager(this, barcodeScannerView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.decode()
    }

    override fun onResume() {
        super.onResume()
        val barcodeView =
            findViewById<com.journeyapps.barcodescanner.CompoundBarcodeView>(R.id.qrcode_scanner)
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        val barcodeView =
            findViewById<com.journeyapps.barcodescanner.CompoundBarcodeView>(R.id.qrcode_scanner)
        barcodeView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        val barcodeView =
            findViewById<com.journeyapps.barcodescanner.CompoundBarcodeView>(R.id.qrcode_scanner)
        barcodeView.pause()
    }

    // 赤い線を消す処理
    private fun disableLaser(decoratedBarcodeView: DecoratedBarcodeView) {
        val scannerAlphaField = ViewfinderView::class.java.getDeclaredField("SCANNER_ALPHA")
        scannerAlphaField.isAccessible = true
        scannerAlphaField.set(decoratedBarcodeView.viewFinder, intArrayOf(0))
    }
}