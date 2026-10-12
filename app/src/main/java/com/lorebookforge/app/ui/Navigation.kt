package com.lorebookforge.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lorebookforge.app.AppViewModel

@Composable
fun AppNavHost(viewModel: AppViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                viewModel = viewModel,
                onOpenBook = { navController.navigate("book") }
            )
        }
        composable("book") {
            BookScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.closeBook()
                    navController.popBackStack()
                },
                onOpenEntry = { id ->
                    viewModel.openEntry(id)
                    navController.navigate("entry")
                }
            )
        }
        composable("entry") {
            EntryEditorScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.closeEntry()
                    navController.popBackStack()
                }
            )
        }
    }
}
