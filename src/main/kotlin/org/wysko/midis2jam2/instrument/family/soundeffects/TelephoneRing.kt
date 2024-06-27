/*
 * Copyright (C) 2024 Jacob Wysko
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
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.loc
import org.wysko.midis2jam2.util.rot
import org.wysko.midis2jam2.util.v3
import org.wysko.midis2jam2.world.modelD
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

/** *You used to call me on my cellphone...* */
class TelephoneRing(context: Midis2jam2, eventList: List<MidiEvent>) :
    SustainedInstrument(context, eventList) {
    /** The Up node. */
    private val upNode =
        Node().apply {
            localRotation = Quaternion().fromAngles(rad(19.0), 0f, 0f)
        }.also {
            geometry.attachChild(it)
        }

    /** The Down node. */
    private val downNode =
        Node().apply {
            localRotation = Quaternion().fromAngles(rad(19.0), 0f, 0f)
        }.also {
            geometry.attachChild(it)
        }

    /** The Up keys. */
    private val upKeys =
        Array(12) {
            context.modelD("TelePhoneKey.obj", "TelePhoneKey${it.toKeyString()}Dark.bmp").apply {
                setLocalTranslation(1.2f * (it % 3 - 1), 3.89f, -2.7f - 1.2f * -(it / 3))
                upNode.attachChild(this)
            }
        }

    /** The Down keys. */
    private val downKeys =
        Array(12) {
            context.modelD("TelePhoneKey.obj", "TelePhoneKey${it.toKeyString()}.bmp").apply {
                setLocalTranslation(1.2f * (it % 3 - 1), 3.4f, -2.7f - 1.2f * -(it / 3))
                cullHint = Always
                downNode.attachChild(this)
            }
        }

    /** For each key, is it playing? */
    private val playing: BooleanArray = BooleanArray(12)

    /** The handle. */
    private val handle: Spatial =
        context.modelD("TelePhoneHandle.obj", "TelephoneHandle.bmp").also {
            geometry.attachChild(it)
        }

    /** The amount to shake the handle. */
    private var force = 0.0

    /** Random for phone animation. */
    private val random = Random()

    init {
        context.modelD("TelePhoneBase.obj", "TelephoneBase.bmp").apply {
            (this as Node).getChild(0).setMaterial(context.diffuseMaterial("RubberFoot.bmp"))
        }.also {
            geometry.attachChild(it)
        }

        geometry.setLocalTranslation(0f, 1f, -50f)
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)

        // Turn off everything
        playing.fill(false)
        upKeys.forEach { it.cullHint = Dynamic }
        downKeys.forEach { it.cullHint = Always }

        // Turn on current note periods
        collector.currentTimedArcs.forEach {
            with((it.note + 3) % 12) {
                playing[this] = true
                upKeys[this].cullHint = Always
                downKeys[this].cullHint = Dynamic
            }
        }

        // Animate phone handle
        handle.loc = v3(0f, (2 + random.nextGaussian() * 0.3).toFloat() * force, 0f)
        handle.rot = v3(
            rad(random.nextGaussian() * 3) * force,
            rad(random.nextGaussian() * 3) * force,
            0f,
        )
        if (playing.any { it }) {
            force += 12 * delta.toDouble(SECONDS)
            force = 1.0.coerceAtMost(force)
        } else {
            force -= 12 * delta.toDouble(SECONDS)
            force = 0.0.coerceAtLeast(force)
        }
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        root.setLocalTranslation(13f * updateInstrumentIndex(delta), 0f, 0f)
    }
}

/**
 * Converts an integer to the telephone key string.
 */
fun Int.toKeyString(): String =
    when {
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
