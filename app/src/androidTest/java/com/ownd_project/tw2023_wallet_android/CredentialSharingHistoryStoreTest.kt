package com.ownd_project.tw2023_wallet_android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistoryStore
import com.google.protobuf.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CredentialSharingHistoryStoreTest {
    private lateinit var store: CredentialSharingHistoryStore
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testFileName = "test_data_${UUID.randomUUID()}.pb"

    @Before
    fun setUp() {
        store = CredentialSharingHistoryStore(context, testFileName)
    }

    @After
    fun tearDown() {
        context.dataDir.resolve(testFileName).delete()
        store.cancel()
    }

    @Test
    fun testGetAllHistoriesNoData() = runBlocking {
        val histories = store.getAll()
        Assert.assertEquals(0, histories.size)
    }

    @Test
    fun testSaveAndGetAllHistories1History() = runBlocking {
        val currentInstant = Instant.now()
        val history = com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory.newBuilder()
            .setRp("someRp")
            .setAccountIndex(1)
            .setCreatedAt(
                Timestamp.newBuilder()
                .setSeconds(currentInstant.epochSecond)
                .setNanos(currentInstant.nano)
                .build())
            .build();
        store.save(history)

        val histories = store.getAll()
        Assert.assertEquals(1, histories.size)
        Assert.assertEquals("someRp", histories[0].rp)
        Assert.assertEquals(1, histories[0].accountIndex)

        val retrievedTimestamp = histories[0].createdAt
        val retrievedInstant = Instant.ofEpochSecond(retrievedTimestamp.seconds, retrievedTimestamp.nanos.toLong())

        Assert.assertEquals(currentInstant, retrievedInstant)
    }

    @Test
    fun testSaveAndGetAllHistories2History() = runBlocking {
        val currentInstant1 = Instant.now()
        val history1 = com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory.newBuilder()
            .setRp("someRp1")
            .setAccountIndex(1)
            .setCreatedAt(
                Timestamp.newBuilder()
                    .setSeconds(currentInstant1.epochSecond)
                    .setNanos(currentInstant1.nano)
                    .build())
            .build();
        store.save(history1)

        delay(100)

        val currentInstant2 = Instant.now()
        val history2 = com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory.newBuilder()
            .setRp("someRp2")
            .setAccountIndex(2)
            .setCreatedAt(
                Timestamp.newBuilder()
                    .setSeconds(currentInstant2.epochSecond)
                    .setNanos(currentInstant2.nano)
                    .build())
            .build();
        store.save(history2)

        val histories = store.getAll()
        Assert.assertEquals(2, histories.size)

        Assert.assertEquals("someRp1", histories[0].rp)
        Assert.assertEquals(1, histories[0].accountIndex)

        val retrievedTimestamp1 = histories[0].createdAt
        val retrievedInstant1 = Instant.ofEpochSecond(retrievedTimestamp1.seconds, retrievedTimestamp1.nanos.toLong())
        Assert.assertEquals(currentInstant1, retrievedInstant1)

        Assert.assertEquals("someRp2", histories[1].rp)
        Assert.assertEquals(2, histories[1].accountIndex)

        val retrievedTimestamp2 = histories[1].createdAt
        val retrievedInstant2 = Instant.ofEpochSecond(retrievedTimestamp2.seconds, retrievedTimestamp2.nanos.toLong())
        Assert.assertEquals(currentInstant2, retrievedInstant2)
    }
}