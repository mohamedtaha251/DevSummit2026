package com.example.devsummit2026.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.devsummit2026.ui.adaptive.AdaptiveScreen
import com.example.devsummit2026.ui.appfunctions.AppFunctionsScreen
import com.example.devsummit2026.ui.features.AiFeatureListScreen
import com.example.devsummit2026.ui.features.ComposeFeatureListScreen
import com.example.devsummit2026.ui.features.FeatureListScreen
import com.example.devsummit2026.ui.home.HomeScreen
import com.example.devsummit2026.ui.litert.ImageClassificationScreen
import com.example.devsummit2026.ui.litert.LiteRtScreen
import com.example.devsummit2026.ui.litert.MeetBackgroundScreen
import com.example.devsummit2026.ui.mlkit.TextRecognitionScreen
import com.example.devsummit2026.ui.qwen.QwenChatScreen
import com.example.devsummit2026.ui.qwenagent.QwenAgentScreen

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
        // Home: Simple Categories List (AI, Compose)
        composable(Screen.FeatureList.route) {
            FeatureListScreen(
                onCategoryClick = { category ->
                    navController.navigate(category.route)
                }
            )
        }

        // Category 1: AI Features List
        composable(Screen.AiFeatures.route) {
            AiFeatureListScreen(
                onItemClick = { item ->
                    navController.navigate(item.route)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Category 2: Compose Concepts List
        composable(Screen.ComposeFeatures.route) {
            ComposeFeatureListScreen(
                onItemClick = { item ->
                    navController.navigate(item.route)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Compose Screen: Live Width/Height & Responsive List-to-Grid on Rotation
        composable(Screen.AdaptiveMediaQuery.route) {
            AdaptiveScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // AI Feature Screens
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

        composable(Screen.LiteRt.route) {
            LiteRtScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onItemClick = { route ->
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

        composable(Screen.QwenChat.route) {
            QwenChatScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.QwenAgent.route) {
            QwenAgentScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
