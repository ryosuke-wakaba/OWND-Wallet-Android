package com.ownd_project.tw2023_wallet_android.ui.shared.composers

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FilledButton(
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val color = if (isDarkTheme) Color(0xFF018786) else Color.Black
    val textColor = if (isDarkTheme) Color.Black else Color.White
//    val disabledColor = if (isDarkTheme) Color(0xFF018786) else Color.DarkGray
    Button(
        enabled = enabled,
        colors = buttonColors(backgroundColor = color),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            label,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = textColor,
                lineHeight = 21.sp
            ), modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFilledButton() {
    FilledButton("Filled Button") {
        // nop
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewFilledButton2() {
    FilledButton("Filled Button") {
        // nop
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFilledButton3() {
    FilledButton("Filled Button", enabled = false) {
        // nop
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewFilledButton4() {
    FilledButton("Filled Button", enabled = false) {
        // nop
    }
}
