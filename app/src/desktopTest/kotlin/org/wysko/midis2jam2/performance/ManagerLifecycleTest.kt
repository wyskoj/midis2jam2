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

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import org.wysko.midis2jam2.manager.ActionsManager
import org.wysko.midis2jam2.manager.CollectorsManager
import org.wysko.midis2jam2.manager.DrumSetVisibilityManager
import org.wysko.midis2jam2.manager.PlaybackManager
import org.wysko.midis2jam2.manager.PerformanceConfigState
import org.wysko.midis2jam2.manager.StageManager
import org.wysko.midis2jam2.manager.StandManager
import org.wysko.midis2jam2.manager.camera.CameraManager
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.ProjectPaths
import org.wysko.midis2jam2.testing.Spec
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks that the managers a performance is made of come up and go down cleanly.
 *
 * Each manager owns one concern of a running performance, and the app is assembled by
 * attaching the whole list. A manager that stops being attached - as the lyric display once
 * did - takes its feature with it, silently.
 */
class ManagerLifecycleTest {

    @Test
    @Spec("app.managers.lifecycle")
    fun `the managers a performance needs are all attached and initialised`() {
        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0)).use { performance ->
            val attached = performance.app.stateManager

            EXPECTED_MANAGERS.forEach { manager ->
                assertTrue(
                    attached.getState(manager.java) != null,
                    "${manager.simpleName} was not attached to the performance"
                )
            }

            assertTrue(
                performance.managers.all { it.isInitialized },
                "Some managers never finished initialising: " +
                    performance.managers.filterNot { it.isInitialized }.map { it::class.simpleName }
            )
        }
    }

    @Test
    fun `the key bindings are registered while a performance is running`() {
        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0)).use { performance ->
            val inputManager = performance.app.inputManager

            val unregistered = boundActionNames().filterNot { inputManager.hasMapping(it) }
            assertTrue(
                unregistered.isEmpty(),
                "These actions are declared in actions.yaml but were never registered with " +
                    "the input manager, so their keys would do nothing: $unregistered"
            )
        }
    }

    @Test
    fun `the key bindings are removed when the performance ends`() {
        val performance = HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0))
        val inputManager = performance.app.inputManager
        val actions = boundActionNames()

        // Detaching is what happens when a song finishes and another begins; mappings that
        // outlive their manager would accumulate across songs.
        performance.onEngineThread {
            performance.app.stateManager.getState(ActionsManager::class.java)?.let {
                performance.app.stateManager.detach(it)
            }
        }
        performance.onEngineThread { /* let the detach take effect on the next frame */ }

        val leftBehind = actions.filter { inputManager.hasMapping(it) }
        performance.close()

        assertEquals(
            emptyList(),
            leftBehind,
            "These key bindings survived the manager that registered them"
        )
    }

    @Test
    fun `a performance shuts down without error`() {
        val performance = HeadlessPerformance.start(MidiFixtures.theWholeBand())
        performance.stepInstruments(frames = 5)
        performance.close()
        performance.throwIfEngineFailed()
    }

    @Test
    fun `the playback manager drives the clock from the start of the intro`() {
        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0)).use { performance ->
            val playback = performance.app.stateManager.getState(PlaybackManager::class.java)
            assertTrue(playback != null, "No playback manager was attached")
        }
    }

    private companion object {

        /** The managers every performance is assembled from, whether or not a screen exists. */
        val EXPECTED_MANAGERS = listOf(
            PerformanceConfigState::class,
            ActionsManager::class,
            CameraManager::class,
            CollectorsManager::class,
            DrumSetVisibilityManager::class,
            StageManager::class,
            StandManager::class,
            PlaybackManager::class,
        )

        @Serializable
        private data class BoundAction(val name: String, val key: String)

        fun boundActionNames(): List<String> {
            val file = File(ProjectPaths.sharedAssets, "actions.yaml")
            return Yaml.default
                .decodeFromString(ListSerializer(BoundAction.serializer()), file.readText())
                .map { it.name }
        }
    }
}
