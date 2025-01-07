package com.ownd_project.tw2023_wallet_android.ui.shared.composers

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ownd_project.tw2023_wallet_android.R
import com.ownd_project.tw2023_wallet_android.model.CertificateInfo
import com.ownd_project.tw2023_wallet_android.model.ClientInfo


@Composable
fun ListItem(label: String, value: String?, onClick: ((url: String) -> Unit)? = null) {
    SubHeadLine(
        value = label,
        modifier = Modifier
            .padding(start = 8.dp, top = 8.dp)
    )
    if (onClick !== null && !value.isNullOrBlank()) {
        OpenLinkText(
            link = value,
            displayText = value,
            onClick = onClick,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp)
        )
    } else {
        Callout(
            value = value ?: "",
            modifier = Modifier
                .padding(start = 8.dp, top = 4.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun Verifier(
    clientInfo: ClientInfo,
    linkOpener: ((url: String) -> Unit),
    isOpen: Boolean = false
) {
    val isExpanded = remember { mutableStateOf(isOpen) }
    val certInfo = clientInfo.certificateInfo

    Column(
        modifier = Modifier
            .padding(16.dp)
            .border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(4.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = clientInfo.logoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp)
            )
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
            ) { // 残りスペースを埋める
                BodyEmphasized(
                    value = clientInfo.name,
                    modifier = Modifier.padding(8.dp, bottom = 0.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.verifier_mark),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(start = 8.dp)
                    )
                    Caption1(
                        value = certInfo.issuer?.organization ?: "invalid organization",
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            IconButton(onClick = { isExpanded.value = !isExpanded.value }) {
                val iconColor = if (isSystemInDarkTheme()) Color.White else Color.Black
                Icon(
                    imageVector = if (isExpanded.value) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded.value) "閉じる" else "開く",
                    tint = iconColor
                )
            }
        }

        Divider(
            color = Color.LightGray,
            thickness = 1.dp,
        )

        val items = listOf(
            Triple("ドメイン", certInfo.domain, null),
            Triple("所在地", certInfo.getFullAddress(), null),
            Triple("国名", certInfo.state, null),
            Triple("連絡先", certInfo.email, null),
            Triple("利用規約", clientInfo.tosUrl, linkOpener),
            Triple("プライバシーポリシー", clientInfo.policyUrl, linkOpener),
        )
        val displayedItems = if (isExpanded.value) items else items.take(1)
        LazyColumn {
            items(displayedItems) { item ->
                ListItem(item.first, item.second, onClick = item.third)
                Divider(
                    color = Color.LightGray,
                    thickness = 1.dp,
                )
            }
        }
    }
}

@Composable
fun OpenLinkText(
    link: String,
    displayText: String = link,
    onClick: ((url: String) -> Unit)? = null,
    modifier: Modifier,
) {
    val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black
    val annotatedString = buildAnnotatedString {
        pushStringAnnotation(tag = "URL", annotation = link)
//        withStyle(
//            style = SpanStyle(
//                textDecoration = TextDecoration.Underline
//            )
//        ) {
//            append(displayText)
//        }
        append(displayText)
        pop()
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.body1.copy(
            color = textColor,
            textDecoration = TextDecoration.Underline
        ),
        modifier = modifier.clickable {
            if (onClick != null) {
                onClick(link)
            } else {
                // nop
            }
        }
    )
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

@Preview(showBackground = true)
@Composable
fun PreviewVerifier() {
    Verifier(clientInfo = clientInfo, linkOpener = { url ->
        Log.d("preview", "open $url")
    })
}

@Preview(showBackground = true)
@Composable
fun PreviewVerifier2() {
    Verifier(clientInfo = clientInfo, linkOpener = { url ->
        Log.d("preview", "open $url")
    }, isOpen = true)
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewVerifier3() {
    Verifier(clientInfo = clientInfo, linkOpener = { url ->
        Log.d("preview", "open $url")
    }, isOpen = true)
}
