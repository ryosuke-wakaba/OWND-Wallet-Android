package com.ownd_project.tw2023_wallet_android.ui.siop_vp

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import coil3.compose.AsyncImage
import com.ownd_project.tw2023_wallet_android.MainActivity
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.model.CertificateInfo
import com.ownd_project.tw2023_wallet_android.model.ClientInfo
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.FilledButton
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.SubHeadLine
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.Title2
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.Title3
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.Verifier

data class RequestInfo(
    var title: String = "",
    var name: String = "",
    var url: String = "",
    var comment: String = "",
    var boolValue: Boolean = true,
    var clientInfo: ClientInfo,
)

class TokenSharingFragment2 : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val requestInfo = RequestInfo(
            title = "真偽情報に署名を行い、その情報をBoolcheckに送信します",
            boolValue = true,
            comment = "このXアカウントはXXX本人のものです",
            url = "https://example.com",
            clientInfo = clientInfo
        )
        return ComposeView(requireContext()).apply {
            setContent {
                Text("Hello")
                RequestContent(requestInfo = requestInfo, linkOpener = { url ->
                    val builder = CustomTabsIntent.Builder()
                    val customTabsIntent = builder.build()
                    // Custom Tabs(アプリ内ブラウザ)のキャンセル時にロックさせないため(暫定)
                    // MainActivityのisLockingをfalseにセット
                    (activity as? MainActivity)?.setIsLocking(true)
                    customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
                }) {
                    // todo move to next view
                }
            }
        }
    }
}

val url = "https://datasign.jp/wp-content/themes/ds_corporate/assets/images/datasign_logo_w.png"

@Composable
fun RequestContent(requestInfo: RequestInfo, linkOpener: (url: String) -> Unit, nextHandler: () -> Unit) {
    Column {
        Title2(requestInfo.title, modifier = Modifier.padding(8.dp))
        Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Image(
                painter = painterResource(R.drawable.logo_owned),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(8.dp)
            )
            Image(
                painter = painterResource(R.drawable.arrow),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(8.dp)
            )
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(8.dp)
            )
        }
        Title3("署名をする内容", modifier = Modifier.padding(8.dp))
        SubHeadLine(
            "URL: ${requestInfo.url}",
            modifier = Modifier.padding(start = 8.dp)
        )
        SubHeadLine(
            "真偽ステータス: ${if (requestInfo.boolValue) "真" else "偽"}",
            modifier = Modifier.padding(start = 8.dp)
        )
        SubHeadLine("コメント: ${requestInfo.comment}", modifier = Modifier.padding(start = 8.dp))

        SubHeadLine("提供先組織情報 ", modifier = Modifier.padding(start = 8.dp, top = 16.dp))
        Verifier(requestInfo.clientInfo, linkOpener = linkOpener)
        FilledButton("次へ", onClick = nextHandler)
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewRequestContent() {
    val requestInfo = RequestInfo(
        title = "真偽情報に署名を行い、その情報をBoolcheckに送信します",
        boolValue = true,
        comment = "このXアカウントはXXX本人のものです",
        url = "https://example.com",
        clientInfo = clientInfo
    )
    RequestContent(requestInfo, linkOpener = {}) {
        // nop
    }
}

val issuerCertInfo = CertificateInfo(
    domain = "",
    organization = "Amazon",
    country = "US",
    state = "",
    locality = "",
    street = "",
    email = ""
)
val certInfo = CertificateInfo(
    domain = "boolcheck.com",
    organization = "datasign.inc",
    country = "JP",
    state = "Tokyo",
    locality = "Sinzyuku-ku",
    street = "",
    email = "by-dev@datasign.jp",
    issuer = issuerCertInfo
)
val clientInfo = ClientInfo(
    name = "Boolcheck",
    certificateInfo = certInfo,
    tosUrl = "https://datasign.jp/tos",
    policyUrl = "https://datasign.jp/policy"
)
