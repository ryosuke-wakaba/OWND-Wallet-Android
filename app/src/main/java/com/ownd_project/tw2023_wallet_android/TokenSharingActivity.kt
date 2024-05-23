package com.ownd_project.tw2023_wallet_android

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment

class TokenSharingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_id_token_sharing)

        supportActionBar?.apply {
            displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
            setCustomView(R.layout.custom_action_bar)
            show()
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_sub_activity) as NavHostFragment
        val navController = navHostFragment.navController

        if (savedInstanceState == null) {
            val siopRequest = intent.getStringExtra("siopRequest")
            val index = intent.getIntExtra("index", -1)

            val args = Bundle().apply {
                putString("siopRequest", siopRequest)
                putInt("index", index)
            }
            navController.navigate(R.id.id_token_sharring, args)
        }
    }

}
