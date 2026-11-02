package com.example.devsummit2026.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.devsummit2026.ui.appfunctions.AppFunctionsScreen
import com.example.devsummit2026.ui.features.FeatureListScreen
import com.example.devsummit2026.ui.home.HomeScreen
import com.example.devsummit2026.ui.litert.ImageClassificationScreen
import com.example.devsummit2026.ui.litert.ImageClassificationTypesScreen
import com.example.devsummit2026.ui.litert.MeetBackgroundScreen
import com.example.devsummit2026.ui.mlkit.TextRecognitionScreen

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

        composable(Screen.Chat.route) {
            HomeScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AppFunctions.route) {
            AppFunctionsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.TextRecognition.route) {
            TextRecognitionScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ImageClassificationTypes.route) {
            ImageClassificationTypesScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onTypeClick = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Screen.ImageClassification.route) {
            ImageClassificationScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.MeetBackgrounds.route) {
            MeetBackgroundScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
