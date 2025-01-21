package com.ownd_project.tw2023_wallet_android.ui.siop_vp.credential_selection

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.BodyText
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.CalloutText
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.FilledButton
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.FootnoteText
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.SingleItemSelection
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.Title2Text
import com.ownd_project.tw2023_wallet_android.ui.siop_vp.request_content.RequestInfo
import com.ownd_project.tw2023_wallet_android.ui.siop_vp.request_content.clientInfo


@Composable
fun CertificateSelectionView(
    viewModel: CertificateSelectionViewModel,
    nextHandler: (credentialId: String?) -> Unit
) {
    val credentials by viewModel.credentialDataList.observeAsState()
    if (credentials !== null) {
        CertificateSelection(credentialInfos = credentials!!, nextHandler = nextHandler)
    } else {
        Title2Text("Loading...", modifier = Modifier.padding(8.dp))
    }
}

@Composable
fun CertificateSelection(
    credentialInfos: List<CredentialInfo>,
    defaultIndex: Int = -1,
    nextHandler: (credentialId: String?) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(defaultIndex) }
    // https://developer.apple.com/design/human-interface-guidelines/color
    val noteBackgroundColor = if (isSystemInDarkTheme()) Color(28, 28, 30) else Color(242, 242, 247)
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Title2Text("証明書を選択", modifier = Modifier.padding(16.dp))
            BodyText(
                "あなたの身元を証明する証明書を選んでください",
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("title")
            )
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .background(color = noteBackgroundColor, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp)
                )
                FootnoteText(
                    "顔写真や住所など、提供したくない情報は、次の画面で編集が可能です。",
                    modifier = Modifier.padding(8.dp)
                )

            }
            SingleItemSelection(
                credentialInfos,
                selectedIndex = selectedIndex,
                onSelectionChanged = { index -> selectedIndex = index },
                itemContent = { item ->
                    Column {
                        Log.d("item:name", item.name)
                        if (item.useCredential) {
                            CalloutText(
                                item.name,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .testTag(item.name)
                            )
                            CalloutText(
                                "発行者: ${item.issuer}",
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else {
                            CalloutText("証明書を選択せず投稿する")
                        }
                    }
                }
            )
        }
        FilledButton(
            "次へ進む",
            enabled = selectedIndex > -1,
            modifier = Modifier.padding(8.dp)
        ) {
            val credentialInfo =
                credentialInfos.filterIndexed { index, _ -> index === selectedIndex }
            val id = if (!credentialInfo[0].useCredential) {
                null
            } else {
                credentialInfo[0].id
            }
            nextHandler(id)
        }
    }
}

val items = listOf(
    CredentialInfo(name = "証明書A", issuer = "Foo"),
    CredentialInfo(name = "証明書B", issuer = "Bar"),
    CredentialInfo(useCredential = false),
)

@Preview(showBackground = true)
@Composable
fun PreviewCertificateSelection() {
    val requestInfo = RequestInfo(
        title = "真偽情報に署名を行い、その情報をBoolcheckに送信します",
        boolValue = true,
        comment = "このXアカウントはXXX本人のものです",
        url = "https://example.com",
        clientInfo = clientInfo
    )
    CertificateSelection(credentialInfos = items) { }
}

@Preview(showBackground = true)
@Composable
fun PreviewCertificateSelection2() {
    val requestInfo = RequestInfo(
        title = "真偽情報に署名を行い、その情報をBoolcheckに送信します",
        boolValue = true,
        comment = "このXアカウントはXXX本人のものです",
        url = "https://example.com",
        clientInfo = clientInfo
    )
    CertificateSelection(credentialInfos = items, defaultIndex = 0) { }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewCertificateSelection3() {
    val requestInfo = RequestInfo(
        title = "真偽情報に署名を行い、その情報をBoolcheckに送信します",
        boolValue = true,
        comment = "このXアカウントはXXX本人のものです",
        url = "https://example.com",
        clientInfo = clientInfo
    )
    CertificateSelection(credentialInfos = items) { }
}
