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
package org.wysko.midis2jam2.instrument.family.percussion.drumset

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue.collectOne
import org.wysko.midis2jam2.instrument.family.percussion.Retexturable
import org.wysko.midis2jam2.instrument.family.percussion.RetextureType
import org.wysko.midis2jam2.instrument.family.percussive.Stick
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.Utils.rad

/** Texture file for metal texture. */
const val METAL_TEXTURE: String = "MetalTexture.bmp"

/**
 * The bass drum has three major animation components:
 *
 * * [beaterArm] swings up and hits the bass drum
 * * [pedal] is pressed down
 * * [drumNode] is recoiled
 *
 * The animation has no future reference. That is, when a note is played, the animation starts immediately and takes
 * time to recoil.
 */
class BassDrum(context: Midis2jam2, hits: MutableList<MidiNoteOnEvent>) : PercussionInstrument(context, hits),
    Retexturable {

    /** The arm that swings to hit the bass drum. */
    private val beaterArm: Spatial

    /** The pedal is pressed when a note is played and slowly comes back to its idle position. */
    private val pedal: Spatial

    /** The Drum node. */
    private val drumNode = Node()

    override fun tick(time: Double, delta: Float) {
        val nextHit = collectOne(hits, context, time)
        if (nextHit == null) { // We need to recoil all animation components

            /* Move the drum forward if it needs to. Coerce Z to at most 0 to not overshoot the idle position. */
            drumNode.setLocalTranslation(
                0f,
                0f,
                (drumNode.localTranslation.z + DRUM_RECOIL_COMEBACK * delta).coerceAtMost(0f)
            )

            /* Gradually rotate the beater back to its idle position */
            var newBeaterAngle = beaterArm.localRotation.toAngles(FloatArray(3))[0] + 8f * delta
            newBeaterAngle = rad(Stick.MAX_ANGLE).coerceAtMost(newBeaterAngle)
            beaterArm.localRotation = Quaternion().fromAngles(
                newBeaterAngle,
                0f,
                0f
            )

            /* Gradually rotate the pedal back to its idle position */
            var newPedalAngle =
                (pedal.localRotation.toAngles(FloatArray(3))[0] + 8f * delta * (PEDAL_MAX_ANGLE / Stick.MAX_ANGLE))
                    .toFloat()
            newPedalAngle = rad(PEDAL_MAX_ANGLE.toDouble()).coerceAtMost(newPedalAngle)
            pedal.localRotation = Quaternion().fromAngles(
                newPedalAngle,
                0f,
                0f
            )

        } else { // There is a note, so perform animation.
            beaterArm.localRotation = Quaternion().fromAngles(0f, 0f, 0f)
            pedal.localRotation = Quaternion().fromAngles(0f, 0f, 0f)
            drumNode.setLocalTranslation(0f, 0f, (-3 * velocityRecoilDampening(nextHit.velocity)).toFloat())
        }
    }

    companion object {
        /** The maximum angle the pedal will fall back to when at rest. */
        private const val PEDAL_MAX_ANGLE = 20
    }

    private val drum = context.loadModel("DrumSet_BassDrum.obj", "DrumShell.bmp")

    init {
        /* Load bass drum */
        drumNode.attachChild(drum)

        /* Load beater arm */
        beaterArm = context.loadModel("DrumSet_BassDrumBeaterArm.fbx", METAL_TEXTURE)

        /* Load beater holder */
        val bassDrumBeaterHolder = context.loadModel("DrumSet_BassDrumBeaterHolder.fbx", METAL_TEXTURE)
        val holder = bassDrumBeaterHolder as Node

        /* Apply materials */
        val arm = beaterArm as Node
        val shinySilverMaterial = context.reflectiveMaterial("Assets/ShinySilver.bmp")
        val darkMetalMaterial = context.unshadedMaterial("Assets/MetalTextureDark.bmp")
        arm.run {
            getChild(0).setMaterial(shinySilverMaterial)
            getChild(1).setMaterial(darkMetalMaterial)
        }
        holder.getChild(0).setMaterial(darkMetalMaterial)

        /* Load pedal */
        pedal = context.loadModel("DrumSet_BassDrumPedal.obj", METAL_TEXTURE)


        val beaterNode = Node()
        beaterNode.run {
            attachChild(beaterArm)
            attachChild(bassDrumBeaterHolder)
            attachChild(pedal)
        }

        highLevelNode.attachChild(drumNode)
        highLevelNode.attachChild(beaterNode)

        beaterArm.setLocalTranslation(0f, 5.5f, 1.35f)
        beaterArm.setLocalRotation(Quaternion().fromAngles(rad(Stick.MAX_ANGLE), 0f, 0f))

        pedal.localRotation = Quaternion().fromAngles(rad(PEDAL_MAX_ANGLE.toDouble()), 0f, 0f)
        pedal.setLocalTranslation(0f, 0.5f, 7.5f)

        beaterNode.setLocalTranslation(0f, 0f, 1.5f)
        highLevelNode.setLocalTranslation(0f, 0f, -80f)
    }

    override fun drum(): Spatial = drum
    override fun retextureType(): RetextureType = RetextureType.OTHER
}