package com.civicfix.app.ui.navigation

import android.util.Log
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.civicfix.app.data.api.RetrofitClient
import com.civicfix.app.data.models.FirebaseLoginRequest
import com.civicfix.app.ui.screens.LoginScreen
import com.civicfix.app.ui.screens.SignupScreen
import com.civicfix.app.ui.screens.HomeScreen
import com.civicfix.app.ui.screens.ReportScreen
import com.civicfix.app.ui.screens.HistoryScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Home : Screen("home")
    object Report : Screen("report")
    object History : Screen("history")
}

@Composable
fun CivicFixNavHost() {
    val navController = rememberNavController()
    var token by remember { mutableStateOf<String?>(null) }
    var checkingAuth by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Check if user is already signed in with Firebase
    LaunchedEffect(Unit) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            try {
                // Get a fresh Firebase ID token
                val idToken = firebaseUser.getIdToken(true).await().token
                if (idToken != null) {
                    // Exchange for backend JWT
                    val response = RetrofitClient.api.firebaseLogin(
                        FirebaseLoginRequest(firebaseToken = idToken)
                    )
                    token = response.accessToken
                    Log.i("Navigation", "Auto-login successful for ${firebaseUser.email}")
                }
            } catch (e: Exception) {
                Log.w("Navigation", "Auto-login failed, user needs to sign in again: ${e.message}")
                // Sign out of Firebase if backend exchange fails
                FirebaseAuth.getInstance().signOut()
            }
        }
        checkingAuth = false
    }

    // Show nothing while checking auth state (brief flash)
    if (checkingAuth) return

    NavHost(
        navController = navController,
        startDestination = if (token != null) Screen.Home.route else Screen.Login.route,
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { accessToken ->
                    token = accessToken
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = { accessToken ->
                    token = accessToken
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onReportClick = { navController.navigate(Screen.Report.route) },
                onHistoryClick = { navController.navigate(Screen.History.route) },
                token = token
            )
        }

        composable(Screen.Report.route) {
            ReportScreen(
                token = token,
                onReportSubmitted = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                token = token,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
