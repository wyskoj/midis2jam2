package org.wysko.midis2jam2.jme3ktdsl

import com.jme3.app.SimpleApplication
import com.jme3.app.state.BaseAppState
import com.jme3.asset.AssetManager
import com.jme3.math.FastMath.DEG_TO_RAD
import com.jme3.math.FastMath.RAD_TO_DEG
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint
import com.jme3.scene.control.Control

fun node(initializer: Node.() -> Unit = {}): Node = Node().apply(initializer)

var Spatial.loc: Vector3f
    get() = localTranslation
    set(value) {
        localTranslation = value
    }

var Spatial.rot: Vector3f
    get() {
        val q = localRotation.toAngles(null)
        return Vector3f(q[0] * RAD_TO_DEG, q[1] * RAD_TO_DEG, q[2] * RAD_TO_DEG)
    }
    set(value) {
        localRotation = value.quat()
    }

var Spatial.rotQ: Quaternion
    get() = localRotation
    set(value) {
        localRotation = value
    }

var Spatial.scale: Double
    get() = localScale.x.toDouble()
    set(value) {
        localScale = Vector3f(value.toFloat(), value.toFloat(), value.toFloat())
    }

var Spatial.scaleVec: Vector3f
    get() = localScale
    set(value) {
        localScale = value
    }

val Boolean.cull: CullHint
    get() = if (this) CullHint.Dynamic else CullHint.Always

fun vec3(x: Number, y: Number, z: Number): Vector3f = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())

operator fun Node.plusAssign(child: Spatial) {
    attachChild(child)
}

operator fun Node.minusAssign(child: Spatial) {
    detachChild(child)
}

operator fun Vector3f.times(scalar: Number): Vector3f = mult(scalar.toFloat())

operator fun Vector3f.plus(other: Vector3f): Vector3f = add(other)

operator fun Vector3f.minus(other: Vector3f): Vector3f = subtract(other)

operator fun Quaternion.times(scalar: Number): Quaternion = mult(scalar.toFloat())

operator fun Quaternion.plus(other: Quaternion): Quaternion = add(other)

operator fun Quaternion.minus(other: Quaternion): Quaternion = subtract(other)


val BaseAppState.root: Node
    get() = (this.application as SimpleApplication).rootNode

val BaseAppState.assetManager: AssetManager
    get() = (this.application as SimpleApplication).assetManager

fun Vector3f.quat(): Quaternion = Quaternion().fromAngles(x * DEG_TO_RAD, y * DEG_TO_RAD, z * DEG_TO_RAD)

inline fun <reified T : Control> Spatial.control(): T = getControl(T::class.java)