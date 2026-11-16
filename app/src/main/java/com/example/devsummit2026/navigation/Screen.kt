package com.example.devsummit2026.navigation

sealed class Screen(val route: String) {
    data object FeatureList : Screen("feature_list")
    data object AiFeatures : Screen("ai_features")
    data object ComposeFeatures : Screen("compose_features")

    // AI Screens
    data object Chat : Screen("chat")
    data object AppFunctions : Screen("app_functions")
    data object TextRecognition : Screen("text_recognition")
    data object LiteRt : Screen("litert")
    data object ImageClassification : Screen("image_classification")
    data object MeetBackgrounds : Screen("meet_backgrounds")
    data object QwenChat : Screen("qwen_chat")
    data object QwenAgent : Screen("qwen_agent_copilot")

    // Compose Screen
    data object AdaptiveMediaQuery : Screen("adaptive_media_query")

    // Security Screen
    data object SecurityFeatures : Screen("security_features")
    data object SmsVerification : Screen("sms_verification")

    // Modern Privacy & Location
    data object OneTimeLocation : Screen("one_time_location")
}
