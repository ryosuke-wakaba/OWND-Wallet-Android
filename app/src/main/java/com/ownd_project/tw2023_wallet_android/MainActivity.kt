package com.ownd_project.tw2023_wallet_android

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.ownd_project.tw2023_wallet_android.databinding.ActivityMainBinding
import com.ownd_project.tw2023_wallet_android.utils.BiometricUtil

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // enrollBiometricRequestの定義
    private val enrollBiometricRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("MainActivity", "ResultCode: ${result.resultCode}")
            shouldLock = if (result.resultCode != Activity.RESULT_CANCELED) {
                // 生体認証の設定が完了した場合の処理
                Toast.makeText(this, "生体認証の設定が完了しました。", Toast.LENGTH_SHORT).show()
                false
            } else {
                // 生体認証の設定がキャンセルまたは失敗した場合の処理
                Toast.makeText(this, "生体認証の設定がキャンセルまたは失敗しました。", Toast.LENGTH_SHORT).show()
                false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = this
        supportActionBar?.apply {
            displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
            setCustomView(R.layout.custom_action_bar)
            val color = ContextCompat.getColor(ctx, R.color.backgroundColorPrimary)
            setBackgroundDrawable(ColorDrawable(color))
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intentからデータ（URI）を取得
        val data: Uri? = intent?.data
        Log.d("MainActivity", "data = $data")

        binding.navView.post {
            val navController = findNavController(R.id.nav_host_fragment_activity_main)

            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.navigation_certificate,
                    R.id.navigation_recipient,
                    R.id.navigation_reader,
                    R.id.navigation_settings
                )
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            binding.navView.setupWithNavController(navController)
            navController.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.navigation_certificate -> {
                        supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    }
                    R.id.credentialDetailFragment, R.id.issuerDetailFragment -> {
                        supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    }
                }
            }
            // URIからパラメータを抽出
            data?.let {
                Log.d("MainActivity", "uri: $it")
                when (data.scheme) {
                    "openid4vp" -> {
                        val newIntent = Intent(this, TokenSharingActivity::class.java).apply {
                            putExtra("siopRequest", it.toString())
                            putExtra("index", -1)
                        }
                        startActivity(newIntent)
                    }
                    "openid-credential-offer" -> {
                        // ここでパラメータを処理
                        val parameterValue = it.getQueryParameter("credential_offer") // クエリパラメータの取得

                        // credential_offerがある場合発行画面に遷移する
                        if (!parameterValue.isNullOrEmpty()) {
                            val bundle = Bundle().apply {
                                putString("parameterValue", parameterValue)
                            }
                            navController.navigate(R.id.action_to_confirmation, bundle)
                        }
                    }
                    else -> {
                       Log.d("MainActivity", "unknown custom scheme: ${data.scheme}")
                    }
                }
            }
        }

        // 生体認証の利用可能性をチェック
        // ディバイスの生体認証の状態ををチェックして設定されていない場合、設定画面に遷移
        val biometricStatus = BiometricUtil.checkBiometricAvailability(this)
        if (biometricStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            BiometricUtil.createAlertDialog(this, enrollBiometricRequest)
        }
    }

    private var shouldLock = false
    private var isLocking = false

    fun setIsLocking(value: Boolean) {
        isLocking = value
    }

    override fun onStop() {
        super.onStop()
        if (!isLocking) {
            shouldLock = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (isLocking) {
            isLocking = false
        }
        if (shouldLock) {
            shouldLock = false  // ここでフラグをリセット
            isLocking = true
            // ロック画面アクティビティに遷移する
            // todo ロックタイミングの仕様を再度整理するまで一時的に機能を封印します
            // startActivity(Intent(this, LockScreenActivity::class.java))
        }
    }
}