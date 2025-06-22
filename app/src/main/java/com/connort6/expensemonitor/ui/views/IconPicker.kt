package com.connort6.expensemonitor.ui.views

import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.connort6.expensemonitor.R
import kotlinx.parcelize.Parcelize

val RESULT_KEY = "picker_result_key"
val PARAM_KEY = "picker_param"

@Parcelize
data class PickerResult(val selected: Boolean, val name: String?) : Parcelable

@Composable
fun IconPicker(reg: Regex = Regex(""), onSelect: (String) -> Unit, navController: NavController = rememberNavController()) {
    val regex = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<Regex>(PARAM_KEY)
        ?: reg

    val drawableItems = remember {
        // Use reflection to get all drawable names that match the regex

        R.drawable::class.java.fields
            .mapNotNull { field ->
                val name = field.name
                Log.d("reg", "name: $name, ${regex.matches(name)}")
                if (regex.matches(name)) {
                    val resId = field.getInt(null)
                    DrawableItem(name, resId)
                } else null
            }
    }


    LazyVerticalGrid(
        columns = GridCells.Fixed(3), // 3 columns
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(drawableItems.size) { index ->
            val item = drawableItems[index]
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            RESULT_KEY,
                            PickerResult(true, item.resId.toString())
                        )
                        navController.popBackStack()
                    },
            ) {
                Image(
                    painter = painterResource(id = item.resId),
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Text(
                    text = item.name,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class DrawableItem(val name: String, val resId: Int)

@Preview
@Composable
private fun PreviewIcons() {
    IconPicker(Regex(".+"), {})
}
