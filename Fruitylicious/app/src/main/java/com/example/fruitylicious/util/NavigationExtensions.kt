package com.example.fruitylicious.util

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Navigates to the given route only if the current backstack entry is in the RESUMED state.
 * This prevents multiple navigation events (e.g., from rapid button clicks) while
 * a transition is already in progress.
 */
fun NavController.navigateSafe(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(route, builder)
    }
}

/**
 * Pops the back stack only if the current backstack entry is in the RESUMED state.
 * This prevents multiple pop events while a transition is already in progress.
 */
fun NavController.popBackStackSafe(): Boolean {
    return if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    } else {
        false
    }
}
