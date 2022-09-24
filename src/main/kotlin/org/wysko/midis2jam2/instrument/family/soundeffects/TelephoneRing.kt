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
package org.wysko.midis2jam2.instrument.family.soundeffects

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad
import java.util.Random

/** *You used to call me on my cellphone...* */
class TelephoneRing(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    SustainedInstrument(context, eventList) {

    /** The Up node. */
    private val upNode = Node().apply {
        localRotation = Quaternion().fromAngles(rad(19.0), 0f, 0f)
    }.also {
        instrumentNode.attachChild(it)
    }

    /** The Down node. */
    private val downNode = Node().apply {
        localRotation = Quaternion().fromAngles(rad(19.0), 0f, 0f)
    }.also {
        instrumentNode.attachChild(it)
    }

    /** The Up keys. */
    private val upKeys = Array(12) {
        context.loadModel("TelePhoneKey.obj", "TelePhoneKey${it.toKeyString()}Dark.bmp").apply {
            setLocalTranslation(1.2f * (it % 3 - 1), 3.89f, -2.7f - 1.2f * -(it / 3))
            upNode.attachChild(this)
        }
    }

    /** The Down keys. */
    private val downKeys = Array(12) {
        context.loadModel("TelePhoneKey.obj", "TelePhoneKey${it.toKeyString()}.bmp").apply {
            setLocalTranslation(1.2f * (it % 3 - 1), 3.4f, -2.7f - 1.2f * -(it / 3))
            cullHint = Always
            downNode.attachChild(this)
        }
    }

    /** For each key, is it playing? */
    private val playing: BooleanArray = BooleanArray(12)

    /** The handle. */
    private val handle: Spatial = context.loadModel("TelePhoneHandle.obj", "TelephoneHandle.bmp").also {
        instrumentNode.attachChild(it)
    }

    /** The amount to shake the handle. */
    private var force = 0f

    /** Random for phone animation. */
    private val random = Random()

    init {
        context.loadModel("TelePhoneBase.obj", "TelephoneBase.bmp").apply {
            (this as Node).getChild(0).setMaterial(context.unshadedMaterial("RubberFoot.bmp"))
        }.also {
            instrumentNode.attachChild(it)
        }

        instrumentNode.setLocalTranslation(0f, 1f, -50f)
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Turn off everything */
        playing.fill(false)
        upKeys.forEach { it.cullHint = Dynamic }
        downKeys.forEach { it.cullHint = Always }

        /* Turn on current note periods */
        currentNotePeriods.forEach {
            with((it.midiNote + 3) % 12) {
                playing[this] = true
                upKeys[this].cullHint = Always
                downKeys[this].cullHint = Dynamic
            }
        }

        /* Animate phone handle */
        handle.setLocalTranslation(0f, (2 + random.nextGaussian() * 0.3).toFloat() * force, 0f)
        handle.localRotation = Quaternion().fromAngles(
            rad(random.nextGaussian() * 3) * force,
            rad(random.nextGaussian() * 3) * force,
            0f
        )
        if (playing.any { it }) {
            force += 12 * delta
            force = 1f.coerceAtMost(force)
        } else {
            force -= 12 * delta
            force = 0f.coerceAtLeast(force)
        }
    }

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(13f * updateInstrumentIndex(delta), 0f, 0f)
    }
}

/**
 * Converts an integer to the telephone key string.
 */
fun Int.toKeyString(): String = when {
    this < 9 -> {
        (this + 1).toString()
    }

    this == 9 -> {
        "Star"
    }

    this == 10 -> {
        "0"
    }

    this == 11 -> {
        "Pound"
    }

    else -> throw IllegalStateException()
}
