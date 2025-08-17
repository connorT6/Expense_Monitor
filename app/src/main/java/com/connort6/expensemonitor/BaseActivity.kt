package com.connort6.expensemonitor

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.connort6.expensemonitor.ui.views.MainViewModel
import kotlinx.coroutines.launch

open class BaseActivity : ComponentActivity() {

    protected val mainViewModel: MainViewModel by lazy {
        val storeOwner = MainViewModelStoreOwner(application)
        ViewModelProvider(storeOwner, MainViewModelFactory(application))[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch { // Use lifecycleScope for automatic cancellation
            repeatOnLifecycle(Lifecycle.State.STARTED){
                mainViewModel.loginAction
                    .collect { action ->
                        if (action == -1) {
                            closeActivity()
                            mainViewModel.resetLoginAction()
                        }
                    }
            }
        }
    }

    fun closeActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}


// Factory for creating MainViewModel
class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainViewModelStoreOwner(private val application: Application) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    fun getViewModelStore(): ViewModelStore {
        return viewModelStore
    }
}