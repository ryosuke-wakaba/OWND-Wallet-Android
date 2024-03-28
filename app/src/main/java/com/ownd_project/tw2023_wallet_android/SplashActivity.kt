package com.ownd_project.tw2023_wallet_android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SharedPreferencesから初回起動かどうかを確認
        val sharedPreferences = getSharedPreferences("com.owned_project", Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)

        // 初回起動の場合はWalkthroughActivityに、そうでない場合はMainActivityに遷移
        if (isFirstLaunch) {
            supportActionBar?.hide()
            // 初回起動時のみスプラッシュ画面を表示
            setContentView(R.layout.activity_splash)

            // スプラッシュ画面を一定時間表示した後にWalkthroughActivityに遷移
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, WalkthroughActivity::class.java))
                finish()
            }, 3000)  // 例: 3秒後に遷移
        } else {
            // 初回起動でない場合は直接MainActivityに遷移
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}