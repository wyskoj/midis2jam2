@file:UseSerializers(Vector3fSerializer::class)

package org.wysko.midis2jam2.scene

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.wysko.midis2jam2.jme3ktdsl.Vector3fSerializer
import org.wysko.midis2jam2.jme3ktdsl.plus
import org.wysko.midis2jam2.jme3ktdsl.quat
import org.wysko.midis2jam2.jme3ktdsl.times
import org.wysko.midis2jam2.jme3ktdsl.vec3

@Serializable
sealed interface PositioningBehavior {
    sealed class Calculated : PositioningBehavior {
        abstract fun getTransform(index: Float): Pair<Vector3f, Quaternion>

        @Serializable
        data class Linear(
            private val baseLocation: Vector3f = vec3(0, 0, 0),
            private val deltaLocation: Vector3f = vec3(0, 0, 0),
            private val baseRotation: Vector3f = vec3(0, 0, 0),
            private val deltaRotation: Vector3f = vec3(0, 0, 0),
        ) : Calculated() {
            override fun getTransform(index: Float): Pair<Vector3f, Quaternion> =
                baseLocation + deltaLocation * index to (baseRotation + deltaRotation * index).quat()
        }

        @Serializable
        data class Pivot(
            private val pivotLocation: Vector3f = vec3(0, 0, 0),
            private val armDirection: Vector3f = vec3(0, 0, 0),
            private val individualRotation: Vector3f = vec3(0, 0, 0),
            private val baseRotation: Float = 0f,
            private val deltaRotation: Float = 0f,
            private val rotationAxis: Axis = Axis.Y,
        ) : Calculated() {
            override fun getTransform(index: Float): Pair<Vector3f, Quaternion> {
                val angle = baseRotation + deltaRotation * index
                val rotation =
                    Quaternion().fromAngleAxis(angle * FastMath.DEG_TO_RAD, rotationAxis.identity).normalizeLocal()
                val location = pivotLocation + rotation.mult(armDirection)
                return location to (rotation.mult(individualRotation.quat())).normalizeLocal()
            }
        }

        companion object {
            fun List<PositioningBehavior>.getTransform(index: Float): Pair<Vector3f, Quaternion> =
                filterIsInstance<Calculated>().fold((vec3(0, 0, 0) to vec3(0, 0, 0).quat())) { acc, behavior ->
                    val (loc, rot) = behavior.getTransform(index)
                    acc.first + loc to (acc.second * rot).normalizeLocal()
                }
        }
    }

    @Serializable
    data object Deferred : PositioningBehavior
}
