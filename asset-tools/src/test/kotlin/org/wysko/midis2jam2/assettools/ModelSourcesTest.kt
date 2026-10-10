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

package org.wysko.midis2jam2.assettools

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * The project's own model sources: every `.blend` must have been exported since it was last saved.
 *
 * The build reads the `.glb` beside each `.blend`, not the `.blend`, so a `.blend` saved but not exported again would
 * otherwise go unnoticed: the build passes and the game shows the old model.
 */
class ModelSourcesTest {

    @Test
    fun `every blend has been exported since it was last saved`() {
        val sharedAssets = File("../sharedAssets").canonicalFile
        val stale = SourceStamps.stale(sharedAssets)
        if (stale.isNotEmpty()) {
            fail(
                "These model sources are out of step with their .glb:\n" +
                    stale.joinToString("\n") { "  ${it.source} ${it.reason}" } +
                    "\nExport them with ./gradlew exportModels (see docs/ASSETS.md)."
            )
        }
    }
}
