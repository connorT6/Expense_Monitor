package com.connort6.expensemonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.connort6.expensemonitor.ui.theme.ExpenseMonitorTheme
import com.connort6.expensemonitor.ui.views.AccountScreen
import com.connort6.expensemonitor.ui.views.CategoryScreen
import com.connort6.expensemonitor.ui.views.HomeScreen
import com.connort6.expensemonitor.ui.views.IconPicker
import com.connort6.expensemonitor.ui.views.IconPickerViewModel
import com.google.firebase.firestore.FirebaseFirestore

//
//
//data class Reg(val regex: Regex)

val mainCollection = FirebaseFirestore.getInstance().collection("test")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        // This must be BEFORE setContent
        WindowCompat.setDecorFitsSystemWindows(window, false)


        setContent {
            ExpenseMonitorTheme {
                val navController = rememberNavController()
                val iconPickerViewModel: IconPickerViewModel = viewModel()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    NavHost(
                        navController = navController,
                        startDestination = "homeScreen",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("accountPage") {
                            AccountScreen(
                                navController = navController,
                                iconPickerViewModel = iconPickerViewModel
                            )
                        }
                        composable("iconPicker") {
                            IconPicker(
                                navController = navController,
                                iconPickerViewModel = iconPickerViewModel
                            )
                        }
                        composable("homeScreen") {
                            HomeScreen(
                                navController
                            )
                        }
                        composable("categoryScreen") {
                            CategoryScreen(
                                navController = navController,
                                iconPickerViewModel = iconPickerViewModel
                            )
                        }
                    }
                }
            }
        }

    }

}

