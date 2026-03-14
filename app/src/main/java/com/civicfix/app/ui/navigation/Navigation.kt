package com.civicfix.app.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.civicfix.app.ui.screens.LoginScreen
import com.civicfix.app.ui.screens.SignupScreen
import com.civicfix.app.ui.screens.HomeScreen
import com.civicfix.app.ui.screens.ReportScreen
import com.civicfix.app.ui.screens.HistoryScreen

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
