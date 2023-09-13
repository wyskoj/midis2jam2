/*
 * Copyright (C) 2023 Jacob Wysko
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
package org.wysko.midis2jam2.instrument.family.piano

import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.instrument.family.percussion.drumset.PercussionInstrument
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.util.cullHint
import org.wysko.midis2jam2.util.toQuat
import org.wysko.midis2jam2.util.toSign

private const val KEY_PRESS_ANGLE = 0.1f
private const val KEY_RECOIL_SPEED = 20f

/**
 * A single key on a [KeyedInstrument].
 */
abstract class Key protected constructor(
    private val rotationAxis: Vector3f,
    private val keyedInstrument: KeyedInstrument,
    private val inverseRotation: Boolean = false,
    keyboardConfiguration: KeyboardConfiguration,
    moveValue: Float,
    midiNote: Int,
) {

    /** The uppermost node of this key. */
    val keyNode: Node = Node()

    /** Contains geometry for the "up" key. */
    private val upNode: Node = Node()

    /** Contains geometry for the "down" key. */
    private val downNode: Node = Node()

    /** Is this key being pressed? */
    val isPressed: Boolean
        get() = currentNote != null

    /** The current note being animated. */
    private var currentNote: MidiNoteOnEvent? = null

    private var rotationFactor = 0f

    init {
        when (noteToKeyboardKeyColor(midiNote)) {
            KeyColor.WHITE -> configureKeyParts(
                configuration = keyboardConfiguration.whiteKeyConfiguration,
                moveValue = moveValue
            )

            KeyColor.BLACK -> configureKeyParts(
                configuration = keyboardConfiguration.blackKeyConfiguration, moveValue = moveValue
            )
        }
        attachKeysToNodes()
        keyedInstrument.instrumentNode.attachChild(keyNode)
        downNode.cullHint = Spatial.CullHint.Always
    }

    /**
     * Animates the motion of the key.
     *
     * @param delta the amount of time since the last frame update
     */
    open fun tick(delta: Float) {
        if (isPressed) {
            rotationFactor = PercussionInstrument.velocityRecoilDampening((currentNote ?: return).velocity).toFloat()
        } else {
            if (rotationFactor > 0f) {
                rotationFactor -= delta * KEY_RECOIL_SPEED
            }
        }
        // Prevent over-rotation
        rotationFactor = rotationFactor.coerceIn(0f..1f)

        (rotationFactor == 0f).let {
            upNode.cullHint = it.cullHint()
            downNode.cullHint = (!it).cullHint()
        }

        keyNode.localRotation = rotationAxis.mult(rotationFactor * KEY_PRESS_ANGLE * -inverseRotation.toSign()).toQuat()
    }

    /**
     * Signals that this key is being pressed. You can safely call this function more than once during the lifetime of a
     * note.
     */
    fun pressKey(note: MidiNoteOnEvent) {
        currentNote = note
    }

    /**
     * Signals that this key is being released.
     */
    fun releaseKey() {
        currentNote = null
    }

    /** Attaches the up and down key nodes to the main key node. */
    private fun attachKeysToNodes() {
        keyNode.apply {
            attachChild(upNode)
            attachChild(downNode)
        }
    }

    /** Loads the key model and attaches it to the given node. */
    private fun loadKeyModel(frontKeyFile: String, backKeyFile: String? = null, texture: String, node: Node) {
        node.attachChild(keyedInstrument.context.loadModel(frontKeyFile, texture))
        backKeyFile?.let { backKey ->
            node.attachChild(keyedInstrument.context.loadModel(backKey, texture).also { it.move(0f, -0.01f, 0f) })
        }
    }

    /** Sets up the key parts. */
    private fun configureKeyParts(
        configuration: KeyConfiguration, moveValue: Float
    ) {
        with(configuration) {
            when (this) {
                is KeyConfiguration.SeparateModels -> {
                    loadKeyModel(frontKeyFile, backKeyFile, texture, upNode)
                    loadKeyModel(frontKeyFileDown, backKeyFileDown, texture, downNode)
                }

                is KeyConfiguration.SeparateTextures -> {
                    loadKeyModel(frontKeyFile, backKeyFile, upTexture, upNode)
                    loadKeyModel(frontKeyFile, backKeyFile, downTexture, downNode)
                }
            }
        }

        keyNode.move(rotationAxis.mult(moveValue))
    }
}
