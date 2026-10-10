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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** How a `.blend` and its exported `.glb` are told apart when they've drifted out of step. */
class SourceStampsTest {

    private val root: File = Files.createTempDirectory("stamps").toFile()
    private val models = File(root, "models").apply { mkdirs() }

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** A minimal `.glb` whose scene is stamped with [stamp] (or not stamped). */
    private fun glb(file: File, stamp: String?) {
        val extras = stamp?.let { ""","extras":{"$SOURCE_PROPERTY":"$it"}""" } ?: ""
        var json = """{"asset":{"version":"2.0"},"scene":0,"scenes":[{"nodes":[]$extras}]}""".toByteArray()
        json += ByteArray((4 - json.size % 4) % 4) { ' '.code.toByte() }
        val buffer = ByteBuffer.allocate(20 + json.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0x46546C67).putInt(2).putInt(20 + json.size).putInt(json.size).putInt(0x4E4F534A).put(json)
        file.writeBytes(buffer.array())
    }

    @Test
    fun `a blend's hash is the file's, or the one its Git LFS pointer names`() {
        val content = "BLENDER-v502 not really a blend".toByteArray()
        val real = File(models, "Real.blend").apply { writeBytes(content) }
        assertEquals(sha256(content), SourceStamps.blendSha256(real))

        val pointer = File(models, "Pointer.blend").apply {
            writeText("version https://git-lfs.github.com/spec/v1\noid sha256:${sha256(content)}\nsize ${content.size}\n")
        }
        assertEquals(sha256(content), SourceStamps.blendSha256(pointer), "CI checks out LFS files as pointers")
    }

    @Test
    fun `a glb stamped with its blend's hash is up to date`() {
        val blend = File(models, "Alto.blend").apply { writeText("saved") }
        glb(File(models, "Alto.glb"), sha256(blend.readBytes()))
        assertEquals(emptyList(), SourceStamps.stale(root))
    }

    @Test
    fun `a blend changed, never exported, or exported by hand is out of date, and so is a glb with no blend`() {
        val changed = File(models, "Changed.blend").apply { writeText("edited after export") }
        glb(File(models, "Changed.glb"), sha256("as exported".toByteArray()))
        File(models, "Never.blend").writeText("new")
        File(models, "ByHand.blend").writeText("exported from File > Export")
        glb(File(models, "ByHand.glb"), null)
        glb(File(models, "Orphan.glb"), sha256(changed.readBytes()))

        assertEquals(
            listOf(
                StaleSource("ByHand.blend", "has a ByHand.glb not exported by tools/blender/export_models.py"),
                StaleSource("Changed.blend", "has changed since Changed.glb was exported"),
                StaleSource("Never.blend", "has never been exported"),
                StaleSource("Orphan.glb", "has no .blend it was exported from"),
            ),
            SourceStamps.stale(root),
        )
    }
}
