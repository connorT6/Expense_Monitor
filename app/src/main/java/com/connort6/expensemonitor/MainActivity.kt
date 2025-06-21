package com.connort6.expensemonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme
import com.connort6.expensemonitor.ui.views.AccountScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        // This must be BEFORE setContent
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ExpenseMonitorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AccountScreen(
                        modifier = Modifier
                            .padding(
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                            )
                            .padding(innerPadding)
                    )
                }
            }
        }

    }
}
