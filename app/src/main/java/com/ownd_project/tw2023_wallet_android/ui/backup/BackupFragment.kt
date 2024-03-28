package com.ownd_project.tw2023_wallet_android.ui.backup

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.ownd_project.tw2023_wallet_android.databinding.FragmentBackupBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class BackupFragment : Fragment() {
    private val viewModel: BackupViewModel by viewModels()
    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    // todo: ハードコーディングのお掃除
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Inflate the layout for this fragment
        val activity = requireActivity()
        val menuProvider = SimpleBackMenuProvider(this, activity.menuInflater)
        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // 最終バックアップ日付
        binding.lastBackupDateLabel.visibility = View.GONE
        binding.lastBackupDate.visibility = View.GONE
        viewModel.lastBackupDate.observe(viewLifecycleOwner, ::onUpdateLastBackupDate)

        // 画面クローズ要求処理
        viewModel.shouldClose.observe(viewLifecycleOwner, ::onUpdateCloseFragment)

        // バックアップ生成ボタンハンドラ
        binding.backupButton.setOnClickListener {
            println("click")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }
            val now = Date()
            val fileName = "owned_wallet_${dateFormat.format(now)}.zip"
            createDocument.launch(fileName)
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.accessPairwiseAccountManager(this)
    }

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
        // ドキュメント作成の結果をここで処理
        if (uri != null) {
            viewModel.saveCompressedTextAsZipFile(uri, requireContext())
        }
    }

    private fun onUpdateLastBackupDate(value: String?) {
        if (value != null) {
            binding.lastBackupDate.text = value
            binding.lastBackupDateLabel.visibility = View.VISIBLE
            binding.lastBackupDate.visibility = View.VISIBLE
        }
    }

    private fun onUpdateCloseFragment(close: Boolean) {
        // フラグメントを終了させる処理
        if (close) {
            findNavController().navigateUp()
        }
    }
}
class SimpleBackMenuProvider(
    private val fragment: Fragment,
    private val menuInflater: MenuInflater
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // メニューをインフレート
        // menuInflater.inflate(R.menu.menu_cancel, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                // 戻るが選択されたときの処理
                fragment.findNavController().navigateUp()
                true
            }
            else -> false
        }
    }
}
