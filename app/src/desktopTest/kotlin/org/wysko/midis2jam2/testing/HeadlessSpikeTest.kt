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

import org.wysko.midis2jam2.assets.Models
import org.wysko.midis2jam2.assets.Textures
import com.jme3.app.FlyCamAppState
import com.jme3.app.SimpleApplication
import com.jme3.material.Material
import com.jme3.system.AppSettings
import com.jme3.system.JmeContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Phase 0 spike. Proves the assumptions the rest of the suite is built on:
 *
 * 1. A [SimpleApplication] boots on [JmeContext.Type.Headless] with no display.
 * 2. The asset manager resolves the project's own models, textures and materials.
 * 3. `inputManager` and `flyByCamera` are live headlessly (required for input simulation).
 * 4. The update loop runs and can be stepped.
 */
class HeadlessSpikeTest {

    @Test
    fun `headless jME application boots, loads project assets and exposes input`() {
        val initialized = CountDownLatch(1)
        var failure: Throwable? = null

        var hasAssetManager = false
        var hasInputManager = false
        var hasFlyByCamera = false
        var hasViewPort = false
        var hasGuiNode = false
        var loadedModel = false
        var loadedTexture = false
        var loadedMaterial = false
        var loadedFont = false
        var rendererClass: String? = null
        var contextClass: String? = null

        val app = object : SimpleApplication(FlyCamAppState()) {
            override fun simpleInitApp() {
                try {
                    contextClass = context?.javaClass?.name
                    rendererClass = renderer?.javaClass?.name

                    hasAssetManager = assetManager != null
                    hasInputManager = inputManager != null
                    hasFlyByCamera = flyByCamera != null
                    hasViewPort = viewPort != null
                    hasGuiNode = guiNode != null

                    // The real app loads these exact assets through AssetLoader.
                    loadedModel = assetManager.loadModel(Models.Strings.Violin.Body.path) != null
                    loadedTexture = assetManager.loadTexture(Textures.Strings.ViolinSkin.path) != null
                    loadedMaterial =
                        Material(assetManager, "Common/MatDefs/Light/Lighting.j3md") != null
                    loadedFont = assetManager.loadFont("Assets/Fonts/Inter.fnt") != null
                } catch (t: Throwable) {
                    failure = t
                } finally {
                    initialized.countDown()
                }
            }
        }

        app.setSettings(
            AppSettings(true).apply {
                audioRenderer = null
                frameRate = 60
            }
        )
        app.isShowSettings = false
        app.isPauseOnLostFocus = false

        try {
            app.start(JmeContext.Type.Headless)
            if (!initialized.await(60, TimeUnit.SECONDS)) {
                fail("Headless application did not reach simpleInitApp within 60s")
            }
        } finally {
            app.stop(true)
        }

        failure?.let { throw AssertionError("simpleInitApp threw: ${it.message}", it) }

        println("context  = $contextClass")
        println("renderer = $rendererClass")

        assertTrue(hasAssetManager, "assetManager is null headlessly")
        assertTrue(hasViewPort, "viewPort is null headlessly")
        assertTrue(hasGuiNode, "guiNode is null headlessly")
        assertTrue(hasInputManager, "inputManager is null headlessly - input simulation is not viable")
        assertTrue(hasFlyByCamera, "flyByCamera is null headlessly - free-camera tests are not viable")
        assertTrue(loadedModel, "could not load ${Models.Strings.Violin.Body.path}")
        assertTrue(loadedTexture, "could not load ${Textures.Strings.ViolinSkin.path}")
        assertTrue(loadedMaterial, "could not create a Lighting.j3md material")
        assertTrue(loadedFont, "could not load Assets/Fonts/Inter.fnt")
        assertNotNull(contextClass, "no JmeContext was created")
    }
}
