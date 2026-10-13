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
                        // Only pop while "book" is still the current
                        // destination. This makes a duplicate call to
                        // onBack() (which can happen if anything ever
                        // reacts to closeBook()'s state change the way
                        // the entry screen does) a harmless no-op instead
                        // of popping an extra screen off the stack.
                        if (navController.currentDestination?.route == "book") {
                            viewModel.closeBook()
                            navController.popBackStack()
                        }
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
                        // EntryEditorScreen falls back to calling onBack()
                        // on its own whenever its entry becomes unavailable
                        // (deleted, or its id cleared on the way out) - and
                        // that can happen right after the user's own tap
                        // already triggered this same callback once. Without
                        // this guard, both calls pop the stack, which blows
                        // straight through "book" and leaves nothing
                        // composed (the blank-screen bug). Only popping
                        // while "entry" is still the current destination
                        // makes the second call a harmless no-op.
                        if (navController.currentDestination?.route == "entry") {
                            viewModel.closeEntry()
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}
