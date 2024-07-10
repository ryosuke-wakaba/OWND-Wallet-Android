package com.ownd_project.tw2023_wallet_android.viewModel


import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.ownd_project.tw2023_wallet_android.datastore.CredentialDataStore
import com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistoryStore
import com.ownd_project.tw2023_wallet_android.testData.TestDataUtil
import com.ownd_project.tw2023_wallet_android.testData.TestDataUtil.jwtVcMetadata
import com.ownd_project.tw2023_wallet_android.testData.TestDataUtil.sdJwtCredential
import com.ownd_project.tw2023_wallet_android.testData.TestDataUtil.sdJwtMetadata
import com.ownd_project.tw2023_wallet_android.testData.TestDataUtil.vcJwtCredential
import com.ownd_project.tw2023_wallet_android.ui.credential_detail.CredentialDetailViewModel
import com.ownd_project.tw2023_wallet_android.vci.CredentialsSupportedDisplay
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations


@OptIn(ExperimentalCoroutinesApi::class)
class CredentialDetailViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: CredentialDetailViewModel
    private lateinit var store: CredentialDataStore
    private lateinit var historyStore: CredentialSharingHistoryStore

    @Mock
    private lateinit var credentialDataObserver: Observer<com.ownd_project.tw2023_wallet_android.datastore.CredentialData>

    @Mock
    private lateinit var matchedHistoriesObserver: Observer<List<com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory>>

    @Mock
    private lateinit var credentialTypeNameObserver: Observer<String>

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        MockitoAnnotations.openMocks(this)

        store = mock(CredentialDataStore::class.java)
        historyStore = mock(CredentialSharingHistoryStore::class.java)

        viewModel = CredentialDetailViewModel(store, historyStore)
        viewModel.credentialData.observeForever(credentialDataObserver)
        viewModel.matchedHistories.observeForever(matchedHistoriesObserver)
        viewModel.credentialTypeName.observeForever(credentialTypeNameObserver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.credentialData.removeObserver(credentialDataObserver)
        viewModel.matchedHistories.removeObserver(matchedHistoriesObserver)
        viewModel.credentialTypeName.removeObserver(credentialTypeNameObserver)
    }

    @Test
    fun testSetJwtVcJsonCredentialData() {
        val jwtVcJsonData = TestDataUtil.jwtVcJsonCredentialData.toByteArray()

        viewModel.setCredentialData(jwtVcJsonData)

        // LiveDataの更新を確認
        val credentialData = viewModel.credentialData.value

        assertNotNull(credentialData)
        assertNotNull(credentialData!!.id)
        assertEquals("jwt_vc_json", credentialData.format)
        assertEquals("UniversityDegreeCredential", credentialData.type)
        assertEquals("https://event.company/issuers/565049", credentialData.iss)
        assertEquals("test_accessToken", credentialData.accessToken)
        assertEquals(86400, credentialData.iat)
        assertEquals(86400, credentialData.exp)
        assertEquals(86400, credentialData.cNonceExpiresIn)
        assertEquals("test_CNonce", credentialData.cNonce)
        assertEquals(vcJwtCredential, credentialData.credential)
        assertNotNull(credentialData.credentialIssuerMetadata) // 保存時のシリアライズがキャメルケースになるので比較が面倒、かつデータソースが同じなので内容の比較にあまり意味がない
    }

    @Test
    fun testSetVcSdJwtCredentialData() {
        val vcSdJwtData = TestDataUtil.vcSdJwtCredentialData.toByteArray()

        viewModel.setCredentialData(vcSdJwtData)

        // LiveDataの更新を確認
        val credentialData = viewModel.credentialData.value

        assertNotNull(credentialData)
        assertNotNull(credentialData!!.id)
        assertEquals("vc+sd-jwt", credentialData.format)
        assertEquals("EmployeeCredential", credentialData.type)
        assertEquals("https://event.company/issuers/565049", credentialData.iss)
        assertEquals("test_accessToken", credentialData.accessToken)
        assertEquals(86400, credentialData.iat)
        assertEquals(86400, credentialData.exp)
        assertEquals(86400, credentialData.cNonceExpiresIn)
        assertEquals("test_CNonce", credentialData.cNonce)
        assertEquals(sdJwtCredential, credentialData.credential)
        assertNotNull(credentialData.credentialIssuerMetadata) // 保存時のシリアライズがキャメルケースになるので比較が面倒、かつデータソースが同じなので内容の比較にあまり意味がない
    }

    @Test
    fun testCredentialTypeName() = runTest {
        val jwtVcJsonData = TestDataUtil.jwtVcJsonCredentialData.toByteArray()
        // ViewModel にデータをセット
        viewModel.setCredentialData(jwtVcJsonData)

        // credentialTypeName の LiveData の値を検証
        viewModel.credentialTypeName.observeForever { typeName ->
            assertTrue(typeName == "学位証明書" || typeName == "University Credential")
        }
    }

    @Test
    fun testCredentialDetails() = runTest {
        val jwtVcJsonData = TestDataUtil.jwtVcJsonCredentialData.toByteArray()
        viewModel.setCredentialData(jwtVcJsonData)

        val detailsObserver = Observer<CredentialDetailViewModel.CredentialDetails> { details ->
            assertNotNull(details)
            assertTrue("QRコードは表示されるべき", details.showQRCode)
            assertEquals(4, details.disclosures.size) // 期待される disclosures の数
        }

        viewModel.credentialDetails.observeForever(detailsObserver)

        try {
            // LiveDataの値がセットされるのを待つ
            advanceUntilIdle()

            // 必要に応じてさらに詳細なアサーションをここに追加
        } finally {
            viewModel.credentialDetails.removeObserver(detailsObserver)
        }
    }

    @Test
    fun testDisplayData() = runTest {
        val vcSdJwtData = TestDataUtil.vcSdJwtCredentialData.toByteArray()
        viewModel.setCredentialData(vcSdJwtData)

        val displayDataObserver = Observer<CredentialsSupportedDisplay> { data ->
            assertNotNull(data)
            assertEquals("Employee Credential", data.name)
            assertEquals("https://datasign.jp/id/logo.png", data.logo?.url)
            // 他のプロパティも必要に応じてテスト
        }

        viewModel.displayData.observeForever(displayDataObserver)

        try {
            // LiveDataの値がセットされるのを待つ
            advanceUntilIdle()

            // 必要に応じてさらに詳細なアサーションをここに追加
        } finally {
            viewModel.displayData.removeObserver(displayDataObserver)
        }
    }


    @Test
    fun testFindHistoriesByCredentialId() = runTest {
        val credentialId = "credentialId"
        val mockHistories = listOf(
            mock(com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory::class.java),
            mock(com.ownd_project.tw2023_wallet_android.datastore.CredentialSharingHistory::class.java)
        )

        Mockito.`when`(historyStore.findAllByCredentialId(credentialId)).thenReturn(mockHistories)

        viewModel.findHistoriesByCredentialId(credentialId)

        // LiveDataの値がセットされるのを待つ
        advanceUntilIdle()

        // モックの結果がLiveDataに反映されているか確認
        viewModel.matchedHistories.observeForever { histories ->
            assertEquals(mockHistories, histories)
        }
    }

}
