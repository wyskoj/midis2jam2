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

import java.io.File

/** The image formats the engine can load as a cube map face. */
val CUBE_MAP_IMAGE_EXTENSIONS: List<String> = listOf("png", "jpg", "jpeg", "bmp", "gif")

/** Whether [fileName] has an extension the engine can load as a cube map face. */
fun isCubeMapImageName(fileName: String): Boolean =
    fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase() in CUBE_MAP_IMAGE_EXTENSIONS

/**
 * Returns [name], or if [existing] already has it, the first free "name (2).ext", "name (3).ext", ...
 */
fun uniqueImageName(existing: Set<String>, name: String): String {
    if (name !in existing) return name
    val base = name.substringBeforeLast('.', missingDelimiterValue = name)
    val extension = name.substringAfterLast('.', missingDelimiterValue = "").let { if (it.isEmpty()) "" else ".$it" }
    return generateSequence(2) { it + 1 }
        .map { "$base ($it)$extension" }
        .first { it !in existing }
}

/**
 * Copies [source] into [folder] so it can be chosen as a cube map face, and returns the name it has
 * there, or `null` when [source] is not a supported image.
 *
 * A file that is already in [folder] is not copied again. A different file with a name that is taken
 * keeps both, and the new copy gets a numbered name.
 */
fun importBackgroundImage(source: File, folder: File): String? {
    if (!source.isFile || !isCubeMapImageName(source.name)) return null
    if (source.absoluteFile.parentFile == folder.absoluteFile) return source.name

    folder.mkdirs()
    val name = uniqueImageName(folder.list()?.toSet().orEmpty(), source.name)
    source.copyTo(File(folder, name))
    return name
}
