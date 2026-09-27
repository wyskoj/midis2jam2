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

package org.wysko.midis2jam2.starter.configuration

import org.wysko.midis2jam2.domain.settings.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The configuration collection is threaded by hand through the whole engine, and every
 * consumer reaches into it with find(). A lookup that misses used to yield a null typed as
 * non-null, which surfaced as an unrelated crash somewhere downstream; it now fails loudly.
 */
class ConfigurationTest {

    @Test
    fun `find returns the configuration of the requested type`() {
        val home = Configuration.HomeConfiguration(selectedMidiDevice = "Gervill")
        val settings = Configuration.AppSettingsConfiguration(AppSettings())
        val configurations = listOf(home, settings)

        assertSame(home, configurations.find<Configuration.HomeConfiguration>())
        assertSame(settings, configurations.find<Configuration.AppSettingsConfiguration>())
    }

    @Test
    fun `find reports a missing configuration instead of yielding null`() {
        val configurations = listOf<Configuration>(Configuration.AppSettingsConfiguration(AppSettings()))

        val failure = assertFailsWith<IllegalStateException> {
            configurations.find<Configuration.HomeConfiguration>()
        }
        assertTrue(
            failure.message.orEmpty().contains("HomeConfiguration"),
            "The failure should name the missing configuration, but said: ${failure.message}"
        )
    }

    @Test
    fun `find on an empty collection reports the missing configuration`() {
        assertFailsWith<IllegalStateException> {
            emptyList<Configuration>().find<Configuration.AppSettingsConfiguration>()
        }
    }

    @Test
    fun `find returns the first match when a type appears more than once`() {
        val first = Configuration.HomeConfiguration(selectedMidiDevice = "first")
        val second = Configuration.HomeConfiguration(selectedMidiDevice = "second")

        assertSame(first, listOf(first, second).find<Configuration.HomeConfiguration>())
    }

    @Test
    fun `home configuration defaults to the built-in synthesizer and no looping`() {
        val home = Configuration.HomeConfiguration()

        assertEquals("Gervill", home.selectedMidiDevice)
        assertEquals(null, home.selectedSoundbank)
        assertEquals(null, home.lastMidiFileSelectedDirectory)
        assertEquals(false, home.isLooping)
    }
}
