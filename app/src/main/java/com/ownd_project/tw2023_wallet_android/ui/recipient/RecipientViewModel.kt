package com.ownd_project.tw2023_wallet_android.ui.recipient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.protobuf.Timestamp
import com.ownd_project.tw2023_wallet_android.datastore.Claim
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistoryStore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale


fun timestampToString(timestamp: Timestamp): String {
    val milliseconds = timestamp.seconds * 1000 + timestamp.nanos / 1_000_000
    val date = java.util.Date(milliseconds)
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return format.format(date)
}


fun concatenateAndTruncate(list: List<Claim>, limit: Int): String {
    val claimNames = list.map { it.name }
    val concatenated = claimNames.joinToString(" | ")
    return if (concatenated.length > limit) {
        concatenated.substring(0, limit) + "..."
    } else {
        concatenated
    }
}


fun getLatestHistoriesByRp(histories: CredentialSharingHistories): CredentialSharingHistories {
    val latestHistories = histories.itemsList
        .groupBy { it.rp }  // rpでグループ化
        .map { (_, group) ->  // 各グループから最新の履歴を選択
            group.maxByOrNull { it.createdAt.seconds }!!
        }

    // 最新の履歴のリストをCredentialSharingHistoriesに変換
    return CredentialSharingHistories.newBuilder()
        .addAllItems(latestHistories)
        .build()
}

class RecipientViewModel(private val credentialSharingHistoryStore: CredentialSharingHistoryStore) :
    ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "提供履歴はありません"
    }
    private val _sharingHistories =
        MutableLiveData<com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories?>()
    private val _targetHistory = MutableLiveData<CredentialSharingHistory?>().apply {
        value = null
    }

    val text: LiveData<String> = _text
    val sharingHistories: LiveData<com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories?> =
        _sharingHistories
    val targetHistory: LiveData<CredentialSharingHistory?> = _targetHistory

    private fun setHistoryData(schema: com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories) {
        _sharingHistories.value = schema
    }

    fun setTargetHistory(history: CredentialSharingHistory) {
        _targetHistory.value = history
    }

    init {
        viewModelScope.launch {
            credentialSharingHistoryStore.credentialSharingHistoriesFlow.collect() { schema ->
                setHistoryData(schema)
            }
        }
    }
}