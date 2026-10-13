package com.lorebookforge.app.ui

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lorebookforge.app.AppViewModel

@Composable
fun AppNavHost(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // Any failure the view model surfaces (a bad file, a failed save) shows
    // here as a snackbar instead of the app crashing.
    LaunchedEffect(viewModel.lastError) {
        val message = viewModel.lastError
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
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
}
