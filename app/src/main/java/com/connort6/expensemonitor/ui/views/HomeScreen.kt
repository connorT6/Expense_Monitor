package com.connort6.expensemonitor.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme

@Composable
fun HomeScreen(navController: NavController) {

    Column {
        Button({
            navController.navigate("accountPage")
        }) {
            Text("Accounts")
        }
        Spacer(Modifier.height(8.dp))
        Button({
            navController.navigate("categoryScreen")
        }) {
            Text("Categories")
        }
    }
}

@Composable
@Preview
private fun HomePreview() {
    ExpenseMonitorTheme {
        HomeScreen(rememberNavController())
    }
}