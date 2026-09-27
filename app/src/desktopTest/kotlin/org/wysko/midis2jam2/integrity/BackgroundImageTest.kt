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

import com.jme3.asset.AssetManager
import com.jme3.asset.plugins.FileLocator
import com.jme3.system.JmeSystem
import com.jme3.texture.plugins.AWTLoader
import org.wysko.midis2jam2.domain.BackgroundImageRepository
import org.wysko.midis2jam2.testing.Spec
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The custom background images a user drops into their own folder.
 *
 * The app offers whatever is in that folder and only finds out whether it can read a file
 * when it tries to build the cubemap, so the formats the documentation promises are checked
 * here against what the engine can actually load.
 */
class BackgroundImageTest {

    @Test
    @Spec("background.cubemap.folder")
    fun `background images are read from the documented folder`() {
        val folder = BackgroundImageRepository().getTexturesFolder()

        assertTrue(
            folder.toPath().endsWith(File(".midis2jam2", "backgrounds").toPath()),
            "The documentation tells people to put images in ~/.midis2jam2/backgrounds, " +
                "but the app reads ${folder.absolutePath}"
        )
        assertTrue(
            folder.parentFile.parentFile == File(System.getProperty("user.home")),
            "The backgrounds folder should sit in the user's home directory, " +
                "but it is at ${folder.absolutePath}"
        )
    }

    @Test
    @Spec("background.cubemap.formats")
    fun `every documented image format can actually be loaded`() {
        val folder = createTempFolder()
        val assetManager = assetManagerFor(folder)

        val unreadable = DOCUMENTED_FORMATS.mapNotNull { format ->
            val file = File(folder, "face.$format")
            if (!writeSquareImage(file, format)) return@mapNotNull "$format could not even be written"

            val loaded = runCatching { assetManager.loadTexture(file.name) }
            when {
                loaded.isFailure -> "$format failed to load: ${loaded.exceptionOrNull()?.message}"
                loaded.getOrNull() == null -> "$format loaded as nothing"
                else -> null
            }
        }

        if (unreadable.isNotEmpty()) {
            fail(
                "The documentation promises these formats work as cubemap images:\n" +
                    unreadable.joinToString("\n") { "  $it" }
            )
        }
    }

    @Test
    fun `the folder lists whatever the user put in it`() {
        // The app does not filter by extension, so anything dropped in the folder is offered
        // and only rejected when it fails to load. Worth knowing when reading the next test.
        val repository = BackgroundImageRepository()

        assertTrue(
            repository.getAvailableImages().none { it.contains(File.separatorChar) },
            "The available images should be plain file names"
        )
    }

    private companion object {

        /** The formats features/background.md promises. */
        val DOCUMENTED_FORMATS = listOf("jpg", "png", "bmp", "gif")

        const val IMAGE_SIZE = 16

        fun createTempFolder(): File =
            java.nio.file.Files.createTempDirectory("midis2jam2-backgrounds").toFile().apply { deleteOnExit() }

        /** Cubemap faces must be square, so the fixtures are too. */
        fun writeSquareImage(file: File, format: String): Boolean {
            val image = BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB).apply {
                createGraphics().run {
                    color = Color.BLUE
                    fillRect(0, 0, IMAGE_SIZE, IMAGE_SIZE)
                    dispose()
                }
            }
            return ImageIO.write(image, format, file)
        }

        fun assetManagerFor(folder: File): AssetManager =
            JmeSystem.newAssetManager().apply {
                registerLoader(AWTLoader::class.java, *DOCUMENTED_FORMATS.toTypedArray())
                registerLocator(folder.absolutePath, FileLocator::class.java)
            }
    }
}
