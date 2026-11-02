package com.example.devsummit2026.navigation

sealed class Screen(val route: String) {
    data object FeatureList : Screen("feature_list")
    data object FirebaseLogicLab : Screen("firebase_logic_lab")
    data object Chat : Screen("chat")
}
