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

package org.wysko.midis2jam2.testing

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Holds the behaviour catalogue and the test suite to each other.
 *
 * This is the test that makes "the app does what the website says" enforceable: it fails when
 * a documented behaviour has no test, and when a test cites a behaviour that isn't catalogued.
 */
class SpecCoverageTest {

    @Test
    fun `every catalogued behaviour is covered by a test, waived, or an acknowledged gap`() {
        val cited = citedSpecIds()
        val uncovered = SpecCatalogue.entries
            .filter { it.waived == null && it.pending == null }
            .filter { it.id !in cited }

        if (uncovered.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("${uncovered.size} catalogued behaviour(s) have no citing test.")
                    appendLine("Write a test annotated @Spec(\"<id>\"), or add a waived/pending reason.")
                    appendLine()
                    uncovered.sortedBy { it.id }.forEach {
                        appendLine("  ${it.id}")
                        appendLine("      ${it.claim}")
                        appendLine("      (${it.source})")
                    }
                }
            )
        }
    }

    @Test
    fun `every cited spec id exists in the catalogue`() {
        val unknown = citedSpecIds().filter { it !in SpecCatalogue.byId }
        if (unknown.isNotEmpty()) {
            fail(
                "Tests cite spec ids that are not in spec/behaviour.yaml: " +
                    unknown.sorted().joinToString(", ")
            )
        }
    }

    @Test
    fun `a covered behaviour is no longer marked pending`() {
        val cited = citedSpecIds()
        val stale = SpecCatalogue.entries.filter { it.pending != null && it.id in cited }
        if (stale.isNotEmpty()) {
            fail(
                "These behaviours now have tests - remove their `pending:` marker from " +
                    "spec/behaviour.yaml: " + stale.map { it.id }.sorted().joinToString(", ")
            )
        }
    }

    @Test
    fun `nothing is both waived and pending`() {
        val both = SpecCatalogue.entries.filter { it.waived != null && it.pending != null }
        assertTrue(both.isEmpty(), "Both waived and pending: ${both.map { it.id }}")
    }

    @Test
    fun `waived behaviours state a reason`() {
        val blank = SpecCatalogue.entries.filter {
            (it.waived != null && it.waived.isBlank()) || (it.pending != null && it.pending.isBlank())
        }
        assertTrue(blank.isEmpty(), "Waived or pending without a reason: ${blank.map { it.id }}")
    }

    @Test
    fun `catalogue is well formed`() {
        val entries = SpecCatalogue.entries
        assertTrue(entries.isNotEmpty(), "The behaviour catalogue is empty")

        val duplicates = entries.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertTrue(duplicates.isEmpty(), "Duplicate spec ids: $duplicates")

        val validPlatforms = setOf("desktop", "android")
        val badPlatforms = entries.filterNot { validPlatforms.containsAll(it.platforms) }
        assertTrue(badPlatforms.isEmpty(), "Unknown platforms in: ${badPlatforms.map { it.id }}")

        val emptyPlatforms = entries.filter { it.platforms.isEmpty() }
        assertTrue(emptyPlatforms.isEmpty(), "No platforms declared for: ${emptyPlatforms.map { it.id }}")

        val emptyClaims = entries.filter { it.claim.isBlank() }
        assertTrue(emptyClaims.isEmpty(), "Blank claim for: ${emptyClaims.map { it.id }}")
    }

    /** Reports coverage, so the gap is visible in the build log even while it is shrinking. */
    @Test
    fun `report coverage`() {
        val total = SpecCatalogue.entries.size
        val waived = SpecCatalogue.entries.count { it.waived != null }
        val pending = SpecCatalogue.entries.count { it.pending != null }
        val testable = total - waived
        val covered = SpecCatalogue.entries.count { it.waived == null && it.id in citedSpecIds() }
        println(
            "Spec coverage: $covered/$testable testable behaviours covered " +
                "($pending still pending, $waived waived, $total catalogued)"
        )
    }

    private companion object {

        /** Every spec id cited by an `@Spec` annotation anywhere in the compiled test classes. */
        fun citedSpecIds(): Set<String> = cited

        private val cited: Set<String> by lazy {
            val dirs = System.getProperty("midis2jam2.testClassesDirs")
                ?: error(
                    "System property midis2jam2.testClassesDirs is not set; " +
                        "the desktopTest task must provide it."
                )

            val loader = SpecCoverageTest::class.java.classLoader
            buildSet {
                dirs.split(File.pathSeparator)
                    .map(::File)
                    .filter { it.isDirectory }
                    .forEach { root ->
                        root.walkTopDown()
                            .filter { it.isFile && it.extension == "class" }
                            .forEach { classFile ->
                                val className = classFile.relativeTo(root).path
                                    .removeSuffix(".class")
                                    .replace(File.separatorChar, '.')
                                val type = runCatching {
                                    Class.forName(className, false, loader)
                                }.getOrNull() ?: return@forEach

                                runCatching { type.getAnnotation(Spec::class.java) }
                                    .getOrNull()
                                    ?.let { addAll(it.ids) }

                                runCatching { type.declaredMethods.toList() }
                                    .getOrDefault(emptyList())
                                    .forEach { method ->
                                        method.getAnnotation(Spec::class.java)?.let { addAll(it.ids) }
                                    }
                            }
                    }
            }
        }
    }
}
