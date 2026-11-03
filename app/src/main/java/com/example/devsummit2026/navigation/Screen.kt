package com.example.devsummit2026.navigation

sealed class Screen(val route: String) {
    data object FeatureList : Screen("feature_list")
    data object Chat : Screen("chat")
    data object AppFunctions : Screen("app_functions")
    data object TextRecognition : Screen("text_recognition")
    data object LiteRt : Screen("litert")
    data object ImageClassification : Screen("image_classification")
    data object MeetBackgrounds : Screen("meet_backgrounds")
    data object QwenChat : Screen("qwen_chat")
}
