package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.repository.ConstructionRepository
import com.example.ui.screens.admin.AdminMainScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.incharge.SiteInchargeMainScreen
import com.example.ui.theme.SiteKhataTheme
import com.example.ui.viewmodel.ConstructionViewModel
import com.example.util.SupabaseSyncManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = ConstructionRepository(database.appDao())
        val syncManager = SupabaseSyncManager(applicationContext)

        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ConstructionViewModel(repository, syncManager) as T
            }
        }

        setContent {
            SiteKhataTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ConstructionViewModel = viewModel(factory = viewModelFactory)
                    SiteKhataApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SiteKhataApp(
    viewModel: ConstructionViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf("LOGIN") } // LOGIN, INCHARGE, ADMIN

    LaunchedEffect(currentUser) {
        val user = currentUser
        if (user == null) {
            currentScreen = "LOGIN"
        } else if (user.role == "ADMIN") {
            currentScreen = "ADMIN"
        } else {
            currentScreen = "INCHARGE"
        }
    }

    when (currentScreen) {
        "LOGIN" -> {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { role ->
                    if (role == "ADMIN") {
                        currentScreen = "ADMIN"
                    } else {
                        currentScreen = "INCHARGE"
                    }
                },
                modifier = modifier
            )
        }
        "INCHARGE" -> {
            SiteInchargeMainScreen(
                viewModel = viewModel,
                onLogout = {
                    currentScreen = "LOGIN"
                },
                modifier = modifier
            )
        }
        "ADMIN" -> {
            AdminMainScreen(
                viewModel = viewModel,
                onLogout = {
                    currentScreen = "LOGIN"
                },
                modifier = modifier
            )
        }
    }
}
