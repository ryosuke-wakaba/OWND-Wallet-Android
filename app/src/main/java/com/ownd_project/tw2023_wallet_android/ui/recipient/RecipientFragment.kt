package com.ownd_project.tw2023_wallet_android.ui.recipient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ownd_project.tw2023_wallet_android.databinding.FragmentRecipientBinding

class RecipientFragment : Fragment() {

    private var _binding: FragmentRecipientBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(RecipientViewModel::class.java)

        _binding = FragmentRecipientBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textSegment
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}