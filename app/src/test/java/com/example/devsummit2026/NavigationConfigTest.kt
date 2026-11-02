package com.example.devsummit2026

import com.example.devsummit2026.navigation.Screen
import com.example.devsummit2026.ui.features.defaultFeatures
import com.example.devsummit2026.ui.lab.defaultLabOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationConfigTest {

    @Test
    fun featureList_firstItemIsFirebaseAiLogic() {
        assertTrue("Feature list should not be empty", defaultFeatures.isNotEmpty())
        val firstItem = defaultFeatures.first()
        assertEquals("Firebase AI logic", firstItem.title)
        assertEquals(Screen.FirebaseLogicLab.route, firstItem.route)
    }

    @Test
    fun firebaseLogicLab_firstOptionNavigatesToChat() {
        assertTrue("Lab options should not be empty", defaultLabOptions.isNotEmpty())
        val firstOption = defaultLabOptions.first()
        assertEquals("Firebase logic lab", firstOption.title)
        assertEquals(Screen.Chat.route, firstOption.route)
    }
}
