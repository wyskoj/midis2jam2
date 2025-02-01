package org.wysko.midis2jam2.scene

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.jme3ktdsl.*

sealed interface InstrumentLocationBehavior {
    fun getTransform(index: Float): Pair<Vector3f, Quaternion>
}

data class Linear(
    private val baseLocation: Vector3f = Vector3f.ZERO,
    private val deltaLocation: Vector3f = Vector3f.ZERO,
    private val baseRotation: Quaternion = Quaternion.IDENTITY,
    private val deltaRotation: Quaternion = Quaternion.ZERO,
) : InstrumentLocationBehavior {
    override fun getTransform(index: Float): Pair<Vector3f, Quaternion> =
        baseLocation + deltaLocation * index to baseRotation + deltaRotation * index
}