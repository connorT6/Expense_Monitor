package com.connort6.expensemonitor.ui.views

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
import com.connort6.expensemonitor.repo.Transaction
import com.connort6.expensemonitor.repo.TransactionType
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Composable
fun TransactionScreen(transactionsViewModel: TransactionsViewModel = viewModel()) {
    val transactions by transactionsViewModel.transactions.collectAsState()
    TransactionList(transactions)
}

@Composable
fun TransactionList(transactions: List<Any>) {
    LazyColumn {
        items(transactions.size) { index ->
            val item = transactions[index]
            when (item) {
                is TransactionDayDetails -> DateItem(item.day)
                is Transaction -> TransactionItem(item)
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
fun TransactionItem(transaction: Transaction) {
    Box() {
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
                Text(transaction.accountId)
                Text(transaction.categoryId)
            }
        }
        val text =
            (if (transaction.transactionType == TransactionType.CREDIT) "+" else "-") + " LKR " +
                    transaction.amount.toString()
        Text(text, modifier = Modifier.align(Alignment.TopEnd), textAlign = TextAlign.End)
    }

}


@Preview
@Composable
fun ListPreview() {
    ExpenseMonitorTheme {

        val list = listOf(
            Transaction(
                accountId = "account_1",
                categoryId = "category_food",
                amount = 50.0,
                transactionType = TransactionType.DEBIT,
                createdTime = Timestamp.now()
            ),
            Transaction(
                accountId = "account_2",
                categoryId = "category_transport",
                amount = 25.50,
                transactionType = TransactionType.DEBIT,
                createdTime = Timestamp.now()
            ),
            Transaction(
                accountId = "account_1",
                categoryId = "category_salary",
                amount = 2000.0,
                transactionType = TransactionType.CREDIT,
                createdTime = Timestamp.now()
            ),
            Transaction(
                accountId = "account_3",
                categoryId = "category_entertainment",
                amount = 75.20,
                transactionType = TransactionType.DEBIT,
                createdTime = Timestamp(
                    Date(
                        System.currentTimeMillis() - 24 * 2 * 60 * 60 * 1000
                    )
                )
            )
        )

        TransactionList(list)
    }
}

@Preview
@Composable
fun TrItemPreview() {

    TransactionItem(Transaction(
        accountId = "account_1",
        categoryId = "category_salary",
        amount = 2000.0,
        transactionType = TransactionType.CREDIT,
        createdTime = Timestamp.now()
    ))

}
