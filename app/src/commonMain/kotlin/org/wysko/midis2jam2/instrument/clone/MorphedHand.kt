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
package org.wysko.midis2jam2.instrument.clone

import com.jme3.anim.MorphControl
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Spatial
import com.jme3.scene.VertexBuffer.Type
import com.jme3.scene.mesh.MorphTarget
import com.jme3.util.BufferUtils
import org.wysko.midis2jam2.instrument.algorithmic.HandShapeBlend.Blend
import java.nio.FloatBuffer

/**
 * A hand drawn as one mesh that morphs between its shapes, so the fingers can move smoothly from one to the next.
 *
 * The first shape is the base mesh and every other shape is a morph target. A hand only ever blends two shapes at
 * once, so at most two targets are active and the GPU does the morphing.
 */
class MorphedHand private constructor(val geometry: Geometry, shapeCount: Int) {

    private val weights = FloatArray(shapeCount - 1)

    /** The shape the hand was last put in. */
    var blend: Blend = Blend(0, 0, 0f)
        private set

    /** Puts the hand in [blend]. */
    fun show(blend: Blend) {
        if (blend == this.blend) return
        this.blend = blend
        weights.fill(0f)
        if (blend.from > 0) weights[blend.from - 1] += 1 - blend.t
        if (blend.to > 0) weights[blend.to - 1] += blend.t
        geometry.setMorphState(weights)
    }

    companion object {
        /**
         * Builds a morphing hand from [shapes], one model per shape, or returns null if they can't be morphed between
         * (they aren't all single meshes with the same vertices in the same order).
         */
        fun from(shapes: List<Spatial>): MorphedHand? {
            val geometries = shapes.map { it as? Geometry ?: return null }
            if (geometries.size < 2) return null
            val base = geometries.first()
            if (geometries.any { !sameTopology(base.mesh, it.mesh) }) return null

            val mesh = base.mesh.deepClone()
            geometries.drop(1).forEach { shape ->
                mesh.addMorphTarget(
                    MorphTarget(shape.name).apply {
                        setBuffer(Type.Position, difference(shape.mesh, base.mesh, Type.Position))
                        difference(shape.mesh, base.mesh, Type.Normal)?.let { setBuffer(Type.Normal, it) }
                    }
                )
            }
            // The hand reaches wherever any of its shapes do, so it isn't culled mid-move.
            mesh.bound = geometries.map { it.mesh.bound }.reduce { a, b -> a.merge(b) }

            val geometry = Geometry(base.name, mesh).apply {
                // The morph weights are read from the material, so each hand needs its own.
                material = base.material.clone()
                // The local modes: the shapes aren't in the scene, so their effective modes would resolve to defaults
                // (no shadow) rather than inheriting from the instrument as they should.
                queueBucket = base.localQueueBucket
                shadowMode = base.localShadowMode
                addControl(MorphControl())
            }
            return MorphedHand(geometry, geometries.size).apply { geometry.setMorphState(weights) }
        }

        private fun sameTopology(a: Mesh, b: Mesh): Boolean {
            if (a.vertexCount != b.vertexCount) return false
            val (indicesA, indicesB) = (a.indexBuffer ?: return b.indexBuffer == null) to (b.indexBuffer ?: return false)
            if (indicesA.size() != indicesB.size()) return false
            return (0 until indicesA.size()).all { indicesA[it] == indicesB[it] }
        }

        private fun difference(shape: Mesh, base: Mesh, type: Type): FloatBuffer? {
            val target = shape.getFloatBuffer(type) ?: return null
            val origin = base.getFloatBuffer(type) ?: return null
            return BufferUtils.createFloatBuffer(origin.limit()).apply {
                for (i in 0 until origin.limit()) put(i, target.get(i) - origin.get(i))
            }
        }
    }
}
