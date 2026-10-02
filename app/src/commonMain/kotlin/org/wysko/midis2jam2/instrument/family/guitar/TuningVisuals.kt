/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.instrument.family.guitar

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.midis2jam2.manager.INTRO
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

/**
 * Shows a fretted instrument's tuning and capo on the instrument itself, following a [TuningMotion].
 *
 * Each tuning key is turned by how far its string is tuned from standard (when the instrument has key art; see
 * [TuningKeyLayout]), the strings vibrate with their tension (read by the instrument through [vibrationSpeed] and
 * [vibrationWidth]), and a capo is clamped across the neck at its fret.
 *
 * @param context The performance.
 * @param parent The instrument's geometry node, whose space the fretboard is in.
 * @property fretboard Where things are on the neck.
 * @param fretting What the fretting engine decided.
 * @param layout Where the tuning keys go, or `null` if the instrument has no key art yet.
 * @param texture The texture of the instrument's body, which its keys and capo share.
 */
class TuningVisuals(
    context: PerformanceManager,
    parent: Node,
    private val fretboard: FretboardSpace,
    fretting: FrettingPlan,
    private val layout: TuningKeyLayout?,
    texture: String,
) {
    /** How the tuning and capo are shown over time. */
    val motion: TuningMotion = TuningMotion(
        offsets = IntArray(fretting.profile.stringCount) { fretting.tuning[it] - fretting.profile.defaultTuning[it] },
        capo = fretting.capo,
        firstNote = fretting.notes.indices
            .filter { fretting.solution.fingerings[it] != null }
            .minOfOrNull { fretting.notes[it].start },
        songStart = -INTRO.toDouble(SECONDS),
    )

    /** The tuning keys, lowest string first, or empty while the instrument has no key art. */
    val keys: List<Spatial> = layout?.let { art ->
        art.keys.take(fretboard.stringCount).map { key ->
            context.modelD(art.key, art.keyTexture ?: texture).also {
                it.localTranslation = TuningKeyLayout.blenderPosition(key.position[0], key.position[1], key.position[2])
                it.setLocalScale(art.keyScale)
                parent.attachChild(it)
            }
        }
    } ?: emptyList()

    /** Each key's orientation in standard tuning, including any turn of the key model within it. */
    private val keyRest: List<Quaternion> = layout?.let { art ->
        val model = with(art.keyRotation) { TuningKeyLayout.blenderRotation(this[0], this[1], this[2]) }
        art.keys.take(keys.size).map {
            TuningKeyLayout.blenderRotation(it.rotation[0], it.rotation[1], it.rotation[2]).mult(model)
        }
    } ?: emptyList()

    /** The capo, or `null` if the part is played without one. */
    val capo: Spatial? = if (fretting.capo > 0) {
        context.modelD(CAPO_MODEL, layout?.keyTexture ?: texture).also {
            it.cullHint = false.ch
            parent.attachChild(it)
        }
    } else {
        null
    }

    private var seconds = 0.0

    /** How fast [string]'s vibration cycles now, relative to standard tuning. */
    fun vibrationSpeed(string: Int): Double = motion.vibrationSpeed(string, seconds)

    /** How wide [string] vibrates now, relative to standard tuning. */
    fun vibrationWidth(string: Int): Double = motion.vibrationWidth(string, seconds)

    /** Whether [string] rings open now because it is being tuned. */
    fun ringsWhileTuning(string: Int): Boolean = motion.ringsWhileTuning(string, seconds)

    /** The angle [string]'s key is turned to now, in degrees from where it sits in standard tuning. */
    fun keyAngle(string: Int): Float {
        val art = layout ?: return 0f
        return (art.keys[string].direction * art.degreesPerSemitone * motion.semitones(string, seconds)).toFloat()
    }

    /** Moves everything to where it is at [time]. */
    fun update(time: Duration) {
        seconds = time.toDouble(SECONDS)
        keys.forEachIndexed { s, key ->
            key.localRotation = keyRest[s].mult(Quaternion().fromAngleAxis(keyAngle(s) * FastMath.DEG_TO_RAD, Vector3f.UNIT_Y))
        }
        capo?.let(::placeCapo)
    }

    private fun placeCapo(capo: Spatial) {
        val fret = motion.capoFret(seconds)
        capo.cullHint = (fret != null).ch
        if (fret == null) return
        // The capo is modelled in place across the neck, so it only slides along it (the model's Y) to just behind
        // its fret, and lifts off the strings (the model's Z) until it clamps.
        val along = fretboard.pointOn((fretboard.stringCount - 1) / 2.0, fret - CAPO_BEHIND_FRET).y
        capo.localTranslation = Vector3f(0f, along, (CAPO_LIFT * motion.capoLift(seconds)).toFloat())
    }

    private companion object {
        const val CAPO_MODEL = "GuitarCapo.obj"
        const val CAPO_BEHIND_FRET = 0.25
        const val CAPO_LIFT = 0.4
    }
}
