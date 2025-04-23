package com.khan.chitchat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.khan.chitchat.screens.auth.SignIn.SignInScreen
import com.khan.chitchat.screens.auth.SignUp.SignUpScreen
import com.khan.chitchat.screens.chat.ChatScreen
import com.khan.chitchat.screens.home.HomeScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Chat : Screen("chat/{userId}") {
        fun createRoute(userId: String) = "chat/$userId"
    }
    object Profile : Screen("profile")
}

@Composable
fun MainApp() {
    Surface(modifier = Modifier.fillMaxSize()) {
        val navController = rememberNavController()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val start = if (currentUser != null) Screen.Home.route else Screen.Login.route

        NavHost(navController = navController, startDestination = start) {
            composable(Screen.Login.route) {
                SignInScreen(navController)
            }
            
            composable(Screen.SignUp.route) {
                SignUpScreen(navController)
            }
            
            composable(Screen.Home.route) {
                HomeScreen(navController)
            }
            
            composable(
                route = Screen.Chat.route,
                arguments = listOf(
                    navArgument("userId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")
                ChatScreen(
                    navController = navController,
                    userId = userId ?: ""
                )
            }

        }
    }
}