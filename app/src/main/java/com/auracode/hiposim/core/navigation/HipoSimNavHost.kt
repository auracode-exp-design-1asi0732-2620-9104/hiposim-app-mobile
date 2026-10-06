package com.auracode.hiposim.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.auracode.hiposim.feature.auth.ui.RegisterScreen
import com.auracode.hiposim.feature.simulation.ui.ResultsScreen
import com.auracode.hiposim.feature.simulation.ui.ScheduleScreen

@Composable
fun HipoSimNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Results,
        modifier = modifier,
    ) {
        composable<Route.Results> {
            ResultsScreen(
                onNavigateToRegister = { navController.navigate(Route.Register) },
                onNavigateToSchedule = { navController.navigate(Route.Schedule) },
            )
        }
        composable<Route.Schedule> {
            ScheduleScreen(
                onBack = { navController.popBackStack() },
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
