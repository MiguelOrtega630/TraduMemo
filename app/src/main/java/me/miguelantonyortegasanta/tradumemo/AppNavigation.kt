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
        composable("libraryPicker") {
            LibraryScreen(navController, pickerMode = true) // modo seleccionar contexto para Inko
        }

        composable("transcription") {
            TranscriptionScreen(navController, docId = null) //Pantalla de transcribir
        }
        composable(
            route = "transcription/{docId}",
            arguments = listOf(
                navArgument("docId") { type = NavType.StringType } //Abrir una transcripción existente
            )
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getString("docId")
            TranscriptionScreen(navController, docId = docId)
        }

        composable("chat") {
            ChatScreen(navController = navController) //Chat Inko
        }
        composable("chat/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") //Chat Inko existente
            ChatScreen(navController = navController, sessionIdArg = sessionId)
        }

    }
}