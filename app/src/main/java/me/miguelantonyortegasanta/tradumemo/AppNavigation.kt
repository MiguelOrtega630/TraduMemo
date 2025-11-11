package me.miguelantonyortegasanta.tradumemo

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("main") { MainScreen(navController) }
        composable("library") { LibraryScreen(navController) }


        composable("transcription") {
            TranscriptionScreen(navController, docId = null)
        }

        // Transcription screen con nota existente
        composable(
            route = "transcription/{docId}",
            arguments = listOf(
                navArgument("docId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getString("docId")
            TranscriptionScreen(navController, docId = docId)
        }

        composable("chat") { ChatScreen(navController) } //INKO
    }
}