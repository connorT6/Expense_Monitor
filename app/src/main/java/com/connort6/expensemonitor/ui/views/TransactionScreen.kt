package com.connort6.expensemonitor.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.connort6.expensemonitor.repo.Transaction
import com.connort6.expensemonitor.repo.TransactionType
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme
import com.google.firebase.Timestamp


@Composable
fun TransactionScreen(
    navController: NavController,
    transactionsViewModel: TransactionsViewModel = viewModel(),
    smsViewModel: ISmsViewModel
) {
    val selectedTransaction by transactionsViewModel.selected.collectAsState()


    if (selectedTransaction != null) {
        val homeScreenViewModel: HomeScreenViewModel = viewModel()
        ShowTransactionView(
            homeScreenViewModel,
            { transactionsViewModel.clearSelection() },
            selectedTransaction,
            {navController.navigate("smsReader")},
            smsViewModel
        )
    }

    TransactionList(transactionsViewModel)

}

@Composable
fun TransactionList(transactionsViewModel: ITransactionsViewModel) {
    val transactions by transactionsViewModel.transactions.collectAsState()

    LazyColumn {
        items(transactions.size) { index ->
            val item = transactions[index]
            when (item) {
                is TransactionDayDetails -> DateItem(item.day)
                is Transaction -> TransactionItem(
                    item,
                    { transactionsViewModel.selectTransaction(item) })
            }
        }
    }
}

@Composable
fun DateItem(day: String) {
    Row {
        Text(day)
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onclick: () -> Unit) {
    Box(modifier = Modifier.clickable {
        onclick.invoke()
    }) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Icon(
                if (transaction.transactionType == TransactionType.DEBIT) Icons.Default.ArrowUpward
                else Icons.Default.ArrowDownward,
                "",
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(transaction.account?.name ?: transaction.accountId)
                Text(transaction.category?.name ?: transaction.categoryId)
            }
        }
        val text =
            (if (transaction.transactionType == TransactionType.CREDIT) "+" else "-") + " LKR " +
                    transaction.amount.toString()
        Text(text, modifier = Modifier.align(Alignment.TopEnd), textAlign = TextAlign.End)
    }

}
//TODO sms

@Preview
@Composable
fun ListPreview() {
    ExpenseMonitorTheme {

        TransactionList(MockTransactionsViewModel())
    }
}

@Preview
@Composable
fun TrItemPreview() {

    TransactionItem(
        Transaction(
            accountId = "account_1",
            categoryId = "category_salary",
            amount = 2000.0,
            transactionType = TransactionType.CREDIT,
            createdTime = Timestamp.now()
        )
    ) {}

}
