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

import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.OPEN_HI_HAT
import org.wysko.midis2jam2.midi.PEDAL_HI_HAT
import java.lang.Integer.max

private val CLOSED_POSITION = Vector3f(0f, 1.2f, 0f)
private val OPEN_POSITION = Vector3f(0f, 2f, 0f)
private const val AMPLITUDE = 0.25
private const val DAMPENING = 2.0
private const val WOBBLE_SPEED = 10.0

/** The hi-hat. */
class HiHat(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) :
    PercussionInstrument(context, hits) {

    private val cymbalsNode = Node().apply {
        recoilNode.attachChild(this)
        scale(1.3f)
        move(0f, 0f, -13f)
    }

    private val topCymbal: Spatial = context.loadModel(
        model = "DrumSet_Cymbal.obj",
        texture = "CymbalSkinSphereMap.bmp",
        brightness = 0.7f
    ).apply {
        localTranslation = CLOSED_POSITION // Start in closed position
        cymbalsNode.attachChild(this)
    }

    init {
        // Add bottom cymbal
        context.loadModel(
            model = "DrumSet_Cymbal.obj",
            texture = "CymbalSkinSphereMap.bmp",
            brightness = 0.7f
        ).apply {
            rotate(FastMath.PI, 0f, 0f) // Rotate upside down
            cymbalsNode.attachChild(this)
        }
    }

    private val stick: Striker = Striker(
        context = context,
        strikeEvents = hits.filter { it.note != PEDAL_HI_HAT },
        stickModel = StickType.DRUMSET_STICK
    ).apply {
        setParent(recoilNode)
        node.move(0f, 1f, 2f)
    }

    private val cymbalAnimator = CymbalAnimator(topCymbal, AMPLITUDE, WOBBLE_SPEED, DAMPENING)

    private val pedalEventCollector = EventCollector(
        events = hits.toList().filter { it.note == PEDAL_HI_HAT },
        context = context
    )

    init {
        instrumentNode.move(-6f, 22f, -72f)
        instrumentNode.rotate(0f, 1.57f, 0f)
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        val stickResults = stick.tick(time, delta)
        stickResults.strike?.let {
            cymbalAnimator.strike()
            topCymbal.localTranslation = when (it.note) {
                OPEN_HI_HAT -> OPEN_POSITION
                else -> {
                    cymbalAnimator.cancel()
                    CLOSED_POSITION
                }
            }
        }

        val pedalResults = pedalEventCollector.advanceCollectOne(time)
        pedalResults?.let {
            cymbalAnimator.cancel()
            topCymbal.localTranslation = CLOSED_POSITION
        }

        recoilDrum(
            drum = recoilNode,
            velocity = max(stickResults.strike?.velocity ?: 0, pedalResults?.velocity ?: 0),
            delta = delta,
            recoilDistance = -0.7f,
            recoilSpeed = 5f
        )

        cymbalAnimator.tick(delta)
    }
}
