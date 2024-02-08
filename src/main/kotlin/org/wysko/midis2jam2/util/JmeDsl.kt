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

package org.wysko.midis2jam2.util

import com.jme3.material.Material
import com.jme3.math.FastMath.DEG_TO_RAD
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial

fun node(initializer: Node.() -> Unit = {}): Node = Node().apply(initializer)

context(Node)
operator fun <T : Spatial> T.unaryPlus(): T {
    attachChild(this@unaryPlus)
    return this@unaryPlus
}

context(Node)
operator fun Node.unaryMinus() {
    removeFromParent()
}

var Spatial.loc: Vector3f
    get() = localTranslation
    set(value) {
        localTranslation = value
    }

var Spatial.rot: Vector3f
    get() {
        val q = localRotation.toAngles(null)
        return Vector3f(q[0], q[1], q[2])
    }
    set(value) {
        localRotation = Quaternion().fromAngles(value.x * DEG_TO_RAD, value.y * DEG_TO_RAD, value.z * DEG_TO_RAD)
    }

var Spatial.scale: Vector3f
    get() = localScale
    set(value) {
        localScale = value
    }

fun v3(
    x: Number,
    y: Number,
    z: Number,
) = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())

operator fun Node.get(index: Int): Spatial = getChild(index)

var Spatial.material: Material
    get() {
        if (this !is Geometry) error("Cannot get material of a non-geometry spatial")
        return this.material
    }
    set(value) {
        this.setMaterial(value)
    }

operator fun Node.plusAssign(spatial: Spatial) {
    attachChild(spatial)
}

operator fun Node.minusAssign(spatial: Spatial) {
    detachChild(spatial)
}

operator fun Vector3f.times(number: Number): Vector3f = this.mult(number.toFloat())

operator fun Vector3f.plus(other: Vector3f): Vector3f = this.add(other)