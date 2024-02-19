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
package org.wysko.midis2jam2.instrument.family.organ

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.organ.Accordion.Companion.SQUEEZE_RANGE
import org.wysko.midis2jam2.instrument.family.piano.*
import org.wysko.midis2jam2.instrument.family.piano.Key.Color
import org.wysko.midis2jam2.instrument.family.piano.Key.Color.*
import org.wysko.midis2jam2.midi.MidiChannelEvent
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelD

private const val WHITE_KEY_FRONT = "AccordionKeyWhiteFront.obj"
private const val WHITE_KEY_BACK = "AccordionKeyWhiteBack.obj"
private const val BLACK_KEY = "AccordionKeyBlack.obj"

private const val WHITE_KEY_TEXTURE = "AccordionKey.bmp"
private const val WHITE_KEY_DOWN_TEXTURE = "AccordionKeyDown.bmp"
private const val BLACK_KEY_TEXTURE = "AccordionKeyBlack.bmp"
private const val BLACK_KEY_DOWN_TEXTURE = "AccordionKeyBlackDown.bmp"

/**
 * The accordion has fourteen sections, where each section is a part of the accordion that independently rotates
 * around a pivot point when the accordion squeezes or expands.
 * This gives the illusion that the accordion is expanding or contracting.
 *
 * On each frame, [calculateAngle] calculates the [angle].
 * This is the amount of squeeze that is applied to the sections of the accordion.
 * [SQUEEZE_RANGE] defines the bounds of the squeezing range.
 *
 * The last section ([accordionSections[13]][accordionSections]) has the keys.
 * There are twenty-six keys: a dummy white key, then two octaves of actual keys, then a dummy white key.
 * The dummy keys never play and are just for show.
 *
 * Because the accordion only has twenty-four playable keys, notes are modulus 24.
 */
