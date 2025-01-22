package com.ownd_project.tw2023_wallet_android.ui.backup

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.databinding.FragmentRestoreBinding

class RestoreFragment : Fragment() {
    private val viewModel: RestoreViewModel by viewModels()
    private var _binding: FragmentRestoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRestoreBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val activity = requireActivity()
        val menuProvider = SimpleCancelMenuProvider(this, activity.menuInflater)
        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        // URL選択状態
        viewModel.uri.observe(viewLifecycleOwner, ::onUpdateUri)

        // 画面クローズ要求処理
        viewModel.shouldClose.observe(viewLifecycleOwner, ::onUpdateCloseFragment)

        // トーストメッセージ
        viewModel.message.observe(viewLifecycleOwner, ::onUpdateMessage)

        // 変更する
        binding.doChange.setOnClickListener {
            viewModel.clearUri()
        }

        binding.selectFileButton.setOnClickListener {
            getContent.launch("*/*")
        }

        binding.restoreButton.setOnClickListener {
            val uri = viewModel.uri.value
            if (uri != null) {
                viewModel.selectFile(requireContext())
            }
        }
        return root
    }

    private var mUrl: Uri? = null
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                mUrl = uri
                viewModel.setUri(uri)
            }
        }

    private fun onUpdateUri(uri: Uri?) {
        if (uri != null) {
            binding.notSelectedFileState.visibility = View.GONE
            binding.selectedFileState.visibility = View.VISIBLE

            binding.selectedFileName.text = uri.toString()
        } else {
            binding.notSelectedFileState.visibility = View.VISIBLE
            binding.selectedFileState.visibility = View.GONE
        }
    }

    private fun onUpdateMessage(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun onUpdateCloseFragment(close: Boolean) {
        // フラグメントを終了させる処理
        if (close) {
            findNavController().navigateUp()
            listener?.onFragmentClosed()
        }
    }

    private var listener: OnFragmentInteractionListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}

class SimpleCancelMenuProvider(
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
                fragment.requireActivity().finish()
                true
            }

            else -> false
        }
    }
}

interface OnFragmentInteractionListener {
    fun onFragmentClosed()
}
