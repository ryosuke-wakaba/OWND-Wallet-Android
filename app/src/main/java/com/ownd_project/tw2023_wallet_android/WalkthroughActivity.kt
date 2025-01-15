package com.ownd_project.tw2023_wallet_android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2

class WalkthroughActivity : AppCompatActivity() {

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_walkthrough)

        supportActionBar?.hide()

        val images = listOf(
            R.drawable.walkthrough_image1,
            R.drawable.walkthrough_image2,
            R.drawable.walkthrough_image3,
            R.drawable.walkthrough_image4
        )

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val adapter = WalkthroughAdapter(images) { action ->
            when (action) {
                WalkthroughAdapter.Action.NEXT -> {
                    if (viewPager.currentItem < images.size - 1) {
                        viewPager.currentItem += 1
                    }
                }

                WalkthroughAdapter.Action.PREVIOUS -> {
                    if (viewPager.currentItem > 0) {
                        viewPager.currentItem -= 1
                    }
                }

                WalkthroughAdapter.Action.SKIP_TO_FINAL -> {
                    viewPager.currentItem = images.size - 1
                }

                WalkthroughAdapter.Action.GOTO_MAIN -> {
                    // WalkThroughが終わったら、isFirstLaunchをfalseに設定
                    // Mainに遷移したときだけフラグをセットする
                    val sharedPreferences =
                        getSharedPreferences("com.owned_project", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isFirstLaunch", false)
                    editor.apply()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()  // オプション: WalkthroughActivity を終了してスタックから削除
                }

                WalkthroughAdapter.Action.RESTORE -> {
                    val intent = Intent(this, RestoreActivity::class.java)
                    // startActivity(intent)
                    resultLauncher.launch(intent)
                }

                WalkthroughAdapter.Action.NONE -> {
                    // 何もしない
                }
            }
        }

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                // アクティビティからの結果を処理
                if (result.resultCode == Activity.RESULT_OK) {
                    val sharedPreferences =
                        getSharedPreferences("com.owned_project", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isFirstLaunch", false)
                    editor.apply()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()  // オプション: WalkthroughActivity を終了してスタックから削除
                }
                // findNavController().navigate(R.id.navigation_certificate)
            }

        viewPager.adapter = adapter
    }
}
