package com.ownd_project.tw2023_wallet_android

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import com.ownd_project.tw2023_wallet_android.ui.backup.OnFragmentInteractionListener

class RestoreActivity : AppCompatActivity(), OnFragmentInteractionListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore)

        val ctx = this
        supportActionBar?.apply {
            displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
            setCustomView(R.layout.custom_action_bar)
            val color = ContextCompat.getColor(ctx, R.color.backgroundColorPrimary)
            setBackgroundDrawable(ColorDrawable(color))
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_start_activity) as NavHostFragment
        val navController = navHostFragment.navController

        if (savedInstanceState == null) {
            navController.navigate(R.id.restoreFragment)
        }
    }
    override fun onFragmentClosed() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}