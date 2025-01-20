package com.ownd_project.tw2023_wallet_android.ui.siop_vp

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ownd_project.tw2023_wallet_android.R

class TokenSharingFragmentMenuProvider(
    private val fragment: Fragment,
    private val menuInflater: MenuInflater
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_cancel, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                fragment.findNavController().navigateUp()
                true
            }
            R.id.action_cancel -> {
                fragment.requireActivity().finish()
                true
            }
            else -> false
        }
    }
}