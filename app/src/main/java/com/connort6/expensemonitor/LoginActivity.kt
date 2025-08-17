package com.connort6.expensemonitor

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme
import com.connort6.expensemonitor.ui.views.IMainViewModel
import com.connort6.expensemonitor.ui.views.MainViewModel
import com.connort6.expensemonitor.ui.views.MockMainViewModel

class LoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navigateToMain : () -> Unit = {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        enableEdgeToEdge()
        setContent {
            ExpenseMonitorTheme {
                LoginScreen(navigateToMain)
            }
        }
    }
}


@Composable
fun LoginScreen(navigateToMain: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    LoginView(viewModel, navigateToMain)
}

@Composable
private fun LoginView(viewModel: IMainViewModel, navigateToMain: () -> Unit) {
    val status by viewModel.status.collectAsStateWithLifecycle()

    Box(
        contentAlignment = Alignment.Center, modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        when (status) {
            0 -> {
                CircularProgressIndicator()
            }
            -1 -> {
                GoogleSignInButton({ viewModel.startLogin() })
            }
            else -> {
                navigateToMain.invoke()
            }
        }
    }
}


@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = "Google Logo",
            tint = Color.Unspecified, // Keep original colors
            modifier = Modifier// Padding after logo
                .size(60.dp)
        )
    }
}


@Preview
@Composable
fun LoginScreenPreview() {
    ExpenseMonitorTheme {
        LoginView(MockMainViewModel(), {})
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GoogleSignInButtonPreview() {
    GoogleSignInButton(onClick = {})
}
