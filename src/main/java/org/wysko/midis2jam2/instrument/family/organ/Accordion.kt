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
package org.wysko.midis2jam2.instrument.family.organ

import com.jme3.material.Material
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.organ.Accordion.Companion.MAX_ANGLE
import org.wysko.midis2jam2.instrument.family.organ.Accordion.Companion.MIN_ANGLE
import org.wysko.midis2jam2.instrument.family.piano.Key
import org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument
import org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument.KeyColor.WHITE
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/** The accordion key texture file. */
const val ACCORDION_KEY_BMP: String = "AccordionKey.bmp"

/** The model file for the front of the white keys. */
const val ACCORDION_KEY_WHITE_FRONT_OBJ: String = "AccordionKeyWhiteFront.obj"

/** The model file for the back of the white keys. */
const val ACCORDION_KEY_WHITE_BACK_OBJ: String = "AccordionKeyWhiteBack.obj"

/**
 * The accordion is composed of 14 sections, where each section is a part of the accordion that independently rotates
 * around a pivot point when the accordion squeezes or expands. This given the illusion that the accordion is expanding
 * or contracting.
 *
 * On each frame, the [angle] is calculated with [calculateAngle]. This is the amount of squeeze
 * that is applied to the sections of the accordion. The bounds of the squeezing range are defined by [MIN_ANGLE]
 * and [MAX_ANGLE].
 *
 * The last section ([accordionSections[13]][accordionSections]) contains the keys. There are 26 keys, a dummy
 * white key, then two octaves of actual keys, then a dummy white key. The dummy keys never play and are just for show.
 *
 * Because the accordion only has 24 playable keys, notes are modulus 24.
 *
 * @see .accordionSections
 *
 * @see .keyByMidiNote
 * @see .dummyWhiteKey
 */
