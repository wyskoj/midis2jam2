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

import com.jme3.asset.ModelKey
import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.shape.Box
import org.wysko.midis2jam2.manager.PerformanceManager
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.quat
import org.wysko.midis2jam2.world.assetLoader
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
 * @param texture The instrument's texture, for the stand-in capo.
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
    )

    /** The tuning keys, lowest string first, or empty while the instrument has no key art. */
    val keys: List<Spatial> = layout?.let { art ->
        art.keys.take(fretboard.stringCount).map { key ->
            context.modelD(art.key, texture).also {
                it.localTranslation = Vector3f(key.position[0], key.position[1], key.position[2])
                parent.attachChild(it)
            }
        }
    } ?: emptyList()

    private val keyRest: List<Quaternion> = layout?.keys?.take(keys.size)?.map {
        Vector3f(it.rotation[0], it.rotation[1], it.rotation[2]).quat()
    } ?: emptyList()

    /** The capo, or `null` if the part is played without one. */
    val capo: Spatial? = if (fretting.capo > 0) loadCapo(context, texture).also { parent.attachChild(it) } else null

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
        // The capo sits just behind its fret, across every string.
        val at = fret - CAPO_BEHIND_FRET
        val low = fretboard.pointOn(0.0, at)
        val high = fretboard.pointOn(fretboard.stringCount - 1.0, at)
        val across = high.subtract(low)
        val width = across.length() * (1 + CAPO_MARGIN)
        across.normalizeLocal()
        val along = across.cross(fretboard.normal).normalizeLocal()
        capo.localTranslation = low.interpolateLocal(high, 0.5f)
        capo.localRotation = Quaternion().fromAxes(across, fretboard.normal, along)
        capo.localScale = Vector3f(width, (1 - motion.capoSquash(seconds)).toFloat(), 1f)
    }

    private fun loadCapo(context: PerformanceManager, texture: String): Spatial =
        if (context.app.assetManager.locateAsset(ModelKey("Assets/$CAPO_MODEL")) != null) {
            context.modelD(CAPO_MODEL, texture)
        } else {
            // A stand-in bar until the capo is modelled: one unit wide across the neck.
            Geometry("Capo", Box(0.5f, CAPO_STAND_IN_HEIGHT, CAPO_STAND_IN_DEPTH)).apply {
                material = context.assetLoader.diffuseMaterial(texture)
            }
        }.apply { cullHint = false.ch }

    private companion object {
        const val CAPO_MODEL = "Capo.obj"
        const val CAPO_BEHIND_FRET = 0.25
        const val CAPO_MARGIN = 0.25f
        const val CAPO_STAND_IN_HEIGHT = 0.12f
        const val CAPO_STAND_IN_DEPTH = 0.2f
    }
}
