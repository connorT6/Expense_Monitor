package com.connort6.expensemonitor.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.connort6.expensemonitor.R
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme

@Composable
fun HomeScreen(navController: NavController) {

    val homeScreenViewModel: HomeScreenViewModel = viewModel()
    val accountTotal by homeScreenViewModel.accountTotal.collectAsState()
    HomeScreenContent(navController, accountTotal.balance)
}

@Composable
fun HomeScreenContent(
    navController: NavController,
    accountTotalBalance: Double
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LKR ${"%.2f".format(accountTotalBalance)}",
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.Edit, contentDescription = "", modifier = Modifier.clickable {
                    navController.navigate("accountPage")
                })
            }
            Spacer(Modifier.height(8.dp))
            Button({
                navController.navigate("categoryScreen")
            }) {
                Text("Categories")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {

                }) {
                Image(
                    painter = painterResource(id = R.drawable.ic_plus_2),
                    contentDescription = "Income",

                    )
                Text("Income")
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {

                }) {
                Image(
                    painter = painterResource(id = R.drawable.ic_minus_2),
                    contentDescription = "Expense"

                )
                Text("Expense")
            }

        }
    }
}

@Composable
@Preview
private fun HomePreview() {
    ExpenseMonitorTheme {
        HomeScreenContent(rememberNavController(), 12345.67)
    }
}