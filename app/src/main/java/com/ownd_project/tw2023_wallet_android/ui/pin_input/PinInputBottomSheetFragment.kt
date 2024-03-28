package com.ownd_project.tw2023_wallet_android.ui.pin_input

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.ui.confirmation.ConfirmationViewModel

class PinInputBottomSheetFragment : BottomSheetDialogFragment() {
    interface PinInputListener {
        fun onPinEntered(pin: String)
    }

    var listener: PinInputListener? = null

    companion object {
        private const val ARG_CREDENTIAL_OFFER = "credential_offer"

        fun newInstance(credentialOffer: String): PinInputBottomSheetFragment {
            val fragment = PinInputBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_CREDENTIAL_OFFER, credentialOffer)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var viewModel: ConfirmationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(ConfirmationViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val editTextPinCode = view.findViewById<EditText>(R.id.editTextPinCode)
        editTextPinCode.requestFocus()
        viewModel.pinError.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                // エラーメッセージを表示
                showErrorMessage(errorMessage)
                // PINコード入力をリセット
                view.findViewById<EditText>(R.id.editTextPinCode).text.clear()
                // エラー情報をリセット
                viewModel.resetPinError()
            }
        }
    }

    private fun showErrorMessage(message: String) {
        // エラーメッセージを表示するためのTextViewを追加
        val labelView = view?.findViewById<TextView>(R.id.label)
        labelView?.text = message
        labelView?.setTextColor(Color.RED) // エラーメッセージの色を赤に設定
    }

    // キーボードを表示させる
    private fun showKeyboard(editText: EditText) {
        val inputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pin_input_bottom_sheet, container, false)

        val credentialOffer = arguments?.getString(ARG_CREDENTIAL_OFFER)

        val editTextPinCode = view.findViewById<EditText>(R.id.editTextPinCode)
        val buttonAuth = view.findViewById<ImageButton>(R.id.buttonAuthenticate)
        // 初期状態としてボタンを無効化する
        buttonAuth.isEnabled = false
        buttonAuth.setImageResource(R.drawable.button_auth_inactive)

        editTextPinCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // ここでは何もしない
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    // テキストが空の場合、ボタンを非アクティブにする
                    buttonAuth.isEnabled = false
                    buttonAuth.setImageResource(R.drawable.button_auth_inactive)
                } else {
                    // テキストがある場合、ボタンをアクティブにする
                    buttonAuth.isEnabled = true
                    buttonAuth.setImageResource(R.drawable.button_auth)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // ここでは何もしない
            }
        })


        // inputにカーソルをフォーカスさせるためにdelayを使う
        editTextPinCode.postDelayed({
            if (editTextPinCode.requestFocus()) {
                val isFocused = editTextPinCode.isFocused
                if (isFocused) {
                    showKeyboard(editTextPinCode)
                }
            } else {
                Log.d("PinInputFragment", "Unable to focus EditText.")
            }
        }, 300)

        // ボタンにリスナーを設定
        buttonAuth.setOnClickListener {
            val pinCode = editTextPinCode.text.toString()
            listener?.onPinEntered(pinCode)
            dismiss() // PinInputBottomSheetFragment を閉じる
        }

        return view
    }

    // このフラグメントの高さを調整する
    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        val bottomSheet =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            val layoutParams = it.layoutParams

            // 画面の高さを取得し、90%の高さを設定
            val windowHeight = Resources.getSystem().displayMetrics.heightPixels
            layoutParams.height = (windowHeight * 0.9).toInt()
            it.layoutParams = layoutParams

            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
}