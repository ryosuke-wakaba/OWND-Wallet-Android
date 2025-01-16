package com.ownd_project.tw2023_wallet_android.ui.siop_vp

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.Claim
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.FilledButton
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.SharedData
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.SubHeadLineText
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.Title2Text
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.Verifier
import com.ownd_project.tw2023_wallet_android.ui.shared.composers.getPreviewData
import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil


val tag = "SharedDataConfirmationView"

@Composable
fun SharedDataConfirmationView(
    viewModel: SharedDataConfirmationViewModel,
    linkOpener: (url: String) -> Unit,
    sendHandler: (selected: List<SDJwtUtil.Disclosure>) -> Unit
) {
    val requestInfo by viewModel.requestInfo.observeAsState()
    val havingClaims by viewModel.claims.observeAsState()
    if (requestInfo != null && havingClaims != null) {
        val claims = havingClaims!!.map {
            Claim(data = it.data, optional = it.optional)
        }
        SharedDataConfirmation(
            claims,
            requestInfo = requestInfo!!,
            linkOpener = linkOpener,
            sendHandler = sendHandler
        )
    }
}

@Composable
fun SharedDataConfirmation(
    claims: List<Claim>,
    requestInfo: RequestInfo,
    linkOpener: (url: String) -> Unit,
    sendHandler: (selected: List<SDJwtUtil.Disclosure>) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .padding(0.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .padding(0.dp)
        ) {
            Title2Text("提供情報の確認", modifier = Modifier.padding(start = 8.dp, top = 8.dp))
            SharedData(claims)
            SubHeadLineText(
                "提供先組織情報 ",
                modifier = Modifier.padding(start = 8.dp, top = 16.dp)
            )
            Verifier(requestInfo.clientInfo, linkOpener = linkOpener)
        }
        FilledButton("送信する", onClick = {
            val selected = claims.filter { it.checked }.map { it.data }
            selected.forEach {
                Log.d(tag, "selected:${it.key}")
            }
            sendHandler(selected)
        })
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSharedDataConfirmation() {
    val claims = getPreviewData(Color.Red)
    val requestInfo = RequestInfo(
        title = "真偽情報に署名を行い、その情報をBoolcheckに送信します",
        boolValue = true,
        comment = "このXアカウントはXXX本人のものです",
        url = "https://example.com",
        clientInfo = clientInfo
    )
    SharedDataConfirmation(claims, requestInfo = requestInfo, linkOpener = { url -> }) { }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSharedDataConfirmation2() {
    val claims = getPreviewData(Color.Blue)
    val requestInfo = RequestInfo(
        title = "真偽情報に署名を行い、その情報をBoolcheckに送信します",
        boolValue = true,
        comment = "このXアカウントはXXX本人のものです",
        url = "https://example.com",
        clientInfo = clientInfo
    )
    SharedDataConfirmation(claims, requestInfo = requestInfo, linkOpener = { url -> }) { }
}
