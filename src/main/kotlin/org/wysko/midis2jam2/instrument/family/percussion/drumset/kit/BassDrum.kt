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
package org.wysko.midis2jam2.instrument.family.percussion.drumset.kit

import com.jme3.math.Quaternion
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.family.percussion.drumset.DrumSetInstrument
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

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
class BassDrum(
    context: Midis2jam2,
    hits: MutableList<NoteEvent.NoteOn>,
    style: ShellStyle,
) :
    DrumSetInstrument(context, hits) {
    private val eventCollector = EventCollector(context, hits)

    private val beaterAssembly =
        Node().apply {
            geometry.attachChild(this)
            move(0f, 0f, 1.5f)
        }

    private val drum =
        context.modelD(style.bassDrumModel, style.shellTexture).apply {
            shadowMode = RenderQueue.ShadowMode.Cast
            recoilNode.attachChild(this)
        }

    private val beaterArm =
        context.modelD("DrumSet_BassDrumBeaterArm.obj", METAL_TEXTURE).apply {
            beaterAssembly.attachChild(this)
            move(0f, 5.5f, 1.35f)
            (this as Node).let { // Set correct materials
                it.children[0].setMaterial(context.reflectiveMaterial("ShinySilver.bmp"))
                it.children[1].setMaterial(context.diffuseMaterial("MetalTextureDark.bmp"))
            }
        }

    private val pedal =
        context.modelD("DrumSet_BassDrumPedal.obj", METAL_TEXTURE).apply {
            beaterAssembly.attachChild(this)
            move(0f, 0.5f, 7.5f)
        }

    private var rotationFactor = 0f

    init {
        beaterAssembly.attachChild(context.modelD("DrumSet_BassDrumBeaterHolder.obj", METAL_TEXTURE))
        geometry.move(0f, 0f, -80f)
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)
        val results = eventCollector.advanceCollectOne(time)
        recoilDrum(
            drum = drum,
            velocity = results?.velocity ?: 0,
            delta = delta,
            recoilAxis = Axis.Z,
            recoilDistance = BASS_DRUM_RECOIL_DISTANCE,
        )
        results?.let {
            rotationFactor = 1f
        }
        beaterArm.localRotation = beaterRotation()
        pedal.localRotation = pedalRotation()

        rotationFactor -= (delta.toDouble(SECONDS) * 8.0).toFloat()
        rotationFactor = rotationFactor.coerceAtLeast(0f)
    }

    private fun beaterRotation(): Quaternion = Quaternion().fromAngles(
        -rotationFactor + 0.87f,
        0f,
        0f,
    )

    private fun pedalRotation(): Quaternion = Quaternion().fromAngles(
        (-rotationFactor * 0.5f) + 0.2f,
        0f,
        0f,
    )
}
