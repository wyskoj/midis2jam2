/*
 * Copyright (C) 2021 Jacob Wysko
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

import com.jme3.material.Material
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad
import java.util.*

/** *You used to call me on my cellphone...* */
class TelephoneRing(context: Midis2jam2, eventList: List<MidiChannelSpecificEvent>) :
    SustainedInstrument(context, eventList) {

    /** The Up keys. */
    private val upKeys = arrayOfNulls<Spatial>(12)

    /** The Down keys. */
    private val downKeys = arrayOfNulls<Spatial>(12)

    /** The Up node. */
    private val upNode = Node()

    /** The Down node. */
    private val downNode = Node()

    /** For each key, is it playing? */
    val playing = BooleanArray(12)

    /** The Handle. */
    private val handle: Spatial

    /** The amount to shake the handle. */
    private var force = 0f

    /** Random for phone animation. */
    private val random = Random()

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)

        /* Turn off everything */
        playing.fill(false)
        upKeys.forEach { it!!.cullHint = Dynamic }
        downKeys.forEach { it!!.cullHint = Always }

        /* Turn on current note periods */
        currentNotePeriods.forEach {
            val keyIndex = (it.midiNote + 3) % 12
            playing[keyIndex] = true
            upKeys[keyIndex]!!.cullHint = Always
            downKeys[keyIndex]!!.cullHint = Dynamic
        }

        /* Animate phone handle */
        val isPlaying = playing.any { it }
        handle.setLocalTranslation(0f, (2 + random.nextGaussian() * 0.3).toFloat() * force, 0f)
        handle.localRotation = Quaternion().fromAngles(
            rad(random.nextGaussian() * 3) * force,
            rad(random.nextGaussian() * 3) * force, 0f
        )
        if (isPlaying) {
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

    init {
        val base = context.loadModel("TelePhoneBase.fbx", "TelephoneBase.bmp")

        /* Set rubber texture */
        (base as Node).getChild(0)
            .setMaterial(Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
                setTexture("ColorMap", context.assetManager.loadTexture("Assets/RubberFoot.bmp"))
            })

        handle = context.loadModel("TelePhoneHandle.obj", "TelephoneHandle.bmp")

        for (i in 0..11) {
            val key: String = when {
                i < 9 -> {
                    (i + 1).toString()
                }
                i == 9 -> {
                    "Star"
                }
                i == 10 -> {
                    "0"
                }
                i == 11 -> {
                    "Pound"
                }
                else -> throw IllegalStateException()
            }

            upKeys[i] = context.loadModel("TelePhoneKey.obj", "TelePhoneKey${key}Dark.bmp")
            downKeys[i] = context.loadModel("TelePhoneKey.obj", "TelePhoneKey$key.bmp")

            val row = -i / 3
            upKeys[i]!!.setLocalTranslation(1.2f * (i % 3 - 1), 3.89f, -2.7f - 1.2f * row)
            downKeys[i]!!.setLocalTranslation(1.2f * (i % 3 - 1), 3.4f, -2.7f - 1.2f * row)
            downKeys[i]!!.cullHint = Always

            upNode.attachChild(upKeys[i])
            downNode.attachChild(downKeys[i])
        }
        instrumentNode.run {
            attachChild(base)
            attachChild(handle)
            attachChild(upNode)
            attachChild(downNode)
            setLocalTranslation(0f, 1f, -50f)
        }
        upNode.localRotation = Quaternion().fromAngles(rad(19.0), 0f, 0f)
        downNode.localRotation = Quaternion().fromAngles(rad(19.0), 0f, 0f)
    }
}