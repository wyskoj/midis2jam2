package org.wysko.midis2jam2.scene

import com.jme3.math.Vector3f
import org.wysko.midis2jam2.jme3ktdsl.vec3

enum class Axis(val componentIndex: Int, val identity: Vector3f) {
    X(0, vec3(1, 0, 0)),
    Y(1, vec3(0, 1, 0)),
    Z(2, vec3(0, 0, 1))
}
