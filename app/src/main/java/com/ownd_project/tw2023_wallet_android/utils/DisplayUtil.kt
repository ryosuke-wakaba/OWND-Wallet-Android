package com.ownd_project.tw2023_wallet_android.utils

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ownd_project.tw2023_wallet_android.R

object DisplayUtil {
    fun setFragmentTitle(activity: AppCompatActivity?, title: String) {
        activity?.supportActionBar?.customView?.findViewById<TextView>(R.id.action_bar_title)?.text =
            title
    }
}