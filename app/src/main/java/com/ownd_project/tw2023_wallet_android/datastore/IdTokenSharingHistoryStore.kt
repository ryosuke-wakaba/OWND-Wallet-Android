package com.ownd_project.tw2023_wallet_android.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream

class IdTokenSharingHistoryStore(
    context: Context,
    storeFile: String,
) {
    companion object {
        @Volatile
        private var instance: IdTokenSharingHistoryStore? = null
        fun getInstance(
            context: Context,
            storeFile: String = "id_token_sharing_history.pb",
        ): IdTokenSharingHistoryStore {
            return instance ?: synchronized(this) {
                instance ?: IdTokenSharingHistoryStore(context, storeFile).also { instance = it }
            }
        }

        fun resetInstance() {
            instance = null
        }
    }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val historyDataStore: DataStore<com.ownd_project.tw2023_wallet_android.datastore.IdTokenSharingHistories> = DataStoreFactory.create(
        serializer = IdTokenSharingHistoryStore.IdTokenSharingHistoriesSerializer,
        produceFile = { context.dataDir.resolve(storeFile) },
        scope = scope
    )

    suspend fun save(history: com.ownd_project.tw2023_wallet_android.datastore.IdTokenSharingHistory) {
        historyDataStore.updateData { currentList ->
            currentList.toBuilder().addItems(history).build()
        }
    }

    suspend fun getAll(): List<com.ownd_project.tw2023_wallet_android.datastore.IdTokenSharingHistory> {
        return historyDataStore.data.first().itemsList
    }

    suspend fun findAllByRp(rp: String): List<com.ownd_project.tw2023_wallet_android.datastore.IdTokenSharingHistory> {
        return getAll().filter { it.rp == rp }
    }

    object IdTokenSharingHistoriesSerializer : Serializer<com.ownd_project.tw2023_wallet_android.datastore.IdTokenSharingHistories> {

        override val defaultValue: com.ownd_project.tw2023_wallet_android.datastore.IdTokenSharingHistories =
            com.ownd_project.tw2023_wallet_android.datastore.IdTokenSharingHistories.getDefaultInstance()

        override suspend fun readFrom(input: InputStream): com.ownd_project.tw2023_wallet_android.datastore.IdTokenSharingHistories {
            try {
                return com.ownd_project.tw2023_wallet_android.datastore.IdTokenSharingHistories.parseFrom(input)
            } catch (exception: InvalidProtocolBufferException) {
                throw CorruptionException("Cannot read proto.", exception)
            }
        }

        override suspend fun writeTo(
            t: com.ownd_project.tw2023_wallet_android.datastore.IdTokenSharingHistories,
            output: OutputStream
        ) {
            t.writeTo(output)
        }
    }
    fun cancel() {
        scope.cancel()
    }
}