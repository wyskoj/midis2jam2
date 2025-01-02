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
package org.wysko.midis2jam2.instrument.family.piano

import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.instrument.family.percussion.PercussionInstrument
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.sign
import org.wysko.midis2jam2.util.toQuaternion
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

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
    val noteNumber: Byte,
) {
    /** Whether this key is pressed. */
    @Deprecated("")
    val isPressed: Boolean
        get() = currentState is State.Down

    /** The uppermost node of this key. */
    val root: Node = Node()

    /** Contains geometry for the "up" key. */
    private val upNode: Node = Node()

    /** Contains geometry for the "down" key. */
    private val downNode: Node = Node()

    /** The current note being animated. */
    var currentState: State = State.Up

    private var rotationFactor = 0f

    init {
        when (Color.fromNoteNumber(noteNumber)) {
            Color.White ->
                configureKeyParts(
                    configuration = keyboardConfiguration.whiteKeyConfiguration,
                    moveValue = moveValue,
                )

            Color.Black ->
                configureKeyParts(
                    configuration = keyboardConfiguration.blackKeyConfiguration,
                    moveValue = moveValue,
                )
        }
        attachKeysToNodes()
        keyedInstrument.geometry.attachChild(root)
        downNode.cullHint = Spatial.CullHint.Always
    }

    /**
     * Animates the motion of the key.
     *
     * @param delta the amount of time since the last frame update
     */
    open fun tick(delta: Duration) {
        currentState = keyedInstrument.keyStatus(noteNumber)
        if (currentState is State.Down) {
            rotationFactor =
                PercussionInstrument.velocityRecoilDampening((currentState as State.Down).velocity).toFloat()
        } else {
            if (rotationFactor > 0f) {
                rotationFactor -= (delta.toDouble(SECONDS) * KEY_RECOIL_SPEED).toFloat()
            }
        }
        // Prevent over-rotation
        rotationFactor = rotationFactor.coerceIn(0f..1f)

        (rotationFactor == 0f).let {
            upNode.cullHint = it.ch
            downNode.cullHint = (!it).ch
        }

        root.localRotation = rotationAxis.mult(rotationFactor * KEY_PRESS_ANGLE * -inverseRotation.sign).toQuaternion()
    }

    /** Attaches the up and down key nodes to the main key node. */
    private fun attachKeysToNodes() {
        root.apply {
            attachChild(upNode)
            attachChild(downNode)
        }
    }

    /** Loads the key model and attaches it to the given node. */
    private fun loadKeyModel(
        frontKeyFile: String,
        backKeyFile: String? = null,
        texture: String,
        node: Node,
    ) {
        node.attachChild(keyedInstrument.context.modelD(frontKeyFile, texture))
        backKeyFile?.let { backKey ->
            node.attachChild(keyedInstrument.context.modelD(backKey, texture).also { it.move(0f, -0.01f, 0f) })
        }
    }

    /** Sets up the key parts. */
    private fun configureKeyParts(
        configuration: KeyConfiguration,
        moveValue: Float,
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

        root.move(rotationAxis.mult(moveValue))
    }

    /** The different states of a key. */
    sealed class State {
        /** The key is up. */
        data object Up : State()

        /**
         * The key is down.
         *
         * @property velocity The velocity of the key press.
         */
        data class Down(val velocity: Byte) : State()
    }

    /** The different colors of keys on a keyboard. */
    enum class Color {
        /** White key color. */
        White,

        /** Black key color. */
        Black;

        companion object {
            /**
             * Given a MIDI [note], determines if it is a [Color.White] or [Color.Black] key on a standard keyboard.
             */
            fun fromNoteNumber(note: Byte): Color =
                when (note % 12) {
                    1, 3, 6, 8, 10 -> Black
                    else -> White
                }
        }
    }
}
