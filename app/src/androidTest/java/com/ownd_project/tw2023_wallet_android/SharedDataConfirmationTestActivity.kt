package com.ownd_project.tw2023_wallet_android

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class SharedDataConfirmationTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val processName = Application.getProcessName()
        Log.d("TestActivity", "Running in process: $processName")
    }
}