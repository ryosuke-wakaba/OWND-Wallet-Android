package com.ownd_project.tw2023_wallet_android.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.fragment.app.FragmentActivity

val TAG = BiometricUtil::class.simpleName

object BiometricUtil {
    private fun gotoSetting(enrollBiometricRequest: ActivityResultLauncher<Intent>) {
        val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            )
        }
        Log.d("BiometricUtil", "gotoSetting")
        enrollBiometricRequest.launch(enrollIntent)
    }


    fun checkBiometricAvailability(
        context: Context,
    ): Int {
        val biometricManager = BiometricManager.from(context)
        val logTag = "BiometricUtil.checkBiometricAvailability"
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(logTag, "App can authenticate using biometrics.")
                BiometricManager.BIOMETRIC_SUCCESS
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.e(logTag, "No biometric features available on this device.")
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.e(logTag, "Biometric features are currently unavailable.")
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.e(logTag, "Biometric features are currently unavailable.")
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
            }

            else -> {
                Log.e(logTag, "Biometric status unknown.")
                BiometricManager.BIOMETRIC_STATUS_UNKNOWN
            }
        }
    }


    fun gotoEnrollBiometricSetting(activity: FragmentActivity) {
        val enrollBiometricRequest =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                Log.d(TAG, "ResultCode: ${result.resultCode}")
                if (result.resultCode != Activity.RESULT_CANCELED) {
                    // 生体認証の設定が完了した場合の処理
                    Toast.makeText(activity, "生体認証の設定が完了しました。", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    // 生体認証の設定がキャンセルまたは失敗した場合の処理
                    Toast.makeText(
                        activity,
                        "生体認証の設定がキャンセルまたは失敗しました。",
                        Toast.LENGTH_SHORT
                    ).show()
                    // todo この時にロック画面を終わらせてアプリを使用可能とするか、ロック状態のままで使用不可とするかは仕様を確定させて対応する
                }
            }
        createAlertDialog(activity, enrollBiometricRequest)
    }

    fun createAlertDialog(
        activity: Activity,
        enrollBiometricRequest: ActivityResultLauncher<Intent>,
    ) {
        val alertDialog = AlertDialog.Builder(activity)
            .setTitle("生体認証の設定")
            .setMessage("このアプリを安全に使用するために、生体認証の設定を行ってください。設定しますか？")
            .setPositiveButton("Yes") { _, _ ->
                gotoSetting(enrollBiometricRequest)
            }
            .setNegativeButton("No", null)
            .create()

        alertDialog.show()
        // ボタンのテキストカラーを設定
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK) // 例：青色
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK) // 例：赤色

    }

}