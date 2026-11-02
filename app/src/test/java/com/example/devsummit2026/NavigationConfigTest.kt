package com.example.devsummit2026

import com.example.devsummit2026.navigation.Screen
import com.example.devsummit2026.ui.features.defaultFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationConfigTest {

    @Test
    fun featureList_firstItemIsFirebaseAiLogicNavigatingToChat() {
        assertTrue("Feature list should not be empty", defaultFeatures.isNotEmpty())
        val firstItem = defaultFeatures.first()
        assertEquals("Firebase AI logic", firstItem.title)
        assertEquals(Screen.Chat.route, firstItem.route)
    }
}
