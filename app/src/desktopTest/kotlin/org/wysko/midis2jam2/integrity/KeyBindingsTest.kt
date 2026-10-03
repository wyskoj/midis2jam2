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

package org.wysko.midis2jam2.integrity

import com.charleskorn.kaml.Yaml
import com.jme3.input.KeyInput
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import org.wysko.midis2jam2.manager.ActionsManager
import org.wysko.midis2jam2.testing.ProjectPaths
import org.wysko.midis2jam2.testing.Spec
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Holds `actions.yaml` to the keys the documentation advertises.
 *
 * The bindings live in a data file that nothing type-checks, so a typo silently produces an
 * action with no trigger at all - [ActionsManager.Action.trigger] resolves the key name
 * reflectively and yields `null` when it does not match a [KeyInput] constant.
 */
class KeyBindingsTest {

    @Test
    @Spec("app.actions.match-docs")
    fun `bundled key bindings match the documented keys`() {
        val bound = actions.associate { it.name to it.key }

        val mismatches = DOCUMENTED.mapNotNull { (action, expectedKey) ->
            when (val actual = bound[action]) {
                null -> "$action is documented but not bound in actions.yaml"
                expectedKey -> null
                else -> "$action is documented as $expectedKey but bound to $actual"
            }
        }

        if (mismatches.isNotEmpty()) {
            fail(
                "actions.yaml has drifted from the documentation " +
                    "(features/camera.md, features/playback.md, features/fretted-instruments.md):\n" +
                    mismatches.joinToString("\n") { "  $it" }
            )
        }
    }

    @Test
    fun `every bound key resolves to a real jMonkeyEngine key constant`() {
        val keyNames = KeyInput::class.java.declaredFields.map { it.name }.toSet()
        val unresolved = actions.filterNot { it.key in keyNames }

        assertTrue(
            unresolved.isEmpty(),
            "These bindings name a key that does not exist on KeyInput, so they would " +
                "register with a null trigger: " + unresolved.joinToString { "${it.name}=${it.key}" }
        )
    }

    @Test
    fun `no action is declared twice and no key is bound twice`() {
        val duplicateActions = actions.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
        assertTrue(duplicateActions.isEmpty(), "Duplicate action names in actions.yaml: $duplicateActions")

        val duplicateKeys = actions.groupingBy { it.key }.eachCount().filterValues { it > 1 }.keys
        assertTrue(duplicateKeys.isEmpty(), "The same key is bound to several actions: $duplicateKeys")
    }

    @Test
    fun `every action constant declared in code is present in actions yaml`() {
        val declared = ActionsManager.Companion::class.java.declaredFields
            .filter { it.type == String::class.java }
            .mapNotNull {
                it.isAccessible = true
                it.get(ActionsManager.Companion) as? String
            }
            .toSet()

        val bound = actions.map { it.name }.toSet()
        val missing = declared - bound

        assertTrue(
            missing.isEmpty(),
            "ActionsManager declares action constants that actions.yaml never binds, " +
                "so they can never fire: $missing"
        )
    }

    @Test
    fun `the six camera angle actions are all bound`() {
        val bound = actions.map { it.name }.toSet()
        val expected = (1..6).map { "camera_angle_$it" }
        assertEquals(emptyList(), expected.filterNot { it in bound }, "Unbound camera angle actions")
    }

    private companion object {

        /**
         * The bindings the documentation promises.
         *
         * Camera keys: features/camera.md. Playback keys: features/playback.md. The fretting readout:
         * features/fretted-instruments.md.
         * The remaining bindings are undocumented and are checked only for internal consistency.
         */
        val DOCUMENTED: Map<String, String> = mapOf(
            // features/playback.md
            "playback_play" to "KEY_SPACE",
            "playback_seek_forward" to "KEY_RIGHT",
            "playback_seek_backward" to "KEY_LEFT",
            // features/camera.md - predefined camera positions 1..6
            "camera_angle_1" to "KEY_1",
            "camera_angle_2" to "KEY_2",
            "camera_angle_3" to "KEY_3",
            "camera_angle_4" to "KEY_4",
            "camera_angle_5" to "KEY_5",
            "camera_angle_6" to "KEY_6",
            // features/camera.md - mode activation and reset
            "camera_plugin_auto" to "KEY_0",
            "camera_plugin_rotating" to "KEY_9",
            "camera_plugin_free" to "KEY_GRAVE",
        )

        @Serializable
        data class BoundAction(val name: String, val key: String)

        val actions: List<BoundAction> by lazy {
            val file = File(ProjectPaths.sharedAssets, "actions.yaml")
            assertTrue(file.isFile, "actions.yaml not found at ${file.absolutePath}")
            Yaml.default.decodeFromString(ListSerializer(BoundAction.serializer()), file.readText())
        }
    }
}
