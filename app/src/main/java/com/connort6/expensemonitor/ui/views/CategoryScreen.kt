package com.connort6.expensemonitor.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.connort6.expensemonitor.R
import com.connort6.expensemonitor.repo.Category
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme

@Composable
fun CategoryScreen(
    navController: NavController,
    iconPickerViewModel: IconPickerViewModel
) {
    val categoryViewModel: CategoryViewModel = viewModel()
    val categories by categoryViewModel.categoriesState.collectAsState()
    val pickerResult by iconPickerViewModel.pickerResult.collectAsState()
    val addOrEditData by categoryViewModel.addOrEditData.collectAsState()

    LaunchedEffect(pickerResult) {
        pickerResult.takeIf { it.selected }?.let {
            iconPickerViewModel.cleanResult()
            it
        }?.name?.let { iconName ->
            categoryViewModel.setAddCatIcon(iconName)
        }
    }

    CategoryView(
        categories.toList(), {
            categoryViewModel.showAddCat(true)
        },
        { id ->
            categoryViewModel.deleteCategory(id)
        }
    )

    if (addOrEditData.dialogShown) {
        AddOrEditAccount(
            {
                categoryViewModel.addCategory()
                categoryViewModel.showAddCat(false)
            },
            {
                categoryViewModel.showAddCat(false)
            },
            { name ->
                categoryViewModel.setAddCatName(name)
            },
            null,
            {
                navController.navigate("iconPicker")
            }, addOrEditData
        )
    }
}

@Composable
private fun CategoryView(
    categories: List<Category>,
    addCat: () -> Unit,
    delCat: (docId: String) -> Unit
) {

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {

            Button(
                onClick = {
                    addCat.invoke()
                }) {
                Icon(Icons.Default.AddCircle, contentDescription = null)

            }
        }
        LazyColumn {
            items(categories) {
                ListItem(it.name, null, it.iconName, R.drawable.ic_car_fuel, it.id, { id ->
                    delCat.invoke(id)
                }, null)
            }
        }
    }


}

@Preview
@Composable
private fun PreviewAcc() {

    // Fake ViewModel data
    val fateCats = listOf(
        Category(name = "Food"),
        Category(name = "Fuel"),
    )


    ExpenseMonitorTheme {
        CategoryView(fateCats, {}, {})
    }

}