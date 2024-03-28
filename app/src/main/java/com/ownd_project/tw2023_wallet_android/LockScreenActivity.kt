package com.ownd_project.tw2023_wallet_android

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT
import androidx.core.content.ContextCompat
import com.ownd_project.tw2023_wallet_android.utils.BiometricUtil

class LockScreenActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val thiz = this
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // 認証に失敗した場合の処理
                    Log.e("BiometricAuth", "Error code: $errorCode, Error message: $errString")
                    if (errorCode == ERROR_LOCKOUT) {
                        Toast.makeText(
                            thiz,
                            "Too many attempts. Use screen lock instead",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("生体認証")
            .setSubtitle("アプリにアクセスするには認証が必要です")
            .setNegativeButtonText("キャンセル")
            .build()

        // 生体認証のプロンプトを表示
        biometricPrompt.authenticate(promptInfo)
    }

    private val enrollBiometricRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("LockScreenActivity", "ResultCode: ${result.resultCode}")
            if (result.resultCode != Activity.RESULT_CANCELED) {
                // 生体認証の設定が完了した場合の処理
                Toast.makeText(this, "生体認証の設定が完了しました。", Toast.LENGTH_SHORT).show()
                showBiometricPrompt()
            } else {
                // 生体認証の設定がキャンセルまたは失敗した場合の処理
                Toast.makeText(this, "生体認証の設定がキャンセルまたは失敗しました。", Toast.LENGTH_SHORT).show()
                // todo この時にロック画面を終わらせてアプリを使用可能とするか、ロック状態のままで使用不可とするかは仕様を確定させて対応する
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_screen)

        val btnAuthenticate = findViewById<Button>(R.id.btn_authenticate)
        btnAuthenticate.setOnClickListener {
            showBiometricPrompt()
        }

        // ディバイスの生体認証の設定状態ををチェックして、必要に応じて設定画面に遷移
        // 必要ない場合は生体認証の表示
        val biometricStatus = BiometricUtil.checkBiometricAvailability(this)
        if (biometricStatus == BiometricManager.BIOMETRIC_SUCCESS) {
            showBiometricPrompt()
        } else if (biometricStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            BiometricUtil.createAlertDialog(this, enrollBiometricRequest)
        }
    }
}
