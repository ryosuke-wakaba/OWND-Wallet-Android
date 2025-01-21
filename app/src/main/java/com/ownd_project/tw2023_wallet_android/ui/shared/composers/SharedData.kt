package com.ownd_project.tw2023_wallet_android.ui.shared.composers

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream


@Composable
fun SharedClaim(claim: Claim) {
    var checked by remember { mutableStateOf(claim.checked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 0.dp, end = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            SubHeadLineText(claim.data.key!!, modifier = Modifier)
            if (claim.data.value!!.isBase64Image()) {
                val imageBitmap = claim.data.value!!.decodeBase64ToBitmap()
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap.asImageBitmap(),
                        contentDescription = "Base64 Decoded Image",
                        modifier = Modifier
                            .size(80.dp)
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "Invalid Image Data",
                        style = MaterialTheme.typography.body2,
                        color = Color.Red
                    )
                }
            } else {
                CalloutText(claim.data.value!!, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Switch(
            checked = checked,
            enabled = claim.optional,
            onCheckedChange = {
                checked = it
                claim.checked = it
            }
        )
    }
}

data class Claim(
    var data: SDJwtUtil.Disclosure,
//    var label: String,
//    var value: String,
    var optional: Boolean = false,
    var checked: Boolean = false
)

@Composable
fun SharedData(claims: List<Claim>) {
    LazyColumn(
        modifier = Modifier
    ) {
        itemsIndexed(claims) { index, item ->
            SharedClaim(item)
        }
    }
}

fun String.isBase64Image(): Boolean {
    return startsWith("data:image/") && contains(";base64,")
}

fun String.decodeBase64ToBitmap(): Bitmap? {
    return try {
        val base64Data = substringAfter(";base64,")
        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: IllegalArgumentException) {
        null
    }
}

fun getImageType(bytes: ByteArray): String {
    return when {
        bytes.isNotEmpty() && bytes.size >= 4 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpeg"
        bytes.isNotEmpty() && bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "png"
        else -> "unknown"
    }
}

fun generateTestImageInputStream(width: Int, height: Int, color: Int): InputStream {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(color)

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    return ByteArrayInputStream(outputStream.toByteArray())
}

@SuppressLint("ResourceType")
fun getPreviewData(color: Color): List<Claim> {
    val inputStream = generateTestImageInputStream(100, 100, color.hashCode())

//    val context = LocalContext.current
//    val inputStream = context.resources.openRawResource(R.drawable.wakaba_mark)
    val bytes = inputStream.readBytes()
    val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
    val imageValue = "data:image/${getImageType(bytes)};base64,$base64String"

    return listOf(
        Claim(
            data = SDJwtUtil.Disclosure(
                key = "Base64 Image",
                value = imageValue,
                disclosure = "dummy"
            ),
            optional = false,
            checked = true,
        ),
        Claim(
            data = SDJwtUtil.Disclosure(
                key = "label2",
                value = "value2",
                disclosure = "dummy"
            ),
            optional = false,
            checked = true,
        ),
        Claim(
            data = SDJwtUtil.Disclosure(
                key = "label3",
                value = "value3",
                disclosure = "dummy",
            ),
            optional = true,
            checked = false,
        ),
    )
}

@SuppressLint("ResourceType")
@Preview(showBackground = true)
@Composable
fun PreviewSharedData() {
    val claims = getPreviewData(Color.Red)
    SharedData(claims)
}

@SuppressLint("ResourceType")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSharedData2() {
    val claims = getPreviewData(Color.Blue)
    SharedData(claims)
}
