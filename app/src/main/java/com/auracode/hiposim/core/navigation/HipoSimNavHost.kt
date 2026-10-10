package com.auracode.hiposim.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
        startDestination = Route.Simulation,
        modifier = modifier,
    ) {
        composable<Route.Simulation> {
            SimulationFormScreen(
                onSimulated = { navController.navigate(Route.Results) },
            )
        }
        composable<Route.Results> {
            ResultsScreen(
                onNavigateToRegister = { navController.navigate(Route.Register) },
                onNoSimulation = { navController.popBackStack(Route.Simulation, inclusive = false) },
            )
        }
        composable<Route.Register> {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegistered = { navController.popBackStack() },
            )
        }
    }
}
