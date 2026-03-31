/*
 * Copyright (C) 2025 Jacob Wysko
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

package org.wysko.midis2jam2.domain

import android.content.Context
import io.github.vinceglb.filekit.core.PlatformFile

internal data class SoundbankImportResult(
    val importedPaths: List<String>,
    val hasInvalidSelection: Boolean,
)

internal suspend fun Context.importValidSoundbanks(
    files: List<PlatformFile>,
): SoundbankImportResult {
    val importedPaths = files.mapNotNull { platformFile ->
        runCatching {
            if (!platformFile.name.endsWith(".sf2", ignoreCase = true)) {
                null
            } else {
                val bytes = platformFile.readBytes()
                copyBytesToInternalStorage(platformFile.name, bytes)
            }
        }.getOrNull()
    }
    return SoundbankImportResult(
        importedPaths = importedPaths,
        hasInvalidSelection = files.isNotEmpty() && importedPaths.isEmpty(),
    )
}
