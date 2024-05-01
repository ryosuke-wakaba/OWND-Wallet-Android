package com.ownd_project.tw2023_wallet_android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import arrow.core.Either
import com.ownd_project.tw2023_wallet_android.datastore.IdTokenSharingHistoryStore
import com.ownd_project.tw2023_wallet_android.pairwise.HDKeyRing
import com.ownd_project.tw2023_wallet_android.pairwise.PairwiseAccount
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID


@RunWith(AndroidJUnit4::class)
class PairwiseAccountTest {
    private lateinit var store: IdTokenSharingHistoryStore
    private lateinit var pairwiseAccount: PairwiseAccount
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testFileName = "test_data_${UUID.randomUUID()}.pb"

    @Before
    fun setUp() {
        store = IdTokenSharingHistoryStore(context, testFileName)
        val keyRing = HDKeyRing(null)
        pairwiseAccount = PairwiseAccount(context, keyRing.getMnemonicString(), store)
    }

    @After
    fun tearDown() {
        context.dataDir.resolve(testFileName).delete()
        store.cancel()
    }

    @Test
    fun testFirstAccount() = runBlocking {
        val rp = "https://sample-rp1.com"

        // 同じrpでアカウントが存在しないこと
        Assert.assertEquals(null, pairwiseAccount.getAccount(rp))

        val result = pairwiseAccount.newAccount(rp)
        Assert.assertTrue(result.isRight())
        val account = (result as Either.Right).value
        Assert.assertEquals(0, account.index)
        Assert.assertEquals(0, pairwiseAccount.getAccount(rp)?.index)
    }

    @Test
    fun testSecondAccount() = runBlocking {
        val rp1 = "https://sample-rp1.com"
        pairwiseAccount.newAccount(rp1)

        val rp2 = "https://sample-rp2.com"
        val result = pairwiseAccount.newAccount(rp2)
        Assert.assertTrue(result.isRight())
        val account = (result as Either.Right).value
        Assert.assertEquals(1, account.index)
        Assert.assertEquals(1, pairwiseAccount.getAccount(rp2)?.index)
    }

    @Test
    fun testDuplicatedAccount() = runBlocking {
        val rp1 = "https://sample-rp1.com"
        pairwiseAccount.newAccount(rp1)

        val result = pairwiseAccount.newAccount(rp1)
        Assert.assertTrue(result.isLeft())
        val msg = (result as Either.Left).value
        Assert.assertEquals("the rp is already shared account", msg)
    }
}
