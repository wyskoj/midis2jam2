package org.wysko.midis2jam2.scene

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.jme3ktdsl.plus
import org.wysko.midis2jam2.jme3ktdsl.times

sealed interface InstrumentLocationBehavior {
    fun getTransform(index: Float): Pair<Vector3f, Quaternion>

    data class Linear(
        private val baseLocation: Vector3f = Vector3f.ZERO,
        private val deltaLocation: Vector3f = Vector3f.ZERO,
        private val baseRotation: Quaternion = Quaternion.ZERO,
        private val deltaRotation: Quaternion = Quaternion.ZERO,
    ) : InstrumentLocationBehavior {
        override fun getTransform(index: Float): Pair<Vector3f, Quaternion> =
            baseLocation + deltaLocation * index to baseRotation + deltaRotation * index
    }

    data class Pivot(
        private val pivotLocation: Vector3f = Vector3f.ZERO,
        private val armDirection: Vector3f = Vector3f.ZERO,
        private val baseRotation: Float = 0f,
        private val deltaRotation: Float = 0f,
        private val rotationAxis: Axis = Axis.Y,
    ) : InstrumentLocationBehavior {
        override fun getTransform(index: Float): Pair<Vector3f, Quaternion> {
            val angle = baseRotation + deltaRotation * index
            val rotation = Quaternion().fromAngleAxis(angle * FastMath.DEG_TO_RAD, rotationAxis.identity)
            val location = pivotLocation + rotation.mult(armDirection)
            return location to rotation
        }
    }

    class Combination(private vararg val behaviors: InstrumentLocationBehavior) : InstrumentLocationBehavior {
        override fun getTransform(index: Float): Pair<Vector3f, Quaternion> =
            behaviors.fold((Vector3f.ZERO.clone() to Quaternion.ZERO.clone())) { acc, behavior ->
                val (loc, rot) = behavior.getTransform(index)
                acc.first + loc to (acc.second + rot).normalizeLocal()
            }
    }
}

