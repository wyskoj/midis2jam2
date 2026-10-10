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
package org.wysko.midis2jam2.instrument.family.organ

import org.wysko.midis2jam2.assets.MaterialAsset
import org.wysko.midis2jam2.assets.Materials
import org.wysko.midis2jam2.world.model
import org.wysko.midis2jam2.assets.Models
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.instrument.MultipleInstancesLinearAdjustment
import org.wysko.midis2jam2.instrument.family.piano.Key
import org.wysko.midis2jam2.instrument.family.piano.Key.Color
import org.wysko.midis2jam2.instrument.family.piano.Key.Color.Black
import org.wysko.midis2jam2.instrument.family.piano.Key.Color.White
import org.wysko.midis2jam2.instrument.family.piano.KeyConfiguration
import org.wysko.midis2jam2.instrument.family.piano.KeyboardConfiguration
import org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.assetLoader
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

private val WHITE_KEY_FRONT = Models.Organ.Accordion.KeyWhiteFront
private val WHITE_KEY_BACK = Models.Organ.Accordion.KeyWhiteBack
private val BLACK_KEY = Models.Organ.Accordion.KeyBlack

private val WHITE_KEY_TEXTURE = Materials.Diffuse.AccordionKey
private val WHITE_KEY_DOWN_TEXTURE = Materials.Diffuse.AccordionKeyDown
private val BLACK_KEY_TEXTURE = Materials.Diffuse.AccordionKeyBlack
private val BLACK_KEY_DOWN_TEXTURE = Materials.Diffuse.AccordionKeyBlackDown

private val SQUEEZE_RANGE: ClosedFloatingPointRange<Double> = 1.0..4.0
private const val SECTION_COUNT = 14
private const val MAX_SQUEEZING_SPEED = 2.0

/**
 * The accordion.
 *
 * @param context Context to the main class.
 * @param eventList The list of MIDI events.
 * @param type The type of accordion.
 */
class Accordion(context: PerformanceManager, eventList: List<MidiEvent>, type: Type) :
    KeyedInstrument(context, eventList, 0, 23), MultipleInstancesLinearAdjustment {
    override val multipleInstancesDirection: Vector3f = v3(0, 30, 0)
    override val keys: Array<Key> = let {
        var whiteCount = 0
        Array(24) {
            when (Color.fromNoteNumber(it.toByte())) {
                White -> AccordionKey(this, it.toByte(), whiteCount++)
                Black -> AccordionKey(this, it.toByte(), it)
            }
        }
    }
    private val accordionSections = Array(SECTION_COUNT) { Node() }
    private var angle = SQUEEZE_RANGE.endInclusive
    private var squeezingSpeed = 0.0
    private var expanding = false

    init {
        accordionSections.first().run {
            +context.model(Models.Organ.Accordion.LeftHand).also {
                (it as Node).getChild("Case").material = context.assetLoader.material(type.case)
            }
        }

        accordionSections.last().run {
            +context.model(Models.Organ.Accordion.RightHand).apply {
                setMaterial(context.assetLoader.material(type.caseFront))
            }
            +node {
                loc = v3(-4, 22, -0.8)
                +dummyWhiteKey().also {
                    it.loc = v3(0, 7, 0)
                }
                +dummyWhiteKey().also {
                    it.loc = v3(0, -8, 0)
                }
                keys.forEach { +it.root }
            }
        }

        accordionSections.forEach {
            geometry += it
            it += context.model(Models.Organ.Accordion.Fold)
        }

        placement.run {
            loc = v3(-75, 10, -65)
            rot = v3(0, 45, -5)
        }
    }

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)

        this.squeezingSpeed = calculateSqueezeSpeed(delta)
        this.angle += calculateDeltaAngle(delta, squeezingSpeed)
        calculateIsExpandingChange()?.let { this.expanding = it }
        accordionSections.forEachIndexed { index, node -> node.rot = v3(0, 0, angle * (index - 7.5)) }
    }

    override fun getKeyByMidiNote(midiNote: Int): Key = keys[midiNote % 24]

    override fun keyStatus(midiNote: Byte): Key.State {
        collector.currentTimedArcs.firstOrNull { it.note % 24 == midiNote % 24 }?.let {
            return Key.State.Down(it.noteOn.velocity)
        } ?: return Key.State.Up
    }

    private fun calculateDeltaAngle(delta: Duration, squeezeSpeed: Double): Double =
        delta.toDouble(SECONDS) * squeezeSpeed * expanding.sign

    private fun calculateIsExpandingChange(): Boolean? = when {
        angle < SQUEEZE_RANGE.start -> true
        angle > SQUEEZE_RANGE.endInclusive -> false
        else -> null
    }

    private fun dummyWhiteKey() = node {
        +context.model(WHITE_KEY_FRONT)
        +context.model(WHITE_KEY_BACK)
    }

    private fun calculateSqueezeSpeed(delta: Duration) = when {
        collector.currentTimedArcs.isNotEmpty() -> MAX_SQUEEZING_SPEED
        else -> interpolateTo(squeezingSpeed, 0.0, delta, 3.0)
    }

    /**
     * A type of accordion.
     *
     * @property case The material of the accordion case.
     * @property caseFront The material of the accordion case front.
     */
    enum class Type(val case: MaterialAsset, val caseFront: MaterialAsset) {

        /**
         * The accordion.
         */
        Accordion(Materials.Diffuse.AccordionCase, Materials.Diffuse.AccordionCaseFront),

        /**
         * The bandoneon.
         */
        Bandoneon(Materials.Diffuse.BandoneonCase, Materials.Diffuse.BandoneonCaseFront),
    }

    companion object {
        /**
         * The number of white keys on the accordion.
         */
        const val WHITE_KEY_COUNT: Int = 12

        /** The keyboard configuration. */
        val keyboardConfiguration: KeyboardConfiguration = KeyboardConfiguration(
            whiteKeyConfiguration = KeyConfiguration.SeparateTextures(
                frontKeyFile = WHITE_KEY_FRONT,
                backKeyFile = WHITE_KEY_BACK,
                upTexture = WHITE_KEY_TEXTURE,
                downTexture = WHITE_KEY_DOWN_TEXTURE,
            ),
            blackKeyConfiguration = KeyConfiguration.SeparateTextures(
                frontKeyFile = BLACK_KEY,
                backKeyFile = null,
                upTexture = BLACK_KEY_TEXTURE,
                downTexture = BLACK_KEY_DOWN_TEXTURE,
            ),
        )
    }

    override fun toString(): String = super.toString() + formatProperties(::angle, ::squeezingSpeed, ::expanding)
}
