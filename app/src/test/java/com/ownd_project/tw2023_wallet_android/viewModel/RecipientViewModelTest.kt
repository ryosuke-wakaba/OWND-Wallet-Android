package com.ownd_project.tw2023_wallet_android.viewModel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.protobuf.Timestamp
import com.ownd_project.tw2023_wallet_android.datastore.Claim
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistories
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistoryStore
import com.ownd_project.tw2023_wallet_android.testData.TestDataUtil
import com.ownd_project.tw2023_wallet_android.ui.recipient.RecipientViewModel
import com.ownd_project.tw2023_wallet_android.ui.recipient.concatenateAndTruncate
import com.ownd_project.tw2023_wallet_android.ui.recipient.getLatestHistoriesByRp
import com.ownd_project.tw2023_wallet_android.ui.recipient.timestampToString
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Locale


@ExperimentalCoroutinesApi
class RecipientViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: RecipientViewModel
    private val mockStore: CredentialSharingHistoryStore = mock()
    private val observer: Observer<CredentialSharingHistory?> = mock()
    private val observerHistories: Observer<CredentialSharingHistories?> = mock()
    private val observerText: Observer<String> = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        whenever(mockStore.credentialSharingHistoriesFlow).thenReturn(flowOf(TestDataUtil.generateHistories()))
        viewModel = RecipientViewModel(mockStore)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setTargetHistory sets value in LiveData`() = runBlockingTest {
        val testData = TestDataUtil.generateHistories()
        val testHistory = testData.itemsList[0]

        viewModel.targetHistory.observeForever(observer)
        viewModel.setTargetHistory(testHistory)

        verify(observer).onChanged(testHistory)
    }

    @Test
    fun `sharingHistories is set correctly`() = runBlockingTest {
        viewModel.sharingHistories.observeForever(observerHistories)

        val testData = TestDataUtil.generateHistories()
        verify(observerHistories).onChanged(testData)
    }

    @Test
    fun `text is initialized correctly`() {
        viewModel.text.observeForever(observerText)

        verify(observerText).onChanged("提供履歴はありません")
    }


    @Test
    fun `timestampToString converts timestamp to string correctly`() {
        val timestamp = Timestamp.newBuilder().setSeconds(1622524800).setNanos(123456789).build()

        if (Locale.getDefault().equals(Locale.JAPAN)) {
            val expected = "2021-06-01 14:20"
            val result = timestampToString(timestamp)
            assertEquals(expected, result)
        }
    }

    @Test
    fun `concatenateAndTruncate concatenates and truncates correctly`() {
        val claims = listOf(
            Claim.newBuilder().setName("Claim1").build(),
            Claim.newBuilder().setName("Claim2").build(),
            Claim.newBuilder().setName("Claim3").build()
        )
        val limit = 10
        val expected = "Claim1 | C..."

        val result = concatenateAndTruncate(claims, limit)

        assertEquals(expected, result)
    }



    @Test
    fun `getLatestHistoriesByRp gets latest histories correctly`() {
        val histories = CredentialSharingHistories.newBuilder()
            .addItems(CredentialSharingHistory.newBuilder().setRp("rp1").setCreatedAt(Timestamp.newBuilder().setSeconds(1)).build())
            .addItems(CredentialSharingHistory.newBuilder().setRp("rp1").setCreatedAt(Timestamp.newBuilder().setSeconds(2)).build())
            .addItems(CredentialSharingHistory.newBuilder().setRp("rp2").setCreatedAt(Timestamp.newBuilder().setSeconds(1)).build())
            .build()
        val expected = CredentialSharingHistories.newBuilder()
            .addItems(CredentialSharingHistory.newBuilder().setRp("rp1").setCreatedAt(Timestamp.newBuilder().setSeconds(2)).build())
            .addItems(CredentialSharingHistory.newBuilder().setRp("rp2").setCreatedAt(Timestamp.newBuilder().setSeconds(1)).build())
            .build()

        val result = getLatestHistoriesByRp(histories)

        assertEquals(expected, result)
    }

}


