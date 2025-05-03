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

package org.wysko.midis2jam2.starter

import com.jme3.app.SimpleApplication
import com.jme3.post.FilterPostProcessor
import com.jme3.post.filters.BloomFilter
import com.jme3.post.filters.BloomFilter.GlowMode.Objects
import com.jme3.renderer.queue.RenderQueue
import com.jme3.shadow.DirectionalLightShadowFilter
import com.jme3.shadow.EdgeFilteringMode
import com.jme3.system.lwjgl.LwjglContext
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.kmidi.midi.reader.readFile
import org.wysko.midis2jam2.DesktopMidis2jam2
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.GraphicsConfiguration
import org.wysko.midis2jam2.starter.configuration.QualityScale
import org.wysko.midis2jam2.starter.configuration.find
import org.wysko.midis2jam2.world.LightingSetup
import java.io.File
import javax.sound.midi.MidiDevice
import javax.sound.midi.Sequencer
import javax.sound.midi.Synthesizer

internal class Midis2jam2Application(
    private val file: File,
    private val configurations: Collection<Configuration>,
    private val onFinish: () -> Unit,
    private val sequencer: Sequencer,
    private val synthesizer: Synthesizer?,
    private val midiDevice: MidiDevice,
) : SimpleApplication() {
    fun execute() {
        applyConfigurations(configurations)
        start()
    }

    override fun simpleInitApp() {
        setupState(configurations)
        val sequence = StandardMidiFileReader().readFile(file).toTimeBasedSequence()
        DesktopMidis2jam2(
            sequencer = sequencer,
            midiFile = sequence,
            onClose = { stop() },
            configs = configurations,
            fileName = file.name,
            synthesizer = synthesizer,
            midiDevice = midiDevice,
        ).also {
            stateManager.attach(it)
            rootNode.attachChild(it.root)
        }
    }

    override fun stop() {
        onFinish()
        super.stop()
    }

    override fun destroy() {
        rootNode.detachAllChildren()
        assetManager.clearCache()
        renderer.invalidateState()
        inputManager.clearMappings()
        (context as LwjglContext).systemListener = null
        super.destroy()
    }
}

internal fun SimpleApplication.setupState(configurations: Collection<Configuration>) {
    renderer.defaultAnisotropicFilter = 4
    flyByCamera.run {
        unregisterInput()
        isEnabled = false
    }
    val graphicsConfig = configurations.find<GraphicsConfiguration>()

    val fpp = FilterPostProcessor(assetManager)

    val lightForShadows = LightingSetup.setupLights(rootNode)

    BloomFilter(Objects).also { fpp.addFilter(it) }

    if (graphicsConfig.shadowQuality != QualityScale.NONE) {
        rootNode.shadowMode = RenderQueue.ShadowMode.CastAndReceive
        val (nbSplits, mapSize) = GraphicsConfiguration.SHADOW_DEFINITION[graphicsConfig.shadowQuality]
            ?: (1 to 1024)

        DirectionalLightShadowFilter(assetManager, mapSize, nbSplits).apply {
            light = lightForShadows
            isEnabled = true
            shadowIntensity = 0.16f
            lambda = 0.65f
            edgeFilteringMode = EdgeFilteringMode.PCFPOISSON
            edgesThickness = 10
        }.also { fpp.addFilter(it) }
    }

    viewPort.addProcessor(fpp)
    fpp.numSamples = GraphicsConfiguration.ANTI_ALIASING_DEFINITION[graphicsConfig.antiAliasingQuality] ?: 1
}
