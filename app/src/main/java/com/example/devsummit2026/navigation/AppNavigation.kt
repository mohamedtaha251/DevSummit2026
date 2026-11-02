package com.example.devsummit2026.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.devsummit2026.ui.features.FeatureListScreen
import com.example.devsummit2026.ui.home.HomeScreen
import com.example.devsummit2026.ui.lab.FirebaseLogicLabScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.FeatureList.route,
        modifier = modifier
    ) {
        composable(Screen.FeatureList.route) {
            FeatureListScreen(
                onFeatureClick = { feature ->
                    navController.navigate(feature.route)
                }
            )
        }

        composable(Screen.FirebaseLogicLab.route) {
            FirebaseLogicLabScreen(
                onOptionClick = { labOption ->
                    navController.navigate(labOption.route)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Chat.route) {
            HomeScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
