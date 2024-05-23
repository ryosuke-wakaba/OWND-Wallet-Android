package com.ownd_project.tw2023_wallet_android.ui.siop_vp

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import com.ownd_project.tw2023_wallet_android.R

class TokenSharingFragmentMenuProvider(
    private val fragment: Fragment,
    private val menuInflater: MenuInflater
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // メニューをインフレート
        menuInflater.inflate(R.menu.menu_cancel, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        // メニューアイテムの選択を処理
        return when (menuItem.itemId) {
            R.id.action_cancel -> {
                // キャンセルが選択されたときの処理
//                fragment.parentFragmentManager.popBackStack()
                // fragment.parentFragmentManager.beginTransaction().remove(fragment).commit()
                fragment.requireActivity().finish()
                true
            }
            else -> false
        }
    }
}