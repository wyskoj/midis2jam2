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
import java.security.MessageDigest

/**
 * A model source whose `.glb` doesn't reflect it.
 *
 * @property source The source, as its path under `models/` (`Reed/Sax/Alto.blend`, or a `.glb` with no `.blend`).
 * @property reason Why, in words.
 */
data class StaleSource(val source: String, val reason: String)

/**
 * Keeps each `.blend` and the `.glb` exported from it in step.
 *
 * `tools/blender/export_models.py` stamps every `.glb` with the SHA-256 of the `.blend` it exported. A `.blend` edited
 * and saved but not exported again no longer matches its stamp; [stale] finds it, the tests fail on it, and
 * `./gradlew exportModels` exports it again.
 */
object SourceStamps {

    private const val LFS_POINTER = "version https://git-lfs.github.com/spec/v1"
    private val LFS_OID = Regex("""^oid sha256:([0-9a-f]{64})$""", RegexOption.MULTILINE)

    /**
     * The SHA-256 of the `.blend` [file], as hex. A checkout without Git LFS (CI's) has only the pointer file, which
     * names the real file's SHA-256, so that is read instead.
     */
    fun blendSha256(file: File): String {
        val bytes = file.readBytes()
        if (bytes.size < 1024) {
            val text = String(bytes, Charsets.UTF_8)
            if (text.startsWith(LFS_POINTER)) {
                return LFS_OID.find(text)?.groupValues?.get(1)
                    ?: throw AssetToolException("${file.name} is a Git LFS pointer with no sha256 oid")
            }
        }
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    /** Every `.blend` under `sharedAssets/models` whose `.glb` is missing or out of date, and every `.glb` without one. */
    fun stale(sharedAssets: File): List<StaleSource> {
        val models = File(sharedAssets, MODELS_SOURCE_DIR)
        if (!models.isDirectory) return emptyList()
        val files = models.walkTopDown().filter { it.isFile }.toList()
        val blends = files.filter { it.extension == "blend" }
        val glbs = files.filter { it.extension == "glb" }
        fun name(file: File) = file.relativeTo(models).invariantSeparatorsPath

        val fromBlends = blends.mapNotNull { blend ->
            val glb = File(blend.parentFile, blend.nameWithoutExtension + ".glb")
            val stamp = if (glb.isFile) GltfDocument.read(glb).sourceSha256 else null
            when {
                !glb.isFile -> StaleSource(name(blend), "has never been exported")
                stamp == null -> StaleSource(name(blend), "has a ${glb.name} not exported by tools/blender/export_models.py")
                stamp != blendSha256(blend) -> StaleSource(name(blend), "has changed since ${glb.name} was exported")
                else -> null
            }
        }
        val orphans = glbs.filter { !File(it.parentFile, it.nameWithoutExtension + ".blend").isFile }
            .map { StaleSource(name(it), "has no .blend it was exported from") }
        return (fromBlends + orphans).sortedBy { it.source }
    }
}