class Accordion(context: Midis2jam2, eventList: MutableList<MidiChannelEvent>, type: Type) :
    KeyedInstrument(context, eventList, 0, 23) {
    override val keys: Array<Key> =
        let {
            var whiteCount = 0
            Array(24) {
                when (Color.fromNote(it)) {
                    White -> AccordionKey(it, whiteCount++, this)
                    Black -> AccordionKey(it, it, this)
                }
            }
        }

    /** The accordion sections. */
    private val accordionSections = Array(SECTION_COUNT) { Node() }

    /** The current amount of squeeze. */
    private var angle = SQUEEZE_RANGE.endInclusive

    /** The current delta [angle], that is, how much to change the angle per frame. */
    private var squeezingSpeed = 0.0

    /** True if the accordion is expanding, false if it is contracting. */
    private var expanding = false

    /**
     * Returns an AccordionWhiteKey that does nothing.
     *
     * @return a dummy white key
     */
    private fun dummyWhiteKey() =
        Node().apply {
            attachChild(context.modelD(WHITE_KEY_FRONT, WHITE_KEY_TEXTURE))
            attachChild(context.modelD(WHITE_KEY_BACK, WHITE_KEY_TEXTURE))
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
        // Squeeze at maximum speed if any key is being pressed.
        if (collector.currentNotePeriods.isNotEmpty()) {
            squeezingSpeed = MAX_SQUEEZING_SPEED.toDouble()
        } else {
            if (squeezingSpeed > 0) { // Gradually decrease squeezing speed
                squeezingSpeed -= (delta * 3)
                squeezingSpeed.coerceAtLeast(0.0).also { squeezingSpeed = it }
            }
        }

        // If expanding, increase the angle, otherwise decrease the angle.
        if (expanding) {
            angle += (delta * squeezingSpeed).toFloat() // Switch direction
            if (angle > SQUEEZE_RANGE.endInclusive) {
                expanding = false
            }
        } else {
            angle -= (delta * squeezingSpeed).toFloat() // Switch direction
            if (angle < SQUEEZE_RANGE.start) {
                expanding = true
            }
        }
    }

    override fun tick(
        time: Double,
        delta: Float,
    ) {
        super.tick(time, delta)
        calculateAngle(delta)

        // Set the rotation of each section
        accordionSections.indices.forEach {
            accordionSections[it].localRotation = Quaternion().fromAngles(0f, 0f, rad(angle * (it - 7.5)))
        }
    }

    override fun getKeyByMidiNote(midiNote: Int): Key {
        return keys[midiNote % 24]
    }

    override fun adjustForMultipleInstances(delta: Float) {
        root.setLocalTranslation(0f, 30 * updateInstrumentIndex(delta), 0f)
    }

    /** Defines a type of Accordion. */
    enum class Type(
        /** The texture file of the case. */
        val textureCaseName: String,
        /** The texture file of the front of the case. */
        val textureCaseFrontName: String,
    ) {
        /** The accordion. */
        ACCORDION("AccordionCase.bmp", "AccordionCaseFront.bmp"),

        /** The bandoneon. */
        BANDONEON("BandoneonCase.bmp", "BandoneonCaseFront.bmp"),
    }

    companion object {
        /** The range of the accordion's squeeze. */
        val SQUEEZE_RANGE: ClosedFloatingPointRange<Float> = 1f..4f

        /** The number of sections the accordion is divided into. */
        const val SECTION_COUNT: Int = 14

        /** The maximum speed at which the accordion squeezes. */
        const val MAX_SQUEEZING_SPEED: Int = 2

        /** The number of white keys on an accordion. */
        const val WHITE_KEY_COUNT: Float = 12f

        /** The keyboard configuration. */
        val KEY_CONFIGURATION: KeyboardConfiguration =
            KeyboardConfiguration(
                whiteKeyConfiguration =
                    KeyConfiguration.SeparateTextures(
                        frontKeyFile = WHITE_KEY_FRONT,
                        backKeyFile = WHITE_KEY_BACK,
                        upTexture = WHITE_KEY_TEXTURE,
                        downTexture = WHITE_KEY_DOWN_TEXTURE,
                    ),
                blackKeyConfiguration =
                    KeyConfiguration.SeparateTextures(
                        frontKeyFile = BLACK_KEY,
                        backKeyFile = null,
                        upTexture = BLACK_KEY_TEXTURE,
                        downTexture = BLACK_KEY_DOWN_TEXTURE,
                    ),
            )
    }

    override fun keyStatus(midiNote: Int): Key.State {
        collector.currentNotePeriods.firstOrNull { it.note % 24 == midiNote % 24 }?.let {
            return Key.State.Down(it.noteOn.velocity)
        } ?: return Key.State.Up
    }

    init { // Load left case
        val leftHandCase =
            context.modelD("AccordionLeftHand.obj", type.textureCaseName).also {
                accordionSections[0].attachChild(it)
            }

        // Set materials
        (leftHandCase as Node).apply {
            getChild(1).setMaterial(context.diffuseMaterial("LeatherStrap.bmp"))
            getChild(2).setMaterial(context.diffuseMaterial("RubberFoot.bmp"))
        }

        // Add the keys
        val keysNode = Node()
        accordionSections[SECTION_COUNT - 1].attachChild(keysNode)

        // Add dummy keys on each end
        dummyWhiteKey().also {
            keysNode.attachChild(it)
            it.setLocalTranslation(0f, 7f, 0f)
        }
        dummyWhiteKey().also {
            keysNode.attachChild(it)
            it.setLocalTranslation(0f, -8f, 0f)
        }

        // Attach keys to node
        keys.forEach { keysNode.attachChild(it.root) }

        keysNode.setLocalTranslation(-4f, 22f, -0.8f)

        // Load and attach accordion folds
        accordionSections.forEach { section ->
            section.attachChild(context.modelD("AccordionFold.obj", "AccordionFold.bmp"))
        }

        // Load right case
        accordionSections[13].attachChild(context.modelD("AccordionRightHand.obj", type.textureCaseFrontName))

        // Attach accordion sections to node
        accordionSections.forEach { geometry.attachChild(it) }

        // Positioning
        geometry.run {
            setLocalTranslation(-70f, 10f, -60f)
            localRotation = Quaternion().fromAngles(rad(0.0), rad(45.0), rad(-5.0))
        }
    }

    override fun toString(): String = super.toString() + formatProperty("angle", angle)
}
