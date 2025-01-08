package com.ownd_project.tw2023_wallet_android.ui.shared.composers

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun <T> SingleItemSelection(
    items: List<T>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(4.dp))
    ) {
        itemsIndexed(items) { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectionChanged(index) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedIndex == index,
                    onClick = { onSelectionChanged(index) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                itemContent(item)
            }
            if (index + 1 < items.size) {
                Divider(
                    color = Color.LightGray,
                    thickness = 1.dp,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRadioButtonCardList() {
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val items = listOf("Foo", "Bar", "Baz")
    SingleItemSelection(
        items,
        selectedIndex = selectedIndex,
        onSelectionChanged = { index -> selectedIndex = index },
        itemContent = { item ->
            Text(
                text = item,
                style = MaterialTheme.typography.body1
            )
        }
    )
    // val item = items[selectedIndex]
    val item = items.filterIndexed { index, s -> index === selectedIndex }
    Log.d("Preview", "item: $item")
}
