/*
 * Copyright (C) 2022 Jacob Wysko
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
package org.wysko.midis2jam2.instrument.family.percussion.drumset

import com.jme3.math.Quaternion
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.family.percussion.Retexturable
import org.wysko.midis2jam2.instrument.family.percussion.RetextureType
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.world.Axis

private const val METAL_TEXTURE = "MetalTexture.bmp"
private const val BASS_DRUM_RECOIL_DISTANCE = -3f

/**
 * The bass drum has three major animation components:
 *
 * * [beaterArm] swings up and hits the bass drum
 * * [pedal] is pressed down
 * * [drum] is recoiled
 *
 * The animation has no future reference. That is, when a note is played, the animation starts immediately and takes
 * time to recoil.
 */
class BassDrum(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) :
    PercussionInstrument(context, hits), Retexturable {

    private val eventCollector = EventCollector(hits, context)

    private val beaterAssembly = Node().apply {
        instrumentNode.attachChild(this)
        move(0f, 0f, 1.5f)
    }

    private val drum = context.loadModel("DrumSet_BassDrum.obj", "DrumShell.bmp").apply {
        shadowMode = RenderQueue.ShadowMode.Cast
        recoilNode.attachChild(this)
    }

    private val beaterArm = context.loadModel("DrumSet_BassDrumBeaterArm.obj", METAL_TEXTURE).apply {
        beaterAssembly.attachChild(this)
        move(0f, 5.5f, 1.35f)
        (this as Node).let { // Set correct materials
            it.children[0].setMaterial(context.reflectiveMaterial("ShinySilver.bmp"))
            it.children[1].setMaterial(context.unshadedMaterial("MetalTextureDark.bmp"))
        }
    }

    private val pedal = context.loadModel("DrumSet_BassDrumPedal.obj", METAL_TEXTURE).apply {
        beaterAssembly.attachChild(this)
        move(0f, 0.5f, 7.5f)
    }

    private var rotationFactor = 0f

    init {
        beaterAssembly.attachChild(context.loadModel("DrumSet_BassDrumBeaterHolder.obj", METAL_TEXTURE))
        instrumentNode.move(0f, 0f, -80f)
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        val results = eventCollector.advanceCollectOne(time)
        recoilDrum(
            drum = drum,
            velocity = results?.velocity ?: 0,
            delta = delta,
            recoilAxis = Axis.Z,
            recoilDistance = BASS_DRUM_RECOIL_DISTANCE
        )
        results?.let {
            rotationFactor = 1f
        }
        beaterArm.localRotation = beaterRotation()
        pedal.localRotation = pedalRotation()

        rotationFactor -= delta * 8f
        rotationFactor = rotationFactor.coerceAtLeast(0f)
    }

    private fun beaterRotation(): Quaternion {
        return Quaternion().fromAngles(
            -rotationFactor + 0.87f,
            0f,
            0f
        )
    }

    private fun pedalRotation(): Quaternion {
        return Quaternion().fromAngles(
            (-rotationFactor * 0.5f) + 0.2f,
            0f,
            0f
        )
    }

    override fun drum(): Spatial = drum
    override fun retextureType(): RetextureType = RetextureType.OTHER
    override fun toString(): String {
        return super.toString() + buildString {
            append(debugProperty("rotationFactor", rotationFactor))
        }
    }
}
