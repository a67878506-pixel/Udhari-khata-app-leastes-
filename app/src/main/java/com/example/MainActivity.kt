package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.KhataRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.KhataViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            FirebaseApp.initializeApp(this)
        } catch (_: Exception) {}

        val repository = KhataRepository(this)
        val authViewModel = AuthViewModel(repository)
        val khataViewModel = KhataViewModel(repository)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UdhaarKhataApp(
                        authViewModel = authViewModel,
                        khataViewModel = khataViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun UdhaarKhataApp(
    authViewModel: AuthViewModel,
    khataViewModel: KhataViewModel
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()

    val isLoggedIn = authState.currentUser != null

    // Determine starting destination based on authentication state once on startup
    val startDestination = remember { if (isLoggedIn) "home" else "login" }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToSignup = { navController.navigate("signup") },
                onLoginSuccess = {
                    khataViewModel.observeKhatas()
                    khataViewModel.observeShareRequests()
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("signup") {
            SignupScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onSignupSuccess = {
                    khataViewModel.observeKhatas()
                    khataViewModel.observeShareRequests()
                    navController.navigate("home") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                khataViewModel = khataViewModel,
                authViewModel = authViewModel,
                onKhataClick = { khata ->
                    khataViewModel.selectKhata(khata)
                    navController.navigate("khata_detail")
                },
                onNavigateToShareRequests = {
                    val current = khataViewModel.uiState.value.currentKhata
                    if (current != null) {
                        navController.navigate("share_khata")
                    } else {
                        val firstKhata = (khataViewModel.uiState.value.khatas as? com.example.ui.viewmodel.UiState.Success)?.data?.firstOrNull()
                        if (firstKhata != null) {
                            khataViewModel.selectKhata(firstKhata)
                            navController.navigate("share_khata")
                        } else {
                            navController.navigate("profile")
                        }
                    }
                },
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }

        composable("khata_detail") {
            KhataDetailScreen(
                khataViewModel = khataViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onPersonClick = { person ->
                    khataViewModel.selectPerson(person)
                    navController.navigate("person_hisaab")
                },
                onShareKhataClick = { navController.navigate("share_khata") },
                onHistoryClick = { navController.navigate("khata_history") }
            )
        }

        composable("person_hisaab") {
            PersonHisaabScreen(
                khataViewModel = khataViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("khata_history") {
            KhataHistoryScreen(
                khataViewModel = khataViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("share_khata") {
            ShareKhataScreen(
                khataViewModel = khataViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("profile") {
            ProfileScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    khataViewModel.resetState()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
