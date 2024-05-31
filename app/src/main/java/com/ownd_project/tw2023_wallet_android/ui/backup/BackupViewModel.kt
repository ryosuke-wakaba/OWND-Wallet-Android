package com.ownd_project.tw2023_wallet_android.ui.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistoryStore
import com.ownd_project.tw2023_wallet_android.datastore.IdTokenSharingHistoryStore
import com.ownd_project.tw2023_wallet_android.datastore.PreferencesDataStore
import com.ownd_project.tw2023_wallet_android.pairwise.HDKeyRing
import com.ownd_project.tw2023_wallet_android.ui.siop_vp.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat
import java.time.Instant
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

class BackupViewModel : ViewModel() {

    // 最終バックアップ日時
    private val _lastBackupDate = MutableLiveData<String?>()
    val lastBackupDate: LiveData<String?> = _lastBackupDate

    // クローズ要求通知
    private val _shouldClose = MutableLiveData<Boolean>()
    val shouldClose: LiveData<Boolean> = _shouldClose

    private var mSeed: String? = null

    private fun requestClose() {
        _shouldClose.value = true
    }

    fun accessPairwiseAccountManager(fragment: Fragment) {
        viewModelScope.launch() {
            val dataStore = PreferencesDataStore(fragment.requireContext())
            val seedState = dataStore.getSeed(fragment)
            if (seedState.isRight()) {
                var seed = (seedState as Either.Right).value
                Log.d(TAG, "accessed seed successfully")
                if (seed.isNullOrEmpty()) {
                    // 初回のシード生成
                    val hdKeyRing = HDKeyRing(null)
                    seed = hdKeyRing.getMnemonicString()
                    dataStore.saveSeed(seed)
                }
                mSeed = seed

                // GMT日時をローカルタイムゾーンの文字列に変換
                val dt = dataStore.getLastBackupAt()
                if (dt != null) {
                    _lastBackupDate.value = convertGmtToLocalStringWithLocale(dt.toDateFromISO8601())
                }

            } else {
                val biometricStatus = (seedState as Either.Left).value
                Log.d(TAG, "BiometricStatus: $biometricStatus")
                withContext(Dispatchers.Main) {
                    requestClose()
                }
            }
        }
    }

    fun saveCompressedTextAsZipFile(uri: Uri, context: Context) {
        try {
            viewModelScope.launch(Dispatchers.IO) {

                val store: IdTokenSharingHistoryStore =
                    IdTokenSharingHistoryStore.getInstance(context)
                val idTokenSharingHistories = store.getAll().map { it ->
                    IdTokenSharingHistory(
                        rp = it.rp,
                        accountIndex = it.accountIndex,
                        createdAt = it.createdAt.toDate().toISO8601String()
                    )
                }

                val store2: CredentialSharingHistoryStore =
                    CredentialSharingHistoryStore.getInstance(context)
                val credentialSharingHistories = store2.getAll().map { it ->
                    val claims = it.claimsList.map{ claim ->
                        Claim(
                            name = claim.name,
                            value = claim.value,
                            purpose = claim.purpose
                        )
                    }
                    CredentialSharingHistory(
                        rp = it.rp,
                        accountIndex = it.accountIndex,
                        createdAt = it.createdAt.toDate().toISO8601String(),
                        credentialID = it.credentialID,
                        claims = claims,
                        rpName = "", // todo: impl
                        location = "", // todo: impl
                        contactUrl = "", // todo: impl
                        privacyPolicyUrl = "", // todo: impl
                        logoUrl = "" // todo: impl
                    )
                }

                val seed = requireNotNull(mSeed)
                val backup = BackupData(
                    seed = seed,
                    idTokenSharingHistories = idTokenSharingHistories,
                    credentialSharingHistories = credentialSharingHistories
                )
                val objectMapper = jacksonObjectMapper()
                val content = objectMapper.writeValueAsString(backup)

                val contentResolver = context.applicationContext.contentResolver
                contentResolver.openFileDescriptor(uri, "w")?.use { parcelFileDescriptor ->
                    FileOutputStream(parcelFileDescriptor.fileDescriptor).use { fileOutputStream ->
                        ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                            val zipEntry = ZipEntry("backup.txt") // ZIP内のファイル名
                            zipOutputStream.putNextEntry(zipEntry)
                            ByteArrayInputStream(content.toByteArray()).use { inputStream ->
                                inputStream.copyTo(zipOutputStream)
                            }
                            zipOutputStream.closeEntry()
                        }
                    }
                }

                val dataStore = PreferencesDataStore(context)
                val now = Date()
                dataStore.saveLastBackupAt(now.toISO8601String())

                withContext(Dispatchers.Main) {
                    val dt = dataStore.getLastBackupAt()
                    if (dt != null) {
                        _lastBackupDate.value = convertGmtToLocalStringWithLocale(dt.toDateFromISO8601())
                    }
                    Toast.makeText(context, R.string.saved_backup_file, Toast.LENGTH_LONG).show()
                }

            }
        } catch (e: IOException) {
            Toast.makeText(context, e.message, Toast.LENGTH_LONG)
                .show()
        }
    }
}

fun com.google.protobuf.Timestamp.toDate(): Date {
    return Date(seconds * 1000 + nanos / 1000000)
}

fun String.toDateFromISO8601(): Date {
    val instant = Instant.parse(this) // ISO 8601文字列をInstantに変換
    return Date.from(instant) // InstantをDateに変換
}

fun Date.toISO8601String(): String {
    val formatter = DateTimeFormatter.ISO_INSTANT
    val instant = this.toInstant()
    return formatter.format(instant)
}

fun Date.toInstant(): Instant = Instant.ofEpochMilli(this.time)

fun Date.toGoogleTimestamp(): com.google.protobuf.Timestamp {
    val seconds = this.time / 1000
    val nanos = ((this.time % 1000) * 1_000_000).toInt()
    return com.google.protobuf.Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos).build()
}

fun convertGmtToLocalStringWithLocale(date: Date): String? {
//    val now = Date()

    // GMT形式の日付フォーマッタを作成
    val gmtFormatter =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }

    // 現在の日時をGMT形式の文字列に変換
    val gmtString = gmtFormatter.format(date)

    // GMT文字列からDateオブジェクトを生成
    val gmtDate = gmtFormatter.parse(gmtString) ?: return null

    // システムのデフォルトロケールに基づいてローカルタイムゾーンのフォーマッタを作成
    val localFormatter =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).apply {
            timeZone = TimeZone.getDefault()
        }

    // GMT日時をローカルタイムゾーンの文字列に変換

    return localFormatter.format(gmtDate)
}