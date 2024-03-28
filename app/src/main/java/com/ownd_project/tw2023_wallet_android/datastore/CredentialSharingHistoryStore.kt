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

open class CredentialSharingHistoryStore(
    context: Context,
    storeFile: String,
) {
    companion object {
        @Volatile
        private var instance: CredentialSharingHistoryStore? = null
        fun getInstance(
            context: Context,
            storeFile: String = "credential_sharing_history.pb",
        ): CredentialSharingHistoryStore {
            return instance ?: synchronized(this) {
                instance ?: CredentialSharingHistoryStore(context, storeFile).also { instance = it }
            }
        }

        fun resetInstance() {
            instance = null
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val historyDataStore: DataStore<com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories> = DataStoreFactory.create(
        serializer = CredentialSharingHistoriesSerializer,
        produceFile = { context.dataDir.resolve(storeFile) },
        scope = scope
    )

    suspend fun save(history: com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory) {
        historyDataStore.updateData { currentList ->
            currentList.toBuilder().addItems(history).build()
        }
    }

    suspend fun getAll(): List<com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory> {
        return historyDataStore.data.first().itemsList
    }

    open suspend fun findAllByCredentialId(credentialId: String): List<com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory> {
        return getAll().filter { it.credentialID == credentialId }
    }

    // 全件削除
    suspend fun deleteAllCredentials() {
        historyDataStore.updateData { currentList ->
            currentList.toBuilder().clearItems().build()
        }
    }

    object CredentialSharingHistoriesSerializer : Serializer<com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories> {

        override val defaultValue: com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories =
            com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories.getDefaultInstance()

        override suspend fun readFrom(input: InputStream): com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories {
            try {
                return com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories.parseFrom(input)
            } catch (exception: InvalidProtocolBufferException) {
                throw CorruptionException("Cannot read proto.", exception)
            }
        }

        override suspend fun writeTo(
            t: com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories,
            output: OutputStream,
        ) {
            t.writeTo(output)
        }
    }

    fun cancel() {
        scope.cancel()
    }
}