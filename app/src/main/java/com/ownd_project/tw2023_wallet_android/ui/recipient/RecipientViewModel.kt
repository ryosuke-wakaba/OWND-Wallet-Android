package com.ownd_project.tw2023_wallet_android.ui.recipient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecipientViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "提供先画面(segment Fragment)"
    }
    val text: LiveData<String> = _text
}