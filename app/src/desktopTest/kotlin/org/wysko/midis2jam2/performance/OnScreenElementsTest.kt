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

package org.wysko.midis2jam2.performance

import org.wysko.midis2jam2.testing.withOnScreenElements
import com.jme3.font.BitmapText
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.domain.BackgroundWarning
import org.wysko.midis2jam2.domain.computeBackgroundWarning
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.domain.settings.AppSettings.BackgroundSettings.BackgroundType
import org.wysko.midis2jam2.manager.HudManager
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import org.wysko.midis2jam2.world.Sprite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The things drawn over the performance: the head-up display and the background.
 */
class OnScreenElementsTest {

    @Test
    @Spec("hud.shows-file-name")
    fun `the head-up display shows the name of the file being played`() {
        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0), fileName = "twinkle.mid")
            .use { performance ->
                assertNotNull(
                    performance.app.stateManager.getState(HudManager::class.java),
                    "No head-up display was attached"
                )

                val texts = performance.onEngineThread { textsIn(performance.app.guiNode) }
                assertTrue(
                    texts.any { it.contains("twinkle.mid") },
                    "The head-up display should name the loaded file, but it shows: $texts"
                )
            }
    }

    @Test
    @Spec("hud.shows-playback-time")
    fun `the head-up display tracks progress through the song`() {
        HeadlessPerformance.start(MidiFixtures.theWholeBand()).use { performance ->
            val hud = assertNotNull(performance.app.stateManager.getState(HudManager::class.java))

            // The progress bar is scaled from the playback clock, so it widens as time passes.
            val early = performance.onEngineThread { fillbarWidth(performance.app.guiNode) }
            performance.awaitFrames(1)
            Thread.sleep(PROGRESS_OBSERVATION_MILLIS)
            val later = performance.onEngineThread { fillbarWidth(performance.app.guiNode) }

            assertTrue(hud.isEnabled, "The head-up display should be showing")
            assertTrue(
                later >= early,
                "The progress indicator went backwards: $early then $later"
            )
        }
    }

    @Test
    @Spec("hud.toggle-setting")
    fun `the head-up display is not drawn when the setting is off`() {
        withHud(showHud = false) { hud, texts, sprites ->
            assertFalse(hud.isEnabled, "The head-up display is turned off, but its manager is enabled")
            assertTrue(
                texts.none { it.contains(FILE_NAME) },
                "The head-up display is turned off in the settings, but it still shows the file name " +
                    "(see #433). The overlay shows: $texts"
            )
            assertEquals(
                0,
                sprites,
                "The head-up display is turned off in the settings, but its progress bar is still drawn (see #433)"
            )
        }
    }

    @Test
    @Spec("hud.toggle-setting")
    fun `the head-up display is drawn when the setting is on`() {
        withHud(showHud = true) { hud, texts, sprites ->
            assertTrue(hud.isEnabled, "The head-up display is turned on, but its manager is disabled")
            assertTrue(
                texts.any { it.contains(FILE_NAME) },
                "The head-up display is turned on in the settings, but it does not show the file name. " +
                    "The overlay shows: $texts"
            )
            assertTrue(sprites > 0, "The head-up display is turned on in the settings, but its progress bar is not drawn")
        }
    }

    @Test
    @Spec("background.default.checkerboard")
    fun `the default background needs no configuration`() {
        val defaults = AppSettings()

        assertEquals(
            BackgroundType.Default,
            defaults.backgroundSettings.type,
            "A new install should start with the default background"
        )
        assertNull(
            computeBackgroundWarning(defaults.backgroundSettings),
            "The default background has nothing to configure, so it cannot be misconfigured"
        )
    }

    @Test
    @Spec("background.cubemap.unassigned-warning")
    fun `a cubemap with an unassigned face is reported`() {
        val settings = AppSettings.BackgroundSettings(
            type = BackgroundType.CubeMap,
            cubeMapTextures = MutableList(6) { if (it == 0) "" else "face$it.png" },
        )

        assertEquals(
            BackgroundWarning.UNASSIGNED,
            computeBackgroundWarning(settings),
            "A cubemap face left blank should be reported before the performance starts"
        )
    }

    @Test
    @Spec("background.cubemap.missing-warning")
    fun `a cubemap naming a file that is not there is reported`() {
        val settings = AppSettings.BackgroundSettings(
            type = BackgroundType.CubeMap,
            cubeMapTextures = MutableList(6) { "definitely-not-a-real-image-$it.png" },
        )

        assertEquals(
            BackgroundWarning.MISSING,
            computeBackgroundWarning(settings),
            "A cubemap naming a file that does not exist should be reported"
        )
    }

    @Test
    fun `a colour background is never reported as misconfigured`() {
        val settings = AppSettings.BackgroundSettings(type = BackgroundType.Color, color = 0x112233)

        assertNull(computeBackgroundWarning(settings))
    }

    private companion object {

        const val PROGRESS_OBSERVATION_MILLIS = 400L

        const val FILE_NAME = "twinkle.mid"

        /** Enough engine updates for anything that attaches to the overlay late to have done so. */
        const val SETTLE_FRAMES = 10

        /**
         * Runs a performance with the head-up display option set to [showHud], lets it settle,
         * and hands [block] the HUD manager, the overlay's text, and how many sprites it draws.
         */
        fun withHud(showHud: Boolean, block: (hud: HudManager, texts: List<String>, sprites: Int) -> Unit) {
            val settings = AppSettings().withOnScreenElements { copy(isShowHeadsUpDisplay = showHud) }
            HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0), settings, fileName = FILE_NAME)
                .use { performance ->
                    val hud = assertNotNull(
                        performance.app.stateManager.getState(HudManager::class.java),
                        "No head-up display manager was attached"
                    )

                    performance.awaitFrames(SETTLE_FRAMES)

                    val (texts, sprites) = performance.onEngineThread {
                        val gui = performance.app.guiNode
                        textsIn(gui) to gui.descendantMatches(Sprite::class.java).size
                    }
                    block(hud, texts, sprites)
                }
        }

        /** The text of every label currently on the overlay. */
        fun textsIn(root: Spatial): List<String> = buildList {
            fun visit(spatial: Spatial) {
                if (spatial is BitmapText) add(spatial.text.toString())
                if (spatial is Node) spatial.children.forEach(::visit)
            }
            visit(root)
        }

        /** How wide the progress bar currently is. */
        fun fillbarWidth(root: Spatial): Float {
            var widest = 0f
            fun visit(spatial: Spatial) {
                widest = maxOf(widest, spatial.localScale.x)
                if (spatial is Node) spatial.children.forEach(::visit)
            }
            visit(root)
            return widest
        }
    }
}
