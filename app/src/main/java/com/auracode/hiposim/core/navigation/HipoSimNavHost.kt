package com.auracode.hiposim.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.auracode.hiposim.feature.auth.ui.LoginScreen
import com.auracode.hiposim.feature.auth.ui.RegisterScreen
import com.auracode.hiposim.feature.simulation.ui.ResultsScreen
import com.auracode.hiposim.feature.simulation.ui.SimulationFormScreen

@Composable
fun HipoSimNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.SimulationForm,
        modifier = modifier,
    ) {
        composable<Route.SimulationForm> {
            SimulationFormScreen(
                onSimulated = { navController.navigate(Route.Results) },
            )
        }
        composable<Route.Results> {
            ResultsScreen(
                onNavigateToRegister = { navController.navigate(Route.Register) },
                onNavigateToLogin = { navController.navigate(Route.Login) },
            )
        }
        composable<Route.Register> {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegistered = { navController.popBackStack() },
                // Registration and sign in replace each other, so "back" always returns to Results.
                onNavigateToLogin = {
                    navController.navigate(Route.Login) { popUpTo<Route.Register> { inclusive = true } }
                },
            )
        }
        composable<Route.Login> {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onSignedIn = { navController.popBackStack() },
                onNavigateToRegister = {
                    navController.navigate(Route.Register) { popUpTo<Route.Login> { inclusive = true } }
                },
            )
        }
    }
}
