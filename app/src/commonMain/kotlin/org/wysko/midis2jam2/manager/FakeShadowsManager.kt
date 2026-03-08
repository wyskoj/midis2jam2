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

package org.wysko.midis2jam2.manager

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.renderer.queue.RenderQueue.Bucket.Transparent
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.family.guitar.BassGuitar
import org.wysko.midis2jam2.instrument.family.guitar.Guitar
import org.wysko.midis2jam2.instrument.family.piano.Keyboard
import org.wysko.midis2jam2.instrument.family.strings.Harp
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.v3

class FakeShadowsManager : BaseManager() {
    private lateinit var keyboardShadow: Spatial
    private lateinit var harpShadows: List<Spatial>
    private lateinit var guitarShadows: List<Spatial>
    private lateinit var bassGuitarShadows: List<Spatial>

    override fun initialize(app: Application) {
        super.initialize(app)
        keyboardShadow = loadFakeShadow("Assets/PianoShadow.obj", "Assets/KeyboardShadow.png").apply {
            loc = v3(-47, 0.1, 3)
            rot = v3(0, 45, 0)
        }
        harpShadows = List(context.count<Harp>()) {
            loadFakeShadow("Assets/HarpShadow.obj", "Assets/HarpShadow.png").apply {
                loc = v3(-126, 0.1, -30 + 60 * it)
                rot = v3(0, -35, 0)
            }
        }
        guitarShadows = List(context.count<Guitar>()) {
            loadFakeShadow("Assets/GuitarShadow.obj", "Assets/GuitarShadow.png").apply {
                loc = v3(43.4f + 5 * (it * 1.5), 0.1f + 0.01f * (it * 1.5), 7.1)
                rot = v3(0, -49.0, 0)
            }
        }
        bassGuitarShadows = List(context.count<BassGuitar>()) {
            loadFakeShadow("Assets/BassShadow.obj", "Assets/BassShadow.png").apply {
                loc = v3(51.6f + 7 * it, 0.1f + 0.01f * it, -16.6)
                rot = v3(0, -43.5, 0)
            }
        }
    }

    override fun update(tpf: Float) {
        super.update(tpf)
        updateKeyboard()
        updateArray<Guitar>(guitarShadows)
        updateArray<BassGuitar>(bassGuitarShadows)
        updateArray<Harp>(harpShadows)
    }

    override fun onEnable() {
        (application as SimpleApplication).rootNode.run {
            attachChild(keyboardShadow)
            harpShadows.forEach(::attachChild)
            guitarShadows.forEach(::attachChild)
            bassGuitarShadows.forEach(::attachChild)
        }
    }

    override fun onDisable() {
        (application as SimpleApplication).rootNode.run {
            detachChild(keyboardShadow)
            harpShadows.forEach(::detachChild)
            guitarShadows.forEach(::detachChild)
            bassGuitarShadows.forEach(::detachChild)
        }
    }

    override fun cleanup(app: Application?): Unit = Unit
    private fun loadFakeShadow(modelName: String, textureName: String): Spatial =
        application.assetManager.loadModel(modelName).apply {
            setMaterial(
                Material(context.app.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
                    setTexture("ColorMap", app.assetManager.loadTexture(textureName))
                    additionalRenderState.blendMode = RenderState.BlendMode.Alpha
                    setFloat("AlphaDiscardThreshold", 0.01f)
                }
            )
            queueBucket = Transparent
        }

    private fun updateKeyboard() {
        keyboardShadow.cullHint = context.instruments.any { it is Keyboard && it.isVisible }.ch
        val keyboards = context.instruments.filterIsInstance<Keyboard>()
        keyboardShadow.localScale =
            v3(1, 1, keyboards.filter { it.isVisible }.maxOfOrNull { it.index }?.toFloat()?.plus(1) ?: 0f)
    }

    private inline fun <reified T> updateArray(shadows: List<Spatial>) {
        val numVisible = context.instruments.count { it is T && it.isVisible }
        shadows.forEachIndexed { index, shadow -> shadow.cullHint = (index < numVisible).ch }
    }

    private inline fun <reified T> PerformanceManager.count(): Int = instruments.count { it is T }
}