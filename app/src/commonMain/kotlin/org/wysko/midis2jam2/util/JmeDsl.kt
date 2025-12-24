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

package org.wysko.midis2jam2.util

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppState
import com.jme3.material.Material
import com.jme3.math.FastMath.DEG_TO_RAD
import com.jme3.math.FastMath.RAD_TO_DEG
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial

/**
 * Creates a new [Node] with the given [initializer].
 */
fun node(initializer: Node.() -> Unit = {}): Node = Node().apply(initializer)

/**
 * Adds this to the parent from context.
 */
context(Node)
operator fun <T : Spatial> T.unaryPlus(): T {
    attachChild(this@unaryPlus)
    return this@unaryPlus
}

/**
 * Removes this from the parent from context.
 */
context(Node)
operator fun Node.unaryMinus() {
    removeFromParent()
}

/**
 * Sets the local translation of this spatial.
 */
var Spatial.loc: Vector3f
    get() = localTranslation
    set(value) {
        localTranslation = value
    }

/**
 * Sets the local rotation of this spatial by converting the given [Vector3f] to a [Quaternion].
 */
var Spatial.rot: Vector3f
    get() {
        val q = localRotation.toAngles(null)
        return Vector3f(q[0], q[1], q[2])
    }
    set(value) {
        localRotation = Quaternion().fromAngles(value.x * DEG_TO_RAD, value.y * DEG_TO_RAD, value.z * DEG_TO_RAD)
    }


var Spatial.rotR: Vector3f
    get() {
        val q = localRotation.toAngles(null)
        return Vector3f(q[0] * RAD_TO_DEG, q[1] * RAD_TO_DEG, q[2] * RAD_TO_DEG)
    }
    set(value) {
        localRotation = value.quat()
    }

fun Vector3f.quat(): Quaternion = Quaternion().fromAngles(x * DEG_TO_RAD, y * DEG_TO_RAD, z * DEG_TO_RAD)

/**
 * Sets the local scale of this spatial.
 */
var Spatial.scale: Vector3f
    get() = localScale
    set(value) {
        localScale = value
    }

/**
 * Convenience function to create a new [Vector3f] from the given [x], [y], and [z].
 *
 * @param x The x component of the vector.
 * @param y The y component of the vector.
 * @param z The z component of the vector.
 */
fun v3(x: Number, y: Number, z: Number): Vector3f = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())

/**
 * Returns the child at the given [index].
 */
operator fun Node.get(index: Int): Spatial = getChild(index)

/**
 * Convenience property to get the material of a [Geometry] and set the material of a [Geometry].
 */
var Spatial.material: Material
    get() {
        if (this !is Geometry) error("Cannot get material of a non-geometry spatial")
        return this.material
    }
    set(value) {
        this.setMaterial(value)
    }

/**
 * Adds the given [spatial] to this node.
 */
operator fun Node.plusAssign(spatial: Spatial) {
    attachChild(spatial)
}

/**
 * Removes the given [spatial] from this node.
 */
operator fun Node.minusAssign(spatial: Spatial) {
    detachChild(spatial)
}

/**
 * Multiplies this [Vector3f] by the given [number].
 */
operator fun Vector3f.times(number: Number): Vector3f = this.mult(number.toFloat())

/**
 * Adds this [Vector3f] to the given [other].
 */
operator fun Vector3f.plus(other: Vector3f): Vector3f = this.add(other)

inline fun <reified T : AppState> Application.state(): T? = this.stateManager.getState<T>(T::class.java)