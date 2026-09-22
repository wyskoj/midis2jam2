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

import org.wysko.midis2jam2.testing.ProjectPaths
import org.wysko.midis2jam2.testing.Spec
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Guards the translations.
 *
 * Adding an English string and forgetting the other seventeen locales is silent at runtime -
 * Compose falls back to the default - so nothing but a test will catch it.
 */
class LocalizationTest {

    @Test
    @Spec("app.i18n.complete-translations")
    fun `every locale defines every string the default locale defines`() {
        val default = stringKeys(defaultStringsFile)
        assertTrue(default.isNotEmpty(), "The default strings.xml declares no strings")

        val problems = buildList {
            translatedStringsFiles.forEach { (locale, file) ->
                val keys = stringKeys(file)
                (default - keys).takeIf { it.isNotEmpty() }?.let {
                    add("$locale is missing ${it.size} string(s): ${it.sorted().joinToString(", ")}")
                }
                (keys - default).takeIf { it.isNotEmpty() }?.let {
                    add("$locale declares ${it.size} string(s) the default locale does not: ${it.sorted().joinToString(", ")}")
                }
            }
        }

        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun `no locale declares the same string twice`() {
        val problems = (translatedStringsFiles + ("default" to defaultStringsFile)).mapNotNull { (locale, file) ->
            val names = STRING_NAME.findAll(file.readText()).map { it.groupValues[1] }.toList()
            val duplicates = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            if (duplicates.isEmpty()) null else "$locale declares duplicates: ${duplicates.sorted()}"
        }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    @Spec("app.i18n.locale-lists-agree")
    fun `the locales offered in settings are the locales that ship`() {
        val onDisk = localeDirectories.keys + "en"

        val offered = SETTINGS_LOCALE_LIST
            .find(File(ProjectPaths.repositoryRoot, SETTINGS_SCREEN_MODEL).readText())
            ?.groupValues?.get(1)
            ?.let { QUOTED.findAll(it).map { m -> m.groupValues[1] }.toSet() }
            ?: fail("Could not find getAvailableLocales() in $SETTINGS_SCREEN_MODEL")

        // The Android resource configuration spells the default locale "en-rUS".
        val configured = RESOURCE_CONFIGURATIONS
            .find(ProjectPaths.appBuildScript.readText())
            ?.groupValues?.get(1)
            ?.let { QUOTED.findAll(it).map { m -> m.groupValues[1].substringBefore("-r") }.toSet() }
            ?: fail("Could not find resourceConfigurations in app/build.gradle.kts")

        assertTrue(
            offered == onDisk,
            "Settings offers locales ${offered - onDisk} that do not ship, " +
                "and ships locales ${onDisk - offered} that settings does not offer"
        )
        assertTrue(
            configured == onDisk,
            "resourceConfigurations lists ${configured - onDisk} that do not ship, " +
                "and omits ${onDisk - configured}"
        )
    }

    private companion object {
        const val SETTINGS_SCREEN_MODEL =
            "app/src/commonMain/kotlin/org/wysko/midis2jam2/ui/settings/SettingsScreenModel.kt"

        val STRING_NAME = Regex("""<string\s+name="([^"]+)"""")
        val QUOTED = Regex(""""([^"]+)"""")
        val SETTINGS_LOCALE_LIST = Regex("""getAvailableLocales\(\)[^(]*\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
        val RESOURCE_CONFIGURATIONS = Regex("""resourceConfigurations[^(]*\(\s*listOf\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)

        val defaultStringsFile: File get() = File(ProjectPaths.composeResources, "values/strings.xml")

        /** Locale code to its `values-xx` directory. */
        val localeDirectories: Map<String, File>
            get() = ProjectPaths.composeResources.listFiles()
                .orEmpty()
                .filter { it.isDirectory && it.name.startsWith("values-") }
                .associateBy { it.name.removePrefix("values-") }

        val translatedStringsFiles: List<Pair<String, File>>
            get() = localeDirectories
                .mapNotNull { (locale, dir) ->
                    File(dir, "strings.xml").takeIf { it.isFile }?.let { locale to it }
                }
                .sortedBy { it.first }

        fun stringKeys(file: File): Set<String> =
            STRING_NAME.findAll(file.readText()).map { it.groupValues[1] }.toSet()
    }
}
