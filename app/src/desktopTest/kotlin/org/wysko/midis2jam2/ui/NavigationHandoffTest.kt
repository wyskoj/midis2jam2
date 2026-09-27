/*
 * Copyright (C) 2026 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.ui

import org.wysko.midis2jam2.testing.ProjectPaths
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.ui.common.navigation.NavigationModel
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Picking a song somewhere other than the home screen, and having it arrive there.
 *
 * Both the history and the search results hand their chosen file to the home screen through
 * the same carrier. The carrier's own behaviour is checked here directly; that each screen
 * still uses it is checked against the sources, because the click itself lives in a
 * composable and the assertion that matters is that the wiring has not been removed.
 */
class NavigationHandoffTest {

    @Test
    fun `a chosen file is carried to the home screen and then let go`() {
        val navigation = NavigationModel()
        assertNull(navigation.applyHomeScreenMidiFile.value, "Nothing should be waiting at first")

        val chosen = File("/music/chosen.mid")
        navigation.setApplyHomeScreenMidiFile(chosen)
        assertEquals(chosen, navigation.applyHomeScreenMidiFile.value)

        // The home screen clears the carrier once it has taken the file, so returning to the
        // tab later does not silently reload the same song.
        navigation.clearApplyHomeScreenMidiFile()
        assertNull(navigation.applyHomeScreenMidiFile.value)
    }

    @Test
    fun `choosing another file replaces the one waiting`() {
        val navigation = NavigationModel()

        navigation.setApplyHomeScreenMidiFile(File("/music/first.mid"))
        navigation.setApplyHomeScreenMidiFile(File("/music/second.mid"))

        assertEquals(File("/music/second.mid"), navigation.applyHomeScreenMidiFile.value)
    }

    @Test
    @Spec("history.entry.play-loads-on-home")
    fun `the history hands its chosen song to the home screen`() {
        assertHandsOffToHomeScreen(
            "app/src/desktopMain/kotlin/org/wysko/midis2jam2/ui/history/HistoryScreen.kt",
            "Pressing Play on a history entry should load that song on the home screen"
        )
    }

    @Test
    @Spec("search.click-loads-on-home")
    fun `the search results hand the clicked song to the home screen`() {
        assertHandsOffToHomeScreen(
            "app/src/desktopMain/kotlin/org/wysko/midis2jam2/ui/search/SearchTab.kt",
            "Clicking a search result should load that file on the home tab"
        )
    }

    @Test
    fun `the home screen takes what it is handed`() {
        val home = File(ProjectPaths.repositoryRoot, HOME_SCREEN).readText()

        assertTrue(
            home.contains("applyHomeScreenMidiFile"),
            "The home screen no longer reads the file handed to it, so choosing a song " +
                "elsewhere would do nothing"
        )
    }

    private companion object {

        const val HOME_SCREEN = "app/src/desktopMain/kotlin/org/wysko/midis2jam2/ui/home/HomeScreen.desktop.kt"

        fun assertHandsOffToHomeScreen(relativePath: String, why: String) {
            val source = File(ProjectPaths.repositoryRoot, relativePath)
            assertTrue(source.isFile, "$relativePath no longer exists")

            assertTrue(
                source.readText().contains("setApplyHomeScreenMidiFile"),
                "$why, but ${source.name} no longer hands it over"
            )
        }
    }
}