class Accordion(context: Midis2jam2, eventList: MutableList<MidiChannelSpecificEvent>, type: AccordionType) :
    KeyedInstrument(context, eventList, 0, 23) {

    /** The accordion is divided into fourteen sections. */
    private val accordionSections = Array(SECTION_COUNT) { Node() }

    /** The current amount of squeeze. */
    private var angle = MAX_ANGLE.toFloat()

    /** The current delta [angle]. That is, how much to change the angle per frame. */
    private var squeezingSpeed = 0.0

    /** True if the accordion is expanding, false if it is contracting. */
    private var expanding = false

    /**
     * Returns an AccordionWhiteKey that does nothing.
     *
     * @return a dummy white key
     */
    private fun dummyWhiteKey() = Node().apply {
        attachChild(context.loadModel(ACCORDION_KEY_WHITE_FRONT_OBJ, ACCORDION_KEY_BMP))
        attachChild(context.loadModel(ACCORDION_KEY_WHITE_BACK_OBJ, ACCORDION_KEY_BMP))
    }

    /**
     * Calculates the amount of squeeze to apply to the accordion and updates the [angle].
     *
     * If the [angle] is greater than [MAX_ANGLE], switches [expanding] to false to begin
     * contracting. If the [angle] is less than [MIN_ANGLE], switches [expanding] to true to begin
     * expanding.
     *
     * @param delta the amount of time since the last frame update
     */
    private fun calculateAngle(delta: Float) {
        if (keys.any { it.isBeingPressed }) {
            /* Squeeze at maximum speed if any key is being pressed. */
            squeezingSpeed = MAX_SQUEEZING_SPEED.toDouble()
        } else {
            if (squeezingSpeed > 0) {
                /* Gradually decrease squeezing speed */
                squeezingSpeed -= (delta * 3)
                squeezingSpeed.coerceAtLeast(0.0).also { squeezingSpeed = it }
            }
        }
        /* If expanding, increase the angle, otherwise decrease the angle. */
        if (expanding) {
            angle += (delta * squeezingSpeed).toFloat()
            /* Switch direction */
            if (angle > MAX_ANGLE) {
                expanding = false
            }
        } else {
            angle -= (delta * squeezingSpeed).toFloat()
            /* Switch direction */
            if (angle < MIN_ANGLE) {
                expanding = true
            }
        }
    }

    override fun tick(time: Double, delta: Float) {
        super.tick(time, delta)
        calculateAngle(delta)

        /* Set the rotation of each section */
        accordionSections.indices.forEach {
            accordionSections[it].localRotation = Quaternion().fromAngles(0f, 0f, rad(angle * (it - 7.5)))
        }
    }

    override fun keyByMidiNote(midiNote: Int): Key {
        return keys[midiNote % 24]
    }

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.setLocalTranslation(0f, 30 * updateInstrumentIndex(delta), 0f)
    }

    /** A single key on the accordion. It behaves just like any other key. */
    private inner class AccordionKey(midiNote: Int, startPos: Int) : Key() {
        override fun tick(delta: Float) {
            if (isBeingPressed) {
                keyNode.localRotation = Quaternion().fromAngles(0f, -0.1f, 0f)
                downNode.cullHint = Dynamic
                upNode.cullHint = Always
            } else {
                val angles = FloatArray(3)
                keyNode.localRotation.toAngles(angles)
                if (angles[1] < -0.0001) {
                    keyNode.localRotation =
                        Quaternion(floatArrayOf(0f, (angles[1] + 0.02f * delta * 50).coerceAtMost(0f), 0f))
                } else {
                    keyNode.localRotation = Quaternion(floatArrayOf(0f, 0f, 0f))
                    downNode.cullHint = Always
                    upNode.cullHint = Dynamic
                }
            }
        }

        init {
            if (midiValueToColor(midiNote) == WHITE) {
                /* Up key */
                val upKeyFront = context.loadModel(ACCORDION_KEY_WHITE_FRONT_OBJ, ACCORDION_KEY_BMP)
                val upKeyBack = context.loadModel(ACCORDION_KEY_WHITE_BACK_OBJ, ACCORDION_KEY_BMP)
                upNode.attachChild(upKeyFront)
                upNode.attachChild(upKeyBack)
                /* Down key */
                val downKeyFront = context.loadModel(ACCORDION_KEY_WHITE_FRONT_OBJ, "AccordionKeyDown.bmp")
                val downKeyBack = context.loadModel(ACCORDION_KEY_WHITE_BACK_OBJ, "AccordionKeyDown.bmp")
                downNode.attachChild(downKeyFront)
                downNode.attachChild(downKeyBack)
                keyNode.attachChild(upNode)
                keyNode.attachChild(downNode)
                keyNode.move(0f, -startPos + 6f, 0f)
            } else {
                /* Up key */
                val blackKey = context.loadModel("AccordionKeyBlack.obj", "AccordionKeyBlack.bmp")
                upNode.attachChild(blackKey)

                /* Down key */
                val blackKeyDown = context.loadModel("AccordionKeyBlack.obj", "AccordionKeyBlackDown.bmp")
                downNode.attachChild(blackKeyDown)
                keyNode.attachChild(upNode)
                keyNode.attachChild(downNode)
                keyNode.move(0f, -midiNote * (7 / 12f) + 6.2f, 0f)
            }
            downNode.cullHint = Always
        }
    }

    /** Defines a type of Accordion. */
    enum class AccordionType(
        /** The texture file of the case. */
        val textureCaseName: String,
        /** The texture file of the front of the case. */
        val textureCaseFrontName: String
    ) {
        /** The accordion. */
        ACCORDION("AccordionCase.bmp", "AccordionCaseFront.bmp"),

        /** The bandoneon. */
        BANDONEON("BandoneonCase.bmp", "BandoneonCaseFront.bmp");
    }

    companion object {
        /** The maximum angle that the accordion will expand to. */
        const val MAX_ANGLE: Int = 4

        /** The minimum angle that the accordion will contract to. */
        const val MIN_ANGLE: Int = 1

        /** The number of sections the accordion is divided into. */
        const val SECTION_COUNT: Int = 14

        /** The maximum speed at which the accordion squeezes. */
        const val MAX_SQUEEZING_SPEED: Int = 2
    }

    init {
        /* Load left case */
        val leftHandCase = context.loadModel("AccordionLeftHand.fbx", type.textureCaseName).also {
            accordionSections[0].attachChild(it)
        }

        /* Load leather strap */
        val leatherStrap = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
            setTexture("ColorMap", context.assetManager.loadTexture("Assets/LeatherStrap.bmp"))
        }

        /* Load rubber foot */
        val rubberFoot = Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
            setTexture("ColorMap", context.assetManager.loadTexture("Assets/RubberFoot.bmp"))
        }

        /* Set materials */
        (leftHandCase as Node).apply {
            getChild(1).setMaterial(leatherStrap)
            getChild(0).setMaterial(rubberFoot)
        }

        /* Add the keys */
        val keysNode = Node()
        accordionSections[SECTION_COUNT - 1].attachChild(keysNode)

        var whiteCount = 0
        keys = Array(24) {
            if (midiValueToColor(it) == WHITE) {
                AccordionKey(it, whiteCount++)
            } else {
                AccordionKey(it, it)
            }
        }
        /* Add dummy keys on each end */
        dummyWhiteKey().also {
            keysNode.attachChild(it)
            it.setLocalTranslation(0f, 7f, 0f)
        }
        dummyWhiteKey().also {
            keysNode.attachChild(it)
            it.setLocalTranslation(0f, -8f, 0f)
        }

        /* Attach keys to node */
        keys.forEach { keysNode.attachChild(it.keyNode) }

        keysNode.setLocalTranslation(-4f, 22f, -0.8f)

        /* Load and attach accordion folds */
        accordionSections.forEach { section ->
            section.attachChild(context.loadModel("AccordionFold.obj", "AccordionFold.bmp"))
        }

        /* Load right case */
        accordionSections[13].attachChild(context.loadModel("AccordionRightHand.obj", type.textureCaseFrontName))

        /* Attach accordion sections to node */
        accordionSections.forEach { instrumentNode.attachChild(it) }

        /* Positioning */
        instrumentNode.run {
            setLocalTranslation(-70f, 10f, -60f)
            localRotation = Quaternion().fromAngles(rad(0.0), rad(45.0), rad(-5.0))
        }
    }
}